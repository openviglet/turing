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
		parent::tearDown();
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
				'success' => false,
				'message' => 'timed out',
				'note'    => 'timed out',
				'code'    => 0,
			),
			$result
		);
	}
}
