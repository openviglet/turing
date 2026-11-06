<?php
/**
 * The Ask this site block (D173), server side: a conversation with Turing's copilot, driven by
 * src/public/copilot.js. Hidden until Turing says the copilot can answer. With `kit` on (D201)
 * it also offers a plan: a described plan turned into the documents it needs, grouped by need.
 *
 * It speaks as a shop only on a WooCommerce shop (D323): there the default copy names the
 * catalog and the kit, the kit is on unless switched off, and it stages products, cites posts
 * as guides and reads stock from `stock_status`. Anywhere else the copy names this site, the
 * plan is off unless switched on, and what it stages, cites and reads availability from are
 * the block's own `stageTypes`, `guideTypes` and `availabilityField`.
 *
 * @package Viglet_Turing
 *
 * @var array $attributes The block's attributes.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

$viglet_turing_shop    = Viglet_Turing_WooCommerce::active();
$viglet_turing_id      = wp_unique_id( 'viglet-turing-copilot-' );
$viglet_turing_heading = '' !== (string) ( $attributes['heading'] ?? '' )
	? (string) $attributes['heading']
	: ( $viglet_turing_shop ? __( 'Ask the catalog', 'viglet-turing-search' ) : __( 'Ask this site', 'viglet-turing-search' ) );
$viglet_turing_kit     = isset( $attributes['kit'] ) ? (bool) $attributes['kit'] : $viglet_turing_shop;
// The plan alone, for a page of its own: it opens on the plan and offers no question beside it,
// which on a page about planning read as a second, unrelated tool.
$viglet_turing_plan_only = $viglet_turing_kit && ! empty( $attributes['planOnly'] );

/**
 * A comma-separated attribute as a list of names, or the shop's default when it is blank.
 *
 * @param mixed    $value    The attribute.
 * @param string[] $fallback What a shop stages or cites when the block names nothing.
 * @param bool     $shop     Whether WooCommerce is running.
 * @return string[]
 */
$viglet_turing_names        = static function ( $value, array $fallback, bool $shop ): array {
	$names = array_values( array_filter( array_map( 'sanitize_key', explode( ',', (string) $value ) ) ) );
	return $names ? $names : ( $shop ? $fallback : array() );
};
$viglet_turing_stage_types  = $viglet_turing_names( $attributes['stageTypes'] ?? '', array( 'product' ), $viglet_turing_shop );
$viglet_turing_guide_types  = $viglet_turing_names( $attributes['guideTypes'] ?? '', array( 'post' ), $viglet_turing_shop );
$viglet_turing_availability = sanitize_key( (string) ( $attributes['availabilityField'] ?? '' ) );
if ( '' === $viglet_turing_availability && $viglet_turing_shop ) {
	$viglet_turing_availability = 'stock_status';
}

// On a product's page (or any indexed post's), the question is about it (D242): Turing pins
// its document first, and the box says what "this" is.
$viglet_turing_indexer = new Viglet_Turing_Indexer();
$viglet_turing_subject = $viglet_turing_indexer->subject();
$viglet_turing_ask     = $viglet_turing_subject
	/* translators: %s: the name of the product or post the page shows. */
	? sprintf( __( 'Ask about %s', 'viglet-turing-search' ), get_the_title( $viglet_turing_subject ) )
	: __( 'What are you looking for?', 'viglet-turing-search' );

// The words the block speaks in: a shop's catalog and kit, or this site and a plan.
$viglet_turing_copy = $viglet_turing_shop
	? array(
		'thinking'    => __( 'Looking through the catalog…', 'viglet-turing-search' ),
		'failed'      => __( 'The catalog could not answer right now.', 'viglet-turing-search' ),
		'nothing'     => __( 'Nothing in the catalog matches that.', 'viglet-turing-search' ),
		'planMode'    => __( 'Plan a kit', 'viglet-turing-search' ),
		'planButton'  => __( 'Build my kit', 'viglet-turing-search' ),
		'kitThinking' => __( 'Putting your kit together…', 'viglet-turing-search' ),
		'kitNothing'  => __( 'The catalog has nothing for that plan.', 'viglet-turing-search' ),
		/* translators: %s: the needs the catalog has no product for, comma-separated. */
		'kitUnfilled' => __( 'Nothing in the catalog yet for: %s.', 'viglet-turing-search' ),
		/* translators: %s: the product's name. */
		'includeFor'  => __( 'Keep %s in the kit', 'viglet-turing-search' ),
		'out'         => __( 'Out of stock', 'viglet-turing-search' ),
	)
	: array(
		'thinking'    => __( 'Looking through this site…', 'viglet-turing-search' ),
		'failed'      => __( 'This site could not answer right now.', 'viglet-turing-search' ),
		'nothing'     => __( 'Nothing on this site matches that.', 'viglet-turing-search' ),
		'planMode'    => __( 'Make a plan', 'viglet-turing-search' ),
		'planButton'  => __( 'Build my plan', 'viglet-turing-search' ),
		'kitThinking' => __( 'Putting your plan together…', 'viglet-turing-search' ),
		'kitNothing'  => __( 'Nothing on this site fits that plan.', 'viglet-turing-search' ),
		/* translators: %s: the needs nothing on the site covers, comma-separated. */
		'kitUnfilled' => __( 'Nothing on this site yet for: %s.', 'viglet-turing-search' ),
		/* translators: %s: the name of a page, event or other item. */
		'includeFor'  => __( 'Keep %s in the plan', 'viglet-turing-search' ),
		'out'         => __( 'Unavailable', 'viglet-turing-search' ),
	);

wp_interactivity_state(
	'viglet-turing/copilot',
	array(
		// What the plan stages, cites and reads availability from (D323).
		'stageTypes'        => $viglet_turing_stage_types,
		'guideTypes'        => $viglet_turing_guide_types,
		'availabilityField' => $viglet_turing_availability,
		'available'         => false,
		'question'          => '',
		'turns'             => array(),
		'busy'              => false,
		'status'            => '',
		/* translators: %s: the product's name. */
		'cartLabel'         => __( 'Add to cart: %s', 'viglet-turing-search' ),
		// Compare on a cited product (D212), into the list the search cards use.
		/* translators: %s: a product's name. */
		'compareFor'        => __( 'Compare %s', 'viglet-turing-search' ),
		/* translators: %d: how many products, two to four. */
		'compareLabel'      => __( 'Compare %d products', 'viglet-turing-search' ),
		'cartAdded'         => __( 'Added to your cart.', 'viglet-turing-search' ),
		'cartFailed'        => __( 'It could not be added to the cart.', 'viglet-turing-search' ),
		'thinking'          => $viglet_turing_copy['thinking'],
		'failed'            => $viglet_turing_copy['failed'],
		'nothing'           => $viglet_turing_copy['nothing'],
		// Turing refused with 429: the site's AI budget is spent until it resets (D332).
		// A spent AI budget, with its reset when the site or the 429 says it (D336).
		'paused'            => Viglet_Turing_AI_Quota::visitor_line(),
		'pausedUntil'       => Viglet_Turing_AI_Quota::visitor_until(),
		// Build my kit (D201).
		'kitOffered'        => $viglet_turing_kit,
		'mode'              => $viglet_turing_plan_only ? 'kit' : 'ask',
		'kit'               => array(
			'shown'    => false,
			'summary'  => '',
			'stages'   => array(),
			'unfilled' => '',
			'guides'   => array(),
		),
		'kitBusy'           => false,
		'kitStatus'         => '',
		'askLabel'          => $viglet_turing_ask,
		'documentId'        => $viglet_turing_subject ? $viglet_turing_indexer->build_doc_id( $viglet_turing_subject->ID ) : '',
		'askButton'         => __( 'Ask', 'viglet-turing-search' ),
		'planLabel'         => __( 'Describe your plan', 'viglet-turing-search' ),
		'planButton'        => $viglet_turing_copy['planButton'],
		'kitThinking'       => $viglet_turing_copy['kitThinking'],
		'kitNothing'        => $viglet_turing_copy['kitNothing'],
		'kitUnfilled'       => $viglet_turing_copy['kitUnfilled'],
		'includeFor'        => $viglet_turing_copy['includeFor'],
		/* translators: %d: how many products. */
		'kitAdd'            => __( 'Add %d products to the cart', 'viglet-turing-search' ),
		'kitAddOne'         => __( 'Add 1 product to the cart', 'viglet-turing-search' ),
		/* translators: 1: how many products were added, 2: the cart's total, as the shop formats it. */
		'kitAdded'          => __( 'Added %1$d to your cart. Cart total: %2$s.', 'viglet-turing-search' ),
		/* translators: 1: how many were added, 2: how many were asked for. */
		'kitPartial'        => __( 'Added %1$d of %2$d to your cart; the rest could not be added.', 'viglet-turing-search' ),
	)
);
?>
<section
	<?php echo wp_kses_data( get_block_wrapper_attributes( array( 'class' => 'viglet-turing-copilot' ) ) ); ?>
	data-wp-interactive="viglet-turing/copilot"
	data-wp-init="callbacks.init"
	aria-labelledby="<?php echo esc_attr( $viglet_turing_id ); ?>-heading"
	hidden
	data-wp-bind--hidden="!state.available"
>
	<h2 id="<?php echo esc_attr( $viglet_turing_id ); ?>-heading"><?php echo esc_html( $viglet_turing_heading ); ?></h2>
	<?php if ( $viglet_turing_kit && ! $viglet_turing_plan_only ) : ?>
	<div class="viglet-turing-copilot__modes" role="group" aria-label="<?php esc_attr_e( 'Mode', 'viglet-turing-search' ); ?>">
		<button type="button" data-mode="ask" data-wp-on--click="actions.setMode" data-wp-bind--aria-pressed="state.asking"><?php esc_html_e( 'Ask a question', 'viglet-turing-search' ); ?></button>
		<button type="button" data-mode="kit" data-wp-on--click="actions.setMode" data-wp-bind--aria-pressed="state.planning"><?php echo esc_html( $viglet_turing_copy['planMode'] ); ?></button>
	</div>
	<?php endif; ?>
	<ol class="viglet-turing-copilot__turns" hidden data-wp-bind--hidden="state.planning">
		<template data-wp-each--turn="state.turns" data-wp-each-key="context.turn.key">
			<li class="viglet-turing-copilot__turn">
				<p class="viglet-turing-copilot__question" data-wp-text="context.turn.question"></p>
				<div class="viglet-turing-copilot__answer" data-wp-watch="callbacks.renderAnswer"></div>
				<ol class="viglet-turing-copilot__items" hidden data-wp-bind--hidden="!context.turn.items.length">
					<template data-wp-each--item="context.turn.items" data-wp-each-key="context.item.key">
						<li class="viglet-turing-copilot__item">
							<span class="viglet-turing-copilot__rank" data-wp-text="context.item.rank"></span>
							<a data-wp-bind--href="context.item.url" data-wp-text="context.item.title"></a>
							<span class="viglet-turing-copilot__shop" hidden data-wp-bind--hidden="!context.item.price">
								<span class="viglet-turing-copilot__price">
									<del hidden data-wp-bind--hidden="!context.item.regular"><span class="screen-reader-text"><?php esc_html_e( 'Was', 'viglet-turing-search' ); ?></span> <span data-wp-text="context.item.regular"></span></del>
									<strong data-wp-text="context.item.price"></strong>
								</span>
								<button type="button" class="wp-element-button" data-wp-on--click="actions.addToCart" data-wp-bind--disabled="context.item.adding" data-wp-bind--hidden="!context.item.buyable" data-wp-bind--aria-label="context.item.cartLabel"><?php esc_html_e( 'Add to cart', 'viglet-turing-search' ); ?></button>
								<span role="status" data-wp-text="context.item.cartStatus"></span>
								<label class="viglet-turing-copilot__compare" hidden data-wp-bind--hidden="!state.canCompare">
									<input type="checkbox" data-wp-bind--aria-label="context.item.compareFor" data-wp-bind--checked="state.compared" data-wp-bind--disabled="state.compareFull" data-wp-on--change="actions.toggleCompare" />
									<?php esc_html_e( 'Compare', 'viglet-turing-search' ); ?>
								</label>
							</span>
						</li>
					</template>
				</ol>
			</li>
		</template>
	</ol>
	<?php
	// Compare is for the products an answer cites; the plan alone cites none, and the bar it
	// would show is the visitor's list from elsewhere, out of place above a plan.
	if ( ! $viglet_turing_plan_only ) :
		?>
	<p class="viglet-turing-copilot__compare-bar" hidden data-wp-bind--hidden="!state.compareReady">
		<a class="wp-element-button" data-wp-bind--href="state.compareUrl" data-wp-text="state.compareText"></a>
	</p>
	<?php endif; ?>
	<?php if ( $viglet_turing_kit ) : ?>
	<div class="viglet-turing-copilot__kit" hidden data-wp-bind--hidden="!state.kitShown">
		<p class="viglet-turing-copilot__kit-summary" data-wp-text="state.kit.summary"></p>
		<ol class="viglet-turing-copilot__stages">
			<template data-wp-each--stage="state.kit.stages" data-wp-each-key="context.stage.key">
				<li class="viglet-turing-copilot__stage">
					<h3 data-wp-text="context.stage.gap"></h3>
					<p class="viglet-turing-copilot__stage-reason" hidden data-wp-bind--hidden="!context.stage.reason" data-wp-text="context.stage.reason"></p>
					<ul class="viglet-turing-copilot__kit-items">
						<template data-wp-each--product="context.stage.products" data-wp-each-key="context.product.key">
							<li class="viglet-turing-copilot__kit-item" data-wp-class--is-out="context.product.out">
								<input type="checkbox" data-wp-bind--checked="context.product.included" data-wp-bind--disabled="context.product.out" data-wp-bind--aria-label="context.product.includeFor" data-wp-on--change="actions.toggleInclude" />
								<span class="viglet-turing-copilot__kit-name">
									<a data-wp-bind--href="context.product.url" data-wp-text="context.product.title"></a>
									<span class="viglet-turing-copilot__kit-reason" data-wp-text="context.product.reason"></span>
								</span>
								<strong hidden data-wp-bind--hidden="!context.product.price" data-wp-text="context.product.price"></strong>
								<span class="viglet-turing-copilot__out" hidden data-wp-bind--hidden="!context.product.out"><?php echo esc_html( $viglet_turing_copy['out'] ); ?></span>
							</li>
						</template>
					</ul>
				</li>
			</template>
		</ol>
		<p class="viglet-turing-copilot__unfilled" hidden data-wp-bind--hidden="!state.kit.unfilled" data-wp-text="state.kit.unfilled"></p>
		<div class="viglet-turing-copilot__guides" hidden data-wp-bind--hidden="!state.kit.guides.length">
			<h3><?php esc_html_e( 'Guides for this plan', 'viglet-turing-search' ); ?></h3>
			<ul>
				<template data-wp-each--guide="state.kit.guides" data-wp-each-key="context.guide.key">
					<li><a data-wp-bind--href="context.guide.url" data-wp-text="context.guide.title"></a></li>
				</template>
			</ul>
		</div>
		<p class="viglet-turing-copilot__kit-cart" hidden data-wp-bind--hidden="!state.kitCanAdd">
			<button type="button" class="wp-element-button" data-wp-on--click="actions.addKit" data-wp-bind--disabled="state.kitBusy" data-wp-text="state.kitAddText"></button>
		</p>
		<p class="viglet-turing-copilot__kit-status" role="status" data-wp-text="state.kitStatus"></p>
	</div>
	<?php endif; ?>
	<form class="viglet-turing-copilot__form" data-wp-on--submit="actions.ask">
		<label for="<?php echo esc_attr( $viglet_turing_id ); ?>" data-wp-text="state.formLabel"><?php echo esc_html( $viglet_turing_ask ); ?></label>
		<input
			type="text"
			id="<?php echo esc_attr( $viglet_turing_id ); ?>"
			autocomplete="off"
			placeholder="<?php echo esc_attr( (string) ( $attributes['placeholder'] ?? '' ) ); ?>"
			data-wp-bind--value="state.question"
			data-wp-on--input="actions.type"
		/>
		<button type="submit" class="wp-element-button" data-wp-bind--disabled="state.busy" data-wp-text="state.formButton"><?php esc_html_e( 'Ask', 'viglet-turing-search' ); ?></button>
	</form>
	<p class="viglet-turing-copilot__status" role="status" data-wp-text="state.status"></p>
</section>
