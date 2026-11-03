<?php
/**
 * The translations a copy installed from GitHub or Composer carries in languages/: such a site
 * gets no language pack from WordPress.org. The WordPress.org package leaves this file and the
 * compiled translations out (bin/build-zip.sh --wporg), its language packs coming from
 * translate.wordpress.org.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Load the plugin's own translations, before anything on init reads a string.
 */
function viglet_dumont_load_bundled_translations(): void {
	load_plugin_textdomain( 'viglet-turing-search', false, dirname( plugin_basename( VIGLET_DUMONT_FILE ) ) . '/languages' ); // phpcs:ignore PluginCheck.CodeAnalysis.DiscouragedFunctions.load_plugin_textdomainFound
}
add_action( 'init', 'viglet_dumont_load_bundled_translations', 0 );
