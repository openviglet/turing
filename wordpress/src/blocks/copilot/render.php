<?php
/**
 * The Ask the catalog block (D173), server side: a conversation with Turing's catalog
 * copilot, driven by src/public/copilot.js. Hidden until Turing says the copilot can answer.
 * With `kit` on (D201) it also offers Build my kit: a described plan turned into the
 * products it needs, grouped by need, that go in the cart together.
 *
 * @package Viglet_Dumont
 *
 * @var array $attributes The block's attributes.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

$viglet_dumont_id      = wp_unique_id( 'viglet-dumont-copilot-' );
$viglet_dumont_heading = '' !== (string) ( $attributes['heading'] ?? '' ) ? (string) $attributes['heading'] : __( 'Ask the catalog', 'viglet-dumont' );
$viglet_dumont_kit     = (bool) ( $attributes['kit'] ?? true );

// On a product's page (or any indexed post's), the question is about it (D242): Turing pins
// its document first, and the box says what "this" is.
$viglet_dumont_indexer = new Viglet_Dumont_Indexer();
$viglet_dumont_subject = $viglet_dumont_indexer->subject();
$viglet_dumont_ask     = $viglet_dumont_subject
	/* translators: %s: the name of the product or post the page shows. */
	? sprintf( __( 'Ask about %s', 'viglet-dumont' ), get_the_title( $viglet_dumont_subject ) )
	: __( 'What are you looking for?', 'viglet-dumont' );

wp_interactivity_state(
	'viglet-dumont/copilot',
	array(
		'available'    => false,
		'question'     => '',
		'turns'        => array(),
		'busy'         => false,
		'status'       => '',
		/* translators: %s: the product's name. */
		'cartLabel'    => __( 'Add to cart: %s', 'viglet-dumont' ),
		// Compare on a cited product (D212), into the list the search cards use.
		/* translators: %s: a product's name. */
		'compareFor'   => __( 'Compare %s', 'viglet-dumont' ),
		/* translators: %d: how many products, two to four. */
		'compareLabel' => __( 'Compare %d products', 'viglet-dumont' ),
		'cartAdded'    => __( 'Added to your cart.', 'viglet-dumont' ),
		'cartFailed'   => __( 'It could not be added to the cart.', 'viglet-dumont' ),
		'thinking'     => __( 'Looking through the catalog…', 'viglet-dumont' ),
		'failed'       => __( 'The catalog could not answer right now.', 'viglet-dumont' ),
		'nothing'      => __( 'Nothing in the catalog matches that.', 'viglet-dumont' ),
		// Build my kit (D201).
		'kitOffered'   => $viglet_dumont_kit,
		'mode'         => 'ask',
		'kit'          => array(
			'shown'    => false,
			'summary'  => '',
			'stages'   => array(),
			'unfilled' => '',
			'guides'   => array(),
		),
		'kitBusy'      => false,
		'kitStatus'    => '',
		'askLabel'     => $viglet_dumont_ask,
		'documentId'   => $viglet_dumont_subject ? $viglet_dumont_indexer->build_doc_id( $viglet_dumont_subject->ID ) : '',
		'askButton'    => __( 'Ask', 'viglet-dumont' ),
		'planLabel'    => __( 'Describe your plan', 'viglet-dumont' ),
		'planButton'   => __( 'Build my kit', 'viglet-dumont' ),
		'kitThinking'  => __( 'Putting your kit together…', 'viglet-dumont' ),
		'kitNothing'   => __( 'The catalog has nothing for that plan.', 'viglet-dumont' ),
		/* translators: %s: the needs the catalog has no product for, comma-separated. */
		'kitUnfilled'  => __( 'Nothing in the catalog yet for: %s.', 'viglet-dumont' ),
		/* translators: %s: the product's name. */
		'includeFor'   => __( 'Keep %s in the kit', 'viglet-dumont' ),
		/* translators: %d: how many products. */
		'kitAdd'       => __( 'Add %d products to the cart', 'viglet-dumont' ),
		'kitAddOne'    => __( 'Add 1 product to the cart', 'viglet-dumont' ),
		/* translators: 1: how many products were added, 2: the cart's total, as the shop formats it. */
		'kitAdded'     => __( 'Added %1$d to your cart. Cart total: %2$s.', 'viglet-dumont' ),
		/* translators: 1: how many were added, 2: how many were asked for. */
		'kitPartial'   => __( 'Added %1$d of %2$d to your cart; the rest could not be added.', 'viglet-dumont' ),
	)
);
?>
<section
	<?php echo get_block_wrapper_attributes( array( 'class' => 'viglet-dumont-copilot' ) ); // phpcs:ignore WordPress.Security.EscapeOutput.OutputNotEscaped -- core escapes the wrapper attributes. ?>
	data-wp-interactive="viglet-dumont/copilot"
	data-wp-init="callbacks.init"
	aria-labelledby="<?php echo esc_attr( $viglet_dumont_id ); ?>-heading"
	hidden
	data-wp-bind--hidden="!state.available"
>
	<h2 id="<?php echo esc_attr( $viglet_dumont_id ); ?>-heading"><?php echo esc_html( $viglet_dumont_heading ); ?></h2>
	<?php if ( $viglet_dumont_kit ) : ?>
	<div class="viglet-dumont-copilot__modes" role="group" aria-label="<?php esc_attr_e( 'Mode', 'viglet-dumont' ); ?>">
		<button type="button" data-mode="ask" data-wp-on--click="actions.setMode" data-wp-bind--aria-pressed="state.asking"><?php esc_html_e( 'Ask a question', 'viglet-dumont' ); ?></button>
		<button type="button" data-mode="kit" data-wp-on--click="actions.setMode" data-wp-bind--aria-pressed="state.planning"><?php esc_html_e( 'Plan a kit', 'viglet-dumont' ); ?></button>
	</div>
	<?php endif; ?>
	<ol class="viglet-dumont-copilot__turns" hidden data-wp-bind--hidden="state.planning">
		<template data-wp-each--turn="state.turns" data-wp-each-key="context.turn.key">
			<li class="viglet-dumont-copilot__turn">
				<p class="viglet-dumont-copilot__question" data-wp-text="context.turn.question"></p>
				<div class="viglet-dumont-copilot__answer" data-wp-watch="callbacks.renderAnswer"></div>
				<ol class="viglet-dumont-copilot__items" hidden data-wp-bind--hidden="!context.turn.items.length">
					<template data-wp-each--item="context.turn.items" data-wp-each-key="context.item.key">
						<li class="viglet-dumont-copilot__item">
							<span class="viglet-dumont-copilot__rank" data-wp-text="context.item.rank"></span>
							<a data-wp-bind--href="context.item.url" data-wp-text="context.item.title"></a>
							<span class="viglet-dumont-copilot__shop" hidden data-wp-bind--hidden="!context.item.price">
								<del hidden data-wp-bind--hidden="!context.item.regular"><span class="screen-reader-text"><?php esc_html_e( 'Was', 'viglet-dumont' ); ?></span> <span data-wp-text="context.item.regular"></span></del>
								<strong data-wp-text="context.item.price"></strong>
								<button type="button" class="wp-element-button" data-wp-on--click="actions.addToCart" data-wp-bind--disabled="context.item.adding" data-wp-bind--hidden="!context.item.buyable" data-wp-bind--aria-label="context.item.cartLabel"><?php esc_html_e( 'Add to cart', 'viglet-dumont' ); ?></button>
								<span role="status" data-wp-text="context.item.cartStatus"></span>
								<label class="viglet-dumont-copilot__compare" hidden data-wp-bind--hidden="!state.canCompare">
									<input type="checkbox" data-wp-bind--aria-label="context.item.compareFor" data-wp-bind--checked="state.compared" data-wp-bind--disabled="state.compareFull" data-wp-on--change="actions.toggleCompare" />
									<?php esc_html_e( 'Compare', 'viglet-dumont' ); ?>
								</label>
							</span>
						</li>
					</template>
				</ol>
			</li>
		</template>
	</ol>
	<p class="viglet-dumont-copilot__compare-bar" hidden data-wp-bind--hidden="!state.compareReady">
		<a class="wp-element-button" data-wp-bind--href="state.compareUrl" data-wp-text="state.compareText"></a>
	</p>
	<?php if ( $viglet_dumont_kit ) : ?>
	<div class="viglet-dumont-copilot__kit" hidden data-wp-bind--hidden="!state.kitShown">
		<p class="viglet-dumont-copilot__kit-summary" data-wp-text="state.kit.summary"></p>
		<ol class="viglet-dumont-copilot__stages">
			<template data-wp-each--stage="state.kit.stages" data-wp-each-key="context.stage.key">
				<li class="viglet-dumont-copilot__stage">
					<h3 data-wp-text="context.stage.gap"></h3>
					<p class="viglet-dumont-copilot__stage-reason" hidden data-wp-bind--hidden="!context.stage.reason" data-wp-text="context.stage.reason"></p>
					<ul class="viglet-dumont-copilot__kit-items">
						<template data-wp-each--product="context.stage.products" data-wp-each-key="context.product.key">
							<li class="viglet-dumont-copilot__kit-item" data-wp-class--is-out="context.product.out">
								<input type="checkbox" data-wp-bind--checked="context.product.included" data-wp-bind--disabled="context.product.out" data-wp-bind--aria-label="context.product.includeFor" data-wp-on--change="actions.toggleInclude" />
								<span class="viglet-dumont-copilot__kit-name">
									<a data-wp-bind--href="context.product.url" data-wp-text="context.product.title"></a>
									<span class="viglet-dumont-copilot__kit-reason" data-wp-text="context.product.reason"></span>
								</span>
								<strong hidden data-wp-bind--hidden="!context.product.price" data-wp-text="context.product.price"></strong>
								<span class="viglet-dumont-copilot__out" hidden data-wp-bind--hidden="!context.product.out"><?php esc_html_e( 'Out of stock', 'viglet-dumont' ); ?></span>
							</li>
						</template>
					</ul>
				</li>
			</template>
		</ol>
		<p class="viglet-dumont-copilot__unfilled" hidden data-wp-bind--hidden="!state.kit.unfilled" data-wp-text="state.kit.unfilled"></p>
		<div class="viglet-dumont-copilot__guides" hidden data-wp-bind--hidden="!state.kit.guides.length">
			<h3><?php esc_html_e( 'Guides for this plan', 'viglet-dumont' ); ?></h3>
			<ul>
				<template data-wp-each--guide="state.kit.guides" data-wp-each-key="context.guide.key">
					<li><a data-wp-bind--href="context.guide.url" data-wp-text="context.guide.title"></a></li>
				</template>
			</ul>
		</div>
		<p class="viglet-dumont-copilot__kit-cart" hidden data-wp-bind--hidden="!state.kitCanAdd">
			<button type="button" class="wp-element-button" data-wp-on--click="actions.addKit" data-wp-bind--disabled="state.kitBusy" data-wp-text="state.kitAddText"></button>
		</p>
		<p class="viglet-dumont-copilot__kit-status" role="status" data-wp-text="state.kitStatus"></p>
	</div>
	<?php endif; ?>
	<form class="viglet-dumont-copilot__form" data-wp-on--submit="actions.ask">
		<label for="<?php echo esc_attr( $viglet_dumont_id ); ?>" data-wp-text="state.formLabel"><?php echo esc_html( $viglet_dumont_ask ); ?></label>
		<input
			type="text"
			id="<?php echo esc_attr( $viglet_dumont_id ); ?>"
			autocomplete="off"
			placeholder="<?php echo esc_attr( (string) ( $attributes['placeholder'] ?? '' ) ); ?>"
			data-wp-bind--value="state.question"
			data-wp-on--input="actions.type"
		/>
		<button type="submit" class="wp-element-button" data-wp-bind--disabled="state.busy" data-wp-text="state.formButton"><?php esc_html_e( 'Ask', 'viglet-dumont' ); ?></button>
	</form>
	<p class="viglet-dumont-copilot__status" role="status" data-wp-text="state.status"></p>
</section>
