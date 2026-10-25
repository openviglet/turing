<?php
/**
 * Turing answers WordPress's own search query; the theme renders it.
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

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
	}
}
