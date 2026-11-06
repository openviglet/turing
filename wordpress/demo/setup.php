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

// The Turing search block and its parts, in the page so the plugin finds it (has_block), with
// popular searches under the box (D202), where the story's shopper arrives (D203).
$search = '<!-- wp:viglet-dumont/search {"placeholder":"Search jackets, boots, tents…","showAnswer":true,"suggestFilters":true,"applyFilters":true,"showTrending":true,"resultsLayout":"grid","align":"wide"} -->
<!-- wp:viglet-dumont/search-box /-->

<!-- wp:viglet-dumont/search-filters /-->

<!-- wp:viglet-dumont/search-results /-->

<!-- wp:viglet-dumont/search-pages /-->
<!-- /wp:viglet-dumont/search -->';

$pages = array(
	'home-demo'   => array( 'Home', '', '' ),
	'search-gear' => array( 'Search the shop', $search, 'page-search' ),
	'journal'     => array( 'Trail journal', '', '' ),
	// WordPress's own search beside Turing's, on one query (D195). Two columns of results need
	// the search page's wide template; the default page squeezed them into its text width.
	'compare-searches' => array( 'Compare searches', '<!-- wp:tarnfell/compare /-->', 'page-search' ),
	// Products ticked in the results or the copilot, side by side (D200).
	'compare-products' => array( 'Compare products', '<!-- wp:viglet-dumont/compare /-->', '' ),
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
			'<!-- wp:paragraph --><p>' . $say( 'Each block on this page is a Viglet Turing feature the plugin brings to WordPress. Open it with How it works for a note on each.' ) . '</p><!-- /wp:paragraph -->',
			// Shown by the theme only while Turing's chat cannot answer (D198).
			'<!-- wp:paragraph {"className":"tarnfell-ai-off"} --><p class="tarnfell-ai-off"><strong>' . $say( 'The AI features on this page are off.' ) . '</strong> ' . $say( 'This demo\'s Viglet Turing has no language model, so the answer, the catalog copilot and the Site assistant stay hidden. Start the stack with OPENAI_API_KEY set to try them.' ) . '</p><!-- /wp:paragraph -->',
			$heading( 'Search, with an answer' ),
			$search,
			// These two head themselves with the feature's name.
			'<!-- wp:viglet-dumont/copilot /-->',
			'<!-- wp:viglet-dumont/assistant /-->',
		)
	),
	'',
);

// Build my kit on a page of its own: the plan is not a search, and beside Ask a question on the
// showcase it read as an afterthought with an empty page around it. Here it is introduced,
// says what it does in three steps, and has the width for the kit it builds.
$step  = static fn ( string $title, string $text ): string =>
	'<!-- wp:column --><div class="wp-block-column"><!-- wp:heading {"level":3,"fontSize":"large"} --><h3 class="wp-block-heading has-large-font-size">' . $say( $title ) . '</h3><!-- /wp:heading -->'
	. '<!-- wp:paragraph --><p>' . $say( $text ) . '</p><!-- /wp:paragraph --></div><!-- /wp:column -->';
$pages['plan-a-kit'] = array(
	'Plan a kit',
	implode(
		"\n\n",
		array(
			'<!-- wp:group {"align":"wide","className":"tarnfell-plan-hero","style":{"spacing":{"padding":{"top":"var:preset|spacing|50","bottom":"var:preset|spacing|50","left":"var:preset|spacing|50","right":"var:preset|spacing|50"}},"border":{"radius":"6px"},"color":{"gradient":"linear-gradient(160deg,#1F3B2D 0%,#2E5E73 100%)"}},"textColor":"paper","layout":{"type":"constrained","contentSize":"760px","justifyContent":"left"}} -->'
			. '<div class="wp-block-group alignwide tarnfell-plan-hero has-paper-color has-text-color has-background" style="border-radius:6px;background:linear-gradient(160deg,#1F3B2D 0%,#2E5E73 100%);padding-top:var(--wp--preset--spacing--50);padding-right:var(--wp--preset--spacing--50);padding-bottom:var(--wp--preset--spacing--50);padding-left:var(--wp--preset--spacing--50)">'
			. '<!-- wp:heading {"textColor":"paper","fontSize":"x-large"} --><h2 class="wp-block-heading has-paper-color has-text-color has-x-large-font-size">' . $say( 'Tell us about your trip. We put the kit together.' ) . '</h2><!-- /wp:heading -->'
			. '<!-- wp:paragraph {"fontSize":"medium"} --><p class="has-medium-font-size">' . $say( 'Where you are going, for how long and when, in your own words. Viglet Turing works out what the trip needs and fills each need with gear we have in stock, at its price.' ) . '</p><!-- /wp:paragraph --></div><!-- /wp:group -->',
			'<!-- wp:columns {"align":"wide","className":"tarnfell-plan-steps"} --><div class="wp-block-columns alignwide tarnfell-plan-steps">'
			. $step( '1. Describe the plan', 'Two days across the mountains in July, a first winter camp, a day walk with rain forecast.' )
			. $step( '2. See what it needs', 'Grouped by need, a place to sleep, something warm, safe footing, each with why it is there.' )
			. $step( '3. Take it all at once', 'Swap any item, untick what you have, and put the whole kit in the cart with one click.' )
			. '</div><!-- /wp:columns -->',
			'<!-- wp:viglet-dumont/copilot ' . wp_json_encode(
				array(
					'heading'     => $names['Your plan'] ?? 'Your plan',
					'placeholder' => $names['e.g. two days across the mountains in July, first time'] ?? 'e.g. two days across the mountains in July, first time',
					'kit'         => true,
					'planOnly'    => true,
					'align'       => 'wide',
					'className'   => 'tarnfell-plan',
				),
				JSON_UNESCAPED_UNICODE
			) . ' /-->',
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
update_post_meta( $ids['compare-products'], '_viglet_dumont_exclude', '1' );
// The home, the search page and the journal hold no text of their own: indexed, a page is one
// more document Turing's kit can stage as a product (D203).
foreach ( array( 'home-demo', 'search-gear', 'journal' ) as $chrome ) {
	update_post_meta( $ids[ $chrome ], '_viglet_dumont_exclude', '1' );
}
// The showcase describes features, not gear: never a search result. Nor is the kit's page,
// which the kit itself would otherwise stage as a product.
update_post_meta( $ids['showcase'], '_viglet_dumont_exclude', '1' );
update_post_meta( $ids['plan-a-kit'], '_viglet_dumont_exclude', '1' );

// WordPress's own first post and page are no part of a shop, and in its search they would
// read as a fresh install (D264).
foreach ( array( array( 'hello-world', 'post' ), array( 'sample-page', 'page' ) ) as list( $default, $kind ) ) {
	$post = get_page_by_path( $default, OBJECT, $kind );
	if ( $post ) {
		wp_delete_post( $post->ID, true );
	}
}

update_option( 'show_on_front', 'page' );
update_option( 'page_on_front', $ids['home-demo'] );
update_option( 'page_for_posts', $ids['journal'] );
// The store's name in its language: Tarnfell in English, Recanto da Serra in Portuguese.
update_option( 'blogname', $names['Tarnfell'] ?? 'Tarnfell' );
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
// Checkout in test mode (D203): an offline payment, so an order is placed and nothing is charged.
update_option( 'woocommerce_cheque_settings', array( 'enabled' => 'yes' ) + (array) get_option( 'woocommerce_cheque_settings', array() ) );
viglet_dumont_update_settings( $settings );

echo wp_json_encode( $ids ), "\n";
