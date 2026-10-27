<?php
/**
 * The Ask the catalog block (D173), server side: a conversation with Turing's catalog
 * copilot, driven by src/public/copilot.js. Hidden until Turing says the copilot can answer.
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
	<ol class="viglet-dumont-copilot__turns">
		<template data-wp-each--turn="state.turns" data-wp-each-key="context.turn.key">
			<li class="viglet-dumont-copilot__turn">
				<p class="viglet-dumont-copilot__question" data-wp-text="context.turn.question"></p>
				<p class="viglet-dumont-copilot__answer" data-wp-text="context.turn.answer"></p>
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
	<form class="viglet-dumont-copilot__form" data-wp-on--submit="actions.ask">
		<label for="<?php echo esc_attr( $viglet_dumont_id ); ?>"><?php esc_html_e( 'What are you looking for?', 'viglet-dumont' ); ?></label>
		<input
			type="text"
			id="<?php echo esc_attr( $viglet_dumont_id ); ?>"
			autocomplete="off"
			placeholder="<?php echo esc_attr( (string) ( $attributes['placeholder'] ?? '' ) ); ?>"
			data-wp-bind--value="state.question"
			data-wp-on--input="actions.type"
		/>
		<button type="submit" class="wp-element-button" data-wp-bind--disabled="state.busy"><?php esc_html_e( 'Ask', 'viglet-dumont' ); ?></button>
	</form>
	<p class="viglet-dumont-copilot__status" role="status" data-wp-text="state.status"></p>
</section>
