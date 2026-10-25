<?php
/**
 * A piece of the Turing search block (D185). The filters applied, each removable.
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
<ul class="viglet-dumont-search__applied" hidden data-wp-bind--hidden="!state.hasApplied" aria-label="<?php esc_attr_e( 'Active filters', 'viglet-dumont' ); ?>">
	<template data-wp-each--filter="state.applied">
		<li>
			<button type="button" class="viglet-dumont-search__chip" data-wp-on--click="actions.removeFilter">
				<span data-wp-text="context.filter.label"></span>
				<span aria-hidden="true">×</span>
				<span class="screen-reader-text"><?php esc_html_e( 'Remove this filter', 'viglet-dumont' ); ?></span>
			</button>
		</li>
	</template>
	<li>
		<button type="button" class="viglet-dumont-search__link" data-wp-on--click="actions.clearFilters"><?php esc_html_e( 'Clear all filters', 'viglet-dumont' ); ?></button>
	</li>
</ul>
