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
 * @package Viglet_Turing
 *
 * @var array  $attributes The block's attributes.
 * @var string $content    Its inner blocks, drawn.
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

wp_enqueue_script_module( 'viglet-turing/search-block' );

$viglet_turing_id = Viglet_Turing_Search_Block::id();

// A result's badge reads the post type's own name, in the site's language.
$viglet_turing_types = array();
foreach ( viglet_turing_get_indexable_types() as $viglet_turing_type ) {
	$viglet_turing_object = get_post_type_object( $viglet_turing_type );
	if ( $viglet_turing_object ) {
		$viglet_turing_types[ $viglet_turing_type ] = $viglet_turing_object->labels->singular_name;
	}
}

// A PDF in search (D309) is no post type: its badge and facet value read "PDF".
$viglet_turing_types[ Viglet_Turing_Pdf::TYPE ] = __( 'PDF', 'viglet-turing-search' );

// A type another source sent under this site key (D327), "Article" from a catalog, reads the
// label Fields gave it.
$viglet_turing_types += ( new Viglet_Turing_Mapping() )->foreign_labels();

$viglet_turing_price = Viglet_Turing_Search_Block::price_field();

$viglet_turing_state = array(
	'query'           => Viglet_Turing_Search_Block::query(),
	'status'          => 'idle',
	'results'         => array(),
	'spotlights'      => array(),
	'facets'          => array(),
	'pages'           => array(),
	'suggestions'     => array(),
	'applied'         => array(),
	'total'           => 0,
	'corrected'       => '',
	'showingFor'      => '',
	'insteadOf'       => '',
	'closeMatches'    => false,
	'error'           => '',
	'open'            => false,
	'active'          => -1,
	'recent'          => array(),
	'inputId'         => $viglet_turing_id,
	'listId'          => $viglet_turing_id . '-suggestions',
	// A count's forms for one and for more, which search-block.js picks between (D184).
	'optionsLabels'   => array(
		/* translators: %d: how many suggestions the search box offers. */
		'one'   => _n( '%d suggestion. Use the up and down arrows to choose it.', '%d suggestions. Use the up and down arrows to choose one.', 1, 'viglet-turing-search' ),
		/* translators: %d: how many suggestions the search box offers. */
		'other' => _n( '%d suggestion. Use the up and down arrows to choose it.', '%d suggestions. Use the up and down arrows to choose one.', 2, 'viglet-turing-search' ),
	),
	'drawerOpen'      => false,
	'expanded'        => array(),
	'filtersLabel0'   => __( 'Filters', 'viglet-turing-search' ),
	/* translators: %d: how many filters are applied. */
	'filtersLabelN'   => __( 'Filters (%d)', 'viglet-turing-search' ),
	/* translators: %d: how many more values the facet has. */
	'moreValues'      => __( 'Show %d more', 'viglet-turing-search' ),
	'fewerLabel'      => __( 'Show fewer', 'viglet-turing-search' ),
	'answerEnabled'   => ! empty( $attributes['showAnswer'] ),
	'answer'          => '',
	'answerSources'   => array(),
	'answerStatus'    => 'idle',
	'answerRated'     => false,
	// A Site assistant on the page that can continue the answer (T1258), told by assistant.js.
	'assistantHere'   => false,
	// Turing refused the answer with 429: the site's AI budget is spent until it resets (D332).
	'answerPaused'    => false,
	// A spent AI budget, with its reset when the site knows it (D336).
	'pausedLabel'     => Viglet_Turing_AI_Quota::visitor_line(),
	'pausedUntil'     => Viglet_Turing_AI_Quota::visitor_until(),
	// The filters a sentence names (D170), and the names of the core fields they may filter.
	'suggestEnabled'  => ! empty( $attributes['suggestFilters'] ),
	'suggested'       => array(),
	// D199: with applyFilters on, the filters a sentence names are applied, said in a line and
	// undone with one click, instead of offered; a price it names is read in code as well, so
	// "under R$400" filters with no language model in Turing.
	'applyEnabled'    => ! empty( $attributes['suggestFilters'] ) && ! empty( $attributes['applyFilters'] ),
	'priceField'      => $viglet_turing_price ? $viglet_turing_price['field'] : '',
	'understood'      => '',
	/* translators: %s: the filters read from the visitor's sentence, comma-separated. */
	'understoodText'  => __( 'Understood: %s', 'viglet-turing-search' ),
	// A product result's "Add to cart" (D173): its name for a screen reader, and what it did.
	/* translators: %s: the product's name. */
	'cartLabel'       => __( 'Add to cart: %s', 'viglet-turing-search' ),
	/* translators: %d: how many products, two to four. */
	'compareLabel'    => __( 'Compare %d products', 'viglet-turing-search' ),
	/* translators: %s: a product's name. */
	'compareFor'      => __( 'Compare %s', 'viglet-turing-search' ),
	'cartAdded'       => __( 'Added to your cart.', 'viglet-turing-search' ),
	'cartFailed'      => __( 'It could not be added to the cart.', 'viglet-turing-search' ),
	'fieldLabels'     => array_merge(
		array(
			'type'             => __( 'Type', 'viglet-turing-search' ),
			'publication_date' => __( 'Published', 'viglet-turing-search' ),
			// The site facet, when this site also searches others (D303).
			'site_key'         => __( 'Site', 'viglet-turing-search' ),
		),
		$viglet_turing_price ? array( $viglet_turing_price['field'] => $viglet_turing_price['label'] ) : array()
	),
	'sort'            => Viglet_Turing_Search_Block::sort(),
	'perPage'         => max( 1, min( 100, (int) ( $attributes['perPage'] ?? 10 ) ) ),
	// The block's scope (D306): applied to every search it makes, never a chip to remove, and
	// where "Search the whole site" leads with the same query.
	'fixedFilters'    => Viglet_Turing_Search_Block::scope( $attributes ),
	'wholeSiteUrl'    => Viglet_Turing_Search_Block::whole_site_url( $attributes ),
	'wholeSiteParam'  => ( new Viglet_Turing_Native_Search() )->search_page()['queryParam'],
	'typeLabels'      => $viglet_turing_types,
	'homeUrl'         => home_url( '/' ),
	'foundLabels'     => array(
		/* translators: %d: how many results Turing found. */
		'one'   => _n( '%d result', '%d results', 1, 'viglet-turing-search' ),
		/* translators: %d: how many results Turing found. */
		'other' => _n( '%d result', '%d results', 2, 'viglet-turing-search' ),
	),
	'noneLabel'       => __( 'No results found. Try different keywords.', 'viglet-turing-search' ),
	// While a sentence's filters are read, its words as typed are not the search (D262).
	'readingLabel'    => __( 'Reading your search…', 'viglet-turing-search' ),
	'readingSentence' => false,
	'typingSentence'  => false,
	'errorLabel'      => __( 'Search is unavailable right now.', 'viglet-turing-search' ),
);

// In the block editor the script does not run: draw Turing's answer to * on the server instead,
// through the same markup.
if ( Viglet_Turing_Native_Search::is_editor_preview() ) {
	$viglet_turing_state = array_merge(
		$viglet_turing_state,
		( new Viglet_Turing_Native_Search() )->preview_state( $viglet_turing_state['perPage'], $viglet_turing_state )
	);
} elseif (
	// A page opened with a search draws its first page here, so nothing moves once the script
	// answers (D209); a block with no results of its own sends its query elsewhere (D208).
	( '' !== $viglet_turing_state['query'] || isset( $_GET['fq'] ) ) // phpcs:ignore WordPress.Security.NonceVerification.Recommended -- read-only.
	&& ( '' === trim( (string) $content ) || str_contains( (string) $content, 'viglet-turing-search__results' ) )
) {
	$viglet_turing_state = array_merge(
		$viglet_turing_state,
		( new Viglet_Turing_Native_Search() )->first_page( $viglet_turing_state )
	);
}

wp_interactivity_state( 'viglet-turing/search', $viglet_turing_state );

// The layout the block's settings chose, as modifier classes style.css reads.
$viglet_turing_parts = '' !== trim( (string) $content );
// A block holding no results part, a search box in a hero say, sends the query to the full
// search page (D208), unless that page is this one.
$viglet_turing_elsewhere = null;
$viglet_turing_inner     = (string) $content;
if ( $viglet_turing_parts && ! str_contains( (string) $content, 'viglet-turing-search__results' ) ) {
	$viglet_turing_target = ( new Viglet_Turing_Native_Search() )->search_page();
	if ( untrailingslashit( $viglet_turing_target['searchPage'] ) !== untrailingslashit( (string) get_permalink() ) ) {
		$viglet_turing_elsewhere = array(
			'url'   => $viglet_turing_target['searchPage'],
			'param' => $viglet_turing_target['queryParam'],
		);
		// Its popular searches go there too: drawn before this block knew where its searches
		// go, each chip linked to this page, so a chip on a home page's hero box opened the
		// home again with ?q= and no results under it.
		$viglet_turing_inner = Viglet_Turing_Search_Block::chips_to( (string) $content, $viglet_turing_target['searchPage'], $viglet_turing_target['queryParam'] );
	}
}
$viglet_turing_classes = implode(
	' ',
	array_filter(
		array(
			'viglet-turing-search',
			'viglet-turing-search--facets-' . sanitize_html_class( $attributes['facetsPosition'] ?? 'left' ),
			'viglet-turing-search--' . sanitize_html_class( $attributes['resultsLayout'] ?? 'list' ),
			$viglet_turing_parts ? 'viglet-turing-search--parts' : '',
		)
	)
);
?>
<div
	<?php echo wp_kses_data( get_block_wrapper_attributes( array( 'class' => $viglet_turing_classes ) ) ); ?>
	data-wp-interactive="viglet-turing/search"
	<?php if ( $viglet_turing_elsewhere ) : ?>
		data-wp-context="<?php echo esc_attr( (string) wp_json_encode( array( 'elsewhere' => $viglet_turing_elsewhere ) ) ); ?>"
	<?php endif; ?>
	data-wp-init="callbacks.init"
>
	<?php if ( $viglet_turing_parts ) : ?>
		<?php
		// The inner blocks (the parts, and any block a theme puts between them), each already
		// rendered and escaped by its own render callback, as WordPress hands $content to every
		// block that wraps others.
		echo $viglet_turing_inner; // phpcs:ignore WordPress.Security.EscapeOutput.OutputNotEscaped -- inner blocks, escaped by their own render callbacks; chips_to() only rewrites hrefs through the HTML API.
		?>
	<?php else : ?>
		<?php
		// The default order, with the filters and the results side by side.
		Viglet_Turing_Search_Block::render( array( 'box', 'correction', 'trending', 'toolbar', 'applied', 'suggested', 'toggle', 'answer' ), $attributes );
		?>
		<div class="viglet-turing-search__layout">
			<?php Viglet_Turing_Search_Block::render( array( 'facets', 'main' ), $attributes ); ?>
		</div>
		<?php Viglet_Turing_Search_Block::render( array( 'pages' ), $attributes ); ?>
	<?php endif; ?>
</div>
<?php
Viglet_Turing_Search_Block::done();
