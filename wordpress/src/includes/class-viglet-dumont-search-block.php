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
		'box'     => array( 'box', 'correction', 'trending' ),
		'filters' => array( 'applied', 'suggested', 'toggle', 'facets' ),
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
	 * The block's fixed filters (D306), as `field:value` filter queries: the `filters`
	 * attribute with anything that is not one dropped. Every search the block makes stays
	 * inside them, and a visitor never sees them as a filter to remove.
	 *
	 * @param mixed $filters The attribute as stored.
	 * @return array<int, string>
	 */
	public static function fixed_filters( $filters ): array {
		$fixed = array();
		foreach ( is_array( $filters ) ? $filters : array() as $filter ) {
			$filter = trim( sanitize_text_field( (string) $filter ) );
			if ( preg_match( '/^\w+:\S.*$/', $filter ) && ! in_array( $filter, $fixed, true ) ) {
				$fixed[] = $filter;
			}
		}

		return $fixed;
	}

	/**
	 * The scope the block searches inside (D306): its own fixed filters, then the scope a
	 * box-only block carried here in the URL (`scope[]`), never as a plain `fq[]`, so it shows
	 * as the context rather than as a filter to remove.
	 *
	 * @param array $attributes The search block's attributes.
	 * @return array<int, string>
	 */
	public static function scope( array $attributes ): array {
		// phpcs:ignore WordPress.Security.NonceVerification.Recommended -- a read-only search.
		$carried = isset( $_GET['scope'] ) ? array_map( 'sanitize_text_field', (array) wp_unslash( $_GET['scope'] ) ) : array();

		return self::fixed_filters( array_merge( (array) ( $attributes['filters'] ?? array() ), $carried ) );
	}

	/**
	 * The scope as a reader names it: a post type by its own name, a facet value as it is.
	 *
	 * @param array<int, string> $scope scope().
	 */
	public static function scope_label( array $scope ): string {
		$names = array();
		foreach ( $scope as $filter ) {
			[ $field, $value ] = explode( ':', $filter, 2 );
			$type              = 'type' === $field ? get_post_type_object( $value ) : null;
			$names[]           = $type ? (string) $type->labels->name : $value;
		}

		return implode( ', ', $names );
	}

	/**
	 * Where "Search the whole site" leads from a scoped block (D306): the site's search page,
	 * with no scope; '' when that is this very page and the scope is the block's own, since
	 * the link would land on the same scoped search.
	 *
	 * @param array $attributes The search block's attributes.
	 */
	public static function whole_site_url( array $attributes ): string {
		$target = ( new Viglet_Dumont_Native_Search() )->search_page();
		$here   = untrailingslashit( (string) get_permalink() ) === untrailingslashit( $target['searchPage'] );
		if ( $here && array() !== self::fixed_filters( $attributes['filters'] ?? array() ) ) {
			return '';
		}

		return remove_query_arg( 'scope', $target['searchPage'] );
	}

	/**
	 * The popular-search chips in a block's drawn parts, pointed at the page its searches go
	 * to: a box without results of its own sends its query to the search page (D208), and its
	 * chips, drawn before the block knew that, linked to the page they sat on.
	 *
	 * @param string $html       The block's inner blocks, drawn.
	 * @param string $page       The search page.
	 * @param string $query_arg  The parameter it reads the query from.
	 * @return string The same HTML, with each chip's link on that page.
	 */
	public static function chips_to( string $html, string $page, string $query_arg ): string {
		$tags = new WP_HTML_Tag_Processor( $html );
		while ( $tags->next_tag(
			array(
				'tag_name'   => 'A',
				'class_name' => 'viglet-dumont-search__chip',
			)
		) ) {
			$args = wp_parse_args( (string) wp_parse_url( (string) $tags->get_attribute( 'href' ), PHP_URL_QUERY ) );
			if ( ! isset( $args['q'] ) ) {
				continue;
			}
			$tags->set_attribute( 'href', add_query_arg( $query_arg, rawurlencode( (string) $args['q'] ), $page ) );
		}

		return $tags->get_updated_html();
	}

	/**
	 * The query the page was opened with (?q=, or WordPress's ?s=).
	 */
	public static function query(): string {
		// phpcs:ignore WordPress.Security.NonceVerification.Recommended -- a public, read-only page parameter.
		return sanitize_text_field( wp_unslash( $_GET['q'] ?? ( $_GET['s'] ?? '' ) ) );
	}

	/**
	 * The orders Turing offers, by value.
	 *
	 * @return array<string, string>
	 */
	public static function sorts(): array {
		return array(
			'relevance' => __( 'Relevance', 'viglet-turing-search' ),
			'newest'    => __( 'Newest', 'viglet-turing-search' ),
			'oldest'    => __( 'Oldest', 'viglet-turing-search' ),
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
	 * The field a WooCommerce product's price is indexed under, and the name a filter on it
	 * shows (D199): what a price a sentence names ("under R$400") filters, read in code. Null
	 * when no mapping sends the shop's price.
	 *
	 * @return array{field: string, label: string}|null
	 */
	public static function price_field(): ?array {
		foreach ( ( new Viglet_Dumont_Mapping( viglet_dumont_get_settings() ) )->all() as $type_mapping ) {
			foreach ( $type_mapping['fields'] as $field ) {
				if ( 'woo:price' === $field['source'] ) {
					return array(
						'field' => $field['field'],
						'label' => '' !== $field['facet_label'] ? $field['facet_label'] : __( 'Price', 'viglet-turing-search' ),
					);
				}
			}
		}

		return null;
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
	 * Draw some pieces, in order. Each piece escapes its own output where it prints it, so
	 * nothing here echoes markup it holds in a variable.
	 *
	 * @param string[] $pieces     Piece names: files in blocks/search/parts/.
	 * @param array    $attributes The parent block's attributes.
	 */
	public static function render( array $pieces, array $attributes ): void {
		foreach ( $pieces as $piece ) {
			self::piece( $piece, $attributes );
		}
	}

	/**
	 * Draw one piece.
	 *
	 * @param string $piece      A file in blocks/search/parts/, without `.php`.
	 * @param array  $attributes The parent block's attributes.
	 */
	private static function piece( string $piece, array $attributes ): void {
		$file = VIGLET_DUMONT_DIR . 'blocks/search/parts/' . sanitize_key( $piece ) . '.php';
		if ( ! file_exists( $file ) ) {
			return;
		}
		$viglet_dumont_id   = self::id();
		$viglet_dumont_show = static fn ( string $part ): bool => (bool) ( $attributes[ $part ] ?? true );

		include $file;
	}
}
