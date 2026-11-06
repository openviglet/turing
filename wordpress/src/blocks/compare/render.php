<?php
/**
 * The compare block (D200): the products named by `?compare=` side by side, drawn on the
 * server. Without products to compare it says how to choose them.
 *
 * @package Viglet_Turing
 *
 * @var array $attributes The block's attributes.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}
// Products are WooCommerce's: without it there is nothing to compare.
if ( ! Viglet_Turing_WooCommerce::active() ) {
	return;
}

// phpcs:ignore WordPress.Security.NonceVerification.Recommended -- a read-only view of public products.
$viglet_turing_ids     = Viglet_Turing_Compare::ids( sanitize_text_field( wp_unslash( (string) ( $_GET[ Viglet_Turing_Compare::QUERY ] ?? '' ) ) ) );
$viglet_turing_heading = '' !== (string) ( $attributes['heading'] ?? '' ) ? $attributes['heading'] : __( 'Compare products', 'viglet-turing-search' );
$viglet_turing_table   = count( $viglet_turing_ids ) >= 2 ? Viglet_Turing_Compare::table( $viglet_turing_ids ) : null;
?>
<section <?php echo wp_kses_data( get_block_wrapper_attributes( array( 'class' => 'viglet-turing-compare' ) ) ); ?>>
	<h2 class="viglet-turing-compare__heading"><?php echo esc_html( $viglet_turing_heading ); ?></h2>
	<?php if ( null === $viglet_turing_table ) : ?>
		<p class="viglet-turing-compare__empty"><?php esc_html_e( 'Tick Compare on two to four products in the search results to see them side by side here.', 'viglet-turing-search' ); ?></p>
	<?php else : ?>
		<?php
		// Asked for once the page is drawn (public/compare.js): the table never waits on a model.
		?>
		<div class="viglet-turing-compare__summary" hidden aria-live="polite" data-summary-url="<?php echo esc_url( add_query_arg( 'ids', implode( ',', $viglet_turing_ids ), rest_url( 'viglet-turing/v1/compare/summary' ) ) ); ?>">
			<p class="viglet-turing-compare__ai"><?php esc_html_e( 'Summary written by AI from the table below', 'viglet-turing-search' ); ?></p>
			<p class="viglet-turing-compare__summary-text"></p>
		</div>
		<div class="viglet-turing-compare__scroll" role="region" tabindex="0" aria-label="<?php echo esc_attr( $viglet_turing_heading ); ?>">
			<table class="viglet-turing-compare__table">
				<thead>
					<tr>
						<td></td>
						<?php foreach ( $viglet_turing_table['products'] as $viglet_turing_product ) : ?>
							<th scope="col">
								<?php if ( '' !== $viglet_turing_product['image'] ) : ?>
									<img src="<?php echo esc_url( $viglet_turing_product['image'] ); ?>" alt="" loading="lazy" width="120" height="120" />
								<?php endif; ?>
								<a href="<?php echo esc_url( $viglet_turing_product['url'] ); ?>"><?php echo esc_html( $viglet_turing_product['name'] ); ?></a>
							</th>
						<?php endforeach; ?>
					</tr>
				</thead>
				<tbody>
					<?php foreach ( $viglet_turing_table['rows'] as $viglet_turing_row ) : ?>
						<tr class="<?php echo $viglet_turing_row['differs'] ? 'viglet-turing-compare__differs' : ''; ?>">
							<th scope="row">
								<?php echo esc_html( $viglet_turing_row['label'] ); ?>
								<?php if ( $viglet_turing_row['differs'] ) : ?>
									<span class="screen-reader-text"><?php esc_html_e( '(differs)', 'viglet-turing-search' ); ?></span>
								<?php endif; ?>
							</th>
							<?php foreach ( $viglet_turing_row['values'] as $viglet_turing_value ) : ?>
								<td><?php echo esc_html( $viglet_turing_value ); ?></td>
							<?php endforeach; ?>
						</tr>
					<?php endforeach; ?>
				</tbody>
			</table>
		</div>
		<p class="viglet-turing-compare__legend"><span class="viglet-turing-compare__swatch" aria-hidden="true"></span> <?php esc_html_e( 'Where they differ', 'viglet-turing-search' ); ?></p>
	<?php endif; ?>
</section>
