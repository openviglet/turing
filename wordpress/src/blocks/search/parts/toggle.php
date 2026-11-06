<?php
/**
 * A piece of the Turing search block (D185). The button that opens the filters on a narrow screen.
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
?>
<?php if ( ! empty( $attributes['showFacets'] ) ) : ?>
	<button
		type="button"
		class="viglet-dumont-search__filters-toggle wp-element-button"
		aria-controls="<?php echo esc_attr( $viglet_dumont_id ); ?>-facets"
		aria-expanded="false"
		hidden
		data-wp-bind--hidden="!state.hasFacets"
		data-wp-bind--aria-expanded="state.drawerOpen"
		data-wp-on--click="actions.openDrawer"
		data-wp-text="state.filtersLabel"
	></button>
<?php endif; ?>
