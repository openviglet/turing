<?php
/**
 * Plugin Name: Viglet Dumont for WordPress
 * Plugin URI:  https://viglet.com/dumont
 * Description: Integrates WordPress with Viglet Turing search engine for powerful content indexing and search.
 * Version:     1.0.0
 * Author:      Viglet
 * Author URI:  https://viglet.com
 * License:     MIT
 * Text Domain: viglet-dumont
 * Domain Path: /languages
 * Requires at least: 5.8
 * Requires PHP: 7.4
 */

if ( ! defined( 'ABSPATH' ) ) {
    exit;
}

define( 'VIGLET_DUMONT_VERSION', '1.0.0' );
define( 'VIGLET_DUMONT_FILE', __FILE__ );
define( 'VIGLET_DUMONT_DIR', plugin_dir_path( __FILE__ ) );
define( 'VIGLET_DUMONT_URL', plugin_dir_url( __FILE__ ) );
define( 'VIGLET_DUMONT_OPTION', 'viglet_dumont_settings' );

require_once VIGLET_DUMONT_DIR . 'includes/class-dumont-api-client.php';
require_once VIGLET_DUMONT_DIR . 'includes/class-dumont-indexer.php';
require_once VIGLET_DUMONT_DIR . 'includes/class-dumont-search.php';

if ( is_admin() ) {
    require_once VIGLET_DUMONT_DIR . 'includes/class-dumont-admin.php';
}

/**
 * Get plugin settings with defaults.
 */
function viglet_dumont_get_settings(): array {
    $defaults = array(
        'server_url'           => 'http://localhost:2700',
        'site_name'            => 'default',
        'api_key'              => '',
        'locale'               => 'en_US',
        'source_app'           => 'wordpress',
        'index_posts'          => 1,
        'index_pages'          => 1,
        'index_custom_types'   => array(),
        'index_comments'       => 0,
        'index_custom_fields'  => '',
        'index_categories'     => 1,
        'index_tags'           => 1,
        'exclude_ids'          => '',
        'auto_index'           => 1,
        'auto_deindex'         => 1,
        'batch_size'           => 50,
        'search_enabled'       => 1,
        'results_per_page'     => 10,
    );

    $settings = get_option( VIGLET_DUMONT_OPTION, array() );

    return wp_parse_args( $settings, $defaults );
}

/**
 * Update plugin settings.
 */
function viglet_dumont_update_settings( array $settings ): bool {
    return update_option( VIGLET_DUMONT_OPTION, $settings );
}

/**
 * Initialize the plugin.
 */
function viglet_dumont_init(): void {
    load_plugin_textdomain( 'viglet-dumont', false, dirname( plugin_basename( VIGLET_DUMONT_FILE ) ) . '/languages' );

    $settings = viglet_dumont_get_settings();

    // Auto-index on publish.
    if ( $settings['auto_index'] ) {
        add_action( 'publish_post', 'viglet_dumont_handle_publish' );
        add_action( 'publish_page', 'viglet_dumont_handle_publish' );
        add_action( 'save_post', 'viglet_dumont_handle_save', 10, 2 );
    }

    // Auto-deindex on delete/trash.
    if ( $settings['auto_deindex'] ) {
        add_action( 'trash_post', 'viglet_dumont_handle_delete' );
        add_action( 'delete_post', 'viglet_dumont_handle_delete' );
        add_action( 'transition_post_status', 'viglet_dumont_handle_status_transition', 10, 3 );
    }

    // Search override.
    if ( $settings['search_enabled'] ) {
        add_action( 'template_redirect', 'viglet_dumont_search_redirect', 1 );
    }
}
add_action( 'init', 'viglet_dumont_init' );

/**
 * Handle post publish — index immediately.
 */
function viglet_dumont_handle_publish( int $post_id ): void {
    $post = get_post( $post_id );
    if ( ! $post || wp_is_post_revision( $post_id ) ) {
        return;
    }

    $indexer = new Viglet_Dumont_Indexer();
    if ( $indexer->should_index( $post ) ) {
        $indexer->index_post( $post );
    }
}

/**
 * Handle post save — index if published, deindex otherwise.
 */
function viglet_dumont_handle_save( int $post_id, WP_Post $post ): void {
    if ( wp_is_post_revision( $post_id ) || wp_is_post_autosave( $post_id ) ) {
        return;
    }
    if ( defined( 'DOING_AUTOSAVE' ) && DOING_AUTOSAVE ) {
        return;
    }

    $indexer = new Viglet_Dumont_Indexer();
    if ( 'publish' === $post->post_status && $indexer->should_index( $post ) ) {
        $indexer->index_post( $post );
    } elseif ( 'publish' !== $post->post_status ) {
        $indexer->deindex_post( $post_id );
    }
}

/**
 * Handle post deletion.
 */
function viglet_dumont_handle_delete( int $post_id ): void {
    $indexer = new Viglet_Dumont_Indexer();
    $indexer->deindex_post( $post_id );
}

/**
 * Handle post status transitions (e.g., publish -> draft).
 */
function viglet_dumont_handle_status_transition( string $new_status, string $old_status, WP_Post $post ): void {
    if ( $old_status === $new_status ) {
        return;
    }

    $indexer = new Viglet_Dumont_Indexer();

    if ( 'publish' === $new_status && $indexer->should_index( $post ) ) {
        $indexer->index_post( $post );
    } elseif ( 'publish' === $old_status && 'publish' !== $new_status ) {
        $indexer->deindex_post( $post->ID );
    }
}

/**
 * Search redirect — replaces default WordPress search with Turing.
 */
function viglet_dumont_search_redirect(): void {
    if ( ! is_search() ) {
        return;
    }

    $settings = viglet_dumont_get_settings();
    if ( ! $settings['search_enabled'] ) {
        return;
    }

    $search = new Viglet_Dumont_Search();
    $search->render();
    exit;
}

/**
 * Plugin activation.
 */
function viglet_dumont_activate(): void {
    $settings = viglet_dumont_get_settings();
    update_option( VIGLET_DUMONT_OPTION, $settings );
}
register_activation_hook( VIGLET_DUMONT_FILE, 'viglet_dumont_activate' );

/**
 * Register REST API endpoints for AJAX operations.
 */
function viglet_dumont_register_rest_routes(): void {
    register_rest_route( 'viglet-dumont/v1', '/index-batch', array(
        'methods'             => 'POST',
        'callback'            => 'viglet_dumont_rest_index_batch',
        'permission_callback' => function () {
            return current_user_can( 'manage_options' );
        },
    ) );

    register_rest_route( 'viglet-dumont/v1', '/delete-all', array(
        'methods'             => 'POST',
        'callback'            => 'viglet_dumont_rest_delete_all',
        'permission_callback' => function () {
            return current_user_can( 'manage_options' );
        },
    ) );

    register_rest_route( 'viglet-dumont/v1', '/ping', array(
        'methods'             => 'GET',
        'callback'            => 'viglet_dumont_rest_ping',
        'permission_callback' => function () {
            return current_user_can( 'manage_options' );
        },
    ) );

    register_rest_route( 'viglet-dumont/v1', '/stats', array(
        'methods'             => 'GET',
        'callback'            => 'viglet_dumont_rest_stats',
        'permission_callback' => function () {
            return current_user_can( 'manage_options' );
        },
    ) );
}
add_action( 'rest_api_init', 'viglet_dumont_register_rest_routes' );

/**
 * REST: Index a batch of posts.
 */
function viglet_dumont_rest_index_batch( WP_REST_Request $request ): WP_REST_Response {
    $offset    = (int) $request->get_param( 'offset' ) ?: 0;
    $post_type = sanitize_text_field( $request->get_param( 'post_type' ) ?: 'post' );

    $indexer = new Viglet_Dumont_Indexer();
    $result  = $indexer->index_batch( $post_type, $offset );

    return new WP_REST_Response( $result, 200 );
}

/**
 * REST: Delete all indexed content.
 */
function viglet_dumont_rest_delete_all(): WP_REST_Response {
    $indexer = new Viglet_Dumont_Indexer();
    $result  = $indexer->delete_all();

    return new WP_REST_Response( array( 'success' => $result ), $result ? 200 : 500 );
}

/**
 * REST: Test server connection.
 */
function viglet_dumont_rest_ping(): WP_REST_Response {
    $client  = new Viglet_Dumont_API_Client();
    $result  = $client->ping();

    return new WP_REST_Response( $result, $result['success'] ? 200 : 500 );
}

/**
 * REST: Get indexing stats.
 */
function viglet_dumont_rest_stats(): WP_REST_Response {
    $settings    = viglet_dumont_get_settings();
    $post_types  = viglet_dumont_get_indexable_types();
    $stats       = array();

    foreach ( $post_types as $type ) {
        $count = wp_count_posts( $type );
        $stats[ $type ] = array(
            'total'     => isset( $count->publish ) ? (int) $count->publish : 0,
            'label'     => get_post_type_object( $type )->labels->name ?? ucfirst( $type ),
        );
    }

    $stats['batch_size'] = (int) $settings['batch_size'];

    return new WP_REST_Response( $stats, 200 );
}

/**
 * Get all post types that should be indexed.
 */
function viglet_dumont_get_indexable_types(): array {
    $settings = viglet_dumont_get_settings();
    $types    = array();

    if ( $settings['index_posts'] ) {
        $types[] = 'post';
    }
    if ( $settings['index_pages'] ) {
        $types[] = 'page';
    }

    if ( ! empty( $settings['index_custom_types'] ) && is_array( $settings['index_custom_types'] ) ) {
        $types = array_merge( $types, $settings['index_custom_types'] );
    }

    return array_unique( $types );
}
