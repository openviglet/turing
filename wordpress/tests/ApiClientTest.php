<?php
/**
 * How the client talks to Turing: URLs, headers and the answers it reports.
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Viglet_Dumont_API_Client;
use WP_Error;
use Brain\Monkey\Functions;

final class ApiClientTest extends PluginTestCase {

	public function test_requests_carry_the_api_key_header(): void {
		( new Viglet_Dumont_API_Client( $this->settings() ) )->import( array() );

		$headers = $this->requests[0][1]['headers'];
		$this->assertSame( 'secret', $headers['Key'] );
		$this->assertSame( 'application/json; charset=utf-8', $headers['Content-Type'] );
	}

	public function test_an_import_waits_two_seconds_a_document_and_never_under_thirty(): void {
		$client = new Viglet_Dumont_API_Client( $this->settings() );

		$client->import( array_fill( 0, 5, array() ) );
		$client->import( array_fill( 0, 50, array() ) );

		// D256: Turing embeds each document while it imports, so a batch of 50 outlived 30 s.
		$this->assertSame( 30, $this->requests[0][1]['timeout'] );
		$this->assertSame( 100, $this->requests[1][1]['timeout'] );
	}

	public function test_an_import_turing_did_not_answer_in_time_is_timed_out_not_refused(): void {
		Functions\when( 'wp_remote_request' )->justReturn( new WP_Error( 'http_request_failed', 'cURL error 28: Operation timed out after 30001 milliseconds with 0 bytes received' ) );

		$result = ( new Viglet_Dumont_API_Client( $this->settings() ) )->import( array( array() ) );

		$this->assertFalse( $result['success'] );
		$this->assertTrue( $result['timed_out'] );
	}

	public function test_a_connection_that_never_opened_is_not_timed_out(): void {
		Functions\when( 'wp_remote_request' )->justReturn( new WP_Error( 'http_request_failed', 'cURL error 7: Failed to connect to localhost port 2700' ) );

		$this->assertFalse( ( new Viglet_Dumont_API_Client( $this->settings() ) )->import( array( array() ) )['timed_out'] );
	}

	/**
	 * Answer every request with an HTTP 400 and this body, from inside WordPress Playground
	 * or not (D237).
	 */
	private function answer_400( string $body, bool $playground ): void {
		if ( $playground ) {
			$_SERVER['SERVER_SOFTWARE'] = 'PHP.wasm';
		}
		Functions\when( 'wp_unslash' )->returnArg();
		Functions\when( 'wp_remote_request' )->justReturn(
			array(
				'response' => array( 'code' => 400 ),
				'body'     => $body,
			)
		);
	}

	protected function tearDown(): void {
		unset( $_SERVER['SERVER_SOFTWARE'] );
		putenv( 'VIGLET_DUMONT_DEMO' );
		parent::tearDown();
	}

	public function test_in_demo_mode_no_write_reaches_turing_and_each_says_why(): void {
		putenv( 'VIGLET_DUMONT_DEMO=1' );
		$client = new Viglet_Dumont_API_Client( $this->settings( array( 'api_key' => '' ) ) );

		$results = array(
			$client->import( array() ),
			$client->deindex( array() ),
			$client->provision( array( 'name' => 'x' ) ),
		);
		$plan    = $client->plan( array( 'name' => 'x' ) );

		$this->assertSame( array(), $this->requests );
		foreach ( $results as $result ) {
			$this->assertFalse( $result['success'] );
			$this->assertStringContainsString( 'Demo mode (VIGLET_DUMONT_DEMO) is on', $result['message'] );
		}
		$this->assertInstanceOf( WP_Error::class, $plan );
		$this->assertSame( 'viglet_dumont_demo', $plan->get_error_code() );
	}

	public function test_in_demo_mode_a_search_still_goes_out_without_a_key(): void {
		putenv( 'VIGLET_DUMONT_DEMO=1' );

		( new Viglet_Dumont_API_Client( $this->settings( array( 'api_key' => '' ) ) ) )->search( 'jacket', 1, 10 );

		$this->assertCount( 1, $this->requests );
		$this->assertArrayNotHasKey( 'Key', $this->requests[0][1]['headers'] );
	}

	public function test_in_playground_its_own_refusal_names_the_cause_and_the_address(): void {
		$this->answer_400( "Playground could not serve the request.\n", true );

		$ping = ( new Viglet_Dumont_API_Client( $this->settings() ) )->ping();

		$this->assertFalse( $ping['success'] );
		$this->assertStringContainsString( 'WordPress Playground could not reach Turing at https://turing.example.org.', $ping['message'] );
		$this->assertStringContainsString( 'such as localhost', $ping['message'] );
	}

	public function test_outside_playground_the_same_answer_is_turing_s_http_400(): void {
		$this->answer_400( 'Playground could not serve the request.', false );

		$ping = ( new Viglet_Dumont_API_Client( $this->settings() ) )->ping();

		$this->assertSame( 'Server returned HTTP 400', $ping['message'] );
	}

	public function test_in_playground_a_400_from_turing_itself_is_left_as_it_came(): void {
		$this->answer_400( '{"error":"bad query"}', true );

		$ping = ( new Viglet_Dumont_API_Client( $this->settings() ) )->ping();

		$this->assertSame( 'Server returned HTTP 400', $ping['message'] );
	}

	public function test_no_key_header_is_sent_without_an_api_key(): void {
		( new Viglet_Dumont_API_Client( $this->settings( array( 'api_key' => '' ) ) ) )->import( array() );

		$this->assertArrayNotHasKey( 'Key', $this->requests[0][1]['headers'] );
	}

	public function test_the_site_name_is_encoded_into_the_search_path(): void {
		( new Viglet_Dumont_API_Client( $this->settings() ) )->ping();

		$this->assertStringStartsWith(
			'https://turing.example.org/api/sn/Sample%20Site/search?',
			$this->requests[0][0]
		);
	}

	public function test_filters_are_sent_as_a_repeated_fq_parameter(): void {
		( new Viglet_Dumont_API_Client( $this->settings() ) )->search( 'x', 1, 10, array( 'fq[]' => array( 'site_key:a', 'type:post' ) ) );

		$this->assertStringEndsWith( '&fq%5B%5D=site_key%3Aa&fq%5B%5D=type%3Apost', $this->requests[0][0] );
	}

	public function test_ai_features_read_each_probe_and_the_answer_streams_first_event(): void {
		$bodies = array(
			'/chat/enabled'      => '{"enabled":true,"reason":"NONE"}',
			'/answer'            => "event:error\ndata:{\"message\":\"AI is not enabled for site: Sample\"}\n\n",
			'/nl-filters'        => '{"available":false,"text":"last week","q":"last week"}',
			'/copilot/available' => '{"available":true}',
		);
		Functions\when( 'wp_remote_request' )->alias(
			function ( string $url, array $args ) use ( $bodies ) {
				$this->requests[] = array( $url, $args );
				foreach ( $bodies as $path => $body ) {
					if ( str_contains( $url, $path ) ) {
						return array(
							'response' => array( 'code' => 200 ),
							'body'     => $body,
						);
					}
				}
				return new WP_Error( 'http_request_failed', 'unexpected ' . $url );
			}
		);

		$rows = ( new Viglet_Dumont_API_Client( $this->settings() ) )->ai_features();

		$this->assertSame(
			array(
				'answer'     => array( false, 'AI is not enabled for site: Sample' ),
				'chat'       => array( true, '' ),
				'nl_filters' => array( false, 'No language model is set up for it in Turing.' ),
				'copilot'    => array( true, '' ),
			),
			array_combine(
				array_column( $rows, 'feature' ),
				array_map( static fn ( $row ) => array( $row['on'], $row['reason'] ), $rows )
			)
		);
		// The answer stream is read only as far as its first event.
		$answer = array_values( array_filter( $this->requests, static fn ( $r ) => str_contains( $r[0], '/answer' ) ) )[0];
		$this->assertSame( 512, $answer[1]['limit_response_size'] );
	}

	public function test_ai_features_read_turings_capabilities_in_one_call_when_it_has_them(): void {
		Functions\when( 'wp_remote_request' )->alias(
			function ( string $url, array $args ) {
				$this->requests[] = array( $url, $args );
				return array(
					'response' => array( 'code' => 200 ),
					'body'     => json_encode(
						array(
							'site'      => 'Sample',
							'chat'      => array(
								'enabled' => false,
								'reason'  => 'NO_GENAI',
							),
							'answer'    => array(
								'enabled' => false,
								'reason'  => 'NO_GENAI',
							),
							'nlFilters' => array(
								'enabled' => false,
								'reason'  => 'MISSING_DEFAULT_LLM',
							),
							'copilot'   => array(
								'enabled' => true,
								'reason'  => 'NONE',
							),
						)
					),
				);
			}
		);

		$rows = ( new Viglet_Dumont_API_Client( $this->settings() ) )->ai_features();

		$this->assertSame(
			array(
				'answer'     => array( false, 'Generative AI is off for this SN site in Turing.' ),
				'chat'       => array( false, 'Generative AI is off for this SN site in Turing.' ),
				'nl_filters' => array( false, 'No language model is set up for it in Turing.' ),
				'copilot'    => array( true, '' ),
			),
			array_combine(
				array_column( $rows, 'feature' ),
				array_map( static fn ( $row ) => array( $row['on'], $row['reason'] ), $rows )
			)
		);
		$this->assertCount( 1, $this->requests, 'one probe, and no model is called' );
		$this->assertStringEndsWith( '/ai/capabilities', $this->requests[0][0] );
	}

	public function test_a_refused_answer_probe_is_off_with_turings_reason_not_on(): void {
		Functions\when( 'wp_remote_request' )->alias(
			function ( string $url ) {
				if ( str_contains( $url, '/answer' ) ) {
					return array(
						'response' => array( 'code' => 403 ),
						'body'     => '{"error":"This API key is bound to a partition of a shared site, and this endpoint does not enforce partitions yet"}',
					);
				}
				return array(
					'response' => array( 'code' => 404 ),
					'body'     => '',
				);
			}
		);

		$answer = ( new Viglet_Dumont_API_Client( $this->settings() ) )->ai_features()[0];

		$this->assertSame( 'answer', $answer['feature'] );
		$this->assertFalse( $answer['on'], 'a 403 body is not an answer stream' );
		$this->assertStringContainsString( 'bound to a partition', $answer['reason'] );
	}

	public function test_a_partition_refusal_is_told_apart_from_other_failures(): void {
		$answer = static fn ( int $code, string $body ) => Functions\when( 'wp_remote_request' )->justReturn(
			array(
				'response' => array( 'code' => $code ),
				'body'     => $body,
			)
		);
		$answer( 403, '{"error":"This API key is bound to a partition of a shared site, and this endpoint does not enforce partitions yet"}' );

		$refused = ( new Viglet_Dumont_API_Client( $this->settings() ) )->call( 'GET', '/api/sn/s1/ranking-expression' );

		$this->assertTrue( Viglet_Dumont_API_Client::refused_by_partition( $refused ) );
		$this->assertStringContainsString( 'does not enforce partitions yet', $refused->get_error_message() );

		$answer( 500, '' );
		$failed = ( new Viglet_Dumont_API_Client( $this->settings() ) )->call( 'GET', '/api/sn/s1/ranking-expression' );

		$this->assertFalse( Viglet_Dumont_API_Client::refused_by_partition( $failed ) );
		$this->assertSame( 'Turing answered HTTP 500.', $failed->get_error_message() );
	}

	public function test_an_import_answered_with_an_error_status_is_a_failure(): void {
		$this->status = 422;

		$result = ( new Viglet_Dumont_API_Client( $this->settings() ) )->import( array() );

		$this->assertFalse( $result['success'] );
		$this->assertStringContainsString( '422', $result['message'] );
	}

	public function test_an_unreachable_server_is_reported_rather_than_thrown(): void {
		Functions\when( 'wp_remote_request' )->justReturn( new WP_Error( 'http_request_failed', 'timed out' ) );

		$result = ( new Viglet_Dumont_API_Client( $this->settings() ) )->deindex( array() );

		$this->assertSame(
			array(
				'success'   => false,
				'message'   => 'timed out',
				'note'      => 'timed out',
				'code'      => 0,
				// Only an import waits long enough to be worth telling apart (D256).
				'timed_out' => false,
			),
			$result
		);
	}
}
