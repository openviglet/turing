#!/usr/bin/env node
// `npm run e2e:look -- <path or URL> [options]`: open one page of the run-docker.cmd stack the
// way a person would and report what is on it, so a change can be judged by its screen rather
// than by its code.
//
// It saves a full-page screenshot to tests/e2e/artifacts/look/<name>.png and prints:
// the title, console errors and page errors, failed or 4xx/5xx requests, the headings, the
// buttons, the form fields with their labels, and the visible text of the main region.
//
// Options:
//   --name <name>       the screenshot's file name (default: derived from the path)
//   --anon              no admin session (a visitor's view)
//   --mobile            a 390x844 phone viewport
//   --width <px>        the viewport width (default 1280); --height <px> (default 900)
//   --wait <ms>         extra time after the network settles (default 1500)
//   --script <file.mjs> a module whose default export runs on the page before the capture,
//                       e.g. async ( page ) => page.getByRole( 'button', { name: 'Search' } ).click()
//
// It logs in as tests/e2e/global-setup.mjs does when no session is stored yet.
import { existsSync, mkdirSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { chromium } from '@playwright/test';

const root = join( dirname( fileURLToPath( import.meta.url ) ), '..', '..' );
const authFile = join( root, 'tests', 'e2e', '.auth', 'admin.json' );
const baseURL = process.env.WP_URL || 'http://localhost:8080';

const args = process.argv.slice( 2 );
const option = ( name, fallback ) => {
	const i = args.indexOf( `--${ name }` );
	return i >= 0 ? args[ i + 1 ] : fallback;
};
const flag = ( name ) => args.includes( `--${ name }` );
const target = args.find(
	( a, i ) => ! a.startsWith( '--' ) && ! args[ i - 1 ]?.startsWith( '--' )
);
if ( ! target ) {
	process.stderr.write(
		'usage: npm run e2e:look -- <path or URL> [--name n] [--anon] [--mobile] [--script f.mjs]\n'
	);
	process.exit( 2 );
}

const url = new URL( target, baseURL ).toString();
const name = option(
	'name',
	new URL( url ).pathname.replace( /\W+/g, '-' ).replace( /^-|-$/g, '' ) +
		( new URL( url ).search
			? '-' +
				new URL( url ).search
					.replace( /\W+/g, '-' )
					.replace( /^-|-$/g, '' )
			: '' ) || 'home'
);
const viewport = flag( 'mobile' )
	? { width: 390, height: 844 }
	: {
			width: Number( option( 'width', 1280 ) ),
			height: Number( option( 'height', 900 ) ),
		};

const browser = await chromium.launch( {
	channel: process.env.E2E_CHANNEL || 'msedge',
} );

async function adminState() {
	if ( existsSync( authFile ) ) {
		return authFile;
	}
	const page = await browser.newPage( { baseURL } );
	await page.goto( '/wp-login.php' );
	await page.fill( '#user_login', process.env.WP_USER || 'admin' );
	await page.fill( '#user_pass', process.env.WP_PASSWORD || 'admin' );
	await Promise.all( [
		page.waitForURL( /wp-admin/ ),
		page.click( '#wp-submit' ),
	] );
	mkdirSync( dirname( authFile ), { recursive: true } );
	await page.context().storageState( { path: authFile } );
	await page.close();
	return authFile;
}

const context = await browser.newContext( {
	baseURL,
	viewport,
	isMobile: flag( 'mobile' ),
	storageState: flag( 'anon' ) ? undefined : await adminState(),
} );
const page = await context.newPage();

const problems = [];
page.on(
	'console',
	( m ) =>
		[ 'error', 'warning' ].includes( m.type() ) &&
		problems.push( `console ${ m.type() }: ${ m.text() }` )
);
page.on( 'pageerror', ( e ) => problems.push( `page error: ${ e.message }` ) );
page.on(
	'requestfailed',
	( r ) =>
		! ( r.failure()?.errorText || '' ).includes( 'ERR_ABORTED' ) &&
		problems.push(
			`request failed: ${ r.url() } (${ r.failure()?.errorText })`
		)
);
page.on(
	'response',
	( r ) =>
		r.status() >= 400 &&
		problems.push( `HTTP ${ r.status() }: ${ r.url() }` )
);

await page.goto( url, { waitUntil: 'networkidle' } );
if ( option( 'script' ) ) {
	const { default: run } = await import(
		pathToFileURL( resolve( option( 'script' ) ) ).href
	);
	await run( page );
	await page.waitForLoadState( 'networkidle' );
}
await page.waitForTimeout( Number( option( 'wait', 1500 ) ) );

const out = join( root, 'tests', 'e2e', 'artifacts', 'look', `${ name }.png` );
mkdirSync( dirname( out ), { recursive: true } );
await page.screenshot( { path: out, fullPage: true } );

const report = await page.evaluate( () => {
	const visible = ( el ) =>
		!! ( el.offsetWidth || el.offsetHeight || el.getClientRects().length );
	const text = ( el ) =>
		( el.innerText || el.textContent || '' ).replace( /\s+/g, ' ' ).trim();
	const labelOf = ( el ) =>
		el.getAttribute( 'aria-label' ) ||
		( el.id &&
			document.querySelector( `label[for="${ el.id }"]` )?.innerText ) ||
		el.closest( 'label' )?.innerText ||
		el.getAttribute( 'placeholder' ) ||
		el.name ||
		'';
	const main = document.querySelector(
		'#wpbody-content, main, .wp-site-blocks, body'
	);
	return {
		title: document.title,
		headings: [ ...document.querySelectorAll( 'h1, h2, h3' ) ]
			.filter( visible )
			.map( ( h ) => `${ h.tagName.toLowerCase() } ${ text( h ) }` ),
		buttons: [
			...document.querySelectorAll(
				'button, [role="button"], input[type="submit"]'
			),
		]
			.filter( visible )
			.map(
				( b ) => text( b ) || b.value || b.getAttribute( 'aria-label' )
			)
			.filter( Boolean ),
		fields: [
			...document.querySelectorAll(
				'input:not([type="hidden"]), select, textarea'
			),
		]
			.filter( visible )
			.map(
				( f ) =>
					`${ f.type || f.tagName.toLowerCase() } "${ labelOf( f ).replace( /\s+/g, ' ' ).trim() }" = ${ JSON.stringify( f.type === 'checkbox' ? f.checked : f.value ) }`
			),
		text: text( main ).slice( 0, 2000 ),
	};
} );

const lines = [
	`URL        ${ page.url() }`,
	`viewport   ${ viewport.width }x${ viewport.height }`,
	`screenshot ${ out }`,
	`title      ${ report.title }`,
	'',
	`problems (${ problems.length })`,
	...problems.map( ( p ) => `  - ${ p }` ),
	'',
	'headings',
	...report.headings.map( ( h ) => `  ${ h }` ),
	'',
	`buttons    ${ [ ...new Set( report.buttons ) ].join( ' | ' ) }`,
	'',
	'fields',
	...report.fields.map( ( f ) => `  ${ f }` ),
	'',
	'text',
	`  ${ report.text }`,
];
process.stdout.write( lines.join( '\n' ) + '\n' );
await browser.close();
