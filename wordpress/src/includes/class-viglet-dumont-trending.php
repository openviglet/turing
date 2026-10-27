<?php
/**
 * Popular searches under the search box (D202): the week's top searches in Turing's metrics,
 * each one checked to return results now, so no chip leads to an empty page.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Builds the chips the search block offers before a visitor types.
 */
class Viglet_Dumont_Trending {

	/** The most chips shown. */
	public const MAX = 8;

	/** How many times a search must have been made this week to become a chip. */
	public const MIN_SEARCHES = 3;

	/** The chips, kept for an hour: each is a search, so they are not asked for per page view. */
	public const CACHE = 'viglet_dumont_trending';

	/**
	 * The plugin settings.
	 *
	 * @var array
	 */
	private array $settings;

	/**
	 * Build over the given settings, or over the stored ones.
	 *
	 * @param array|null $settings The plugin settings; null reads them from the options table.
	 */
	public function __construct( ?array $settings = null ) {
		$this->settings = $settings ?? viglet_dumont_get_settings();
	}

	/**
	 * The chips: up to self::MAX searches, most searched first, each returning results.
	 *
	 * @return string[]
	 */
	public function chips(): array {
		$cached = get_transient( self::CACHE );
		if ( is_array( $cached ) ) {
			return $cached;
		}

		$report  = ( new Viglet_Dumont_Insights( $this->settings ) )->report( 'this-week' );
		$popular = array();
		if ( ! is_wp_error( $report ) ) {
			foreach ( $report['top'] as $term ) {
				if ( (int) $term['searches'] >= self::MIN_SEARCHES ) {
					$popular[] = (string) $term['term'];
				}
			}
		}
		$chips = $this->answered( $popular );
		// A site with no history yet: the admin's own list, held to the same checks.
		if ( ! $chips ) {
			$chips = $this->answered( (array) ( $this->settings['trending_fallback'] ?? array() ) );
		}
		set_transient( self::CACHE, $chips, HOUR_IN_SECONDS );

		return $chips;
	}

	/**
	 * Of these searches, in order, the ones that may be shown and return results now: at most
	 * self::MAX, each spelling once, none holding a word the settings block.
	 *
	 * @param string[] $queries Searches, most wanted first.
	 * @return string[]
	 */
	private function answered( array $queries ): array {
		$client  = new Viglet_Dumont_API_Client( $this->settings );
		$indexer = new Viglet_Dumont_Indexer( $this->settings );
		$blocked = array_map( 'mb_strtolower', (array) ( $this->settings['trending_block'] ?? array() ) );
		$chips   = array();
		foreach ( $queries as $query ) {
			$query = trim( (string) $query );
			if ( ! self::presentable( $query ) || in_array( mb_strtolower( $query ), array_map( 'mb_strtolower', $chips ), true ) || self::holds_any( $query, $blocked ) ) {
				continue;
			}
			$found = $client->search( $query, 1, 1, array( 'fq[]' => array( 'site_key:' . $indexer->site_key() ) ) );
			if ( ! is_wp_error( $found ) && (int) ( $found['queryContext']['count'] ?? 0 ) > 0 ) {
				$chips[] = $query;
			}
			if ( count( $chips ) >= self::MAX ) {
				break;
			}
		}

		return $chips;
	}

	/**
	 * Whether a search holds one of these words or phrases, whole, whatever the case.
	 *
	 * @param string   $query   A search.
	 * @param string[] $blocked Lowercase words or phrases.
	 */
	private static function holds_any( string $query, array $blocked ): bool {
		$query = ' ' . mb_strtolower( $query ) . ' ';
		foreach ( $blocked as $word ) {
			if ( '' !== $word && preg_match( '/(?<![\p{L}\p{N}])' . preg_quote( $word, '/' ) . '(?![\p{L}\p{N}])/u', $query ) ) {
				return true;
			}
		}

		return false;
	}

	/**
	 * A textarea's lines as a list: trimmed, plain text, each once, at most twenty.
	 *
	 * @param string $text One entry per line.
	 * @return string[]
	 */
	public static function lines( string $text ): array {
		$lines = array_filter( array_map( static fn ( $line ) => sanitize_text_field( $line ), (array) preg_split( '/\R/', $text ) ) );

		return array_slice( array_values( array_unique( $lines ) ), 0, 20 );
	}

	/**
	 * Whether a search may be shown to every visitor: not a typed prefix, not long, and
	 * nothing that looks like personal data (an e-mail address, a long run of digits such
	 * as a document or phone number).
	 *
	 * @param string $query A search, as visitors typed it.
	 */
	public static function presentable( string $query ): bool {
		return '' !== $query
			&& mb_strlen( $query ) <= 40
			&& ! str_contains( $query, '*' )
			&& ! preg_match( '/\S+@\S+/', $query )
			&& ! preg_match( '/\d[\d.\-\s\/]{5,}\d/', $query );
	}
}
