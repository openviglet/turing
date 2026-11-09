<?php
/**
 * Viglet Turing Admin
 *
 * Admin settings page and AJAX handlers.
 *
 * @package Viglet_Turing
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * The settings page under wp-admin and the assets it loads.
 */
class Viglet_Turing_Admin {

	/**
	 * Hook the menu entry, the settings registration and the assets.
	 */
	public function __construct() {
		add_action( 'admin_menu', array( $this, 'add_menu_page' ) );
		add_action( 'admin_init', array( $this, 'register_settings' ) );
		add_action( 'admin_enqueue_scripts', array( $this, 'enqueue_assets' ) );
		add_action( 'wp_dashboard_setup', array( $this, 'add_dashboard_widget' ) );
		add_action( 'admin_notices', array( $this, 'demo_notice' ) );
		add_action( 'admin_notices', array( $this, 'stopped_notice' ) );
	}

	/**
	 * While queued edits have stopped retrying (D329), say so on every admin screen but Health
	 * itself, for whoever can act on it: an edit that never reaches search is otherwise silent.
	 * The queue replays them on its own once Turing answers again.
	 */
	public function stopped_notice(): void {
		if ( ! current_user_can( 'manage_options' ) || Viglet_Turing_Settings::demo() ) {
			return;
		}
		$screen = function_exists( 'get_current_screen' ) ? get_current_screen() : null;
		if ( $screen && str_contains( (string) $screen->id, 'viglet-turing-health' ) ) {
			return;
		}
		$failed = ( new Viglet_Turing_Queue() )->counts()['failed'];
		if ( $failed < 1 ) {
			return;
		}
		printf(
			'<div class="notice notice-warning"><p>%s <a href="%s">%s</a></p></div>',
			esc_html(
				sprintf(
					/* translators: %d: queued edits that stopped retrying. */
					_n(
						'%d edit has not reached search: Viglet Turing did not answer, and it stopped retrying. It is sent again on its own once Viglet Turing answers.',
						'%d edits have not reached search: Viglet Turing did not answer, and they stopped retrying. They are sent again on their own once Viglet Turing answers.',
						$failed,
						'viglet-turing-search'
					),
					$failed
				)
			),
			esc_url( admin_url( 'admin.php?page=viglet-turing-health' ) ),
			esc_html__( 'Search health', 'viglet-turing-search' )
		);
	}

	/**
	 * On the plugin's own screens, say that demo mode is on and what it turns off (D238), so an
	 * indexing button that does nothing is never a mystery.
	 */
	public function demo_notice(): void {
		$screen = function_exists( 'get_current_screen' ) ? get_current_screen() : null;
		if ( ! Viglet_Turing_Settings::demo() || ! $screen || ! str_contains( (string) $screen->id, 'viglet-turing' ) ) {
			return;
		}
		printf(
			'<div class="notice notice-info"><p>%s</p></div>',
			esc_html( Viglet_Turing_API_Client::demo_refusal()->get_error_message() . ' ' . __( 'Search and the AI features work from the visitors\' browsers; Search tuning and Search insights are hidden.', 'viglet-turing-search' ) )
		);
	}

	/**
	 * The week's searches on the dashboard (D178), for whoever may read Search insights.
	 */
	public function add_dashboard_widget(): void {
		// Insights read Turing's metrics, which need the key demo mode never holds (D238).
		if ( ! current_user_can( 'manage_options' ) || Viglet_Turing_Settings::demo() ) {
			return;
		}
		wp_add_dashboard_widget(
			'viglet_turing_insights',
			__( 'Site search this week', 'viglet-turing-search' ),
			array( new Viglet_Turing_Insights(), 'render_widget' )
		);
	}

	/**
	 * Register the admin menu page.
	 */
	public function add_menu_page(): void {
		add_menu_page(
			__( 'Viglet Turing Search', 'viglet-turing-search' ),
			__( 'Viglet Turing Search', 'viglet-turing-search' ),
			'manage_options',
			'viglet-turing',
			array( $this, 'render_settings_page' ),
			'dashicons-search',
			80
		);
		add_submenu_page(
			'viglet-turing',
			__( 'Settings', 'viglet-turing-search' ),
			__( 'Settings', 'viglet-turing-search' ),
			'manage_options',
			'viglet-turing',
			array( $this, 'render_settings_page' )
		);
		add_submenu_page(
			'viglet-turing',
			__( 'Connect to Viglet Turing', 'viglet-turing-search' ),
			__( 'Setup', 'viglet-turing-search' ),
			'manage_options',
			'viglet-turing-setup',
			array( $this, 'render_setup_page' )
		);
		add_submenu_page(
			'viglet-turing',
			__( 'Search fields', 'viglet-turing-search' ),
			__( 'Fields', 'viglet-turing-search' ),
			'manage_options',
			'viglet-turing-fields',
			array( $this, 'render_fields_page' )
		);
		add_submenu_page(
			'viglet-turing',
			__( 'Search health', 'viglet-turing-search' ),
			__( 'Health', 'viglet-turing-search' ),
			'manage_options',
			'viglet-turing-health',
			array( $this, 'render_health_page' )
		);
		// Tuning and Insights read and write through Turing's keyed endpoints: demo mode has none (D238).
		if ( Viglet_Turing_Settings::demo() ) {
			return;
		}
		add_submenu_page(
			'viglet-turing',
			__( 'Search tuning', 'viglet-turing-search' ),
			__( 'Tuning', 'viglet-turing-search' ),
			'manage_options',
			'viglet-turing-tuning',
			array( $this, 'render_tuning_page' )
		);
		add_submenu_page(
			'viglet-turing',
			__( 'Search insights', 'viglet-turing-search' ),
			__( 'Insights', 'viglet-turing-search' ),
			'manage_options',
			'viglet-turing-insights',
			array( $this, 'render_insights_page' )
		);
	}

	/**
	 * The Search insights page (D178): a mount point for the app in admin/build/insights.js.
	 */
	public function render_insights_page(): void {
		echo '<div class="wrap"><h1>' . esc_html__( 'Search insights', 'viglet-turing-search' ) . '</h1><div id="viglet-turing-insights"></div></div>';
	}

	/**
	 * The Search tuning page: a mount point for the app in admin/build/tuning.js.
	 */
	public function render_tuning_page(): void {
		echo '<div class="wrap"><h1>' . esc_html__( 'Search tuning', 'viglet-turing-search' ) . '</h1><div id="viglet-turing-tuning"></div></div>';
	}

	/**
	 * The Setup page: a mount point for the app in admin/build/setup.js.
	 */
	public function render_setup_page(): void {
		echo '<div class="wrap"><h1>' . esc_html__( 'Connect to Viglet Turing', 'viglet-turing-search' ) . '</h1><div id="viglet-turing-setup"></div></div>';
	}

	/**
	 * The Fields page: a mount point for the app in admin/build/mapping.js.
	 */
	public function render_fields_page(): void {
		echo '<div class="wrap"><h1>' . esc_html__( 'Search fields', 'viglet-turing-search' ) . '</h1><div id="viglet-turing-mapping"></div></div>';
	}

	/**
	 * The Health page: a mount point for the app in admin/build/health.js.
	 */
	public function render_health_page(): void {
		echo '<div class="wrap"><h1>' . esc_html__( 'Search health', 'viglet-turing-search' ) . '</h1><div id="viglet-turing-health"></div></div>';
	}

	/**
	 * Register settings with the WordPress Settings API.
	 */
	public function register_settings(): void {
		register_setting(
			'viglet_turing_settings_group',
			VIGLET_TURING_OPTION,
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
		$sanitized['site_key']   = Viglet_Turing_Indexer::sanitize_site_key( (string) ( $input['site_key'] ?? '' ) );
		// The sibling sites a search here also reads (D303), typed one per line.
		$sanitized['search_site_keys'] = Viglet_Turing_Settings::search_sites( $input['search_site_keys'] ?? '' );
		// The key is never printed back into the form, so an empty field means "keep the stored one".
		$stored                  = get_option( VIGLET_TURING_OPTION, array() );
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
		$sanitized['index_pdfs']          = ! empty( $input['index_pdfs'] ) ? 1 : 0;
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
		$sanitized['instant_overlay']     = ! empty( $input['instant_overlay'] ) ? 1 : 0;
		$sanitized['overlay_layout']      = 'immersive' === ( $input['overlay_layout'] ?? '' ) ? 'immersive' : 'list';
		$sanitized['overlay_answers']     = ! empty( $input['overlay_answers'] ) ? 1 : 0;
		$sanitized['overlay_explore']     = Viglet_Turing_Native_Search::sanitize_explore( $input['overlay_explore'] ?? '' );
		$sanitized['search_page']         = max( 0, (int) ( $input['search_page'] ?? 0 ) );
		$sanitized['search_url']          = Viglet_Turing_Native_Search::sanitize_search_url( (string) ( $input['search_url'] ?? '' ) );
		$sanitized['track_clicks']        = ! empty( $input['track_clicks'] ) ? 1 : 0;
		$sanitized['analytics_ga']        = ! empty( $input['analytics_ga'] ) ? 1 : 0;
		$sanitized['ga_measurement_id']   = Viglet_Turing_Native_Search::sanitize_measurement_id( (string) ( $input['ga_measurement_id'] ?? '' ) );
		$sanitized['related_after']       = array_values( array_intersect( (array) ( $input['related_after'] ?? array() ), get_post_types( array( 'public' => true ) ) ) );
		$sanitized['generative_ai']       = ! empty( $input['generative_ai'] ) ? 1 : 0;
		$sanitized['assistant_launcher']  = ! empty( $input['assistant_launcher'] ) ? 1 : 0;
		$sanitized['entity_extraction']   = ! empty( $input['entity_extraction'] ) ? 1 : 0;
		$sanitized['name_variants']       = Viglet_Turing_Entities::variants( (string) ( $input['name_variants'] ?? '' ) );
		$sanitized['trending_block']      = Viglet_Turing_Trending::lines( (string) ( $input['trending_block'] ?? '' ) );
		$sanitized['trending_fallback']   = Viglet_Turing_Trending::lines( (string) ( $input['trending_fallback'] ?? '' ) );
		// The chips follow the new lists at once, not after the hour they are kept.
		delete_transient( Viglet_Turing_Trending::CACHE );

		return Viglet_Turing_Settings::without_overridden( $sanitized );
	}

	/**
	 * Enqueue admin CSS and JS on our settings page only.
	 *
	 * @param string $hook The current admin page's hook suffix.
	 */
	public function enqueue_assets( string $hook ): void {
		// By the page's slug: WordPress prefixes a submenu's hook with its parent menu's title,
		// sanitized and translated, so the prefix changes with the plugin's name (D273).
		$builds = array(
			'viglet-turing-health'   => 'health',
			'viglet-turing-fields'   => 'mapping',
			'viglet-turing-setup'    => 'setup',
			'viglet-turing-tuning'   => 'tuning',
			'viglet-turing-insights' => 'insights',
		);
		$page   = preg_match( '/_page_(viglet-turing-[a-z]+)$/', $hook, $match ) ? $match[1] : '';
		if ( isset( $builds[ $page ] ) ) {
			$this->enqueue_build( $builds[ $page ] );
			return;
		}
		if ( 'toplevel_page_viglet-turing' !== $hook ) {
			return;
		}

		wp_enqueue_style(
			'viglet-turing-admin',
			VIGLET_TURING_URL . 'admin/css/admin.css',
			array(),
			VIGLET_TURING_VERSION
		);

		wp_enqueue_script(
			'viglet-turing-admin',
			VIGLET_TURING_URL . 'admin/js/admin.js',
			array( 'jquery' ),
			VIGLET_TURING_VERSION,
			true
		);

		wp_localize_script(
			'viglet-turing-admin',
			'vigletTuring',
			array(
				'restUrl' => rest_url( 'viglet-turing/v1/' ),
				'nonce'   => wp_create_nonce( 'wp_rest' ),
				'strings' => array(
					'indexing'        => __( 'Indexing...', 'viglet-turing-search' ),
					'complete'        => __( 'Complete!', 'viglet-turing-search' ),
					'error'           => __( 'Error', 'viglet-turing-search' ),
					'confirm_del'     => __( 'Are you sure you want to delete all indexed content? This cannot be undone.', 'viglet-turing-search' ),
					'deleting'        => __( 'Deleting...', 'viglet-turing-search' ),
					'deleted'         => __( 'All indexed content has been deleted.', 'viglet-turing-search' ),
					'pinging'         => __( 'Testing connection...', 'viglet-turing-search' ),
					'total'           => __( 'Total', 'viglet-turing-search' ),
					// Index All runs in the background (D331).
					/* translators: 1: posts done, 2: posts to do in all, 3: posts that failed. */
					'running'         => __( 'Running in the background, so you can leave this page: %1$d of %2$d posts done, %3$d failed.', 'viglet-turing-search' ),
					/* translators: 1: posts sent, 2: posts that failed. */
					'ended_done'      => __( 'Indexing complete: %1$d posts sent.', 'viglet-turing-search' ),
					/* translators: 1: posts sent, 2: posts that failed. */
					'ended_failed'    => __( 'The reindex ended with failures: %1$d sent, %2$d failed. Search health shows the errors.', 'viglet-turing-search' ),
					/* translators: 1: posts sent, 2: posts that failed. */
					'ended_cancelled' => __( 'The reindex was cancelled after %1$d posts sent.', 'viglet-turing-search' ),
					'health'          => __( 'Search health', 'viglet-turing-search' ),
					'health_url'      => admin_url( 'admin.php?page=viglet-turing-health' ),
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
		$asset_file = VIGLET_TURING_DIR . "admin/build/{$entry}.asset.php";
		if ( ! file_exists( $asset_file ) ) {
			add_action(
				'admin_notices',
				static function () {
					echo '<div class="notice notice-error"><p>' . esc_html__( 'The plugin\'s admin scripts are not built. Run npm ci && npm run build in the plugin\'s source folder, or install a release zip.', 'viglet-turing-search' ) . '</p></div>';
				}
			);
			return;
		}

		$asset = include $asset_file;
		wp_enqueue_script(
			"viglet-turing-{$entry}",
			VIGLET_TURING_URL . "admin/build/{$entry}.js",
			$asset['dependencies'],
			$asset['version'],
			true
		);
		wp_set_script_translations( "viglet-turing-{$entry}", 'viglet-turing-search', VIGLET_TURING_DIR . 'languages' );
		wp_enqueue_style( 'wp-components' );
	}

	/**
	 * Render the settings page.
	 */
	public function render_settings_page(): void {
		$settings   = viglet_turing_get_settings();
		$post_types = get_post_types(
			array(
				'public'   => true,
				'_builtin' => false,
			),
			'objects'
		);

		include VIGLET_TURING_DIR . 'admin/views/settings-page.php';
	}
}

new Viglet_Turing_Admin();
