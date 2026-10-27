<?php
/**
 * What PHPStan needs to know of WooCommerce (D173): the few functions and classes the plugin
 * calls, always behind Viglet_Dumont_WooCommerce::active(). Not loaded by PHPUnit, whose
 * tests define them per case.
 *
 * @package Viglet_Dumont
 */

// phpcs:disable -- signatures of a third-party API, not plugin code.

class WooCommerce {}

class WC_Product {
	public function get_id(): int { return 0; }
	public function get_price(): string { return ''; }
	public function get_regular_price(): string { return ''; }
	public function get_sale_price(): string { return ''; }
	public function get_stock_status(): string { return ''; }
	public function get_sku(): string { return ''; }
	public function get_average_rating(): string { return ''; }
	// Product comparison (D200).
	public function get_name(): string { return ''; }
	public function get_image_id(): string { return ''; }
	public function has_weight(): bool { return false; }
	public function get_weight(): string { return ''; }
	public function has_dimensions(): bool { return false; }
	/** @return array<string, string>|string */
	public function get_dimensions( bool $formatted = true ) { return ''; }
	public function get_rating_count(): int { return 0; }
	public function get_attribute( string $attribute ): string { return ''; }
	/** @return array<string, WC_Product_Attribute|string> */
	public function get_attributes(): array { return array(); }
}

class WC_Product_Attribute {
	public function get_visible(): bool { return false; }
}

/** @param array<string, mixed> $args */
function wc_price( float $price, array $args = array() ): string { return ''; }

function wc_format_weight( float $weight ): string { return ''; }

/** @param array<string, string> $dimensions */
function wc_format_dimensions( array $dimensions ): string { return ''; }

function wc_attribute_label( string $name, ?WC_Product $product = null ): string { return ''; }

/**
 * @param mixed $the_product A product, its post or its id.
 * @return WC_Product|null|false
 */
function wc_get_product( $the_product = false ) { return null; }

/** @return array<string, string> */
function wc_get_product_stock_status_options(): array { return array(); }

/** @return array<int, object{attribute_name: string, attribute_label: string}> */
function wc_get_attribute_taxonomies(): array { return array(); }
