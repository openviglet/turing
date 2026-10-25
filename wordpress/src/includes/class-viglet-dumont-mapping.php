<?php
/**
 * Viglet Dumont Mapping
 *
 * The per-post-type field mapping: which taxonomies, meta keys and core fields a post sends
 * beyond the fixed document, as which Turing field and type, and the manifest that tells
 * Turing about those fields so a facet needs no console step.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Reads the mapping, turns a post into typed values and provisions the fields.
 *
 * The stored shape (option `viglet_dumont_mapping`, also what the REST route exports and
 * imports) is keyed by post type:
 *
 *     { "evento": { "exclude": false, "fields": [
 *         { "source": "meta:start_date", "field": "start_date", "type": "DATE",
 *           "multi": false, "facet": true, "facet_label": "Date", "weight": null } ] } }
 *
 * A source is `taxonomy:<slug>`, `meta:<key>`, `core:<excerpt|slug|menu_order|parent>` or
 * `thumbnail`. With nothing stored, every public custom taxonomy of an indexed type is
 * mapped as a multi-valued STRING facet, so a custom taxonomy is searchable out of the box.
 */
class Viglet_Dumont_Mapping {

	/** The option the mapping lives in. */
	public const OPTION = 'viglet_dumont_mapping';

	/** The option recording the last manifest sent: its hash, when, and the error if any. */
	public const PROVISIONED_OPTION = 'viglet_dumont_provisioned';

	/** The post meta that keeps one post out of the index. */
	public const EXCLUDE_META = '_viglet_dumont_exclude';

	/** The field types Turing's manifest accepts and the plugin knows how to convert to. */
	public const TYPES = array( 'STRING', 'TEXT', 'INT', 'LONG', 'FLOAT', 'DOUBLE', 'DATE', 'BOOL' );

	/** The core sources a mapping may name. */
	private const CORE_SOURCES = array( 'excerpt', 'slug', 'menu_order', 'parent' );

	/** Fields the indexer always sends; a mapping cannot take their names. */
	private const RESERVED = array( 'id', 'title', 'text', 'abstract', 'url', 'type', 'author', 'publication_date', 'modification_date', 'site', 'image', 'categories', 'section', 'tags', 'comments', 'source_apps', 'site_key' );

	/**
	 * The plugin settings this mapping was built with.
	 *
	 * @var array
	 */
	private array $settings;

	/**
	 * The mapping, read once per instance.
	 *
	 * @var array|null
	 */
	private ?array $resolved = null;

	/**
	 * Build a mapping over the given settings, or over the stored ones.
	 *
	 * @param array|null $settings The plugin settings; null reads them from the options table.
	 */
	public function __construct( ?array $settings = null ) {
		$this->settings = $settings ?? viglet_dumont_get_settings();
	}

	/**
	 * The mapping of every indexed post type, after the `viglet_dumont_mapping` filter.
	 *
	 * @return array<string, array{exclude: bool, fields: array<int, array>}>
	 */
	public function all(): array {
		if ( null !== $this->resolved ) {
			return $this->resolved;
		}

		$stored  = get_option( self::OPTION, array() );
		$stored  = is_array( $stored ) ? $stored : array();
		$mapping = array();

		foreach ( viglet_dumont_get_indexable_types() as $post_type ) {
			$mapping[ $post_type ] = isset( $stored[ $post_type ] )
				? self::sanitize_type( $stored[ $post_type ] )
				: $this->default_for( $post_type );
		}

		/**
		 * Filters the field mapping, so a theme can map in code what the settings cannot.
		 *
		 * @param array $mapping  The mapping, keyed by post type.
		 * @param array $settings The plugin settings.
		 */
		$filtered = apply_filters( 'viglet_dumont_mapping', $mapping, $this->settings );

		$this->resolved = array_map( array( self::class, 'sanitize_type' ), $filtered );

		return $this->resolved;
	}

	/**
	 * Store a mapping, keeping only what validates. Returns what was stored.
	 *
	 * @param array $mapping The mapping, keyed by post type.
	 */
	public function save( array $mapping ): array {
		$clean = array();
		foreach ( $mapping as $post_type => $type_mapping ) {
			if ( is_array( $type_mapping ) ) {
				$clean[ sanitize_key( (string) $post_type ) ] = self::sanitize_type( $type_mapping );
			}
		}
		update_option( self::OPTION, $clean, false );
		$this->resolved = null;

		return $clean;
	}

	/**
	 * Whether a post type is switched off in the mapping.
	 *
	 * @param string $post_type The post type.
	 */
	public function excludes_type( string $post_type ): bool {
		return ! empty( $this->all()[ $post_type ]['exclude'] );
	}

	/**
	 * The mapped fields of a post, as typed values. A value that does not convert is left out.
	 *
	 * @param WP_Post $post The post.
	 * @return array<string, mixed>
	 */
	public function values( WP_Post $post ): array {
		$fields = $this->all()[ $post->post_type ]['fields'] ?? array();
		$values = array();

		foreach ( $fields as $field ) {
			$raw = $this->read( $post, $field['source'], $field['multi'] );
			$raw = is_array( $raw ) ? $raw : array( $raw );

			$converted = array();
			foreach ( $raw as $value ) {
				$value = self::convert( $value, $field['type'] );
				if ( null !== $value ) {
					$converted[] = $value;
				}
			}

			if ( empty( $converted ) ) {
				continue;
			}
			$values[ $field['field'] ] = $field['multi'] ? $converted : $converted[0];
		}

		return $values;
	}

	/**
	 * The manifest the mapped fields converge to, as `POST /api/sn/manifest` binds it.
	 *
	 * A field two post types declare differently is declared once, as the first type has it.
	 * `site_key` is always declared: delete-all filters on it, and with several sites in one SN
	 * site it is the facet that tells them apart.
	 *
	 * @return array{name: string, locales: array<int, string>, fields: array<int, array>}
	 */
	public function manifest(): array {
		$locale = $this->settings['locale'] ?? 'en_US';
		$fields = array(
			'site_key' => array(
				'name'        => 'site_key',
				'type'        => 'STRING',
				'mandatory'   => false,
				'multiValued' => false,
				'facet'       => true,
				'facetName'   => array( $locale => __( 'Site', 'viglet-dumont' ) ),
			),
		);

		foreach ( $this->all() as $type_mapping ) {
			if ( $type_mapping['exclude'] ) {
				continue;
			}
			foreach ( $type_mapping['fields'] as $field ) {
				if ( isset( $fields[ $field['field'] ] ) ) {
					continue;
				}
				$spec = array(
					'name'        => $field['field'],
					'type'        => $field['type'],
					'mandatory'   => false,
					'multiValued' => $field['multi'],
					'facet'       => $field['facet'],
				);
				if ( '' !== $field['facet_label'] ) {
					$spec['facetName'] = array( $locale => $field['facet_label'] );
				}
				if ( null !== $field['weight'] ) {
					$spec['searchWeight'] = $field['weight'];
				}
				$fields[ $field['field'] ] = $spec;
			}
		}//end foreach

		return array(
			'name'    => $this->settings['site_name'],
			'locales' => array( $locale ),
			'fields'  => array_values( $fields ),
		);
	}

	/**
	 * Send the manifest to Turing unless the same one already went through.
	 *
	 * Re-posting an unchanged manifest is a no-op on Turing's side too, but it is a request
	 * on every worker run, so the hash of the last accepted one is kept. A refusal (a 409 when
	 * a field's type would have to change) does not stop indexing: the documents still flow
	 * with the site's current schema, and the error is kept for the admin to show.
	 *
	 * @param Viglet_Dumont_API_Client $client The client to send it through.
	 * @param bool                     $force  Send even when the same manifest went through.
	 * @return array{sent: bool, success: bool, message: string}
	 */
	public function provision( Viglet_Dumont_API_Client $client, bool $force = false ): array {
		$manifest = $this->manifest();
		if ( empty( $manifest['fields'] ) ) {
			return array(
				'sent'    => false,
				'success' => true,
				'message' => __( 'No mapped field to provision.', 'viglet-dumont' ),
			);
		}

		$hash = md5( (string) wp_json_encode( array( $manifest, $client->get_server_url() ) ) );
		$last = get_option( self::PROVISIONED_OPTION, array() );
		if ( ! $force && is_array( $last ) && ( $last['hash'] ?? '' ) === $hash && '' === ( $last['error'] ?? '' ) ) {
			return array(
				'sent'    => false,
				'success' => true,
				'message' => __( 'The fields are already provisioned.', 'viglet-dumont' ),
			);
		}

		$result = $client->provision( $manifest );
		update_option(
			self::PROVISIONED_OPTION,
			array(
				'hash'  => $hash,
				'at'    => time(),
				'error' => $result['success'] ? '' : $result['message'],
			),
			false
		);

		return array(
			'sent'    => true,
			'success' => $result['success'],
			'message' => $result['message'],
		);
	}

	/**
	 * The mapping a post type gets when nothing is stored: its public custom taxonomies.
	 *
	 * @param string $post_type The post type.
	 * @return array{exclude: bool, fields: array<int, array>}
	 */
	private function default_for( string $post_type ): array {
		$fields = array();
		foreach ( get_object_taxonomies( $post_type, 'objects' ) as $taxonomy ) {
			if ( ! $taxonomy->public || in_array( $taxonomy->name, array( 'category', 'post_tag', 'post_format' ), true ) ) {
				continue;
			}
			$fields[] = array(
				'source'      => 'taxonomy:' . $taxonomy->name,
				'field'       => $taxonomy->name,
				'type'        => 'STRING',
				'multi'       => true,
				'facet'       => true,
				'facet_label' => (string) $taxonomy->label,
				'weight'      => null,
			);
		}

		return self::sanitize_type( array( 'fields' => $fields ) );
	}

	/**
	 * Read a source's raw value(s) from a post.
	 *
	 * @param WP_Post $post   The post.
	 * @param string  $source The source, e.g. `meta:start_date`.
	 * @param bool    $multi  Whether every value is wanted, or the first.
	 * @return mixed
	 */
	private function read( WP_Post $post, string $source, bool $multi ) {
		[ $kind, $name ] = array_pad( explode( ':', $source, 2 ), 2, '' );

		switch ( $kind ) {
			case 'taxonomy':
				$terms = wp_get_post_terms( $post->ID, $name, array( 'fields' => 'names' ) );
				return is_wp_error( $terms ) ? array() : $terms;
			case 'meta':
				return get_post_meta( $post->ID, $name, ! $multi );
			case 'thumbnail':
				$url = get_the_post_thumbnail_url( $post->ID, 'large' );
				return is_string( $url ) ? $url : null;
			case 'core':
				$core = array(
					'excerpt'    => $post->post_excerpt,
					'slug'       => $post->post_name,
					'menu_order' => $post->menu_order,
					'parent'     => $post->post_parent,
				);
				return $core[ $name ] ?? null;
		}

		return null;
	}

	/**
	 * Convert one raw value to its declared type; null when it does not convert or is empty.
	 *
	 * A value that does not parse is dropped rather than coerced: a date field holding "soon"
	 * sends nothing, never the epoch.
	 *
	 * @param mixed  $value The raw value.
	 * @param string $type  One of self::TYPES.
	 * @return string|int|float|bool|null
	 */
	public static function convert( $value, string $type ) {
		if ( null === $value || '' === $value || is_array( $value ) || is_object( $value ) ) {
			return null;
		}

		switch ( $type ) {
			case 'INT':
			case 'LONG':
				return is_numeric( $value ) && (string) (int) $value === (string) $value ? (int) $value : null;
			case 'FLOAT':
			case 'DOUBLE':
				return is_numeric( $value ) ? (float) $value : null;
			case 'BOOL':
				return filter_var( $value, FILTER_VALIDATE_BOOLEAN, FILTER_NULL_ON_FAILURE );
			case 'DATE':
				return self::convert_date( (string) $value );
		}

		$value = trim( (string) $value );

		return '' === $value ? null : $value;
	}

	/**
	 * A date as Turing reads it (UTC, ISO 8601), from the shapes WordPress meta holds it in.
	 *
	 * Eight digits are a `Ymd` date (how ACF stores a date picker), other digits a Unix
	 * timestamp, anything else what strtotime() reads.
	 *
	 * @param string $value The raw date.
	 */
	private static function convert_date( string $value ): ?string {
		$value = trim( $value );
		if ( preg_match( '/^\d{8}$/', $value ) ) {
			$date      = DateTimeImmutable::createFromFormat( '!Ymd', $value, new DateTimeZone( 'UTC' ) );
			$timestamp = $date ? $date->getTimestamp() : false;
		} elseif ( ctype_digit( $value ) ) {
			$timestamp = (int) $value;
		} else {
			$timestamp = strtotime( $value );
		}

		return false === $timestamp ? null : gmdate( 'Y-m-d\TH:i:s\Z', $timestamp );
	}

	/**
	 * One post type's mapping with every field validated; an invalid field is dropped.
	 *
	 * @param mixed $type_mapping The post type's mapping as stored or filtered.
	 * @return array{exclude: bool, fields: array<int, array>}
	 */
	private static function sanitize_type( $type_mapping ): array {
		$type_mapping = is_array( $type_mapping ) ? $type_mapping : array();
		$fields       = array();
		$names        = array();

		foreach ( (array) ( $type_mapping['fields'] ?? array() ) as $field ) {
			$field = self::sanitize_field( $field );
			if ( null !== $field && ! isset( $names[ $field['field'] ] ) ) {
				$names[ $field['field'] ] = true;
				$fields[]                 = $field;
			}
		}

		return array(
			'exclude' => ! empty( $type_mapping['exclude'] ),
			'fields'  => $fields,
		);
	}

	/**
	 * One field validated, or null when its source, name or type is not one the plugin knows.
	 *
	 * @param mixed $field The field as stored or filtered.
	 */
	private static function sanitize_field( $field ): ?array {
		if ( ! is_array( $field ) ) {
			return null;
		}

		$source          = trim( (string) ( $field['source'] ?? '' ) );
		[ $kind, $name ] = array_pad( explode( ':', $source, 2 ), 2, '' );
		$valid_source    = ( 'thumbnail' === $source )
			|| ( in_array( $kind, array( 'taxonomy', 'meta' ), true ) && '' !== $name )
			|| ( 'core' === $kind && in_array( $name, self::CORE_SOURCES, true ) );

		$target = preg_replace( '/[^A-Za-z0-9_]/', '', (string) ( $field['field'] ?? '' ) );
		$type   = strtoupper( (string) ( $field['type'] ?? 'STRING' ) );

		if ( ! $valid_source || '' === $target || in_array( $target, self::RESERVED, true ) || ! in_array( $type, self::TYPES, true ) ) {
			return null;
		}

		$weight = $field['weight'] ?? null;

		return array(
			'source'      => $source,
			'field'       => $target,
			'type'        => $type,
			'multi'       => ! empty( $field['multi'] ),
			'facet'       => ! empty( $field['facet'] ),
			'facet_label' => sanitize_text_field( (string) ( $field['facet_label'] ?? '' ) ),
			'weight'      => is_numeric( $weight ) ? max( 1.0, min( 100.0, (float) $weight ) ) : null,
		);
	}
}
