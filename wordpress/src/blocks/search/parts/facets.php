<?php
/**
 * A piece of the Turing search block (D185). The facets, which a narrow screen shows as a drawer.
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
	<aside
		id="<?php echo esc_attr( $viglet_dumont_id ); ?>-facets"
		class="viglet-dumont-search__facets"
		aria-label="<?php esc_attr_e( 'Filters', 'viglet-dumont' ); ?>"
		hidden
		data-wp-bind--hidden="!state.hasFacets"
		data-wp-class--is-open="state.drawerOpen"
		data-wp-on--keydown="actions.drawerKeydown"
	>
		<template data-wp-each--facet="state.facets" data-wp-each-key="context.facet.name">
			<details class="viglet-dumont-search__facet" open>
				<summary>
					<span data-wp-text="context.facet.label"></span>
					<span class="viglet-dumont-search__badge" hidden data-wp-bind--hidden="!context.facet.selectedCount" data-wp-text="context.facet.selectedCount"></span>
				</summary>
				<fieldset>
					<legend class="screen-reader-text" data-wp-text="context.facet.label"></legend>
					<template data-wp-each--item="context.facet.items" data-wp-each-key="context.item.target.filterQuery">
						<label data-wp-bind--hidden="state.itemHidden">
							<input type="checkbox" data-wp-bind--checked="context.item.selected" data-wp-on--change="actions.toggleFacet" />
							<span data-wp-text="context.item.label"></span>
							<span class="viglet-dumont-search__count" data-wp-text="context.item.count"></span>
						</label>
					</template>
					<button type="button" class="viglet-dumont-search__link" hidden data-wp-bind--hidden="!context.facet.hasMore" data-wp-bind--aria-expanded="state.facetExpanded" data-wp-on--click="actions.toggleMore" data-wp-text="state.moreLabel"></button>
				</fieldset>
			</details>
		</template>
		<div class="viglet-dumont-search__drawer-actions">
			<button type="button" class="wp-element-button" data-wp-on--click="actions.closeDrawer"><?php esc_html_e( 'Show results', 'viglet-dumont' ); ?></button>
			<button type="button" class="viglet-dumont-search__link" data-wp-on--click="actions.clearFilters"><?php esc_html_e( 'Clear all filters', 'viglet-dumont' ); ?></button>
		</div>
	</aside>
<?php endif; ?>
