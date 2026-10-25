<?php
/**
 * The search block in pieces (D185): the id its parts share, and the block's attributes as
 * its inner blocks read them.
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Brain\Monkey\Functions;
use Viglet_Dumont_Search_Block;

final class SearchBlockTest extends PluginTestCase {

	protected function setUp(): void {
		parent::setUp();
		$count = 0;
		Functions\when( 'wp_unique_id' )->alias(
			function ( $prefix ) use ( &$count ) {
				return $prefix . ( ++$count );
			}
		);
	}

	public function test_the_parts_of_one_block_share_an_id_and_the_next_block_gets_its_own(): void {
		$first = Viglet_Dumont_Search_Block::id();
		// Inner blocks are drawn before their parent: both ask, both get the same.
		$this->assertSame( $first, Viglet_Dumont_Search_Block::id() );

		Viglet_Dumont_Search_Block::done();
		$this->assertNotSame( $first, Viglet_Dumont_Search_Block::id() );
		Viglet_Dumont_Search_Block::done();
	}

	public function test_an_inner_block_reads_the_blocks_attributes_from_its_context(): void {
		$this->assertSame(
			array(
				'showSort'    => false,
				'placeholder' => 'Find a course',
			),
			Viglet_Dumont_Search_Block::attributes_from(
				array(
					'viglet-dumont/search/showSort'    => false,
					'viglet-dumont/search/placeholder' => 'Find a course',
					'postId'                           => 12,
				)
			)
		);
	}

	public function test_every_part_draws_pieces_that_exist(): void {
		foreach ( Viglet_Dumont_Search_Block::PARTS as $part => $pieces ) {
			foreach ( $pieces as $piece ) {
				$this->assertFileExists( dirname( __DIR__ ) . "/src/blocks/search/parts/$piece.php", $part );
			}
		}
	}
}
