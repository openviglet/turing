<?php
/**
 * Viglet Dumont WooCommerce
 *
 * A product's shop fields as mapping sources (D173): price, sale price, stock status, SKU and
 * rating, read through WooCommerce's own product object, plus the defaults a product type
 * gets and the stock changes that send a product again.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * WooCommerce products as a searchable catalog. Everything here is inert without WooCommerce.
 */
class Viglet_Dumont_WooCommerce {

	/** The post type WooCommerce stores products in. */
	public const PRODUCT_TYPE = 'product';

	/** The product fields a mapping may name as `woo:<field>`, with the type each converts to. */
	public const FIELDS = array(
		'price'          => 'FLOAT',
		'regular_price'  => 'FLOAT',
		'sale_price'     => 'FLOAT',
		'stock_status'   => 'STRING',
		'sku'            => 'STRING',
		'average_rating' => 'FLOAT',
	);

	/**
	 * Whether WooCommerce is running.
	 */
	public static function active(): bool {
		return class_exists( 'WooCommerce' ) && function_exists( 'wc_get_product' );
	}

	/**
	 * Send a product again when only its stock changed: WooCommerce writes stock from an order
	 * without saving the post, so save_post never hears of it.
	 */
	public static function register(): void {
		if ( ! self::active() ) {
			return;
		}
		add_action( 'woocommerce_product_set_stock_status', array( self::class, 'stock_changed' ) );
		add_action( 'woocommerce_product_set_stock', array( self::class, 'stock_changed' ) );
	}

	/**
	 * Queue a product whose stock changed.
	 *
	 * @param int|WC_Product $product The product, or its id.
	 */
	public static function stock_changed( $product ): void {
		$id   = $product instanceof WC_Product ? $product->get_id() : (int) $product;
		$post = get_post( $id );
		if ( ! $post instanceof WP_Post || 'publish' !== $post->post_status ) {
			return;
		}
		$settings = viglet_dumont_get_settings();
		if ( $settings['auto_index'] && ( new Viglet_Dumont_Indexer( $settings ) )->should_index( $post ) ) {
			( new Viglet_Dumont_Queue( $settings ) )->enqueue( $id, 'index' );
		}
	}

	/**
	 * What the search block needs to sell from a result card: the shop's currency, and the
	 * Store API it adds to the cart through, with the nonce WooCommerce checks. Null without
	 * WooCommerce.
	 *
	 * @return array{currency: string, storeApi: string, nonce: string, cartUrl: string}|null
	 */
	public static function shop_config(): ?array {
		if ( ! self::active() ) {
			return null;
		}

		return array(
			'currency' => function_exists( 'get_woocommerce_currency' ) ? (string) get_woocommerce_currency() : 'USD',
			'storeApi' => rest_url( 'wc/store/v1/' ),
			'nonce'    => wp_create_nonce( 'wc_store_api' ),
			'cartUrl'  => function_exists( 'wc_get_cart_url' ) ? (string) wc_get_cart_url() : '',
		);
	}

	/**
	 * The shop fields the mapping screen offers for products.
	 *
	 * @return array<int, array{source: string, label: string}>
	 */
	public static function sources(): array {
		$labels = array(
			'price'          => __( 'Price', 'viglet-dumont' ),
			'regular_price'  => __( 'Regular price', 'viglet-dumont' ),
			'sale_price'     => __( 'Sale price', 'viglet-dumont' ),
			'stock_status'   => __( 'Stock status', 'viglet-dumont' ),
			'sku'            => __( 'SKU', 'viglet-dumont' ),
			'average_rating' => __( 'Average rating', 'viglet-dumont' ),
		);

		return array_map(
			static fn ( string $field ) => array(
				'source' => 'woo:' . $field,
				/* translators: %s: a product field, e.g. Price. */
				'label'  => sprintf( __( 'Product: %s', 'viglet-dumont' ), $labels[ $field ] ),
			),
			array_keys( self::FIELDS )
		);
	}

	/**
	 * What a product type maps with nothing stored, beside its public taxonomies: its price,
	 * its stock as a facet, and each global attribute (taxonomies WooCommerce keeps private).
	 *
	 * @return array<int, array>
	 */
	public static function default_fields(): array {
		$fields = array(
			array(
				'source'      => 'woo:price',
				'field'       => 'price',
				'type'        => 'FLOAT',
				'multi'       => false,
				'facet'       => false,
				'facet_label' => __( 'Price', 'viglet-dumont' ),
				'weight'      => null,
			),
			// What a result card strikes through when the product is on sale.
			array(
				'source'      => 'woo:regular_price',
				'field'       => 'regular_price',
				'type'        => 'FLOAT',
				'multi'       => false,
				'facet'       => false,
				'facet_label' => '',
				'weight'      => null,
			),
			array(
				'source'      => 'woo:stock_status',
				'field'       => 'stock_status',
				'type'        => 'STRING',
				'multi'       => false,
				'facet'       => true,
				'facet_label' => __( 'Stock', 'viglet-dumont' ),
				'weight'      => null,
			),
		);
		if ( function_exists( 'wc_get_attribute_taxonomies' ) ) {
			foreach ( wc_get_attribute_taxonomies() as $attribute ) {
				$fields[] = array(
					'source'      => 'taxonomy:pa_' . $attribute->attribute_name,
					'field'       => 'pa_' . $attribute->attribute_name,
					'type'        => 'STRING',
					'multi'       => true,
					'facet'       => true,
					'facet_label' => (string) $attribute->attribute_label,
					'weight'      => null,
				);
			}
		}

		return $fields;
	}

	/**
	 * One shop field of a product: null when the post is not a product or the field is empty.
	 * A stock status is sent as the words the shop shows ("In stock"), which is what a facet
	 * value reads as.
	 *
	 * @param WP_Post $post  The product's post.
	 * @param string  $field One of self::FIELDS.
	 * @return string|float|null
	 */
	public static function value( WP_Post $post, string $field ) {
		if ( ! self::active() || ! isset( self::FIELDS[ $field ] ) ) {
			return null;
		}
		$product = wc_get_product( $post->ID );
		if ( ! $product ) {
			return null;
		}

		switch ( $field ) {
			case 'stock_status':
				$status  = (string) $product->get_stock_status();
				$options = function_exists( 'wc_get_product_stock_status_options' ) ? wc_get_product_stock_status_options() : array();
				return '' === $status ? null : (string) ( $options[ $status ] ?? $status );
			case 'sku':
				return (string) $product->get_sku();
			default:
				$value = $product->{'get_' . $field}();
				return '' === (string) $value ? null : $value;
		}
	}
}
