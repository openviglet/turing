<?php
/**
 * A piece of the Turing search block (D185). The results: promoted ones, the list, and the loading, empty and error states.
 *
 * Drawn by Viglet_Dumont_Search_Block::pieces(), for the block or one of its inner blocks.
 *
 * @package Viglet_Dumont
 *
 * @var array    $attributes         The search block's attributes.
 * @var string   $viglet_dumont_id   The id the block's pieces share.
 * @var callable $viglet_dumont_show Whether a part of a result shows.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

// Where an empty search can send the visitor: the latest posts, from WordPress itself.
$viglet_dumont_recent = array_map(
	static fn ( WP_Post $post ) => array(
		'title' => get_the_title( $post ),
		'url'   => get_permalink( $post ),
	),
	get_posts(
		array(
			'numberposts' => 3,
			'post_status' => 'publish',
		)
	)
);
?>
<div class="viglet-dumont-search__main">
	<ol class="viglet-dumont-search__spotlights" aria-label="<?php esc_attr_e( 'Featured', 'viglet-dumont' ); ?>" hidden data-wp-bind--hidden="!state.hasSpotlights">
		<template data-wp-each--spot="state.spotlights" data-wp-each-key="context.spot.url">
			<li class="viglet-dumont-search__result viglet-dumont-search__result--featured">
				<div class="viglet-dumont-search__body">
					<p class="viglet-dumont-search__meta"><span class="viglet-dumont-search__featured"><?php esc_html_e( 'Featured', 'viglet-dumont' ); ?></span></p>
					<h2 class="viglet-dumont-search__title"><a data-wp-bind--href="context.spot.url" data-wp-text="context.spot.title"></a></h2>
					<p class="viglet-dumont-search__snippet" data-wp-text="context.spot.content"></p>
				</div>
			</li>
		</template>
	</ol>
	<ol class="viglet-dumont-search__results" data-wp-bind--aria-busy="state.isLoading" data-wp-class--is-loading="state.isLoading">
		<template data-wp-each--result="state.results">
			<li class="viglet-dumont-search__result">
				<?php if ( $viglet_dumont_show( 'showImage' ) ) : ?>
					<img class="viglet-dumont-search__image" alt="" loading="lazy" hidden data-wp-bind--hidden="!context.result.image" data-wp-bind--src="context.result.image" />
				<?php endif; ?>
				<div class="viglet-dumont-search__body">
					<?php if ( $viglet_dumont_show( 'showType' ) || $viglet_dumont_show( 'showDate' ) ) : ?>
						<p class="viglet-dumont-search__meta">
							<?php if ( $viglet_dumont_show( 'showType' ) ) : ?>
								<span class="viglet-dumont-search__type" data-wp-text="context.result.type"></span>
							<?php endif; ?>
							<?php if ( $viglet_dumont_show( 'showDate' ) ) : ?>
								<time data-wp-bind--hidden="!context.result.date" data-wp-bind--datetime="context.result.iso" data-wp-text="context.result.date"></time>
							<?php endif; ?>
						</p>
					<?php endif; ?>
					<h2 class="viglet-dumont-search__title"><a data-wp-bind--href="context.result.url" data-wp-text="context.result.title" data-wp-on--click="actions.resultClicked" data-wp-on--auxclick="actions.resultClicked"></a></h2>
					<p class="viglet-dumont-search__crumb" data-wp-text="context.result.crumb"></p>
					<?php if ( $viglet_dumont_show( 'showSnippet' ) ) : ?>
						<p class="viglet-dumont-search__snippet" data-wp-text="context.result.snippetText" data-wp-watch="callbacks.snippet"></p>
					<?php endif; ?>
					<?php if ( Viglet_Dumont_WooCommerce::active() ) : ?>
						<?php
						// A WooCommerce product's price and "Add to cart" (D173).
						?>
						<div class="viglet-dumont-search__shop" hidden data-wp-bind--hidden="!context.result.price">
							<p class="viglet-dumont-search__price">
								<del hidden data-wp-bind--hidden="!context.result.regular"><span class="screen-reader-text"><?php esc_html_e( 'Was', 'viglet-dumont' ); ?></span> <span data-wp-text="context.result.regular"></span></del>
								<span data-wp-text="context.result.price"></span>
							</p>
							<button type="button" class="wp-element-button viglet-dumont-search__cart" data-wp-on--click="actions.addToCart" data-wp-bind--disabled="state.resultAdding" data-wp-bind--aria-label="context.result.cartLabel"><?php esc_html_e( 'Add to cart', 'viglet-dumont' ); ?></button>
							<span class="viglet-dumont-search__cart-status" role="status" data-wp-text="state.resultCartStatus"></span>
							<label class="viglet-dumont-search__compare" hidden data-wp-bind--hidden="!state.canCompare">
								<input type="checkbox" data-wp-bind--aria-label="context.result.compareFor" data-wp-bind--checked="state.compared" data-wp-bind--disabled="state.compareFull" data-wp-on--change="actions.toggleCompare" />
								<?php esc_html_e( 'Compare', 'viglet-dumont' ); ?>
							</label>
						</div>
					<?php endif; ?>
				</div>
			</li>
		</template>
	</ol>
	<?php if ( Viglet_Dumont_WooCommerce::active() ) : ?>
		<p class="viglet-dumont-search__compare-bar" hidden data-wp-bind--hidden="!state.compareReady">
			<a class="wp-element-button" data-wp-bind--href="state.compareUrl" data-wp-text="state.compareText"></a>
		</p>
	<?php endif; ?>

	<ol class="viglet-dumont-search__skeleton" aria-hidden="true" hidden data-wp-bind--hidden="!state.showSkeleton">
		<li></li>
		<li></li>
		<li></li>
	</ol>

	<div class="viglet-dumont-search__empty" hidden data-wp-bind--hidden="!state.isEmpty">
		<?php if ( $viglet_dumont_recent ) : ?>
			<p><?php esc_html_e( 'Or start from the latest posts:', 'viglet-dumont' ); ?></p>
			<ul>
				<?php foreach ( $viglet_dumont_recent as $viglet_dumont_post ) : ?>
					<li><a href="<?php echo esc_url( $viglet_dumont_post['url'] ); ?>"><?php echo esc_html( $viglet_dumont_post['title'] ); ?></a></li>
				<?php endforeach; ?>
			</ul>
		<?php endif; ?>
	</div>

	<div class="viglet-dumont-search__error" hidden data-wp-bind--hidden="!state.isError">
		<button type="button" class="wp-element-button" data-wp-on--click="actions.retry"><?php esc_html_e( 'Try again', 'viglet-dumont' ); ?></button>
		<a data-wp-bind--href="state.wordpressSearchUrl"><?php esc_html_e( 'Search with WordPress instead', 'viglet-dumont' ); ?></a>
	</div>
</div>
