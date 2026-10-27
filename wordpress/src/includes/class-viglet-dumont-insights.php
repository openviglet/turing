<?php
/**
 * Viglet Dumont Insights
 *
 * What visitors search for, what they fail to find and what they open (D178), read from the
 * SN site's metrics in Turing with the stored key, for the Search insights screen.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * One period's searches, as Turing's metrics report them.
 */
class Viglet_Dumont_Insights {

	/** The periods Turing's metrics answer for. */
	public const PERIODS = array( 'today', 'this-week', 'this-month', 'all-time' );

	/** The transient the dashboard widget keeps its report in. */
	public const WIDGET_CACHE = 'viglet_dumont_insights_widget';

	/** How many terms each table shows. */
	private const ROWS = 20;

	/**
	 * The plugin settings.
	 *
	 * @var array
	 */
	private array $settings;

	/**
	 * The client the metrics are read through.
	 *
	 * @var Viglet_Dumont_API_Client
	 */
	private Viglet_Dumont_API_Client $client;

	/**
	 * Read over the given settings, or over the stored ones.
	 *
	 * @param array|null $settings The plugin settings; null reads them from the options table.
	 */
	public function __construct( ?array $settings = null ) {
		$this->settings = $settings ?? viglet_dumont_get_settings();
		$this->client   = new Viglet_Dumont_API_Client( $this->settings );
	}

	/**
	 * A period's top searches with their clicks, the searches that found nothing, and the ones
	 * that found results nobody opened.
	 *
	 * The search block searches as the visitor types. Since D183 it does so in Turing's instant
	 * mode, which keeps those searches out of the metrics; before, each reached them as a typed
	 * prefix ending in `*`, and the ones still in the log are left out. The totals are the
	 * shown terms' own, so typing does not skew the click-through rate either.
	 *
	 * @param string $period One of self::PERIODS.
	 * @return array{period: string, searches: int, clicks: int, ctr: float, top: array, failed: array, unopened: array, metricsOff: bool}|WP_Error
	 */
	public function report( string $period ) {
		$period = in_array( $period, self::PERIODS, true ) ? $period : 'this-week';
		$site   = ( new Viglet_Dumont_Tuning( $this->settings ) )->site_id();
		if ( is_wp_error( $site ) ) {
			return $site;
		}
		// Room for the wildcard terms dropped below.
		$query  = '?period=' . rawurlencode( $period ) . '&rows=' . ( self::ROWS * 3 );
		$clicks = $this->client->call( 'GET', "/api/sn/{$site}/metrics/clicks{$query}" );
		if ( is_wp_error( $clicks ) ) {
			return $clicks;
		}
		$failed = $this->client->call( 'GET', "/api/sn/{$site}/metrics/failed-searches{$query}" );
		if ( is_wp_error( $failed ) ) {
			return $failed;
		}

		$top      = $this->with_trend( self::terms( (array) ( $clicks['terms'] ?? array() ) ), $site, $clicks );
		$searches = (int) array_sum( array_column( $top, 'searches' ) );
		$clicked  = (int) array_sum( array_column( $top, 'clicks' ) );

		return array(
			'period'     => $period,
			'searches'   => $searches,
			'clicks'     => $clicked,
			'ctr'        => $searches > 0 ? round( $clicked / $searches, 4 ) : 0.0,
			'top'        => $top,
			'failed'     => self::terms( (array) ( $failed['terms'] ?? array() ) ),
			'unopened'   => self::terms( (array) ( $clicks['neverClicked'] ?? array() ) ),
			// An empty period with metrics off is not a quiet site: Turing kept nothing. The report
			// says so since Turing 2026.3.6 (D215); an older one is asked with a probe click.
			'metricsOff' => false === ( is_bool( $clicks['metricsEnabled'] ?? null ) ? $clicks['metricsEnabled'] : $this->client->metrics_enabled() ),
		);
	}

	/**
	 * A few sentences on the period by Turing's language model: what visitors looked for, what
	 * the site lacks and what to write. Written from the report's own terms, cached by Turing
	 * under a key of what it read, so an unchanged period answers from the cache.
	 *
	 * @param string $period One of self::PERIODS.
	 * @param bool   $again  Write another rather than the one Turing cached.
	 * @return array{summary: string, error: string}
	 */
	public function summary( string $period, bool $again = false ): array {
		$report = $this->report( $period );
		if ( is_wp_error( $report ) ) {
			return array(
				'summary' => '',
				'error'   => $report->get_error_message(),
			);
		}
		if ( ! $report['top'] && ! $report['failed'] ) {
			return array(
				'summary' => '',
				'error'   => __( 'There are no searches in this period to summarize.', 'viglet-dumont' ),
			);
		}

		$lines  = static fn ( array $terms ) => implode(
			"\n",
			array_map( static fn ( array $t ) => sprintf( '- %s: %d searches, %d clicks', $t['term'], $t['searches'], $t['clicks'] ?? 0 ), $terms )
		);
		$data   = sprintf(
			"Period: %s\nSearches: %d, clicks: %d\n\nTop searches:\n%s\n\nSearches that found nothing:\n%s\n\nSearches whose results nobody opened:\n%s",
			$report['period'],
			$report['searches'],
			$report['clicks'],
			$lines( $report['top'] ),
			$lines( $report['failed'] ),
			$lines( $report['unopened'] )
		);
		$locale = get_locale();
		$prompt = sprintf(
			'You summarize a website\'s search log for its owner in three to five short sentences: what visitors looked for most, what they searched for and did not find, and which one or two posts would be worth writing. Write in the language of the locale %s. Use only the searches listed; name searches as written, never invent numbers. Answer as plain text: no heading, no list, no markdown.',
			$locale
		);
		$key    = 'viglet-dumont-insights-' . md5( $locale . "\n" . $data );

		$result = $this->client->summary( $key, $data, $prompt, $again );
		if ( is_wp_error( $result ) ) {
			return array(
				'summary' => '',
				'error'   => $result->get_error_message(),
			);
		}
		$summary = trim( (string) ( $result['content'] ?? '' ) );
		if ( empty( $result['success'] ) || '' === $summary ) {
			return array(
				'summary' => '',
				'error'   => '' !== (string) ( $result['error'] ?? '' ) ? (string) $result['error'] : __( 'Turing wrote no summary.', 'viglet-dumont' ),
			);
		}

		return array(
			'summary' => $summary,
			'error'   => '',
		);
	}

	/**
	 * The dashboard widget: the week's five top searches and five that found nothing, each a
	 * link to the screen. The report is kept for a few minutes, so the dashboard does not ask
	 * Turing on every visit.
	 */
	public function render_widget(): void {
		$report = get_transient( self::WIDGET_CACHE );
		if ( ! is_array( $report ) ) {
			$report = $this->report( 'this-week' );
			if ( is_wp_error( $report ) ) {
				echo '<p>' . esc_html( $report->get_error_message() ) . '</p>';
				return;
			}
			set_transient( self::WIDGET_CACHE, $report, 10 * MINUTE_IN_SECONDS );
		}

		$lists = array_filter(
			array(
				__( 'Top searches', 'viglet-dumont' ) => array_slice( $report['top'], 0, 5 ),
				__( 'Searches that found nothing', 'viglet-dumont' ) => array_slice( $report['failed'], 0, 5 ),
			)
		);
		if ( ! empty( $report['metricsOff'] ) ) {
			echo '<p>' . esc_html__( 'Turing is not keeping search metrics, so no new searches are recorded.', 'viglet-dumont' ) . '</p>';
		} elseif ( ! $lists ) {
			echo '<p>' . esc_html__( 'No searches this week yet.', 'viglet-dumont' ) . '</p>';
		}
		foreach ( $lists as $title => $terms ) {
			echo '<h3>' . esc_html( $title ) . '</h3><ul>';
			foreach ( $terms as $term ) {
				printf(
					'<li>%1$s <span class="count">(%2$s)</span></li>',
					esc_html( $term['term'] ),
					esc_html( number_format_i18n( $term['searches'] ) )
				);
			}
			echo '</ul>';
		}
		printf(
			'<p><a href="%1$s">%2$s</a></p>',
			esc_url( admin_url( 'admin.php?page=viglet-dumont-insights' ) ),
			esc_html__( 'All search insights', 'viglet-dumont' )
		);
	}

	/**
	 * Each top search with how often it was made in the window just before this one, as long
	 * (D207): the screen reads the two as rising, falling or new.
	 *
	 * @param array  $top    The period's top searches, as terms() returns them.
	 * @param string $site   The SN site's id.
	 * @param array  $clicks Turing's click report for the period, which names its window.
	 * @return array The same terms, each with `previous`.
	 */
	private function with_trend( array $top, string $site, array $clicks ): array {
		$from = strtotime( (string) ( $clicks['from'] ?? '' ) );
		$to   = strtotime( (string) ( $clicks['to'] ?? '' ) );
		// All time has nothing before it to compare with.
		if ( ! $from || ! $to || $to <= $from ) {
			return $top;
		}
		$query    = '?from=' . rawurlencode( gmdate( 'Y-m-d\TH:i:s\Z', $from - ( $to - $from ) ) )
			. '&to=' . rawurlencode( gmdate( 'Y-m-d\TH:i:s\Z', $from ) ) . '&rows=' . ( self::ROWS * 5 );
		$previous = $this->client->call( 'GET', "/api/sn/{$site}/metrics/clicks{$query}" );
		if ( is_wp_error( $previous ) ) {
			return $top;
		}
		$before = array();
		foreach ( (array) ( $previous['terms'] ?? array() ) as $row ) {
			$before[ mb_strtolower( (string) ( $row['term'] ?? '' ) ) ] = (int) ( $row['searches'] ?? 0 );
		}

		return array_map(
			static fn ( array $term ) => $term + array( 'previous' => $before[ mb_strtolower( $term['term'] ) ] ?? 0 ),
			$top
		);
	}

	/**
	 * Turing's rows for one table: typed prefixes left out, the rest in a shape the screen
	 * reads, at most self::ROWS.
	 *
	 * @param array $rows The report's terms.
	 * @return array<int, array{term: string, searches: int, clicks: int, ctr: float, lastSeen: string}>
	 */
	private static function terms( array $rows ): array {
		$terms = array();
		foreach ( $rows as $row ) {
			$term = trim( (string) ( $row['term'] ?? '' ) );
			if ( '' === $term || str_ends_with( $term, '*' ) ) {
				continue;
			}
			$terms[] = array(
				'term'     => $term,
				'searches' => (int) ( $row['searches'] ?? 0 ),
				'clicks'   => (int) ( $row['clicks'] ?? 0 ),
				'ctr'      => (float) ( $row['ctr'] ?? 0 ),
				'lastSeen' => (string) ( $row['lastSeen'] ?? '' ),
			);
			if ( count( $terms ) >= self::ROWS ) {
				break;
			}
		}

		return $terms;
	}
}
