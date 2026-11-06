<?php
/**
 * Title: Front page
 * Slug: lumen/front-page
 * Inserter: no
 *
 * The home: the hero whose search box is the plugin's, the three ways in (agenda, openings,
 * library) and the school's news, in the site's language.
 *
 * @package Lumen
 */

$lumen_tiles = array(
	array( 'slate', '/agenda/?q=*', __( 'Agenda', 'lumen' ), __( 'Open days, debates, concerts and the fair: today, this week, this month.', 'lumen' ) ),
	array( 'sage', '/openings/?q=*', __( 'Openings', 'lumen' ), __( 'Jobs, enrolment calls and scholarships, each until its closing day.', 'lumen' ) ),
	array( 'rust', '/library/?q=*', __( 'Library', 'lumen' ), __( 'Books with a sample chapter you can search inside.', 'lumen' ) ),
);
?>
<!-- wp:group {"tagName":"main","style":{"spacing":{"blockGap":"0"}},"layout":{"type":"constrained","wideSize":"1160px"}} -->
<main class="wp-block-group"><!-- wp:group {"align":"full","className":"lumen-hero","style":{"spacing":{"padding":{"top":"var:preset|spacing|60","bottom":"var:preset|spacing|60"}},"color":{"gradient":"linear-gradient(160deg,#1E2A44 0%,#2F5D50 100%)"}},"textColor":"paper","layout":{"type":"constrained","contentSize":"760px"}} -->
<div class="wp-block-group alignfull lumen-hero has-paper-color has-text-color has-background" style="background:linear-gradient(160deg,#1E2A44 0%,#2F5D50 100%);padding-top:var(--wp--preset--spacing--60);padding-bottom:var(--wp--preset--spacing--60)"><!-- wp:heading {"level":1,"textColor":"paper"} -->
<h1 class="wp-block-heading has-paper-color has-text-color"><?php esc_html_e( 'A school that is easy to find your way around', 'lumen' ); ?></h1>
<!-- /wp:heading -->

<!-- wp:paragraph {"textColor":"mist","fontSize":"large"} -->
<p class="has-mist-color has-text-color has-large-font-size"><?php esc_html_e( 'Ask the way you would ask at the front desk: what is on this week, which calls are still open, which book talks about the tides.', 'lumen' ); ?></p>
<!-- /wp:paragraph -->

<!-- wp:viglet-turing/search <?php echo wp_json_encode( array( 'placeholder' => __( 'Debates in November, the science fair…', 'lumen' ) ) ); ?> -->
<!-- wp:viglet-turing/search-box /-->
<!-- /wp:viglet-turing/search --></div>
<!-- /wp:group -->

<!-- wp:group {"style":{"spacing":{"padding":{"top":"var:preset|spacing|50","bottom":"var:preset|spacing|50"}}},"layout":{"type":"constrained","wideSize":"1160px"}} -->
<div class="wp-block-group" style="padding-top:var(--wp--preset--spacing--50);padding-bottom:var(--wp--preset--spacing--50)"><!-- wp:heading {"align":"wide"} -->
<h2 class="wp-block-heading alignwide"><?php esc_html_e( 'Find your way in', 'lumen' ); ?></h2>
<!-- /wp:heading -->

<!-- wp:group {"align":"wide","layout":{"type":"grid","minimumColumnWidth":"14rem"}} -->
<div class="wp-block-group alignwide"><?php foreach ( $lumen_tiles as list( $lumen_colour, $lumen_url, $lumen_title, $lumen_text ) ) : ?><!-- wp:group {"className":"lumen-tile","style":{"spacing":{"padding":{"top":"var:preset|spacing|40","bottom":"var:preset|spacing|40","left":"var:preset|spacing|40","right":"var:preset|spacing|40"}},"border":{"radius":"6px"}},"backgroundColor":"<?php echo esc_attr( $lumen_colour ); ?>","textColor":"paper","layout":{"type":"default"}} -->
<div class="wp-block-group lumen-tile has-paper-color has-<?php echo esc_attr( $lumen_colour ); ?>-background-color has-text-color has-background" style="border-radius:6px;padding-top:var(--wp--preset--spacing--40);padding-right:var(--wp--preset--spacing--40);padding-bottom:var(--wp--preset--spacing--40);padding-left:var(--wp--preset--spacing--40)"><!-- wp:heading {"level":3,"textColor":"paper"} -->
<h3 class="wp-block-heading has-paper-color has-text-color"><a href="<?php echo esc_url( $lumen_url ); ?>"><?php echo esc_html( $lumen_title ); ?></a></h3>
<!-- /wp:heading -->

<!-- wp:paragraph {"fontSize":"small"} -->
<p class="has-small-font-size"><?php echo esc_html( $lumen_text ); ?></p>
<!-- /wp:paragraph --></div>
<!-- /wp:group --><?php endforeach; ?></div>
<!-- /wp:group --></div>
<!-- /wp:group -->

<!-- wp:group {"align":"full","style":{"spacing":{"padding":{"top":"var:preset|spacing|50","bottom":"var:preset|spacing|50"}}},"backgroundColor":"mist","layout":{"type":"constrained","wideSize":"1160px"}} -->
<div class="wp-block-group alignfull has-mist-background-color has-background" style="padding-top:var(--wp--preset--spacing--50);padding-bottom:var(--wp--preset--spacing--50)"><!-- wp:heading {"align":"wide"} -->
<h2 class="wp-block-heading alignwide"><?php esc_html_e( 'School news', 'lumen' ); ?></h2>
<!-- /wp:heading -->

<!-- wp:query {"queryId":1,"query":{"perPage":3,"postType":"post","order":"desc","orderBy":"date","inherit":false},"align":"wide","layout":{"type":"default"}} -->
<div class="wp-block-query alignwide"><!-- wp:post-template {"layout":{"type":"grid","columnCount":3,"minimumColumnWidth":"16rem"}} -->
<!-- wp:post-title {"level":3,"isLink":true} /-->

<!-- wp:post-date {"fontSize":"small","textColor":"graphite"} /-->

<!-- wp:post-excerpt {"excerptLength":24} /-->
<!-- /wp:post-template --></div>
<!-- /wp:query --></div>
<!-- /wp:group --></main>
<!-- /wp:group -->
