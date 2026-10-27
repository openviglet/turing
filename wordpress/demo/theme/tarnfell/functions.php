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

add_action(
	'after_setup_theme',
	static function (): void {
		add_editor_style( 'style.css' );
	}
);
