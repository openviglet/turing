<?php
/**
 * Viglet Turing Entities
 *
 * The people, organisations and places Turing finds in a post's text (D310): the opt-in that
 * asks for them, the three facets they fill, the name variants that fold into one name, and
 * what the editor's Search panel shows of them.
 *
 * @package Viglet_Turing
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Turing's entities at import (Turing T1228): with `genAi.entityExtraction` on, the site's AI
 * agent reads every document indexed and writes the names it holds to `entity_person`,
 * `entity_organization` and `entity_place`, each a multi-valued STRING facet Turing creates on
 * first use. A name the site's Thesaurus knows is written as its preferred term.
 *
 * The name variants are kept in a microthesaurus of the plugin's own, selected for the SN
 * site, which Turing's Thesaurus API (`/api/kb`, `/api/sn/<id>/microthesaurus`) writes with
 * the site's key when the key is not bound to a partition of a shared site.
 */
class Viglet_Turing_Entities {

	/** The option holding each entity facet's label and whether the search block offers it. */
	public const OPTION = 'viglet_turing_entities';

	/** The option recording the last name-variant sync: when, how many names, the error if any. */
	public const SYNC_OPTION = 'viglet_turing_name_variants_sync';

	/** Each kind of entity and the facet field Turing fills with it. */
	public const FIELDS = array(
		'person'       => 'entity_person',
		'organization' => 'entity_organization',
		'place'        => 'entity_place',
	);

	/** How many names the variants list keeps, and how many variants each. */
	public const MAX_NAMES = 200;

	/** How many variants one name keeps. */
	public const MAX_VARIANTS = 20;

	/** The microthesaurus domain the plugin's name variants are filed under. */
	private const DOMAIN = 'NAMES';

	/**
	 * The plugin settings.
	 *
	 * @var array
	 */
	private array $settings;

	/**
	 * Build over the given settings, or over the stored ones.
	 *
	 * @param array|null $settings The plugin settings; null reads them from the options table.
	 */
	public function __construct( ?array $settings = null ) {
		$this->settings = $settings ?? viglet_turing_get_settings();
	}

	/**
	 * Whether Turing is asked to find the entities: the setting is on, and so is generative AI,
	 * since the names are found by the LLM of the site's GenAI agent. Off by default, since
	 * every document indexed is an LLM call billed to the site.
	 *
	 * @param array $settings The plugin settings.
	 */
	public static function extracts( array $settings ): bool {
		return ! empty( $settings['entity_extraction'] ) && Viglet_Turing_Mapping::declares_generative_ai( $settings );
	}

	/**
	 * A kind's label when Fields gives it none.
	 *
	 * @param string $kind One of the FIELDS keys.
	 */
	public static function default_label( string $kind ): string {
		$labels = array(
			'person'       => __( 'People', 'viglet-turing-search' ),
			'organization' => __( 'Organisations', 'viglet-turing-search' ),
			'place'        => __( 'Places', 'viglet-turing-search' ),
		);

		return $labels[ $kind ] ?? $kind;
	}

	/**
	 * Each kind's facet as Fields set it: its label (empty for the default) and whether the
	 * search block offers it. Every kind is offered until Fields says otherwise.
	 *
	 * @return array<string, array{label: string, offered: bool}>
	 */
	public function facets(): array {
		return self::sanitize( get_option( self::OPTION, array() ) );
	}

	/**
	 * Store the entity facets, keeping only what validates. Returns what was stored.
	 *
	 * @param mixed $facets Keyed by kind, each `{label, offered}`.
	 * @return array<string, array{label: string, offered: bool}>
	 */
	public function save( $facets ): array {
		$clean = self::sanitize( $facets );
		update_option( self::OPTION, $clean, false );

		return $clean;
	}

	/**
	 * The entity facets validated: every kind present, an unknown one dropped.
	 *
	 * @param mixed $facets As stored or posted.
	 * @return array<string, array{label: string, offered: bool}>
	 */
	public static function sanitize( $facets ): array {
		$facets = is_array( $facets ) ? $facets : array();
		$clean  = array();
		foreach ( array_keys( self::FIELDS ) as $kind ) {
			$facet          = is_array( $facets[ $kind ] ?? null ) ? $facets[ $kind ] : array();
			$label          = $facet['label'] ?? '';
			$clean[ $kind ] = array(
				'label'   => sanitize_text_field( is_scalar( $label ) ? (string) $label : '' ),
				'offered' => ! array_key_exists( 'offered', $facet ) || ! empty( $facet['offered'] ),
			);
		}

		return $clean;
	}

	/**
	 * A kind's label as the search block shows it.
	 *
	 * @param string $kind One of the FIELDS keys.
	 */
	public function label( string $kind ): string {
		$label = $this->facets()[ $kind ]['label'] ?? '';

		return '' !== $label ? $label : self::default_label( $kind );
	}

	/**
	 * The three fields as the manifest declares them, while Turing is asked to find entities:
	 * exactly the spec Turing creates them with (several STRING values), so declaring them
	 * first never conflicts with it, plus the label and whether each is a facet at all.
	 *
	 * @param string $locale The site's locale.
	 * @return array<string, array>
	 */
	public function manifest_fields( string $locale ): array {
		if ( ! self::extracts( $this->settings ) ) {
			return array();
		}
		$fields = array();
		foreach ( $this->facets() as $kind => $facet ) {
			$name            = self::FIELDS[ $kind ];
			$fields[ $name ] = array(
				'name'        => $name,
				'type'        => 'STRING',
				'mandatory'   => false,
				'multiValued' => true,
				'facet'       => $facet['offered'],
				'facetName'   => array( $locale => $this->label( $kind ) ),
			);
		}

		return $fields;
	}

	/**
	 * The entity facets the search block offers, as public_config() lists facets.
	 *
	 * @return array<string, array{field: string, label: string, type: string, multi: bool}>
	 */
	public function public_facets(): array {
		if ( ! self::extracts( $this->settings ) ) {
			return array();
		}
		$facets = array();
		foreach ( $this->facets() as $kind => $facet ) {
			if ( ! $facet['offered'] ) {
				continue;
			}
			$facets[ self::FIELDS[ $kind ] ] = array(
				'field' => self::FIELDS[ $kind ],
				'label' => $this->label( $kind ),
				'type'  => 'STRING',
				'multi' => true,
			);
		}

		return $facets;
	}

	/**
	 * The entities Turing holds for one indexed document, each kind with its label and values,
	 * a kind with none left out. Read from the stored document, never guessed.
	 *
	 * @param Viglet_Turing_API_Client $client The client to read through.
	 * @param string                   $doc_id The document's id.
	 * @return array<int, array{kind: string, field: string, label: string, values: array<int, string>}>|WP_Error
	 */
	public function of_document( Viglet_Turing_API_Client $client, string $doc_id ) {
		$documents = $client->documents( array( $doc_id ) );
		if ( is_wp_error( $documents ) ) {
			return $documents;
		}
		$fields = is_array( $documents[0] ?? null ) ? $documents[0] : array();
		// Turing answers a document's stored fields at the top, or under `fields` in older answers.
		$fields   = is_array( $fields['fields'] ?? null ) ? $fields['fields'] : $fields;
		$entities = array();
		foreach ( self::FIELDS as $kind => $field ) {
			$values = array_values(
				array_unique(
					array_filter(
						array_map(
							static fn ( $value ) => is_scalar( $value ) ? trim( wp_strip_all_tags( (string) $value ) ) : '',
							(array) ( $fields[ $field ] ?? array() )
						),
						static fn ( $value ) => '' !== $value
					)
				)
			);
			if ( $values ) {
				$entities[] = array(
					'kind'   => $kind,
					'field'  => $field,
					'label'  => $this->label( $kind ),
					'values' => $values,
				);
			}
		}//end foreach

		return $entities;
	}

	/**
	 * The name variants, however they were written: the list the option holds, or the Settings
	 * text, one name per line as `Preferred name | variant; variant`. A line with no `|` is a
	 * name with no variant yet. Variants equal to the name, or repeated, are dropped; a name
	 * written twice keeps its first line.
	 *
	 * @param mixed $value The list, or its text.
	 * @return array<int, array{name: string, variants: array<int, string>}>
	 */
	public static function variants( $value ): array {
		if ( is_string( $value ) ) {
			$value = preg_split( '/\r\n|\r|\n/', $value );
		}
		$names = array();
		foreach ( is_array( $value ) ? $value : array() as $entry ) {
			if ( is_string( $entry ) ) {
				$parts = explode( '|', $entry, 2 );
				$entry = array(
					'name'     => $parts[0],
					'variants' => explode( ';', $parts[1] ?? '' ),
				);
			}
			if ( ! is_array( $entry ) ) {
				continue;
			}
			$name = self::clean_name( $entry['name'] ?? '' );
			$key  = mb_strtolower( $name );
			if ( '' === $name || isset( $names[ $key ] ) ) {
				continue;
			}
			$variants = array();
			foreach ( (array) ( $entry['variants'] ?? array() ) as $variant ) {
				$variant = self::clean_name( $variant );
				$folded  = mb_strtolower( $variant );
				if ( '' === $variant || $folded === $key || isset( $variants[ $folded ] ) ) {
					continue;
				}
				$variants[ $folded ] = $variant;
				if ( count( $variants ) >= self::MAX_VARIANTS ) {
					break;
				}
			}
			$names[ $key ] = array(
				'name'     => $name,
				'variants' => array_values( $variants ),
			);
			if ( count( $names ) >= self::MAX_NAMES ) {
				break;
			}
		}//end foreach

		return array_values( $names );
	}

	/**
	 * The name variants as the Settings form writes them.
	 *
	 * @param mixed $value The list, as variants() returns it.
	 */
	public static function variants_text( $value ): string {
		$lines = array();
		foreach ( self::variants( $value ) as $entry ) {
			$lines[] = $entry['variants'] ? $entry['name'] . ' | ' . implode( '; ', $entry['variants'] ) : $entry['name'];
		}

		return implode( "\n", $lines );
	}

	/**
	 * One name, as text on one line: no markup, no `|` or `;`, at most 200 characters.
	 *
	 * @param mixed $value The name as written.
	 */
	private static function clean_name( $value ): string {
		$value = sanitize_text_field( is_scalar( $value ) ? (string) $value : '' );
		$value = trim( (string) preg_replace( '/\s+/u', ' ', str_replace( array( '|', ';' ), ' ', $value ) ) );

		return mb_substr( $value, 0, 200 );
	}

	/**
	 * Sync the name variants once the settings saved changed them (`update_option` of the
	 * plugin's option), recording the outcome for the Settings screen.
	 *
	 * @param mixed $old_value The settings before.
	 * @param mixed $value     The settings saved.
	 */
	public static function on_settings_saved( $old_value, $value ): void {
		$before = self::variants( is_array( $old_value ) ? ( $old_value['name_variants'] ?? array() ) : array() );
		$after  = self::variants( is_array( $value ) ? ( $value['name_variants'] ?? array() ) : array() );
		if ( $before === $after || Viglet_Turing_Settings::demo() ) {
			return;
		}
		$settings = viglet_turing_get_settings();
		$result   = ( new self( $settings ) )->sync_variants( new Viglet_Turing_API_Client( $settings ), $after );
		update_option(
			self::SYNC_OPTION,
			array(
				'at'    => time(),
				'names' => count( $after ),
				'error' => $result['success'] ? '' : $result['message'],
			),
			false
		);
	}

	/**
	 * The thesaurus the variants live in, named after this site key, so each site of a shared
	 * SN site keeps its own and none replaces another's.
	 */
	public function thesaurus_name(): string {
		return sprintf( 'WordPress name variants: %s', ( new Viglet_Turing_Indexer( $this->settings ) )->site_key() );
	}

	/**
	 * Write the name variants to Turing's Thesaurus, so the names they list are written as their
	 * preferred name in the entity facets: a thesaurus and a microthesaurus (in the site's
	 * language) of the plugin's own, created when missing, one term per name with its variants
	 * as surface forms, and the microthesaurus selected for the SN site. Terms are added, changed
	 * and removed to match the list; an empty list drops the selection and the plugin's
	 * thesaurus with it.
	 *
	 * A key bound to a partition of a shared SN site cannot write the Thesaurus (Turing answers
	 * 403): the message says so.
	 *
	 * @param Viglet_Turing_API_Client $client   The client to write through.
	 * @param array                    $variants The names, as variants() returns them.
	 * @return array{success: bool, message: string}
	 */
	public function sync_variants( Viglet_Turing_API_Client $client, array $variants ): array {
		$site = ( new Viglet_Turing_Tuning( $this->settings ) )->site_id();
		if ( is_wp_error( $site ) ) {
			return self::failed( $site );
		}
		$micro = $this->microthesaurus( $client, array() !== $variants );
		if ( is_wp_error( $micro ) ) {
			return self::failed( $micro );
		}
		if ( null === $micro ) {
			// Nothing to write and nothing written before.
			return array(
				'success' => true,
				'message' => '',
			);
		}

		$terms = $client->call( 'GET', "/api/kb/microthesaurus/{$micro}/term" );
		if ( is_wp_error( $terms ) ) {
			return self::failed( $terms );
		}
		$held = array();
		foreach ( $terms as $term ) {
			if ( is_array( $term ) && isset( $term['id'], $term['label'] ) ) {
				$held[ mb_strtolower( (string) $term['label'] ) ] = $term;
			}
		}

		$locale = (string) ( $this->settings['locale'] ?? 'en_US' );
		foreach ( $variants as $order => $entry ) {
			$key  = mb_strtolower( $entry['name'] );
			$term = $this->term( $entry, $order, $locale, $micro );
			$was  = $held[ $key ] ?? null;
			unset( $held[ $key ] );
			if ( null === $was ) {
				$answer = $client->call( 'POST', "/api/kb/microthesaurus/{$micro}/term", $term );
			} elseif ( self::surface_forms( $was ) !== $entry['variants'] || (string) $was['label'] !== $entry['name'] ) {
				$answer = $client->call( 'PUT', "/api/kb/microthesaurus/{$micro}/term/" . rawurlencode( (string) $was['id'] ), $term );
			} else {
				continue;
			}
			if ( is_wp_error( $answer ) ) {
				return self::failed( $answer );
			}
		}
		foreach ( $held as $gone ) {
			$answer = $client->call( 'DELETE', "/api/kb/microthesaurus/{$micro}/term/" . rawurlencode( (string) $gone['id'] ) );
			if ( is_wp_error( $answer ) ) {
				return self::failed( $answer );
			}
		}

		$selected = $this->select( $client, $site, $micro, array() !== $variants );
		if ( is_wp_error( $selected ) ) {
			return self::failed( $selected );
		}
		// No names left: the plugin's thesaurus goes too, rather than staying in Turing's
		// console as an empty tree nobody made there.
		if ( array() === $variants ) {
			$gone = $this->drop_thesaurus( $client );
			if ( is_wp_error( $gone ) ) {
				return self::failed( $gone );
			}
		}

		return array(
			'success' => true,
			'message' => '',
		);
	}

	/**
	 * Delete the plugin's own thesaurus, found by its name, with what it holds.
	 *
	 * @param Viglet_Turing_API_Client $client The client.
	 * @return true|WP_Error
	 */
	private function drop_thesaurus( Viglet_Turing_API_Client $client ) {
		$all = $client->call( 'GET', '/api/kb' );
		if ( is_wp_error( $all ) ) {
			return $all;
		}
		foreach ( $all as $thesaurus ) {
			if ( is_array( $thesaurus ) && (string) ( $thesaurus['name'] ?? '' ) === $this->thesaurus_name() ) {
				$deleted = $client->call( 'DELETE', '/api/kb/' . rawurlencode( (string) $thesaurus['id'] ) );
				if ( is_wp_error( $deleted ) ) {
					return $deleted;
				}
			}
		}

		return true;
	}

	/**
	 * The plugin's microthesaurus for the site's language: its id, created with its thesaurus
	 * when missing and `$create`, or null when there is none and none is wanted.
	 *
	 * @param Viglet_Turing_API_Client $client The client.
	 * @param bool                     $create Whether to create what is missing.
	 * @return string|WP_Error|null
	 */
	private function microthesaurus( Viglet_Turing_API_Client $client, bool $create ) {
		$name   = $this->thesaurus_name();
		$locale = (string) ( $this->settings['locale'] ?? 'en_US' );
		$all    = $client->call( 'GET', '/api/kb' );
		if ( is_wp_error( $all ) ) {
			return $all;
		}
		$kb = null;
		foreach ( $all as $thesaurus ) {
			if ( is_array( $thesaurus ) && (string) ( $thesaurus['name'] ?? '' ) === $name ) {
				$kb = (string) $thesaurus['id'];
				break;
			}
		}
		if ( null === $kb ) {
			if ( ! $create ) {
				return null;
			}
			$made = $client->call(
				'POST',
				'/api/kb',
				array(
					'name'        => $name,
					'description' => 'Kept by the Viglet Turing Search plugin from its Settings: edits made here are replaced on its next save.',
					'source'      => 'USER',
				)
			);
			if ( is_wp_error( $made ) ) {
				return $made;
			}
			$kb = (string) ( $made['id'] ?? '' );
		}

		$micros = $client->call( 'GET', '/api/kb/' . rawurlencode( $kb ) . '/microthesaurus' );
		if ( is_wp_error( $micros ) ) {
			return $micros;
		}
		$language = self::language( $locale );
		foreach ( $micros as $micro ) {
			if ( is_array( $micro ) && self::DOMAIN === (string) ( $micro['domain'] ?? '' ) && self::language( (string) ( $micro['language'] ?? '' ) ) === $language ) {
				return (string) $micro['id'];
			}
		}
		if ( ! $create ) {
			return null;
		}
		$made = $client->call(
			'POST',
			'/api/kb/' . rawurlencode( $kb ) . '/microthesaurus',
			array(
				'name'        => $name,
				'description' => 'One term per name, its variants as surface forms.',
				'language'    => $locale,
				'domain'      => self::DOMAIN,
			)
		);
		if ( is_wp_error( $made ) ) {
			return $made;
		}

		return '' !== (string) ( $made['id'] ?? '' ) ? (string) $made['id'] : new WP_Error( 'viglet_turing_thesaurus', __( 'Viglet Turing did not create the thesaurus.', 'viglet-turing-search' ) );
	}

	/**
	 * A locale's language, as Turing matches a microthesaurus to a document: `pt` of `pt_BR`.
	 *
	 * @param string $locale The locale, `pt_BR` or `pt-BR`.
	 */
	private static function language( string $locale ): string {
		return strtolower( (string) preg_split( '/[_-]/', $locale )[0] );
	}

	/**
	 * Select the microthesaurus for the SN site (enabled), or drop the selection.
	 *
	 * @param Viglet_Turing_API_Client $client The client.
	 * @param string                   $site   The SN site's id.
	 * @param string                   $micro  The microthesaurus' id.
	 * @param bool                     $wanted Whether the site should use it.
	 * @return true|WP_Error
	 */
	private function select( Viglet_Turing_API_Client $client, string $site, string $micro, bool $wanted ) {
		$path       = '/api/sn/' . rawurlencode( $site ) . '/microthesaurus';
		$selections = $client->call( 'GET', $path );
		if ( is_wp_error( $selections ) ) {
			return $selections;
		}
		foreach ( $selections as $selection ) {
			if ( ! is_array( $selection ) || (string) ( $selection['microthesaurusId'] ?? '' ) !== $micro ) {
				continue;
			}
			$id = rawurlencode( (string) $selection['id'] );
			if ( ! $wanted ) {
				$answer = $client->call( 'DELETE', "{$path}/{$id}" );
			} elseif ( empty( $selection['enabled'] ) ) {
				$answer = $client->call( 'PUT', "{$path}/{$id}?enabled=true" );
			} else {
				return true;
			}

			return is_wp_error( $answer ) ? $answer : true;
		}
		if ( ! $wanted ) {
			return true;
		}
		$answer = $client->call(
			'POST',
			$path,
			array(
				'microthesaurusId' => $micro,
				'enabled'          => true,
			)
		);

		return is_wp_error( $answer ) ? $answer : true;
	}

	/**
	 * One name as a Thesaurus term: the preferred label, its variants as surface forms matched
	 * whatever their case and accents.
	 *
	 * @param array{name: string, variants: array<int, string>} $entry  The name.
	 * @param int                                               $order  Its place in the list.
	 * @param string                                            $locale The site's locale.
	 * @param string                                            $micro  The microthesaurus' id.
	 */
	private function term( array $entry, int $order, string $locale, string $micro ): array {
		return array(
			'label'            => $entry['name'],
			'enabled'          => true,
			'termOrder'        => $order,
			'microthesaurusId' => $micro,
			'variations'       => array_map(
				static fn ( string $variant ) => array(
					'surfaceForm'     => $variant,
					'weight'          => 100.0,
					'caseSensitive'   => false,
					'accentSensitive' => false,
					'language'        => $locale,
				),
				$entry['variants']
			),
			'relations'        => array(),
		);
	}

	/**
	 * A term's surface forms, in its order, as variants() lists them.
	 *
	 * @param array $term The term as Turing answered it.
	 * @return array<int, string>
	 */
	private static function surface_forms( array $term ): array {
		return array_values(
			array_map(
				static fn ( $variation ) => (string) ( $variation['surfaceForm'] ?? '' ),
				array_filter( (array) ( $term['variations'] ?? array() ), 'is_array' )
			)
		);
	}

	/**
	 * A failed sync, with a key bound to a partition named as the cause.
	 *
	 * @param WP_Error $error What Turing answered.
	 * @return array{success: bool, message: string}
	 */
	private static function failed( WP_Error $error ): array {
		$data = $error->get_error_data();
		if ( is_array( $data ) && 403 === (int) ( $data['status'] ?? 0 ) ) {
			return array(
				'success' => false,
				'message' => __( 'Viglet Turing refused to write the name variants (HTTP 403). An API key bound to a partition of a shared SN site cannot write the Thesaurus; save them with a key that is not, or add them in Viglet Turing\'s Thesaurus.', 'viglet-turing-search' ),
			);
		}

		return array(
			'success' => false,
			'message' => $error->get_error_message(),
		);
	}
}
