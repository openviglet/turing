<?php
/**
 * A piece of the Turing search block (D185). The results summary, and the sort when the block offers it.
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

$viglet_dumont_sorts = Viglet_Dumont_Search_Block::sorts();
$viglet_dumont_sort  = Viglet_Dumont_Search_Block::sort();
?>
<div class="viglet-dumont-search__toolbar">
	<p class="viglet-dumont-search__summary" aria-live="polite" data-wp-text="state.summary"></p>
	<?php if ( ! empty( $attributes['showSort'] ) ) : ?>
		<p class="viglet-dumont-search__sort" hidden data-wp-bind--hidden="!state.hasResults">
			<label for="<?php echo esc_attr( $viglet_dumont_id ); ?>-sort"><?php esc_html_e( 'Sort by', 'viglet-dumont' ); ?></label>
			<select id="<?php echo esc_attr( $viglet_dumont_id ); ?>-sort" data-wp-bind--value="state.sort" data-wp-on--change="actions.sortBy">
				<?php foreach ( $viglet_dumont_sorts as $viglet_dumont_value => $viglet_dumont_label ) : ?>
					<option value="<?php echo esc_attr( $viglet_dumont_value ); ?>" <?php selected( $viglet_dumont_sort, $viglet_dumont_value ); ?>><?php echo esc_html( $viglet_dumont_label ); ?></option>
				<?php endforeach; ?>
			</select>
		</p>
	<?php endif; ?>
</div>
