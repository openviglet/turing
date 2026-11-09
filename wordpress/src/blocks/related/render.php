<?php
/**
 * The Related content block (D177): the posts Turing finds most like the one being read,
 * drawn on the server so the links are in the HTML for crawlers and visitors without
 * JavaScript. related.js only reports which one was opened (D176).
 *
 * @package Viglet_Turing
 *
 * @var array    $attributes The block's attributes.
 * @var WP_Block $block      The block, with the post it sits in as context.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

$viglet_turing_post_id = (int) ( $block->context['postId'] ?? get_the_ID() );
$viglet_turing_preview = Viglet_Turing_Native_Search::is_editor_preview();
$viglet_turing_posts   = $viglet_turing_post_id > 0
	? ( new Viglet_Turing_Related() )->related(
		$viglet_turing_post_id,
		max( 1, min( 12, (int) ( $attributes['count'] ?? 3 ) ) ),
		! empty( $attributes['sameType'] ),
		(string) ( $attributes['mode'] ?? '' ),
		// The sites this one also searches (D303), when the block asks for them.
		'all' === ( $attributes['sites'] ?? 'own' )
	)
	: array();

// A post with no neighbours gets nothing, not an empty box; the editor says why instead.
if ( ! $viglet_turing_posts ) {
	if ( $viglet_turing_preview ) {
		printf(
			'<p %s>%s</p>',
			wp_kses_data( get_block_wrapper_attributes( array( 'class' => 'viglet-turing-related__empty' ) ) ),
			esc_html__( 'Related posts appear here once this post is indexed and Viglet Turing finds posts like it.', 'viglet-turing-search' )
		);
	}
	return;
}

$viglet_turing_heading = '' !== trim( (string) ( $attributes['heading'] ?? '' ) )
	? (string) $attributes['heading']
	: __( 'Related posts', 'viglet-turing-search' );
$viglet_turing_id      = wp_unique_id( 'viglet-turing-related-' );
$viglet_turing_doc_ids = new Viglet_Turing_Indexer();
// A product among them offers Compare, ticked into the list the search cards use (D212).
$viglet_turing_shop = Viglet_Turing_WooCommerce::active()
	&& array_filter( $viglet_turing_posts, static fn ( $post ) => $post instanceof WP_Post && 'product' === $post->post_type );
if ( $viglet_turing_shop ) {
	wp_interactivity_state(
		'viglet-turing/related',
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
				'class'               => 'viglet-turing-related--' . ( 'list' === ( $attributes['layout'] ?? 'grid' ) ? 'list' : 'grid' ),
				'aria-labelledby'     => $viglet_turing_id,
				'data-wp-interactive' => 'viglet-turing/related',
			)
		)
	);
	?>
>
	<h2 class="viglet-turing-related__heading" id="<?php echo esc_attr( $viglet_turing_id ); ?>"><?php echo esc_html( $viglet_turing_heading ); ?></h2>
	<ul class="viglet-turing-related__list">
		<?php foreach ( $viglet_turing_posts as $viglet_turing_index => $viglet_turing_related ) : ?>
			<?php
			// A sibling site's document (D303): its title, linked to its own site, and where it is from.
			if ( is_array( $viglet_turing_related ) ) :
				?>
				<li
					class="viglet-turing-related__item viglet-turing-related__item--remote"
					data-wp-context="
					<?php
					echo esc_attr(
						(string) wp_json_encode(
							array(
								'id'        => $viglet_turing_related['id'],
								'position'  => $viglet_turing_index + 1,
								'productId' => 0,
							)
						)
					);
					?>
										"
				>
					<p class="viglet-turing-related__origin"><?php echo esc_html( $viglet_turing_related['origin'] ); ?></p>
					<h3 class="viglet-turing-related__title">
						<a href="<?php echo esc_url( $viglet_turing_related['url'] ); ?>" data-wp-on--click="actions.open" data-wp-on--auxclick="actions.open"><?php echo esc_html( '' !== $viglet_turing_related['title'] ? $viglet_turing_related['title'] : $viglet_turing_related['url'] ); ?></a>
					</h3>
				</li>
				<?php
				continue;
			endif;
			$viglet_turing_url     = get_permalink( $viglet_turing_related );
			$viglet_turing_image   = ! empty( $attributes['showImage'] ) && has_post_thumbnail( $viglet_turing_related )
				? get_the_post_thumbnail(
					$viglet_turing_related,
					'medium_large',
					array(
						'alt'     => '',
						'loading' => 'lazy',
					)
				)
				: '';
			$viglet_turing_context = array(
				'id'        => $viglet_turing_doc_ids->build_doc_id( $viglet_turing_related->ID ),
				'position'  => $viglet_turing_index + 1,
				'productId' => $viglet_turing_related->ID,
			);
			?>
			<li
				class="viglet-turing-related__item"
				data-wp-context="<?php echo esc_attr( (string) wp_json_encode( $viglet_turing_context ) ); ?>"
			>
				<?php
				if ( '' !== $viglet_turing_image ) :
					// The same picture, blurred behind it where its shape leaves the frame empty.
					$viglet_turing_backdrop = (string) get_the_post_thumbnail_url( $viglet_turing_related, 'medium' );
					?>
					<a class="viglet-turing-related__image" href="<?php echo esc_url( $viglet_turing_url ); ?>" tabindex="-1" aria-hidden="true" data-wp-on--click="actions.open" data-wp-on--auxclick="actions.open" style="<?php echo esc_attr( "background-image: url('" . esc_url( $viglet_turing_backdrop ) . "')" ); ?>">
						<?php echo wp_kses( $viglet_turing_image, Viglet_Turing_Related::IMAGE_HTML ); ?>
					</a>
				<?php endif; ?>
				<h3 class="viglet-turing-related__title">
					<a href="<?php echo esc_url( $viglet_turing_url ); ?>" data-wp-on--click="actions.open" data-wp-on--auxclick="actions.open"><?php echo esc_html( get_the_title( $viglet_turing_related ) ); ?></a>
				</h3>
				<?php if ( ! empty( $attributes['showDate'] ) ) : ?>
					<time class="viglet-turing-related__date" datetime="<?php echo esc_attr( get_the_date( 'c', $viglet_turing_related ) ); ?>"><?php echo esc_html( get_the_date( '', $viglet_turing_related ) ); ?></time>
				<?php endif; ?>
				<?php if ( ! empty( $attributes['showExcerpt'] ) ) : ?>
					<p class="viglet-turing-related__excerpt"><?php echo esc_html( wp_trim_words( get_the_excerpt( $viglet_turing_related ), 24 ) ); ?></p>
				<?php endif; ?>
				<?php if ( $viglet_turing_shop && 'product' === $viglet_turing_related->post_type ) : ?>
					<label class="viglet-turing-related__compare" hidden data-wp-bind--hidden="!state.canCompare">
						<?php /* translators: %s: a product's name. */ ?>
						<input type="checkbox" aria-label="<?php echo esc_attr( sprintf( __( 'Compare %s', 'viglet-turing-search' ), get_the_title( $viglet_turing_related ) ) ); ?>" data-wp-bind--checked="state.compared" data-wp-bind--disabled="state.compareFull" data-wp-on--change="actions.toggleCompare" />
						<?php esc_html_e( 'Compare', 'viglet-turing-search' ); ?>
					</label>
				<?php endif; ?>
			</li>
		<?php endforeach; ?>
	</ul>
	<?php if ( $viglet_turing_shop ) : ?>
		<p class="viglet-turing-related__compare-bar" hidden data-wp-bind--hidden="!state.compareReady">
			<a class="wp-element-button" data-wp-bind--href="state.compareUrl" data-wp-text="state.compareText"></a>
		</p>
	<?php endif; ?>
</section>
