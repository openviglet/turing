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

// The orders Turing offers; the URL's `sort` picks one (search-block.js reads the rest).
$viglet_dumont_sorts = array(
	'relevance' => __( 'Relevance', 'viglet-dumont' ),
	'newest'    => __( 'Newest', 'viglet-dumont' ),
	'oldest'    => __( 'Oldest', 'viglet-dumont' ),
);
// phpcs:ignore WordPress.Security.NonceVerification.Recommended -- read-only, as above.
$viglet_dumont_sort = sanitize_key( wp_unslash( $_GET['sort'] ?? '' ) );
if ( ! isset( $viglet_dumont_sorts[ $viglet_dumont_sort ] ) ) {
	$viglet_dumont_sort = 'relevance';
}

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

$viglet_dumont_state = array(
	'query'         => $viglet_dumont_query,
	'status'        => 'idle',
	'results'       => array(),
	'spotlights'    => array(),
	'facets'        => array(),
	'pages'         => array(),
	'suggestions'   => array(),
	'applied'       => array(),
	'total'         => 0,
	'corrected'     => '',
	'error'         => '',
	'open'          => false,
	'active'        => -1,
	'recent'        => array(),
	'inputId'       => $viglet_dumont_id,
	'listId'        => $viglet_dumont_id . '-suggestions',
	// A count's forms for one and for more, which search-block.js picks between (D184).
	'optionsLabels' => array(
		/* translators: %d: how many suggestions the search box offers. */
		'one'   => _n( '%d suggestion. Use the up and down arrows to choose it.', '%d suggestions. Use the up and down arrows to choose one.', 1, 'viglet-dumont' ),
		/* translators: %d: how many suggestions the search box offers. */
		'other' => _n( '%d suggestion. Use the up and down arrows to choose it.', '%d suggestions. Use the up and down arrows to choose one.', 2, 'viglet-dumont' ),
	),
	'drawerOpen'    => false,
	'expanded'      => array(),
	'filtersLabel0' => __( 'Filters', 'viglet-dumont' ),
	/* translators: %d: how many filters are applied. */
	'filtersLabelN' => __( 'Filters (%d)', 'viglet-dumont' ),
	/* translators: %d: how many more values the facet has. */
	'moreValues'    => __( 'Show %d more', 'viglet-dumont' ),
	'fewerLabel'    => __( 'Show fewer', 'viglet-dumont' ),
	'answerEnabled' => ! empty( $attributes['showAnswer'] ),
	'answer'        => '',
	'answerSources' => array(),
	'answerStatus'  => 'idle',
	'answerRated'   => false,
	'sort'          => $viglet_dumont_sort,
	'perPage'       => max( 1, min( 100, (int) ( $attributes['perPage'] ?? 10 ) ) ),
	'typeLabels'    => $viglet_dumont_types,
	'homeUrl'       => home_url( '/' ),
	'foundLabels'   => array(
		/* translators: %d: how many results Turing found. */
		'one'   => _n( '%d result', '%d results', 1, 'viglet-dumont' ),
		/* translators: %d: how many results Turing found. */
		'other' => _n( '%d result', '%d results', 2, 'viglet-dumont' ),
	),
	'noneLabel'     => __( 'No results found. Try different keywords.', 'viglet-dumont' ),
	'errorLabel'    => __( 'Search is unavailable right now.', 'viglet-dumont' ),
);

// In the block editor the script does not run: draw Turing's answer to * on the server instead,
// through the same markup.
if ( Viglet_Dumont_Native_Search::is_editor_preview() ) {
	$viglet_dumont_state = array_merge(
		$viglet_dumont_state,
		( new Viglet_Dumont_Native_Search() )->preview_state( $viglet_dumont_state['perPage'], $viglet_dumont_state )
	);
}

wp_interactivity_state( 'viglet-dumont/search', $viglet_dumont_state );

// The layout the block's settings chose, as modifier classes style.css reads.
$viglet_dumont_show    = static fn ( string $part ): bool => (bool) ( $attributes[ $part ] ?? true );
$viglet_dumont_classes = implode(
	' ',
	array(
		'viglet-dumont-search',
		'viglet-dumont-search--facets-' . sanitize_html_class( $attributes['facetsPosition'] ?? 'left' ),
		'viglet-dumont-search--' . sanitize_html_class( $attributes['resultsLayout'] ?? 'list' ),
	)
);

$viglet_dumont_placeholder = '' !== (string) ( $attributes['placeholder'] ?? '' ) ? $attributes['placeholder'] : __( 'Search…', 'viglet-dumont' );
?>
<div
	<?php echo get_block_wrapper_attributes( array( 'class' => $viglet_dumont_classes ) ); // phpcs:ignore WordPress.Security.EscapeOutput.OutputNotEscaped -- core escapes the wrapper attributes. ?>
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

	<?php if ( ! empty( $attributes['showAnswer'] ) ) : ?>
		<section class="viglet-dumont-search__answer" aria-labelledby="<?php echo esc_attr( $viglet_dumont_id ); ?>-answer" hidden data-wp-bind--hidden="!state.showAnswer" data-wp-bind--aria-busy="state.answerStreaming">
			<h2 class="viglet-dumont-search__answer-label" id="<?php echo esc_attr( $viglet_dumont_id ); ?>-answer"><?php esc_html_e( 'Answer generated from this site', 'viglet-dumont' ); ?></h2>
			<p class="viglet-dumont-search__answer-text" data-wp-text="state.answer"></p>
			<ol class="viglet-dumont-search__sources" aria-label="<?php esc_attr_e( 'Sources', 'viglet-dumont' ); ?>">
				<template data-wp-each--source="state.answerSources" data-wp-each-key="context.source.key">
					<li data-wp-bind--value="context.source.rank">
						<a data-wp-bind--href="context.source.url" data-wp-text="context.source.title" data-wp-on--click="actions.citationClicked"></a>
					</li>
				</template>
			</ol>
			<p class="viglet-dumont-search__answer-feedback" data-wp-bind--hidden="state.answerRated">
				<?php esc_html_e( 'Was this helpful?', 'viglet-dumont' ); ?>
				<button type="button" class="viglet-dumont-search__link" data-wp-on--click="actions.answerHelpful"><?php esc_html_e( 'Yes', 'viglet-dumont' ); ?></button>
				<button type="button" class="viglet-dumont-search__link" data-wp-on--click="actions.answerNotHelpful"><?php esc_html_e( 'No', 'viglet-dumont' ); ?></button>
			</p>
			<p class="viglet-dumont-search__answer-feedback" role="status" hidden data-wp-bind--hidden="!state.answerRated"><?php esc_html_e( 'Thank you.', 'viglet-dumont' ); ?></p>
		</section>
	<?php endif; ?>

	<div class="viglet-dumont-search__layout">
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

		<div class="viglet-dumont-search__main">
			<ol class="viglet-dumont-search__spotlights" aria-label="<?php esc_attr_e( 'Featured', 'viglet-dumont' ); ?>" hidden data-wp-bind--hidden="!state.hasSpotlights">
				<template data-wp-each--spot="state.spotlights" data-wp-each-key="context.spot.url">
					<li class="viglet-dumont-search__result viglet-dumont-search__result--featured">
						<div class="viglet-dumont-search__body">
							<p class="viglet-dumont-search__meta"><span class="viglet-dumont-search__featured"><?php esc_html_e( 'Featured', 'viglet-dumont' ); ?></span></p>
							<h2 class="viglet-dumont-search__title"><a data-wp-bind--href="context.spot.url" data-wp-text="context.spot.title"></a></h2>
							<p class="viglet-dumont-search__snippet" data-wp-text="context.spot.content"></p>
						</div>
					</li>
				</template>
			</ol>
			<ol class="viglet-dumont-search__results" data-wp-bind--aria-busy="state.isLoading" data-wp-class--is-loading="state.isLoading">
				<template data-wp-each--result="state.results">
					<li class="viglet-dumont-search__result">
						<?php if ( $viglet_dumont_show( 'showImage' ) ) : ?>
							<img class="viglet-dumont-search__image" alt="" loading="lazy" hidden data-wp-bind--hidden="!context.result.image" data-wp-bind--src="context.result.image" />
						<?php endif; ?>
						<div class="viglet-dumont-search__body">
							<?php if ( $viglet_dumont_show( 'showType' ) || $viglet_dumont_show( 'showDate' ) ) : ?>
								<p class="viglet-dumont-search__meta">
									<?php if ( $viglet_dumont_show( 'showType' ) ) : ?>
										<span class="viglet-dumont-search__type" data-wp-text="context.result.type"></span>
									<?php endif; ?>
									<?php if ( $viglet_dumont_show( 'showDate' ) ) : ?>
										<time data-wp-bind--hidden="!context.result.date" data-wp-bind--datetime="context.result.iso" data-wp-text="context.result.date"></time>
									<?php endif; ?>
								</p>
							<?php endif; ?>
							<h2 class="viglet-dumont-search__title"><a data-wp-bind--href="context.result.url" data-wp-text="context.result.title" data-wp-on--click="actions.resultClicked" data-wp-on--auxclick="actions.resultClicked"></a></h2>
							<p class="viglet-dumont-search__crumb" data-wp-text="context.result.crumb"></p>
							<?php if ( $viglet_dumont_show( 'showSnippet' ) ) : ?>
								<p class="viglet-dumont-search__snippet" data-wp-watch="callbacks.snippet"></p>
							<?php endif; ?>
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
