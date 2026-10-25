<?php
/**
 * Viglet Dumont Queue
 *
 * Sits between the editor and Turing: a save only records what the post needs, and a
 * WP-Cron worker (or `wp dumont queue run`) sends it in batches with retry and backoff.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * One de-duplicated job per post, kept in a non-autoloaded option.
 *
 * A job is `{action, queued_at, attempts, next_at, error, failed}`, keyed by post id, so the
 * last save wins. Permanently deleted posts have no meta to hold a job, which is why the
 * queue is an option and not post meta; the per-post state of a post that still exists
 * (last indexed at, last error, attempts) is post meta.
 */
class Viglet_Dumont_Queue {

	/** The option the jobs live in. */
	public const OPTION = 'viglet_dumont_queue';

	/** The post meta that records how the post last fared. */
	public const STATE_META = '_viglet_dumont_index_state';

	/** The option recording the worker's last run that sent anything: when, sent, failed. */
	public const LAST_RUN_OPTION = 'viglet_dumont_last_run';

	/** The WP-Cron hook the worker runs on. */
	public const CRON_HOOK = 'viglet_dumont_process_queue';

	/** The transient that keeps two workers from sending the same jobs. */
	public const LOCK = 'viglet_dumont_queue_lock';

	/** After this many failed sends a job stops retrying on its own and waits for a replay. */
	public const MAX_ATTEMPTS = 8;

	/** Backoff after the first failure, doubled on each one after. */
	public const BASE_DELAY = 60;

	/** The longest wait between two attempts. */
	public const MAX_DELAY = 3600;

	/**
	 * The plugin settings this queue was built with.
	 *
	 * @var array
	 */
	private array $settings;

	/**
	 * Build a queue over the given settings, or over the stored ones.
	 *
	 * @param array|null $settings The plugin settings; null reads them from the options table.
	 */
	public function __construct( ?array $settings = null ) {
		$this->settings = $settings ?? viglet_dumont_get_settings();
	}

	/**
	 * Record that a post must be indexed or removed, replacing any job it already has.
	 *
	 * @param int    $post_id The post.
	 * @param string $action  `index` or `delete`.
	 */
	public function enqueue( int $post_id, string $action ): void {
		$jobs             = $this->jobs();
		$jobs[ $post_id ] = array(
			'action'    => 'delete' === $action ? 'delete' : 'index',
			'queued_at' => microtime( true ),
			'attempts'  => 0,
			'next_at'   => 0,
			'error'     => '',
			'failed'    => false,
		);
		$this->save( $jobs );
		$this->schedule( time() );
	}

	/**
	 * Every job, keyed by post id.
	 *
	 * @return array<int, array>
	 */
	public function jobs(): array {
		// The worker re-reads after a send that may have taken seconds: never trust the request cache.
		wp_cache_delete( self::OPTION, 'options' );
		$jobs = get_option( self::OPTION, array() );

		return is_array( $jobs ) ? $jobs : array();
	}

	/**
	 * How many jobs wait, and how many stopped retrying.
	 *
	 * @return array{pending: int, failed: int}
	 */
	public function counts(): array {
		$jobs   = $this->jobs();
		$failed = count( array_filter( $jobs, static fn ( $job ) => ! empty( $job['failed'] ) ) );

		return array(
			'pending' => count( $jobs ) - $failed,
			'failed'  => $failed,
		);
	}

	/**
	 * Send the jobs that are due, at most one batch, and record how each one fared.
	 *
	 * @param bool $ignore_backoff Send jobs still waiting out a backoff (the WP-CLI runner).
	 * @return array{sent: int, failed: int, errors: array<int, string>, remaining: int, locked: bool} `errors` is the failure per post id.
	 */
	public function process( bool $ignore_backoff = false ): array {
		if ( get_transient( self::LOCK ) ) {
			return array(
				'sent'      => 0,
				'failed'    => 0,
				'errors'    => array(),
				'remaining' => count( $this->jobs() ),
				'locked'    => true,
			);
		}
		set_transient( self::LOCK, 1, 5 * MINUTE_IN_SECONDS );

		try {
			$batch = $this->due( $ignore_backoff );
			$items = array();
			foreach ( $batch as $post_id => $job ) {
				$items[ $post_id ] = $this->build_item( (int) $post_id, $job['action'] );
			}
			$items = array_filter( $items );

			// A mapping change reaches Turing before the documents that carry the new fields.
			if ( ! empty( $items ) ) {
				( new Viglet_Dumont_Indexer( $this->settings ) )->provision();
			}

			$outcomes = $this->send( $items );

			// Jobs that resolved to nothing (an index job for a post that is not indexable while
			// deindexing is off) are done without a call.
			foreach ( array_diff_key( $batch, $items ) as $post_id => $job ) {
				$outcomes[ $post_id ] = '';
			}

			$this->record( $batch, $outcomes );
		} finally {
			delete_transient( self::LOCK );
		}//end try

		$remaining = $this->jobs();
		$this->schedule_next( $remaining );

		$errors = array_filter( $outcomes, static fn ( $error ) => '' !== $error );

		// The health page's "last run": only a run that sent something, so an idle tick does not hide the last real one.
		if ( ! empty( $outcomes ) ) {
			update_option(
				self::LAST_RUN_OPTION,
				array(
					'at'     => time(),
					'sent'   => count( $outcomes ) - count( $errors ),
					'failed' => count( $errors ),
				),
				false
			);
		}

		return array(
			'sent'      => count( $outcomes ) - count( $errors ),
			'failed'    => count( $errors ),
			'errors'    => $errors,
			'remaining' => count( $remaining ),
			'locked'    => false,
		);
	}

	/**
	 * Put every job that stopped retrying back in line.
	 *
	 * @return int How many were replayed.
	 */
	public function retry_failed(): int {
		$jobs     = $this->jobs();
		$replayed = 0;
		foreach ( $jobs as $post_id => $job ) {
			if ( ! empty( $job['failed'] ) ) {
				$jobs[ $post_id ] = array_merge(
					$job,
					array(
						'attempts' => 0,
						'next_at'  => 0,
						'failed'   => false,
					)
				);
				++$replayed;
			}
		}
		if ( $replayed > 0 ) {
			$this->save( $jobs );
			$this->schedule( time() );
		}

		return $replayed;
	}

	/**
	 * The jobs to send now, oldest first, at most one batch.
	 *
	 * @param bool $ignore_backoff Take jobs still waiting out a backoff.
	 * @return array<int, array>
	 */
	private function due( bool $ignore_backoff ): array {
		$now  = time();
		$jobs = array_filter(
			$this->jobs(),
			static fn ( $job ) => empty( $job['failed'] ) && ( $ignore_backoff || (int) $job['next_at'] <= $now )
		);
		uasort( $jobs, static fn ( $a, $b ) => $a['queued_at'] <=> $b['queued_at'] );

		$batch_size = (int) $this->settings['batch_size'];

		return array_slice( $jobs, 0, $batch_size > 0 ? $batch_size : 50, true );
	}

	/**
	 * The job item a post's job becomes, read from the post as it is now.
	 *
	 * An index job for a post that is gone or no longer indexable becomes a delete, so the
	 * index ends up matching the post whatever happened between the save and the send.
	 *
	 * @param int    $post_id The post.
	 * @param string $action  What the job asked for.
	 * @return array|null The item, or null when nothing is to be sent.
	 */
	private function build_item( int $post_id, string $action ): ?array {
		$indexer = new Viglet_Dumont_Indexer( $this->settings );

		if ( 'index' === $action ) {
			$post = get_post( $post_id );
			if ( $post instanceof WP_Post && $indexer->should_index( $post ) ) {
				return $indexer->build_create_item( $post );
			}
			if ( empty( $this->settings['auto_deindex'] ) ) {
				return null;
			}
		}

		return $indexer->build_delete_item( $post_id );
	}

	/**
	 * Send the items in one import and say, per post, what went wrong ('' for nothing).
	 *
	 * Turing answers a malformed body with 400 (T1062), which would fail every item in the
	 * batch for the sake of one. So a batch the server refused is sent again one item at a
	 * time, and the failure lands on the post that caused it. A transport error or a 5xx is
	 * the server, not an item, and the batch fails as a whole.
	 *
	 * @param array<int, array> $items The items, keyed by post id.
	 * @return array<int, string>
	 */
	private function send( array $items ): array {
		if ( empty( $items ) ) {
			return array();
		}

		$client = new Viglet_Dumont_API_Client( $this->settings );
		$result = $client->import( array_values( $items ) );
		if ( $result['success'] ) {
			return array_fill_keys( array_keys( $items ), '' );
		}

		$refused = $result['code'] >= 400 && $result['code'] < 500;
		if ( ! $refused || count( $items ) === 1 ) {
			return array_fill_keys( array_keys( $items ), $result['message'] );
		}

		$outcomes = array();
		foreach ( $items as $post_id => $item ) {
			$single               = $client->import( array( $item ) );
			$outcomes[ $post_id ] = $single['success'] ? '' : $single['message'];
		}

		return $outcomes;
	}

	/**
	 * Drop the jobs that went through and back off the ones that did not.
	 *
	 * The queue is read again here because a save may have replaced a job while it was being
	 * sent; a job whose `queued_at` changed is newer than what was sent and is left alone.
	 *
	 * @param array<int, array>  $batch    The jobs as they were sent.
	 * @param array<int, string> $outcomes The error per post, '' for success.
	 */
	private function record( array $batch, array $outcomes ): void {
		$jobs = $this->jobs();
		$now  = time();

		foreach ( $outcomes as $post_id => $error ) {
			$sent    = $batch[ $post_id ];
			$current = $jobs[ $post_id ] ?? null;
			$same    = null !== $current && $current['queued_at'] === $sent['queued_at'];

			if ( '' === $error ) {
				if ( $same ) {
					unset( $jobs[ $post_id ] );
				}
				$this->write_state( (int) $post_id, $sent['action'], '', 0, $now );
				continue;
			}

			$attempts = (int) $sent['attempts'] + 1;
			if ( $same ) {
				$jobs[ $post_id ] = array_merge(
					$current,
					array(
						'attempts' => $attempts,
						'next_at'  => $now + $this->delay( $attempts ),
						'error'    => $error,
						'failed'   => $attempts >= self::MAX_ATTEMPTS,
					)
				);
			}
			$this->write_state( (int) $post_id, $sent['action'], $error, $attempts, null );
		}//end foreach

		$this->save( $jobs );
	}

	/**
	 * Record on the post how its last send went. A deleted post has nowhere to hold it.
	 *
	 * @param int      $post_id    The post.
	 * @param string   $action     What was sent.
	 * @param string   $error      The failure, '' for none.
	 * @param int      $attempts   Failed sends so far.
	 * @param int|null $indexed_at When the send went through; null keeps the previous one.
	 */
	private function write_state( int $post_id, string $action, string $error, int $attempts, ?int $indexed_at ): void {
		if ( ! get_post( $post_id ) ) {
			return;
		}

		$state = get_post_meta( $post_id, self::STATE_META, true );
		$state = is_array( $state ) ? $state : array();
		if ( null !== $indexed_at ) {
			$state['indexed_at'] = 'index' === $action ? $indexed_at : null;
		}
		$state['action']   = $action;
		$state['error']    = $error;
		$state['attempts'] = $attempts;

		update_post_meta( $post_id, self::STATE_META, $state );
	}

	/**
	 * Seconds to wait after the given number of failed sends.
	 *
	 * @param int $attempts Failed sends so far.
	 */
	private function delay( int $attempts ): int {
		return (int) min( self::MAX_DELAY, self::BASE_DELAY * ( 2 ** ( $attempts - 1 ) ) );
	}

	/**
	 * Store the jobs, or drop the option when there are none.
	 *
	 * @param array<int, array> $jobs The jobs, keyed by post id.
	 */
	private function save( array $jobs ): void {
		if ( empty( $jobs ) ) {
			delete_option( self::OPTION );
			return;
		}
		update_option( self::OPTION, $jobs, false );
	}

	/**
	 * Ask WP-Cron to run the worker the next time a job is due.
	 *
	 * @param array<int, array> $jobs The jobs left after a run.
	 */
	private function schedule_next( array $jobs ): void {
		$waiting = array_filter( $jobs, static fn ( $job ) => empty( $job['failed'] ) );
		if ( empty( $waiting ) ) {
			return;
		}
		$this->schedule( max( time(), min( array_map( static fn ( $job ) => (int) $job['next_at'], $waiting ) ) ) );
	}

	/**
	 * Schedule one worker run at the given time, unless one is already due sooner.
	 *
	 * @param int $timestamp When to run.
	 */
	private function schedule( int $timestamp ): void {
		$next = wp_next_scheduled( self::CRON_HOOK );
		if ( false !== $next && $next <= $timestamp ) {
			return;
		}
		if ( false !== $next ) {
			wp_unschedule_event( $next, self::CRON_HOOK );
		}
		wp_schedule_single_event( $timestamp, self::CRON_HOOK );
	}
}
