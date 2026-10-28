<?php
/**
 * Title: Front page
 * Slug: tarnfell/front-page
 * Inserter: no
 *
 * The home: the hero whose search box is the plugin's, the terrain tiles and the latest guides,
 * in the site's language (D223).
 *
 * @package Tarnfell
 */

$tarnfell_tiles = array(
	array( 'pine', __( 'hiking', 'tarnfell' ), __( 'Hill and trail', 'tarnfell' ), __( 'Boots, packs and shells for a long day out.', 'tarnfell' ) ),
	array( 'tarn', __( 'waterproof', 'tarnfell' ), __( 'Wet weather', 'tarnfell' ), __( 'Waterproofs that breathe on the climb.', 'tarnfell' ) ),
	array( 'moss', __( 'camping', 'tarnfell' ), __( 'Camp and bivvy', 'tarnfell' ), __( 'Tents, bags and stoves for the night up high.', 'tarnfell' ) ),
);
?>
<!-- wp:group {"tagName":"main","style":{"spacing":{"blockGap":"0"}},"layout":{"type":"constrained","wideSize":"1200px"}} -->
<main class="wp-block-group"><!-- wp:group {"align":"full","className":"tarnfell-hero","style":{"spacing":{"padding":{"top":"var:preset|spacing|60","bottom":"var:preset|spacing|60"}},"color":{"gradient":"linear-gradient(160deg,#1F3B2D 0%,#2E5E73 100%)"}},"textColor":"paper","layout":{"type":"constrained","contentSize":"760px"}} -->
<div class="wp-block-group alignfull tarnfell-hero has-paper-color has-text-color has-background" style="background:linear-gradient(160deg,#1F3B2D 0%,#2E5E73 100%);padding-top:var(--wp--preset--spacing--60);padding-bottom:var(--wp--preset--spacing--60)"><!-- wp:heading {"level":1,"textColor":"paper"} -->
<h1 class="wp-block-heading has-paper-color has-text-color"><?php esc_html_e( 'Gear for the high ground', 'tarnfell' ); ?></h1>
<!-- /wp:heading -->

<!-- wp:paragraph {"textColor":"lichen","fontSize":"large"} -->
<p class="has-lichen-color has-text-color has-large-font-size"><?php esc_html_e( 'Tell us where you are going. Search answers as you type, in words a shop assistant would understand.', 'tarnfell' ); ?></p>
<!-- /wp:paragraph -->

<!-- wp:viglet-dumont/search <?php echo wp_json_encode( array( 'placeholder' => __( 'A waterproof jacket for the Lakes…', 'tarnfell' ) ) ); ?> -->
<!-- wp:viglet-dumont/search-box /-->
<!-- /wp:viglet-dumont/search -->

<!-- wp:paragraph {"textColor":"lichen","fontSize":"small"} -->
<p class="has-lichen-color has-text-color has-small-font-size"><?php esc_html_e( 'Try “light jacket for rain”, “raincoat” or press Ctrl+K anywhere.', 'tarnfell' ); ?></p>
<!-- /wp:paragraph --></div>
<!-- /wp:group -->

<!-- wp:group {"style":{"spacing":{"padding":{"top":"var:preset|spacing|50","bottom":"var:preset|spacing|50"}}},"layout":{"type":"constrained","wideSize":"1200px"}} -->
<div class="wp-block-group" style="padding-top:var(--wp--preset--spacing--50);padding-bottom:var(--wp--preset--spacing--50)"><!-- wp:heading {"align":"wide"} -->
<h2 class="wp-block-heading alignwide"><?php esc_html_e( 'Shop by terrain', 'tarnfell' ); ?></h2>
<!-- /wp:heading -->

<!-- wp:group {"align":"wide","layout":{"type":"grid","minimumColumnWidth":"14rem"}} -->
<div class="wp-block-group alignwide"><?php foreach ( $tarnfell_tiles as list( $tarnfell_colour, $tarnfell_query, $tarnfell_title, $tarnfell_text ) ) : ?><!-- wp:group {"className":"tarnfell-tile","style":{"spacing":{"padding":{"top":"var:preset|spacing|40","bottom":"var:preset|spacing|40","left":"var:preset|spacing|40","right":"var:preset|spacing|40"}},"border":{"radius":"6px"}},"backgroundColor":"<?php echo esc_attr( $tarnfell_colour ); ?>","textColor":"paper","layout":{"type":"default"}} -->
<div class="wp-block-group tarnfell-tile has-paper-color has-<?php echo esc_attr( $tarnfell_colour ); ?>-background-color has-text-color has-background" style="border-radius:6px;padding-top:var(--wp--preset--spacing--40);padding-right:var(--wp--preset--spacing--40);padding-bottom:var(--wp--preset--spacing--40);padding-left:var(--wp--preset--spacing--40)"><!-- wp:heading {"level":3,"textColor":"paper"} -->
<h3 class="wp-block-heading has-paper-color has-text-color"><a href="<?php echo esc_url( '/search-gear/?q=' . rawurlencode( $tarnfell_query ) ); ?>"><?php echo esc_html( $tarnfell_title ); ?></a></h3>
<!-- /wp:heading -->

<!-- wp:paragraph {"fontSize":"small"} -->
<p class="has-small-font-size"><?php echo esc_html( $tarnfell_text ); ?></p>
<!-- /wp:paragraph --></div>
<!-- /wp:group --><?php endforeach; ?></div>
<!-- /wp:group --></div>
<!-- /wp:group -->

<!-- wp:group {"align":"full","style":{"spacing":{"padding":{"top":"var:preset|spacing|50","bottom":"var:preset|spacing|50"}}},"backgroundColor":"lichen","layout":{"type":"constrained","wideSize":"1200px"}} -->
<div class="wp-block-group alignfull has-lichen-background-color has-background" style="padding-top:var(--wp--preset--spacing--50);padding-bottom:var(--wp--preset--spacing--50)"><!-- wp:heading {"align":"wide"} -->
<h2 class="wp-block-heading alignwide"><?php esc_html_e( 'From the trail journal', 'tarnfell' ); ?></h2>
<!-- /wp:heading -->

<!-- wp:query {"queryId":1,"query":{"perPage":3,"postType":"post","order":"desc","orderBy":"date","inherit":false},"align":"wide","layout":{"type":"default"}} -->
<div class="wp-block-query alignwide"><!-- wp:post-template {"layout":{"type":"grid","columnCount":3,"minimumColumnWidth":"16rem"}} -->
<!-- wp:post-title {"level":3,"isLink":true} /-->

<!-- wp:post-date {"fontSize":"small","textColor":"granite"} /-->

<!-- wp:post-excerpt {"excerptLength":24} /-->
<!-- /wp:post-template --></div>
<!-- /wp:query --></div>
<!-- /wp:group --></main>
<!-- /wp:group -->
