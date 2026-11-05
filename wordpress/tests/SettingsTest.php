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
		putenv( 'VIGLET_DUMONT_DEMO' );
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

	public function test_demo_mode_is_off_unless_set_outside_wordpress_and_a_false_value_keeps_it_off(): void {
		$this->assertFalse( Viglet_Dumont_Settings::demo() );

		putenv( 'VIGLET_DUMONT_DEMO=1' );
		$this->assertTrue( Viglet_Dumont_Settings::demo() );

		putenv( 'VIGLET_DUMONT_DEMO=off' );
		$this->assertFalse( Viglet_Dumont_Settings::demo() );
	}

	/**
	 * The sibling sites (D303) are read the same from the option, the form's text and the
	 * environment: keys sanitized as site keys are, an empty or repeated one dropped, the
	 * label defaulting to the key, and an address other than http or https left out.
	 */
	public function test_the_sibling_sites_are_sanitized_from_text_a_list_or_json(): void {
		$this->assertSame(
			array(
				array(
					'key'   => 'other-site',
					'label' => 'Other site',
					'url'   => 'https://other.example.org',
				),
				array(
					'key'   => 'third.example',
					'label' => 'third.example',
					'url'   => '',
				),
			),
			Viglet_Dumont_Settings::search_sites( " Other Site | Other site | https://other.example.org \n\n third.example | | javascript:alert(1)\n other-site | Again\n ! " )
		);
		$this->assertSame(
			array(
				array(
					'key'   => 'b',
					'label' => 'B',
					'url'   => 'http://b.example.org/',
				),
			),
			Viglet_Dumont_Settings::search_sites( '[{"key":"b","label":"<b>B</b>","url":"http://b.example.org/"},{"key":""}]' )
		);
		$this->assertSame( array(), Viglet_Dumont_Settings::search_sites( '' ) );
		$this->assertSame( array(), Viglet_Dumont_Settings::search_sites( null ) );
		$this->assertSame( "other-site | Other site | https://other.example.org\nthird.example | third.example", Viglet_Dumont_Settings::search_sites_text( "other-site | Other site | https://other.example.org\nthird.example" ) );
	}

	/** The stored list is in effect, and its empty default is an empty list. */
	public function test_the_stored_sibling_sites_are_in_effect(): void {
		$this->assertSame( array(), Viglet_Dumont_Settings::get()['search_site_keys'] );

		$this->options[ VIGLET_DUMONT_OPTION ]['search_site_keys'] = array(
			array(
				'key'   => 'b',
				'label' => 'B',
			),
		);
		$this->assertSame( 'b', Viglet_Dumont_Settings::get()['search_site_keys'][0]['key'] );
		$this->assertSame( '', Viglet_Dumont_Settings::get()['search_site_keys'][0]['url'] );
	}

	public function test_the_sibling_sites_set_by_the_environment_win_and_are_never_stored(): void {
		$this->options[ VIGLET_DUMONT_OPTION ]['search_site_keys'] = array( array( 'key' => 'stored' ) );
		putenv( 'VIGLET_DUMONT_SEARCH_SITE_KEYS=site-b|Site B|https://b.example.org,site-c' );

		$this->assertSame( array( 'site-b', 'site-c' ), array_column( Viglet_Dumont_Settings::get()['search_site_keys'], 'key' ) );
		$this->assertTrue( Viglet_Dumont_Settings::is_overridden( 'search_site_keys' ) );
		Viglet_Dumont_Settings::update( array( 'search_site_keys' => array( array( 'key' => 'typed' ) ) ) );
		$this->assertSame( array( array( 'key' => 'stored' ) ), $this->options[ VIGLET_DUMONT_OPTION ]['search_site_keys'] );
		putenv( 'VIGLET_DUMONT_SEARCH_SITE_KEYS' );
	}

	#[\PHPUnit\Framework\Attributes\RunInSeparateProcess]
	#[\PHPUnit\Framework\Attributes\PreserveGlobalState( false )]
	public function test_the_sibling_sites_constant_wins_and_is_reported(): void {
		define( 'VIGLET_DUMONT_SEARCH_SITE_KEYS', '[{"key":"site-b","label":"Site B","url":"https://b.example.org"}]' );

		$this->assertSame(
			array(
				array(
					'key'   => 'site-b',
					'label' => 'Site B',
					'url'   => 'https://b.example.org',
				),
			),
			Viglet_Dumont_Settings::get()['search_site_keys']
		);
		$this->assertSame( 'constant', Viglet_Dumont_Settings::sources()['search_site_keys']['source'] );
	}

	public function test_a_constant_wins_over_the_environment(): void {
		putenv( 'VIGLET_DUMONT_TURING_LOCALE=en_US' );
		define( 'VIGLET_DUMONT_TURING_LOCALE', 'pt_BR' );

		$this->assertSame( 'pt_BR', Viglet_Dumont_Settings::get()['locale'] );
		$this->assertSame( 'constant', Viglet_Dumont_Settings::sources()['locale']['source'] );

		putenv( 'VIGLET_DUMONT_TURING_LOCALE' );
	}
}
