<?php
/**
 * PHPUnit bootstrap: the plugin's classes against Brain Monkey, with no WordPress install.
 *
 * @package Viglet_Dumont
 */

define( 'ABSPATH', __DIR__ . '/' );

require_once __DIR__ . '/../vendor/autoload.php';
require_once __DIR__ . '/phpstan-constants.php';
require_once __DIR__ . '/stubs/class-wp-post.php';
require_once __DIR__ . '/stubs/class-wp-error.php';
