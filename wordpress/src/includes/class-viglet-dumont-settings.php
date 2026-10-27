<?php
/**
 * Viglet Dumont Settings
 *
 * The plugin's settings: the stored option under the defaults, with the connection values a
 * deploy can set from outside WordPress laid over them.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Reads and writes the settings, and says where each connection value came from.
 *
 * A value set by a constant (in wp-config.php) or an environment variable of the same name
 * wins over the option, is read-only in the admin, and is never written to the database:
 * a container configured from its environment needs no wp-admin step, and the API key
 * stays out of every database backup.
 */
class Viglet_Dumont_Settings {

	/** Each overridable setting and the constant or environment variable that sets it. */
	public const OVERRIDES = array(
		'server_url' => 'VIGLET_DUMONT_TURING_URL',
		'site_name'  => 'VIGLET_DUMONT_TURING_SITE',
		'api_key'    => 'VIGLET_DUMONT_TURING_API_KEY',
		'locale'     => 'VIGLET_DUMONT_TURING_LOCALE',
		'site_key'   => 'VIGLET_DUMONT_SITE_KEY',
		'public_url' => 'VIGLET_DUMONT_PUBLIC_URL',
	);

	/** The settings a fresh install starts from. */
	public const DEFAULTS = array(
		'server_url'          => 'http://localhost:2700',
		'site_name'           => 'default',
		'site_key'            => '',
		'api_key'             => '',
		'locale'              => 'en_US',
		// The URL the browser reaches Turing at, when it differs from server_url (the search blocks).
		'public_url'          => '',
		'source_app'          => 'wordpress',
		'index_posts'         => 1,
		'index_pages'         => 1,
		'index_custom_types'  => array(),
		'index_comments'      => 0,
		'index_custom_fields' => '',
		'index_categories'    => 1,
		'index_tags'          => 1,
		'exclude_ids'         => '',
		'auto_index'          => 1,
		'auto_deindex'        => 1,
		'batch_size'          => 50,
		// Opt-in: a fresh install leaves WordPress search alone until Turing is set up.
		'search_enabled'      => 0,
		// theme: Turing answers the query and the theme's search template renders it.
		// template: the plugin's own results page (classic themes, with facets).
		'search_mode'         => 'theme',
		'results_per_page'    => 10,
		// With Turing search on: Ctrl/⌘+K and the theme's search box open an instant search.
		'instant_overlay'     => 1,
		// The full search page the overlay sends Enter to; 0 finds the page holding the block.
		'search_page'         => 0,
		// Opt-in: which result a visitor opened, reported to Turing's click-through rates.
		'track_clicks'        => 0,
		// Opt-in: the search events, sent to the site's own Google Analytics (gtag or GTM).
		'analytics_ga'        => 0,
		// Stamped on each event as send_to; empty sends to whatever the tag is set up for.
		'ga_measurement_id'   => '',
		// The post types that get the Related content block after their content (D177).
		'related_after'       => array(),
		// Popular-search chips (D202): words never shown, and chips for a site with no history.
		'trending_block'      => array(),
		'trending_fallback'   => array(),
		// Opt-in: the manifest turns the site's generative AI on, which spends the owner's LLM budget.
		'generative_ai'       => 0,
	);

	/**
	 * The settings in effect: defaults, then the stored option, then the overrides.
	 */
	public static function get(): array {
		$settings = wp_parse_args( self::stored(), self::DEFAULTS );
		foreach ( self::OVERRIDES as $key => $name ) {
			$value = self::external( $name );
			if ( null !== $value ) {
				$settings[ $key ] = $value;
			}
		}

		return $settings;
	}

	/**
	 * Store settings. A value an override sets is not stored: the option keeps what it had.
	 *
	 * @param array $settings The settings to store.
	 */
	public static function update( array $settings ): bool {
		return update_option( VIGLET_DUMONT_OPTION, self::without_overridden( $settings ) );
	}

	/**
	 * The settings with every overridden value put back to what the option holds, for the
	 * settings form's sanitizer and for update().
	 *
	 * @param array $settings The settings about to be stored.
	 */
	public static function without_overridden( array $settings ): array {
		$stored = self::stored();
		foreach ( self::OVERRIDES as $key => $name ) {
			if ( null === self::external( $name ) ) {
				continue;
			}
			if ( array_key_exists( $key, $stored ) ) {
				$settings[ $key ] = $stored[ $key ];
			} else {
				unset( $settings[ $key ] );
			}
		}

		return $settings;
	}

	/**
	 * Where each overridable value comes from: `constant`, `env` or `option`.
	 *
	 * @return array<string, array{source: string, name: string}>
	 */
	public static function sources(): array {
		$sources = array();
		foreach ( self::OVERRIDES as $key => $name ) {
			if ( defined( $name ) && '' !== (string) constant( $name ) ) {
				$source = 'constant';
			} elseif ( '' !== (string) getenv( $name ) ) {
				$source = 'env';
			} else {
				$source = 'option';
			}
			$sources[ $key ] = array(
				'source' => $source,
				'name'   => $name,
			);
		}

		return $sources;
	}

	/**
	 * Whether a setting is set from outside WordPress, so the admin shows it read-only.
	 *
	 * @param string $key The setting.
	 */
	public static function is_overridden( string $key ): bool {
		return isset( self::OVERRIDES[ $key ] ) && null !== self::external( self::OVERRIDES[ $key ] );
	}

	/**
	 * A value from a constant, else an environment variable; null when neither is set.
	 *
	 * @param string $name The constant's and the variable's name.
	 */
	private static function external( string $name ): ?string {
		if ( defined( $name ) && '' !== (string) constant( $name ) ) {
			return (string) constant( $name );
		}
		$env = getenv( $name );

		return false !== $env && '' !== $env ? $env : null;
	}

	/**
	 * The option as stored.
	 */
	private static function stored(): array {
		$stored = get_option( VIGLET_DUMONT_OPTION, array() );

		return is_array( $stored ) ? $stored : array();
	}
}
