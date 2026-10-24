<?php
/**
 * The fields of core's WP_Post the plugin reads.
 *
 * @package Viglet_Dumont
 */

// phpcs:disable -- a stand-in for a core class, shaped like core rather than like this plugin.

final class WP_Post {
	public $ID           = 0;
	public $post_author  = '0';
	public $post_title   = '';
	public $post_content = '';
	public $post_status  = 'publish';
	public $post_type    = 'post';

	public function __construct( array $fields = array() ) {
		foreach ( $fields as $name => $value ) {
			$this->$name = $value;
		}
	}
}
