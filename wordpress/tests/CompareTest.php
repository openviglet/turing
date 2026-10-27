<?php
/**
 * Product comparison (D200): which products a request may compare, and which rows the table
 * keeps and marks.
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Brain\Monkey\Functions;
use Viglet_Dumont_Compare;

final class CompareTest extends PluginTestCase {

	public function test_only_published_products_each_once_and_at_most_four_in_the_order_asked(): void {
		Functions\stubs(
			array(
				'absint'          => static fn ( $n ) => abs( (int) $n ),
				'get_post_type'   => static fn ( $id ) => 9 === $id ? 'page' : 'product',
				'get_post_status' => static fn ( $id ) => 7 === $id ? 'draft' : 'publish',
			)
		);

		$this->assertSame( array( 3, 12, 15, 18 ), Viglet_Dumont_Compare::ids( '3, 3,9,x,7,12,15,18,21' ) );
		$this->assertSame( array(), Viglet_Dumont_Compare::ids( '' ) );
	}

	public function test_a_row_with_a_gap_is_dropped_and_a_row_that_differs_is_marked(): void {
		$row = new \ReflectionMethod( Viglet_Dumont_Compare::class, 'row' );

		$this->assertNull( $row->invoke( null, 'Weight', array( '1 kg', '' ) ), 'a product without it: nothing to compare' );
		$this->assertSame(
			array(
				'label'   => 'Color',
				'values'  => array( 'Red', 'Blue' ),
				'differs' => true,
			),
			$row->invoke( null, 'Color', array( 'Red', 'Blue' ) )
		);
		$this->assertFalse( $row->invoke( null, 'Availability', array( 'In stock', 'in stock ' ) )['differs'], 'the same, whatever the case' );
	}

	private function jackets(): array {
		return array(
			'products' => array(
				array( 'name' => 'Rain jacket' ),
				array( 'name' => 'Wool jacket' ),
			),
			'rows'     => array(
				array(
					'label'   => 'Price',
					'values'  => array( '$149.90', '$89.00' ),
					'differs' => true,
				),
				array(
					'label'   => 'Availability',
					'values'  => array( 'In stock', 'In stock' ),
					'differs' => false,
				),
			),
		);
	}

	public function test_the_facts_are_the_table_one_line_a_row_each_value_with_its_product(): void {
		$this->assertSame(
			"Products: Rain jacket; Wool jacket\n" .
			"Price: Rain jacket = \$149.90; Wool jacket = \$89.00 (differs)\n" .
			'Availability: Rain jacket = In stock; Wool jacket = In stock (the same)',
			Viglet_Dumont_Compare::facts( $this->jackets() )
		);
	}

	public function test_a_summary_holding_a_figure_the_table_lacks_is_not_grounded(): void {
		$facts = Viglet_Dumont_Compare::facts( $this->jackets() );

		$this->assertTrue( Viglet_Dumont_Compare::grounded( 'Both are in stock; the Rain jacket costs $149.90 and the Wool jacket $89.00.', $facts ) );
		$this->assertTrue( Viglet_Dumont_Compare::grounded( 'They differ in price.', $facts ), 'no figure at all' );
		$this->assertFalse( Viglet_Dumont_Compare::grounded( 'The Wool jacket saves you $60.90.', $facts ), 'a difference the model worked out' );
		$this->assertFalse( Viglet_Dumont_Compare::grounded( 'The Rain jacket weighs 2 kg.', $facts ) );
	}
}
