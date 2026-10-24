<?php
/**
 * Viglet Dumont WP-CLI commands
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Drains the save queue from the command line, for hosts that disable WP-Cron.
 */
class Viglet_Dumont_CLI_Queue {

	/**
	 * Send every queued job now, batch by batch, ignoring backoff.
	 *
	 * Stops at the first batch that has a failure, so an outage costs one batch and not every
	 * remaining attempt, and exits non-zero when anything failed.
	 *
	 * ## OPTIONS
	 *
	 * [--retry-failed]
	 * : Put the jobs that stopped retrying back in line first.
	 *
	 * ## EXAMPLES
	 *
	 *     wp dumont queue run
	 *     wp dumont queue run --retry-failed
	 *
	 * @param array $args       Positional arguments (none).
	 * @param array $assoc_args `retry-failed`.
	 */
	public function run( array $args, array $assoc_args ): void {
		$queue = new Viglet_Dumont_Queue();

		if ( ! empty( $assoc_args['retry-failed'] ) ) {
			/* translators: %d: how many failed jobs were put back in line. */
			WP_CLI::log( sprintf( __( 'Replaying %d failed jobs.', 'viglet-dumont' ), $queue->retry_failed() ) );
		}

		$sent = 0;
		do {
			$result = $queue->process( true );
			if ( $result['locked'] ) {
				WP_CLI::error( __( 'Another worker is sending the queue; try again in a few minutes.', 'viglet-dumont' ) );
			}
			$sent += $result['sent'];
		} while ( 0 === $result['failed'] && $queue->counts()['pending'] > 0 );

		$counts = $queue->counts();
		/* translators: 1: jobs sent, 2: jobs still waiting, 3: jobs that stopped retrying. */
		$summary = sprintf( __( '%1$d sent, %2$d waiting, %3$d stopped retrying.', 'viglet-dumont' ), $sent, $counts['pending'], $counts['failed'] );

		if ( $result['failed'] > 0 ) {
			foreach ( $result['errors'] as $post_id => $error ) {
				/* translators: 1: the post id, 2: why sending it failed. */
				WP_CLI::warning( sprintf( __( 'Post %1$d: %2$s', 'viglet-dumont' ), $post_id, $error ) );
			}
			/* translators: 1: how many jobs failed, 2: the queue summary. */
			WP_CLI::error( sprintf( __( 'Stopped after %1$d failed. %2$s', 'viglet-dumont' ), $result['failed'], $summary ) );
		}
		WP_CLI::success( $summary );
	}

	/**
	 * Show how many jobs wait and which ones failed, with their last error.
	 *
	 * ## EXAMPLES
	 *
	 *     wp dumont queue status
	 */
	public function status(): void {
		$rows = array();
		foreach ( ( new Viglet_Dumont_Queue() )->jobs() as $post_id => $job ) {
			$rows[] = array(
				'post'     => $post_id,
				'action'   => $job['action'],
				'attempts' => $job['attempts'],
				'next'     => $job['next_at'] ? gmdate( 'c', (int) $job['next_at'] ) : '',
				'failed'   => $job['failed'] ? 'yes' : 'no',
				'error'    => $job['error'],
			);
		}

		if ( empty( $rows ) ) {
			WP_CLI::success( __( 'The queue is empty.', 'viglet-dumont' ) );
			return;
		}
		WP_CLI\Utils\format_items( 'table', $rows, array( 'post', 'action', 'attempts', 'next', 'failed', 'error' ) );
	}
}
