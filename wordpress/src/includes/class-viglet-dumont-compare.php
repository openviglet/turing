<?php
/**
 * Product comparison (D200): up to four WooCommerce products side by side, in a table of what
 * they all have, with the rows where they differ marked.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Builds the comparison table and finds the page that shows it.
 */
class Viglet_Dumont_Compare {

	/** The most products compared at once. */
	public const MAX = 4;

	/** The query argument carrying the product ids, comma separated. */
	public const QUERY = 'compare';

	/**
	 * The products a request asks to compare: published, purchasable-or-not products only,
	 * each once, at most self::MAX, in the order asked.
	 *
	 * @param string $ids Comma-separated post ids, as the query argument carries them.
	 * @return int[]
	 */
	public static function ids( string $ids ): array {
		$found = array();
		foreach ( explode( ',', $ids ) as $id ) {
			$id = absint( $id );
			if ( $id && ! in_array( $id, $found, true ) && 'product' === get_post_type( $id ) && 'publish' === get_post_status( $id ) ) {
				$found[] = $id;
			}
			if ( count( $found ) >= self::MAX ) {
				break;
			}
		}

		return $found;
	}

	/**
	 * The comparison of these products: one column each, one row per thing they all have.
	 * A row whose values are not all the same is marked `differs`.
	 *
	 * @param int[] $ids Product ids, as ids() returns them.
	 * @return array{products: array<int, array{id: int, name: string, url: string, image: string, price: string}>, rows: array<int, array{label: string, values: string[], differs: bool}>}
	 */
	public static function table( array $ids ): array {
		$products = array_values( array_filter( array_map( 'wc_get_product', $ids ) ) );

		$columns = array_map(
			static fn ( WC_Product $product ) => array(
				'id'    => $product->get_id(),
				'name'  => $product->get_name(),
				'url'   => (string) get_permalink( $product->get_id() ),
				'image' => (string) wp_get_attachment_image_url( (int) $product->get_image_id(), 'woocommerce_thumbnail' ),
				'price' => wp_strip_all_tags( (string) wc_price( (float) $product->get_price() ) ),
			),
			$products
		);

		$statuses = wc_get_product_stock_status_options();
		$fixed    = array(
			__( 'Price', 'viglet-dumont' )        => static fn ( WC_Product $p ) => '' === (string) $p->get_price() ? '' : wp_strip_all_tags( (string) wc_price( (float) $p->get_price() ) ),
			__( 'Availability', 'viglet-dumont' ) => static fn ( WC_Product $p ) => (string) ( $statuses[ $p->get_stock_status() ] ?? $p->get_stock_status() ),
			__( 'Weight', 'viglet-dumont' )       => static fn ( WC_Product $p ) => $p->has_weight() ? wc_format_weight( (float) $p->get_weight() ) : '',
			__( 'Dimensions', 'viglet-dumont' )   => static fn ( WC_Product $p ) => $p->has_dimensions() ? wc_format_dimensions( $p->get_dimensions( false ) ) : '',
			__( 'Rating', 'viglet-dumont' )       => static fn ( WC_Product $p ) => $p->get_rating_count() > 0 ? number_format_i18n( (float) $p->get_average_rating(), 1 ) : '',
		);

		$rows = array();
		foreach ( $fixed as $label => $value ) {
			$rows[] = self::row( $label, array_map( $value, $products ) );
		}
		foreach ( self::attribute_labels( $products ) as $name => $label ) {
			$rows[] = self::row(
				$label,
				array_map( static fn ( WC_Product $p ) => wp_strip_all_tags( (string) $p->get_attribute( $name ) ), $products )
			);
		}

		return array(
			'products' => $columns,
			// Only what every product has: a row with a gap compares nothing.
			'rows'     => array_values( array_filter( $rows ) ),
		);
	}

	/**
	 * The page holding the compare block: the first published one, as the search page is
	 * found (Viglet_Dumont_Native_Search::search_page()).
	 */
	public static function page_url(): string {
		$found = get_posts(
			array(
				'post_type'   => 'page',
				'post_status' => 'publish',
				'numberposts' => 1,
				'fields'      => 'ids',
				'orderby'     => 'ID',
				'order'       => 'ASC',
				's'           => 'wp:viglet-dumont/compare',
			)
		);

		return $found ? (string) get_permalink( (int) $found[0] ) : '';
	}

	/**
	 * A row, or null when any product lacks a value for it.
	 *
	 * @param string   $label  What the row compares.
	 * @param string[] $values One per product.
	 * @return array{label: string, values: string[], differs: bool}|null
	 */
	private static function row( string $label, array $values ): ?array {
		$values = array_map( 'trim', $values );
		if ( ! $values || in_array( '', $values, true ) ) {
			return null;
		}

		return array(
			'label'   => $label,
			'values'  => $values,
			'differs' => count( array_unique( array_map( 'strtolower', $values ) ) ) > 1,
		);
	}

	/**
	 * The visible attributes of the first product, by name, with their labels: a row is kept
	 * only when every product has the attribute (row()).
	 *
	 * @param WC_Product[] $products The products.
	 * @return array<string, string>
	 */
	private static function attribute_labels( array $products ): array {
		if ( ! $products ) {
			return array();
		}
		$labels = array();
		foreach ( $products[0]->get_attributes() as $name => $attribute ) {
			if ( $attribute instanceof WC_Product_Attribute && $attribute->get_visible() ) {
				$labels[ (string) $name ] = wc_attribute_label( (string) $name, $products[0] );
			}
		}

		return $labels;
	}
}
