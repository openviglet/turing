<?php
/**
 * The one-time move from the plugin's old prefix (T1253): settings, post flags, scheduled jobs
 * and saved blocks reach their new names, once, without overwriting what the new names hold.
 *
 * @package Viglet_Turing
 */

namespace Viglet\Turing\Tests;

use Brain\Monkey\Functions;
use Viglet_Turing_Upgrade;

final class UpgradeTest extends PluginTestCase {

	/** The options table, by name. */
	private array $options = array();

	/** The pending jobs, as _get_cron_array() returns them. */
	private array $cron = array();

	/** What was scheduled again, each as [timestamp, hook, args, recurrence or null]. */
	private array $scheduled = array();

	/** The queries the routine ran. */
	private array $queries = array();

	protected function setUp(): void {
		parent::setUp();
		if ( ! defined( 'ARRAY_A' ) ) {
			define( 'ARRAY_A', 'ARRAY_A' );
		}
		$this->options = array();
		Functions\when( 'get_option' )->alias( fn ( $name, $fallback = false ) => $this->options[ $name ] ?? $fallback );
		Functions\when( 'add_option' )->alias(
			function ( $name, $value ) {
				$this->options[ $name ] = $value;
				return true;
			}
		);
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
		Functions\when( 'maybe_unserialize' )->alias( static fn ( $value ) => is_string( $value ) && str_starts_with( $value, 'a:' ) ? unserialize( $value ) : $value ); // phpcs:ignore WordPress.PHP.DiscouragedPHPFunctions.serialize_unserialize -- the test's own fixture.
		Functions\when( '_get_cron_array' )->alias( fn () => $this->cron );
		Functions\when( 'wp_unschedule_event' )->justReturn( true );
		Functions\when( 'wp_schedule_single_event' )->alias(
			function ( $timestamp, $hook, $args ) {
				$this->scheduled[] = array( $timestamp, $hook, $args, null );
				return true;
			}
		);
		Functions\when( 'wp_schedule_event' )->alias(
			function ( $timestamp, $recurrence, $hook, $args ) {
				$this->scheduled[] = array( $timestamp, $hook, $args, $recurrence );
				return true;
			}
		);
		Functions\when( 'wp_cache_flush' )->justReturn( true );

		$test = $this;
		// phpcs:ignore WordPress.WP.GlobalVariablesOverride.Prohibited -- a stand-in database, cleared in tearDown().
		$GLOBALS['wpdb'] = new class( $test ) {
			public string $options  = 'wp_options';
			public string $postmeta = 'wp_postmeta';
			public string $posts    = 'wp_posts';

			public function __construct( private UpgradeTest $test ) {}

			public function esc_like( $text ) {
				return addcslashes( $text, '_%\\' );
			}

			public function prepare( $query, ...$args ) {
				return array( $query, $args );
			}

			public function get_results( $prepared ) {
				return $this->test->old_option_rows();
			}

			public function query( $prepared ) {
				return $this->test->ran( $prepared );
			}
		};
	}

	protected function tearDown(): void {
		unset( $GLOBALS['wpdb'] );
		parent::tearDown();
	}

	/** The options rows under the old prefix, as the routine's SELECT finds them. */
	public function old_option_rows(): array {
		$rows = array();
		foreach ( $this->options as $name => $value ) {
			if ( 'viglet_dumont' === $name || str_starts_with( $name, 'viglet_dumont_' ) ) {
				$rows[] = array(
					'option_name'  => $name,
					'option_value' => is_array( $value ) ? serialize( $value ) : $value, // phpcs:ignore WordPress.PHP.DiscouragedPHPFunctions.serialize_serialize -- as the table stores it.
					'autoload'     => 'on',
				);
			}
		}
		return $rows;
	}

	/**
	 * Records an UPDATE or DELETE and answers how many rows it touched.
	 *
	 * @param array $prepared The query and its arguments.
	 */
	public function ran( array $prepared ): int {
		$this->queries[] = $prepared;
		return str_starts_with( $prepared[0], 'UPDATE' ) ? 2 : 0;
	}

	public function test_names_under_the_old_prefix_get_the_new_one_and_others_are_left_alone(): void {
		$this->assertSame( 'viglet_turing_settings', Viglet_Turing_Upgrade::renamed( 'viglet_dumont_settings' ) );
		$this->assertSame( '_viglet_turing_exclude', Viglet_Turing_Upgrade::renamed( '_viglet_dumont_exclude' ) );
		$this->assertSame( 'viglet_turing', Viglet_Turing_Upgrade::renamed( 'viglet_dumont' ) );
		$this->assertNull( Viglet_Turing_Upgrade::renamed( 'viglet_dumontish' ) );
		$this->assertNull( Viglet_Turing_Upgrade::renamed( 'blogname' ) );
	}

	public function test_settings_move_to_the_new_key_and_the_old_key_goes(): void {
		$this->options['viglet_dumont_settings'] = array( 'server_url' => 'https://turing.example.org' );
		$this->options['viglet_dumont_queue']    = array( 12 => 'index' );

		Viglet_Turing_Upgrade::maybe_run();

		$this->assertSame( array( 'server_url' => 'https://turing.example.org' ), $this->options['viglet_turing_settings'] );
		$this->assertSame( array( 12 => 'index' ), $this->options['viglet_turing_queue'] );
		$this->assertArrayNotHasKey( 'viglet_dumont_settings', $this->options );
		$this->assertArrayNotHasKey( 'viglet_dumont_queue', $this->options );
		$this->assertArrayHasKey( Viglet_Turing_Upgrade::DONE_OPTION, $this->options );
	}

	public function test_a_value_already_under_the_new_key_wins(): void {
		$this->options['viglet_dumont_settings'] = array( 'server_url' => 'https://old.example.org' );
		$this->options['viglet_turing_settings'] = array( 'server_url' => 'https://new.example.org' );

		Viglet_Turing_Upgrade::run();

		$this->assertSame( array( 'server_url' => 'https://new.example.org' ), $this->options['viglet_turing_settings'] );
		$this->assertArrayNotHasKey( 'viglet_dumont_settings', $this->options );
	}

	public function test_it_runs_once(): void {
		$this->options[ Viglet_Turing_Upgrade::DONE_OPTION ] = '1.0.0';
		$this->options['viglet_dumont_settings']             = array( 'a' => 1 );

		Viglet_Turing_Upgrade::maybe_run();

		$this->assertArrayHasKey( 'viglet_dumont_settings', $this->options );
		$this->assertSame( array(), $this->queries );
	}

	public function test_pending_jobs_keep_their_time_and_arguments_under_the_new_hook(): void {
		$this->cron = array(
			1800000000 => array(
				'viglet_dumont_expire'   => array(
					'k1' => array(
						'schedule' => false,
						'args'     => array( 42 ),
					),
				),
				'viglet_dumont_trending' => array(
					'k2' => array(
						'schedule' => 'daily',
						'args'     => array(),
						'interval' => 86400,
					),
				),
				'wp_version_check'       => array(
					'k3' => array(
						'schedule' => 'twicedaily',
						'args'     => array(),
					),
				),
			),
		);

		$moved = Viglet_Turing_Upgrade::run();

		$this->assertSame( 2, $moved['cron'] );
		$this->assertSame(
			array(
				array( 1800000000, 'viglet_turing_expire', array( 42 ), null ),
				array( 1800000000, 'viglet_turing_trending', array(), 'daily' ),
			),
			$this->scheduled
		);
	}

	public function test_post_flags_and_saved_blocks_are_renamed_in_place(): void {
		Viglet_Turing_Upgrade::run();

		$sql = implode( "\n", array_map( static fn ( $q ) => $q[0], $this->queries ) );
		$this->assertStringContainsString( 'UPDATE wp_postmeta SET meta_key', $sql );
		$this->assertStringContainsString( 'UPDATE wp_posts SET post_content', $sql );
		$meta = array_values( array_filter( $this->queries, static fn ( $q ) => str_contains( $q[0], 'wp_postmeta' ) ) )[0][1];
		$this->assertSame( array( '_viglet_turing_', 16, '\_viglet\_dumont\_%' ), $meta );
	}

	public function test_block_markup_names_the_new_blocks(): void {
		$this->assertSame(
			'<!-- wp:viglet-turing/search {"answer":true} --><div class="wp-block-viglet-turing-search"></div><!-- /wp:viglet-turing/search -->',
			Viglet_Turing_Upgrade::blocks( '<!-- wp:viglet-dumont/search {"answer":true} --><div class="wp-block-viglet-dumont-search"></div><!-- /wp:viglet-dumont/search -->' )
		);
	}

	public function test_block_widgets_are_renamed(): void {
		$this->options['widget_block'] = array(
			2              => array( 'content' => '<!-- wp:viglet-dumont/assistant /-->' ),
			3              => array( 'content' => '<!-- wp:paragraph --><p>hi</p><!-- /wp:paragraph -->' ),
			'_multiwidget' => 1,
		);

		Viglet_Turing_Upgrade::run();

		$this->assertSame( '<!-- wp:viglet-turing/assistant /-->', $this->options['widget_block'][2]['content'] );
		$this->assertSame( '<!-- wp:paragraph --><p>hi</p><!-- /wp:paragraph -->', $this->options['widget_block'][3]['content'] );
	}
}
