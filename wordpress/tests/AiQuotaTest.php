<?php
/**
 * A spent AI budget (D332): what Turing's 429 records, when it is gone, and the visitor's report.
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Brain\Monkey\Functions;
use Viglet_Dumont_AI_Quota;
use Viglet_Dumont_API_Client;
use Viglet_Dumont_Events;

final class AiQuotaTest extends PluginTestCase {

	/** The options table, as the stubs read and write it. */
	private array $options = array();

	/** The transients set, by name. */
	private array $transients = array();

	/** What every stubbed request answers with: [status, body]. */
	private array $answer = array( 200, '{}' );

	protected function setUp(): void {
		parent::setUp();

		Functions\when( 'get_option' )->alias( fn ( $name, $fallback = false ) => $this->options[ $name ] ?? $fallback );
		Functions\when( 'update_option' )->alias(
			function ( $name, $value ) {
				$this->options[ $name ] = $value;
				return true;
			}
		);
		Functions\when( 'delete_option' )->alias(
			function ( $name ) {
				unset( $this->options[ $name ] );
				return true;
			}
		);
		Functions\when( 'get_transient' )->alias( fn ( $name ) => $this->transients[ $name ] ?? false );
		Functions\when( 'set_transient' )->alias(
			function ( $name, $value, $ttl ) {
				$this->transients[ $name ] = array( $value, $ttl );
				return true;
			}
		);
		Functions\when( 'wp_date' )->alias( static fn ( $format, $at ) => gmdate( 'Y-m-d H:i', $at ) );
		Functions\when( 'wp_remote_request' )->alias(
			function ( string $url, array $args ) {
				$this->requests[] = array( $url, $args );
				return array(
					'response' => array( 'code' => $this->answer[0] ),
					'body'     => $this->answer[1],
				);
			}
		);
	}

	private function client(): Viglet_Dumont_API_Client {
		return new Viglet_Dumont_API_Client( $this->settings() );
	}

	/** Turing's answer to a call over the site's AI budget. */
	private function exhausted( ?string $resets = '2099-12-01T00:00:00Z', string $type = 'https://turing.example.org/problems/quota-exhausted' ): void {
		$this->answer = array(
			429,
			json_encode(
				array(
					'type'        => $type,
					'title'       => 'AI budget exhausted',
					'detail'      => 'The monthly AI budget of this site is spent.',
					'resourceKey' => 'site:sample',
					'used'        => 100,
					'limit'       => 100,
					'resetsAt'    => $resets,
					'upgradeUrl'  => 'https://turing.example.org/upgrade',
				)
			),
		);
	}

	public function test_a_quota_exhausted_429_is_recorded_with_its_reset_date_and_an_event(): void {
		$this->exhausted();

		$this->client()->call( 'POST', '/api/sn/Sample%20Site/copilot', array( 'messages' => array() ) );

		$record = Viglet_Dumont_AI_Quota::current();
		$this->assertSame( '2099-12-01T00:00:00Z', $record['resetsAt'] );
		$this->assertSame( 'The monthly AI budget of this site is spent.', $record['detail'] );
		$event = $this->options[ Viglet_Dumont_Events::OPTION ][0];
		$this->assertSame( 'ai', $event['type'] );
		$this->assertFalse( $event['ok'] );
		$this->assertSame(
			'AI budget exhausted until 2099-12-01 00:00. The monthly AI budget of this site is spent.',
			Viglet_Dumont_Events::text( $event['message'] )
		);
	}

	public function test_a_visitor_reads_when_answers_come_back_once_the_site_knows_it(): void {
		// D336: every AI surface takes this line, so a controller with no error body still says the date.
		$this->assertSame( 'Answers are paused for now; search still works.', Viglet_Dumont_AI_Quota::visitor_line() );

		$this->exhausted();
		$this->client()->call( 'POST', '/api/sn/Sample%20Site/copilot', array( 'messages' => array() ) );

		$this->assertSame( 'Answers are paused until 2099-12-01 00:00; search still works.', Viglet_Dumont_AI_Quota::visitor_line() );
		$this->assertStringContainsString( '%s', Viglet_Dumont_AI_Quota::visitor_until() );
	}

	public function test_the_same_answer_again_writes_nothing(): void {
		$this->exhausted();

		$this->client()->call( 'POST', '/api/sn/Sample%20Site/copilot' );
		$this->client()->call( 'POST', '/api/sn/Sample%20Site/copilot' );

		$this->assertCount( 1, $this->options[ Viglet_Dumont_Events::OPTION ] );
	}

	public function test_a_429_that_is_not_quota_exhausted_records_nothing(): void {
		$this->exhausted( '2099-12-01T00:00:00Z', 'https://turing.example.org/problems/rate-limited' );
		$this->client()->call( 'POST', '/api/sn/Sample%20Site/copilot' );

		$this->answer = array( 429, 'Too Many Requests' );
		$this->client()->call( 'POST', '/api/sn/Sample%20Site/copilot' );

		$this->assertNull( Viglet_Dumont_AI_Quota::current() );
		$this->assertArrayNotHasKey( Viglet_Dumont_AI_Quota::OPTION, $this->options );
	}

	public function test_a_record_whose_reset_date_has_passed_is_gone(): void {
		$this->options[ Viglet_Dumont_AI_Quota::OPTION ] = array(
			'resetsAt' => '2020-01-01T00:00:00Z',
			'detail'   => 'Spent.',
			'seen_at'  => time() - 60,
		);

		$this->assertNull( Viglet_Dumont_AI_Quota::current() );
		$this->assertNull( Viglet_Dumont_AI_Quota::report() );
	}

	public function test_a_record_with_no_reset_date_says_for_now_and_lasts_a_day(): void {
		$this->exhausted( null );
		$this->client()->call( 'POST', '/api/sn/Sample%20Site/nl-filters' );

		$this->assertSame( 'AI budget exhausted for now.', Viglet_Dumont_AI_Quota::report()['message'] );

		$this->options[ Viglet_Dumont_AI_Quota::OPTION ]['seen_at'] = time() - Viglet_Dumont_AI_Quota::UNDATED_SECONDS;
		$this->assertNull( Viglet_Dumont_AI_Quota::current() );
	}

	public function test_an_ai_call_that_goes_through_clears_the_record_and_any_other_call_does_not(): void {
		$this->exhausted();
		$this->client()->call( 'POST', '/api/sn/Sample%20Site/copilot' );

		$this->answer = array( 200, '{}' );
		$this->client()->search( 'books' );
		$this->assertNotNull( Viglet_Dumont_AI_Quota::current(), 'a search spends no AI budget' );

		$this->client()->summary( 'key', 'data', 'prompt' );
		$this->assertNull( Viglet_Dumont_AI_Quota::current() );
		$this->assertTrue( $this->options[ Viglet_Dumont_Events::OPTION ][0]['ok'] );
	}

	public function test_a_visitors_report_probes_turing_once_and_records_its_answer(): void {
		$this->exhausted();

		$result = Viglet_Dumont_AI_Quota::reported( $this->settings( array( 'site_key' => 'site-a' ) ) );

		$this->assertSame(
			array(
				'paused' => true,
				'probed' => true,
			),
			$result
		);
		$this->assertCount( 1, $this->requests );
		$this->assertStringEndsWith( '/api/sn/Sample%20Site/nl-filters', $this->requests[0][0] );
		$this->assertSame(
			array(
				'text'    => 'last week',
				'locale'  => 'pt_BR',
				'filters' => array( 'site_key' => array( 'site-a' ) ),
			),
			$this->body()
		);
		$this->assertSame( Viglet_Dumont_AI_Quota::THROTTLE_SECONDS, $this->transients[ Viglet_Dumont_AI_Quota::THROTTLE ][1] );

		// With the record live, the next report asks Turing nothing.
		Viglet_Dumont_AI_Quota::reported( $this->settings() );
		$this->assertCount( 1, $this->requests );
	}

	public function test_reports_are_throttled_and_a_fake_one_invents_nothing(): void {
		// Turing answers the probe: the budget is not spent, whatever the visitor said.
		$this->answer = array( 200, '{"available":true}' );

		$first = Viglet_Dumont_AI_Quota::reported( $this->settings() );
		$this->assertFalse( $first['paused'] );
		$this->assertTrue( $first['probed'] );
		$this->assertNull( Viglet_Dumont_AI_Quota::current() );

		// Within ten minutes a report, real or not, does not reach Turing.
		$this->exhausted();
		$second = Viglet_Dumont_AI_Quota::reported( $this->settings() );
		$this->assertSame(
			array(
				'paused' => false,
				'probed' => false,
			),
			$second
		);
		$this->assertCount( 1, $this->requests );
	}
}
