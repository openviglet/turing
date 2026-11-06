<?php
/**
 * A piece of the Turing search block (D185). The results summary, and the sort when the block offers it.
 *
 * Drawn by Viglet_Turing_Search_Block::pieces(), for the block or one of its inner blocks.
 *
 * @package Viglet_Turing
 *
 * @var array    $attributes         The search block's attributes.
 * @var string   $viglet_turing_id   The id the block's pieces share.
 * @var callable $viglet_turing_show Whether a part of a result shows.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

$viglet_turing_sorts = Viglet_Turing_Search_Block::sorts();
$viglet_turing_sort  = Viglet_Turing_Search_Block::sort();
?>
<div class="viglet-turing-search__toolbar">
	<p class="viglet-turing-search__summary" aria-live="polite" data-wp-text="state.summary"></p>
	<?php if ( ! empty( $attributes['showSort'] ) ) : ?>
		<p class="viglet-turing-search__sort" hidden data-wp-bind--hidden="!state.hasResults">
			<label for="<?php echo esc_attr( $viglet_turing_id ); ?>-sort"><?php esc_html_e( 'Sort by', 'viglet-turing-search' ); ?></label>
			<select id="<?php echo esc_attr( $viglet_turing_id ); ?>-sort" data-wp-bind--value="state.sort" data-wp-on--change="actions.sortBy">
				<?php foreach ( $viglet_turing_sorts as $viglet_turing_value => $viglet_turing_label ) : ?>
					<option value="<?php echo esc_attr( $viglet_turing_value ); ?>" <?php selected( $viglet_turing_sort, $viglet_turing_value ); ?>><?php echo esc_html( $viglet_turing_label ); ?></option>
				<?php endforeach; ?>
			</select>
		</p>
	<?php endif; ?>
</div>
