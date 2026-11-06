// Before the demo specs (D220): the demo storefront on its own WordPress (run-docker.cmd demo,
// D219), checked rather than built, and its catalog seeded only when it is missing, so a run
// does not pay for it again. The default site is never touched.
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname } from 'node:path';
import { test } from '@playwright/test';
import { assertSameSite, wp } from '../global-setup.mjs';
import { demoFile } from '../demo/curated.mjs';
import { demoCli, demoStore, demoURL } from '../../../playwright.config.mjs';

const demo = ( args ) => wp( args, demoCli );

test( 'the demo storefront is up, with its catalog', async () => {
	test.setTimeout( 5 * 60 * 1000 );
	try {
		demo( [ 'core', 'is-installed' ] );
	} catch {
		throw new Error(
			'The demo WordPress is not installed: run `run-docker.cmd demo` first.'
		);
	}
	assertSameSite( demoURL, demoCli, 'WP_DEMO_URL' );
	// The specs check the curated searches in the language the demo speaks (DEMO_LOCALE, D217).
	const locale = demo( [ 'eval', 'echo get_locale();' ] )
		.trim()
		.split( /\s+/ )
		.pop();
	mkdirSync( dirname( demoFile ), { recursive: true } );
	writeFileSync( demoFile, JSON.stringify( { locale } ) );
	// What says the store is seeded, and the seed, are its descriptor's (demo/sites, D325).
	const seeded = demo( [
		'eval',
		`echo ( ${ demoStore.seeded } ) ? "yes" : "no";`,
	] ).trim();
	if ( seeded.endsWith( 'yes' ) ) {
		return;
	}
	demo( [ 'plugin', 'activate', 'woocommerce' ] );
	demo( [ 'eval-file', demoStore.seed ] );
	// The shop's addresses in the demo's language (D266), set after WooCommerce registered them.
	demo( [ 'rewrite', 'flush', '--hard' ] );
	demo( [ 'turing', 'reindex' ] );
	// Turing commits within a few seconds; the specs read what was just indexed.
	await new Promise( ( resolve ) => setTimeout( resolve, 5000 ) );
} );
