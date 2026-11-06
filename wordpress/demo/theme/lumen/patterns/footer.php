<?php
/**
 * Title: Footer
 * Slug: lumen/footer
 * Inserter: no
 *
 * The school's line and the help links, in the site's language.
 *
 * @package Lumen
 */

?>
<!-- wp:group {"align":"full","tagName":"footer","style":{"spacing":{"padding":{"top":"var:preset|spacing|50","bottom":"var:preset|spacing|50"}},"elements":{"link":{"color":{"text":"var:preset|color|ochre"}}}},"backgroundColor":"ink","textColor":"mist","layout":{"type":"constrained","wideSize":"1160px"}} -->
<footer class="wp-block-group alignfull has-mist-color has-ink-background-color has-text-color has-background has-link-color" style="padding-top:var(--wp--preset--spacing--50);padding-bottom:var(--wp--preset--spacing--50)"><!-- wp:columns {"align":"wide"} -->
<div class="wp-block-columns alignwide"><!-- wp:column -->
<div class="wp-block-column"><!-- wp:heading {"level":2,"fontSize":"large","textColor":"paper"} -->
<h2 class="wp-block-heading has-paper-color has-text-color has-large-font-size"><?php echo esc_html( get_bloginfo( 'name' ) ); ?></h2>
<!-- /wp:heading -->

<!-- wp:paragraph {"fontSize":"small"} -->
<p class="has-small-font-size"><?php esc_html_e( 'A demo school for Viglet Turing Search: every event, opening and book here is invented.', 'lumen' ); ?></p>
<!-- /wp:paragraph --></div>
<!-- /wp:column -->

<!-- wp:column -->
<div class="wp-block-column"><!-- wp:heading {"level":2,"fontSize":"large","textColor":"paper"} -->
<h2 class="wp-block-heading has-paper-color has-text-color has-large-font-size"><?php esc_html_e( 'Find your way', 'lumen' ); ?></h2>
<!-- /wp:heading -->

<!-- wp:list {"fontSize":"small"} -->
<ul class="wp-block-list has-small-font-size"><!-- wp:list-item -->
<li><a href="/search/"><?php esc_html_e( 'Search the school', 'lumen' ); ?></a></li>
<!-- /wp:list-item -->

<!-- wp:list-item -->
<li><a href="/agenda/?q=*&amp;tour=1"><?php esc_html_e( 'How it works', 'lumen' ); ?></a></li>
<!-- /wp:list-item --></ul>
<!-- /wp:list --></div>
<!-- /wp:column --></div>
<!-- /wp:columns --></footer>
<!-- /wp:group -->
