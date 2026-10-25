// The `test` every spec imports: Playwright's, plus
// - a guard that fails the test on a console error, an uncaught page error, a failed request
//   or a 4xx/5xx response from this site or Turing (allow expected ones with `allow`);
// - `look( name )`, which saves a full-page screenshot to tests/e2e/artifacts/screens/ so a
//   reviewer (or an agent) can see every screen the suite visited;
// - `seed`, the ids tests/e2e/fixtures/seed.php reported.
/* eslint-disable react-hooks/rules-of-hooks -- Playwright fixtures hand their value to use(), which is not a React hook. */
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { test as base, expect } from '@playwright/test';
import { seedFile } from './global-setup.mjs';

const ignoredUrls = [ /favicon\.ico/, /\/wp-json\/wp\/v2\/users\/me\?/ ];

export const test = base.extend( {
	seed: async ( {}, use ) => {
		await use( JSON.parse( readFileSync( seedFile, 'utf8' ) ) );
	},

	// `auto`: every test gets the guard without asking for it.
	problems: [
		async ( { page }, use ) => {
			const problems = [];
			const allowed = [];
			const ignore = ( text ) =>
				[ ...ignoredUrls, ...allowed ].some( ( pattern ) =>
					pattern.test( text )
				);

			page.on( 'console', ( message ) => {
				if (
					'error' === message.type() &&
					! ignore( message.text() )
				) {
					problems.push( `console: ${ message.text() }` );
				}
			} );
			page.on( 'pageerror', ( error ) =>
				problems.push( `page error: ${ error.message }` )
			);
			page.on( 'requestfailed', ( request ) => {
				const failure = request.failure()?.errorText || '';
				// A navigation away cancels the requests still in flight; that is not a failure.
				if (
					! failure.includes( 'ERR_ABORTED' ) &&
					! ignore( request.url() )
				) {
					problems.push(
						`request failed: ${ request.url() } (${ failure })`
					);
				}
			} );
			page.on( 'response', ( response ) => {
				if ( response.status() >= 400 && ! ignore( response.url() ) ) {
					problems.push(
						`HTTP ${ response.status() }: ${ response.url() }`
					);
				}
			} );

			await use( {
				allow: ( pattern ) => allowed.push( pattern ),
				list: () => problems,
			} );

			expect(
				problems,
				'the page logged errors or failed requests'
			).toEqual( [] );
		},
		{ auto: true },
	],

	look: async ( { page }, use, testInfo ) => {
		await use( async ( name ) => {
			const path = join(
				'tests',
				'e2e',
				'artifacts',
				'screens',
				`${ name }.png`
			);
			await page.screenshot( { path, fullPage: true } );
			await testInfo.attach( name, { path, contentType: 'image/png' } );
		} );
	},
} );

export { expect };
