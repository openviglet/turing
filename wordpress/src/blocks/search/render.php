<?php
/**
 * The Turing search block, server side: the markup, with Interactivity API directives that
 * src/public/search-block.js drives through the Turing JS SDK.
 *
 * The query starts from ?q= (or ?s=), so a results URL can be shared and the block also
 * works as a site's search page. Everything the visitor sees comes from Turing, filtered to
 * this site's key.
 *
 * The block owns the store and its state. Its parts are pieces (blocks/search/parts/): with
 * no inner blocks, the block draws them in its default order; with inner blocks (search
 * box, filters, results, pages, and any other block a theme puts between them), it draws
 * those, in the order the editor gave them (D185).
 *
 * @package Viglet_Dumont
 *
 * @var array  $attributes The block's attributes.
 * @var string $content    Its inner blocks, drawn.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

wp_enqueue_script_module( 'viglet-dumont/search-block' );

$viglet_dumont_id = Viglet_Dumont_Search_Block::id();

// A result's badge reads the post type's own name, in the site's language.
$viglet_dumont_types = array();
foreach ( viglet_dumont_get_indexable_types() as $viglet_dumont_type ) {
	$viglet_dumont_object = get_post_type_object( $viglet_dumont_type );
	if ( $viglet_dumont_object ) {
		$viglet_dumont_types[ $viglet_dumont_type ] = $viglet_dumont_object->labels->singular_name;
	}
}

$viglet_dumont_state = array(
	'query'          => Viglet_Dumont_Search_Block::query(),
	'status'         => 'idle',
	'results'        => array(),
	'spotlights'     => array(),
	'facets'         => array(),
	'pages'          => array(),
	'suggestions'    => array(),
	'applied'        => array(),
	'total'          => 0,
	'corrected'      => '',
	'error'          => '',
	'open'           => false,
	'active'         => -1,
	'recent'         => array(),
	'inputId'        => $viglet_dumont_id,
	'listId'         => $viglet_dumont_id . '-suggestions',
	// A count's forms for one and for more, which search-block.js picks between (D184).
	'optionsLabels'  => array(
		/* translators: %d: how many suggestions the search box offers. */
		'one'   => _n( '%d suggestion. Use the up and down arrows to choose it.', '%d suggestions. Use the up and down arrows to choose one.', 1, 'viglet-dumont' ),
		/* translators: %d: how many suggestions the search box offers. */
		'other' => _n( '%d suggestion. Use the up and down arrows to choose it.', '%d suggestions. Use the up and down arrows to choose one.', 2, 'viglet-dumont' ),
	),
	'drawerOpen'     => false,
	'expanded'       => array(),
	'filtersLabel0'  => __( 'Filters', 'viglet-dumont' ),
	/* translators: %d: how many filters are applied. */
	'filtersLabelN'  => __( 'Filters (%d)', 'viglet-dumont' ),
	/* translators: %d: how many more values the facet has. */
	'moreValues'     => __( 'Show %d more', 'viglet-dumont' ),
	'fewerLabel'     => __( 'Show fewer', 'viglet-dumont' ),
	'answerEnabled'  => ! empty( $attributes['showAnswer'] ),
	'answer'         => '',
	'answerSources'  => array(),
	'answerStatus'   => 'idle',
	'answerRated'    => false,
	// The filters a sentence names (D170), and the names of the core fields they may filter.
	'suggestEnabled' => ! empty( $attributes['suggestFilters'] ),
	'suggested'      => array(),
	// A product result's "Add to cart" (D173): its name for a screen reader, and what it did.
	/* translators: %s: the product's name. */
	'cartLabel'      => __( 'Add to cart: %s', 'viglet-dumont' ),
	/* translators: %d: how many products, two to four. */
	'compareLabel'   => __( 'Compare %d products', 'viglet-dumont' ),
	/* translators: %s: a product's name. */
	'compareFor'     => __( 'Compare %s', 'viglet-dumont' ),
	'cartAdded'      => __( 'Added to your cart.', 'viglet-dumont' ),
	'cartFailed'     => __( 'It could not be added to the cart.', 'viglet-dumont' ),
	'fieldLabels'    => array(
		'type'             => __( 'Type', 'viglet-dumont' ),
		'publication_date' => __( 'Published', 'viglet-dumont' ),
	),
	'sort'           => Viglet_Dumont_Search_Block::sort(),
	'perPage'        => max( 1, min( 100, (int) ( $attributes['perPage'] ?? 10 ) ) ),
	'typeLabels'     => $viglet_dumont_types,
	'homeUrl'        => home_url( '/' ),
	'foundLabels'    => array(
		/* translators: %d: how many results Turing found. */
		'one'   => _n( '%d result', '%d results', 1, 'viglet-dumont' ),
		/* translators: %d: how many results Turing found. */
		'other' => _n( '%d result', '%d results', 2, 'viglet-dumont' ),
	),
	'noneLabel'      => __( 'No results found. Try different keywords.', 'viglet-dumont' ),
	'errorLabel'     => __( 'Search is unavailable right now.', 'viglet-dumont' ),
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
$viglet_dumont_parts = '' !== trim( (string) $content );
// A block holding no results part, a search box in a hero say, sends the query to the full
// search page (D208), unless that page is this one.
$viglet_dumont_elsewhere = null;
if ( $viglet_dumont_parts && ! str_contains( (string) $content, 'viglet-dumont-search__results' ) ) {
	$viglet_dumont_target = ( new Viglet_Dumont_Native_Search() )->search_page();
	if ( untrailingslashit( $viglet_dumont_target['searchPage'] ) !== untrailingslashit( (string) get_permalink() ) ) {
		$viglet_dumont_elsewhere = array(
			'url'   => $viglet_dumont_target['searchPage'],
			'param' => $viglet_dumont_target['queryParam'],
		);
	}
}
$viglet_dumont_classes = implode(
	' ',
	array_filter(
		array(
			'viglet-dumont-search',
			'viglet-dumont-search--facets-' . sanitize_html_class( $attributes['facetsPosition'] ?? 'left' ),
			'viglet-dumont-search--' . sanitize_html_class( $attributes['resultsLayout'] ?? 'list' ),
			$viglet_dumont_parts ? 'viglet-dumont-search--parts' : '',
		)
	)
);
?>
<div
	<?php echo get_block_wrapper_attributes( array( 'class' => $viglet_dumont_classes ) ); // phpcs:ignore WordPress.Security.EscapeOutput.OutputNotEscaped -- core escapes the wrapper attributes. ?>
	data-wp-interactive="viglet-dumont/search"
	<?php echo $viglet_dumont_elsewhere ? wp_interactivity_data_wp_context( array( 'elsewhere' => $viglet_dumont_elsewhere ) ) : ''; // phpcs:ignore WordPress.Security.EscapeOutput.OutputNotEscaped -- core escapes the context attribute. ?>
	data-wp-init="callbacks.init"
>
	<?php
	if ( $viglet_dumont_parts ) {
		echo $content; // phpcs:ignore WordPress.Security.EscapeOutput.OutputNotEscaped -- inner blocks, drawn by WordPress.
	} else {
		// The default order, with the filters and the results side by side.
		// phpcs:disable WordPress.Security.EscapeOutput.OutputNotEscaped -- the pieces escape their own output.
		echo Viglet_Dumont_Search_Block::pieces( array( 'box', 'correction', 'trending', 'toolbar', 'applied', 'suggested', 'toggle', 'answer' ), $attributes );
		echo '<div class="viglet-dumont-search__layout">';
		echo Viglet_Dumont_Search_Block::pieces( array( 'facets', 'main' ), $attributes );
		echo '</div>';
		echo Viglet_Dumont_Search_Block::pieces( array( 'pages' ), $attributes );
		// phpcs:enable WordPress.Security.EscapeOutput.OutputNotEscaped
	}
	?>
</div>
<?php
Viglet_Dumont_Search_Block::done();
