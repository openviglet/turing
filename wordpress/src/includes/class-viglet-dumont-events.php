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
	 * The message is stored as a note, `array{0: key, 1: args}` from note(), so it is
	 * translated when read, in the reader's language, not in the language of whatever request
	 * wrote it (a WP-CLI run, another admin). A plain string is stored as it is: an error
	 * WordPress's HTTP layer or Turing wrote.
	 *
	 * @param string       $type    What happened: index, delete, provision, sweep, reindex.
	 * @param bool         $ok      Whether it went through.
	 * @param string|array $message What to show: a note, or text to show as it is.
	 * @param int|null     $post_id The post it was about, when it was about one.
	 */
	public static function record( string $type, bool $ok, $message = '', ?int $post_id = null ): void {
		$events = self::stored();
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
	 * Every kept event, newest first, each message in the reader's language.
	 *
	 * @return array<int, array{at: int, type: string, ok: bool, message: string, post_id: int|null}>
	 */
	public static function all(): array {
		return array_map(
			static fn ( array $event ) => array(
				'message' => self::text( $event['message'] ?? '' ),
				'when'    => self::when( (int) ( $event['at'] ?? 0 ) ),
			) + $event,
			self::stored()
		);
	}

	/**
	 * A moment as the site writes one (D189): its date and time formats, its time zone and the
	 * reader's language, never the browser's.
	 *
	 * @param int $at A Unix timestamp; 0 for never.
	 */
	public static function when( int $at ): string {
		return $at > 0 ? (string) wp_date( get_option( 'date_format' ) . ' ' . get_option( 'time_format' ), $at ) : '';
	}

	/**
	 * The events as stored, notes untranslated.
	 */
	private static function stored(): array {
		$events = get_option( self::OPTION, array() );

		return is_array( $events ) ? array_values( array_filter( $events, 'is_array' ) ) : array();
	}

	/**
	 * A message to translate when it is read.
	 *
	 * @param string $key  One of the keys text() knows.
	 * @param array  $args What its sentence is filled with, in order.
	 * @return array{0: string, 1: array}
	 */
	public static function note( string $key, array $args = array() ): array {
		return array( $key, array_values( $args ) );
	}

	/**
	 * A stored message as the reader reads it: a note in the current language, any other
	 * text as it was stored (older events stored the sentence itself).
	 *
	 * @param mixed $message A note, or text.
	 */
	public static function text( $message ): string {
		if ( ! is_array( $message ) ) {
			return (string) $message;
		}
		$key  = (string) ( $message[0] ?? '' );
		$args = array_values( (array) ( $message[1] ?? array() ) );

		switch ( $key ) {
			case 'imported':
				return __( 'Documents imported successfully', 'viglet-dumont' );
			case 'import_failed':
				/* translators: 1: the HTTP status code, 2: the response body. */
				$format = __( 'Import failed with HTTP %1$d: %2$s', 'viglet-dumont' );
				break;
			case 'removed':
				return __( 'Documents removed successfully', 'viglet-dumont' );
			case 'deindex_failed':
				/* translators: %d: the HTTP status code Turing answered with. */
				$format = __( 'Deindex failed with HTTP %d', 'viglet-dumont' );
				break;
			case 'provisioned':
				return __( 'Fields provisioned', 'viglet-dumont' );
			case 'provision_breaking':
				/* translators: %s: the fields whose type Turing would have to change. */
				$format = __( 'Turing kept the site\'s schema: a mapped field would change an existing field\'s type (%s). Rename the field or migrate it in Turing.', 'viglet-dumont' );
				break;
			case 'provision_failed':
				/* translators: 1: the HTTP status code, 2: Turing's error message. */
				$format = __( 'Provisioning failed with HTTP %1$d: %2$s', 'viglet-dumont' );
				break;
			case 'bulk':
				/* translators: 1: posts sent, 2: posts that failed. */
				$format = __( 'Bulk: %1$d sent, %2$d failed.', 'viglet-dumont' );
				break;
			case 'swept':
				/* translators: %d: documents removed from Turing. */
				$format = __( '%d documents WordPress no longer publishes removed.', 'viglet-dumont' );
				break;
			default:
				return $key;
		}//end switch

		// A note written with fewer arguments than its sentence wants still reads.
		$wanted = preg_match_all( '/%(?:\d+\$)?[ds]/', $format );

		return vsprintf( $format, array_pad( $args, (int) $wanted, '' ) );
	}
}
