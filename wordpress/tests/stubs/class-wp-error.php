<?php
/**
 * The part of core's WP_Error the plugin reads.
 *
 * @package Viglet_Dumont
 */

// phpcs:disable -- a stand-in for a core class, shaped like core rather than like this plugin.

class WP_Error {
	private $code;
	private $message;

	public function __construct( $code = '', $message = '' ) {
		$this->code    = $code;
		$this->message = $message;
	}

	public function get_error_code() {
		return $this->code;
	}

	public function get_error_message() {
		return $this->message;
	}
}
