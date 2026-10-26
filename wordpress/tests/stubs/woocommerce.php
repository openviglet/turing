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
}

/**
 * @param mixed $the_product A product, its post or its id.
 * @return WC_Product|null|false
 */
function wc_get_product( $the_product = false ) { return null; }

/** @return array<string, string> */
function wc_get_product_stock_status_options(): array { return array(); }

/** @return array<int, object{attribute_name: string, attribute_label: string}> */
function wc_get_attribute_taxonomies(): array { return array(); }
