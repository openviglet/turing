<?php
/**
 * A piece of the Turing search block (D185). The search box: the field, its suggestions and the submit button.
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

$viglet_dumont_query       = Viglet_Dumont_Search_Block::query();
$viglet_dumont_placeholder = '' !== (string) ( $attributes['placeholder'] ?? '' ) ? $attributes['placeholder'] : __( 'Search…', 'viglet-dumont' );
?>
<form role="search" class="viglet-dumont-search__form" data-wp-on--submit="actions.submit">
	<label class="screen-reader-text" for="<?php echo esc_attr( $viglet_dumont_id ); ?>"><?php esc_html_e( 'Search', 'viglet-dumont' ); ?></label>
	<div class="viglet-dumont-search__field">
		<input
			type="search"
			id="<?php echo esc_attr( $viglet_dumont_id ); ?>"
			name="q"
			role="combobox"
			autocomplete="off"
			aria-autocomplete="list"
			aria-expanded="false"
			aria-controls="<?php echo esc_attr( $viglet_dumont_id ); ?>-suggestions"
			placeholder="<?php echo esc_attr( $viglet_dumont_placeholder ); ?>"
			value="<?php echo esc_attr( $viglet_dumont_query ); ?>"
			data-wp-bind--value="state.query"
			data-wp-bind--aria-expanded="state.isExpanded"
			data-wp-bind--aria-activedescendant="state.activeId"
			data-wp-on--input="actions.type"
			data-wp-on--keydown="actions.keydown"
			data-wp-on--focus="actions.focus"
			data-wp-on--blur="actions.blur"
		/>
		<button type="button" class="viglet-dumont-search__clear" hidden data-wp-bind--hidden="!state.hasQuery" data-wp-on--click="actions.clear">
			<span aria-hidden="true">×</span>
			<span class="screen-reader-text"><?php esc_html_e( 'Clear the search', 'viglet-dumont' ); ?></span>
		</button>
	</div>
	<button type="submit" class="wp-element-button"><?php esc_html_e( 'Search', 'viglet-dumont' ); ?></button>
	<div class="viglet-dumont-search__popup" hidden data-wp-bind--hidden="!state.isExpanded" data-wp-on--mousedown="actions.keepFocus">
		<ul
			id="<?php echo esc_attr( $viglet_dumont_id ); ?>-suggestions"
			class="viglet-dumont-search__suggestions"
			role="listbox"
			aria-label="<?php esc_attr_e( 'Suggestions', 'viglet-dumont' ); ?>"
		>
			<template data-wp-each--option="state.options" data-wp-each-key="context.option.id">
				<li
					role="option"
					aria-selected="false"
					data-wp-bind--id="context.option.id"
					data-wp-bind--aria-selected="context.option.active"
					data-wp-class--is-active="context.option.active"
					data-wp-on--click="actions.pick"
				><strong data-wp-text="context.option.prefix"></strong><span data-wp-text="context.option.rest"></span></li>
			</template>
		</ul>
		<button type="button" class="viglet-dumont-search__link" hidden data-wp-bind--hidden="!state.showsRecent" data-wp-on--click="actions.clearRecent"><?php esc_html_e( 'Clear recent searches', 'viglet-dumont' ); ?></button>
	</div>
	<p class="screen-reader-text" aria-live="polite" data-wp-text="state.optionsAnnouncement"></p>
</form>
