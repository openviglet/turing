#!/usr/bin/env node
// The demo sites (D325): one descriptor per site in demo/sites/<key>.json, read by run-docker.cmd,
// bin/run-docker.sh and the Playwright configs, so none of them hard-codes a store. A descriptor
// names the site's theme, its seed, its title, whether it runs WooCommerce, its port and SN site
// (each overridable by the environment variable it names), its language and its specs' folder.
//
//   node bin/demo-site.mjs edu         KEY=value lines, for run-docker.cmd's `for /f`
//   node bin/demo-site.mjs edu --sh    the same as shell assignments, for bin/run-docker.sh
//   node bin/demo-site.mjs --keys      every key, space-separated
//
// The keys are the files' names: en and pt are the stores (D283), edu and edu-pt the school.
import { readFileSync, readdirSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const sites = join(
	dirname( fileURLToPath( import.meta.url ) ),
	'..',
	'demo',
	'sites'
);

/** @return {string[]} Every demo site's key, in name order. */
export function demoKeys() {
	return readdirSync( sites )
		.filter( ( file ) => file.endsWith( '.json' ) )
		.map( ( file ) => file.slice( 0, -5 ) )
		.sort();
}

/**
 * One demo site, with the environment's port and SN site applied and what follows from them.
 *
 * @param {string} key The descriptor's name, e.g. `edu`.
 * @return {Object} The descriptor, plus `url` and `cli` (its WP-CLI compose service).
 */
export function demoSite( key ) {
	if ( ! demoKeys().includes( key ) ) {
		throw new Error(
			`Unknown demo site: ${ key }. Use one of: ${ demoKeys().join(
				', '
			) }.`
		);
	}
	const site = JSON.parse(
		readFileSync( join( sites, `${ key }.json` ), 'utf8' )
	);
	const port = Number( process.env[ site.portEnv ] || site.port );
	const snSite = process.env[ site.snSiteEnv ] || site.snSite;
	return {
		...site,
		key,
		port,
		snSite,
		url: `http://localhost:${ port }`,
		cli: `cli-demo${ site.services }`,
	};
}

/**
 * The values the run scripts need, in the order they use them.
 *
 * @param {Object} site A demo site.
 * @return {Array<[string, string]>} Name and value pairs.
 */
function scriptValues( site ) {
	return [
		[ 'NAME', site.name ],
		[ 'SFX', site.services ],
		[ 'PORT', String( site.port ) ],
		[ 'SITE', site.snSite ],
		[ 'LOCALE', site.locale ],
		[ 'TITLE', site.title ],
		[ 'THEME', site.theme ],
		[ 'WOO', site.woocommerce ? '1' : '' ],
		[ 'SEED', site.seed ],
		[ 'HISTORY', site.history || '' ],
	];
}

// Run as a script, not imported (a path compared without case: Windows drives differ in it).
const self = fileURLToPath( import.meta.url ).toLowerCase();
if ( resolve( process.argv[ 1 ] || '' ).toLowerCase() === self ) {
	const [ key, format ] = process.argv.slice( 2 );
	if ( '--keys' === key ) {
		process.stdout.write( `${ demoKeys().join( ' ' ) }\n` );
		process.exit( 0 );
	}
	let site;
	try {
		site = demoSite( key || '' );
	} catch ( error ) {
		process.stderr.write( `${ error.message }\n` );
		process.exit( 1 );
	}
	const quote = ( value ) => `'${ value.replace( /'/g, `'\\''` ) }'`;
	for ( const [ name, value ] of scriptValues( site ) ) {
		process.stdout.write(
			'--sh' === format
				? `demo_${ name.toLowerCase() }=${ quote( value ) }\n`
				: `${ name }=${ value }\n`
		);
	}
}
