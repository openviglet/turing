<?php
/**
 * The demo storefront's catalog (D194), run on the demo's own WordPress by `run-docker.cmd demo`,
 * and by the browser suite's `demo-setup` when it is missing: the Tarnfell pages (setup.php), then about 150 products of a fictional brand
 * across eight categories, each with colour, material, use and size attributes, a price, some
 * on sale and some out of stock, and a description that answers what a shopper asks (how
 * waterproof, how heavy, how warm, how to wash it). No reviews or ratings: invented ones would
 * read as genuine.
 *
 * Idempotent: a product is found again by its SKU (`TF-…`). Then the shop's fields are mapped
 * as facets and a few synonyms are set in Turing. The searches that show each feature are in
 * the theme's curated.json, which demo/curated.spec.mjs checks.
 *
 * Prints JSON: { products: <count>, guides: <count>, pages: { … } }.
 *
 * @package Viglet_Dumont
 */

// phpcs:disable -- a setup script run by WP-CLI, not plugin code.

ob_start();
require __DIR__ . '/../setup.php';
$pages = json_decode( (string) ob_get_clean(), true );

// The site's language picks the copy (D217): Portuguese from pt_BR.php, keyed by the English it
// replaces, or the English below. Turing indexes it under the site's locale either way.
$pt = 'pt_BR' === get_locale() ? require __DIR__ . '/pt_BR.php' : array();
$tr = static fn ( string $group, string $text ): string => $pt[ $group ][ $text ] ?? $text;

// Each product's and guide's picture, drawn here rather than downloaded (D218).
require_once __DIR__ . '/images.php';

// The global attributes, and their taxonomies for this request (WooCommerce registers them on init).
foreach ( array( 'color' => 'Colour', 'material' => 'Material', 'use' => 'Use', 'size' => 'Size' ) as $slug => $label ) {
	if ( ! wc_attribute_taxonomy_id_by_name( $slug ) ) {
		wc_create_attribute(
			array(
				'name' => $label,
				'slug' => $slug,
			)
		);
	}
	if ( ! taxonomy_exists( "pa_{$slug}" ) ) {
		register_taxonomy( "pa_{$slug}", 'product', array( 'hierarchical' => false ) );
	}
}

$categories = array(
	'waterproof-jackets' => 'Waterproof jackets',
	'insulated-jackets'  => 'Insulated jackets',
	'fleece-midlayers'   => 'Fleece and midlayers',
	'hiking-boots'       => 'Hiking boots',
	'backpacks'          => 'Backpacks',
	'tents'              => 'Tents',
	'sleeping-bags'      => 'Sleeping bags',
	'accessories'        => 'Accessories',
);

$apparel = array( 'S', 'M', 'L', 'XL' );
$feet    = array( '38', '39', '40', '41', '42', '43', '44', '45', '46' );

// Each model: category, name, price, materials, uses, sizes, colours, and what its page says.
$models = array(
	// Waterproof jackets.
	array( 'waterproof-jackets', 'Catbells light rain jacket', 189, array( 'Nylon' ), array( 'Hiking', 'Travel' ), $apparel, array( 'Slate', 'Moss', 'Ember', 'Sky' ),
		'A light rain jacket that packs into its own pocket, for a day out when showers are forecast.',
		'Weight 240 g (size M). Hydrostatic head 15,000 mm; breathability 20,000 g/m²/24h. Taped seams, a hood that rolls into the collar, two hand pockets.' ),
	array( 'waterproof-jackets', 'Helvellyn mountain shell', 329, array( 'Gore-Tex' ), array( 'Mountaineering', 'Hiking' ), $apparel, array( 'Slate', 'Ember', 'Glacier' ),
		'A three-layer waterproof shell for ridges in wind-driven rain, with a helmet-compatible hood.',
		'Weight 420 g (size M). Hydrostatic head 28,000 mm. Pit zips, a stiffened peak, pockets placed above a harness and a hipbelt.' ),
	array( 'waterproof-jackets', 'Borrowdale everyday waterproof', 149, array( 'Polyester' ), array( 'Travel', 'Hiking' ), $apparel, array( 'Navy', 'Moss', 'Sand', 'Black' ),
		'A waterproof jacket for town and low-level walks, long enough to cover the hips.',
		'Weight 380 g (size M). Hydrostatic head 10,000 mm. A soft lining against the skin, a two-way zip and an inside pocket for a phone.' ),
	array( 'waterproof-jackets', 'Wasdale running shell', 159, array( 'Nylon' ), array( 'Running' ), $apparel, array( 'Ember', 'Sky', 'Black' ),
		'A minimal waterproof for fell running, with taped seams that meet race kit rules.',
		'Weight 150 g (size M). Hydrostatic head 20,000 mm. Elasticated cuffs, a fitted hood and a back vent.' ),
	array( 'waterproof-jackets', 'Skiddaw waterproof trousers', 119, array( 'Nylon' ), array( 'Hiking', 'Mountaineering' ), $apparel, array( 'Black', 'Slate' ),
		'Waterproof overtrousers with full-length side zips, to pull on over boots when the rain sets in.',
		'Weight 290 g (size M). Hydrostatic head 15,000 mm. Reinforced ankles against crampon scuffs.' ),
	// Insulated jackets.
	array( 'insulated-jackets', 'Blencathra down jacket', 279, array( 'Down', 'Nylon' ), array( 'Mountaineering', 'Camping' ), $apparel, array( 'Ember', 'Slate', 'Glacier', 'Black' ),
		'A warm down jacket for cold belays and winter camps, filled with responsibly sourced 800-fill goose down.',
		'Weight 480 g (size M). 180 g of down. A hood that fits over a helmet. Warm to about -10 °C when moving little.' ),
	array( 'insulated-jackets', 'Glaramara synthetic jacket', 189, array( 'Synthetic insulation', 'Nylon' ), array( 'Hiking', 'Camping' ), $apparel, array( 'Moss', 'Navy', 'Ember' ),
		'A synthetic insulated jacket that stays warm when damp, for the wet cold of a British winter.',
		'Weight 420 g (size M). 100 g/m² synthetic fill that keeps its loft in drizzle. Packs into its chest pocket.' ),
	array( 'insulated-jackets', 'Haystacks down vest', 139, array( 'Down', 'Nylon' ), array( 'Hiking', 'Travel' ), $apparel, array( 'Slate', 'Sand', 'Black' ),
		'A down vest to keep the core warm under a shell, with arms free for scrambling.',
		'Weight 210 g (size M). 700-fill duck down. Zipped hand pockets.' ),
	array( 'insulated-jackets', 'Kentmere hybrid jacket', 169, array( 'Synthetic insulation', 'Polyester' ), array( 'Hiking', 'Running' ), $apparel, array( 'Moss', 'Sky', 'Ember' ),
		'Insulation on the front and stretch fleece on the back and sides, for steady effort on cold days.',
		'Weight 330 g (size M). Breathes on the climb and stays warm on the summit.' ),
	array( 'insulated-jackets', 'Bowfell parka', 349, array( 'Down', 'Polyester' ), array( 'Camping', 'Travel' ), $apparel, array( 'Black', 'Navy' ),
		'A long down parka for winter camps and still nights, down to the thighs.',
		'Weight 890 g (size M). 300 g of 750-fill down. Warm to about -20 °C at rest.' ),
	// Fleece and midlayers.
	array( 'fleece-midlayers', 'Fairfield grid fleece', 89, array( 'Polyester' ), array( 'Hiking', 'Running' ), $apparel, array( 'Moss', 'Ember', 'Sky', 'Slate' ),
		'A grid fleece that traps warmth and lets sweat out, the midlayer for most days on the hill.',
		'Weight 280 g (size M). A half zip and thumb loops. Quick to dry.' ),
	array( 'fleece-midlayers', 'Coniston merino base layer', 75, array( 'Merino wool' ), array( 'Hiking', 'Travel' ), $apparel, array( 'Black', 'Navy', 'Moss' ),
		'A merino wool base layer that stays fresh for days, soft enough for sensitive skin.',
		'Weight 190 g (size M). 200 g/m² merino. Flatlock seams that do not rub under a pack.' ),
	array( 'fleece-midlayers', 'Langdale heavyweight fleece', 109, array( 'Polyester' ), array( 'Camping', 'Travel' ), $apparel, array( 'Sand', 'Slate', 'Navy' ),
		'A thick, warm fleece jacket for camp evenings and cold mornings in the tent.',
		'Weight 520 g (size M). A full zip, a high collar and two zipped pockets.' ),
	array( 'fleece-midlayers', 'Wansfell running top', 55, array( 'Polyester' ), array( 'Running' ), $apparel, array( 'Ember', 'Sky', 'Black', 'Moss' ),
		'A light long-sleeved top for running in cool weather.',
		'Weight 140 g (size M). Wicks sweat and dries in an hour on a line.' ),
	array( 'fleece-midlayers', 'Grisedale wool jumper', 129, array( 'Merino wool' ), array( 'Travel', 'Camping' ), $apparel, array( 'Sand', 'Navy' ),
		'A knitted wool jumper that is warm when wet, for the pub after the walk as much as the walk.',
		'Weight 460 g (size M). Knitted from undyed merino.' ),
	// Hiking boots.
	array( 'hiking-boots', 'Scafell waterproof hiking boot', 179, array( 'Leather', 'Gore-Tex' ), array( 'Hiking', 'Mountaineering' ), $feet, array( 'Brown', 'Black' ),
		'A waterproof leather boot for rough, wet ground, stiff enough for a long day on scree.',
		'Weight 1,380 g a pair (size 42). A Gore-Tex membrane, a Vibram sole and a rubber rand. Fits a C1 crampon.' ),
	array( 'hiking-boots', 'Pillar lightweight hiking boot', 139, array( 'Synthetic', 'Gore-Tex' ), array( 'Hiking', 'Travel' ), $feet, array( 'Slate', 'Moss', 'Brown' ),
		'A light waterproof boot for well-made paths, comfortable from the first walk.',
		'Weight 980 g a pair (size 42). A waterproof membrane and a cushioned midsole.' ),
	array( 'hiking-boots', 'Ennerdale trail shoe', 119, array( 'Synthetic' ), array( 'Running', 'Hiking' ), $feet, array( 'Ember', 'Sky', 'Black', 'Slate' ),
		'A trail running shoe with deep lugs for mud and wet grass.',
		'Weight 620 g a pair (size 42). 5 mm lugs, a rock plate and a 6 mm drop. Not waterproof: it drains and dries.' ),
	array( 'hiking-boots', 'Kirkstone winter boot', 259, array( 'Leather' ), array( 'Mountaineering' ), $feet, array( 'Brown', 'Black' ),
		'An insulated winter mountaineering boot for snow and ice, rated for B2 crampons.',
		'Weight 1,780 g a pair (size 42). Insulated to about -15 °C. A stiff sole and a heel welt.' ),
	array( 'hiking-boots', 'Mardale approach shoe', 129, array( 'Suede' ), array( 'Hiking', 'Travel' ), $feet, array( 'Sand', 'Slate' ),
		'A grippy approach shoe for scrambles and rocky walk-ins.',
		'Weight 760 g a pair (size 42). Sticky rubber at the toe for edging.' ),
	// Backpacks.
	array( 'backpacks', 'Helvellyn 28 litre daypack', 99, array( 'Nylon' ), array( 'Hiking' ), array( '28 L' ), array( 'Slate', 'Ember', 'Moss', 'Black' ),
		'A 28 litre daypack with a ventilated back, the right size for a full day on the fells.',
		'Weight 980 g. A rain cover in the base, a hydration sleeve and hipbelt pockets.' ),
	array( 'backpacks', 'Scafell 45 litre pack', 159, array( 'Nylon' ), array( 'Hiking', 'Camping' ), array( '45 L' ), array( 'Slate', 'Moss', 'Ember' ),
		'A 45 litre pack for hut trips and a night out with light kit.',
		'Weight 1,450 g. An adjustable back length and a lid that comes off.' ),
	array( 'backpacks', 'Ennerdale 65 litre trekking pack', 219, array( 'Nylon' ), array( 'Camping' ), array( '65 L' ), array( 'Slate', 'Navy', 'Moss' ),
		'A 65 litre trekking pack for multi-day routes carrying a tent and food.',
		'Weight 2,100 g. A load-lifting frame, a sleeping bag compartment and a hipbelt that carries most of the weight.' ),
	array( 'backpacks', 'Wasdale 12 litre running vest', 89, array( 'Polyester' ), array( 'Running' ), array( '12 L' ), array( 'Ember', 'Sky', 'Black' ),
		'A 12 litre running vest that holds a waterproof and two soft flasks without bouncing.',
		'Weight 310 g. Front pockets for flasks and a phone.' ),
	array( 'backpacks', 'Borrowdale travel pack', 139, array( 'Polyester' ), array( 'Travel' ), array( '35 L' ), array( 'Black', 'Navy', 'Sand' ),
		'A 35 litre travel pack that opens like a suitcase and fits cabin luggage limits.',
		'Weight 1,200 g. A laptop sleeve and harness that zips away.' ),
	array( 'backpacks', 'Catbells 20 litre kids pack', 49, array( 'Polyester' ), array( 'Hiking' ), array( '20 L' ), array( 'Sky', 'Ember', 'Moss' ),
		'A 20 litre pack sized for children from about seven, with a whistle on the chest strap.',
		'Weight 520 g. Reflective details and a name label inside.' ),
	// Tents.
	array( 'tents', 'Haystacks one-person tent', 289, array( 'Nylon' ), array( 'Camping', 'Mountaineering' ), array( '1 person' ), array( 'Moss', 'Sand' ),
		'A one-person backpacking tent that pitches outer first, so the inner stays dry in rain.',
		'Weight 1,100 g. Hydrostatic head 3,000 mm on the fly, 5,000 mm on the floor. One pole.' ),
	array( 'tents', 'Glaramara two-person tent', 389, array( 'Nylon' ), array( 'Camping' ), array( '2 person' ), array( 'Moss', 'Sand', 'Ember' ),
		'A two-person tent with two doors and two porches, light enough to carry for a week.',
		'Weight 1,750 g. Hydrostatic head 3,000 mm on the fly. Pitches in about five minutes.' ),
	array( 'tents', 'Blencathra mountain tent', 549, array( 'Polyester' ), array( 'Mountaineering', 'Camping' ), array( '2 person' ), array( 'Ember', 'Slate' ),
		'A geodesic mountain tent for exposed, windy pitches and winter snow.',
		'Weight 3,200 g. Four crossing poles and snow valances.' ),
	array( 'tents', 'Langdale family tent', 499, array( 'Polyester' ), array( 'Camping' ), array( '4 person' ), array( 'Sand', 'Moss' ),
		'A four-person tunnel tent with two bedrooms and room to stand in the living area.',
		'Weight 9,800 g. Hydrostatic head 4,000 mm.' ),
	array( 'tents', 'Coniston bivvy bag', 159, array( 'Gore-Tex' ), array( 'Mountaineering', 'Camping' ), array( '1 person' ), array( 'Moss', 'Black' ),
		'A waterproof, breathable bivvy bag for a night under the stars without a tent.',
		'Weight 480 g. A hooped head end that keeps the fabric off your face.' ),
	// Sleeping bags.
	array( 'sleeping-bags', 'Skiddaw three-season down bag', 259, array( 'Down', 'Nylon' ), array( 'Camping' ), array( 'Regular', 'Long' ), array( 'Ember', 'Slate' ),
		'A three-season down sleeping bag for spring to autumn camps.',
		'Comfort -2 °C, limit -8 °C (EN ISO 23537). Weight 980 g. 600 g of 650-fill down.' ),
	array( 'sleeping-bags', 'Bowfell winter down bag', 429, array( 'Down', 'Nylon' ), array( 'Mountaineering', 'Camping' ), array( 'Regular', 'Long' ), array( 'Ember', 'Slate' ),
		'A winter down sleeping bag for snow camps.',
		'Comfort -10 °C, limit -17 °C (EN ISO 23537). Weight 1,650 g.' ),
	array( 'sleeping-bags', 'Fairfield synthetic summer bag', 89, array( 'Synthetic insulation', 'Polyester' ), array( 'Camping', 'Travel' ), array( 'Regular' ), array( 'Moss', 'Sky', 'Navy' ),
		'A light synthetic sleeping bag for summer nights and hostels, warm even if it gets damp.',
		'Comfort 8 °C, limit 3 °C (EN ISO 23537). Weight 750 g.' ),
	array( 'sleeping-bags', 'Grisedale sleeping mat', 99, array( 'Nylon' ), array( 'Camping' ), array( 'Regular', 'Long' ), array( 'Ember', 'Sky' ),
		'An insulated inflatable sleeping mat, 6 cm thick, for cold ground.',
		'R-value 4.2. Weight 540 g. Packs to the size of a water bottle.' ),
	array( 'sleeping-bags', 'Kirkstone silk liner', 45, array( 'Silk' ), array( 'Travel', 'Camping' ), array( 'Regular' ), array( 'Sand', 'Navy' ),
		'A silk sleeping bag liner that adds warmth and keeps a bag clean.',
		'Weight 130 g. Adds about 2 °C.' ),
	// Accessories.
	array( 'accessories', 'Helvellyn waterproof gloves', 49, array( 'Gore-Tex', 'Polyester' ), array( 'Hiking', 'Mountaineering' ), $apparel, array( 'Black', 'Slate', 'Ember' ),
		'Waterproof insulated gloves for wet, cold days on the tops.',
		'Weight 120 g a pair (size M). A waterproof insert and a grippy palm.' ),
	array( 'accessories', 'Catbells merino beanie', 29, array( 'Merino wool' ), array( 'Hiking', 'Travel', 'Running' ), array( 'One size' ), array( 'Ember', 'Moss', 'Navy', 'Sand', 'Slate' ),
		'A merino beanie, warm and thin enough to wear under a hood.',
		'Weight 45 g.' ),
	array( 'accessories', 'Scafell trekking poles', 79, array( 'Aluminium' ), array( 'Hiking' ), array( 'One size' ), array( 'Slate', 'Ember', 'Black' ),
		'A pair of adjustable aluminium trekking poles that take the strain off knees on the descent.',
		'Weight 520 g a pair. Adjusts from 105 to 135 cm. Flick locks and cork grips.' ),
	array( 'accessories', 'Wasdale head torch', 59, array( 'Polycarbonate' ), array( 'Running', 'Camping', 'Hiking' ), array( 'One size' ), array( 'Black', 'Ember', 'Sky' ),
		'A rechargeable head torch, 400 lumens, for winter afternoons that end in the dark.',
		'Weight 85 g. Up to 40 hours on its low setting. Charges by USB-C. Waterproof to IPX4.' ),
	array( 'accessories', 'Borrowdale gaiters', 45, array( 'Nylon' ), array( 'Hiking', 'Mountaineering' ), array( 'S/M', 'L/XL' ), array( 'Black', 'Slate' ),
		'Waterproof gaiters that keep bog water and snow out of the boots.',
		'Weight 240 g a pair. A front zip and a tough instep strap.' ),
	array( 'accessories', 'Ennerdale insulated bottle', 35, array( 'Steel' ), array( 'Camping', 'Travel', 'Hiking' ), array( '750 ml' ), array( 'Moss', 'Ember', 'Sky', 'Slate', 'Sand' ),
		'A 750 ml insulated steel bottle that keeps tea hot for twelve hours.',
		'Weight 380 g. Hot for 12 hours, cold for 24.' ),
	array( 'accessories', 'Kentmere map case', 22, array( 'Polyurethane' ), array( 'Hiking' ), array( 'One size' ), array( 'Black', 'Ember' ),
		'A waterproof map case that takes a folded OS map and hangs round the neck.',
		'Weight 70 g. A roll-top seal.' ),
	array( 'accessories', 'Haystacks dry bag set', 39, array( 'Nylon' ), array( 'Camping', 'Travel' ), array( '3 L, 8 L, 13 L' ), array( 'Ember', 'Sky', 'Moss' ),
		'Three roll-top dry bags that keep a sleeping bag and spare clothes dry inside a pack.',
		'Weight 110 g together. Taped seams.' ),
	array( 'accessories', 'Fairfield camp stove', 69, array( 'Steel' ), array( 'Camping' ), array( 'One size' ), array( 'Slate', 'Black' ),
		'A light gas stove that boils a litre of water in about four minutes.',
		'Weight 95 g. Screws on a standard gas canister. Piezo ignition.' ),
	array( 'accessories', 'Langdale cook set', 55, array( 'Aluminium' ), array( 'Camping' ), array( 'One size' ), array( 'Slate', 'Moss', 'Ember' ),
		'A two-pot aluminium cook set that nests around a gas canister and the stove.',
		'Weight 320 g. A 1.2 litre and a 0.8 litre pot, and a lid that is a frying pan.' ),
);

$care = array(
	'waterproof-jackets' => 'Wash at 30 °C with a technical cleaner, then tumble dry low to restore the water-repellent finish.',
	'insulated-jackets'  => 'Wash at 30 °C with a down or synthetic wash and tumble dry low with two clean tennis balls.',
	'fleece-midlayers'   => 'Machine wash at 30 °C. Do not iron.',
	'hiking-boots'       => 'Brush off mud, dry away from direct heat and reproof with a leather or fabric treatment.',
	'backpacks'          => 'Wipe clean with a damp cloth. Do not machine wash.',
	'tents'              => 'Pitch to dry before storing, loosely packed and out of sunlight.',
	'sleeping-bags'      => 'Store loose in its storage sack, not compressed. Wash rarely, with a specialist wash.',
	'accessories'        => 'Rinse and dry after use; see the label for washing.',
);
$categories = array_replace( $categories, $pt['categories'] ?? array() );
$care       = array_replace( $care, $pt['care'] ?? array() );

$term = static function ( string $taxonomy, string $name ): int {
	$found = term_exists( $name, $taxonomy ) ?: wp_insert_term( $name, $taxonomy );
	return (int) $found['term_id'];
};

$category_ids = array();
foreach ( $categories as $slug => $name ) {
	$found                 = get_term_by( 'slug', $slug, 'product_cat' );
	$category_ids[ $slug ] = $found ? (int) $found->term_id : (int) wp_insert_term( $name, 'product_cat', array( 'slug' => $slug ) )['term_id'];
	// Named in the site's language, whichever language seeded it before.
	if ( $found && $found->name !== $name ) {
		wp_update_term( $found->term_id, 'product_cat', array( 'name' => $name ) );
	}
}

$count = 0;
foreach ( $models as $m => list( $category, $model, $price, $materials, $uses, $sizes, $colours, $lead, $facts ) ) {
	list( $title, $lead, $facts ) = $pt['models'][ $model ] ?? array( $model, $lead, $facts );
	$materials                    = array_map( static fn ( $value ) => $tr( 'terms', $value ), $materials );
	$uses                         = array_map( static fn ( $value ) => $tr( 'terms', $value ), $uses );
	$sizes                        = array_map( static fn ( $value ) => $tr( 'terms', $value ), $sizes );
	foreach ( $colours as $c => $colour_en ) {
		++$count;
		// The SKU keeps the English colour, so a product is found again in either language.
		$sku     = sprintf( 'TF-%02d-%s', $m + 1, strtoupper( substr( $colour_en, 0, 3 ) ) );
		$colour  = $tr( 'terms', $colour_en );
		$id      = wc_get_product_id_by_sku( $sku );
		$product = $id ? wc_get_product( $id ) : new WC_Product_Simple();
		$product->set_name( "{$title}, " . mb_strtolower( $colour ) );
		$product->set_sku( $sku );
		$product->set_short_description( $lead );
		$product->set_description( "{$lead}\n\n{$facts}\n\n" . $tr( 'labels', 'Colour' ) . ": {$colour}. " . $tr( 'labels', 'Materials' ) . ': ' . implode( ', ', $materials ) . ".\n\n" . $tr( 'labels', 'Care' ) . ": {$care[ $category ]}" );
		$product->set_regular_price( (string) $price );
		// One colour in five on sale, a fifth off; one product in eleven sold out.
		$product->set_sale_price( 0 === ( $m + $c ) % 5 ? number_format( $price * 0.8, 2, '.', '' ) : '' );
		$product->set_stock_status( 0 === $count % 11 ? 'outofstock' : 'instock' );
		$product->set_category_ids( array( $category_ids[ $category ] ) );
		$product->set_image_id( tarnfell_product_image( $m, $model, $category, $colour_en, "{$title}, " . mb_strtolower( $colour ) ) );
		$product->set_status( 'publish' );

		$attributes = array();
		foreach ( array( 'color' => array( $colour ), 'material' => $materials, 'use' => $uses, 'size' => $sizes ) as $slug => $values ) {
			$ids       = array_map( static fn ( $value ) => $term( "pa_{$slug}", $value ), $values );
			$attribute = new WC_Product_Attribute();
			$attribute->set_id( wc_attribute_taxonomy_id_by_name( $slug ) );
			$attribute->set_name( "pa_{$slug}" );
			$attribute->set_options( $ids );
			$attribute->set_visible( true );
			$attributes[] = $attribute;
		}
		$product->set_attributes( $attributes );
		$saved = $product->save();
		foreach ( array( 'color' => array( $colour ), 'material' => $materials, 'use' => $uses, 'size' => $sizes ) as $slug => $values ) {
			wp_set_object_terms( $saved, $values, "pa_{$slug}" );
		}
	}
}

// The trail journal: guides and how-tos that name the products above.
$guides = require __DIR__ . '/journal.php';

// Products indexed, the shop's fields as facets: category, price ranges, stock, colour, material, use.
$settings                       = viglet_dumont_get_settings();
$settings['index_custom_types'] = array( 'product' );
viglet_dumont_update_settings( $settings );

$facet   = static fn ( string $source, string $field, string $label, bool $multi = true ) => array(
	'source'      => $source,
	'field'       => $field,
	'type'        => 'STRING',
	'multi'       => $multi,
	'facet'       => true,
	'facet_label' => $label,
);
$mapping            = get_option( 'viglet_dumont_mapping', array() );
$mapping['product'] = array(
	'exclude' => false,
	'fields'  => array(
		$facet( 'taxonomy:product_cat', 'product_cat', $tr( 'labels', 'Category' ) ),
		array(
			'source'      => 'woo:price',
			'field'       => 'price',
			'type'        => 'FLOAT',
			'facet'       => false,
			'facet_label' => 'Price',
			'range'       => true,
			'ranges'      => array(
				array( 'label' => '', 'from' => null, 'to' => 100 ),
				array( 'label' => '', 'from' => 100, 'to' => 250 ),
				array( 'label' => '', 'from' => 250, 'to' => null ),
			),
		),
		array(
			'source' => 'woo:regular_price',
			'field'  => 'regular_price',
			'type'   => 'FLOAT',
		),
		$facet( 'woo:stock_status', 'stock_status', $tr( 'labels', 'Stock' ), false ),
		$facet( 'taxonomy:pa_color', 'pa_color', $tr( 'labels', 'Colour' ) ),
		$facet( 'taxonomy:pa_material', 'pa_material', $tr( 'labels', 'Material' ) ),
		$facet( 'taxonomy:pa_use', 'pa_use', $tr( 'labels', 'Use' ) ),
	),
);
update_option( 'viglet_dumont_mapping', $mapping, false );

// Words a shopper uses for the same thing.
$tuning = new Viglet_Dumont_Tuning();
$have   = $tuning->all();
$kept   = is_wp_error( $have ) ? array() : array_map( static fn ( $row ) => implode( '|', $row['terms'] ), (array) $have['synonyms'] );
foreach ( $pt['synonyms'] ?? array(
	array( 'raincoat', 'rain jacket', 'waterproof jacket', 'waterproof' ),
	array( 'puffer', 'down jacket', 'insulated jacket' ),
	array( 'rucksack', 'backpack', 'daypack' ),
	array( 'jumper', 'sweater', 'fleece' ),
) as $synonym ) {
	if ( ! in_array( implode( '|', $synonym ), $kept, true ) ) {
		$tuning->add_synonym( $synonym );
	}
}

echo wp_json_encode(
	array(
		'products' => $count,
		'guides'   => count( $guides ),
		'pages'    => $pages,
	)
);
