<?php
/**
 * The part of core's WP_Error the plugin reads.
 *
 * @package Viglet_Dumont
 */

// phpcs:disable -- a stand-in for a core class, shaped like core rather than like this plugin.

class WP_Error {
	private $message;

	public function __construct( $code = '', $message = '' ) {
		$this->message = $message;
	}

	public function get_error_message() {
		return $this->message;
	}
}
