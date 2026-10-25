// Before the specs: seed the stack's data (tests/e2e/fixtures/seed.php), reindex it into the
// local Turing, and log in once so the admin specs reuse the session.
import { execFileSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { chromium } from '@playwright/test';

const root = join( dirname( fileURLToPath( import.meta.url ) ), '..', '..' );
export const seedFile = join( root, 'tests', 'e2e', '.auth', 'seed.json' );

/**
 * Run a WP-CLI command in the stack's cli container and return its output.
 *
 * @param {string[]} args The wp arguments.
 */
export function wp( args ) {
	return execFileSync(
		'docker',
		[ 'compose', 'run', '--rm', '--no-deps', 'cli', 'wp', ...args ],
		{
			cwd: root,
			encoding: 'utf8',
			stdio: [ 'ignore', 'pipe', 'pipe' ],
		}
	);
}

export default async function globalSetup( config ) {
	const { baseURL, channel, storageState } = config.projects.find(
		( p ) => p.name === 'admin'
	).use;
	mkdirSync( dirname( seedFile ), { recursive: true } );

	if ( ! process.env.E2E_SKIP_SEED ) {
		const out = wp( [ 'eval-file', '/e2e/seed.php' ] );
		const seed = JSON.parse( out.slice( out.indexOf( '{' ) ) );
		writeFileSync( seedFile, JSON.stringify( seed ) );
		wp( [ 'dumont', 'reindex' ] );
		// Turing commits within a few seconds; the search specs read what was just indexed.
		await new Promise( ( resolve ) => setTimeout( resolve, 5000 ) );
	}

	const browser = await chromium.launch( { channel } );
	const page = await browser.newPage( { baseURL } );
	await page.goto( '/wp-login.php' );
	await page.fill( '#user_login', process.env.WP_USER || 'admin' );
	await page.fill( '#user_pass', process.env.WP_PASSWORD || 'admin' );
	await Promise.all( [
		page.waitForURL( /wp-admin/ ),
		page.click( '#wp-submit' ),
	] );
	// A pending database upgrade would put every admin page behind upgrade.php.
	if ( page.url().includes( 'upgrade.php' ) ) {
		throw new Error(
			'WordPress wants a database upgrade: run `run-docker.cmd wp core update-db`.'
		);
	}
	await page.context().storageState( { path: storageState } );
	await browser.close();
}
