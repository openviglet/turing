<?php
/**
 * Viglet Dumont Operations
 *
 * The bulk operations `wp dumont` and the REST namespace share: index a set of posts, reindex
 * everything, sweep what Turing holds that WordPress no longer publishes, and report status.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Runs over the posts a filter selects, synchronously, batch by batch.
 */
class Viglet_Dumont_Operations {

	/**
	 * The plugin settings these operations were built with.
	 *
	 * @var array
	 */
	private array $settings;

	/**
	 * The indexer that builds and sends the documents.
	 *
	 * @var Viglet_Dumont_Indexer
	 */
	private Viglet_Dumont_Indexer $indexer;

	/**
	 * The client for the calls the indexer does not make.
	 *
	 * @var Viglet_Dumont_API_Client
	 */
	private Viglet_Dumont_API_Client $client;

	/**
	 * Build the operations over the given settings, or over the stored ones.
	 *
	 * @param array|null $settings The plugin settings; null reads them from the options table.
	 */
	public function __construct( ?array $settings = null ) {
		$this->settings = $settings ?? viglet_dumont_get_settings();
		$this->indexer  = new Viglet_Dumont_Indexer( $this->settings );
		$this->client   = new Viglet_Dumont_API_Client( $this->settings );
	}

	/**
	 * The ids of the published posts of the indexed types, narrowed by type, ids or date.
	 *
	 * @param string|null $type  One post type, or null for every indexed one.
	 * @param int[]       $ids   Only these posts, when not empty.
	 * @param string|null $since Only posts modified on or after this date (anything strtotime reads).
	 * @return int[]
	 */
	public function find_post_ids( ?string $type = null, array $ids = array(), ?string $since = null ): array {
		$args = array(
			'post_type'      => null !== $type && '' !== $type ? array( $type ) : viglet_dumont_get_indexable_types(),
			'post_status'    => 'publish',
			'posts_per_page' => -1,
			'fields'         => 'ids',
			'orderby'        => 'ID',
			'order'          => 'ASC',
			'no_found_rows'  => true,
		);
		if ( ! empty( $ids ) ) {
			$args['post__in'] = array_map( 'intval', $ids );
		}
		if ( null !== $since && '' !== $since ) {
			$args['date_query'] = array(
				array(
					'column'    => 'post_modified_gmt',
					'after'     => $since,
					'inclusive' => true,
				),
			);
		}

		return array_map( 'intval', ( new WP_Query( $args ) )->posts );
	}

	/**
	 * Send the given posts now: a create for each one that is indexable, a delete for the rest.
	 *
	 * @param int[]         $post_ids The posts.
	 * @param callable|null $progress Called with the number of posts done after each batch.
	 * @return array{sent: int, failed: int, errors: array<int, string>}
	 */
	public function index( array $post_ids, ?callable $progress = null ): array {
		$batch_size = max( 1, (int) $this->settings['batch_size'] );
		$sent       = 0;
		$errors     = array();

		$this->indexer->provision();

		foreach ( array_chunk( $post_ids, $batch_size ) as $chunk ) {
			$items = array();
			foreach ( $chunk as $post_id ) {
				$post    = get_post( $post_id );
				$items[] = ( $post instanceof WP_Post && $this->indexer->should_index( $post ) )
					? $this->indexer->build_create_item( $post )
					: $this->indexer->build_delete_item( (int) $post_id );
			}

			$result = $this->client->import( $items );
			if ( $result['success'] ) {
				$sent += count( $chunk );
			} else {
				foreach ( $chunk as $post_id ) {
					$errors[ (int) $post_id ] = $result['message'];
				}
			}

			if ( null !== $progress ) {
				$progress( count( $chunk ) );
			}
		}//end foreach

		return array(
			'sent'   => $sent,
			'failed' => count( $errors ),
			'errors' => $errors,
		);
	}

	/**
	 * The ids Turing holds under this site key that WordPress does not publish as indexable.
	 *
	 * An id whose shape is not this key's `<key>:post:<ID>` cannot be a current document of this
	 * site either, so it is an orphan too.
	 *
	 * @return string[]|null Null when Turing could not be listed.
	 */
	public function orphans(): ?array {
		$held = $this->indexer->list_site_ids();
		if ( null === $held ) {
			return null;
		}

		$published = array_flip( $this->find_post_ids() );
		$prefix    = $this->indexer->site_key() . ':post:';
		$orphans   = array();

		foreach ( $held as $doc_id ) {
			$post_id = str_starts_with( $doc_id, $prefix ) ? substr( $doc_id, strlen( $prefix ) ) : '';
			if ( ! ctype_digit( $post_id ) || ! isset( $published[ (int) $post_id ] ) ) {
				$orphans[] = $doc_id;
				continue;
			}
			$post = get_post( (int) $post_id );
			if ( ! $post instanceof WP_Post || ! $this->indexer->should_index( $post ) ) {
				$orphans[] = $doc_id;
			}
		}

		return $orphans;
	}

	/**
	 * Remove the orphans from Turing, or only list them.
	 *
	 * @param bool $dry_run List without deleting.
	 * @return array{success: bool, orphans: string[], message: string}
	 */
	public function sweep( bool $dry_run = false ): array {
		$orphans = $this->orphans();
		if ( null === $orphans ) {
			return array(
				'success' => false,
				'orphans' => array(),
				'message' => __( 'Turing could not be searched for this site\'s documents.', 'viglet-dumont' ),
			);
		}
		if ( $dry_run || empty( $orphans ) ) {
			return array(
				'success' => true,
				'orphans' => $orphans,
				'message' => '',
			);
		}

		$batch_size = max( 1, (int) $this->settings['batch_size'] );
		foreach ( array_chunk( $orphans, $batch_size ) as $chunk ) {
			$result = $this->client->deindex( array_map( array( $this->client, 'build_delete_item' ), $chunk ) );
			if ( ! $result['success'] ) {
				return array(
					'success' => false,
					'orphans' => $orphans,
					'message' => $result['message'],
				);
			}
		}

		return array(
			'success' => true,
			'orphans' => $orphans,
			'message' => '',
		);
	}

	/**
	 * Everything: the fields first, then every published post, then the orphans.
	 *
	 * @param callable|null $progress Called with the number of posts done after each batch.
	 * @return array{sent: int, failed: int, errors: array<int, string>, swept: int, sweep_error: string}
	 */
	public function reindex( ?callable $progress = null ): array {
		$this->indexer->provision( true );
		$result = $this->index( $this->find_post_ids(), $progress );

		// A failed index would read as orphans every post it did not reach; sweep only after a clean run.
		$sweep = $result['failed'] > 0
			? array(
				'success' => false,
				'orphans' => array(),
				'message' => __( 'Skipped: the reindex had failures.', 'viglet-dumont' ),
			)
			: $this->sweep();

		return $result + array(
			'swept'       => count( $sweep['orphans'] ),
			'sweep_error' => $sweep['success'] ? '' : $sweep['message'],
		);
	}

	/**
	 * The connection, the SN site, published versus indexed documents per type, and the queue.
	 *
	 * @return array{connection: array, site_name: string, site_key: string, types: array<string, array{published: int, indexed: int|null}>, queue: array, last_run: mixed, provisioned: mixed} Per type, `published` is what WordPress publishes and `indexed` what Turing holds; null when it cannot say.
	 */
	public function status(): array {
		$types = array();
		foreach ( viglet_dumont_get_indexable_types() as $type ) {
			$types[ $type ] = array(
				'published' => count( $this->find_post_ids( $type ) ),
				'indexed'   => $this->count_in_turing( $type ),
			);
		}

		return array(
			'connection'  => $this->client->ping(),
			'site_name'   => $this->client->get_site_name(),
			'site_key'    => $this->indexer->site_key(),
			'types'       => $types,
			'queue'       => ( new Viglet_Dumont_Queue( $this->settings ) )->counts(),
			'last_run'    => get_option( Viglet_Dumont_Queue::LAST_RUN_OPTION, null ),
			'provisioned' => get_option( Viglet_Dumont_Mapping::PROVISIONED_OPTION, null ),
		);
	}

	/**
	 * How many documents of a type Turing holds under this site key; null when it cannot say.
	 *
	 * @param string $type The post type.
	 */
	private function count_in_turing( string $type ): ?int {
		$response = $this->client->search(
			'*',
			1,
			0,
			array( 'fq[]' => array( 'site_key:' . $this->indexer->site_key(), 'type:' . $type ) )
		);

		return is_wp_error( $response ) ? null : (int) ( $response['queryContext']['count'] ?? 0 );
	}
}
