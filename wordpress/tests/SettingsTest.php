<?php
/**
 * The connection set from outside WordPress wins, is reported, and is never stored.
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Brain\Monkey\Functions;
use Viglet_Dumont_Settings;

final class SettingsTest extends PluginTestCase {

	/** The options table, in memory. */
	private array $options = array();

	protected function setUp(): void {
		parent::setUp();

		$this->options[ VIGLET_DUMONT_OPTION ] = array(
			'server_url' => 'https://stored.example.org',
			'api_key'    => 'stored-key',
		);
		Functions\when( 'get_option' )->alias( fn ( $name, $fallback = false ) => $this->options[ $name ] ?? $fallback );
		Functions\when( 'update_option' )->alias(
			function ( $name, $value ) {
				$this->options[ $name ] = $value;
				return true;
			}
		);
		Functions\when( 'wp_parse_args' )->alias( static fn ( $args, $defaults ) => array_merge( $defaults, $args ) );
	}

	protected function tearDown(): void {
		putenv( 'VIGLET_DUMONT_TURING_URL' );
		putenv( 'VIGLET_DUMONT_TURING_API_KEY' );
		parent::tearDown();
	}

	public function test_without_overrides_the_stored_option_is_in_effect(): void {
		$settings = Viglet_Dumont_Settings::get();

		$this->assertSame( 'https://stored.example.org', $settings['server_url'] );
		$this->assertSame( 'option', Viglet_Dumont_Settings::sources()['server_url']['source'] );
		$this->assertSame( 'default', $settings['site_name'] );
	}

	public function test_an_environment_variable_wins_and_is_reported(): void {
		putenv( 'VIGLET_DUMONT_TURING_URL=https://env.example.org' );

		$this->assertSame( 'https://env.example.org', Viglet_Dumont_Settings::get()['server_url'] );
		$this->assertTrue( Viglet_Dumont_Settings::is_overridden( 'server_url' ) );
		$this->assertSame(
			array(
				'source' => 'env',
				'name'   => 'VIGLET_DUMONT_TURING_URL',
			),
			Viglet_Dumont_Settings::sources()['server_url']
		);
	}

	public function test_an_overridden_key_is_never_written_to_the_option(): void {
		putenv( 'VIGLET_DUMONT_TURING_API_KEY=env-secret' );

		$settings              = Viglet_Dumont_Settings::get();
		$settings['site_name'] = 'Changed';
		Viglet_Dumont_Settings::update( $settings );

		$stored = $this->options[ VIGLET_DUMONT_OPTION ];
		$this->assertSame( 'stored-key', $stored['api_key'] );
		$this->assertSame( 'Changed', $stored['site_name'] );
		$this->assertStringNotContainsString( 'env-secret', (string) json_encode( $stored ) );
	}

	public function test_a_constant_wins_over_the_environment(): void {
		putenv( 'VIGLET_DUMONT_TURING_LOCALE=en_US' );
		define( 'VIGLET_DUMONT_TURING_LOCALE', 'pt_BR' );

		$this->assertSame( 'pt_BR', Viglet_Dumont_Settings::get()['locale'] );
		$this->assertSame( 'constant', Viglet_Dumont_Settings::sources()['locale']['source'] );

		putenv( 'VIGLET_DUMONT_TURING_LOCALE' );
	}
}
