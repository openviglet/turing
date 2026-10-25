<?php
/**
 * The Health page's last events: newest first, at most fifty.
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Brain\Monkey\Functions;
use Viglet_Dumont_Events;

final class EventsTest extends PluginTestCase {

	/** The options table, in memory. */
	private array $options = array();

	protected function setUp(): void {
		parent::setUp();

		Functions\when( 'get_option' )->alias( fn ( $name, $fallback = false ) => $this->options[ $name ] ?? $fallback );
		Functions\when( 'update_option' )->alias(
			function ( $name, $value ) {
				$this->options[ $name ] = $value;
				return true;
			}
		);
	}

	public function test_the_newest_event_comes_first(): void {
		Viglet_Dumont_Events::record( 'index', true, '', 1 );
		Viglet_Dumont_Events::record( 'delete', false, 'HTTP 500', 2 );

		$events = Viglet_Dumont_Events::all();

		$this->assertSame( array( 'delete', 'index' ), array_column( $events, 'type' ) );
		$this->assertFalse( $events[0]['ok'] );
		$this->assertSame( 'HTTP 500', $events[0]['message'] );
		$this->assertSame( 2, $events[0]['post_id'] );
	}

	public function test_only_the_last_fifty_are_kept(): void {
		for ( $i = 1; $i <= Viglet_Dumont_Events::KEEP + 5; $i++ ) {
			Viglet_Dumont_Events::record( 'index', true, '', $i );
		}

		$events = Viglet_Dumont_Events::all();

		$this->assertCount( Viglet_Dumont_Events::KEEP, $events );
		$this->assertSame( Viglet_Dumont_Events::KEEP + 5, $events[0]['post_id'] );
	}
}
