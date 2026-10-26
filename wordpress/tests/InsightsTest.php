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
