<?php
/**
 * Viglet Dumont WP-CLI commands: `wp dumont status|index|reindex|sweep`.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Loads, reloads and cleans the index from a script, so a container's first boot or a reset
 * dev stack needs no click in wp-admin.
 */
class Viglet_Dumont_CLI {

	/**
	 * Show the connection, the SN site and site key, documents per type in WordPress versus
	 * in Turing, and the queue.
	 *
	 * ## OPTIONS
	 *
	 * [--format=<format>]
	 * : table (default) or json.
	 *
	 * ## EXAMPLES
	 *
	 *     wp dumont status
	 *
	 * @param array $args       Positional arguments (none).
	 * @param array $assoc_args `format`.
	 */
	public function status( array $args, array $assoc_args ): void {
		$status = ( new Viglet_Dumont_Operations() )->status();

		if ( 'json' === ( $assoc_args['format'] ?? 'table' ) ) {
			WP_CLI::log( (string) wp_json_encode( $status, JSON_PRETTY_PRINT ) );
			return;
		}

		/* translators: 1: Turing's URL answer, 2: the SN site, 3: the site key. */
		WP_CLI::log( sprintf( __( 'Connection: %1$s. SN site: %2$s. Site key: %3$s.', 'viglet-dumont' ), $status['connection']['message'], $status['site_name'], $status['site_key'] ) );
		/* translators: 1: jobs waiting, 2: jobs that stopped retrying. */
		WP_CLI::log( sprintf( __( 'Queue: %1$d waiting, %2$d stopped retrying.', 'viglet-dumont' ), $status['queue']['pending'], $status['queue']['failed'] ) );
		foreach ( $status['sources'] as $key => $source ) {
			/* translators: 1: the setting, 2: constant, env or option, 3: the constant's or variable's name. */
			WP_CLI::log( sprintf( __( 'Setting %1$s comes from %2$s (%3$s).', 'viglet-dumont' ), $key, $source['source'], $source['name'] ) );
		}
		if ( is_array( $status['provisioned'] ) && '' !== ( $status['provisioned']['error'] ?? '' ) ) {
			/* translators: %s: why Turing refused the fields. */
			WP_CLI::warning( sprintf( __( 'Fields not provisioned: %s', 'viglet-dumont' ), $status['provisioned']['error'] ) );
		}

		$rows = array();
		foreach ( $status['types'] as $type => $counts ) {
			$rows[] = array(
				'type'      => $type,
				'published' => $counts['published'],
				'indexed'   => $counts['indexed'] ?? '?',
			);
		}
		WP_CLI\Utils\format_items( 'table', $rows, array( 'type', 'published', 'indexed' ) );
	}

	/**
	 * Index published posts now, synchronously. Exits non-zero when any batch failed.
	 *
	 * ## OPTIONS
	 *
	 * [--type=<post-type>]
	 * : Only this post type. Default: every indexed type.
	 *
	 * [--ids=<ids>]
	 * : Only these post ids, comma-separated.
	 *
	 * [--since=<date>]
	 * : Only posts modified on or after this date, e.g. 2026-10-01 or "-1 day".
	 *
	 * ## EXAMPLES
	 *
	 *     wp dumont index
	 *     wp dumont index --type=page --since="-1 day"
	 *     wp dumont index --ids=12,40
	 *
	 * @param array $args       Positional arguments (none).
	 * @param array $assoc_args `type`, `ids`, `since`.
	 */
	public function index( array $args, array $assoc_args ): void {
		$operations = new Viglet_Dumont_Operations();
		$ids        = array_filter( array_map( 'intval', explode( ',', (string) ( $assoc_args['ids'] ?? '' ) ) ) );
		$post_ids   = $operations->find_post_ids( $assoc_args['type'] ?? null, $ids, $assoc_args['since'] ?? null );

		// A post asked for by id but not published is removed, so `--ids` also unindexes.
		$post_ids = array_values( array_unique( array_merge( $post_ids, $ids ) ) );

		$this->finish( $operations->index( $post_ids, $this->progress( __( 'Indexing', 'viglet-dumont' ), count( $post_ids ) ) ) );
	}

	/**
	 * Provision the fields, index every published post, then sweep what is left over. Run it
	 * after a mapping change or a site key change.
	 *
	 * ## EXAMPLES
	 *
	 *     wp dumont reindex
	 */
	public function reindex(): void {
		$operations = new Viglet_Dumont_Operations();
		$total      = count( $operations->find_post_ids() );
		$result     = $operations->reindex( $this->progress( __( 'Reindexing', 'viglet-dumont' ), $total ) );

		if ( '' !== $result['sweep_error'] ) {
			/* translators: %s: why the sweep did not run or failed. */
			WP_CLI::warning( sprintf( __( 'Sweep: %s', 'viglet-dumont' ), $result['sweep_error'] ) );
		} else {
			/* translators: %d: documents removed from Turing. */
			WP_CLI::log( sprintf( __( 'Swept %d documents WordPress no longer publishes.', 'viglet-dumont' ), $result['swept'] ) );
		}
		$this->finish( $result );
	}

	/**
	 * Remove from Turing this site key's documents that WordPress does not publish: posts
	 * deleted while the plugin was off or Turing was down.
	 *
	 * ## OPTIONS
	 *
	 * [--dry-run]
	 * : List them without deleting.
	 *
	 * ## EXAMPLES
	 *
	 *     wp dumont sweep --dry-run
	 *     wp dumont sweep
	 *
	 * @param array $args       Positional arguments (none).
	 * @param array $assoc_args `dry-run`.
	 */
	public function sweep( array $args, array $assoc_args ): void {
		$dry_run = ! empty( $assoc_args['dry-run'] );
		$result  = ( new Viglet_Dumont_Operations() )->sweep( $dry_run );

		foreach ( $result['orphans'] as $doc_id ) {
			WP_CLI::log( $doc_id );
		}
		if ( ! $result['success'] ) {
			WP_CLI::error( $result['message'] );
		}
		WP_CLI::success(
			$dry_run
				/* translators: %d: documents that would be removed. */
				? sprintf( __( '%d documents would be removed.', 'viglet-dumont' ), count( $result['orphans'] ) )
				/* translators: %d: documents removed. */
				: sprintf( __( '%d documents removed.', 'viglet-dumont' ), count( $result['orphans'] ) )
		);
	}

	/**
	 * A progress bar advanced by the operation after each batch.
	 *
	 * @param string $label What is running.
	 * @param int    $total How many posts.
	 */
	private function progress( string $label, int $total ): callable {
		$bar = WP_CLI\Utils\make_progress_bar( $label, $total );

		return static function ( int $done ) use ( $bar ): void {
			$bar->tick( $done );
		};
	}

	/**
	 * Report an index run, naming each failed post, and exit non-zero when any failed.
	 *
	 * @param array{sent: int, failed: int, errors: array<int, string>} $result The run.
	 */
	private function finish( array $result ): void {
		foreach ( $result['errors'] as $post_id => $error ) {
			/* translators: 1: the post id, 2: why sending it failed. */
			WP_CLI::warning( sprintf( __( 'Post %1$d: %2$s', 'viglet-dumont' ), $post_id, $error ) );
		}
		/* translators: 1: posts sent, 2: posts that failed. */
		$summary = sprintf( __( '%1$d sent, %2$d failed.', 'viglet-dumont' ), $result['sent'], $result['failed'] );
		if ( $result['failed'] > 0 ) {
			WP_CLI::error( $summary );
		}
		WP_CLI::success( $summary );
	}
}
