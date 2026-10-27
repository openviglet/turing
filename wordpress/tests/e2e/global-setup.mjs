// Before the specs: seed the stack's data (tests/e2e/fixtures/seed.php), reindex it into the
// local Turing, and log in once so the admin specs reuse the session.
import { execFileSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { chromium } from '@playwright/test';

const root = join( dirname( fileURLToPath( import.meta.url ) ), '..', '..' );
export const seedFile = join( root, 'tests', 'e2e', '.auth', 'seed.json' );
export const networkSeedFile = join(
	root,
	'tests',
	'e2e',
	'.auth',
	'network.json'
);
// The multisite network's two sites (run-docker.cmd multisite), when E2E_MULTISITE is set.
export const networkURL = process.env.WP_MS_URL || 'http://localhost:8081';
export const networkSites = [ `${ networkURL }/`, `${ networkURL }/second/` ];

/**
 * Run a WP-CLI command in the stack's cli container and return its output.
 *
 * @param {string[]} args    The wp arguments.
 * @param {string}   service The compose service: cli, or cli-ms for the network.
 */
export function wp( args, service = 'cli' ) {
	return execFileSync(
		'docker',
		[ 'compose', 'run', '--rm', '--no-deps', service, 'wp', ...args ],
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

	// The variant every run starts from, even one that skips the seed: a run stopped half-way
	// through the theme and language projects (setup/switch.setup.mjs) leaves another.
	wp( [ 'theme', 'activate', 'twentytwentyfive' ] );
	wp( [ 'site', 'switch-language', 'en_US' ] );
	// The woo project's shop (D173). Its pages outlive the plugin, so they are drafted: in the
	// theme's page list they would move every screenshot, and indexed they would be counted.
	try {
		wp( [ 'plugin', 'deactivate', 'woocommerce' ] );
	} catch {}
	const shopPages = wp( [
		'post',
		'list',
		'--post_type=page',
		'--post_status=publish',
		// And the demo storefront's (run-docker.cmd demo, D193).
		'--post_name__in=shop,cart,checkout,my-account,ask-the-shop,compare-products,home-demo,search-gear,journal',
		'--format=ids',
	] ).trim();
	if ( shopPages ) {
		wp( [
			'post',
			'update',
			...shopPages.split( /\s+/ ),
			'--post_status=draft',
		] );
	}

	// The demo's trail journal (demo/seed/journal.php, D194): searched it would be counted, and
	// listed it would fill the post list. The demo's seed writes it again.
	const journal = wp( [
		'post',
		'list',
		'--post_type=post',
		'--post_status=any',
		'--category_name=buying-guides,how-tos',
		'--format=ids',
	] ).trim();
	if ( journal ) {
		wp( [ 'post', 'delete', ...journal.split( /\s+/ ), '--force' ] );
		// Their deletions sent now, not by the cron worker after the specs have started. A
		// held lock means that worker is sending them already.
		try {
			wp( [ 'dumont', 'queue', 'run' ] );
		} catch {}
	}
	// And the synonyms it set in Turing, which the Search tuning spec would list beside its own.
	wp( [
		'eval',
		'$t = new Viglet_Dumont_Tuning(); $all = $t->all();' +
			' foreach ( is_wp_error( $all ) ? array() : $all["synonyms"] as $row ) { if ( $row["ours"] ) { $t->remove_synonym( $row["id"] ); } }',
	] );

	// What the demo storefront changes besides its pages: permalinks, the front page, the
	// site's name and the plugin's search page.
	wp( [ 'rewrite', 'structure', '' ] );
	wp( [
		'eval',
		'update_option( "show_on_front", "posts" ); update_option( "page_on_front", 0 ); update_option( "page_for_posts", 0 );' +
			' update_option( "blogname", "Viglet Dumont test" ); update_option( "blogdescription", "" );' +
			' $s = viglet_dumont_get_settings(); $s["search_page"] = 0; viglet_dumont_update_settings( $s );',
	] );

	if ( ! process.env.E2E_SKIP_SEED ) {
		const out = wp( [ 'eval-file', '/e2e/seed.php' ] );
		const seed = JSON.parse( out.slice( out.indexOf( '{' ) ) );
		writeFileSync( seedFile, JSON.stringify( seed ) );
		wp( [ 'dumont', 'reindex' ] );
		if ( process.env.E2E_MULTISITE ) {
			const network = networkSites.map( ( url, i ) => {
				const printed = wp(
					[
						`--url=${ url }`,
						'eval-file',
						'/e2e/seed.php',
						...( i ? [ 'second' ] : [] ),
					],
					'cli-ms'
				);
				wp( [ `--url=${ url }`, 'dumont', 'reindex' ], 'cli-ms' );
				return {
					url,
					...JSON.parse( printed.slice( printed.indexOf( '{' ) ) ),
				};
			} );
			writeFileSync( networkSeedFile, JSON.stringify( network ) );
		}
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
