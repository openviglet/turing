<?php
/**
 * A piece of the Turing search block (D185). The search box: the field, its suggestions and the submit button.
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

$viglet_turing_query       = Viglet_Turing_Search_Block::query();
$viglet_turing_placeholder = '' !== (string) ( $attributes['placeholder'] ?? '' ) ? $attributes['placeholder'] : __( 'Search…', 'viglet-turing-search' );
?>
<form role="search" class="viglet-turing-search__form" data-wp-on--submit="actions.submit">
	<label class="screen-reader-text" for="<?php echo esc_attr( $viglet_turing_id ); ?>"><?php esc_html_e( 'Search', 'viglet-turing-search' ); ?></label>
	<div class="viglet-turing-search__field">
		<input
			type="search"
			id="<?php echo esc_attr( $viglet_turing_id ); ?>"
			name="q"
			role="combobox"
			autocomplete="off"
			aria-autocomplete="list"
			aria-expanded="false"
			aria-controls="<?php echo esc_attr( $viglet_turing_id ); ?>-suggestions"
			placeholder="<?php echo esc_attr( $viglet_turing_placeholder ); ?>"
			value="<?php echo esc_attr( $viglet_turing_query ); ?>"
			data-wp-bind--value="state.query"
			data-wp-bind--aria-expanded="state.isExpanded"
			data-wp-bind--aria-activedescendant="state.activeId"
			data-wp-on--input="actions.type"
			data-wp-on--keydown="actions.keydown"
			data-wp-on--focus="actions.focus"
			data-wp-on--blur="actions.blur"
		/>
		<button type="button" class="viglet-turing-search__clear" hidden data-wp-bind--hidden="!state.hasQuery" data-wp-on--click="actions.clear">
			<span aria-hidden="true">×</span>
			<span class="screen-reader-text"><?php esc_html_e( 'Clear the search', 'viglet-turing-search' ); ?></span>
		</button>
	</div>
	<button type="submit" class="wp-element-button"><?php esc_html_e( 'Search', 'viglet-turing-search' ); ?></button>
	<div class="viglet-turing-search__popup" hidden data-wp-bind--hidden="!state.isExpanded" data-wp-on--mousedown="actions.keepFocus">
		<ul
			id="<?php echo esc_attr( $viglet_turing_id ); ?>-suggestions"
			class="viglet-turing-search__suggestions"
			role="listbox"
			aria-label="<?php esc_attr_e( 'Suggestions', 'viglet-turing-search' ); ?>"
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
		<button type="button" class="viglet-turing-search__link" hidden data-wp-bind--hidden="!state.showsRecent" data-wp-on--click="actions.clearRecent"><?php esc_html_e( 'Clear recent searches', 'viglet-turing-search' ); ?></button>
	</div>
	<p class="screen-reader-text" aria-live="polite" data-wp-text="state.optionsAnnouncement"></p>
</form>
<?php
// The block's scope (D306), said as the context of the search rather than a filter to remove.
$viglet_turing_scope = Viglet_Turing_Search_Block::scope( $attributes );
if ( array() !== $viglet_turing_scope ) :
	$viglet_turing_whole = Viglet_Turing_Search_Block::whole_site_url( $attributes );
	?>
	<p class="viglet-turing-search__scope">
		<?php
		/* translators: %s: what the search is limited to, e.g. "Events". */
		echo esc_html( sprintf( __( 'In %s', 'viglet-turing-search' ), Viglet_Turing_Search_Block::scope_label( $viglet_turing_scope ) ) );
		?>
		<?php if ( '' !== $viglet_turing_whole ) : ?>
			<a href="<?php echo esc_url( $viglet_turing_whole ); ?>" data-wp-bind--href="state.wholeSiteHref"><?php esc_html_e( 'Search the whole site', 'viglet-turing-search' ); ?></a>
		<?php endif; ?>
	</p>
<?php endif; ?>
