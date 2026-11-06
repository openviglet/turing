<?php
/**
 * The search block in pieces (D185): the id its parts share, and the block's attributes as
 * its inner blocks read them.
 *
 * @package Viglet_Turing
 */

namespace Viglet\Turing\Tests;

use Brain\Monkey\Functions;
use Viglet_Turing_Search_Block;

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
		$first = Viglet_Turing_Search_Block::id();
		// Inner blocks are drawn before their parent: both ask, both get the same.
		$this->assertSame( $first, Viglet_Turing_Search_Block::id() );

		Viglet_Turing_Search_Block::done();
		$this->assertNotSame( $first, Viglet_Turing_Search_Block::id() );
		Viglet_Turing_Search_Block::done();
	}

	public function test_an_inner_block_reads_the_blocks_attributes_from_its_context(): void {
		$this->assertSame(
			array(
				'showSort'    => false,
				'placeholder' => 'Find a course',
			),
			Viglet_Turing_Search_Block::attributes_from(
				array(
					'viglet-turing/search/showSort'    => false,
					'viglet-turing/search/placeholder' => 'Find a course',
					'postId'                           => 12,
				)
			)
		);
	}

	/** The block's fixed filters keep only `field:value` queries, once each (D306). */
	public function test_fixed_filters_keep_only_field_value_queries(): void {
		$this->assertSame(
			array( 'type:page', 'editora:sociologia e politica' ),
			Viglet_Turing_Search_Block::fixed_filters( array( ' type:page ', 'no-colon', ':value', 'type:', 'type:page', 'editora:sociologia e politica' ) )
		);
		$this->assertSame( array(), Viglet_Turing_Search_Block::fixed_filters( 'type:page' ) );
	}

	/**
	 * The scope is the block's own filters and what a box-only block carried in `scope[]`, and
	 * reads as the post type's name or the value (D306).
	 */
	public function test_the_scope_joins_the_carried_one_and_reads_as_names(): void {
		Functions\when( 'wp_unslash' )->returnArg();
		Functions\when( 'get_post_type_object' )->alias(
			static fn ( $type ) => 'page' === $type ? (object) array( 'labels' => (object) array( 'name' => 'Pages' ) ) : null
		);
		$_GET = array( 'scope' => array( 'editora:sociologia', 'bad' ) );

		$scope = Viglet_Turing_Search_Block::scope( array( 'filters' => array( 'type:page' ) ) );

		$this->assertSame( array( 'type:page', 'editora:sociologia' ), $scope );
		$this->assertSame( 'Pages, sociologia', Viglet_Turing_Search_Block::scope_label( $scope ) );
		$_GET = array();
	}

	public function test_every_part_draws_pieces_that_exist(): void {
		foreach ( Viglet_Turing_Search_Block::PARTS as $part => $pieces ) {
			foreach ( $pieces as $piece ) {
				$this->assertFileExists( dirname( __DIR__ ) . "/src/blocks/search/parts/$piece.php", $part );
			}
		}
	}
}
