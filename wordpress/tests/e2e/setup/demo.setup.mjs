// Before the demo specs (D220): the demo storefront on its own WordPress (run-docker.cmd demo,
// D219), checked rather than built, and its catalog seeded only when it is missing, so a run
// does not pay for it again. The default site is never touched.
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname } from 'node:path';
import { test } from '@playwright/test';
import { wp } from '../global-setup.mjs';
import { demoFile } from '../demo/curated.mjs';

const demo = ( args ) => wp( args, 'cli-demo' );

test( 'the demo storefront is up, with its catalog', async () => {
	test.setTimeout( 5 * 60 * 1000 );
	try {
		demo( [ 'core', 'is-installed' ] );
	} catch {
		throw new Error(
			'The demo WordPress is not installed: run `run-docker.cmd demo` first.'
		);
	}
	// The specs check the curated searches in the language the demo speaks (DEMO_LOCALE, D217).
	const locale = demo( [ 'eval', 'echo get_locale();' ] )
		.trim()
		.split( /\s+/ )
		.pop();
	mkdirSync( dirname( demoFile ), { recursive: true } );
	writeFileSync( demoFile, JSON.stringify( { locale } ) );
	const seeded = demo( [
		'eval',
		'echo function_exists( "wc_get_product_id_by_sku" ) && wc_get_product_id_by_sku( "TF-01-SLA" ) ? "yes" : "no";',
	] ).trim();
	if ( seeded.endsWith( 'yes' ) ) {
		return;
	}
	demo( [ 'plugin', 'activate', 'woocommerce' ] );
	demo( [ 'eval-file', '/demo/seed/catalog.php' ] );
	demo( [ 'dumont', 'reindex' ] );
	// Turing commits within a few seconds; the specs read what was just indexed.
	await new Promise( ( resolve ) => setTimeout( resolve, 5000 ) );
} );
