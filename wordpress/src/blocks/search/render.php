<?php
/**
 * The Turing search block, server side: the markup, with Interactivity API directives that
 * src/public/search-block.js drives through the Turing JS SDK.
 *
 * The query starts from ?q= (or ?s=), so a results URL can be shared and the block also
 * works as a site's search page. Everything the visitor sees comes from Turing, filtered to
 * this site's key.
 *
 * @package Viglet_Dumont
 *
 * @var array $attributes The block's attributes.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

wp_enqueue_script_module( 'viglet-dumont/search-block' );

// A public, read-only page parameter: nothing is changed, so there is no nonce to check.
// phpcs:ignore WordPress.Security.NonceVerification.Recommended
$viglet_dumont_query = sanitize_text_field( wp_unslash( $_GET['q'] ?? ( $_GET['s'] ?? '' ) ) );
$viglet_dumont_id    = wp_unique_id( 'viglet-dumont-search-' );

wp_interactivity_state(
	'viglet-dumont/search',
	array(
		'query'       => $viglet_dumont_query,
		'status'      => 'idle',
		'results'     => array(),
		'facets'      => array(),
		'pages'       => array(),
		'suggestions' => array(),
		'applied'     => array(),
		'total'       => 0,
		'corrected'   => '',
		'error'       => '',
		/* translators: %d: how many results Turing found. */
		'foundLabel'  => __( '%d results', 'viglet-dumont' ),
		'noneLabel'   => __( 'No results found. Try different keywords.', 'viglet-dumont' ),
		'errorLabel'  => __( 'Search is unavailable right now.', 'viglet-dumont' ),
	)
);

$viglet_dumont_placeholder = '' !== (string) ( $attributes['placeholder'] ?? '' ) ? $attributes['placeholder'] : __( 'Search…', 'viglet-dumont' );
?>
<div
	<?php echo get_block_wrapper_attributes( array( 'class' => 'viglet-dumont-search' ) ); // phpcs:ignore WordPress.Security.EscapeOutput.OutputNotEscaped -- core escapes the wrapper attributes. ?>
	data-wp-interactive="viglet-dumont/search"
	data-wp-init="callbacks.init"
>
	<form role="search" class="viglet-dumont-search__form" data-wp-on--submit="actions.submit">
		<label class="screen-reader-text" for="<?php echo esc_attr( $viglet_dumont_id ); ?>"><?php esc_html_e( 'Search', 'viglet-dumont' ); ?></label>
		<input
			type="search"
			id="<?php echo esc_attr( $viglet_dumont_id ); ?>"
			name="q"
			autocomplete="off"
			aria-autocomplete="list"
			aria-controls="<?php echo esc_attr( $viglet_dumont_id ); ?>-suggestions"
			placeholder="<?php echo esc_attr( $viglet_dumont_placeholder ); ?>"
			value="<?php echo esc_attr( $viglet_dumont_query ); ?>"
			data-wp-bind--value="state.query"
			data-wp-on--input="actions.type"
			data-wp-on--keydown="actions.keydown"
		/>
		<button type="submit" class="wp-element-button"><?php esc_html_e( 'Search', 'viglet-dumont' ); ?></button>
		<ul
			id="<?php echo esc_attr( $viglet_dumont_id ); ?>-suggestions"
			class="viglet-dumont-search__suggestions"
			role="listbox"
			hidden
			data-wp-bind--hidden="!state.hasSuggestions"
		>
			<template data-wp-each--suggestion="state.suggestions">
				<li role="option">
					<button type="button" data-wp-on--click="actions.pick" data-wp-text="context.suggestion"></button>
				</li>
			</template>
		</ul>
	</form>

	<p class="viglet-dumont-search__correction" hidden data-wp-bind--hidden="!state.hasCorrection">
		<?php esc_html_e( 'Did you mean:', 'viglet-dumont' ); ?>
		<button type="button" class="viglet-dumont-search__link" data-wp-on--click="actions.applyCorrection" data-wp-text="state.corrected"></button>
	</p>

	<p class="viglet-dumont-search__summary" aria-live="polite" data-wp-text="state.summary"></p>

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

	<div class="viglet-dumont-search__layout">
		<?php if ( ! empty( $attributes['showFacets'] ) ) : ?>
			<aside class="viglet-dumont-search__facets" hidden data-wp-bind--hidden="!state.hasFacets" aria-label="<?php esc_attr_e( 'Filters', 'viglet-dumont' ); ?>">
				<template data-wp-each--facet="state.facets">
					<fieldset>
						<legend data-wp-text="context.facet.label"></legend>
						<template data-wp-each--item="context.facet.items">
							<label>
								<input type="checkbox" data-wp-bind--checked="context.item.selected" data-wp-on--change="actions.toggleFacet" />
								<span data-wp-text="context.item.label"></span>
								<span class="viglet-dumont-search__count" data-wp-text="context.item.count"></span>
							</label>
						</template>
					</fieldset>
				</template>
			</aside>
		<?php endif; ?>

		<ol class="viglet-dumont-search__results">
			<template data-wp-each--result="state.results">
				<li>
					<a data-wp-bind--href="context.result.url" data-wp-text="context.result.title"></a>
					<p data-wp-text="context.result.description"></p>
				</li>
			</template>
		</ol>
	</div>

	<nav class="viglet-dumont-search__pages" hidden data-wp-bind--hidden="!state.hasPages" aria-label="<?php esc_attr_e( 'Result pages', 'viglet-dumont' ); ?>">
		<template data-wp-each--page="state.pages">
			<button type="button" data-wp-on--click="actions.goToPage" data-wp-bind--aria-current="context.page.current" data-wp-text="context.page.text"></button>
		</template>
	</nav>
</div>
