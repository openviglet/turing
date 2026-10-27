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

		$report = ( new Viglet_Dumont_Insights( $this->settings ) )->report( 'this-week' );
		$chips  = array();
		if ( ! is_wp_error( $report ) ) {
			$client  = new Viglet_Dumont_API_Client( $this->settings );
			$indexer = new Viglet_Dumont_Indexer( $this->settings );
			foreach ( $report['top'] as $term ) {
				$query = trim( (string) $term['term'] );
				if ( (int) $term['searches'] < self::MIN_SEARCHES || ! self::presentable( $query ) || in_array( mb_strtolower( $query ), array_map( 'mb_strtolower', $chips ), true ) ) {
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
		}
		set_transient( self::CACHE, $chips, HOUR_IN_SECONDS );

		return $chips;
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
