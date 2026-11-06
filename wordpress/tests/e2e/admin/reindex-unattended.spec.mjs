// A started reindex finishes with nobody on the site (D335): no visitor, no Health page, and
// WP-Cron's runner stopped. Each step asks the site for the next one over a loopback. In this
// stack the site's own address (localhost:8080) does not reach WordPress from inside its
// container, so a must-use plugin points the loopback at the compose service, as the
// `viglet_turing_reindex_step_url` filter is there for.
import { execFileSync } from 'node:child_process';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { expect, test } from '../fixtures.mjs';
import { setSetting, wp } from '../global-setup.mjs';

const root = join(
	dirname( fileURLToPath( import.meta.url ) ),
	'..',
	'..',
	'..'
);
const muPlugin = 'wp-content/mu-plugins/viglet-turing-e2e-loopback.php';

/**
 * Run `docker compose` in the plugin's folder.
 *
 * @param {string[]} args The compose arguments.
 */
const compose = ( args ) =>
	execFileSync( 'docker', [ 'compose', ...args ], {
		cwd: root,
		stdio: 'ignore',
	} );

/**
 * The last line WP-CLI printed, so a notice before it is no answer.
 *
 * @param {string[]} args The wp arguments.
 */
const ask = ( args ) => wp( args ).trim().split( /\r?\n/ ).pop().trim();

test.beforeAll( () => {
	// Nothing but the loopback may move the run.
	compose( [ 'stop', 'cron' ] );
	wp( [
		'eval',
		`wp_mkdir_p( WPMU_PLUGIN_DIR ); file_put_contents( ABSPATH . ${ JSON.stringify(
			muPlugin
		) }, "<?php add_filter( 'viglet_turing_reindex_step_url', static fn ( \\$url ) => str_replace( untrailingslashit( home_url() ), 'http://wordpress', \\$url ) );\\n" );`,
	] );
	setSetting( 'batch_size', 2 );
} );

test.afterAll( () => {
	setSetting( 'batch_size', null );
	wp( [ 'eval', `@unlink( ABSPATH . ${ JSON.stringify( muPlugin ) } );` ] );
	compose( [ 'start', 'cron' ] );
} );

test( 'a started reindex finishes with no visitor, no Health page and WP-Cron stopped', async () => {
	test.setTimeout( 240_000 );
	// Started as Settings does; the first step is asked for at once.
	expect(
		ask( [
			'eval',
			'$r = ( new Viglet_Turing_Reindex() )->start(); echo is_wp_error( $r ) ? $r->get_error_message() : $r["state"];',
		] )
	).toBe( 'running' );

	await expect
		.poll(
			() =>
				ask( [
					'eval',
					'$r = ( new Viglet_Turing_Reindex() )->report(); echo $r["running"] ? "running" : ( $r["last"]["state"] ?? "none" );',
				] ),
			{ timeout: 180_000, intervals: [ 5_000 ] }
		)
		.toBe( 'done' );
	const last = JSON.parse(
		ask( [
			'eval',
			'echo wp_json_encode( ( new Viglet_Turing_Reindex() )->report()["last"] );',
		] )
	);
	expect( last.sent ).toBe( last.total );
	expect( last.failed ).toBe( 0 );
} );
