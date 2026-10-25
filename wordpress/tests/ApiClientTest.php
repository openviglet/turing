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
			),
			$result
		);
	}
}
