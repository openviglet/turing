<?php
/**
 * The Health page's last events: newest first, at most fifty, each stored as what happened
 * and translated when read (D180).
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Brain\Monkey\Functions;
use Viglet_Dumont_Events;

final class EventsTest extends PluginTestCase {

	/** The option, as stored. */
	private array $stored = array();

	/** The language __() answers in. */
	private string $locale = 'en_US';

	protected function setUp(): void {
		parent::setUp();

		Functions\when( 'get_option' )->alias(
			function ( $name, $fallback = false ) {
				return Viglet_Dumont_Events::OPTION === $name ? $this->stored : $fallback;
			}
		);
		Functions\when( 'update_option' )->alias(
			function ( $name, $value ) {
				$this->stored = $value;
				return true;
			}
		);
		$pt = array(
			'Bulk: %1$d sent, %2$d failed.' => 'Em lote: %1$d enviados, %2$d com falha.',
			'Fields provisioned'            => 'Campos provisionados',
		);
		Functions\when( '__' )->alias( fn ( $text ) => 'pt_BR' === $this->locale ? ( $pt[ $text ] ?? $text ) : $text );
		// The site's formats, and wp_date() as the site's time zone would answer.
		$formats = array(
			'date_format' => 'j/m/Y',
			'time_format' => 'H:i',
		);
		Functions\when( 'get_option' )->alias(
			function ( $name, $fallback = false ) use ( $formats ) {
				if ( Viglet_Dumont_Events::OPTION === $name ) {
					return $this->stored;
				}
				return $formats[ $name ] ?? $fallback;
			}
		);
		Functions\when( 'wp_date' )->alias( static fn ( $format, $at ) => gmdate( $format, $at - 3 * 3600 ) );
	}

	public function test_a_moment_reads_in_the_sites_formats_and_time_zone_not_the_browsers(): void {
		Viglet_Dumont_Events::record( 'index', true );
		$this->stored[0]['at'] = 1792951215; // 2026-10-25 18:00:15 UTC.

		$this->assertSame( '25/10/2026 15:00', Viglet_Dumont_Events::all()[0]['when'] );
		$this->assertSame( '', Viglet_Dumont_Events::when( 0 ), 'never is said by the screen' );
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

	public function test_an_event_written_in_english_reads_in_the_readers_language(): void {
		Viglet_Dumont_Events::record( 'index', true, Viglet_Dumont_Events::note( 'bulk', array( 16, 0 ) ) );
		Viglet_Dumont_Events::record( 'provision', true, Viglet_Dumont_Events::note( 'provisioned' ) );

		$this->assertSame( 'Fields provisioned', Viglet_Dumont_Events::all()[0]['message'] );

		$this->locale = 'pt_BR';
		$this->assertSame(
			array( 'Campos provisionados', 'Em lote: 16 enviados, 0 com falha.' ),
			array_column( Viglet_Dumont_Events::all(), 'message' )
		);
		// What is stored is the note, never a sentence in somebody's language.
		$this->assertSame( array( 'bulk', array( 16, 0 ) ), $this->stored[1]['message'] );
	}

	public function test_text_stays_as_it_came_and_older_sentences_still_show(): void {
		$this->stored = array(
			array(
				'at'      => 1,
				'type'    => 'index',
				'ok'      => true,
				'message' => 'Bulk: 3 sent, 0 failed.',
				'post_id' => null,
			),
		);
		Viglet_Dumont_Events::record( 'index', false, 'cURL error 7: Failed to connect', 12 );

		$this->locale = 'pt_BR';
		$this->assertSame(
			array( 'cURL error 7: Failed to connect', 'Bulk: 3 sent, 0 failed.' ),
			array_column( Viglet_Dumont_Events::all(), 'message' )
		);
		$this->assertSame( 12, Viglet_Dumont_Events::all()[0]['post_id'] );
	}

	public function test_a_note_short_of_arguments_still_reads(): void {
		$this->assertSame( 'Import failed with HTTP 500: ', Viglet_Dumont_Events::text( array( 'import_failed', array( 500 ) ) ) );
		$this->assertSame( 'unknown_key', Viglet_Dumont_Events::text( array( 'unknown_key', array() ) ) );
	}
}
