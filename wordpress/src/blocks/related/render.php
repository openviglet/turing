<?php
/**
 * The Related content block (D177): the posts Turing finds most like the one being read,
 * drawn on the server so the links are in the HTML for crawlers and visitors without
 * JavaScript. related.js only reports which one was opened (D176).
 *
 * @package Viglet_Dumont
 *
 * @var array    $attributes The block's attributes.
 * @var WP_Block $block      The block, with the post it sits in as context.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

$viglet_dumont_post_id = (int) ( $block->context['postId'] ?? get_the_ID() );
$viglet_dumont_preview = Viglet_Dumont_Native_Search::is_editor_preview();
$viglet_dumont_posts   = $viglet_dumont_post_id > 0
	? ( new Viglet_Dumont_Related() )->posts(
		$viglet_dumont_post_id,
		max( 1, min( 12, (int) ( $attributes['count'] ?? 3 ) ) ),
		! empty( $attributes['sameType'] ),
		(string) ( $attributes['mode'] ?? '' )
	)
	: array();

// A post with no neighbours gets nothing, not an empty box; the editor says why instead.
if ( ! $viglet_dumont_posts ) {
	if ( $viglet_dumont_preview ) {
		printf(
			'<p %s>%s</p>',
			wp_kses_data( get_block_wrapper_attributes( array( 'class' => 'viglet-dumont-related__empty' ) ) ),
			esc_html__( 'Related posts appear here once this post is indexed and Turing finds posts like it.', 'viglet-turing-search' )
		);
	}
	return;
}

$viglet_dumont_heading = '' !== trim( (string) ( $attributes['heading'] ?? '' ) )
	? (string) $attributes['heading']
	: __( 'Related posts', 'viglet-turing-search' );
$viglet_dumont_id      = wp_unique_id( 'viglet-dumont-related-' );
$viglet_dumont_doc_ids = new Viglet_Dumont_Indexer();
// A product among them offers Compare, ticked into the list the search cards use (D212).
$viglet_dumont_shop = Viglet_Dumont_WooCommerce::active()
	&& array_filter( $viglet_dumont_posts, static fn ( WP_Post $post ) => 'product' === $post->post_type );
if ( $viglet_dumont_shop ) {
	wp_interactivity_state(
		'viglet-dumont/related',
		array(
			/* translators: %d: how many products, two to four. */
			'compareLabel' => __( 'Compare %d products', 'viglet-turing-search' ),
		)
	);
}
?>
<section
	<?php
	echo wp_kses_data(
		get_block_wrapper_attributes(
			array(
				'class'               => 'viglet-dumont-related--' . ( 'list' === ( $attributes['layout'] ?? 'grid' ) ? 'list' : 'grid' ),
				'aria-labelledby'     => $viglet_dumont_id,
				'data-wp-interactive' => 'viglet-dumont/related',
			)
		)
	);
	?>
>
	<h2 class="viglet-dumont-related__heading" id="<?php echo esc_attr( $viglet_dumont_id ); ?>"><?php echo esc_html( $viglet_dumont_heading ); ?></h2>
	<ul class="viglet-dumont-related__list">
		<?php foreach ( $viglet_dumont_posts as $viglet_dumont_index => $viglet_dumont_related ) : ?>
			<?php
			$viglet_dumont_url     = get_permalink( $viglet_dumont_related );
			$viglet_dumont_image   = ! empty( $attributes['showImage'] ) && has_post_thumbnail( $viglet_dumont_related )
				? get_the_post_thumbnail(
					$viglet_dumont_related,
					'medium_large',
					array(
						'alt'     => '',
						'loading' => 'lazy',
					)
				)
				: '';
			$viglet_dumont_context = array(
				'id'        => $viglet_dumont_doc_ids->build_doc_id( $viglet_dumont_related->ID ),
				'position'  => $viglet_dumont_index + 1,
				'productId' => $viglet_dumont_related->ID,
			);
			?>
			<li
				class="viglet-dumont-related__item"
				data-wp-context="<?php echo esc_attr( (string) wp_json_encode( $viglet_dumont_context ) ); ?>"
			>
				<?php
				if ( '' !== $viglet_dumont_image ) :
					// The same picture, blurred behind it where its shape leaves the frame empty.
					$viglet_dumont_backdrop = (string) get_the_post_thumbnail_url( $viglet_dumont_related, 'medium' );
					?>
					<a class="viglet-dumont-related__image" href="<?php echo esc_url( $viglet_dumont_url ); ?>" tabindex="-1" aria-hidden="true" data-wp-on--click="actions.open" data-wp-on--auxclick="actions.open" style="<?php echo esc_attr( "background-image: url('" . esc_url( $viglet_dumont_backdrop ) . "')" ); ?>">
						<?php echo wp_kses( $viglet_dumont_image, Viglet_Dumont_Related::IMAGE_HTML ); ?>
					</a>
				<?php endif; ?>
				<h3 class="viglet-dumont-related__title">
					<a href="<?php echo esc_url( $viglet_dumont_url ); ?>" data-wp-on--click="actions.open" data-wp-on--auxclick="actions.open"><?php echo esc_html( get_the_title( $viglet_dumont_related ) ); ?></a>
				</h3>
				<?php if ( ! empty( $attributes['showDate'] ) ) : ?>
					<time class="viglet-dumont-related__date" datetime="<?php echo esc_attr( get_the_date( 'c', $viglet_dumont_related ) ); ?>"><?php echo esc_html( get_the_date( '', $viglet_dumont_related ) ); ?></time>
				<?php endif; ?>
				<?php if ( ! empty( $attributes['showExcerpt'] ) ) : ?>
					<p class="viglet-dumont-related__excerpt"><?php echo esc_html( wp_trim_words( get_the_excerpt( $viglet_dumont_related ), 24 ) ); ?></p>
				<?php endif; ?>
				<?php if ( $viglet_dumont_shop && 'product' === $viglet_dumont_related->post_type ) : ?>
					<label class="viglet-dumont-related__compare" hidden data-wp-bind--hidden="!state.canCompare">
						<?php /* translators: %s: a product's name. */ ?>
						<input type="checkbox" aria-label="<?php echo esc_attr( sprintf( __( 'Compare %s', 'viglet-turing-search' ), get_the_title( $viglet_dumont_related ) ) ); ?>" data-wp-bind--checked="state.compared" data-wp-bind--disabled="state.compareFull" data-wp-on--change="actions.toggleCompare" />
						<?php esc_html_e( 'Compare', 'viglet-turing-search' ); ?>
					</label>
				<?php endif; ?>
			</li>
		<?php endforeach; ?>
	</ul>
	<?php if ( $viglet_dumont_shop ) : ?>
		<p class="viglet-dumont-related__compare-bar" hidden data-wp-bind--hidden="!state.compareReady">
			<a class="wp-element-button" data-wp-bind--href="state.compareUrl" data-wp-text="state.compareText"></a>
		</p>
	<?php endif; ?>
</section>
