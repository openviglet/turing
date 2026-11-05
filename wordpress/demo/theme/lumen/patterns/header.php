<?php
/**
 * Title: Header
 * Slug: lumen/header
 * Inserter: no
 *
 * The notice bar, the brand and the menu, in the site's language.
 *
 * @package Lumen
 */

?>
<!-- wp:group {"align":"full","style":{"spacing":{"padding":{"top":"0.5rem","bottom":"0.5rem"}}},"backgroundColor":"slate","textColor":"paper","layout":{"type":"constrained","wideSize":"1160px"}} -->
<div class="wp-block-group alignfull has-paper-color has-slate-background-color has-text-color has-background" style="padding-top:0.5rem;padding-bottom:0.5rem"><!-- wp:paragraph {"align":"center","fontSize":"small"} -->
<p class="has-text-align-center has-small-font-size"><?php esc_html_e( 'Enrolment for next year is open · Visit us on an open day', 'lumen' ); ?></p>
<!-- /wp:paragraph --></div>
<!-- /wp:group -->

<!-- wp:group {"align":"full","style":{"spacing":{"padding":{"top":"1rem","bottom":"1rem"}},"border":{"bottom":{"color":"var:preset|color|mist","width":"1px"}}},"backgroundColor":"paper","layout":{"type":"constrained","wideSize":"1160px"}} -->
<div class="wp-block-group alignfull has-paper-background-color has-background" style="border-bottom-color:var(--wp--preset--color--mist);border-bottom-width:1px;padding-top:1rem;padding-bottom:1rem"><!-- wp:group {"align":"wide","className":"lumen-header-row","layout":{"type":"flex","flexWrap":"nowrap"}} -->
<div class="wp-block-group alignwide lumen-header-row"><!-- wp:group {"className":"lumen-brand","layout":{"type":"flex","flexWrap":"nowrap"}} -->
<div class="wp-block-group lumen-brand"><!-- wp:html -->
<svg width="34" height="34" viewBox="0 0 34 34" aria-hidden="true" focusable="false"><circle cx="17" cy="17" r="16" fill="#1E2A44"/><path d="M17 6l3 8h8l-6.5 5 2.5 8-7-5-7 5 2.5-8L6 14h8z" fill="#E3B04B"/></svg>
<!-- /wp:html -->

<!-- wp:site-title {"level":0} /--></div>
<!-- /wp:group -->

<!-- wp:navigation {"overlayMenu":"mobile","layout":{"type":"flex","justifyContent":"right"}} -->
<?php
foreach ( array(
	'/agenda/?q=*'   => __( 'Agenda', 'lumen' ),
	'/openings/?q=*' => __( 'Openings', 'lumen' ),
	'/library/?q=*'  => __( 'Library', 'lumen' ),
	'/search/'       => __( 'Search', 'lumen' ),
) as $lumen_url => $lumen_label ) {
	printf( "<!-- wp:navigation-link %s /-->\n", wp_json_encode( array( 'label' => $lumen_label, 'url' => $lumen_url ) ) );
}
?>
<!-- /wp:navigation --></div>
<!-- /wp:group --></div>
<!-- /wp:group -->
