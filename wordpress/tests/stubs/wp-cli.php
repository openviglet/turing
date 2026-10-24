<?php
/**
 * The slice of WP-CLI the plugin calls, declared for static analysis only.
 *
 * @package Viglet_Dumont
 */

// phpcs:disable -- a stand-in for WP-CLI's own classes, shaped like WP-CLI rather than like this plugin.

namespace {
	class WP_CLI {
		public static function add_command( string $name, $callable, array $args = array() ): bool {
			return true;
		}

		public static function log( string $message ): void {}

		public static function success( string $message ): void {}

		public static function warning( string $message ): void {}

		/**
		 * @return never
		 */
		public static function error( string $message ) {
			exit( 1 );
		}
	}
}

namespace WP_CLI\Utils {
	function format_items( string $format, array $items, array $fields ): void {}
}
