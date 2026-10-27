<?php
/**
 * The per-post-type mapping: typed values, the manifest it provisions, and what stays out.
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Brain\Monkey\Filters;
use Brain\Monkey\Functions;
use Viglet_Dumont_API_Client;
use Viglet_Dumont_Indexer;
use Viglet_Dumont_Mapping;
use WP_Post;

final class MappingTest extends PluginTestCase {

	/** The options table, in memory. */
	private array $options = array();

	/** Post meta of post 42, by key; a list holds every value of a multi-valued key. */
	private array $meta = array();

	private WP_Post $post;

	protected function setUp(): void {
		parent::setUp();

		$this->post = new WP_Post(
			array(
				'ID'           => 42,
				'post_title'   => 'Workshop',
				'post_content' => 'Body',
				'post_type'    => 'evento',
				'post_excerpt' => 'Short',
			)
		);

		Functions\when( 'get_option' )->alias( fn ( $name, $fallback = false ) => $this->options[ $name ] ?? $fallback );
		Functions\when( 'update_option' )->alias(
			function ( $name, $value ) {
				$this->options[ $name ] = $value;
				return true;
			}
		);
		Functions\when( 'get_post_meta' )->alias(
			function ( $id, $key, $single = false ) {
				$value = $this->meta[ $key ] ?? ( $single ? '' : array() );
				if ( $single ) {
					return is_array( $value ) ? ( $value[0] ?? '' ) : $value;
				}
				return is_array( $value ) ? $value : array( $value );
			}
		);
		Functions\when( 'viglet_dumont_get_indexable_types' )->justReturn( array( 'post', 'evento' ) );
		Functions\when( 'wp_get_post_terms' )->alias(
			static fn ( $id, $taxonomy ) => 'area' === $taxonomy ? array( 'Culture', 'Education' ) : array()
		);
		Functions\stubs(
			array(
				'strip_shortcodes'           => static fn ( $text ) => $text,
				'wp_strip_all_tags'          => static fn ( $text ) => strip_tags( $text ),
				'wp_trim_words'              => static fn ( $text ) => $text,
				'get_permalink'              => 'https://site.example.org/workshop/',
				'get_post_time'              => '2026-10-01T09:00:00+00:00',
				'get_post_modified_time'     => '2026-10-02T09:00:00+00:00',
				'get_bloginfo'               => 'Institutional',
				'get_the_post_thumbnail_url' => false,
				'wp_get_post_categories'     => array(),
				'wp_get_post_tags'           => array(),
				'get_the_author_meta'        => 'Ana',
				'is_multisite'               => false,
				'wp_is_post_revision'        => false,
			)
		);
	}

	private function store( array $fields, bool $exclude = false ): void {
		$this->options[ Viglet_Dumont_Mapping::OPTION ] = array(
			'evento' => array(
				'exclude' => $exclude,
				'fields'  => $fields,
			),
		);
	}

	private function mapping(): Viglet_Dumont_Mapping {
		return new Viglet_Dumont_Mapping( $this->settings() );
	}

	public function test_mapped_sources_become_typed_values(): void {
		$this->store(
			array(
				array(
					'source' => 'meta:start_date',
					'field'  => 'start_date',
					'type'   => 'DATE',
				),
				array(
					'source' => 'meta:seats',
					'field'  => 'seats',
					'type'   => 'INT',
				),
				array(
					'source' => 'meta:open',
					'field'  => 'open',
					'type'   => 'BOOL',
				),
				array(
					'source' => 'taxonomy:area',
					'field'  => 'area',
					'type'   => 'STRING',
					'multi'  => true,
				),
				array(
					'source' => 'meta:speakers',
					'field'  => 'speakers',
					'type'   => 'STRING',
					'multi'  => true,
				),
				array(
					'source' => 'core:excerpt',
					'field'  => 'summary',
					'type'   => 'TEXT',
				),
			)
		);
		$this->meta = array(
			'start_date' => '20261130',
			'seats'      => '40',
			'open'       => '1',
			'speakers'   => array( 'Ana', 'Bruno' ),
		);

		$this->assertSame(
			array(
				'start_date' => '2026-11-30T00:00:00Z',
				'seats'      => 40,
				'open'       => true,
				'area'       => array( 'Culture', 'Education' ),
				'speakers'   => array( 'Ana', 'Bruno' ),
				'summary'    => 'Short',
			),
			$this->mapping()->values( $this->post )
		);
	}

	public function test_a_value_that_does_not_convert_is_left_out_rather_than_guessed(): void {
		$this->store(
			array(
				array(
					'source' => 'meta:start_date',
					'field'  => 'start_date',
					'type'   => 'DATE',
				),
				array(
					'source' => 'meta:seats',
					'field'  => 'seats',
					'type'   => 'INT',
				),
			)
		);
		$this->meta = array(
			'start_date' => 'soon',
			'seats'      => 'forty',
		);

		$this->assertSame( array(), $this->mapping()->values( $this->post ) );
	}

	public function test_an_invalid_or_reserved_field_is_dropped_from_the_mapping(): void {
		$this->store(
			array(
				array(
					'source' => 'meta:x',
					'field'  => 'title',
					'type'   => 'STRING',
				),
				array(
					'source' => 'unknown:x',
					'field'  => 'x',
					'type'   => 'STRING',
				),
				array(
					'source' => 'meta:x',
					'field'  => 'x',
					'type'   => 'GEO',
				),
				array(
					'source' => 'core:post_password',
					'field'  => 'pw',
					'type'   => 'STRING',
				),
			)
		);

		$this->assertSame( array(), $this->mapping()->all()['evento']['fields'] );
	}

	public function test_with_nothing_stored_a_public_custom_taxonomy_is_a_facet(): void {
		Functions\when( 'get_object_taxonomies' )->justReturn(
			array(
				(object) array(
					'name'   => 'area',
					'label'  => 'Areas',
					'public' => true,
				),
				(object) array(
					'name'   => 'category',
					'label'  => 'Categories',
					'public' => true,
				),
				(object) array(
					'name'   => 'internal',
					'label'  => 'Internal',
					'public' => false,
				),
			)
		);

		$fields = $this->mapping()->all()['evento']['fields'];

		$this->assertCount( 1, $fields );
		$this->assertSame( 'taxonomy:area', $fields[0]['source'] );
		$this->assertTrue( $fields[0]['multi'] );
		$this->assertTrue( $fields[0]['facet'] );
		$this->assertSame( 'Areas', $fields[0]['facet_label'] );
	}

	public function test_the_manifest_declares_each_field_once_with_its_facet_label_and_weight(): void {
		$this->options[ Viglet_Dumont_Mapping::OPTION ] = array(
			'post'   => array(
				'fields' => array(
					array(
						'source'      => 'taxonomy:area',
						'field'       => 'area',
						'type'        => 'STRING',
						'multi'       => true,
						'facet'       => true,
						'facet_label' => 'Área',
						'weight'      => 5,
					),
				),
			),
			'evento' => array(
				'fields' => array(
					array(
						'source' => 'meta:area',
						'field'  => 'area',
						'type'   => 'TEXT',
					),
				),
			),
		);

		$this->assertSame(
			array(
				'name'    => 'Sample Site',
				'locales' => array( 'pt_BR' ),
				'fields'  => array(
					array(
						'name'        => 'site_key',
						'type'        => 'STRING',
						'mandatory'   => false,
						'multiValued' => false,
						'facet'       => true,
						'facetName'   => array( 'pt_BR' => 'Site' ),
					),
					array(
						'name'        => 'sticky',
						'type'        => 'STRING',
						'mandatory'   => false,
						'multiValued' => false,
						'facet'       => false,
					),
					array(
						'name'         => 'area',
						'type'         => 'STRING',
						'mandatory'    => false,
						'multiValued'  => true,
						'facet'        => true,
						'facetName'    => array( 'pt_BR' => 'Área' ),
						'searchWeight' => 5.0,
					),
				),
			),
			$this->mapping()->manifest()
		);
	}

	public function test_a_date_or_number_field_with_ranges_declares_a_range_facet(): void {
		$this->store(
			array(
				array(
					'source'      => 'meta:start_date',
					'field'       => 'start_date',
					'type'        => 'DATE',
					'facet_label' => 'Date',
					'range'       => true,
				),
				array(
					'source' => 'meta:seats',
					'field'  => 'seats',
					'type'   => 'INT',
					'range'  => true,
					'ranges' => array(
						array(
							'label' => 'Small',
							'from'  => '0',
							'to'    => '20',
						),
						array(
							'from' => '20',
							'to'   => '50.5',
						),
						array( 'from' => '50' ),
						array( 'label' => 'Unbounded' ),
						array(
							'label' => 'Small',
							'to'    => '5',
						),
					),
				),
				array(
					'source' => 'meta:title_alt',
					'field'  => 'title_alt',
					'type'   => 'STRING',
					'range'  => true,
					'ranges' => array( array( 'from' => 1 ) ),
				),
			)
		);

		$facets = $this->mapping()->manifest()['customFacets'];

		$this->assertSame( array( 'start_date_range', 'seats_range' ), array_column( $facets, 'name' ) );
		$this->assertSame( 'start_date', $facets[0]['field'] );
		$this->assertSame( array( 'pt_BR' => 'Date' ), $facets[0]['label'] );
		$this->assertSame(
			array(
				'label'          => 'Last 7 days',
				'rangeStartDate' => 'NOW-7DAYS',
				'rangeEndDate'   => 'NOW',
			),
			$facets[0]['items'][1]
		);
		$this->assertSame(
			array(
				'label'        => 'Before this year',
				'rangeEndDate' => 'NOW/YEAR',
			),
			$facets[0]['items'][4]
		);
		// A range with no bound is dropped, one with no label is named after its bounds, and a
		// label already taken keeps the first range that had it.
		$this->assertSame(
			array(
				array(
					'label'      => 'Small',
					'rangeStart' => 0,
					'rangeEnd'   => 20,
				),
				array(
					'label'      => '20 – 50.5',
					'rangeStart' => 20,
					'rangeEnd'   => 50.5,
				),
				array(
					'label'      => '50 or more',
					'rangeStart' => 50,
				),
			),
			$facets[1]['items']
		);
		$this->assertSame( 'seats', $facets[1]['defaultLabel'] );
	}

	public function test_without_a_range_the_manifest_declares_no_custom_facet(): void {
		$this->store(
			array(
				array(
					'source' => 'meta:seats',
					'field'  => 'seats',
					'type'   => 'INT',
					'ranges' => array( array( 'from' => 1 ) ),
				),
				array(
					'source' => 'meta:size',
					'field'  => 'size',
					'type'   => 'INT',
					'range'  => true,
				),
			)
		);

		$this->assertArrayNotHasKey( 'customFacets', $this->mapping()->manifest() );
	}

	public function test_provisioning_posts_the_manifest_once_until_it_changes(): void {
		$this->store(
			array(
				array(
					'source' => 'meta:seats',
					'field'  => 'seats',
					'type'   => 'INT',
				),
			)
		);
		$client = new Viglet_Dumont_API_Client( $this->settings() );

		$first  = $this->mapping()->provision( $client );
		$second = $this->mapping()->provision( $client );

		$this->assertTrue( $first['sent'] );
		$this->assertFalse( $second['sent'] );
		$this->assertCount( 1, $this->requests );
		$this->assertSame( 'https://turing.example.org/api/sn/manifest', $this->requests[0][0] );
		$this->assertSame( array( 'site_key', 'sticky', 'seats' ), array_column( $this->body()['fields'], 'name' ) );

		$this->mapping()->provision( $client, true );
		$this->assertCount( 2, $this->requests );
	}

	public function test_a_refused_manifest_is_recorded_and_sent_again_next_time(): void {
		$this->status = 409;
		$this->store(
			array(
				array(
					'source' => 'meta:seats',
					'field'  => 'seats',
					'type'   => 'INT',
				),
			)
		);
		$client = new Viglet_Dumont_API_Client( $this->settings() );

		$result = $this->mapping()->provision( $client );

		$this->assertFalse( $result['success'] );
		$this->assertStringContainsString( 'type', $result['message'] );
		$this->assertNotSame( '', $this->options[ Viglet_Dumont_Mapping::PROVISIONED_OPTION ]['error'] );

		$this->mapping()->provision( $client );
		$this->assertCount( 2, $this->requests );
	}

	public function test_the_built_in_fields_are_provisioned_even_with_no_mapped_field(): void {
		$result = $this->mapping()->provision( new Viglet_Dumont_API_Client( $this->settings() ) );

		$this->assertTrue( $result['sent'] );
		$this->assertSame( array( 'site_key', 'sticky' ), array_column( $this->body()['fields'], 'name' ) );
	}

	public function test_the_plan_says_where_each_field_stands(): void {
		Functions\when( 'wp_remote_request' )->alias(
			function ( string $url, array $args ) {
				$this->requests[] = array( $url, $args );
				return array(
					'response' => array( 'code' => 200 ),
					'body'     => json_encode(
						array(
							'fieldsUnchanged' => array( 'site_key' ),
							'fieldsToAdd'     => array( 'seats' ),
							'breakingChanges' => array(
								array(
									'field'         => 'area',
									'attribute'     => 'type',
									'liveValue'     => 'TEXT',
									'manifestValue' => 'STRING',
								),
							),
						)
					),
				);
			}
		);

		$status = $this->mapping()->field_status( new Viglet_Dumont_API_Client( $this->settings() ) );

		$this->assertStringEndsWith( '/api/sn/manifest/plan', $this->requests[0][0] );
		$this->assertSame( 'provisioned', $status['site_key']['status'] );
		$this->assertSame( 'pending', $status['seats']['status'] );
		$this->assertSame( 'conflict', $status['area']['status'] );
		$this->assertSame( 'type is TEXT in Turing, STRING here', $status['area']['detail'] );
	}

	public function test_the_manifest_turns_generative_ai_on_only_when_the_setting_does(): void {
		$this->assertArrayNotHasKey( 'genAi', $this->mapping()->manifest() );

		$manifest = ( new Viglet_Dumont_Mapping( $this->settings( array( 'generative_ai' => 1 ) ) ) )->manifest();

		$this->assertEquals( new \stdClass(), $manifest['genAi'], 'no agent named: the site keeps the one it has' );
		$this->assertSame( '{}', wp_json_encode( $manifest['genAi'] ) );
	}

	public function test_turning_generative_ai_on_sends_the_manifest_again(): void {
		$client = new Viglet_Dumont_API_Client( $this->settings() );
		$this->mapping()->provision( $client );
		( new Viglet_Dumont_Mapping( $this->settings( array( 'generative_ai' => 1 ) ) ) )->provision( $client );

		$this->assertCount( 2, $this->requests );
		$this->assertStringContainsString( '"genAi":{}', $this->requests[1][1]['body'] );
	}

	public function test_the_plan_says_whether_the_site_s_generative_ai_changes(): void {
		Functions\when( 'wp_remote_request' )->justReturn(
			array(
				'response' => array( 'code' => 200 ),
				'body'     => '{"fieldsUnchanged":["site_key"],"genAiToChange":true}',
			)
		);

		$plan = $this->mapping()->plan_status( new Viglet_Dumont_API_Client( $this->settings() ) );

		$this->assertTrue( $plan['genai_to_change'] );
		$this->assertSame( 'provisioned', $plan['fields']['site_key']['status'] );
	}

	public function test_an_excluded_type_or_post_is_not_indexed(): void {
		$indexer = new Viglet_Dumont_Indexer( $this->settings() );
		$this->assertTrue( $indexer->should_index( $this->post ) );

		$this->meta[ Viglet_Dumont_Mapping::EXCLUDE_META ] = '1';
		$this->assertFalse( $indexer->should_index( $this->post ) );

		$this->meta = array();
		$this->store( array(), true );
		$this->assertFalse( ( new Viglet_Dumont_Indexer( $this->settings() ) )->should_index( $this->post ) );
	}

	public function test_the_document_carries_the_mapped_fields_and_passes_through_the_filter(): void {
		$this->store(
			array(
				array(
					'source' => 'taxonomy:area',
					'field'  => 'area',
					'type'   => 'STRING',
					'multi'  => true,
				),
			)
		);
		Filters\expectApplied( 'viglet_dumont_document' )->once()->andReturnUsing(
			static fn ( $attributes ) => array_merge( $attributes, array( 'origin' => 'theme' ) )
		);

		$item = ( new Viglet_Dumont_Indexer( $this->settings() ) )->build_create_item( $this->post );

		$this->assertSame( array( 'Culture', 'Education' ), $item['attributes']['area'] );
		$this->assertSame( 'theme', $item['attributes']['origin'] );
	}

	public function test_hidden_ui_text_is_stripped_from_the_body(): void {
		$html = '<p>Visible <span class="screen-reader-text">opens in a new tab</span>text.</p>'
			. '<div hidden>Draft note</div><span aria-hidden="true">★</span>'
			. '<button>Share</button><svg><title>Icon</title></svg><p>Ção</p>';

		$this->assertSame(
			'Visible text.Ção',
			strip_tags( Viglet_Dumont_Indexer::strip_non_content( $html ) )
		);
	}
}
