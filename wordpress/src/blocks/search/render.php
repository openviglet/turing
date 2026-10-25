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

// A result's badge reads the post type's own name, in the site's language.
$viglet_dumont_types = array();
foreach ( viglet_dumont_get_indexable_types() as $viglet_dumont_type ) {
	$viglet_dumont_object = get_post_type_object( $viglet_dumont_type );
	if ( $viglet_dumont_object ) {
		$viglet_dumont_types[ $viglet_dumont_type ] = $viglet_dumont_object->labels->singular_name;
	}
}

// Where an empty search can send the visitor: the latest posts, from WordPress itself.
$viglet_dumont_recent = array_map(
	static fn ( WP_Post $post ) => array(
		'title' => get_the_title( $post ),
		'url'   => get_permalink( $post ),
	),
	get_posts(
		array(
			'numberposts' => 3,
			'post_status' => 'publish',
		)
	)
);

wp_interactivity_state(
	'viglet-dumont/search',
	array(
		'query'        => $viglet_dumont_query,
		'status'       => 'idle',
		'results'      => array(),
		'facets'       => array(),
		'pages'        => array(),
		'suggestions'  => array(),
		'applied'      => array(),
		'total'        => 0,
		'corrected'    => '',
		'error'        => '',
		'open'         => false,
		'active'       => -1,
		'recent'       => array(),
		'inputId'      => $viglet_dumont_id,
		'listId'       => $viglet_dumont_id . '-suggestions',
		/* translators: %d: how many suggestions the search box offers. */
		'optionsLabel' => __( '%d suggestions. Use the up and down arrows to choose one.', 'viglet-dumont' ),
		'typeLabels'   => $viglet_dumont_types,
		'homeUrl'      => home_url( '/' ),
		/* translators: %d: how many results Turing found. */
		'foundLabel'   => __( '%d results', 'viglet-dumont' ),
		'noneLabel'    => __( 'No results found. Try different keywords.', 'viglet-dumont' ),
		'errorLabel'   => __( 'Search is unavailable right now.', 'viglet-dumont' ),
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

		<div class="viglet-dumont-search__main">
			<ol class="viglet-dumont-search__results" data-wp-bind--aria-busy="state.isLoading" data-wp-class--is-loading="state.isLoading">
				<template data-wp-each--result="state.results">
					<li class="viglet-dumont-search__result">
						<img class="viglet-dumont-search__image" alt="" loading="lazy" hidden data-wp-bind--hidden="!context.result.image" data-wp-bind--src="context.result.image" />
						<div class="viglet-dumont-search__body">
							<p class="viglet-dumont-search__meta">
								<span class="viglet-dumont-search__type" data-wp-text="context.result.type"></span>
								<time data-wp-bind--hidden="!context.result.date" data-wp-bind--datetime="context.result.iso" data-wp-text="context.result.date"></time>
							</p>
							<h2 class="viglet-dumont-search__title"><a data-wp-bind--href="context.result.url" data-wp-text="context.result.title"></a></h2>
							<p class="viglet-dumont-search__crumb" data-wp-text="context.result.crumb"></p>
							<p class="viglet-dumont-search__snippet" data-wp-watch="callbacks.snippet"></p>
						</div>
					</li>
				</template>
			</ol>

			<ol class="viglet-dumont-search__skeleton" aria-hidden="true" hidden data-wp-bind--hidden="!state.showSkeleton">
				<li></li>
				<li></li>
				<li></li>
			</ol>

			<div class="viglet-dumont-search__empty" hidden data-wp-bind--hidden="!state.isEmpty">
				<?php if ( $viglet_dumont_recent ) : ?>
					<p><?php esc_html_e( 'Or start from the latest posts:', 'viglet-dumont' ); ?></p>
					<ul>
						<?php foreach ( $viglet_dumont_recent as $viglet_dumont_post ) : ?>
							<li><a href="<?php echo esc_url( $viglet_dumont_post['url'] ); ?>"><?php echo esc_html( $viglet_dumont_post['title'] ); ?></a></li>
						<?php endforeach; ?>
					</ul>
				<?php endif; ?>
			</div>

			<div class="viglet-dumont-search__error" hidden data-wp-bind--hidden="!state.isError">
				<button type="button" class="wp-element-button" data-wp-on--click="actions.retry"><?php esc_html_e( 'Try again', 'viglet-dumont' ); ?></button>
				<a data-wp-bind--href="state.wordpressSearchUrl"><?php esc_html_e( 'Search with WordPress instead', 'viglet-dumont' ); ?></a>
			</div>
		</div>
	</div>

	<nav class="viglet-dumont-search__pages" hidden data-wp-bind--hidden="!state.hasPages" aria-label="<?php esc_attr_e( 'Result pages', 'viglet-dumont' ); ?>">
		<template data-wp-each--page="state.pages">
			<button type="button" data-wp-on--click="actions.goToPage" data-wp-bind--aria-current="context.page.current" data-wp-text="context.page.text"></button>
		</template>
	</nav>
</div>
