<?php
/**
 * Viglet Dumont Admin
 *
 * Admin settings page and AJAX handlers.
 */

if ( ! defined( 'ABSPATH' ) ) {
    exit;
}

class Viglet_Dumont_Admin {

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
    }

    /**
     * Register settings with the WordPress Settings API.
     */
    public function register_settings(): void {
        register_setting( 'viglet_dumont_settings_group', VIGLET_DUMONT_OPTION, array(
            'sanitize_callback' => array( $this, 'sanitize_settings' ),
        ) );
    }

    /**
     * Sanitize settings on save.
     */
    public function sanitize_settings( $input ): array {
        $sanitized = array();

        $sanitized['server_url']           = esc_url_raw( $input['server_url'] ?? 'http://localhost:2700' );
        $sanitized['site_name']            = sanitize_text_field( $input['site_name'] ?? 'default' );
        $sanitized['api_key']              = sanitize_text_field( $input['api_key'] ?? '' );
        $sanitized['locale']               = sanitize_text_field( $input['locale'] ?? 'en_US' );
        $sanitized['source_app']           = sanitize_text_field( $input['source_app'] ?? 'wordpress' );
        $sanitized['index_posts']          = ! empty( $input['index_posts'] ) ? 1 : 0;
        $sanitized['index_pages']          = ! empty( $input['index_pages'] ) ? 1 : 0;
        $sanitized['index_custom_types']   = array_map( 'sanitize_text_field', $input['index_custom_types'] ?? array() );
        $sanitized['index_comments']       = ! empty( $input['index_comments'] ) ? 1 : 0;
        $sanitized['index_custom_fields']  = sanitize_text_field( $input['index_custom_fields'] ?? '' );
        $sanitized['index_categories']     = ! empty( $input['index_categories'] ) ? 1 : 0;
        $sanitized['index_tags']           = ! empty( $input['index_tags'] ) ? 1 : 0;
        $sanitized['exclude_ids']          = sanitize_text_field( $input['exclude_ids'] ?? '' );
        $sanitized['auto_index']           = ! empty( $input['auto_index'] ) ? 1 : 0;
        $sanitized['auto_deindex']         = ! empty( $input['auto_deindex'] ) ? 1 : 0;
        $sanitized['batch_size']           = max( 10, min( 200, (int) ( $input['batch_size'] ?? 50 ) ) );
        $sanitized['search_enabled']       = ! empty( $input['search_enabled'] ) ? 1 : 0;
        $sanitized['results_per_page']     = max( 1, min( 100, (int) ( $input['results_per_page'] ?? 10 ) ) );

        return $sanitized;
    }

    /**
     * Enqueue admin CSS and JS on our settings page only.
     */
    public function enqueue_assets( string $hook ): void {
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

        wp_localize_script( 'viglet-dumont-admin', 'vigletDumont', array(
            'restUrl'  => rest_url( 'viglet-dumont/v1/' ),
            'nonce'    => wp_create_nonce( 'wp_rest' ),
            'strings'  => array(
                'indexing'    => __( 'Indexing...', 'viglet-dumont' ),
                'complete'    => __( 'Complete!', 'viglet-dumont' ),
                'error'       => __( 'Error', 'viglet-dumont' ),
                'confirm_del' => __( 'Are you sure you want to delete all indexed content? This cannot be undone.', 'viglet-dumont' ),
                'deleting'    => __( 'Deleting...', 'viglet-dumont' ),
                'deleted'     => __( 'All indexed content has been deleted.', 'viglet-dumont' ),
                'pinging'     => __( 'Testing connection...', 'viglet-dumont' ),
            ),
        ) );
    }

    /**
     * Render the settings page.
     */
    public function render_settings_page(): void {
        $settings   = viglet_dumont_get_settings();
        $post_types = get_post_types( array( 'public' => true, '_builtin' => false ), 'objects' );

        include VIGLET_DUMONT_DIR . 'admin/views/settings-page.php';
    }
}

new Viglet_Dumont_Admin();
