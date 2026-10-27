<?php
/**
 * Tarnfell: the demo storefront theme for the Viglet Dumont plugin (D193).
 *
 * @package Tarnfell
 */

/**
 * What theme.json cannot say (style.css), on the site and in the editor.
 */
function tarnfell_styles(): void {
	wp_enqueue_style( 'tarnfell', get_stylesheet_uri(), array(), wp_get_theme()->get( 'Version' ) );
}
add_action( 'wp_enqueue_scripts', 'tarnfell_styles' );

/**
 * The faces the first screen is set in, asked for before the stylesheet names them: a font
 * that swaps in after the first paint moves the search results under the visitor's eye.
 */
function tarnfell_preload_fonts(): void {
	foreach ( array( 'source-sans-3-latin-wght-normal', 'barlow-condensed-latin-700-normal', 'barlow-condensed-latin-600-normal' ) as $font ) {
		printf(
			'<link rel="preload" href="%s" as="font" type="font/woff2" crossorigin>' . "\n",
			esc_url( get_theme_file_uri( "assets/fonts/{$font}.woff2" ) )
		);
	}
}
add_action( 'wp_head', 'tarnfell_preload_fonts', 1 );

add_action(
	'after_setup_theme',
	static function (): void {
		add_editor_style( 'style.css' );
	}
);

// The "Compare searches" page: WordPress's own search beside Turing's (D195).
require get_theme_file_path( 'inc/compare.php' );

/**
 * "How it works" (D195): the tour's markers and notes, on a page opened with ?tour=1 only, so
 * no other visit pays for its script.
 */
function tarnfell_tour(): void {
	// A read-only page parameter: nothing is changed, so there is no nonce to check.
	// phpcs:ignore WordPress.Security.NonceVerification.Recommended
	if ( empty( $_GET['tour'] ) ) {
		return;
	}
	wp_enqueue_script( 'tarnfell-tour', get_theme_file_uri( 'assets/tour.js' ), array(), wp_get_theme()->get( 'Version' ), array( 'in_footer' => true ) );
}
add_action( 'wp_enqueue_scripts', 'tarnfell_tour' );
