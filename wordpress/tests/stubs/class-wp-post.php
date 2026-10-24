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
	public $post_excerpt = '';
	public $post_name    = '';
	public $menu_order   = 0;
	public $post_parent  = 0;

	public function __construct( array $fields = array() ) {
		foreach ( $fields as $name => $value ) {
			$this->$name = $value;
		}
	}
}
