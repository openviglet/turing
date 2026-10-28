<?php
/**
 * The Site assistant (D175): the first questions it suggests, and the launcher only when the
 * site asks for it.
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Brain\Monkey\Actions;
use Brain\Monkey\Functions;
use Viglet_Dumont_Assistant;

final class AssistantTest extends PluginTestCase {

	public function test_the_blocks_own_questions_come_first_one_per_line(): void {
		Functions\expect( 'get_transient' )->never();

		$suggestions = ( new Viglet_Dumont_Assistant( $this->settings() ) )->suggestions( "When does the grant close?\n\n  Where is the meeting?  \nWhen does the grant close?" );

		$this->assertSame( array( 'When does the grant close?', 'Where is the meeting?' ), $suggestions );
	}

	public function test_without_its_own_the_block_suggests_the_weeks_popular_searches(): void {
		Functions\when( 'get_transient' )->justReturn( array( 'workshop', 'grant', 'meeting', 'tents', 'boots' ) );

		$suggestions = ( new Viglet_Dumont_Assistant( $this->settings() ) )->suggestions( '' );

		$this->assertSame( array( 'workshop', 'grant', 'meeting', 'tents' ), $suggestions, 'at most four' );
	}

	public function test_the_launcher_is_off_until_the_site_turns_it_on(): void {
		Actions\expectAdded( 'wp_footer' )->never();
		( new Viglet_Dumont_Assistant( $this->settings() ) )->register();

		$this->addToAssertionCount( 1 );
	}

	public function test_with_the_setting_on_every_page_gets_the_launcher(): void {
		$assistant = new Viglet_Dumont_Assistant( $this->settings( array( 'assistant_launcher' => 1 ) ) );
		Actions\expectAdded( 'wp_enqueue_scripts' )->once()->with( array( $assistant, 'enqueue' ) );
		Actions\expectAdded( 'wp_footer' )->once()->with( array( $assistant, 'launcher' ), 5 );

		$assistant->register();

		$this->addToAssertionCount( 1 );
	}
}
