<?php
/**
 * Viglet Dumont Admin
 *
 * Admin settings page and AJAX handlers.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * The settings page under wp-admin and the assets it loads.
 */
class Viglet_Dumont_Admin {

	/**
	 * Hook the menu entry, the settings registration and the assets.
	 */
	public function __construct() {
		add_action( 'admin_menu', array( $this, 'add_menu_page' ) );
		add_action( 'admin_init', array( $this, 'register_settings' ) );
		add_action( 'admin_enqueue_scripts', array( $this, 'enqueue_assets' ) );
	}

	/**
	 * Register the admin menu page.
	 */
	public function add_menu_page(): void {
		add_menu_page(
			__( 'Viglet Dumont', 'viglet-dumont' ),
			__( 'Viglet Dumont', 'viglet-dumont' ),
			'manage_options',
			'viglet-dumont',
			array( $this, 'render_settings_page' ),
			'dashicons-search',
			80
		);
		add_submenu_page(
			'viglet-dumont',
			__( 'Settings', 'viglet-dumont' ),
			__( 'Settings', 'viglet-dumont' ),
			'manage_options',
			'viglet-dumont',
			array( $this, 'render_settings_page' )
		);
		add_submenu_page(
			'viglet-dumont',
			__( 'Connect to Turing', 'viglet-dumont' ),
			__( 'Setup', 'viglet-dumont' ),
			'manage_options',
			'viglet-dumont-setup',
			array( $this, 'render_setup_page' )
		);
		add_submenu_page(
			'viglet-dumont',
			__( 'Search fields', 'viglet-dumont' ),
			__( 'Fields', 'viglet-dumont' ),
			'manage_options',
			'viglet-dumont-fields',
			array( $this, 'render_fields_page' )
		);
		add_submenu_page(
			'viglet-dumont',
			__( 'Search health', 'viglet-dumont' ),
			__( 'Health', 'viglet-dumont' ),
			'manage_options',
			'viglet-dumont-health',
			array( $this, 'render_health_page' )
		);
	}

	/**
	 * The Setup page: a mount point for the app in admin/build/setup.js.
	 */
	public function render_setup_page(): void {
		echo '<div class="wrap"><h1>' . esc_html__( 'Connect to Turing', 'viglet-dumont' ) . '</h1><div id="viglet-dumont-setup"></div></div>';
	}

	/**
	 * The Fields page: a mount point for the app in admin/build/mapping.js.
	 */
	public function render_fields_page(): void {
		echo '<div class="wrap"><h1>' . esc_html__( 'Search fields', 'viglet-dumont' ) . '</h1><div id="viglet-dumont-mapping"></div></div>';
	}

	/**
	 * The Health page: a mount point for the app in admin/build/health.js.
	 */
	public function render_health_page(): void {
		echo '<div class="wrap"><h1>' . esc_html__( 'Search health', 'viglet-dumont' ) . '</h1><div id="viglet-dumont-health"></div></div>';
	}

	/**
	 * Register settings with the WordPress Settings API.
	 */
	public function register_settings(): void {
		register_setting(
			'viglet_dumont_settings_group',
			VIGLET_DUMONT_OPTION,
			array(
				'sanitize_callback' => array( $this, 'sanitize_settings' ),
			)
		);
	}

	/**
	 * Sanitize settings on save.
	 *
	 * @param mixed $input The submitted settings.
	 */
	public function sanitize_settings( $input ): array {
		$sanitized = array();

		$sanitized['server_url'] = esc_url_raw( $input['server_url'] ?? 'http://localhost:2700' );
		$sanitized['site_name']  = sanitize_text_field( $input['site_name'] ?? 'default' );
		$sanitized['site_key']   = Viglet_Dumont_Indexer::sanitize_site_key( (string) ( $input['site_key'] ?? '' ) );
		// The key is never printed back into the form, so an empty field means "keep the stored one".
		$stored                  = get_option( VIGLET_DUMONT_OPTION, array() );
		$typed_key               = sanitize_text_field( $input['api_key'] ?? '' );
		$sanitized['api_key']    = '' !== $typed_key ? $typed_key : (string) ( is_array( $stored ) ? ( $stored['api_key'] ?? '' ) : '' );
		$sanitized['locale']     = sanitize_text_field( $input['locale'] ?? 'en_US' );
		$sanitized['public_url'] = esc_url_raw( $input['public_url'] ?? '' );
		// phpcs:ignore WordPress.WP.CapitalPDangit.MisspelledInText -- a source_apps value Turing matches on, not prose.
		$sanitized['source_app']          = sanitize_text_field( $input['source_app'] ?? 'wordpress' );
		$sanitized['index_posts']         = ! empty( $input['index_posts'] ) ? 1 : 0;
		$sanitized['index_pages']         = ! empty( $input['index_pages'] ) ? 1 : 0;
		$sanitized['index_custom_types']  = array_map( 'sanitize_text_field', $input['index_custom_types'] ?? array() );
		$sanitized['index_comments']      = ! empty( $input['index_comments'] ) ? 1 : 0;
		$sanitized['index_custom_fields'] = sanitize_text_field( $input['index_custom_fields'] ?? '' );
		$sanitized['index_categories']    = ! empty( $input['index_categories'] ) ? 1 : 0;
		$sanitized['index_tags']          = ! empty( $input['index_tags'] ) ? 1 : 0;
		$sanitized['exclude_ids']         = sanitize_text_field( $input['exclude_ids'] ?? '' );
		$sanitized['auto_index']          = ! empty( $input['auto_index'] ) ? 1 : 0;
		$sanitized['auto_deindex']        = ! empty( $input['auto_deindex'] ) ? 1 : 0;
		$sanitized['batch_size']          = max( 10, min( 200, (int) ( $input['batch_size'] ?? 50 ) ) );
		$sanitized['search_enabled']      = ! empty( $input['search_enabled'] ) ? 1 : 0;
		$sanitized['search_mode']         = 'template' === ( $input['search_mode'] ?? '' ) ? 'template' : 'theme';
		$sanitized['results_per_page']    = max( 1, min( 100, (int) ( $input['results_per_page'] ?? 10 ) ) );

		return Viglet_Dumont_Settings::without_overridden( $sanitized );
	}

	/**
	 * Enqueue admin CSS and JS on our settings page only.
	 *
	 * @param string $hook The current admin page's hook suffix.
	 */
	public function enqueue_assets( string $hook ): void {
		$builds = array(
			'viglet-dumont_page_viglet-dumont-health' => 'health',
			'viglet-dumont_page_viglet-dumont-fields' => 'mapping',
			'viglet-dumont_page_viglet-dumont-setup'  => 'setup',
		);
		if ( isset( $builds[ $hook ] ) ) {
			$this->enqueue_build( $builds[ $hook ] );
			return;
		}
		if ( 'toplevel_page_viglet-dumont' !== $hook ) {
			return;
		}

		wp_enqueue_style(
			'viglet-dumont-admin',
			VIGLET_DUMONT_URL . 'admin/css/admin.css',
			array(),
			VIGLET_DUMONT_VERSION
		);

		wp_enqueue_script(
			'viglet-dumont-admin',
			VIGLET_DUMONT_URL . 'admin/js/admin.js',
			array( 'jquery' ),
			VIGLET_DUMONT_VERSION,
			true
		);

		wp_localize_script(
			'viglet-dumont-admin',
			'vigletDumont',
			array(
				'restUrl' => rest_url( 'viglet-dumont/v1/' ),
				'nonce'   => wp_create_nonce( 'wp_rest' ),
				'strings' => array(
					'indexing'    => __( 'Indexing...', 'viglet-dumont' ),
					'complete'    => __( 'Complete!', 'viglet-dumont' ),
					'error'       => __( 'Error', 'viglet-dumont' ),
					'confirm_del' => __( 'Are you sure you want to delete all indexed content? This cannot be undone.', 'viglet-dumont' ),
					'deleting'    => __( 'Deleting...', 'viglet-dumont' ),
					'deleted'     => __( 'All indexed content has been deleted.', 'viglet-dumont' ),
					'pinging'     => __( 'Testing connection...', 'viglet-dumont' ),
				),
			)
		);
	}

	/**
	 * Load one entry of the admin build (`npm run build`), with the dependencies wp-scripts
	 * wrote beside it. Without a build the page says how to make one instead of staying blank.
	 *
	 * @param string $entry The entry name, e.g. health.
	 */
	private function enqueue_build( string $entry ): void {
		$asset_file = VIGLET_DUMONT_DIR . "admin/build/{$entry}.asset.php";
		if ( ! file_exists( $asset_file ) ) {
			add_action(
				'admin_notices',
				static function () {
					echo '<div class="notice notice-error"><p>' . esc_html__( 'The plugin\'s admin scripts are not built. Run npm ci && npm run build in the plugin\'s source folder, or install a release zip.', 'viglet-dumont' ) . '</p></div>';
				}
			);
			return;
		}

		$asset = include $asset_file;
		wp_enqueue_script(
			"viglet-dumont-{$entry}",
			VIGLET_DUMONT_URL . "admin/build/{$entry}.js",
			$asset['dependencies'],
			$asset['version'],
			true
		);
		wp_set_script_translations( "viglet-dumont-{$entry}", 'viglet-dumont', VIGLET_DUMONT_DIR . 'languages' );
		wp_enqueue_style( 'wp-components' );
	}

	/**
	 * Render the settings page.
	 */
	public function render_settings_page(): void {
		$settings   = viglet_dumont_get_settings();
		$post_types = get_post_types(
			array(
				'public'   => true,
				'_builtin' => false,
			),
			'objects'
		);

		include VIGLET_DUMONT_DIR . 'admin/views/settings-page.php';
	}
}

new Viglet_Dumont_Admin();
