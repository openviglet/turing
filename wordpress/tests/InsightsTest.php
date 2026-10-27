<?php
/**
 * The Search insights report (D178): a period's searches, failures and unopened results, read
 * from Turing's metrics.
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Brain\Monkey\Functions;
use Viglet_Dumont_Insights;

final class InsightsTest extends PluginTestCase {

	/**
	 * Answer each Turing path with its body; anything else is a 404.
	 *
	 * @param array<string, mixed> $bodies Path fragment to decoded body.
	 */
	private function turing( array $bodies ): void {
		Functions\when( 'wp_remote_request' )->alias(
			function ( string $url, array $args ) use ( $bodies ) {
				$this->requests[] = array( $url, $args );
				foreach ( $bodies as $path => $body ) {
					if ( str_contains( $url, $path ) ) {
						return array(
							'response' => array( 'code' => 200 ),
							'body'     => json_encode( $body ),
						);
					}
				}
				return array(
					'response' => array( 'code' => 404 ),
					'body'     => '',
				);
			}
		);
	}

	public function test_a_period_reads_clicks_and_failures_without_the_typed_prefixes(): void {
		$this->turing(
			array(
				'/metrics/clicks'          => array(
					'terms'        => array(
						array(
							'term'     => 'workshop',
							'searches' => 40,
							'clicks'   => 10,
							'ctr'      => 0.25,
						),
						array(
							'term'     => 'work*',
							'searches' => 500,
							'clicks'   => 0,
							'ctr'      => 0,
						),
						array(
							'term'     => 'seminar',
							'searches' => 10,
							'clicks'   => 0,
							'ctr'      => 0,
						),
					),
					'neverClicked' => array(
						array(
							'term'     => 'wo*',
							'searches' => 90,
						),
						array(
							'term'     => 'seminar',
							'searches' => 10,
						),
					),
				),
				'/metrics/failed-searches' => array(
					'terms' => array(
						array(
							'term'     => 'worskhop',
							'searches' => 7,
							'lastSeen' => '2026-10-26T22:32:30Z',
						),
					),
				),
				'/api/sn'                  => array(
					array(
						'id'   => 'site-uuid',
						'name' => 'Sample Site',
					),
				),
			)
		);

		$report = ( new Viglet_Dumont_Insights( $this->settings() ) )->report( 'this-month' );

		$this->assertSame( array( 'workshop', 'seminar' ), array_column( $report['top'], 'term' ) );
		$this->assertSame( 50, $report['searches'], 'the totals are the shown terms’, typing left out' );
		$this->assertSame( 10, $report['clicks'] );
		$this->assertSame( 0.2, $report['ctr'] );
		$this->assertSame( array( 'seminar' ), array_column( $report['unopened'], 'term' ) );
		$this->assertSame( '2026-10-26T22:32:30Z', $report['failed'][0]['lastSeen'] );
		$this->assertStringContainsString( '/api/sn/site-uuid/metrics/clicks?period=this-month', $this->requests[1][0] );
	}

	public function test_the_dashboard_widget_lists_the_weeks_top_and_failed_searches_from_its_cache(): void {
		$term = static fn ( string $name, int $searches ) => array(
			'term'     => $name,
			'searches' => $searches,
		);
		Functions\when( 'get_transient' )->justReturn(
			array(
				'top'    => array( $term( 'workshop', 1200 ), $term( 'a', 6 ), $term( 'b', 5 ), $term( 'c', 4 ), $term( 'd', 3 ), $term( 'e', 2 ) ),
				'failed' => array( $term( 'worskhop', 7 ) ),
			)
		);
		Functions\stubEscapeFunctions();
		Functions\stubs(
			array(
				'number_format_i18n' => static fn ( $n ) => number_format( $n ),
				'admin_url'          => static fn ( $path ) => 'https://site.example.org/wp-admin/' . $path,
				'esc_url'            => static fn ( $url ) => $url,
			)
		);

		ob_start();
		( new Viglet_Dumont_Insights( $this->settings() ) )->render_widget();
		$html = (string) ob_get_clean();

		$this->assertSame( array(), $this->requests, 'a cached report asks Turing nothing' );
		$this->assertSame( 6, substr_count( $html, '<li>' ), 'five top searches and the one that failed' );
		$this->assertStringContainsString( 'workshop <span class="count">(1,200)</span>', $html );
		$this->assertStringContainsString( 'worskhop', $html );
		$this->assertStringContainsString( 'page=viglet-dumont-insights', $html );
	}

	/**
	 * Turing's click endpoint as it answers an empty click: 204 and the header with metrics
	 * off, 400 with them on.
	 *
	 * @param bool $on Whether Turing keeps metrics.
	 */
	private function metrics( bool $on ): void {
		Functions\when( 'wp_remote_request' )->alias(
			function ( string $url, array $args ) use ( $on ) {
				$this->requests[] = array( $url, $args );
				if ( str_ends_with( $url, '/click' ) ) {
					return $on
						? array(
							'response' => array( 'code' => 400 ),
							'body'     => '',
						)
						: array(
							'response' => array( 'code' => 204 ),
							'headers'  => array( 'x-turing-metrics' => 'disabled' ),
							'body'     => '',
						);
				}
				$body = str_contains( $url, '/metrics/' ) ? array( 'terms' => array() ) : array(
					array(
						'id'   => 'site-uuid',
						'name' => 'Sample Site',
					),
				);
				return array(
					'response' => array( 'code' => 200 ),
					'body'     => json_encode( $body ),
				);
			}
		);
	}

	public function test_metrics_turned_off_in_turing_are_reported_rather_than_read_as_a_quiet_period(): void {
		$this->metrics( false );
		$report = ( new Viglet_Dumont_Insights( $this->settings() ) )->report( 'today' );
		$this->assertTrue( $report['metricsOff'] );

		$click = array_values( array_filter( $this->requests, static fn ( $r ) => str_ends_with( $r[0], '/click' ) ) );
		$this->assertSame( '{"documentId":""}', $click[0][1]['body'], 'a click with no document, which Turing never records' );
	}

	public function test_metrics_kept_by_turing_are_not_reported_off(): void {
		$this->metrics( true );
		$report = ( new Viglet_Dumont_Insights( $this->settings() ) )->report( 'today' );
		$this->assertFalse( $report['metricsOff'] );
	}

	public function test_the_dashboard_widget_says_metrics_are_off(): void {
		Functions\when( 'get_transient' )->justReturn(
			array(
				'top'        => array(),
				'failed'     => array(),
				'metricsOff' => true,
			)
		);
		Functions\stubEscapeFunctions();
		Functions\stubs(
			array(
				'admin_url' => static fn ( $path ) => 'https://site.example.org/wp-admin/' . $path,
				'esc_url'   => static fn ( $url ) => $url,
			)
		);

		ob_start();
		( new Viglet_Dumont_Insights( $this->settings() ) )->render_widget();
		$html = (string) ob_get_clean();

		$this->assertStringContainsString( 'not keeping search metrics', $html );
		$this->assertStringNotContainsString( 'No searches this week yet.', $html );
	}

	/**
	 * A period with one top search and one that found nothing, and Turing's summary answer.
	 *
	 * @param array $summary The decoded body of `/api/v2/summary`.
	 */
	private function period_with_searches( array $summary ): void {
		Functions\when( 'get_locale' )->justReturn( 'pt_BR' );
		$this->turing(
			array(
				'/api/v2/summary'          => $summary,
				'/metrics/clicks'          => array(
					'terms' => array(
						array(
							'term'     => 'workshop',
							'searches' => 40,
							'clicks'   => 10,
						),
					),
				),
				'/metrics/failed-searches' => array(
					'terms' => array(
						array(
							'term'     => 'worskhop',
							'searches' => 7,
						),
					),
				),
				'/api/sn'                  => array(
					array(
						'id'   => 'site-uuid',
						'name' => 'Sample Site',
					),
				),
			)
		);
	}

	public function test_a_summary_is_written_from_the_periods_searches_in_the_sites_language(): void {
		$this->period_with_searches(
			array(
				'success' => true,
				'content' => "Visitors looked for workshops.\n",
			)
		);

		$result = ( new Viglet_Dumont_Insights( $this->settings() ) )->summary( 'this-week' );

		$this->assertSame(
			array(
				'summary' => 'Visitors looked for workshops.',
				'error'   => '',
			),
			$result
		);
		$sent = array_values( array_filter( $this->requests, static fn ( $r ) => str_contains( $r[0], '/api/v2/summary' ) ) );
		$this->assertCount( 1, $sent );
		$this->assertStringNotContainsString( 'regenerate', $sent[0][0], 'the cached summary unless asked again' );
		$body = json_decode( $sent[0][1]['body'], true );
		$this->assertStringContainsString( '- workshop: 40 searches, 10 clicks', $body['data'] );
		$this->assertStringContainsString( "Searches that found nothing:\n- worskhop: 7 searches", $body['data'] );
		$this->assertStringContainsString( 'pt_BR', $body['systemPrompt'] );
		$this->assertSame( 'viglet-dumont-insights-' . md5( "pt_BR\n" . $body['data'] ), $body['cacheKey'] );
	}

	public function test_a_summary_turing_cannot_write_says_why(): void {
		$this->period_with_searches(
			array(
				'success' => false,
				'error'   => 'No default LLM is configured.',
			)
		);

		$result = ( new Viglet_Dumont_Insights( $this->settings() ) )->summary( 'this-week', true );

		$this->assertSame( 'No default LLM is configured.', $result['error'] );
		$this->assertSame( '', $result['summary'] );
	}

	public function test_a_period_with_no_searches_asks_no_summary(): void {
		$this->metrics( true );
		$result = ( new Viglet_Dumont_Insights( $this->settings() ) )->summary( 'today' );

		$this->assertNotSame( '', $result['error'] );
		$this->assertCount( 0, array_filter( $this->requests, static fn ( $r ) => str_contains( $r[0], '/api/v2/summary' ) ) );
	}

	public function test_an_unknown_period_reads_the_last_week(): void {
		$this->turing(
			array(
				'/metrics/' => array( 'terms' => array() ),
				'/api/sn'   => array(
					array(
						'id'   => 'site-uuid',
						'name' => 'Sample Site',
					),
				),
			)
		);

		$report = ( new Viglet_Dumont_Insights( $this->settings() ) )->report( 'forever' );

		$this->assertSame( 'this-week', $report['period'] );
		$this->assertSame( 0, $report['searches'] );
		$this->assertSame( 0.0, $report['ctr'] );
	}
}
