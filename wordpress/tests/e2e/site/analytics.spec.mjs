// Search analytics (D176): the result a visitor opens is reported to Turing's click-through
// rates and, with the searches they settled on, to Google Analytics, never before the
// visitor allows statistics. The seed turns both on; gtag is a stub that keeps its calls in
// sessionStorage, so they survive the navigation a click starts.
import { expect, test } from '../fixtures.mjs';

const block = ( page ) => page.locator( '.viglet-dumont-search' );
const titles = ( page ) =>
	block( page ).locator(
		'.viglet-dumont-search__results .viglet-dumont-search__title a'
	);

/**
 * Stub the Google tag, and the WP Consent API when `consent` is a boolean.
 *
 * @param {import('@playwright/test').Page} page
 * @param {boolean|null}                    consent What the consent plugin answers for statistics.
 */
async function stub( page, consent = null ) {
	await page.addInitScript( ( allowed ) => {
		window.gtag = ( ...args ) => {
			const kept = JSON.parse( sessionStorage.getItem( 'gtag' ) || '[]' );
			kept.push( args );
			sessionStorage.setItem( 'gtag', JSON.stringify( kept ) );
		};
		if ( null !== allowed ) {
			window.wp_has_consent = ( category ) =>
				'statistics' === category && allowed;
		}
	}, consent );
	const clicks = [];
	page.on( 'request', ( request ) => {
		if (
			'POST' === request.method() &&
			/\/api\/sn\/[^/]+\/click$/.test( request.url() )
		) {
			clicks.push( request );
		}
	} );
	return clicks;
}

/**
 * The events gtag received, as [name, params].
 *
 * @param {import('@playwright/test').Page} page
 */
const events = async ( page ) =>
	(
		await page.evaluate( () =>
			JSON.parse( sessionStorage.getItem( 'gtag' ) || '[]' )
		)
	).map( ( [ , name, params ] ) => [ name, params ] );

test( 'a result opened from the block is reported once, with its rank, to Turing and to the Google tag', async ( {
	page,
	seed,
} ) => {
	const clicks = await stub( page );
	await page.goto(
		`/?page_id=${ seed.searchPageId }&q=workshop&sort=oldest`
	);
	await expect( titles( page ) ).toHaveCount( 4 );

	const sent = page.waitForRequest( /\/api\/sn\/[^/]+\/click$/ );
	const title = await titles( page ).nth( 1 ).textContent();
	await titles( page ).nth( 1 ).click();
	await expect( page ).toHaveTitle( new RegExp( `^${ title }` ) );

	const click = ( await sent ).postDataJSON();
	expect( click ).toMatchObject( { term: 'workshop', position: 2 } );
	expect( click.documentId ).toMatch( /:post:\d+$/ );
	expect( clicks ).toHaveLength( 1 );
	// Turing took it: the request outlived the page that sent it.
	expect( ( await ( await sent ).response() )?.status() ).toBe( 202 );

	const received = await events( page );
	expect( received ).toContainEqual( [
		'turing_search',
		expect.objectContaining( {
			query: 'workshop',
			results: 4,
			send_to: 'G-E2ETEST',
		} ),
	] );
	expect( received ).toContainEqual( [
		'turing_search_result_click',
		expect.objectContaining( {
			query: 'workshop',
			position: 2,
			document_id: click.documentId,
		} ),
	] );
} );

test( 'only a search the visitor settles on is reported, not each one typing runs', async ( {
	page,
	seed,
} ) => {
	await stub( page );
	await page.goto( `/?page_id=${ seed.searchPageId }` );
	const input = block( page ).getByRole( 'combobox' );
	await input.click();
	await page.keyboard.type( 'workshop', { delay: 400 } );
	await expect( titles( page ) ).toHaveCount( 4 );
	expect( await events( page ) ).toEqual( [] );

	await page.keyboard.press( 'Enter' );
	await expect
		.poll( () => events( page ) )
		.toEqual( [
			[
				'turing_search',
				expect.objectContaining( { query: 'workshop', results: 4 } ),
			],
		] );
} );

test( 'nothing is sent while the consent plugin has no statistics consent', async ( {
	page,
	seed,
} ) => {
	const clicks = await stub( page, false );
	await page.goto(
		`/?page_id=${ seed.searchPageId }&q=workshop&sort=oldest`
	);
	await expect( titles( page ) ).toHaveCount( 4 );
	const title = await titles( page ).first().textContent();
	await titles( page ).first().click();
	await expect( page ).toHaveTitle( new RegExp( `^${ title }` ) );

	expect( clicks ).toHaveLength( 0 );
	expect( await events( page ) ).toEqual( [] );
} );

test( 'with statistics consent, a result opened from the overlay is reported with its rank', async ( {
	page,
	seed,
} ) => {
	const clicks = await stub( page, true );
	await page.goto( `/?page_id=${ seed.corePageId }` );
	await page.keyboard.press( 'Control+K' );
	await page.keyboard.type( 'worksh' );
	const options = page.locator(
		'dialog.viglet-dumont-overlay [role="option"]'
	);
	await expect( options ).toHaveCount( 4 );

	const sent = page.waitForRequest( /\/api\/sn\/[^/]+\/click$/ );
	const title = await options
		.nth( 1 )
		.locator( '.viglet-dumont-overlay__title' )
		.textContent();
	await page.keyboard.press( 'ArrowDown' );
	await page.keyboard.press( 'ArrowDown' );
	await page.keyboard.press( 'Enter' );
	await expect( page ).toHaveTitle( new RegExp( `^${ title }` ) );
	expect( ( await sent ).postDataJSON() ).toMatchObject( {
		term: 'worksh',
		position: 2,
	} );
	expect( clicks ).toHaveLength( 1 );
	expect( await events( page ) ).toContainEqual( [
		'turing_search_result_click',
		expect.objectContaining( { query: 'worksh', position: 2 } ),
	] );
} );
