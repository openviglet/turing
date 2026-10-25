// The `test` every spec imports: Playwright's, plus
// - a guard that fails the test on a console error, an uncaught page error, a failed request
//   or a 4xx/5xx response from this site or Turing (allow expected ones with `allow`);
// - `look( name )`, which saves a full-page screenshot to tests/e2e/artifacts/screens/<project>/
//   so a reviewer (or an agent) can see every screen the suite visited, at every viewport;
// - `axe( name, { include, exclude } )`, which fails the test on a serious or critical
//   accessibility violation on what the page shows now;
// - `keyboard.tabTo( locator )`, which reaches a control with Tab alone and asserts its focus
//   is visible;
// - `seed`, the ids tests/e2e/fixtures/seed.php reported.
/* eslint-disable react-hooks/rules-of-hooks -- Playwright fixtures hand their value to use(), which is not a React hook. */
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { test as base, expect } from '@playwright/test';
import { axeViolations, describe, failingImpacts } from './a11y.mjs';
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
				testInfo.project.name,
				`${ name }.png`
			);
			await page.screenshot( { path, fullPage: true } );
			await testInfo.attach( name, { path, contentType: 'image/png' } );
		} );
	},

	axe: async ( { page }, use, testInfo ) => {
		await use( async ( name, options ) => {
			const violations = await axeViolations( page, options );
			await testInfo.attach( `axe-${ name }`, {
				body: JSON.stringify( violations, null, 2 ),
				contentType: 'application/json',
			} );
			expect(
				violations
					.filter( ( v ) => failingImpacts.includes( v.impact ) )
					.map( describe ),
				`${ name }: serious or critical accessibility violations`
			).toEqual( [] );
		} );
	},

	keyboard: async ( { page }, use ) => {
		await use( {
			// Tab (never a click or focus()) until the control has the focus, then check that a
			// sighted keyboard user can see where it is.
			async tabTo( locator, { max = 80 } = {} ) {
				for ( let i = 0; i < max; i++ ) {
					await page.keyboard.press( 'Tab' );
					if (
						await locator.evaluate(
							( el ) => el === el.ownerDocument.activeElement
						)
					) {
						await expectFocusVisible( locator );
						return;
					}
				}
				throw new Error(
					`${ locator } was not reached in ${ max } Tabs`
				);
			},
		} );
	},
} );

/**
 * The focused control shows a ring: an outline or a box shadow, drawn under :focus-visible.
 *
 * @param {import('@playwright/test').Locator} locator
 */
export async function expectFocusVisible( locator ) {
	const ring = await locator.evaluate( ( el ) => {
		const style = window.getComputedStyle( el );
		return {
			focusVisible: el.matches( ':focus-visible' ),
			outline:
				'none' !== style.outlineStyle &&
				parseFloat( style.outlineWidth ) > 0,
			shadow: 'none' !== style.boxShadow,
		};
	} );
	expect(
		ring.focusVisible && ( ring.outline || ring.shadow ),
		`${ locator } has a visible focus ring (${ JSON.stringify( ring ) })`
	).toBe( true );
}

export { expect };
