<?php
/**
 * A piece of the Turing search block (D185). The filters a sentence names (D170): Turing's
 * reading of the query, offered as chips that apply with one click and never on their own.
 * With applyFilters on (D199) they are applied instead, and a line says what was read, with
 * one click to undo it.
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
<?php if ( ! empty( $attributes['suggestFilters'] ) ) : ?>
	<div class="viglet-dumont-search__suggested" hidden data-wp-bind--hidden="!state.hasSuggested">
		<span class="viglet-dumont-search__suggested-label" id="<?php echo esc_attr( $viglet_dumont_id ); ?>-suggested"><?php esc_html_e( 'Filter by what you asked for:', 'viglet-dumont' ); ?></span>
		<ul aria-labelledby="<?php echo esc_attr( $viglet_dumont_id ); ?>-suggested">
			<template data-wp-each--suggestion="state.suggested" data-wp-each-key="context.suggestion.fq">
				<li>
					<button type="button" class="viglet-dumont-search__chip viglet-dumont-search__chip--suggested" data-wp-on--click="actions.applySuggestion">
						<span aria-hidden="true">+</span>
						<span data-wp-text="context.suggestion.label"></span>
						<span class="screen-reader-text"><?php esc_html_e( 'Apply this filter', 'viglet-dumont' ); ?></span>
					</button>
				</li>
			</template>
		</ul>
	</div>
	<?php if ( ! empty( $attributes['applyFilters'] ) ) : ?>
		<p class="viglet-dumont-search__understood" role="status" hidden data-wp-bind--hidden="!state.understood">
			<span data-wp-text="state.understood"></span>
			<button type="button" class="viglet-dumont-search__undo" data-wp-on--click="actions.undoUnderstood"><?php esc_html_e( 'Undo', 'viglet-dumont' ); ?></button>
		</p>
	<?php endif; ?>
<?php endif; ?>
