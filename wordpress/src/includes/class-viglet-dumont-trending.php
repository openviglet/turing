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
	 * The spellings that ask for one thing ("raincoat", "rain jacket", "rain jaket"), as lists
	 * of lowercase searches, written once a day by group() and read by the chips (D213).
	 */
	public const GROUPS = 'viglet_dumont_trending_groups';

	/** The daily WP-Cron job that writes self::GROUPS, on a site with generative AI. */
	public const CRON_HOOK = 'viglet_dumont_group_trending';

	/** The most searches sent to be grouped. */
	private const MAX_GROUPED = 50;

	/** What Turing's language model is asked to do with the week's searches. */
	private const GROUP_PROMPT = 'You group the searches visitors made on one website. Two searches belong together only when they ask for the same thing: another spelling, a typo, a plural, or a synonym for the same product or topic. The searches are given one per line. Answer only with a JSON array of groups, each an array of two or more searches copied exactly from the list. Leave out every search that has no equivalent, never put one search in two groups, and never group searches that want different things. No markdown, no explanation.';

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
	 * Keep the daily grouping scheduled while generative AI is on, and stopped while it is off
	 * (`init`). The first run is an hour out, never on the request that turned it on.
	 */
	public static function schedule(): void {
		$next = wp_next_scheduled( self::CRON_HOOK );
		$on   = Viglet_Dumont_Mapping::declares_generative_ai( viglet_dumont_get_settings() );
		if ( $on && ! $next ) {
			wp_schedule_event( time() + HOUR_IN_SECONDS, 'daily', self::CRON_HOOK );
		} elseif ( ! $on && $next ) {
			wp_clear_scheduled_hook( self::CRON_HOOK );
		}
	}

	/**
	 * Ask Turing's language model which of the week's searches are one want spelled several
	 * ways, and keep its answer for the chips (the daily job, D213). A failed call keeps the
	 * last grouping; without generative AI there is none, and the chips match exact spellings.
	 */
	public function group(): void {
		if ( ! Viglet_Dumont_Mapping::declares_generative_ai( $this->settings ) ) {
			delete_option( self::GROUPS );
			return;
		}
		$report = ( new Viglet_Dumont_Insights( $this->settings ) )->report( 'this-week' );
		if ( is_wp_error( $report ) ) {
			return;
		}

		$terms = array();
		foreach ( $report['top'] as $term ) {
			$query = mb_strtolower( trim( (string) $term['term'] ) );
			if ( (int) $term['searches'] >= self::MIN_SEARCHES && self::presentable( $query ) && ! in_array( $query, $terms, true ) ) {
				$terms[] = $query;
			}
		}
		$terms  = array_slice( $terms, 0, self::MAX_GROUPED );
		$groups = array();
		if ( count( $terms ) >= 2 ) {
			$data   = implode( "\n", $terms );
			$result = ( new Viglet_Dumont_API_Client( $this->settings ) )->summary( 'viglet-dumont-trending-groups-' . md5( $data ), $data, self::GROUP_PROMPT );
			if ( is_wp_error( $result ) || empty( $result['success'] ) ) {
				return;
			}
			$groups = self::parse_groups( (string) ( $result['content'] ?? '' ), $terms );
		}
		update_option( self::GROUPS, $groups, false );
		delete_transient( self::CACHE );
	}

	/**
	 * The groups a model answered, held to the searches it was given: each a list of two or
	 * more of them, lowercase, and no search in two groups. Anything else in the answer, or an
	 * answer that is not JSON, is dropped rather than guessed at.
	 *
	 * @param string   $answer The model's answer: a JSON array of arrays, maybe fenced.
	 * @param string[] $terms  The lowercase searches it was given.
	 * @return array<int, string[]>
	 */
	public static function parse_groups( string $answer, array $terms ): array {
		$json   = json_decode( trim( (string) preg_replace( '/^\s*```(?:json)?|```\s*$/i', '', $answer ) ), true );
		$given  = array_fill_keys( $terms, true );
		$groups = array();
		$taken  = array();
		foreach ( is_array( $json ) && array_is_list( $json ) ? $json : array() as $group ) {
			$members = array();
			foreach ( is_array( $group ) && array_is_list( $group ) ? $group : array() as $query ) {
				$query = is_string( $query ) ? mb_strtolower( trim( $query ) ) : '';
				if ( isset( $given[ $query ] ) && ! isset( $taken[ $query ] ) ) {
					$members[ $query ] = true;
				}
			}
			if ( count( $members ) >= 2 ) {
				$groups[] = array_map( 'strval', array_keys( $members ) );
				$taken   += $members;
			}
		}

		return $groups;
	}

	/**
	 * Each grouped spelling's group, so the chips show one of them: by lowercase search, the
	 * group's place in self::GROUPS. Empty without generative AI.
	 *
	 * @return array<string, int>
	 */
	private function groups(): array {
		if ( ! Viglet_Dumont_Mapping::declares_generative_ai( $this->settings ) ) {
			return array();
		}
		$of = array();
		foreach ( (array) get_option( self::GROUPS, array() ) as $index => $group ) {
			foreach ( (array) $group as $query ) {
				$of[ (string) $query ] = (int) $index;
			}
		}

		return $of;
	}

	/**
	 * Of these searches, in order, the ones that may be shown and return results now: at most
	 * self::MAX, each spelling once and each group of spellings once (the first that answers,
	 * so the most searched), none holding a word the settings block.
	 *
	 * @param string[] $queries Searches, most wanted first.
	 * @return string[]
	 */
	private function answered( array $queries ): array {
		$client  = new Viglet_Dumont_API_Client( $this->settings );
		$indexer = new Viglet_Dumont_Indexer( $this->settings );
		$blocked = array_map( 'mb_strtolower', (array) ( $this->settings['trending_block'] ?? array() ) );
		$groups  = $this->groups();
		$chips   = array();
		$shown   = array();
		foreach ( $queries as $query ) {
			$query = trim( (string) $query );
			$lower = mb_strtolower( $query );
			// A grouped spelling stands for its group; any other, for itself.
			$want = isset( $groups[ $lower ] ) ? 'group:' . $groups[ $lower ] : 'query:' . $lower;
			if ( ! self::presentable( $query ) || in_array( $want, $shown, true ) || self::holds_any( $query, $blocked ) ) {
				continue;
			}
			$found = $client->search( $query, 1, 1, array( 'fq[]' => array( 'site_key:' . $indexer->site_key() ) ) );
			if ( ! is_wp_error( $found ) && (int) ( $found['queryContext']['count'] ?? 0 ) > 0 ) {
				$chips[] = $query;
				$shown[] = $want;
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
