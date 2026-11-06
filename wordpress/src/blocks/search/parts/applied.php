<?php
/**
 * A piece of the Turing search block (D185). The filters applied, each removable.
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
?>
<ul class="viglet-turing-search__applied" hidden data-wp-bind--hidden="!state.hasApplied" aria-label="<?php esc_attr_e( 'Active filters', 'viglet-turing-search' ); ?>">
	<template data-wp-each--filter="state.applied">
		<li>
			<button type="button" class="viglet-turing-search__chip" data-wp-on--click="actions.removeFilter">
				<span data-wp-text="context.filter.label"></span>
				<span aria-hidden="true">×</span>
				<span class="screen-reader-text"><?php esc_html_e( 'Remove this filter', 'viglet-turing-search' ); ?></span>
			</button>
		</li>
	</template>
	<li>
		<button type="button" class="viglet-turing-search__link" data-wp-on--click="actions.clearFilters"><?php esc_html_e( 'Clear all filters', 'viglet-turing-search' ); ?></button>
	</li>
</ul>
