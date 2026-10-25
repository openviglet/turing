<?php
/**
 * Viglet Dumont Search Block
 *
 * The Turing search block's markup, in pieces (D185): the parent block draws them in its
 * default order, and its inner blocks (search box, filters, results, pages) each draw their
 * own, so a theme can reorder the parts or put its own blocks between them. Every piece
 * reads the one Interactivity store the parent declares.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Renders the pieces of the search block and keeps the id they share.
 */
class Viglet_Dumont_Search_Block {

	/** The inner blocks, each with the pieces it draws, in order. */
	public const PARTS = array(
		'box'     => array( 'box', 'correction' ),
		'filters' => array( 'applied', 'toggle', 'facets' ),
		'results' => array( 'toolbar', 'answer', 'main' ),
		'pages'   => array( 'pages' ),
	);

	/**
	 * What starts the context key of each parent attribute its inner blocks read: a block
	 * provides one attribute per key (`providesContext` in blocks/search/block.json).
	 */
	public const CONTEXT = 'viglet-dumont/search/';

	/**
	 * The id of the block being drawn, shared by its pieces.
	 *
	 * @var string
	 */
	private static string $id = '';

	/**
	 * The id the pieces of the block being drawn share: made by the first piece that asks,
	 * since WordPress draws inner blocks before their parent, and forgotten by done().
	 */
	public static function id(): string {
		if ( '' === self::$id ) {
			self::$id = wp_unique_id( 'viglet-dumont-search-' );
		}

		return self::$id;
	}

	/**
	 * The parent block is drawn: the next block makes its own id.
	 */
	public static function done(): void {
		self::$id = '';
	}

	/**
	 * The query the page was opened with (?q=, or WordPress's ?s=).
	 */
	public static function query(): string {
		// A public, read-only page parameter: nothing is changed, so there is no nonce to check.
		// phpcs:ignore WordPress.Security.NonceVerification.Recommended
		return sanitize_text_field( wp_unslash( $_GET['q'] ?? ( $_GET['s'] ?? '' ) ) );
	}

	/**
	 * The orders Turing offers, by value.
	 *
	 * @return array<string, string>
	 */
	public static function sorts(): array {
		return array(
			'relevance' => __( 'Relevance', 'viglet-dumont' ),
			'newest'    => __( 'Newest', 'viglet-dumont' ),
			'oldest'    => __( 'Oldest', 'viglet-dumont' ),
		);
	}

	/**
	 * The order the URL's `sort` picks (search-block.js reads the rest of the URL).
	 */
	public static function sort(): string {
		// phpcs:ignore WordPress.Security.NonceVerification.Recommended -- read-only, as above.
		$sort = sanitize_key( wp_unslash( $_GET['sort'] ?? '' ) );

		return isset( self::sorts()[ $sort ] ) ? $sort : 'relevance';
	}

	/**
	 * The parent's attributes as an inner block sees them through its context.
	 *
	 * @param array $context The inner block's context.
	 * @return array
	 */
	public static function attributes_from( array $context ): array {
		$attributes = array();
		foreach ( $context as $key => $value ) {
			if ( str_starts_with( (string) $key, self::CONTEXT ) ) {
				$attributes[ substr( (string) $key, strlen( self::CONTEXT ) ) ] = $value;
			}
		}

		return $attributes;
	}

	/**
	 * The markup of some pieces, in order.
	 *
	 * @param string[] $pieces     Piece names: files in blocks/search/parts/.
	 * @param array    $attributes The parent block's attributes.
	 */
	public static function pieces( array $pieces, array $attributes ): string {
		$html = '';
		foreach ( $pieces as $piece ) {
			$html .= self::piece( $piece, $attributes );
		}

		return $html;
	}

	/**
	 * One piece's markup.
	 *
	 * @param string $piece      A file in blocks/search/parts/, without `.php`.
	 * @param array  $attributes The parent block's attributes.
	 */
	private static function piece( string $piece, array $attributes ): string {
		$file = VIGLET_DUMONT_DIR . 'blocks/search/parts/' . sanitize_key( $piece ) . '.php';
		if ( ! file_exists( $file ) ) {
			return '';
		}
		$viglet_dumont_id   = self::id();
		$viglet_dumont_show = static fn ( string $part ): bool => (bool) ( $attributes[ $part ] ?? true );

		ob_start();
		include $file;

		return (string) ob_get_clean();
	}
}
