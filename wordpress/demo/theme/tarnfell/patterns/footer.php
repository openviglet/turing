<?php
/**
 * Title: Footer
 * Slug: tarnfell/footer
 * Inserter: no
 *
 * The brand line and the help links, in the site's language (D223).
 *
 * @package Tarnfell
 */

?>
<!-- wp:group {"align":"full","tagName":"footer","style":{"spacing":{"padding":{"top":"var:preset|spacing|50","bottom":"var:preset|spacing|50"}}},"backgroundColor":"ink","textColor":"lichen","layout":{"type":"constrained","wideSize":"1200px"}} -->
<footer class="wp-block-group alignfull has-lichen-color has-ink-background-color has-text-color has-background" style="padding-top:var(--wp--preset--spacing--50);padding-bottom:var(--wp--preset--spacing--50)"><!-- wp:columns {"align":"wide"} -->
<div class="wp-block-columns alignwide"><!-- wp:column -->
<div class="wp-block-column"><!-- wp:heading {"level":2,"fontSize":"large","textColor":"paper"} -->
<h2 class="wp-block-heading has-paper-color has-text-color has-large-font-size">Tarnfell</h2>
<!-- /wp:heading -->

<!-- wp:paragraph {"fontSize":"small"} -->
<p class="has-small-font-size"><?php esc_html_e( 'Gear for the high ground, since the first wet boot. A demo store for Viglet Dumont: every product and post here is invented.', 'tarnfell' ); ?></p>
<!-- /wp:paragraph --></div>
<!-- /wp:column -->

<!-- wp:column -->
<div class="wp-block-column"><!-- wp:heading {"level":2,"fontSize":"large","textColor":"paper"} -->
<h2 class="wp-block-heading has-paper-color has-text-color has-large-font-size"><?php esc_html_e( 'Help', 'tarnfell' ); ?></h2>
<!-- /wp:heading -->

<!-- wp:list {"style":{"elements":{"link":{"color":{"text":"var:preset|color|lichen"}}}},"fontSize":"small"} -->
<ul class="wp-block-list has-link-color has-small-font-size"><!-- wp:list-item -->
<li><a href="/search-gear/"><?php esc_html_e( 'Search the shop', 'tarnfell' ); ?></a></li>
<!-- /wp:list-item -->

<!-- wp:list-item -->
<li><a href="/journal/"><?php esc_html_e( 'Trail journal', 'tarnfell' ); ?></a></li>
<!-- /wp:list-item -->

<!-- wp:list-item -->
<li><a href="?tour=1"><?php esc_html_e( 'How it works', 'tarnfell' ); ?></a></li>
<!-- /wp:list-item --></ul>
<!-- /wp:list --></div>
<!-- /wp:column --></div>
<!-- /wp:columns --></footer>
<!-- /wp:group -->
