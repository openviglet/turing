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

		$top      = self::terms( (array) ( $clicks['terms'] ?? array() ) );
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
			// An empty period with metrics off is not a quiet site: Turing kept nothing.
			'metricsOff' => false === $this->client->metrics_enabled(),
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
