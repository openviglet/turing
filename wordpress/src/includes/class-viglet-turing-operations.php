<?php
/**
 * Viglet Turing Operations
 *
 * The bulk operations `wp turing` and the REST namespace share: index a set of posts, reindex
 * everything, sweep what Turing holds that WordPress no longer publishes, and report status.
 *
 * @package Viglet_Turing
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Runs over the posts a filter selects, synchronously, batch by batch.
 */
class Viglet_Turing_Operations {

	/**
	 * The plugin settings these operations were built with.
	 *
	 * @var array
	 */
	private array $settings;

	/**
	 * The indexer that builds and sends the documents.
	 *
	 * @var Viglet_Turing_Indexer
	 */
	private Viglet_Turing_Indexer $indexer;

	/**
	 * The client for the calls the indexer does not make.
	 *
	 * @var Viglet_Turing_API_Client
	 */
	private Viglet_Turing_API_Client $client;

	/**
	 * Build the operations over the given settings, or over the stored ones.
	 *
	 * @param array|null $settings The plugin settings; null reads them from the options table.
	 */
	public function __construct( ?array $settings = null ) {
		$this->settings = $settings ?? viglet_turing_get_settings();
		$this->indexer  = new Viglet_Turing_Indexer( $this->settings );
		$this->client   = new Viglet_Turing_API_Client( $this->settings );
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
			'post_type'      => null !== $type && '' !== $type ? array( $type ) : viglet_turing_get_indexable_types(),
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
	 * A batch Turing did not answer in time was still sent, and Turing goes on importing it
	 * (D256): it counts as `unconfirmed`, never as failed.
	 *
	 * @param int[]         $post_ids  The posts.
	 * @param callable|null $progress  Called with the number of posts done after each batch.
	 * @param bool          $send_pdfs Send the PDFs the posts newly hold (D319); false when the
	 *                                 caller sends every PDF afterwards (Index All).
	 * @return array{sent: int, failed: int, errors: array<int, string>, unconfirmed: int}
	 */
	public function index( array $post_ids, ?callable $progress = null, bool $send_pdfs = true ): array {
		$batch_size  = max( 1, (int) $this->settings['batch_size'] );
		$sent        = 0;
		$unconfirmed = 0;
		$errors      = array();

		$this->indexer->provision();

		foreach ( array_chunk( $post_ids, $batch_size ) as $chunk ) {
			$result       = $this->send_batch( $chunk, $send_pdfs );
			$sent        += $result['sent'];
			$unconfirmed += $result['unconfirmed'];
			$errors      += $result['errors'];

			if ( null !== $progress ) {
				$progress( count( $chunk ) );
			}
		}//end foreach

		Viglet_Turing_Events::record(
			'index',
			empty( $errors ),
			Viglet_Turing_Events::note( 'bulk', array( $sent, count( $errors ) ) )
		);

		return array(
			'sent'        => $sent,
			'failed'      => count( $errors ),
			'errors'      => $errors,
			'unconfirmed' => $unconfirmed,
		);
	}

	/**
	 * Send one batch of posts in one import: a create for each one that is indexable, a delete
	 * for the rest. Records no event; the caller says what the whole run did.
	 *
	 * Turing answers a malformed body with 400 (T1062), failing the batch for the sake of one
	 * post, so a batch it refused is sent again one post at a time and the failure lands on the
	 * post that caused it. A transport error or a 5xx is the server, not a post: the batch fails
	 * as a whole and `outage` says so, for a caller that retries it later.
	 *
	 * @param int[] $post_ids  The posts, at most one batch.
	 * @param bool  $send_pdfs Send the PDFs the posts newly hold (D319).
	 * @param bool  $migrate   Also remove each post's copy under the id used before the site key
	 *                         (D148), as Index All always has.
	 * @return array{sent: int, errors: array<int, string>, unconfirmed: int, outage: bool, message: string|array} `message` is the client's note when the batch failed.
	 */
	public function send_batch( array $post_ids, bool $send_pdfs = true, bool $migrate = false ): array {
		$items   = array();
		$kept    = array();
		$removed = array();
		foreach ( $post_ids as $post_id ) {
			$post = get_post( $post_id );
			if ( $post instanceof WP_Post && $this->indexer->should_index( $post ) ) {
				$items[] = $this->indexer->build_create_item( $post );
				$kept[]  = $post;
			} else {
				$items[]   = $this->indexer->build_delete_item( (int) $post_id );
				$removed[] = (int) $post_id;
			}
			if ( $migrate ) {
				// Deleting an id that is not there is a no-op.
				$items[] = $this->indexer->build_legacy_delete_item( (int) $post_id );
			}
		}

		$outcome = array(
			'sent'        => 0,
			'errors'      => array(),
			'unconfirmed' => 0,
			'outage'      => false,
			'message'     => '',
		);
		if ( empty( $items ) ) {
			return $outcome;
		}

		$result = $this->client->import( $items );
		if ( $result['success'] ) {
			$outcome['sent'] = count( $post_ids );
			// The PDFs the posts attach or link follow them in or out (D319).
			( new Viglet_Turing_Pdf( $this->settings ) )->settle( $kept, $removed, $send_pdfs );
			return $outcome;
		}
		if ( ! empty( $result['timed_out'] ) ) {
			// D256: Turing goes on importing a batch it did not answer in time.
			$outcome['unconfirmed'] = count( $post_ids );
			return $outcome;
		}

		$code    = (int) $result['code'];
		$refused = $code >= 400 && $code < 500;
		if ( $refused && count( $post_ids ) > 1 ) {
			foreach ( $post_ids as $post_id ) {
				$single                  = $this->send_batch( array( $post_id ), $send_pdfs, $migrate );
				$outcome['sent']        += $single['sent'];
				$outcome['unconfirmed'] += $single['unconfirmed'];
				$outcome['errors']      += $single['errors'];
			}
			return $outcome;
		}

		foreach ( $post_ids as $post_id ) {
			$outcome['errors'][ (int) $post_id ] = (string) $result['message'];
		}
		$outcome['outage']  = ! $refused;
		$outcome['message'] = $result['note'];

		return $outcome;
	}

	/**
	 * The ids Turing holds under this site key that WordPress does not publish as indexable.
	 *
	 * Only this plugin's ids, `<key>:post:<ID>`, are ever orphans (D322): an id of another shape
	 * under the same key belongs to another source (a Dumont connector given this site key to
	 * share the search), and the plugin never deletes what it did not send.
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
		$pdf       = new Viglet_Turing_Pdf( $this->settings );

		foreach ( $held as $doc_id ) {
			if ( ! str_starts_with( $doc_id, $prefix ) ) {
				continue;
			}
			$post_id = substr( $doc_id, strlen( $prefix ) );
			// A PDF in search (D309) is an attachment, never among the published posts.
			if ( ctype_digit( $post_id ) && $pdf->indexable( get_post( (int) $post_id ) ) ) {
				continue;
			}
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
				'message' => __( 'Viglet Turing could not be searched for this site\'s documents.', 'viglet-turing-search' ),
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
				Viglet_Turing_Events::record( 'sweep', false, $result['note'] );
				return array(
					'success' => false,
					'orphans' => $orphans,
					'message' => $result['message'],
				);
			}
		}

		Viglet_Turing_Events::record( 'sweep', true, Viglet_Turing_Events::note( 'swept', array( count( $orphans ) ) ) );

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
	 * @return array{sent: int, failed: int, errors: array<int, string>, unconfirmed: int, swept: int, sweep_error: string}
	 */
	public function reindex( ?callable $progress = null ): array {
		$this->indexer->provision( true );
		// Each post records the PDFs it links on the way (D319); all of them are sent below.
		$result = $this->index( $this->find_post_ids(), $progress, false );
		// The PDFs of the posts in search (D309), once the posts that hold them are there.
		$pdfs = ( new Viglet_Turing_Pdf( $this->settings ) )->index_all();
		if ( ! $pdfs['success'] ) {
			++$result['failed'];
			$result['errors'][] = $pdfs['message'];
		}

		// A failed index would read as orphans every post it did not reach; sweep only after a clean run.
		$sweep = $result['failed'] > 0
			? array(
				'success' => false,
				'orphans' => array(),
				'message' => __( 'Skipped: the reindex had failures.', 'viglet-turing-search' ),
			)
			: $this->sweep();

		return $result + array(
			'swept'       => count( $sweep['orphans'] ),
			'sweep_error' => $sweep['success'] ? '' : $sweep['message'],
		);
	}

	/**
	 * The worker's last run that sent anything, with its moment as the site writes it.
	 *
	 * @return array{at: int, sent: int, failed: int, when: string}|null
	 */
	private function last_run(): ?array {
		$run = get_option( Viglet_Turing_Queue::LAST_RUN_OPTION, null );
		if ( ! is_array( $run ) ) {
			return null;
		}

		return $run + array( 'when' => Viglet_Turing_Events::when( (int) ( $run['at'] ?? 0 ) ) );
	}

	/**
	 * The connection, the SN site, published versus indexed documents per type, and the queue.
	 *
	 * @return array{connection: array, site_name: string, site_key: string, types: array<string, array{published: int, indexed: int|null}>, queue: array, last_run: mixed, events: array, sources: array, provisioned: mixed, foreign: int|null, reindex: array, ai_quota: array|null} Per type, `published` is what WordPress publishes for the index and `indexed` what Turing holds; null when it cannot say.
	 */
	public function status(): array {
		$types = array();
		foreach ( viglet_turing_get_indexable_types() as $type ) {
			$types[ $type ] = array(
				'published' => $this->count_indexable( $type ),
				'indexed'   => $this->count_in_turing( $type ),
			);
		}

		return array(
			'connection'  => $this->client->ping(),
			'site_name'   => $this->client->get_site_name(),
			'site_key'    => $this->indexer->site_key(),
			'types'       => $types,
			'queue'       => ( new Viglet_Turing_Queue( $this->settings ) )->counts(),
			'last_run'    => $this->last_run(),
			'events'      => Viglet_Turing_Events::all(),
			'sources'     => Viglet_Turing_Settings::sources(),
			'provisioned' => get_option( Viglet_Turing_Mapping::PROVISIONED_OPTION, null ),
			'foreign'     => $this->count_foreign(),
			// The Index All running in the background and the last one that ended (D331).
			'reindex'     => ( new Viglet_Turing_Reindex( $this->settings ) )->report(),
			// The site's AI budget in Turing, while it is known to be spent (D332).
			'ai_quota'    => Viglet_Turing_AI_Quota::report(),
		);
	}

	/**
	 * How many documents under this site key another source sent (D322): every one the key
	 * holds less the ones carrying this plugin's `source_apps`. The plugin's deletes leave them
	 * alone, and Health says they are there. Null when Turing cannot say.
	 */
	private function count_foreign(): ?int {
		$all  = $this->client->search( '*', 1, 0, array( 'fq[]' => $this->indexer->own_filter() ) );
		$mine = $this->client->search(
			'*',
			1,
			0,
			array( 'fq[]' => array( $this->indexer->own_filter(), 'source_apps:' . $this->client->source_app() ) )
		);
		if ( is_wp_error( $all ) || is_wp_error( $mine ) ) {
			return null;
		}

		return max( 0, (int) ( $all['queryContext']['count'] ?? 0 ) - (int) ( $mine['queryContext']['count'] ?? 0 ) );
	}

	/**
	 * How many published posts of a type the indexer would send. A post kept out of the index (by
	 * its own checkbox, the excluded ids or an excluded type) is never in Turing, so counting it
	 * would report a difference no reindex can close (D284).
	 *
	 * @param string $type The post type.
	 */
	private function count_indexable( string $type ): int {
		$count = 0;
		foreach ( $this->find_post_ids( $type ) as $post_id ) {
			$post = get_post( $post_id );
			if ( $post instanceof WP_Post && $this->indexer->should_index( $post ) ) {
				++$count;
			}
		}

		return $count;
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
			array( 'fq[]' => array( $this->indexer->own_filter(), 'type:' . $type ) )
		);

		return is_wp_error( $response ) ? null : (int) ( $response['queryContext']['count'] ?? 0 );
	}
}
