<?php
/**
 * The shop the `woo` project tests (D173), made idempotent: run through WP-CLI by
 * setup/switch.setup.mjs once WooCommerce is active, never on a real site.
 *
 * - three jackets: Rain (149.90, on sale from 199, red), Wool (89, blue) and Linen (39, out of
 *   stock, red), each with a SKU and the global attribute Color;
 * - a variable Wool hat (25) in Red, sold out, and Blue (D245);
 * - products indexed, mapped with their price (as ranges), stock and color as facets.
 *
 * Prints JSON: { products: { rain, wool, linen } } post ids.
 *
 * @package Viglet_Dumont
 */

// phpcs:disable -- a test fixture run by WP-CLI, not plugin code.

// The global attribute, and its taxonomy for this request (WooCommerce registers it on init).
if ( ! wc_attribute_taxonomy_id_by_name( 'color' ) ) {
	wc_create_attribute(
		array(
			'name' => 'Color',
			'slug' => 'color',
		)
	);
}
if ( ! taxonomy_exists( 'pa_color' ) ) {
	register_taxonomy( 'pa_color', 'product', array( 'hierarchical' => false ) );
}

$jackets = array(
	'rain'  => array( 'Rain jacket', 'RJ-01', '199', '149.90', 'instock', 'Red' ),
	'wool'  => array( 'Wool jacket', 'WJ-01', '89', '', 'instock', 'Blue' ),
	'linen' => array( 'Linen jacket', 'LJ-01', '39', '', 'outofstock', 'Red' ),
);

$ids = array();
foreach ( $jackets as $key => list( $name, $sku, $regular, $sale, $stock, $color ) ) {
	$id      = wc_get_product_id_by_sku( $sku );
	$product = $id ? wc_get_product( $id ) : new WC_Product_Simple();
	$product->set_name( $name );
	$product->set_sku( $sku );
	$product->set_description( "A {$color} jacket for the field." );
	$product->set_regular_price( $regular );
	$product->set_sale_price( $sale );
	$product->set_stock_status( $stock );
	$product->set_status( 'publish' );

	$term = term_exists( $color, 'pa_color' ) ?: wp_insert_term( $color, 'pa_color' );
	$attribute = new WC_Product_Attribute();
	$attribute->set_id( wc_attribute_taxonomy_id_by_name( 'color' ) );
	$attribute->set_name( 'pa_color' );
	$attribute->set_options( array( (int) $term['term_id'] ) );
	$attribute->set_visible( true );
	$product->set_attributes( array( $attribute ) );

	$ids[ $key ] = $product->save();
	wp_set_object_terms( $ids[ $key ], array( (int) $term['term_id'] ), 'pa_color' );
}

// A variable product (D245): one hat in two colours, Red sold out and Blue in stock, so the
// cart has to be given the colour the shop can sell.
$hat_id = wc_get_product_id_by_sku( 'WH-01' );
$hat    = $hat_id ? wc_get_product( $hat_id ) : new WC_Product_Variable();
$hat->set_name( 'Wool hat' );
$hat->set_sku( 'WH-01' );
$hat->set_description( 'A wool hat for the field, in red or blue.' );
$hat->set_status( 'publish' );
$colours = array();
foreach ( array( 'Red', 'Blue' ) as $color ) {
	$colours[ $color ] = term_exists( $color, 'pa_color' ) ?: wp_insert_term( $color, 'pa_color' );
}
$attribute = new WC_Product_Attribute();
$attribute->set_id( wc_attribute_taxonomy_id_by_name( 'color' ) );
$attribute->set_name( 'pa_color' );
$attribute->set_options( array_map( static fn ( $term ) => (int) $term['term_id'], array_values( $colours ) ) );
$attribute->set_visible( true );
$attribute->set_variation( true );
$hat->set_attributes( array( $attribute ) );
$ids['hat'] = $hat->save();
wp_set_object_terms( $ids['hat'], array_map( static fn ( $term ) => (int) $term['term_id'], array_values( $colours ) ), 'pa_color' );
foreach ( array( 'Red' => 'outofstock', 'Blue' => 'instock' ) as $color => $stock ) {
	$sku       = 'WH-01-' . strtoupper( $color );
	$found     = wc_get_product_id_by_sku( $sku );
	$variation = $found ? wc_get_product( $found ) : new WC_Product_Variation();
	$variation->set_parent_id( $ids['hat'] );
	$variation->set_sku( $sku );
	$variation->set_attributes( array( 'pa_color' => get_term( (int) $colours[ $color ]['term_id'], 'pa_color' )->slug ) );
	$variation->set_regular_price( '25' );
	$variation->set_stock_status( $stock );
	$variation->set_status( 'publish' );
	$variation->save();
}
WC_Product_Variable::sync( $ids['hat'] );

// A page with the Ask the catalog block (woo/copilot.spec.mjs); global-setup drafts it again.
$ask_shop = get_page_by_path( 'ask-the-shop' );
$ask_id   = $ask_shop ? $ask_shop->ID : (int) wp_insert_post(
	array(
		'post_type'    => 'page',
		'post_name'    => 'ask-the-shop',
		'post_title'   => 'Ask the shop',
		'post_content' => '<!-- wp:viglet-dumont/copilot {"placeholder":"A jacket under 100"} /-->',
	)
);
wp_update_post(
	array(
		'ID'          => $ask_id,
		'post_status' => 'publish',
	)
);
// Out of the index, so the jacket searches find the jackets alone.
update_post_meta( $ask_id, '_viglet_dumont_exclude', '1' );

// A page with the Compare products block (woo/compare.spec.mjs, D200); drafted again too.
$compare    = get_page_by_path( 'compare-products' );
$compare_id = $compare ? $compare->ID : (int) wp_insert_post(
	array(
		'post_type'    => 'page',
		'post_name'    => 'compare-products',
		'post_title'   => 'Compare products',
		'post_content' => '<!-- wp:viglet-dumont/compare /-->',
	)
);
wp_update_post(
	array(
		'ID'          => $compare_id,
		'post_status' => 'publish',
	)
);
update_post_meta( $compare_id, '_viglet_dumont_exclude', '1' );

// A search page that applies the filters a sentence names (woo/understand.spec.mjs, D199).
$find    = get_page_by_path( 'find-gear' );
$find_id = $find ? $find->ID : (int) wp_insert_post(
	array(
		'post_type'    => 'page',
		'post_name'    => 'find-gear',
		'post_title'   => 'Find gear',
		'post_content' => '<!-- wp:viglet-dumont/search {"suggestFilters":true,"applyFilters":true} /-->',
	)
);
wp_update_post(
	array(
		'ID'          => $find_id,
		'post_status' => 'publish',
	)
);
update_post_meta( $find_id, '_viglet_dumont_exclude', '1' );

$settings                       = get_option( 'viglet_dumont_settings', array() );
$settings['index_custom_types'] = array( 'product' );
// Related content on a product page, whose product cards offer Compare (compare.spec.mjs, D212).
$settings['related_after'] = array( 'post', 'product' );
update_option( 'viglet_dumont_settings', $settings );

$mapping            = get_option( 'viglet_dumont_mapping', array() );
$mapping['product'] = array(
	'exclude' => false,
	'fields'  => array(
		array(
			'source'      => 'woo:price',
			'field'       => 'price',
			'type'        => 'FLOAT',
			'facet'       => false,
			'facet_label' => 'Price',
			'range'       => true,
			'ranges'      => array(
				array( 'label' => '', 'from' => null, 'to' => 50 ),
				array( 'label' => '', 'from' => 50, 'to' => 100 ),
				array( 'label' => '', 'from' => 100, 'to' => null ),
			),
		),
		array(
			'source' => 'woo:regular_price',
			'field'  => 'regular_price',
			'type'   => 'FLOAT',
		),
		array(
			'source'      => 'woo:stock_status',
			'field'       => 'stock_status',
			'type'        => 'STRING',
			'facet'       => true,
			'facet_label' => 'Stock',
		),
		array(
			'source'      => 'taxonomy:pa_color',
			'field'       => 'pa_color',
			'type'        => 'STRING',
			'multi'       => true,
			'facet'       => true,
			'facet_label' => 'Color',
		),
	),
);
update_option( 'viglet_dumont_mapping', $mapping, false );

echo wp_json_encode(
	array(
		'products' => $ids,
		'askPage'  => $ask_id,
	)
);
