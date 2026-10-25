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
 * Requires at least: 6.6
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

require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-settings.php';
require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-api-client.php';
require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-events.php';
require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-mapping.php';
require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-indexer.php';
require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-search.php';
require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-native-search.php';
require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-search-block.php';
require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-queue.php';
require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-operations.php';
require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-post-state.php';
require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-diagnostics.php';
require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-tuning.php';
require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-related.php';

if ( is_admin() ) {
	require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-admin.php';
}

if ( defined( 'WP_CLI' ) && WP_CLI ) {
	require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-cli.php';
	require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-cli-queue.php';
	require_once VIGLET_DUMONT_DIR . 'includes/class-viglet-dumont-cli-ai.php';
	WP_CLI::add_command( 'dumont', 'Viglet_Dumont_CLI' );
	WP_CLI::add_command( 'dumont queue', 'Viglet_Dumont_CLI_Queue' );
	WP_CLI::add_command( 'dumont ai', 'Viglet_Dumont_CLI_AI' );
}

/**
 * The settings in effect: defaults, the stored option, then what the environment sets.
 */
function viglet_dumont_get_settings(): array {
	return Viglet_Dumont_Settings::get();
}

/**
 * What a theme's own search blocks need to query Turing from the browser: its public URL,
 * the SN site, the locale, this site's key and the facets the mapping declares. Holds no
 * secret, so it can be printed into a page or cached.
 *
 * @return array{url: string, site: string, locale: string, siteKey: string, facets: array}
 */
function viglet_dumont_public_config(): array {
	return ( new Viglet_Dumont_Native_Search() )->public_config();
}

/**
 * Which of Turing's AI features this site can use, for Health and `wp dumont ai status`: each
 * row with a label, whether it is on, the reason when it is not, and what to do about it.
 * It spends LLM calls when features are on (Turing T1123), so it runs on demand only.
 *
 * @return array<int, array{feature: string, label: string, on: bool, reason: string, fix: string}>
 */
function viglet_dumont_ai_status(): array {
	$settings = viglet_dumont_get_settings();
	$console  = untrailingslashit( '' !== (string) $settings['public_url'] ? $settings['public_url'] : $settings['server_url'] );
	$labels   = array(
		'answer'     => __( 'Answers with sources', 'viglet-dumont' ),
		'chat'       => __( 'Chat', 'viglet-dumont' ),
		'nl_filters' => __( 'Natural-language filters', 'viglet-dumont' ),
		'copilot'    => __( 'Catalog copilot', 'viglet-dumont' ),
	);

	return array_map(
		static fn ( array $row ) => array(
			'feature' => $row['feature'],
			'label'   => $labels[ $row['feature'] ] ?? $row['feature'],
			'on'      => $row['on'],
			// A sentence, so the fix after it reads as the next one.
			'reason'  => '' === $row['reason'] || preg_match( '/[.!?]$/', $row['reason'] ) ? $row['reason'] : $row['reason'] . '.',
			// The manifest cannot turn a site's generative AI on yet (Turing T1116): name the step.
			'fix'     => ! $row['on'] && 'answer' === $row['feature']
				/* translators: %s: Turing's console address. */
				? sprintf( __( 'Turn on generative AI for this SN site in Turing\'s console (%s), under the site\'s Generative AI settings.', 'viglet-dumont' ), $console )
				: '',
		),
		( new Viglet_Dumont_API_Client( $settings ) )->ai_features()
	);
}

/**
 * Store the settings; a value the environment sets is never written.
 *
 * @param array $settings The full settings array to store.
 */
function viglet_dumont_update_settings( array $settings ): bool {
	return Viglet_Dumont_Settings::update( $settings );
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

	// Where each post stands: the Search column in the post lists and the editor panel.
	( new Viglet_Dumont_Post_State( $settings ) )->register();

	// Search override.
	// The SDK as script modules, for a theme's own search blocks.
	Viglet_Dumont_Native_Search::register_modules();
	Viglet_Dumont_Native_Search::register_blocks();
	add_action( 'wp_enqueue_scripts', array( 'Viglet_Dumont_Native_Search', 'provide_config' ) );
	// Related content after the chosen post types' content (D177).
	( new Viglet_Dumont_Related( $settings ) )->register();

	if ( $settings['search_enabled'] ) {
		if ( 'template' === $settings['search_mode'] ) {
			add_action( 'template_redirect', 'viglet_dumont_search_redirect', 1 );
		} else {
			( new Viglet_Dumont_Native_Search( $settings ) )->register();
		}
		if ( $settings['instant_overlay'] ) {
			( new Viglet_Dumont_Native_Search( $settings ) )->register_overlay();
		}
	}
}
add_action( 'init', 'viglet_dumont_init' );
add_filter( 'wp_robots', array( 'Viglet_Dumont_Native_Search', 'robots' ) );
// The WP Consent API's handshake: this plugin asks for statistics consent before reporting.
add_filter( 'wp_consent_api_registered_' . plugin_basename( VIGLET_DUMONT_FILE ), '__return_true' );

/**
 * Point WordPress at the JS translations: `wp i18n make-json` names each file after the
 * source path the strings were found in (assets/admin/health.js, src/admin/js/...), and
 * WordPress looks one up by the path of the script it loads (admin/build/health.js).
 *
 * @param string|false $relative The script's path relative to the plugin.
 * @param string       $src      The script's URL.
 * @return string|false
 */
function viglet_dumont_script_translation_path( $relative, $src ) {
	if ( ! is_string( $relative ) || ! str_starts_with( (string) $src, VIGLET_DUMONT_URL ) ) {
		return $relative;
	}
	if ( preg_match( '#^admin/build/([a-z-]+)\.js$#', $relative, $match ) ) {
		return 'assets/admin/' . $match[1] . '.js';
	}

	return 'src/' . $relative;
}
add_filter( 'load_script_textdomain_relative_path', 'viglet_dumont_script_translation_path', 10, 2 );

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
	// Opening the editor creates an auto-draft, which was never indexed and has nothing to remove.
	if ( 'auto-draft' === $post->post_status ) {
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
	viglet_dumont_update_settings( viglet_dumont_get_settings() );
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

	// Public on purpose: what a browser needs to search through Turing, never the API key.
	register_rest_route(
		'viglet-dumont/v1',
		'/public-config',
		array(
			'methods'             => 'GET',
			'callback'            => static fn () => new WP_REST_Response( viglet_dumont_public_config(), 200 ),
			'permission_callback' => '__return_true',
		)
	);

	register_rest_route(
		'viglet-dumont/v1',
		'/status',
		array(
			'methods'             => 'GET',
			'callback'            => static fn () => new WP_REST_Response( ( new Viglet_Dumont_Operations() )->status(), 200 ),
			'permission_callback' => function () {
				return current_user_can( 'manage_options' );
			},
		)
	);

	register_rest_route(
		'viglet-dumont/v1',
		'/index',
		array(
			'methods'             => 'POST',
			'callback'            => 'viglet_dumont_rest_index',
			'permission_callback' => function () {
				return current_user_can( 'manage_options' );
			},
		)
	);

	register_rest_route(
		'viglet-dumont/v1',
		'/queue/retry',
		array(
			'methods'             => 'POST',
			'callback'            => static fn () => new WP_REST_Response( array( 'replayed' => ( new Viglet_Dumont_Queue() )->retry_failed() ), 200 ),
			'permission_callback' => function () {
				return current_user_can( 'manage_options' );
			},
		)
	);

	register_rest_route(
		'viglet-dumont/v1',
		'/queue/run',
		array(
			'methods'             => 'POST',
			'callback'            => static fn () => new WP_REST_Response( ( new Viglet_Dumont_Queue() )->process( true ), 200 ),
			'permission_callback' => function () {
				return current_user_can( 'manage_options' );
			},
		)
	);

	register_rest_route(
		'viglet-dumont/v1',
		'/sweep',
		array(
			'methods'             => 'POST',
			'callback'            => 'viglet_dumont_rest_sweep',
			'permission_callback' => function () {
				return current_user_can( 'manage_options' );
			},
		)
	);

	register_rest_route(
		'viglet-dumont/v1',
		'/connection',
		array(
			array(
				'methods'             => 'GET',
				'callback'            => 'viglet_dumont_rest_get_connection',
				'permission_callback' => function () {
					return current_user_can( 'manage_options' );
				},
			),
			array(
				'methods'             => 'POST',
				'callback'            => 'viglet_dumont_rest_save_connection',
				'permission_callback' => function () {
					return current_user_can( 'manage_options' );
				},
			),
		)
	);

	register_rest_route(
		'viglet-dumont/v1',
		'/diagnose',
		array(
			'methods'             => 'GET',
			'callback'            => static fn () => new WP_REST_Response( ( new Viglet_Dumont_Diagnostics() )->run(), 200 ),
			'permission_callback' => function () {
				return current_user_can( 'manage_options' );
			},
		)
	);

	register_rest_route(
		'viglet-dumont/v1',
		'/mapping/preview',
		array(
			'methods'             => 'GET',
			'callback'            => 'viglet_dumont_rest_preview_mapping',
			'permission_callback' => function () {
				return current_user_can( 'manage_options' );
			},
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

	// Search tuning (D172): synonyms and promoted results on the SN site.
	$can_manage = static fn () => current_user_can( 'manage_options' );
	$answer     = static fn ( $result ) => is_wp_error( $result )
		? new WP_REST_Response( array( 'message' => $result->get_error_message() ), 'viglet_dumont_not_ours' === $result->get_error_code() ? 403 : 502 )
		: new WP_REST_Response( true === $result ? ( new Viglet_Dumont_Tuning() )->all() : $result, 200 );
	register_rest_route(
		'viglet-dumont/v1',
		'/tuning',
		array(
			'methods'             => 'GET',
			'callback'            => static fn () => $answer( ( new Viglet_Dumont_Tuning() )->all() ),
			'permission_callback' => $can_manage,
		)
	);
	register_rest_route(
		'viglet-dumont/v1',
		'/tuning/synonym',
		array(
			'methods'             => 'POST',
			'callback'            => static fn ( WP_REST_Request $request ) => $answer(
				( new Viglet_Dumont_Tuning() )->add_synonym( array_map( 'sanitize_text_field', (array) $request->get_param( 'terms' ) ) )
			),
			'permission_callback' => $can_manage,
		)
	);
	register_rest_route(
		'viglet-dumont/v1',
		'/tuning/synonym/(?P<id>[A-Za-z0-9-]+)',
		array(
			'methods'             => 'DELETE',
			'callback'            => static fn ( WP_REST_Request $request ) => $answer( ( new Viglet_Dumont_Tuning() )->remove_synonym( (string) $request['id'] ) ),
			'permission_callback' => $can_manage,
		)
	);
	register_rest_route(
		'viglet-dumont/v1',
		'/tuning/spotlight',
		array(
			'methods'             => 'POST',
			'callback'            => static fn ( WP_REST_Request $request ) => $answer(
				( new Viglet_Dumont_Tuning() )->add_spotlight(
					sanitize_text_field( (string) $request->get_param( 'query' ) ),
					array_map( 'absint', (array) $request->get_param( 'posts' ) )
				)
			),
			'permission_callback' => $can_manage,
		)
	);
	register_rest_route(
		'viglet-dumont/v1',
		'/tuning/boosts',
		array(
			'methods'             => 'POST',
			'callback'            => static fn ( WP_REST_Request $request ) => $answer(
				( new Viglet_Dumont_Tuning() )->set_boosts(
					(bool) $request->get_param( 'recency' ),
					(bool) $request->get_param( 'sticky' ),
					array_map( 'sanitize_key', (array) $request->get_param( 'types' ) )
				)
			),
			'permission_callback' => $can_manage,
		)
	);
	register_rest_route(
		'viglet-dumont/v1',
		'/tuning/suggestions',
		array(
			'methods'             => 'GET',
			'callback'            => static fn () => $answer( ( new Viglet_Dumont_Tuning() )->suggestions() ),
			'permission_callback' => $can_manage,
		)
	);
	register_rest_route(
		'viglet-dumont/v1',
		'/tuning/try',
		array(
			'methods'             => 'GET',
			'callback'            => static fn ( WP_REST_Request $request ) => $answer(
				( new Viglet_Dumont_Tuning() )->try_query( sanitize_text_field( (string) $request->get_param( 'q' ) ) )
			),
			'permission_callback' => $can_manage,
		)
	);
	register_rest_route(
		'viglet-dumont/v1',
		'/tuning/spotlight/(?P<id>[A-Za-z0-9-]+)',
		array(
			'methods'             => 'DELETE',
			'callback'            => static fn ( WP_REST_Request $request ) => $answer( ( new Viglet_Dumont_Tuning() )->remove_spotlight( (string) $request['id'] ) ),
			'permission_callback' => $can_manage,
		)
	);

	register_rest_route(
		'viglet-dumont/v1',
		'/ai',
		array(
			'methods'             => 'GET',
			'callback'            => static fn () => new WP_REST_Response( viglet_dumont_ai_status(), 200 ),
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
	$settings = viglet_dumont_get_settings();
	$mapping  = new Viglet_Dumont_Mapping( $settings );
	$status   = $mapping->field_status( new Viglet_Dumont_API_Client( $settings ) );
	$all      = $mapping->all();

	$sources = array();
	foreach ( array_keys( $all ) as $post_type ) {
		$sources[ $post_type ] = Viglet_Dumont_Mapping::sources( $post_type );
	}

	return new WP_REST_Response(
		array(
			'mapping'      => $all,
			'manifest'     => $mapping->manifest(),
			'provisioned'  => get_option( Viglet_Dumont_Mapping::PROVISIONED_OPTION, null ),
			'field_status' => is_wp_error( $status ) ? null : $status,
			'status_error' => is_wp_error( $status ) ? $status->get_error_message() : '',
			'sources'      => $sources,
			'types'        => Viglet_Dumont_Mapping::TYPES,
		),
		200
	);
}

/**
 * REST: The connection settings the Setup page edits. The key itself is never sent back,
 * only whether one is set.
 */
function viglet_dumont_rest_get_connection(): WP_REST_Response {
	$settings = viglet_dumont_get_settings();

	return new WP_REST_Response(
		array(
			'server_url'  => $settings['server_url'],
			'site_name'   => $settings['site_name'],
			'locale'      => $settings['locale'],
			'site_key'    => $settings['site_key'],
			'has_api_key' => '' !== (string) $settings['api_key'],
			'derived_key' => ( new Viglet_Dumont_Indexer( array_merge( $settings, array( 'site_key' => '' ) ) ) )->site_key(),
			// Per setting, what sets it: constant, env or option. The first two are read-only here.
			'sources'     => Viglet_Dumont_Settings::sources(),
		),
		200
	);
}

/**
 * REST: Store the connection settings. An empty or absent `api_key` keeps the stored one.
 *
 * @param WP_REST_Request $request Carries `server_url`, `site_name`, `locale`, `site_key`, `api_key`.
 */
function viglet_dumont_rest_save_connection( WP_REST_Request $request ): WP_REST_Response {
	$settings = viglet_dumont_get_settings();

	foreach ( array( 'site_name', 'locale' ) as $key ) {
		if ( null !== $request->get_param( $key ) ) {
			$settings[ $key ] = sanitize_text_field( (string) $request->get_param( $key ) );
		}
	}
	if ( null !== $request->get_param( 'server_url' ) ) {
		$settings['server_url'] = esc_url_raw( (string) $request->get_param( 'server_url' ) );
	}
	if ( null !== $request->get_param( 'site_key' ) ) {
		$settings['site_key'] = Viglet_Dumont_Indexer::sanitize_site_key( (string) $request->get_param( 'site_key' ) );
	}
	$api_key = (string) $request->get_param( 'api_key' );
	if ( '' !== $api_key ) {
		$settings['api_key'] = sanitize_text_field( $api_key );
	}

	viglet_dumont_update_settings( $settings );

	return viglet_dumont_rest_get_connection();
}

/**
 * REST: The document the newest published post of a type becomes, as it would be sent.
 *
 * @param WP_REST_Request $request Carries `post_type`.
 */
function viglet_dumont_rest_preview_mapping( WP_REST_Request $request ): WP_REST_Response {
	$post_type = sanitize_key( (string) $request->get_param( 'post_type' ) );
	$posts     = get_posts(
		array(
			'post_type'   => $post_type,
			'post_status' => 'publish',
			'numberposts' => 1,
			'orderby'     => 'modified',
		)
	);
	if ( empty( $posts ) ) {
		return new WP_REST_Response( array( 'message' => __( 'No published post of this type to preview.', 'viglet-dumont' ) ), 404 );
	}

	$item = ( new Viglet_Dumont_Indexer() )->build_create_item( $posts[0] );

	return new WP_REST_Response(
		array(
			'post'     => array(
				'id'    => $posts[0]->ID,
				'title' => $posts[0]->post_title,
			),
			'document' => $item['attributes'],
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
 * REST: Index the posts `type`, `ids` and `since` select, synchronously — `wp dumont index`.
 *
 * @param WP_REST_Request $request Carries the optional `type`, `ids` (array) and `since`.
 */
function viglet_dumont_rest_index( WP_REST_Request $request ): WP_REST_Response {
	$operations = new Viglet_Dumont_Operations();
	$ids        = array_filter( array_map( 'intval', (array) $request->get_param( 'ids' ) ) );
	$type       = sanitize_key( (string) $request->get_param( 'type' ) );
	$since      = sanitize_text_field( (string) $request->get_param( 'since' ) );
	$post_ids   = array_values( array_unique( array_merge( $operations->find_post_ids( $type, $ids, $since ), $ids ) ) );
	$result     = $operations->index( $post_ids );

	return new WP_REST_Response( $result, 0 === $result['failed'] ? 200 : 502 );
}

/**
 * REST: Remove this site key's documents WordPress no longer publishes — `wp dumont sweep`.
 *
 * @param WP_REST_Request $request Carries `dry_run`.
 */
function viglet_dumont_rest_sweep( WP_REST_Request $request ): WP_REST_Response {
	$result = ( new Viglet_Dumont_Operations() )->sweep( (bool) $request->get_param( 'dry_run' ) );

	return new WP_REST_Response( $result, $result['success'] ? 200 : 502 );
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
