<?php
/**
 * Lumen: the demo school theme for the Viglet Turing Search plugin (D325). A school's site is
 * not a store: its content is an agenda, openings that close on a day, and a library of books
 * whose words live in PDFs. The theme registers those three types, so the school's content is
 * the theme's and the plugin sees ordinary custom post types, as on any school's site.
 *
 * @package Lumen
 */

/**
 * What theme.json cannot say (style.css), on the site and in the editor.
 */
function lumen_styles(): void {
	wp_enqueue_style( 'lumen', get_stylesheet_uri(), array(), wp_get_theme()->get( 'Version' ) );
}
add_action( 'wp_enqueue_scripts', 'lumen_styles' );

add_action(
	'after_setup_theme',
	static function (): void {
		add_editor_style( 'style.css' );
		add_theme_support( 'post-thumbnails' );
		// The theme in the site's language: languages/<locale>.l10n.php.
		load_theme_textdomain( 'lumen', get_theme_file_path( 'languages' ) );
	}
);

/**
 * The school's three types and the meta they carry. Each meta is in the REST API, so the
 * single templates show it through block bindings and the plugin's mapping screen lists it.
 */
function lumen_register_types(): void {
	$types = array(
		'event'   => array(
			'labels' => array(
				'name'          => __( 'Events', 'lumen' ),
				'singular_name' => __( 'Event', 'lumen' ),
			),
			'slug'   => _x( 'event', 'URL slug', 'lumen' ),
			'icon'   => 'dashicons-calendar-alt',
			// The day it happens (yyyy-mm-dd), where, and its poster's address: the card's image.
			'meta'   => array( 'event_day', 'venue', 'poster' ),
		),
		'opening' => array(
			'labels' => array(
				'name'          => __( 'Openings', 'lumen' ),
				'singular_name' => __( 'Opening', 'lumen' ),
			),
			'slug'   => _x( 'opening', 'URL slug', 'lumen' ),
			'icon'   => 'dashicons-megaphone',
			// The last day to apply (yyyy-mm-dd): the opening leaves search after it.
			'meta'   => array( 'closes_on', 'opening_kind' ),
		),
		'book'    => array(
			'labels' => array(
				'name'          => __( 'Books', 'lumen' ),
				'singular_name' => __( 'Book', 'lumen' ),
			),
			'slug'   => _x( 'book', 'URL slug', 'lumen' ),
			'icon'   => 'dashicons-book',
			// Who wrote it, its shelf, and its sample chapter: a PDF attachment's id.
			'meta'   => array( 'author_name', 'shelf', 'sample' ),
		),
	);
	foreach ( $types as $type => $args ) {
		register_post_type(
			$type,
			array(
				'labels'       => $args['labels'],
				'public'       => true,
				'show_in_rest' => true,
				'has_archive'  => false,
				'menu_icon'    => $args['icon'],
				'rewrite'      => array( 'slug' => $args['slug'] ),
				'supports'     => array( 'title', 'editor', 'excerpt', 'thumbnail', 'custom-fields' ),
			)
		);
		foreach ( $args['meta'] as $key ) {
			register_post_meta(
				$type,
				$key,
				array(
					'type'         => 'string',
					'single'       => true,
					'show_in_rest' => true,
				)
			);
		}
	}
}
add_action( 'init', 'lumen_register_types' );

/**
 * The text a template keeps in block attributes (a heading, a placeholder, a prefix), in the
 * site's language: the patterns print their own text through __(), these are the strings the
 * HTML templates cannot. Only a string the theme's translations hold changes.
 *
 * @param array $block The parsed block about to render.
 * @return array
 */
function lumen_translate_attributes( array $block ): array {
	foreach ( array( 'heading', 'placeholder', 'prefix' ) as $attribute ) {
		if ( isset( $block['attrs'][ $attribute ] ) && is_string( $block['attrs'][ $attribute ] ) ) {
			// phpcs:ignore WordPress.WP.I18n.NonSingularStringLiteralText -- the templates' own strings, translated in languages/.
			$block['attrs'][ $attribute ] = translate( $block['attrs'][ $attribute ], 'lumen' );
		}
	}

	return $block;
}
add_filter( 'render_block_data', 'lumen_translate_attributes' );

/**
 * An event's day as the site writes dates, for its page: the meta keeps yyyy-mm-dd, which
 * reads as a database's, not a school's. Bound to a paragraph by the single template.
 *
 * @param mixed $value The meta value.
 * @param array $args  The binding's arguments.
 * @return mixed
 */
function lumen_day_binding( $value, array $args ) {
	$key = $args['key'] ?? '';
	if ( in_array( $key, array( 'event_day', 'closes_on' ), true ) && is_string( $value ) && '' !== $value ) {
		$at = strtotime( $value . ' 12:00:00' );
		return false === $at ? $value : wp_date( get_option( 'date_format' ), $at );
	}

	return $value;
}
add_filter(
	'block_bindings_source_value',
	static function ( $value, string $source, array $args ) {
		return 'core/post-meta' === $source ? lumen_day_binding( $value, $args ) : $value;
	},
	10,
	3
);

/**
 * "How it works": the tour's markers and notes, on a page opened with ?tour=1 only, so no
 * other visit pays for its script.
 */
function lumen_tour(): void {
	// A read-only page parameter: nothing is changed, so there is no nonce to check.
	// phpcs:ignore WordPress.Security.NonceVerification.Recommended
	if ( empty( $_GET['tour'] ) ) {
		return;
	}
	wp_enqueue_script( 'lumen-tour', get_theme_file_uri( 'assets/tour.js' ), array(), wp_get_theme()->get( 'Version' ), array( 'in_footer' => true ) );
}
add_action( 'wp_enqueue_scripts', 'lumen_tour' );
