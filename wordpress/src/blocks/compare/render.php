<?php
/**
 * The compare block (D200): the products named by `?compare=` side by side, drawn on the
 * server. Without products to compare it says how to choose them.
 *
 * @package Viglet_Dumont
 *
 * @var array $attributes The block's attributes.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}
// Products are WooCommerce's: without it there is nothing to compare.
if ( ! Viglet_Dumont_WooCommerce::active() ) {
	return;
}

// phpcs:ignore WordPress.Security.NonceVerification.Recommended -- a read-only view of public products.
$viglet_dumont_ids     = Viglet_Dumont_Compare::ids( sanitize_text_field( wp_unslash( (string) ( $_GET[ Viglet_Dumont_Compare::QUERY ] ?? '' ) ) ) );
$viglet_dumont_heading = '' !== (string) ( $attributes['heading'] ?? '' ) ? $attributes['heading'] : __( 'Compare products', 'viglet-dumont' );
$viglet_dumont_table   = count( $viglet_dumont_ids ) >= 2 ? Viglet_Dumont_Compare::table( $viglet_dumont_ids ) : null;
?>
<section <?php echo get_block_wrapper_attributes( array( 'class' => 'viglet-dumont-compare' ) ); // phpcs:ignore WordPress.Security.EscapeOutput.OutputNotEscaped -- escaped by WordPress. ?>>
	<h2 class="viglet-dumont-compare__heading"><?php echo esc_html( $viglet_dumont_heading ); ?></h2>
	<?php if ( null === $viglet_dumont_table ) : ?>
		<p class="viglet-dumont-compare__empty"><?php esc_html_e( 'Tick Compare on two to four products in the search results to see them side by side here.', 'viglet-dumont' ); ?></p>
	<?php else : ?>
		<?php
		// Asked for once the page is drawn (public/compare.js): the table never waits on a model.
		?>
		<div class="viglet-dumont-compare__summary" hidden aria-live="polite" data-summary-url="<?php echo esc_url( add_query_arg( 'ids', implode( ',', $viglet_dumont_ids ), rest_url( 'viglet-dumont/v1/compare/summary' ) ) ); ?>">
			<p class="viglet-dumont-compare__ai"><?php esc_html_e( 'Summary written by AI from the table below', 'viglet-dumont' ); ?></p>
			<p class="viglet-dumont-compare__summary-text"></p>
		</div>
		<div class="viglet-dumont-compare__scroll" role="region" tabindex="0" aria-label="<?php echo esc_attr( $viglet_dumont_heading ); ?>">
			<table class="viglet-dumont-compare__table">
				<thead>
					<tr>
						<td></td>
						<?php foreach ( $viglet_dumont_table['products'] as $viglet_dumont_product ) : ?>
							<th scope="col">
								<?php if ( '' !== $viglet_dumont_product['image'] ) : ?>
									<img src="<?php echo esc_url( $viglet_dumont_product['image'] ); ?>" alt="" loading="lazy" width="120" height="120" />
								<?php endif; ?>
								<a href="<?php echo esc_url( $viglet_dumont_product['url'] ); ?>"><?php echo esc_html( $viglet_dumont_product['name'] ); ?></a>
							</th>
						<?php endforeach; ?>
					</tr>
				</thead>
				<tbody>
					<?php foreach ( $viglet_dumont_table['rows'] as $viglet_dumont_row ) : ?>
						<tr class="<?php echo $viglet_dumont_row['differs'] ? 'viglet-dumont-compare__differs' : ''; ?>">
							<th scope="row">
								<?php echo esc_html( $viglet_dumont_row['label'] ); ?>
								<?php if ( $viglet_dumont_row['differs'] ) : ?>
									<span class="screen-reader-text"><?php esc_html_e( '(differs)', 'viglet-dumont' ); ?></span>
								<?php endif; ?>
							</th>
							<?php foreach ( $viglet_dumont_row['values'] as $viglet_dumont_value ) : ?>
								<td><?php echo esc_html( $viglet_dumont_value ); ?></td>
							<?php endforeach; ?>
						</tr>
					<?php endforeach; ?>
				</tbody>
			</table>
		</div>
		<p class="viglet-dumont-compare__legend"><span class="viglet-dumont-compare__swatch" aria-hidden="true"></span> <?php esc_html_e( 'Where they differ', 'viglet-dumont' ); ?></p>
	<?php endif; ?>
</section>
