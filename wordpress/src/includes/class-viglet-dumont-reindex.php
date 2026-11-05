<?php
/**
 * Viglet Dumont Reindex
 *
 * Index All from wp-admin, run by WP-Cron rather than by the browser tab that asked for it: the
 * tab can close, a batch can fail, and the run goes on.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * One reindex at a time, its progress in a non-autoloaded option.
 *
 * The run is `{id, type, state, phase, offset, total, sent, failed, unconfirmed, attempts,
 * next_at, error, started_at, ticked_at, finished_at, pdfs, swept, sweep_error}`. Its post ids,
 * read once when it starts, live in an option of their own that is never rewritten, so each
 * batch rewrites only the small one. `offset` is how many of them are done.
 *
 * The phases are `posts`, then for a full reindex `pdfs` and `sweep`, as
 * Viglet_Dumont_Operations::reindex() does them. Each WP-Cron run takes a few steps (a step
 * is one batch of posts, the PDFs or the sweep) and schedules the next run. A step that fails
 * waits out a backoff and is tried again; after MAX_ATTEMPTS its posts count as failed and the
 * run moves on, so one bad stretch never stops it. When it ends, the run moves to the last
 * finished one, with its state: done, failed (it ended with failures) or cancelled.
 */
class Viglet_Dumont_Reindex {

	/** The option holding the running reindex. */
	public const OPTION = 'viglet_dumont_reindex';

	/** The option holding the running reindex's post ids, written once when it starts. */
	public const IDS_OPTION = 'viglet_dumont_reindex_ids';

	/** The option holding the last reindex that ended, however it ended. */
	public const LAST_OPTION = 'viglet_dumont_reindex_last';

	/** The WP-Cron hook a run takes its next steps on. */
	public const CRON_HOOK = 'viglet_dumont_reindex';

	/** The transient that keeps two runs from sending the same batch. */
	public const LOCK = 'viglet_dumont_reindex_lock';

	/** Steps one run takes at most, before it hands over to the next. */
	public const STEPS_PER_RUN = 3;

	/** Seconds one run spends at most, well inside a shared host's execution limit. */
	public const TIME_BUDGET = 20;

	/** Tries a step gets before its posts count as failed and the run moves on. */
	public const MAX_ATTEMPTS = 5;

	/** Backoff after a step's first failure, doubled on each one after. */
	public const BASE_DELAY = 30;

	/** The longest wait between two tries of a step. */
	public const MAX_DELAY = 600;

	/** With nothing done for this long while it should have been, the run is said to be stalled. */
	public const STALL_AFTER = 10 * MINUTE_IN_SECONDS;

	/**
	 * The plugin settings this reindex was built with.
	 *
	 * @var array
	 */
	private array $settings;

	/**
	 * The operations that send a batch, the PDFs and the sweep.
	 *
	 * @var Viglet_Dumont_Operations
	 */
	private Viglet_Dumont_Operations $operations;

	/**
	 * Build a reindex over the given settings, or over the stored ones.
	 *
	 * @param array|null $settings The plugin settings; null reads them from the options table.
	 */
	public function __construct( ?array $settings = null ) {
		$this->settings   = $settings ?? viglet_dumont_get_settings();
		$this->operations = new Viglet_Dumont_Operations( $this->settings );
	}

	/**
	 * Start a reindex of every indexed type, or of one, and leave the rest to WP-Cron.
	 *
	 * @param string $type `all`, or one of the indexed post types.
	 * @return array|WP_Error The run as report() shows it; an error when one is running already
	 *                        (`viglet_dumont_reindex_running`), the type is not indexed, or demo mode is on.
	 */
	public function start( string $type = 'all' ) {
		if ( Viglet_Dumont_Settings::demo() ) {
			return Viglet_Dumont_API_Client::demo_refusal();
		}
		if ( null !== $this->current() ) {
			return new WP_Error( 'viglet_dumont_reindex_running', __( 'A reindex is already running.', 'viglet-turing-search' ), array( 'status' => 409 ) );
		}
		if ( 'all' !== $type && ! in_array( $type, viglet_dumont_get_indexable_types(), true ) ) {
			return new WP_Error(
				'viglet_dumont_reindex_type',
				/* translators: %s: the post type asked for. */
				sprintf( __( 'Posts of type %s are not indexed.', 'viglet-turing-search' ), $type ),
				array( 'status' => 400 )
			);
		}

		$ids = $this->operations->find_post_ids( 'all' === $type ? null : $type );
		update_option( self::IDS_OPTION, $ids, false );

		$run = array(
			'id'          => uniqid( '', true ),
			'type'        => $type,
			'state'       => 'running',
			'phase'       => 'posts',
			'offset'      => 0,
			'total'       => count( $ids ),
			'sent'        => 0,
			'failed'      => 0,
			'unconfirmed' => 0,
			'attempts'    => 0,
			'next_at'     => 0,
			'error'       => '',
			'started_at'  => time(),
			'ticked_at'   => 0,
			'finished_at' => 0,
			'pdfs'        => 0,
			'swept'       => 0,
			'sweep_error' => '',
			// What a loopback step must show (D335): only this site's own requests carry it.
			'token'       => wp_generate_password( 32, false ),
		);
		update_option( self::OPTION, $run, false );
		$this->schedule( time() );
		$this->kick( $run );

		return $this->view( $run );
	}

	/**
	 * Whether a step request may advance the running reindex (D335): it carries the run's own
	 * token, which only this site's loopback knows.
	 *
	 * @param string $token The token the request carries.
	 */
	public function accepts( string $token ): bool {
		$run = $this->current();

		return null !== $run && '' !== $token && hash_equals( (string) ( $run['token'] ?? '' ), $token );
	}

	/**
	 * Keep a due run moving without traffic (D335): WP-Cron only runs on a page view and never
	 * from inside a cron run, so the reindex asks this site for its own next step, without
	 * waiting for the answer. A host that blocks loopbacks still has WP-Cron and the Health
	 * page, which steps a run it sees idle.
	 *
	 * @param array $run The running reindex.
	 */
	private function kick( array $run ): void {
		if ( 'running' !== $run['state'] || (int) $run['next_at'] > time() || '' === (string) ( $run['token'] ?? '' ) ) {
			return;
		}
		/**
		 * Filters whether the background reindex asks this site for its next step (D335).
		 *
		 * @param bool $kick True by default.
		 */
		if ( ! apply_filters( 'viglet_dumont_reindex_loopback', true ) ) {
			return;
		}
		/**
		 * Filters the URL the reindex asks for its next step, as `cron_request` does for
		 * WP-Cron: a site whose own address does not reach it from the server (a container
		 * behind a mapped port) points it at one that does.
		 *
		 * @param string $url The REST route on the site's own address.
		 */
		$url = (string) apply_filters( 'viglet_dumont_reindex_step_url', rest_url( 'viglet-dumont/v1/reindex/step' ) );
		wp_remote_post(
			$url,
			array(
				'blocking'  => false,
				'timeout'   => 0.01,
				'body'      => array( 'token' => (string) $run['token'] ),
				/**
				 * Filters whether the loopback verifies the site's certificate (D335). On by
				 * default; a site on a self-signed certificate turns it off here, as core's
				 * loopbacks read `https_local_ssl_verify`.
				 *
				 * @param bool $verify True by default.
				 */
				'sslverify' => (bool) apply_filters( 'viglet_dumont_reindex_step_sslverify', true ),
			)
		);
	}

	/**
	 * Stop the running reindex where it stands. What it sent stays sent.
	 *
	 * @return bool Whether one was running.
	 */
	public function cancel(): bool {
		$run = $this->current();
		if ( null === $run ) {
			return false;
		}
		Viglet_Dumont_Events::record( 'reindex', false, Viglet_Dumont_Events::note( 'reindex_cancelled', array( (int) $run['offset'], (int) $run['total'] ) ) );
		$this->finish( $run, 'cancelled' );

		return true;
	}

	/**
	 * Schedule the running reindex again, as after the plugin was turned off and on.
	 */
	public function resume(): void {
		$run = $this->current();
		if ( null !== $run ) {
			$this->schedule( max( time(), (int) $run['next_at'] ) );
		}
	}

	/**
	 * The WP-Cron run: take the next few steps, then schedule the run after.
	 *
	 * @return array|null The run as it stands after, as report() shows it; null when none is running.
	 */
	public function tick(): ?array {
		if ( get_transient( self::LOCK ) ) {
			$run = $this->current();
			return null === $run ? null : $this->view( $run );
		}
		set_transient( self::LOCK, 1, 5 * MINUTE_IN_SECONDS );

		$began = microtime( true );
		try {
			for ( $step = 0; $step < self::STEPS_PER_RUN; $step++ ) {
				$run = $this->current();
				if ( microtime( true ) - $began >= self::TIME_BUDGET ) {
					break;
				}
				if ( null === $run || (int) $run['next_at'] > time() ) {
					break;
				}
				$waits            = ! $this->step( $run );
				$run['ticked_at'] = time();
				// Cancelled while the step was being sent: what was cancelled stays cancelled.
				if ( ! $this->still_running( $run['id'] ) ) {
					break;
				}
				if ( in_array( $run['state'], array( 'done', 'failed' ), true ) ) {
					$this->finish( $run, $run['state'] );
					break;
				}
				update_option( self::OPTION, $run, false );
				if ( $waits ) {
					break;
				}
			}//end for
		} finally {
			delete_transient( self::LOCK );
		}//end try

		$run = $this->current();
		if ( null === $run ) {
			return null;
		}
		$this->schedule( max( time(), (int) $run['next_at'] ) );
		// The next steps are due now: ask for them rather than wait for a visitor (D335).
		$this->kick( $run );

		return $this->view( $run );
	}

	/**
	 * The running reindex and the last one that ended, for Health and the Settings screen.
	 *
	 * @return array{running: array|null, last: array|null}
	 */
	public function report(): array {
		$run  = $this->current();
		$last = get_option( self::LAST_OPTION, null );

		return array(
			'running' => null === $run ? null : $this->view( $run ),
			'last'    => is_array( $last ) ? $this->view( $last ) : null,
		);
	}

	/**
	 * The running reindex as stored, or null.
	 */
	private function current(): ?array {
		// A run re-reads after a batch that may have taken seconds: never trust the request cache.
		wp_cache_delete( self::OPTION, 'options' );
		$run = get_option( self::OPTION, null );

		return is_array( $run ) && 'running' === ( $run['state'] ?? '' ) ? $run : null;
	}

	/**
	 * Whether the run with this id is still the running one.
	 *
	 * @param string $id The run's id.
	 */
	private function still_running( string $id ): bool {
		$run = $this->current();

		return null !== $run && $run['id'] === $id;
	}

	/**
	 * Take one step of the run.
	 *
	 * @param array $run The run, changed in place.
	 * @return bool False when the step failed and waits out a backoff.
	 */
	private function step( array &$run ): bool {
		switch ( $run['phase'] ) {
			case 'posts':
				return $this->step_posts( $run );
			case 'pdfs':
				$pdfs = ( new Viglet_Dumont_Pdf( $this->settings ) )->index_all();
				if ( ! $pdfs['success'] ) {
					return $this->failed_step( $run, $pdfs['message'], 1 );
				}
				$run['pdfs']     = (int) $pdfs['sent'];
				$run['phase']    = 'sweep';
				$run['error']    = $run['attempts'] > 0 ? '' : $run['error'];
				$run['attempts'] = 0;
				return true;
			case 'sweep':
				// A run with failures would read as orphans every post it did not reach.
				if ( $run['failed'] > 0 ) {
					$run['sweep_error'] = __( 'Skipped: the reindex had failures.', 'viglet-turing-search' );
				} else {
					$sweep              = $this->operations->sweep();
					$run['swept']       = count( $sweep['orphans'] );
					$run['sweep_error'] = $sweep['success'] ? '' : $sweep['message'];
				}
				$this->end( $run );
				return true;
			default:
				$this->end( $run );
				return true;
		}//end switch
	}

	/**
	 * Send the next batch of posts, or move past the posts when none are left.
	 *
	 * @param array $run The run, changed in place.
	 * @return bool False when the batch failed and waits out a backoff.
	 */
	private function step_posts( array &$run ): bool {
		$batch_size = max( 1, (int) $this->settings['batch_size'] );
		$ids        = get_option( self::IDS_OPTION, array() );
		$chunk      = array_slice( is_array( $ids ) ? $ids : array(), (int) $run['offset'], $batch_size );

		if ( array() === $chunk ) {
			// A full reindex sends every PDF and sweeps after the posts; one type's ends here.
			if ( 'all' === $run['type'] ) {
				$run['phase'] = 'pdfs';
			} else {
				$this->end( $run );
			}
			return true;
		}

		if ( 0 === (int) $run['offset'] ) {
			// The fields reach Turing before the documents that carry them, on each try of the
			// first batch, since a Turing that refused that batch refused the fields too.
			( new Viglet_Dumont_Indexer( $this->settings ) )->provision( 'all' === $run['type'] );
		}

		// A full reindex sends every PDF at the end; one type's sends the ones its posts hold.
		$result = $this->operations->send_batch( $chunk, 'all' !== $run['type'], true );
		if ( $result['outage'] ) {
			return $this->failed_step( $run, $result['message'], count( $chunk ) );
		}

		$run['offset']      += count( $chunk );
		$run['sent']        += $result['sent'];
		$run['unconfirmed'] += $result['unconfirmed'];
		$run['failed']      += count( $result['errors'] );
		if ( array() !== $result['errors'] ) {
			$run['error'] = (string) reset( $result['errors'] );
		} elseif ( $run['attempts'] > 0 ) {
			// The outage is over: what it said no longer describes the run.
			$run['error'] = '';
		}
		$run['attempts'] = 0;
		$run['next_at']  = 0;

		return true;
	}

	/**
	 * A step that failed: back off and try again, or after the last try count it and move on.
	 *
	 * @param array        $run     The run, changed in place.
	 * @param string|array $message Why it failed, as text or an event note.
	 * @param int          $count   What it counts as failed when it gives up: its posts, or 1 for the PDFs.
	 * @return bool False when the step waits out a backoff, true when the run moved on.
	 */
	private function failed_step( array &$run, $message, int $count ): bool {
		$run['error']    = Viglet_Dumont_Events::text( $message );
		$run['attempts'] = (int) $run['attempts'] + 1;

		if ( $run['attempts'] < self::MAX_ATTEMPTS ) {
			$run['next_at'] = time() + (int) min( self::MAX_DELAY, self::BASE_DELAY * ( 2 ** ( $run['attempts'] - 1 ) ) );
			return false;
		}

		$run['failed']  += $count;
		$run['attempts'] = 0;
		$run['next_at']  = 0;
		if ( 'posts' === $run['phase'] ) {
			$run['offset'] += $count;
		} else {
			$run['phase'] = 'sweep';
		}

		return true;
	}

	/**
	 * Mark the run as ended, done or with failures, and say so in the events.
	 *
	 * @param array $run The run, changed in place.
	 */
	private function end( array &$run ): void {
		$run['state'] = $run['failed'] > 0 ? 'failed' : 'done';
		Viglet_Dumont_Events::record( 'reindex', 'done' === $run['state'], Viglet_Dumont_Events::note( 'reindexed', array( (int) $run['sent'], (int) $run['failed'] ) ) );
	}

	/**
	 * Keep the run as the last one that ended, and clear the running one.
	 *
	 * @param array  $run   The run.
	 * @param string $state How it ended: done, failed or cancelled.
	 */
	private function finish( array $run, string $state ): void {
		$run['state']       = $state;
		$run['finished_at'] = time();
		$run['next_at']     = 0;
		update_option( self::LAST_OPTION, $run, false );
		delete_option( self::OPTION );
		delete_option( self::IDS_OPTION );
		wp_clear_scheduled_hook( self::CRON_HOOK );
	}

	/**
	 * A run as Health and Settings read it: its fields, its moments as the site writes them, and
	 * whether WP-Cron stopped advancing it.
	 *
	 * @param array $run The run as stored.
	 */
	private function view( array $run ): array {
		$moving = max( (int) $run['started_at'], (int) $run['ticked_at'], (int) $run['next_at'] );
		// The loopback's token never leaves the server.
		unset( $run['token'] );

		return array(
			'started'  => Viglet_Dumont_Events::when( (int) $run['started_at'] ),
			'finished' => Viglet_Dumont_Events::when( (int) $run['finished_at'] ),
			'retry_at' => (int) $run['next_at'] > time() ? Viglet_Dumont_Events::when( (int) $run['next_at'] ) : '',
			'stalled'  => 'running' === $run['state'] && time() - $moving > self::STALL_AFTER,
			// Seconds a due run has gone without a step, for Health to step it (D335).
			'idle'     => 'running' === $run['state'] && (int) $run['next_at'] <= time() ? max( 0, time() - $moving ) : 0,
		) + $run;
	}

	/**
	 * Schedule one run at the given time, unless one is already due sooner.
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
