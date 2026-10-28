<?php
/**
 * Title: Header
 * Slug: tarnfell/header
 * Inserter: no
 *
 * The announcement bar, the brand and the menu, in the site's language (D223).
 *
 * @package Tarnfell
 */

?>
<!-- wp:group {"align":"full","tagName":"div","style":{"spacing":{"padding":{"top":"0.5rem","bottom":"0.5rem"}}},"backgroundColor":"pine","textColor":"lichen","layout":{"type":"constrained","wideSize":"1200px"}} -->
<div class="wp-block-group alignfull has-lichen-color has-pine-background-color has-text-color has-background" style="padding-top:0.5rem;padding-bottom:0.5rem"><!-- wp:paragraph {"align":"center","fontSize":"small"} -->
<p class="has-text-align-center has-small-font-size"><?php esc_html_e( 'Free returns for 60 days · Repairs on every jacket we sell', 'tarnfell' ); ?></p>
<!-- /wp:paragraph --></div>
<!-- /wp:group -->

<!-- wp:group {"align":"full","tagName":"div","style":{"spacing":{"padding":{"top":"1rem","bottom":"1rem"}},"border":{"bottom":{"color":"var:preset|color|lichen","width":"1px"}}},"backgroundColor":"paper","layout":{"type":"constrained","wideSize":"1200px"}} -->
<div class="wp-block-group alignfull has-paper-background-color has-background" style="border-bottom-color:var(--wp--preset--color--lichen);border-bottom-width:1px;padding-top:1rem;padding-bottom:1rem"><!-- wp:group {"align":"wide","className":"tarnfell-header-row","layout":{"type":"flex","flexWrap":"nowrap"}} -->
<div class="wp-block-group alignwide tarnfell-header-row"><!-- wp:pattern {"slug":"tarnfell/brand"} /-->

<!-- wp:navigation {"overlayMenu":"mobile","layout":{"type":"flex","justifyContent":"right"}} -->
<?php
foreach ( array(
	'/shop/'             => __( 'Shop', 'tarnfell' ),
	'/search-gear/'      => __( 'Search', 'tarnfell' ),
	'/journal/'          => __( 'Journal', 'tarnfell' ),
	'/compare-searches/' => __( 'Compare', 'tarnfell' ),
) as $tarnfell_url => $tarnfell_label ) {
	printf( "<!-- wp:navigation-link %s /-->\n", wp_json_encode( array( 'label' => $tarnfell_label, 'url' => $tarnfell_url ) ) );
}
?>
<!-- /wp:navigation --></div>
<!-- /wp:group --></div>
<!-- /wp:group -->
