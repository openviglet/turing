<?php
/**
 * The fresh store (D281) after its catalog seed: every trace the plugin left in the database
 * removed, so the release zip installed on it in the developer video starts as it would on any
 * WordPress. Its settings, mapping, tuning and logs are options, its caches transients and its
 * jobs cron events; the plugin creates no table.
 *
 * Run by `run-docker.cmd fresh` with WP-CLI, after `wp plugin deactivate viglet-dumont`:
 *
 *   wp eval-file /demo/seed/forget-plugin.php
 *
 * @package Viglet_Dumont
 */

global $wpdb;

$deleted = $wpdb->query( // phpcs:ignore WordPress.DB.DirectDatabaseQuery
	$wpdb->prepare(
		"DELETE FROM {$wpdb->options} WHERE option_name LIKE %s OR option_name LIKE %s OR option_name LIKE %s",
		$wpdb->esc_like( 'viglet_dumont' ) . '%',
		$wpdb->esc_like( '_transient_viglet_dumont' ) . '%',
		$wpdb->esc_like( '_transient_timeout_viglet_dumont' ) . '%'
	)
);

$hooks = array();
foreach ( (array) _get_cron_array() as $events ) {
	foreach ( array_keys( (array) $events ) as $hook ) {
		if ( str_starts_with( $hook, 'viglet_dumont' ) ) {
			$hooks[ $hook ] = true;
		}
	}
}
foreach ( array_keys( $hooks ) as $hook ) {
	wp_clear_scheduled_hook( $hook );
}

echo wp_json_encode(
	array(
		'options' => (int) $deleted,
		'cron'    => array_keys( $hooks ),
	)
);
