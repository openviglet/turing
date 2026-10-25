<?php
/**
 * Turing answers WordPress's own search query; the theme renders it.
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Brain\Monkey\Filters;
use Brain\Monkey\Functions;
use Viglet_Dumont_Native_Search;
use WP_Error;

final class NativeSearchTest extends PluginTestCase {

	/** The document ids Turing answers with. */
	private array $held = array();

	protected function setUp(): void {
		parent::setUp();

		Functions\when( 'wp_remote_request' )->alias(
			function ( string $url, array $args ) {
				$this->requests[] = array( $url, $args );
				return array(
					'response' => array( 'code' => $this->status ),
					'body'     => json_encode(
						array(
							'queryContext' => array( 'count' => 42 ),
							'results'      => array(
								'document' => array_map( static fn ( $id ) => array( 'fields' => array( 'id' => $id ) ), $this->held ),
							),
						)
					),
				);
			}
		);
	}

	private function search(): Viglet_Dumont_Native_Search {
		return new Viglet_Dumont_Native_Search( $this->settings( array( 'site_key' => 'site-a' ) ) );
	}

	public function test_the_answer_is_this_sites_post_ids_in_turings_order_with_the_total(): void {
		$this->held = array( 'site-a:post:7', 'site-a:post:3', 'wp-9', 'site-a:post:x' );

		$found = $this->search()->search( 'workshop', 2, 10 );

		$this->assertSame(
			array(
				'ids'   => array( 7, 3 ),
				'total' => 42,
			),
			$found
		);
		$this->assertStringContainsString( 'fq%5B%5D=site_key%3Asite-a', $this->requests[0][0] );
		$this->assertStringContainsString( 'p=2', $this->requests[0][0] );
	}

	public function test_an_unreachable_turing_answers_null_so_wordpress_searches_instead(): void {
		Functions\when( 'wp_remote_request' )->justReturn( new WP_Error( 'http_request_failed', 'refused' ) );

		$this->assertNull( $this->search()->search( 'workshop', 1, 10 ) );
	}

	public function test_a_refined_search_page_is_noindex_and_the_plain_one_is_not(): void {
		Functions\when( 'is_singular' )->justReturn( true );
		Functions\when( 'get_queried_object' )->justReturn( null );
		Functions\when( 'has_block' )->justReturn( true );

		$_GET = array( 'q' => 'workshop' );
		$this->assertSame( array(), Viglet_Dumont_Native_Search::robots( array() ) );

		foreach ( array(
			'fq'   => array( 'seats:40' ),
			'pg'   => '2',
			'sort' => 'newest',
		) as $key => $value ) {
			$_GET = array(
				'q'  => 'workshop',
				$key => $value,
			);
			$this->assertSame(
				array(
					'noindex' => true,
					'follow'  => true,
				),
				Viglet_Dumont_Native_Search::robots( array() ),
				$key
			);
		}

		// A refined URL on a page without the block is not this plugin's to judge.
		Functions\when( 'has_block' )->justReturn( false );
		$this->assertSame( array(), Viglet_Dumont_Native_Search::robots( array() ) );
		$_GET = array();
	}

	public function test_the_overlay_sends_enter_to_the_chosen_page_then_the_block_page_then_wordpress_search(): void {
		Functions\when( 'get_permalink' )->alias( static fn ( $id ) => "https://example.org/?page_id=$id" );
		Functions\when( 'home_url' )->justReturn( 'https://example.org/' );
		Functions\when( 'get_post_status' )->justReturn( 'publish' );
		Functions\when( 'get_posts' )->justReturn( array( 7 ) );

		$chosen = new Viglet_Dumont_Native_Search( $this->settings( array( 'search_page' => 12 ) ) );
		$this->assertSame(
			array(
				'searchPage' => 'https://example.org/?page_id=12',
				'queryParam' => 'q',
			),
			$chosen->search_page()
		);

		$found = new Viglet_Dumont_Native_Search( $this->settings( array( 'search_page' => 0 ) ) );
		$this->assertSame( 'https://example.org/?page_id=7', $found->search_page()['searchPage'] );

		Functions\when( 'get_posts' )->justReturn( array() );
		$this->assertSame(
			array(
				'searchPage' => 'https://example.org/',
				'queryParam' => 's',
			),
			$found->search_page()
		);
	}

	public function test_the_public_config_carries_no_key_and_lists_the_facets(): void {
		$options = array(
			'viglet_dumont_mapping' => array(
				'post' => array(
					'fields' => array(
						array(
							'source'      => 'taxonomy:area',
							'field'       => 'area',
							'type'        => 'STRING',
							'multi'       => true,
							'facet'       => true,
							'facet_label' => 'Área',
						),
					),
				),
			),
		);
		Functions\when( 'get_option' )->alias( static fn ( $name, $fallback = false ) => $options[ $name ] ?? $fallback );

		$config = ( new Viglet_Dumont_Native_Search( $this->settings( array( 'public_url' => 'https://search.example.org/' ) ) ) )->public_config();

		$this->assertSame( 'https://search.example.org', $config['url'] );
		$this->assertSame( 'Sample Site', $config['site'] );
		$this->assertSame(
			array(
				array(
					'field' => 'area',
					'label' => 'Área',
					'type'  => 'STRING',
					'multi' => true,
				),
			),
			$config['facets']
		);
		$this->assertStringNotContainsString( 'secret', (string) json_encode( $config ) );
		$this->assertArrayNotHasKey( 'analytics', $config, 'a site that reports nothing says nothing about it' );
	}

	public function test_analytics_say_what_is_reported_and_whether_consent_comes_first(): void {
		$both = new Viglet_Dumont_Native_Search(
			$this->settings(
				array(
					'track_clicks'      => 1,
					'analytics_ga'      => 1,
					'ga_measurement_id' => 'G-ABC123',
				)
			)
		);
		// Without a consent plugin, turning reporting on is the site's answer.
		$this->assertSame(
			array(
				'clicks'         => true,
				'ga'             => true,
				'measurementId'  => 'G-ABC123',
				'requireConsent' => false,
			),
			$both->analytics()
		);

		Filters\expectApplied( 'viglet_dumont_tracking_consent' )->andReturn( false );
		$this->assertTrue( $both->analytics()['requireConsent'], 'the filter can make every visitor say yes first' );

		$clicks = new Viglet_Dumont_Native_Search(
			$this->settings(
				array(
					'track_clicks'      => 1,
					'ga_measurement_id' => 'G-ABC123',
				)
			)
		);
		$this->assertSame( '', $clicks->analytics()['measurementId'], 'no measurement without the Google Analytics sink' );
		$this->assertNull( $this->search()->analytics() );
	}

	public function test_only_a_ga4_measurement_id_is_kept(): void {
		$this->assertSame( 'G-ABC123XYZ', Viglet_Dumont_Native_Search::sanitize_measurement_id( ' g-abc123xyz ' ) );
		$this->assertSame( '', Viglet_Dumont_Native_Search::sanitize_measurement_id( 'UA-1234-1' ) );
		$this->assertSame( '', Viglet_Dumont_Native_Search::sanitize_measurement_id( 'G-"><script>' ) );
	}
}
