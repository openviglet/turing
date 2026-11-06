<?php
/**
 * Viglet Dumont AI Quota
 *
 * Whether Turing's AI budget for this site has run out (D332). Turing answers a call over the
 * budget with HTTP 429 and an RFC 7807 problem whose `type` ends in `quota-exhausted`, naming
 * when the budget resets. Every call the plugin makes to Turing passes observe(), which keeps
 * the last such answer; a visitor's browser, which sees the 429 but never its body, reports it
 * to the `ai-paused` REST route, and the server asks Turing once to read the rest.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * The last quota-exhausted answer, in a non-autoloaded option.
 */
class Viglet_Dumont_AI_Quota {

	/** The option the record lives in: `{resetsAt, detail, seen_at}`. */
	public const OPTION = 'viglet_dumont_ai_quota';

	/** The transient that keeps a visitor's report from asking Turing more than once a while. */
	public const THROTTLE = 'viglet_dumont_ai_paused_report';

	/** How long a report keeps the next ones from asking Turing, in seconds. */
	public const THROTTLE_SECONDS = 600;

	/** How long a record that names no reset date holds, in seconds. */
	public const UNDATED_SECONDS = 86400;

	/** The paths of Turing's calls that spend the AI budget: a 2xx from one means it is back. */
	private const AI_PATHS = '#^/api/v2/summary$|^/api/sn/[^/]+/(answer|chat|nl-filters|copilot|copilot/kit|persona/[^/]+/content-fit)$#';

	/**
	 * Keep what Turing's answer says about the budget: a 429 quota-exhausted problem records
	 * it, a 2xx from an AI call clears it. Anything else, another 429 among it, says nothing.
	 *
	 * @param string         $path     The API path asked.
	 * @param array|WP_Error $response What wp_remote_request returned.
	 */
	public static function observe( string $path, $response ): void {
		if ( is_wp_error( $response ) ) {
			return;
		}
		$code = (int) wp_remote_retrieve_response_code( $response );
		if ( 429 === $code ) {
			$problem = json_decode( (string) wp_remote_retrieve_body( $response ), true );
			if ( is_array( $problem ) && str_ends_with( (string) ( $problem['type'] ?? '' ), 'quota-exhausted' ) ) {
				self::record( $problem );
			}
			return;
		}
		if ( $code >= 200 && $code < 300 && 1 === preg_match( self::AI_PATHS, $path ) ) {
			self::clear();
		}
	}

	/**
	 * Record a quota-exhausted problem, writing only when it says something new.
	 *
	 * @param array $problem Turing's problem body.
	 */
	public static function record( array $problem ): void {
		$resets = is_string( $problem['resetsAt'] ?? null ) && '' !== $problem['resetsAt'] ? $problem['resetsAt'] : null;
		$detail = sanitize_text_field( (string) ( $problem['detail'] ?? '' ) );
		$live   = self::current();
		if ( null !== $live && $live['resetsAt'] === $resets && $live['detail'] === $detail ) {
			return;
		}
		update_option(
			self::OPTION,
			array(
				'resetsAt' => $resets,
				'detail'   => $detail,
				'seen_at'  => time(),
			),
			false
		);
		Viglet_Dumont_Events::record( 'ai', false, Viglet_Dumont_Events::note( 'quota', array( (string) $resets, $detail ) ) );
	}

	/**
	 * Forget the record, once an AI call went through again.
	 */
	public static function clear(): void {
		if ( false === get_option( self::OPTION, false ) ) {
			return;
		}
		delete_option( self::OPTION );
		Viglet_Dumont_Events::record( 'ai', true, Viglet_Dumont_Events::note( 'quota_back' ) );
	}

	/**
	 * The live record: null when there is none, when its reset date has passed, or when it
	 * names no date and is older than a day.
	 *
	 * @return array{resetsAt: string|null, detail: string, seen_at: int}|null
	 */
	public static function current(): ?array {
		$stored = get_option( self::OPTION, false );
		if ( ! is_array( $stored ) ) {
			return null;
		}
		$resets = is_string( $stored['resetsAt'] ?? null ) && '' !== $stored['resetsAt'] ? $stored['resetsAt'] : null;
		$seen   = (int) ( $stored['seen_at'] ?? 0 );
		if ( null !== $resets ) {
			$at = strtotime( $resets );
			if ( false !== $at && $at <= time() ) {
				return null;
			}
		} elseif ( $seen + self::UNDATED_SECONDS <= time() ) {
			return null;
		}

		return array(
			'resetsAt' => $resets,
			'detail'   => (string) ( $stored['detail'] ?? '' ),
			'seen_at'  => $seen,
		);
	}

	/**
	 * What Health and `wp dumont ai status` show: the record with its reset as the site writes
	 * a moment and the line that says it, or null while the budget is not known to be spent.
	 *
	 * @return array{resetsAt: string|null, detail: string, seen_at: int, until: string, message: string}|null
	 */
	public static function report(): ?array {
		$live = self::current();
		if ( null === $live ) {
			return null;
		}
		$until = self::until( $live['resetsAt'] );

		return $live + array(
			'until'   => $until,
			'message' => self::line( $until ),
		);
	}

	/**
	 * The reset date as the site writes a moment; empty when Turing named none.
	 *
	 * @param string|null $resets Turing's `resetsAt`.
	 */
	public static function until( ?string $resets ): string {
		$at = null !== $resets && '' !== $resets ? strtotime( $resets ) : false;

		return false !== $at ? Viglet_Dumont_Events::when( $at ) : '';
	}

	/**
	 * "AI budget exhausted until <date>.", or "for now" without one.
	 *
	 * @param string $until The reset date as until() writes it.
	 */
	public static function line( string $until ): string {
		return '' !== $until
			/* translators: %s: when Turing's AI budget for this site resets, as the site writes a date and time. */
			? sprintf( __( 'AI budget exhausted until %s.', 'viglet-turing-search' ), $until )
			: __( 'AI budget exhausted for now.', 'viglet-turing-search' );
	}

	/**
	 * What a visitor reads where an AI answer would be while the budget is spent (D336): with
	 * the reset date when this site already knows it (the record a 429 left), so every surface
	 * says when answers come back, even one whose SDK controller keeps no error body.
	 */
	public static function visitor_line(): string {
		$live  = self::current();
		$until = null !== $live ? self::until( $live['resetsAt'] ) : '';

		return '' !== $until
			? sprintf( self::visitor_until(), $until )
			: __( 'Answers are paused for now; search still works.', 'viglet-turing-search' );
	}

	/**
	 * The visitor's line with a reset date in it, for the browser to fill from the 429 it
	 * received (the SDK's `error.problem.resetsAt`, Turing T1234).
	 */
	public static function visitor_until(): string {
		/* translators: %s: when the site's AI answers come back, as a date and time. */
		return __( 'Answers are paused until %s; search still works.', 'viglet-turing-search' );
	}

	/**
	 * Ask Turing once, through an AI call, whether the budget is spent: a short nl-filters
	 * reading, as the search block sends one. observe() records what it answers.
	 *
	 * @param array $settings The plugin settings.
	 */
	public static function probe( array $settings ): void {
		$indexer = new Viglet_Dumont_Indexer( $settings );
		( new Viglet_Dumont_API_Client( $settings ) )->ai_probe( array( 'site_key' => array( $indexer->site_key() ) ) );
	}

	/**
	 * A visitor's browser saw Turing refuse an AI call with 429 (the `ai-paused` route). With
	 * no live record, the server asks Turing itself, at most once every ten minutes whatever
	 * the visitors send, so a report never invents a spent budget: only Turing's answer does.
	 *
	 * @param array $settings The plugin settings.
	 * @return array{paused: bool, probed: bool}
	 */
	public static function reported( array $settings ): array {
		if ( null !== self::current() ) {
			return array(
				'paused' => true,
				'probed' => false,
			);
		}
		if ( get_transient( self::THROTTLE ) ) {
			return array(
				'paused' => false,
				'probed' => false,
			);
		}
		set_transient( self::THROTTLE, time(), self::THROTTLE_SECONDS );
		self::probe( $settings );

		return array(
			'paused' => null !== self::current(),
			'probed' => true,
		);
	}

	/**
	 * Before the AI status says the budget is spent, ask Turing again: an owner who raised the
	 * budget sees it back at once. Only while a record is live, so it costs nothing otherwise.
	 *
	 * @param array $settings The plugin settings.
	 */
	public static function confirm( array $settings ): void {
		if ( null !== self::current() ) {
			self::probe( $settings );
		}
	}
}
