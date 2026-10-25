<?php
/**
 * Plugin Name: Viglet Dumont for WordPress
 * Plugin URI:  https://dumont.viglet.org
 * Description: Integrates WordPress with Viglet Turing search engine for powerful content indexing and search.
 * Version:     2.0.0
 * Author:      Viglet
 * Author URI:  https://viglet.com
 * License:     MIT
 * Text Domain: viglet-dumont
 * Domain Path: /languages
 * Requires at least: 6.5
 * Requires PHP: 8.1
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

define( 'VIGLET_DUMONT_VERSION', '2.0.0' );
define( 'VIGLET_DUMONT_FILE', __FILE__ );
define( 'VIGLET_DUMONT_DIR', plugin_dir_path( __FILE__ ) );
define( 'VIGLET_DUMONT_URL', plugin_dir_url( __FILE__ ) );
define( 'VIGLET_DUMONT_OPTION', 'viglet_dumont_settings' );

require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-api-client.php';
require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-mapping.php';
require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-indexer.php';
require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-search.php';
require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-queue.php';

if ( is_admin() ) {
	require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-admin.php';
}

if ( defined( 'WP_CLI' ) && WP_CLI ) {
	require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-cli-queue.php';
	WP_CLI::add_command( 'dumont queue', 'Viglet_Dumont_CLI_Queue' );
}

/**
 * Get plugin settings with defaults.
 */
function viglet_dumont_get_settings(): array {
	$defaults = array(
		'server_url'          => 'http://localhost:2700',
		'site_name'           => 'default',
		'site_key'            => '',
		'api_key'             => '',
		'locale'              => 'en_US',
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
		'search_enabled'      => 1,
		'results_per_page'    => 10,
	);

	$settings = get_option( VIGLET_DUMONT_OPTION, array() );

	return wp_parse_args( $settings, $defaults );
}

/**
 * Update plugin settings.
 *
 * @param array $settings The full settings array to store.
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

	// Per-post "exclude from search", exposed to the block editor so a sidebar can toggle it.
	foreach ( viglet_dumont_get_indexable_types() as $post_type ) {
		register_post_meta(
			$post_type,
			Viglet_Dumont_Mapping::EXCLUDE_META,
			array(
				'type'          => 'boolean',
				'single'        => true,
				'default'       => false,
				'show_in_rest'  => true,
				'auth_callback' => static fn ( $allowed, $meta_key, $post_id ) => current_user_can( 'edit_post', $post_id ),
			)
		);
	}

	// A save only queues the post; the worker below talks to Turing. save_post fires on every
	// status change too (publish, unpublish, trash, scheduled publish), so it is the one hook.
	if ( $settings['auto_index'] || $settings['auto_deindex'] ) {
		add_action( 'save_post', 'viglet_dumont_handle_save', 10, 2 );
	}
	if ( $settings['auto_deindex'] ) {
		add_action( 'delete_post', 'viglet_dumont_handle_delete' );
	}

	// Search override.
	if ( $settings['search_enabled'] ) {
		add_action( 'template_redirect', 'viglet_dumont_search_redirect', 1 );
	}
}
add_action( 'init', 'viglet_dumont_init' );

/**
 * Handle post save — queue an index if published, a removal otherwise.
 *
 * @param int     $post_id The saved post's ID.
 * @param WP_Post $post    The saved post.
 */
function viglet_dumont_handle_save( int $post_id, WP_Post $post ): void {
	if ( wp_is_post_revision( $post_id ) || wp_is_post_autosave( $post_id ) ) {
		return;
	}
	if ( defined( 'DOING_AUTOSAVE' ) && DOING_AUTOSAVE ) {
		return;
	}

	$settings = viglet_dumont_get_settings();
	$indexer  = new Viglet_Dumont_Indexer( $settings );
	$queue    = new Viglet_Dumont_Queue( $settings );

	if ( 'publish' === $post->post_status ) {
		if ( $settings['auto_index'] && $indexer->should_index( $post ) ) {
			$queue->enqueue( $post_id, 'index' );
		}
	} elseif ( $settings['auto_deindex'] && in_array( $post->post_type, viglet_dumont_get_indexable_types(), true ) ) {
		$queue->enqueue( $post_id, 'delete' );
	}
}

/**
 * Handle permanent deletion — queue a removal.
 *
 * @param int $post_id The deleted post.
 */
function viglet_dumont_handle_delete( int $post_id ): void {
	// delete_post fires for revisions, menu items and attachments too; the post is still readable here.
	$post = get_post( $post_id );
	if ( ! $post || ! in_array( $post->post_type, viglet_dumont_get_indexable_types(), true ) ) {
		return;
	}
	( new Viglet_Dumont_Queue() )->enqueue( $post_id, 'delete' );
}

/**
 * The WP-Cron worker: send one batch of queued jobs.
 */
function viglet_dumont_process_queue(): void {
	( new Viglet_Dumont_Queue() )->process();
}
add_action( Viglet_Dumont_Queue::CRON_HOOK, 'viglet_dumont_process_queue' );

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
 * Plugin deactivation: stop the worker. Queued jobs stay and resume on reactivation's next save.
 */
function viglet_dumont_deactivate(): void {
	wp_clear_scheduled_hook( Viglet_Dumont_Queue::CRON_HOOK );
}
register_deactivation_hook( VIGLET_DUMONT_FILE, 'viglet_dumont_deactivate' );

/**
 * Register REST API endpoints for AJAX operations.
 */
function viglet_dumont_register_rest_routes(): void {
	register_rest_route(
		'viglet-dumont/v1',
		'/index-batch',
		array(
			'methods'             => 'POST',
			'callback'            => 'viglet_dumont_rest_index_batch',
			'permission_callback' => function () {
				return current_user_can( 'manage_options' );
			},
		)
	);

	register_rest_route(
		'viglet-dumont/v1',
		'/delete-all',
		array(
			'methods'             => 'POST',
			'callback'            => 'viglet_dumont_rest_delete_all',
			'permission_callback' => function () {
				return current_user_can( 'manage_options' );
			},
		)
	);

	register_rest_route(
		'viglet-dumont/v1',
		'/ping',
		array(
			'methods'             => 'GET',
			'callback'            => 'viglet_dumont_rest_ping',
			'permission_callback' => function () {
				return current_user_can( 'manage_options' );
			},
		)
	);

	register_rest_route(
		'viglet-dumont/v1',
		'/mapping',
		array(
			array(
				'methods'             => 'GET',
				'callback'            => 'viglet_dumont_rest_get_mapping',
				'permission_callback' => function () {
					return current_user_can( 'manage_options' );
				},
			),
			array(
				'methods'             => 'POST',
				'callback'            => 'viglet_dumont_rest_save_mapping',
				'permission_callback' => function () {
					return current_user_can( 'manage_options' );
				},
			),
		)
	);

	register_rest_route(
		'viglet-dumont/v1',
		'/provision',
		array(
			'methods'             => 'POST',
			'callback'            => 'viglet_dumont_rest_provision',
			'permission_callback' => function () {
				return current_user_can( 'manage_options' );
			},
		)
	);

	register_rest_route(
		'viglet-dumont/v1',
		'/stats',
		array(
			'methods'             => 'GET',
			'callback'            => 'viglet_dumont_rest_stats',
			'permission_callback' => function () {
				return current_user_can( 'manage_options' );
			},
		)
	);
}
add_action( 'rest_api_init', 'viglet_dumont_register_rest_routes' );

/**
 * REST: Index a batch of posts.
 *
 * @param WP_REST_Request $request Carries `offset` and `post_type`.
 */
function viglet_dumont_rest_index_batch( WP_REST_Request $request ): WP_REST_Response {
	$offset    = max( 0, (int) $request->get_param( 'offset' ) );
	$post_type = sanitize_text_field( (string) $request->get_param( 'post_type' ) );
	if ( '' === $post_type ) {
		$post_type = 'post';
	}

	$indexer = new Viglet_Dumont_Indexer();
	$result  = $indexer->index_batch( $post_type, $offset );

	return new WP_REST_Response( $result, empty( $result['error'] ) ? 200 : 502 );
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
	$client = new Viglet_Dumont_API_Client();
	$result = $client->ping();

	return new WP_REST_Response( $result, $result['success'] ? 200 : 500 );
}

/**
 * REST: The field mapping in effect, plus the manifest it provisions. This is the JSON export.
 */
function viglet_dumont_rest_get_mapping(): WP_REST_Response {
	$mapping = new Viglet_Dumont_Mapping();

	return new WP_REST_Response(
		array(
			'mapping'     => $mapping->all(),
			'manifest'    => $mapping->manifest(),
			'provisioned' => get_option( Viglet_Dumont_Mapping::PROVISIONED_OPTION, null ),
		),
		200
	);
}

/**
 * REST: Store a field mapping (the JSON import). Answers with what was kept after validation.
 *
 * @param WP_REST_Request $request Carries `mapping`, keyed by post type.
 */
function viglet_dumont_rest_save_mapping( WP_REST_Request $request ): WP_REST_Response {
	$mapping = $request->get_param( 'mapping' );
	if ( ! is_array( $mapping ) ) {
		return new WP_REST_Response( array( 'message' => __( 'The body needs a mapping object keyed by post type.', 'viglet-dumont' ) ), 400 );
	}

	return new WP_REST_Response( array( 'mapping' => ( new Viglet_Dumont_Mapping() )->save( $mapping ) ), 200 );
}

/**
 * REST: Send the mapped fields' manifest to Turing now.
 */
function viglet_dumont_rest_provision(): WP_REST_Response {
	$result = ( new Viglet_Dumont_Indexer() )->provision( true );

	return new WP_REST_Response( $result, $result['success'] ? 200 : 502 );
}

/**
 * REST: Get indexing stats.
 */
function viglet_dumont_rest_stats(): WP_REST_Response {
	$settings   = viglet_dumont_get_settings();
	$post_types = viglet_dumont_get_indexable_types();
	$stats      = array();

	foreach ( $post_types as $type ) {
		$count          = wp_count_posts( $type );
		$stats[ $type ] = array(
			'total' => isset( $count->publish ) ? (int) $count->publish : 0,
			'label' => get_post_type_object( $type )->labels->name ?? ucfirst( $type ),
		);
	}

	$stats['batch_size'] = (int) $settings['batch_size'];
	$stats['queue']      = ( new Viglet_Dumont_Queue( $settings ) )->counts();

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
