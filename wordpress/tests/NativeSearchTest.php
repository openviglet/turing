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

	/**
	 * An index-only site sends its searches to the results page another site shows (D304):
	 * the first branch of search_page(), so the overlay, the box-only block and the forms move
	 * together, and a theme's `?s=` page on the other site keeps its own parameter.
	 */
	public function test_a_results_page_on_another_site_is_where_every_search_goes(): void {
		Functions\when( 'esc_url_raw' )->returnArg();
		Functions\expect( 'get_posts' )->never();

		$remote = new Viglet_Dumont_Native_Search( $this->settings( array( 'search_url' => 'https://www.example.org/busca/' ) ) );
		$this->assertSame(
			array(
				'searchPage' => 'https://www.example.org/busca/',
				'queryParam' => 'q',
			),
			$remote->search_page()
		);

		$theme = new Viglet_Dumont_Native_Search( $this->settings( array( 'search_url' => 'https://www.example.org/?s=' ) ) );
		$this->assertSame( 's', $theme->search_page()['queryParam'] );
	}

	/** Only an absolute https page on another site is kept (D304). */
	public function test_a_results_page_url_must_be_https_and_not_this_site(): void {
		Functions\when( 'esc_url_raw' )->returnArg();

		$this->assertSame( 'https://www.example.org/busca/', Viglet_Dumont_Native_Search::sanitize_search_url( ' https://www.example.org/busca/ ' ) );
		$this->assertSame( '', Viglet_Dumont_Native_Search::sanitize_search_url( 'http://www.example.org/busca/' ), 'not https' );
		$this->assertSame( '', Viglet_Dumont_Native_Search::sanitize_search_url( '/busca/' ), 'not absolute' );
		$this->assertSame( '', Viglet_Dumont_Native_Search::sanitize_search_url( 'https://site.example.org/?s=' ), 'this site' );
		$this->assertSame( '', Viglet_Dumont_Native_Search::sanitize_search_url( '' ) );

		// A local stack served over http may point at another http site of the same stack.
		Functions\when( 'home_url' )->justReturn( 'http://localhost:8081/second/' );
		$this->assertSame( 'http://localhost:8081/busca/', Viglet_Dumont_Native_Search::sanitize_search_url( 'http://localhost:8081/busca/' ) );
	}

	public function test_the_public_config_names_the_results_page_on_another_site(): void {
		Functions\when( 'esc_url_raw' )->returnArg();
		Functions\when( 'get_option' )->alias( static fn ( $name, $fallback = false ) => $fallback );

		$config = ( new Viglet_Dumont_Native_Search( $this->settings( array( 'search_url' => 'https://www.example.org/busca/' ) ) ) )->public_config();
		$this->assertSame( 'https://www.example.org/busca/', $config['searchUrl'] );

		$this->assertArrayNotHasKey( 'searchUrl', ( new Viglet_Dumont_Native_Search( $this->settings() ) )->public_config() );
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
		// Where a result indexed by another WordPress opens on this one (D239).
		$this->assertSame( 'https://site.example.org', $config['home'] );
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

	/**
	 * A mapped field marked "Show on result cards" reaches the browser as a card line (D305),
	 * labelled, once even when two types map it, and only when marked.
	 */
	public function test_the_public_config_lists_the_card_fields_in_mapping_order(): void {
		$options = array(
			'viglet_dumont_mapping' => array(
				'post' => array(
					'fields' => array(
						array(
							'source'      => 'meta:inicio',
							'field'       => 'event_start',
							'type'        => 'DATE',
							'facet_label' => 'Starts',
							'card'        => true,
						),
						array(
							'source' => 'meta:local',
							'field'  => 'venue',
							'type'   => 'STRING',
							'card'   => true,
						),
						array(
							'source' => 'meta:sala',
							'field'  => 'room',
							'type'   => 'STRING',
						),
					),
				),
				'page' => array(
					'fields' => array(
						array(
							'source' => 'meta:local',
							'field'  => 'venue',
							'type'   => 'STRING',
							'card'   => true,
						),
					),
				),
			),
		);
		Functions\when( 'get_option' )->alias( static fn ( $name, $fallback = false ) => $options[ $name ] ?? $fallback );

		$this->assertSame(
			array(
				array(
					'field' => 'event_start',
					'label' => 'Starts',
					'type'  => 'DATE',
				),
				array(
					'field' => 'venue',
					'label' => 'venue',
					'type'  => 'STRING',
				),
			),
			( new Viglet_Dumont_Native_Search( $this->settings() ) )->public_config()['cards']
		);
	}

	/** A card line is what a reader can use: a date in the site's format, a list, never a raw URL. */
	public function test_card_values_format_dates_join_lists_and_drop_urls(): void {
		Functions\when( 'wp_strip_all_tags' )->alias( 'strip_tags' );
		Functions\when( 'get_option' )->justReturn( 'd/m/Y' );
		Functions\when( 'wp_date' )->alias( static fn ( $format, $timestamp ) => gmdate( $format, $timestamp ) );

		$cards = array(
			array(
				'field' => 'event_start',
				'label' => 'Starts',
				'type'  => 'DATE',
			),
			array(
				'field' => 'authors',
				'label' => 'Authors',
				'type'  => 'STRING',
			),
			array(
				'field' => 'link',
				'label' => 'Link',
				'type'  => 'STRING',
			),
			array(
				'field' => 'absent',
				'label' => 'Absent',
				'type'  => 'STRING',
			),
		);
		$lines = Viglet_Dumont_Native_Search::card_values(
			$cards,
			array(
				'event_start' => '2026-11-20T13:00:00Z',
				'authors'     => array( 'Ana <mark>Lima</mark>', '', 'Rui Costa' ),
				'link'        => 'https://example.org/inscricao',
			)
		);

		$this->assertSame(
			array(
				array(
					'field' => 'event_start',
					'label' => 'Starts',
					'value' => '20/11/2026',
				),
				array(
					'field' => 'authors',
					'label' => 'Authors',
					'value' => 'Ana Lima, Rui Costa',
				),
			),
			$lines
		);
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

	public function test_a_page_opened_with_a_query_draws_its_first_page_unrecorded_in_the_shape_the_script_keeps(): void {
		Functions\stubs(
			array(
				'wp_date'            => static fn ( $format, $ts ) => gmdate( 'Y-m-d', $ts ),
				'get_option'         => static fn ( $name, $fallback = false ) => $fallback,
				'number_format_i18n' => static fn ( $n ) => (string) $n,
				'wp_unslash'         => static fn ( $v ) => $v,
				'wp_strip_all_tags'  => static fn ( $v ) => trim( strip_tags( $v ) ),
				'_n'                 => static fn ( $one, $many, $n ) => 1 === $n ? $one : $many,
			)
		);
		Functions\when( 'wp_remote_request' )->alias(
			function ( string $url, array $args ) {
				$this->requests[] = array( $url, $args );
				return array(
					'response' => array( 'code' => 200 ),
					'body'     => json_encode(
						array(
							'queryContext' => array(
								'count'         => 7,
								'defaultFields' => array(
									'title'       => 'title',
									'description' => 'abstract',
									'url'         => 'url',
								),
							),
							'results'      => array(
								'document' => array(
									array(
										'fields' => array(
											'id'       => 'site-a:post:8',
											'title'    => 'Workshop on <mark>search</mark>',
											'abstract' => 'About <mark>search</mark> and facets.',
											'url'      => 'https://example.org/workshop/',
											'type'     => 'post',
										),
									),
								),
							),
							'pagination'   => array(
								array(
									'type' => 'CURRENT',
									'page' => 1,
									'text' => '1',
								),
								array(
									'type' => 'PAGE',
									'page' => 2,
									'text' => '2',
								),
								array(
									'type' => 'NEXT',
									'page' => 2,
									'text' => 'Next',
								),
							),
							'widget'       => array(
								'facet' => array(
									array(
										'name'   => 'seats',
										'label'  => array( 'text' => 'Seats' ),
										'facets' => array(
											array(
												'label' => '40',
												'count' => 3,
												'filterQuery' => 'seats:40',
											),
											array(
												'label' => '20',
												'count' => 0,
												'filterQuery' => 'seats:20',
											),
										),
									),
								),
							),
						)
					),
				);
			}
		);

		$page = $this->search()->first_page(
			array(
				'query'      => 'search',
				'perPage'    => 10,
				'sort'       => 'newest',
				'typeLabels' => array( 'post' => 'Post' ),
				'cartLabel'  => 'Add to cart: %s',
				'noneLabel'  => 'No results found.',
			)
		);

		[ $url, $args ] = $this->requests[0];
		$this->assertSame( 'POST', $args['method'] );
		$this->assertSame( array( 'populateMetrics' => false ), json_decode( $args['body'], true ), 'the script\'s search is the one counted' );
		$this->assertStringContainsString( 'q=search', $url );
		$this->assertStringContainsString( 'sort=newest', $url );
		$this->assertStringContainsString( 'fq%5B%5D=site_key%3Asite-a', $url );
		$this->assertStringNotContainsString( 'type%3Apage', $url, 'no fixed filter, none sent' );

		$result = $page['results'][0];
		$this->assertSame( 'Workshop on search', $result['title'] );
		$this->assertSame( 'Post', $result['type'] );
		$this->assertSame( 'example.org › workshop', $result['crumb'], 'a result on another site names its domain' );
		$this->assertSame(
			array(
				array(
					'text' => 'About ',
					'mark' => false,
				),
				array(
					'text' => 'search',
					'mark' => true,
				),
				array(
					'text' => ' and facets.',
					'mark' => false,
				),
			),
			$result['snippet'],
			'the spaces between highlights kept'
		);
		$this->assertSame( 'About search and facets.', $result['snippetText'] );
		$this->assertSame( '7 results', $page['summary'] );
		$this->assertTrue( $page['hasResults'] );
		$this->assertTrue( $page['hasPages'] );
		$this->assertCount( 2, $page['pages'], 'the page links, not Next' );
		$this->assertSame( array( '40' ), array_column( $page['facets'][0]['items'], 'label' ), 'a value with no results dropped' );
		$this->assertSame( 'seats:40', $page['facets'][0]['items'][0]['target']['filterQuery'] );
	}

	/**
	 * A type whose cards show a mapped date and image draws them on the first page (D305),
	 * with the card lines, and the card goes through viglet_dumont_result_card.
	 */
	public function test_the_first_page_draws_a_types_card_date_image_and_lines(): void {
		$options = array(
			'viglet_dumont_mapping' => array(
				'post' => array(
					'fields'     => array(
						array(
							'source'      => 'meta:inicio',
							'field'       => 'event_start',
							'type'        => 'DATE',
							'facet_label' => 'Starts',
							'card'        => true,
						),
						array(
							'source' => 'meta:capa',
							'field'  => 'cover',
							'type'   => 'STRING',
						),
					),
					'card_date'  => 'event_start',
					'card_image' => 'cover',
				),
			),
		);
		Functions\stubs(
			array(
				'wp_date'            => static fn ( $format, $ts ) => gmdate( 'Y-m-d', $ts ),
				'get_option'         => static fn ( $name, $fallback = false ) => $options[ $name ] ?? $fallback,
				'wp_unslash'         => static fn ( $v ) => $v,
				'wp_strip_all_tags'  => static fn ( $v ) => trim( strip_tags( $v ) ),
				'number_format_i18n' => static fn ( $n ) => (string) $n,
				'_n'                 => static fn ( $one, $many, $n ) => 1 === $n ? $one : $many,
			)
		);
		$answer = array(
			'response' => array( 'code' => 200 ),
			'body'     => json_encode(
				array(
					'queryContext' => array( 'count' => 1 ),
					'results'      => array(
						'document' => array(
							array(
								'fields' => array(
									'id'               => 'site-a:post:8',
									'title'            => 'Open day',
									'url'              => 'https://site.example.org/open-day/',
									'type'             => 'post',
									'publication_date' => '2026-01-01T00:00:00Z',
									'event_start'      => '2026-11-20T13:00:00Z',
									'image'            => 'https://site.example.org/thumb.jpg',
									'cover'            => 'https://site.example.org/cover.jpg',
								),
							),
						),
					),
				)
			),
		);
		Functions\when( 'wp_remote_request' )->alias(
			function ( string $url, array $args ) use ( $answer ) {
				$this->requests[] = array( $url, $args );
				return $answer;
			}
		);
		Filters\expectApplied( 'viglet_dumont_result_card' )->once()->andReturnUsing(
			static fn ( array $card ) => array_merge( $card, array( 'title' => 'Filtered' ) )
		);

		$result = $this->search()->first_page(
			array(
				'query'        => 'open',
				'perPage'      => 10,
				'sort'         => '',
				'typeLabels'   => array(),
				'cartLabel'    => '%s',
				'noneLabel'    => 'None',
				'fixedFilters' => array( 'type:post' ),
			)
		)['results'][0];

		$this->assertSame( '2026-11-20', $result['date'], 'the event starts, not the day it was posted' );
		$this->assertSame( 'https://site.example.org/cover.jpg', $result['image'] );
		$this->assertTrue( $result['hasFields'] );
		$this->assertSame( 'Starts', $result['fields'][0]['label'] );
		$this->assertSame( 'Filtered', $result['title'] );
		// The block's fixed filter (D306) right after the site's scope.
		$this->assertStringContainsString( 'fq%5B%5D=site_key%3Asite-a&fq%5B%5D=type%3Apost', $this->requests[0][0] );
	}

	/**
	 * D327: a record another source sent, typed ["Article","Thesis"], reads as Article: its
	 * badge is the label Fields gave Article, and its card date is the field chosen for Article.
	 * The same looks reach the browser as `cardTypes`.
	 */
	public function test_a_foreign_types_label_and_card_date_follow_its_first_value(): void {
		$options = array(
			'viglet_dumont_foreign_types' => array(
				'Article' => array(
					'label'      => 'Journal article',
					'card_date'  => 'defense_date',
					'card_image' => 'cover_url',
				),
				// Its second value configured too: never what the record reads as.
				'Thesis'  => array(
					'label'     => 'Thesis',
					'card_date' => 'issued',
				),
			),
		);
		Functions\stubs(
			array(
				'wp_date'            => static fn ( $format, $ts ) => gmdate( 'Y-m-d', $ts ),
				'get_option'         => static fn ( $name, $fallback = false ) => $options[ $name ] ?? $fallback,
				'wp_unslash'         => static fn ( $v ) => $v,
				'wp_strip_all_tags'  => static fn ( $v ) => trim( strip_tags( $v ) ),
				'number_format_i18n' => static fn ( $n ) => (string) $n,
				'_n'                 => static fn ( $one, $many, $n ) => 1 === $n ? $one : $many,
			)
		);
		Functions\when( 'wp_remote_request' )->alias(
			function ( string $url, array $args ) {
				$this->requests[] = array( $url, $args );
				return array(
					'response' => array( 'code' => 200 ),
					'body'     => json_encode(
						array(
							'queryContext' => array( 'count' => 1 ),
							'results'      => array(
								'document' => array(
									array(
										'fields' => array(
											'id'           => 'oai:repo.example:1',
											'title'        => 'Tidal marshes',
											'url'          => 'https://repo.example/record/1',
											'type'         => array( 'Article', 'Thesis' ),
											'publication_date' => '2020-01-01T00:00:00Z',
											'defense_date' => '2024-03-15T00:00:00Z',
											'issued'       => '2019-05-05T00:00:00Z',
											'cover_url'    => 'https://repo.example/cover.png',
										),
									),
								),
							),
						)
					),
				);
			}
		);
		$mapping = new \Viglet_Dumont_Mapping( $this->settings() );
		$state   = array(
			'query'      => 'marshes',
			'perPage'    => 10,
			'sort'       => '',
			// As render.php gives it: the post types' names, then the foreign labels.
			'typeLabels' => array( 'post' => 'Post' ) + $mapping->foreign_labels(),
			'cartLabel'  => '%s',
			'noneLabel'  => 'None',
		);

		$result = $this->search()->first_page( $state )['results'][0];
		$this->assertSame( 'Journal article', $result['type'] );
		$this->assertSame( '2024-03-15', $result['date'], 'the date chosen for Article, not Thesis\'s nor the publication date' );
		$this->assertSame( 'https://repo.example/cover.png', $result['image'] );

		$preview = $this->search()->preview_state( 10, $state + array( 'errorLabel' => 'Error' ) )['results'][0];
		$this->assertSame( 'Journal article', $preview['type'] );
		$this->assertSame( '2024-03-15', $preview['date'] );

		$this->assertSame(
			array(
				'date'  => 'defense_date',
				'image' => 'cover_url',
			),
			$this->search()->public_config()['cardTypes']['Article']
		);
	}

	/**
	 * D263: the crumb drops this site's domain, and keeps another site's.
	 */
	public function test_crumb_names_the_host_only_for_another_site(): void {
		$this->assertSame( 'product › catbells', Viglet_Dumont_Native_Search::crumb( 'https://site.example.org/product/catbells/' ) );
		$this->assertSame( 'other.example › journal › hut', Viglet_Dumont_Native_Search::crumb( 'https://other.example/journal/hut/' ) );
		$this->assertSame( 'other.example:8081 › hut', Viglet_Dumont_Native_Search::crumb( 'http://other.example:8081/hut' ) );
	}
}
