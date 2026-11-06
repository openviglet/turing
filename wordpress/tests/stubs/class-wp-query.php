<?php
/**
 * Core's WP_Query, reduced to what the plugin's `fields => ids` queries read: the ids a test
 * sets in WP_Query::$ids, narrowed by post__in. The arguments of the last query are kept.
 *
 * @package Viglet_Dumont
 */

// phpcs:disable -- a stand-in for a core class, shaped like core rather than like this plugin.

final class WP_Query {
	/** @var int[] What every query answers, before post__in. */
	public static $ids = array();

	/** @var array The arguments of the last query. */
	public static $last_args = array();

	public $posts = array();

	public $found_posts = 0;

	public function __construct( array $args = array() ) {
		self::$last_args   = $args;
		$ids               = self::$ids;
		if ( isset( $args['post__in'] ) ) {
			$ids = array_values( array_intersect( $ids, $args['post__in'] ) );
		}
		$this->posts       = $ids;
		$this->found_posts = count( $ids );
	}
}
