<?php
/**
 * The demo storefront (D193), run by `run-docker.cmd demo` after the stack is up: the Tarnfell
 * theme, a static home, the search page and the journal, and the plugin's instant search on
 * the theme's own search box. Idempotent. It runs on the demo's own WordPress (D219), never
 * on the browser tests' site.
 *
 * @package Viglet_Dumont
 */

// phpcs:disable -- a setup script run by WP-CLI, not plugin code.

// The Turing search block and its parts, in the page so the plugin finds it (has_block).
$search = '<!-- wp:viglet-dumont/search {"placeholder":"Search jackets, boots, tents…","showAnswer":true,"suggestFilters":true,"resultsLayout":"grid","align":"wide"} -->
<!-- wp:viglet-dumont/search-box /-->

<!-- wp:viglet-dumont/search-filters /-->

<!-- wp:viglet-dumont/search-results /-->

<!-- wp:viglet-dumont/search-pages /-->
<!-- /wp:viglet-dumont/search -->';

$pages = array(
	'home-demo'   => array( 'Home', '', '' ),
	'search-gear' => array( 'Search the shop', $search, 'page-search' ),
	'journal'     => array( 'Trail journal', '', '' ),
	// WordPress's own search beside Turing's, on one query (D195).
	'compare-searches' => array( 'Compare searches', '<!-- wp:tarnfell/compare /-->', '' ),
);

// Named in the site's language (D223): seed/pt_BR.php carries the Portuguese.
$names = 'pt_BR' === get_locale() ? ( require __DIR__ . '/seed/pt_BR.php' )['pages'] : array();
$say   = static fn ( string $text ): string => esc_html( $names[ $text ] ?? $text );

// Every Turing block on one page (D179), each under the feature it shows; ?tour=1 numbers them.
$heading           = static fn ( string $text ): string => '<!-- wp:heading --><h2 class="wp-block-heading">' . $say( $text ) . '</h2><!-- /wp:heading -->';
$pages['showcase'] = array(
	'Showcase',
	implode(
		"\n\n",
		array(
			'<!-- wp:paragraph --><p>' . $say( 'Each block on this page is a Turing feature the plugin brings to WordPress. Open it with How it works for a note on each.' ) . '</p><!-- /wp:paragraph -->',
			$heading( 'Search, with an answer' ),
			$search,
			// These two head themselves with the feature's name.
			'<!-- wp:viglet-dumont/copilot /-->',
			'<!-- wp:viglet-dumont/assistant /-->',
		)
	),
	'',
);

$ids = array();
foreach ( $pages as $slug => list( $title, $content, $template ) ) {
	$title = $names[ $title ] ?? $title;
	$found = get_page_by_path( $slug, OBJECT, 'page' );
	$args  = array(
		'post_type'    => 'page',
		'post_name'    => $slug,
		'post_title'   => $title,
		'post_content' => $content,
		'post_status'  => 'publish',
	);
	$ids[ $slug ] = $found ? (int) wp_update_post( array( 'ID' => $found->ID ) + $args ) : (int) wp_insert_post( $args );
	update_post_meta( $ids[ $slug ], '_wp_page_template', $template );
}

// The compare page names every curated search: indexed, it would be a result for each.
update_post_meta( $ids['compare-searches'], '_viglet_dumont_exclude', '1' );
// The showcase describes features, not gear: never a search result.
update_post_meta( $ids['showcase'], '_viglet_dumont_exclude', '1' );

update_option( 'show_on_front', 'page' );
update_option( 'page_on_front', $ids['home-demo'] );
update_option( 'page_for_posts', $ids['journal'] );
update_option( 'blogname', 'Tarnfell' );
update_option( 'blogdescription', $names['Gear for the high ground'] ?? 'Gear for the high ground' );

// The hero's search box opens the instant search, and Enter goes to the page above.
$settings                      = viglet_dumont_get_settings();
$settings['search_enabled']    = 1;
$settings['instant_overlay']   = 1;
$settings['search_page']       = $ids['search-gear'];
// The theme's templates place Related themselves; appended too, it would show twice (D210).
$settings['related_after']     = array();
// Turing's generative AI on the store (D198): sent with the fields on the next index (D186), and
// the Site assistant on every page. Both stay hidden on a stack whose Turing has no LLM key.
$settings['generative_ai']      = 1;
$settings['assistant_launcher'] = 1;

// A launched store: WooCommerce shows visitors a "coming soon" page until it is.
update_option( 'woocommerce_coming_soon', 'no' );
viglet_dumont_update_settings( $settings );

echo wp_json_encode( $ids ), "\n";
