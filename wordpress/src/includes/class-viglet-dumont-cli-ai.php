<?php
/**
 * Viglet Dumont WP-CLI commands: `wp dumont ai status`.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Which of Turing's AI features this site can use, for scripts and CI.
 */
class Viglet_Dumont_CLI_AI {

	/**
	 * One row per AI feature: on or off, and why when off. Exits non-zero when any is off,
	 * so a pipeline that needs them can stop.
	 *
	 * Checking answers and natural-language filters spends a short LLM call each when they
	 * are on: Turing has no cheaper probe for them yet.
	 *
	 * ## OPTIONS
	 *
	 * [--format=<format>]
	 * : table (default) or json.
	 *
	 * ## EXAMPLES
	 *
	 *     wp dumont ai status
	 *     wp dumont ai status --format=json
	 *
	 * @param array $args       Positional arguments (none).
	 * @param array $assoc_args `format`.
	 */
	public function status( array $args, array $assoc_args ): void {
		$rows = viglet_dumont_ai_status();

		if ( 'json' === ( $assoc_args['format'] ?? 'table' ) ) {
			WP_CLI::log( (string) wp_json_encode( $rows, JSON_PRETTY_PRINT ) );
		} else {
			WP_CLI\Utils\format_items(
				'table',
				array_map(
					static fn ( array $row ) => array(
						'feature' => $row['label'],
						'on'      => $row['on'] ? __( 'yes', 'viglet-dumont' ) : __( 'no', 'viglet-dumont' ),
						'why'     => trim( $row['reason'] . ' ' . $row['fix'] ),
					),
					$rows
				),
				array( 'feature', 'on', 'why' )
			);
		}

		$off = count( array_filter( $rows, static fn ( array $row ) => ! $row['on'] ) );
		if ( $off > 0 ) {
			/* translators: %d: how many AI features are off. */
			WP_CLI::error( sprintf( _n( '%d AI feature is off.', '%d AI features are off.', $off, 'viglet-dumont' ), $off ) );
		}
	}
}
