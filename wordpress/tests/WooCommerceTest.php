<?php
/**
 * WooCommerce products as mapping sources (D173): the shop fields a product sends, the
 * defaults a product type gets, and the stock change that sends a product again.
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Brain\Monkey\Functions;
use Viglet_Dumont_Mapping;
use Viglet_Dumont_WooCommerce;
use WP_Post;

final class WooCommerceTest extends PluginTestCase {

	/** The options table, in memory. */
	private array $options = array();

	private WP_Post $product;

	protected function setUp(): void {
		parent::setUp();

		// WooCommerce is "running" in this process from here on: active() checks the class.
		if ( ! class_exists( 'WooCommerce' ) ) {
			eval( 'class WooCommerce {}' ); // phpcs:ignore Squiz.PHP.Eval.Discouraged -- a stand-in class for one test file.
		}

		$this->product = new WP_Post(
			array(
				'ID'          => 77,
				'post_type'   => 'product',
				'post_status' => 'publish',
				'post_title'  => 'Rain jacket',
			)
		);

		$product = new class() {
			public function get_price() {
				return '149.9';
			}
			public function get_regular_price() {
				return '199';
			}
			public function get_sale_price() {
				return '149.9';
			}
			public function get_stock_status() {
				return 'outofstock';
			}
			public function get_sku() {
				return 'RJ-01';
			}
			public function get_average_rating() {
				return '0';
			}
		};

		Functions\when( 'wc_get_product' )->justReturn( $product );
		Functions\when( 'wc_get_product_stock_status_options' )->justReturn(
			array(
				'instock'    => 'In stock',
				'outofstock' => 'Out of stock',
			)
		);
		Functions\when( 'wc_get_attribute_taxonomies' )->justReturn(
			array(
				(object) array(
					'attribute_name'  => 'color',
					'attribute_label' => 'Color',
				),
			)
		);
		Functions\when( 'get_option' )->alias( fn ( $name, $fallback = false ) => $this->options[ $name ] ?? $fallback );
		Functions\when( 'viglet_dumont_get_indexable_types' )->justReturn( array( 'product' ) );
		Functions\when( 'get_object_taxonomies' )->justReturn(
			array(
				(object) array(
					'name'   => 'product_cat',
					'label'  => 'Product categories',
					'public' => true,
				),
			)
		);
	}

	protected function tearDown(): void {
		unset( $GLOBALS['wpdb'] );
		parent::tearDown();
	}

	public function test_a_product_sends_its_shop_fields_with_the_stock_in_words(): void {
		$this->options[ Viglet_Dumont_Mapping::OPTION ] = array(
			'product' => array(
				'fields' => array(
					array(
						'source' => 'woo:price',
						'field'  => 'price',
						'type'   => 'FLOAT',
					),
					array(
						'source' => 'woo:stock_status',
						'field'  => 'stock_status',
						'type'   => 'STRING',
					),
					array(
						'source' => 'woo:sku',
						'field'  => 'sku',
						'type'   => 'STRING',
					),
					array(
						'source' => 'woo:average_rating',
						'field'  => 'rating',
						'type'   => 'FLOAT',
					),
					array(
						'source' => 'woo:post_password',
						'field'  => 'secret',
						'type'   => 'STRING',
					),
				),
			),
		);

		$mapping = new Viglet_Dumont_Mapping( $this->settings() );

		$this->assertSame(
			array(
				'price'        => 149.9,
				'stock_status' => 'Out of stock',
				'sku'          => 'RJ-01',
				'rating'       => 0.0,
			),
			$mapping->values( $this->product ),
			'a woo: source WooCommerce does not name is dropped from the mapping'
		);
	}

	public function test_a_product_type_maps_its_price_stock_and_attributes_by_default(): void {
		$fields = ( new Viglet_Dumont_Mapping( $this->settings() ) )->all()['product']['fields'];

		$this->assertSame(
			array( 'taxonomy:product_cat', 'woo:price', 'woo:stock_status', 'taxonomy:pa_color' ),
			array_column( $fields, 'source' )
		);
		$this->assertSame( array( true, false, true, true ), array_column( $fields, 'facet' ) );
		$this->assertSame( 'Color', $fields[3]['facet_label'] );

		// The meta keys a product carries: none here.
		// phpcs:ignore WordPress.WP.GlobalVariablesOverride.Prohibited -- a stand-in database, cleared in tearDown().
		$GLOBALS['wpdb'] = new class() {
			public string $posts    = 'wp_posts';
			public string $postmeta = 'wp_postmeta';
			public function prepare( $query ) {
				return $query;
			}
			public function esc_like( $text ) {
				return $text;
			}
			public function get_col() {
				return array();
			}
		};
		Functions\stubs( array( '__' => static fn ( $text ) => $text ) );
		$this->assertContains(
			'woo:sku',
			array_column( Viglet_Dumont_Mapping::sources( 'product' ), 'source' )
		);
	}

	public function test_a_stock_change_queues_the_product_again(): void {
		Functions\when( 'get_post' )->justReturn( $this->product );
		Functions\when( 'viglet_dumont_get_settings' )->justReturn( $this->settings( array( 'auto_index' => 1 ) ) );
		Functions\when( 'get_post_meta' )->justReturn( '' );
		Functions\stubs(
			array(
				'wp_cache_delete'          => true,
				'wp_is_post_revision'      => false,
				'wp_next_scheduled'        => true,
				'wp_schedule_single_event' => true,
			)
		);
		Functions\when( 'update_option' )->alias(
			function ( $name, $value ) {
				$this->options[ $name ] = $value;
				return true;
			}
		);

		Viglet_Dumont_WooCommerce::stock_changed( 77 );

		$this->assertSame( 'index', $this->options[ \Viglet_Dumont_Queue::OPTION ][77]['action'] ?? null );
	}
}
