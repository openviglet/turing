<?php
/**
 * Viglet Dumont Post State
 *
 * Tells an editor whether a post is in the index: a column in the post lists and a panel in
 * the block editor's sidebar, both reading the same state.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Where a post stands with Turing, from its queue job, its recorded send and its exclusion.
 */
class Viglet_Dumont_Post_State {

	/**
	 * The plugin settings this state is read with.
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
		$this->settings = $settings ?? viglet_dumont_get_settings();
	}

	/**
	 * Hook the list column, the editor panel and their REST routes.
	 */
	public function register(): void {
		foreach ( viglet_dumont_get_indexable_types() as $type ) {
			add_filter( "manage_{$type}_posts_columns", array( $this, 'add_column' ) );
			add_action( "manage_{$type}_posts_custom_column", array( $this, 'render_column' ), 10, 2 );
		}
		add_action( 'enqueue_block_editor_assets', array( $this, 'enqueue_editor_panel' ) );
		add_action( 'rest_api_init', array( $this, 'register_routes' ) );
	}

	/**
	 * The state of a post: a machine `state`, a short `label` and a `detail` (an error, or
	 * when it was indexed). `state` is one of indexed, queued, failed, excluded, unpublished,
	 * not_indexed.
	 *
	 * @param WP_Post $post The post.
	 * @return array{state: string, label: string, detail: string}
	 */
	public function describe( WP_Post $post ): array {
		$indexer = new Viglet_Dumont_Indexer( $this->settings );

		if ( 'publish' !== $post->post_status ) {
			return $this->state( 'unpublished', __( 'Not published', 'viglet-dumont' ) );
		}
		if ( ! $indexer->should_index( $post ) ) {
			return $this->state( 'excluded', __( 'Excluded', 'viglet-dumont' ) );
		}

		$job = ( new Viglet_Dumont_Queue( $this->settings ) )->jobs()[ $post->ID ] ?? null;
		if ( is_array( $job ) ) {
			if ( ! empty( $job['failed'] ) ) {
				return $this->state( 'failed', __( 'Failed', 'viglet-dumont' ), (string) $job['error'] );
			}
			return $this->state( 'queued', __( 'Queued', 'viglet-dumont' ), (string) $job['error'] );
		}

		$recorded = get_post_meta( $post->ID, Viglet_Dumont_Queue::STATE_META, true );
		$recorded = is_array( $recorded ) ? $recorded : array();
		if ( '' !== ( $recorded['error'] ?? '' ) ) {
			return $this->state( 'failed', __( 'Failed', 'viglet-dumont' ), (string) $recorded['error'] );
		}
		if ( ! empty( $recorded['indexed_at'] ) ) {
			return $this->state(
				'indexed',
				/* translators: %s: how long ago, e.g. "2 mins". */
				sprintf( __( 'Indexed %s ago', 'viglet-dumont' ), human_time_diff( (int) $recorded['indexed_at'] ) )
			);
		}

		return $this->state( 'not_indexed', __( 'Not indexed yet', 'viglet-dumont' ) );
	}

	/**
	 * Add the Search column to a post list.
	 *
	 * @param array $columns The list's columns.
	 */
	public function add_column( array $columns ): array {
		$columns['viglet_dumont'] = __( 'Search', 'viglet-dumont' );
		return $columns;
	}

	/**
	 * Print a post's state in the Search column.
	 *
	 * @param string $column  The column being printed.
	 * @param int    $post_id The row's post.
	 */
	public function render_column( string $column, int $post_id ): void {
		$post = get_post( $post_id );
		if ( 'viglet_dumont' !== $column || ! $post instanceof WP_Post ) {
			return;
		}

		$state = $this->describe( $post );
		printf(
			'<span class="viglet-dumont-state viglet-dumont-state--%1$s" title="%2$s">%3$s</span>',
			esc_attr( $state['state'] ),
			esc_attr( $state['detail'] ),
			esc_html( $state['label'] )
		);
	}

	/**
	 * Load the block editor's Search panel for the indexed post types.
	 */
	public function enqueue_editor_panel(): void {
		$screen = function_exists( 'get_current_screen' ) ? get_current_screen() : null;
		if ( ! $screen || ! in_array( $screen->post_type, viglet_dumont_get_indexable_types(), true ) ) {
			return;
		}

		wp_enqueue_script(
			'viglet-dumont-editor',
			VIGLET_DUMONT_URL . 'admin/js/editor-panel.js',
			array( 'wp-plugins', 'wp-edit-post', 'wp-editor', 'wp-element', 'wp-components', 'wp-data', 'wp-api-fetch', 'wp-i18n' ),
			VIGLET_DUMONT_VERSION,
			true
		);
		wp_set_script_translations( 'viglet-dumont-editor', 'viglet-dumont', VIGLET_DUMONT_DIR . 'languages' );
	}

	/**
	 * `GET viglet-dumont/v1/post/<id>/state` and `POST .../reindex`, for whoever can edit the post.
	 */
	public function register_routes(): void {
		$can_edit = static fn ( WP_REST_Request $request ) => current_user_can( 'edit_post', (int) $request['id'] );

		register_rest_route(
			'viglet-dumont/v1',
			'/post/(?P<id>\d+)/state',
			array(
				'methods'             => 'GET',
				'callback'            => array( $this, 'rest_state' ),
				'permission_callback' => $can_edit,
			)
		);
		register_rest_route(
			'viglet-dumont/v1',
			'/post/(?P<id>\d+)/reindex',
			array(
				'methods'             => 'POST',
				'callback'            => array( $this, 'rest_reindex' ),
				'permission_callback' => $can_edit,
			)
		);
	}

	/**
	 * REST: a post's state.
	 *
	 * @param WP_REST_Request $request Carries `id`.
	 */
	public function rest_state( WP_REST_Request $request ): WP_REST_Response {
		$post = get_post( (int) $request['id'] );
		if ( ! $post instanceof WP_Post ) {
			return new WP_REST_Response( array( 'message' => __( 'No such post.', 'viglet-dumont' ) ), 404 );
		}

		return new WP_REST_Response( $this->describe( $post ), 200 );
	}

	/**
	 * REST: send a post now, without the queue, and answer with its new state.
	 *
	 * @param WP_REST_Request $request Carries `id`.
	 */
	public function rest_reindex( WP_REST_Request $request ): WP_REST_Response {
		$post_id = (int) $request['id'];
		$result  = ( new Viglet_Dumont_Operations( $this->settings ) )->index( array( $post_id ) );

		$post = get_post( $post_id );
		if ( $post instanceof WP_Post ) {
			$state = get_post_meta( $post_id, Viglet_Dumont_Queue::STATE_META, true );
			$state = is_array( $state ) ? $state : array();
			if ( 0 === $result['failed'] ) {
				// An excluded or unpublished post was sent as a delete: it is now out of the index.
				$state['indexed_at'] = ( new Viglet_Dumont_Indexer( $this->settings ) )->should_index( $post ) ? time() : null;
				$state['error']      = '';
				$state['attempts']   = 0;
			} else {
				$state['error'] = $result['errors'][ $post_id ] ?? '';
			}
			update_post_meta( $post_id, Viglet_Dumont_Queue::STATE_META, $state );
		}

		return new WP_REST_Response(
			$post instanceof WP_Post ? $this->describe( $post ) : array(),
			0 === $result['failed'] ? 200 : 502
		);
	}

	/**
	 * One state.
	 *
	 * @param string $state  The machine state.
	 * @param string $label  What the column shows.
	 * @param string $detail The error, or nothing.
	 * @return array{state: string, label: string, detail: string}
	 */
	private function state( string $state, string $label, string $detail = '' ): array {
		return array(
			'state'  => $state,
			'label'  => $label,
			'detail' => $detail,
		);
	}
}
