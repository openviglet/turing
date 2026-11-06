<?php
/**
 * The people, organisations and places Turing finds in a post (D310): the opt-in in the
 * manifest, the facets it declares and offers, the name variants sent to Turing's Thesaurus,
 * and the values the editor's Search panel shows.
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Brain\Monkey\Functions;
use Viglet_Dumont_API_Client;
use Viglet_Dumont_Entities;
use Viglet_Dumont_Mapping;
use Viglet_Dumont_Native_Search;
use Viglet_Dumont_Post_State;
use WP_Post;

final class EntitiesTest extends PluginTestCase {

	/** The options table, in memory. */
	private array $options = array();

	/** Each Turing path fragment and the JSON it answers; anything else is a 404. */
	private array $bodies = array();

	protected function setUp(): void {
		parent::setUp();

		Functions\when( 'get_option' )->alias( fn ( $name, $fallback = false ) => $this->options[ $name ] ?? $fallback );
		Functions\when( 'update_option' )->alias(
			function ( $name, $value ) {
				$this->options[ $name ] = $value;
				return true;
			}
		);
		Functions\stubs(
			array(
				'wp_strip_all_tags' => static fn ( $text ) => strip_tags( $text ),
				'get_transient'     => false,
				'set_transient'     => true,
				'esc_url_raw'       => static fn ( $url ) => $url,
			)
		);
		Functions\when( 'wp_remote_request' )->alias(
			function ( string $url, array $args ) {
				$this->requests[] = array( $url, $args );
				$path             = (string) parse_url( $url, PHP_URL_PATH );
				$method           = $args['method'];
				foreach ( $this->bodies as $key => $body ) {
					[ $want_method, $want_path ] = explode( ' ', $key, 2 );
					if ( $want_method === $method && preg_match( '#^' . $want_path . '$#', $path ) ) {
						return array(
							'response' => array( 'code' => 200 ),
							'body'     => $body,
						);
					}
				}
				return array(
					'response' => array( 'code' => 404 ),
					'body'     => '',
				);
			}
		);
	}

	/** Settings with generative AI and entity extraction as given. */
	private function with( int $generative_ai, int $entities ): array {
		return $this->settings(
			array(
				'generative_ai'     => $generative_ai,
				'entity_extraction' => $entities,
				'site_key'          => 'site-a',
			)
		);
	}

	/** The manifest's fields, by name. */
	private static function fields( array $manifest ): array {
		return array_column( $manifest['fields'], null, 'name' );
	}

	public function test_the_manifest_asks_for_entities_only_when_opted_in_with_generative_ai(): void {
		$off = ( new Viglet_Dumont_Mapping( $this->with( 1, 0 ) ) )->manifest();
		$this->assertSame( '{}', wp_json_encode( $off['genAi'] ), 'not opted in: no entityExtraction' );
		$this->assertArrayNotHasKey( 'entity_person', self::fields( $off ) );

		$on = ( new Viglet_Dumont_Mapping( $this->with( 1, 1 ) ) )->manifest();
		$this->assertSame( array( 'entityExtraction' => true ), $on['genAi'] );

		// The names are found by the GenAI agent's LLM: without generative AI nothing is asked.
		$alone = ( new Viglet_Dumont_Mapping( $this->with( 0, 1 ) ) )->manifest();
		$this->assertArrayNotHasKey( 'genAi', $alone );
		$this->assertArrayNotHasKey( 'entity_person', self::fields( $alone ) );
		$this->assertFalse( Viglet_Dumont_Entities::extracts( $this->with( 0, 1 ) ) );
	}

	public function test_the_entity_fields_are_declared_as_turing_creates_them_with_their_labels(): void {
		$this->options[ Viglet_Dumont_Entities::OPTION ] = array(
			'person' => array(
				'label'   => 'Pessoas',
				'offered' => true,
			),
			'place'  => array(
				'label'   => '',
				'offered' => false,
			),
		);

		$fields = self::fields( ( new Viglet_Dumont_Mapping( $this->with( 1, 1 ) ) )->manifest() );

		$this->assertSame(
			array(
				'name'        => 'entity_person',
				'type'        => 'STRING',
				'mandatory'   => false,
				'multiValued' => true,
				'facet'       => true,
				'facetName'   => array( 'pt_BR' => 'Pessoas' ),
			),
			$fields['entity_person']
		);
		$this->assertSame( array( 'pt_BR' => 'Organisations' ), $fields['entity_organization']['facetName'], 'no label: the default' );
		$this->assertFalse( $fields['entity_place']['facet'], 'not offered: no facet' );
		$this->assertTrue( $fields['entity_place']['multiValued'], 'same cardinality as Turing, so never a breaking change' );
	}

	public function test_a_mapped_field_cannot_take_an_entity_field_s_name(): void {
		$this->options[ Viglet_Dumont_Mapping::OPTION ] = array(
			'post' => array(
				'fields' => array(
					array(
						'source' => 'taxonomy:city',
						'field'  => 'entity_place',
						'type'   => 'TEXT',
					),
				),
			),
		);

		$this->assertSame( array(), ( new Viglet_Dumont_Mapping( $this->with( 1, 1 ) ) )->all()['post']['fields'] );
	}

	public function test_turning_it_off_sends_false_once_after_a_manifest_that_turned_it_on(): void {
		$client                                = new Viglet_Dumont_API_Client( $this->with( 1, 1 ) );
		$this->bodies['POST /api/sn/manifest'] = '{}';

		( new Viglet_Dumont_Mapping( $this->with( 1, 1 ) ) )->provision( $client );
		$this->assertTrue( $this->options[ Viglet_Dumont_Mapping::PROVISIONED_OPTION ]['entities'] );

		( new Viglet_Dumont_Mapping( $this->with( 1, 0 ) ) )->provision( $client );
		$this->assertStringContainsString( '"genAi":{"entityExtraction":false}', $this->requests[1][1]['body'] );
		$this->assertFalse( $this->options[ Viglet_Dumont_Mapping::PROVISIONED_OPTION ]['entities'] );

		$this->assertSame( '{}', wp_json_encode( ( new Viglet_Dumont_Mapping( $this->with( 1, 0 ) ) )->manifest()['genAi'] ) );
	}

	public function test_the_offered_entity_facets_reach_the_public_config_with_their_labels(): void {
		$this->options[ Viglet_Dumont_Entities::OPTION ] = array(
			'person'       => array( 'label' => 'Pessoas' ),
			'organization' => array( 'offered' => false ),
		);

		$facets = array_column( ( new Viglet_Dumont_Native_Search( $this->with( 1, 1 ) ) )->public_config()['facets'], null, 'field' );

		$this->assertSame(
			array(
				'field' => 'entity_person',
				'label' => 'Pessoas',
				'type'  => 'STRING',
				'multi' => true,
			),
			$facets['entity_person']
		);
		$this->assertSame( 'Places', $facets['entity_place']['label'] );
		$this->assertArrayNotHasKey( 'entity_organization', $facets );

		$off = ( new Viglet_Dumont_Native_Search( $this->with( 1, 0 ) ) )->public_config()['facets'];
		$this->assertSame( array(), array_intersect( array_column( $off, 'field' ), Viglet_Dumont_Entities::FIELDS ) );
	}

	public function test_name_variants_are_read_from_lines_and_sanitized(): void {
		$text = "Marta Vilar | Vilar, Marta; M. Vilar; marta vilar; Vilar, Marta\n"
			. "  <b>Ilha do Norte</b>  \n"
			. "\n"
			. "marta vilar | Another line for the same name\n"
			. 'Fundação Aurora | Aurora; Fundação Aurora';

		$this->assertSame(
			array(
				array(
					'name'     => 'Marta Vilar',
					'variants' => array( 'Vilar, Marta', 'M. Vilar' ),
				),
				array(
					'name'     => 'Ilha do Norte',
					'variants' => array(),
				),
				array(
					'name'     => 'Fundação Aurora',
					'variants' => array( 'Aurora' ),
				),
			),
			Viglet_Dumont_Entities::variants( $text )
		);
		$this->assertSame(
			"Marta Vilar | Vilar, Marta; M. Vilar\nIlha do Norte\nFundação Aurora | Aurora",
			Viglet_Dumont_Entities::variants_text( Viglet_Dumont_Entities::variants( $text ) )
		);
	}

	public function test_name_variants_go_to_a_thesaurus_of_the_plugin_s_own_selected_for_the_site(): void {
		$this->bodies = array(
			'GET /api/sn'                                => '[{"id":"sn-1","name":"Sample Site"}]',
			'GET /api/kb'                                => '[{"id":"kb-other","name":"Education"}]',
			'POST /api/kb'                               => '{"id":"kb-1"}',
			'GET /api/kb/kb-1/microthesaurus'            => '[]',
			'POST /api/kb/kb-1/microthesaurus'           => '{"id":"mt-1"}',
			'GET /api/kb/microthesaurus/mt-1/term'       => '[{"id":"t-old","label":"Gone Name","variations":[]},{"id":"t-same","label":"Ilha do Norte","variations":[]}]',
			'POST /api/kb/microthesaurus/mt-1/term'      => '{"id":"t-new"}',
			'DELETE /api/kb/microthesaurus/mt-1/term/.+' => 'true',
			'GET /api/sn/sn-1/microthesaurus'            => '[]',
			'POST /api/sn/sn-1/microthesaurus'           => '{"id":"sel-1"}',
		);
		$variants     = Viglet_Dumont_Entities::variants( "Marta Vilar | Vilar, Marta\nIlha do Norte" );

		$result = ( new Viglet_Dumont_Entities( $this->with( 1, 1 ) ) )->sync_variants( new Viglet_Dumont_API_Client( $this->with( 1, 1 ) ), $variants );

		$this->assertTrue( $result['success'], $result['message'] );
		$sent = array();
		foreach ( $this->requests as $request ) {
			$sent[ $request[1]['method'] . ' ' . parse_url( $request[0], PHP_URL_PATH ) ] = json_decode( $request[1]['body'] ?? 'null', true );
		}
		$this->assertSame( 'WordPress name variants: site-a', $sent['POST /api/kb']['name'] );
		$this->assertSame( 'pt_BR', $sent['POST /api/kb/kb-1/microthesaurus']['language'] );
		$term = $sent['POST /api/kb/microthesaurus/mt-1/term'];
		$this->assertSame( 'Marta Vilar', $term['label'] );
		$this->assertSame( 'Vilar, Marta', $term['variations'][0]['surfaceForm'] );
		$this->assertFalse( $term['variations'][0]['caseSensitive'] );
		$this->assertArrayHasKey( 'DELETE /api/kb/microthesaurus/mt-1/term/t-old', $sent, 'a name no longer listed goes' );
		$this->assertArrayNotHasKey( 'PUT /api/kb/microthesaurus/mt-1/term/t-same', $sent, 'an unchanged name is left alone' );
		$this->assertSame(
			array(
				'microthesaurusId' => 'mt-1',
				'enabled'          => true,
			),
			$sent['POST /api/sn/sn-1/microthesaurus']
		);
	}

	public function test_an_emptied_list_removes_the_plugin_s_thesaurus_from_turing(): void {
		// No empty tree is left in Turing's console for a list the editor cleared.
		$this->bodies = array(
			'GET /api/sn'                                => '[{"id":"sn-1","name":"Sample Site"}]',
			'GET /api/kb'                                => '[{"id":"kb-other","name":"Education"},{"id":"kb-1","name":"WordPress name variants: site-a"}]',
			'GET /api/kb/kb-1/microthesaurus'            => '[{"id":"mt-1","domain":"NAMES","language":"pt-BR"}]',
			'GET /api/kb/microthesaurus/mt-1/term'       => '[{"id":"t-1","label":"Marta Vilar","variations":[]}]',
			'DELETE /api/kb/microthesaurus/mt-1/term/.+' => 'true',
			'GET /api/sn/sn-1/microthesaurus'            => '[{"id":"sel-1","microthesaurusId":"mt-1","enabled":true}]',
			'DELETE /api/sn/sn-1/microthesaurus/sel-1'   => 'true',
			'DELETE /api/kb/kb-1'                        => 'true',
		);

		$result = ( new Viglet_Dumont_Entities( $this->with( 1, 1 ) ) )->sync_variants( new Viglet_Dumont_API_Client( $this->with( 1, 1 ) ), array() );

		$this->assertTrue( $result['success'], $result['message'] );
		$deleted = array_map(
			static fn ( $r ) => (string) parse_url( $r[0], PHP_URL_PATH ),
			array_values( array_filter( $this->requests, static fn ( $r ) => 'DELETE' === $r[1]['method'] ) )
		);
		$this->assertContains( '/api/sn/sn-1/microthesaurus/sel-1', $deleted );
		$this->assertContains( '/api/kb/kb-1', $deleted );
		$this->assertNotContains( '/api/kb/kb-other', $deleted, 'a thesaurus the plugin did not make stays' );
	}

	public function test_a_partitioned_key_is_named_as_why_the_variants_were_not_written(): void {
		$this->bodies = array( 'GET /api/sn' => '[{"id":"sn-1","name":"Sample Site"}]' );
		Functions\when( 'wp_remote_request' )->alias(
			static fn ( string $url ) => str_ends_with( (string) parse_url( $url, PHP_URL_PATH ), '/api/sn' )
				? array(
					'response' => array( 'code' => 200 ),
					'body'     => '[{"id":"sn-1","name":"Sample Site"}]',
				)
				: array(
					'response' => array( 'code' => 403 ),
					'body'     => '{"error":"This API key is bound to a partition of a shared site"}',
				)
		);

		$result = ( new Viglet_Dumont_Entities( $this->with( 1, 1 ) ) )->sync_variants( new Viglet_Dumont_API_Client( $this->with( 1, 1 ) ), Viglet_Dumont_Entities::variants( 'Marta Vilar | M. Vilar' ) );

		$this->assertFalse( $result['success'] );
		$this->assertStringContainsString( 'partition', $result['message'] );
	}

	public function test_saving_changed_variants_syncs_them_and_records_the_outcome(): void {
		Functions\stubs( array( 'viglet_dumont_get_settings' => $this->with( 1, 1 ) ) );
		$this->bodies = array( 'GET /api/sn' => '[]' );

		Viglet_Dumont_Entities::on_settings_saved( array( 'name_variants' => array() ), array( 'name_variants' => array() ) );
		$this->assertSame( array(), $this->requests, 'unchanged: nothing is sent' );

		Viglet_Dumont_Entities::on_settings_saved( array(), array( 'name_variants' => 'Marta Vilar | M. Vilar' ) );
		$this->assertSame( 1, $this->options[ Viglet_Dumont_Entities::SYNC_OPTION ]['names'] );
		$this->assertStringContainsString( 'Sample Site', $this->options[ Viglet_Dumont_Entities::SYNC_OPTION ]['error'], 'no such SN site' );
	}

	public function test_the_panel_state_carries_the_entities_turing_holds_for_the_post(): void {
		$this->bodies['GET /api/sn/Sample%20Site/search/document'] = json_encode(
			array(
				array(
					'id'            => 'site-a:post:42',
					'entity_person' => array( 'Marta Vilar' ),
					'entity_place'  => array( 'Ilha do Norte', 'Porto Claro' ),
				),
			)
		);
		$post  = new WP_Post(
			array(
				'ID'          => 42,
				'post_type'   => 'post',
				'post_status' => 'publish',
			)
		);
		$state = array(
			'state'  => 'indexed',
			'label'  => 'Indexed',
			'detail' => '',
		);

		$with = ( new Viglet_Dumont_Post_State( $this->with( 1, 1 ) ) )->with_entities( $post, $state );

		$this->assertStringContainsString( 'id=site-a%3Apost%3A42', $this->requests[0][0] );
		$this->assertSame(
			array(
				array(
					'kind'   => 'person',
					'field'  => 'entity_person',
					'label'  => 'People',
					'values' => array( 'Marta Vilar' ),
				),
				array(
					'kind'   => 'place',
					'field'  => 'entity_place',
					'label'  => 'Places',
					'values' => array( 'Ilha do Norte', 'Porto Claro' ),
				),
			),
			$with['entities']
		);

		$this->requests = array();
		$this->assertSame( $state, ( new Viglet_Dumont_Post_State( $this->with( 1, 0 ) ) )->with_entities( $post, $state ), 'not asked for: not read' );
		$this->assertSame( array(), $this->requests );
	}
}
