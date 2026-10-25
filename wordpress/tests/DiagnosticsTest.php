<?php
/**
 * The Setup page's connection test names what failed.
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Brain\Monkey\Functions;
use Viglet_Dumont_Diagnostics;
use WP_Error;

final class DiagnosticsTest extends PluginTestCase {

	/** What GET /api/discovery answers: [status, body] or a WP_Error. */
	private $discovery = array( 200, '{"product":"turing"}' );

	/** What the manifest plan answers: [status, body]. */
	private array $plan = array( 200, '{"siteExists":true,"fieldsUnchanged":["site_key"]}' );

	protected function setUp(): void {
		parent::setUp();

		Functions\when( 'wp_remote_get' )->alias(
			fn () => $this->discovery instanceof WP_Error
				? $this->discovery
				: array(
					'response' => array( 'code' => $this->discovery[0] ),
					'body'     => $this->discovery[1],
				)
		);
		Functions\when( 'wp_remote_request' )->alias(
			fn () => array(
				'response' => array( 'code' => $this->plan[0] ),
				'body'     => $this->plan[1],
			)
		);
	}

	private function run_with( array $overrides = array() ): array {
		return ( new Viglet_Dumont_Diagnostics( $this->settings( $overrides ) ) )->run();
	}

	private function failed_step( array $result ): array {
		return end( $result['steps'] );
	}

	public function test_a_reachable_turing_with_a_good_key_and_the_site_passes_every_step(): void {
		$result = $this->run_with();

		$this->assertTrue( $result['ok'] );
		$this->assertTrue( $result['site_exists'] );
		$this->assertSame( array( 'url', 'reach', 'turing', 'key', 'site' ), array_column( $result['steps'], 'step' ) );
	}

	public function test_an_address_without_a_scheme_fails_at_the_url(): void {
		$step = $this->failed_step( $this->run_with( array( 'server_url' => 'turing.example.org' ) ) );

		$this->assertSame( 'url', $step['step'] );
		$this->assertFalse( $step['ok'] );
	}

	public function test_a_host_that_does_not_resolve_is_named_as_dns(): void {
		$this->discovery = new WP_Error( 'http_request_failed', 'cURL error 6: Could not resolve host: turing.example.org' );

		$step = $this->failed_step( $this->run_with() );

		$this->assertSame( 'reach', $step['step'] );
		$this->assertStringContainsString( 'DNS', $step['message'] );
	}

	public function test_a_bad_certificate_is_named_as_tls(): void {
		$this->discovery = new WP_Error( 'http_request_failed', 'cURL error 60: SSL certificate problem: self-signed certificate' );

		$this->assertStringContainsString( 'TLS', $this->failed_step( $this->run_with() )['message'] );
	}

	public function test_a_server_that_is_not_turing_fails_at_turing(): void {
		$this->discovery = array( 404, '<html>Not found</html>' );

		$step = $this->failed_step( $this->run_with() );

		$this->assertSame( 'turing', $step['step'] );
		$this->assertStringContainsString( '404', $step['message'] );
	}

	public function test_no_key_and_a_refused_key_both_fail_at_the_key(): void {
		$this->assertSame( 'key', $this->failed_step( $this->run_with( array( 'api_key' => '' ) ) )['step'] );

		$this->plan = array( 401, '' );
		$step       = $this->failed_step( $this->run_with() );
		$this->assertSame( 'key', $step['step'] );
		$this->assertStringContainsString( 'refused the API key', $step['message'] );
	}

	public function test_a_missing_sn_site_is_reported_so_the_page_can_offer_to_create_it(): void {
		$this->plan = array( 200, '{"siteExists":false,"fieldsToAdd":["site_key"]}' );

		$result = $this->run_with();

		$this->assertFalse( $result['ok'] );
		$this->assertFalse( $result['site_exists'] );
		$this->assertSame( 'site', $this->failed_step( $result )['step'] );
	}
}
