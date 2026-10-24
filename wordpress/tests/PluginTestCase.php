<?php
/**
 * Brain Monkey set up and torn down around each test, with the WordPress functions every test reads.
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Brain\Monkey;
use Brain\Monkey\Functions;
use PHPUnit\Framework\TestCase;

abstract class PluginTestCase extends TestCase {

	/** The requests the plugin sent, each as [url, args]. */
	protected array $requests = array();

	/** The status code every stubbed request answers with. */
	protected int $status = 200;

	protected function setUp(): void {
		parent::setUp();
		Monkey\setUp();

		Functions\stubTranslationFunctions();
		Functions\stubs(
			array(
				'untrailingslashit'                => static fn ( $value ) => rtrim( $value, '/' ),
				'wp_json_encode'                   => static fn ( $value ) => json_encode( $value ),
				'is_wp_error'                      => static fn ( $value ) => $value instanceof \WP_Error,
				'add_query_arg'                    => static fn ( array $args, string $url ) => $url . '?' . http_build_query( $args ),
				'wp_remote_retrieve_response_code' => static fn ( $response ) => $response['response']['code'],
				'wp_remote_retrieve_body'          => static fn ( $response ) => $response['body'],
			)
		);
		Functions\when( 'wp_remote_request' )->alias(
			function ( string $url, array $args ) {
				$this->requests[] = array( $url, $args );
				return array(
					'response' => array( 'code' => $this->status ),
					'body'     => '',
				);
			}
		);
	}

	protected function tearDown(): void {
		Monkey\tearDown();
		parent::tearDown();
	}

	protected function settings( array $overrides = array() ): array {
		return array_merge(
			array(
				'server_url'          => 'https://turing.example.org/',
				'site_name'           => 'Sample Site',
				'api_key'             => 'secret',
				'locale'              => 'pt_BR',
				'source_app'          => 'wordpress',
				'index_posts'         => 1,
				'index_pages'         => 1,
				'index_custom_types'  => array(),
				'index_comments'      => 0,
				'index_custom_fields' => '',
				'index_categories'    => 1,
				'index_tags'          => 1,
				'exclude_ids'         => '',
				'batch_size'          => 50,
				'results_per_page'    => 10,
			),
			$overrides
		);
	}

	/** The JSON body of the n-th request, decoded. */
	protected function body( int $index = 0 ): array {
		return json_decode( $this->requests[ $index ][1]['body'], true );
	}
}
