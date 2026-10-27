// Before the demo specs (D220): the demo storefront on its own WordPress (run-docker.cmd demo,
// D219), checked rather than built, and its catalog seeded only when it is missing, so a run
// does not pay for it again. The default site is never touched.
import { test } from '@playwright/test';
import { wp } from '../global-setup.mjs';

const demo = ( args ) => wp( args, 'cli-demo' );

test( 'the demo storefront is up, in English, with its catalog', async () => {
	test.setTimeout( 5 * 60 * 1000 );
	try {
		demo( [ 'core', 'is-installed' ] );
	} catch {
		throw new Error(
			'The demo WordPress is not installed: run `run-docker.cmd demo` first.'
		);
	}
	// The curated searches are English: a demo seeded in Portuguese (DEMO_LOCALE) answers others.
	const locale = demo( [ 'eval', 'echo get_locale();' ] ).trim();
	if ( ! locale.endsWith( 'en_US' ) ) {
		throw new Error(
			`The demo speaks ${ locale }: run \`run-docker.cmd demo\` with DEMO_LOCALE=en_US first.`
		);
	}
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
