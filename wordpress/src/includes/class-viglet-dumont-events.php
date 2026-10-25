<?php
/**
 * Viglet Dumont Events
 *
 * The last things the plugin did with Turing, for the Health page: what was sent, removed or
 * provisioned, and what failed with which error.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * A ring of the last events, newest first, in a non-autoloaded option.
 */
class Viglet_Dumont_Events {

	/** The option the events live in. */
	public const OPTION = 'viglet_dumont_events';

	/** How many are kept. */
	public const KEEP = 50;

	/**
	 * Record one event.
	 *
	 * @param string   $type    What happened: index, delete, provision, sweep, reindex.
	 * @param bool     $ok      Whether it went through.
	 * @param string   $message What to show: the error, or a summary.
	 * @param int|null $post_id The post it was about, when it was about one.
	 */
	public static function record( string $type, bool $ok, string $message = '', ?int $post_id = null ): void {
		$events = self::all();
		array_unshift(
			$events,
			array(
				'at'      => time(),
				'type'    => $type,
				'ok'      => $ok,
				'message' => $message,
				'post_id' => $post_id,
			)
		);
		update_option( self::OPTION, array_slice( $events, 0, self::KEEP ), false );
	}

	/**
	 * Every kept event, newest first.
	 *
	 * @return array<int, array{at: int, type: string, ok: bool, message: string, post_id: int|null}>
	 */
	public static function all(): array {
		$events = get_option( self::OPTION, array() );

		return is_array( $events ) ? $events : array();
	}
}
