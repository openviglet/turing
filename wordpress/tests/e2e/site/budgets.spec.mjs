// Speed, as the visitor's browser measures it on the local stack: the first results within
// 1.5 s of the navigation starting, and a suggestion within 500 ms of the typing. Desktop only.
import { expect, test } from '../fixtures.mjs';

const firstResultsMs = 1500;
const suggestionMs = 500;

// Stamped by the page itself (performance.now(), whose zero is the navigation start), so the
// numbers carry none of Playwright's polling.
test.beforeEach( async ( { page } ) => {
	await page.addInitScript( () => {
		const marks = ( window.__vigletDumontTiming = {} );
		document.addEventListener(
			'input',
			() => {
				marks.typed = window.performance.now();
				delete marks.suggested;
			},
			true
		);
		new window.MutationObserver( () => {
			const now = window.performance.now();
			if (
				! marks.results &&
				document.querySelector( '.viglet-dumont-search__results li' )
			) {
				marks.results = now;
			}
			if (
				marks.typed &&
				! marks.suggested &&
				document.querySelector(
					'.viglet-dumont-search__suggestions:not([hidden]) button'
				)
			) {
				marks.suggested = now;
			}
		} ).observe( document, {
			subtree: true,
			childList: true,
			attributes: true,
		} );
	} );
} );

const marks = ( page ) => page.evaluate( () => window.__vigletDumontTiming );

test( `the first results show within ${ firstResultsMs } ms`, async ( {
	page,
	seed,
}, testInfo ) => {
	await page.goto( `/?page_id=${ seed.searchPageId }&q=workshop` );
	await expect(
		page.locator( '.viglet-dumont-search__results li' ).first()
	).toBeVisible();
	const { results } = await marks( page );
	testInfo.annotations.push( {
		type: 'timing',
		description: `first results ${ Math.round( results ) } ms`,
	} );
	expect( results ).toBeLessThan( firstResultsMs );
} );

test( `a suggestion shows within ${ suggestionMs } ms of typing`, async ( {
	page,
	seed,
}, testInfo ) => {
	await page.goto( `/?page_id=${ seed.searchPageId }` );
	const input = page.locator( '.viglet-dumont-search input[type="search"]' );
	// Warm the path once: the first request pays for the connection, not the search.
	await input.fill( 'wo' );
	await expect(
		page.locator( '.viglet-dumont-search__suggestions button' ).first()
	).toBeVisible();
	await input.fill( '' );
	await expect(
		page.locator( '.viglet-dumont-search__suggestions' )
	).toBeHidden();

	await input.fill( 'work' );
	await expect(
		page
			.locator( '.viglet-dumont-search__suggestions button' )
			.filter( { hasText: 'workshop' } )
	).toBeVisible();
	const { typed, suggested } = await marks( page );
	const elapsed = suggested - typed;
	testInfo.annotations.push( {
		type: 'timing',
		description: `suggestion ${ Math.round( elapsed ) } ms`,
	} );
	expect( elapsed ).toBeLessThan( suggestionMs );
} );
