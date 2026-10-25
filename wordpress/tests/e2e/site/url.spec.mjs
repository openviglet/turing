// The search lives in the address bar: filters, page and sort survive a reload and a shared
// link, Back undoes a step, the sort orders by date, and a refined URL is kept out of
// search engines. Desktop only (playwright.config.mjs).
import { expect, test } from '../fixtures.mjs';

const block = ( page ) => page.locator( '.viglet-dumont-search' );
const summary = ( page ) =>
	block( page ).locator( '.viglet-dumont-search__summary' );
const titles = ( page ) =>
	block( page ).locator( '.viglet-dumont-search__title a' );
const seats40 = ( page ) =>
	block( page )
		.locator( '.viglet-dumont-search__facets label' )
		.filter( { hasText: '40' } )
		.locator( 'input' );
const sort = ( page ) => block( page ).getByLabel( 'Sort by' );
const robots = ( page ) =>
	page.locator( 'meta[name="robots"]' ).getAttribute( 'content' );

test( 'a reload keeps the filter, the sort and the page', async ( {
	page,
	seed,
} ) => {
	await page.goto( `/?page_id=${ seed.searchPageId }&q=*` );
	await expect( summary( page ) ).toHaveText( '16 results' );
	await sort( page ).selectOption( 'newest' );
	await expect( page ).toHaveURL( /[?&]sort=newest/ );
	// The sorted answer, not the one it replaces.
	await page.waitForLoadState( 'networkidle' );
	const first = await titles( page ).first().textContent();
	await block( page )
		.locator( '.viglet-dumont-search__pages button' )
		.nth( 1 )
		.click();
	await expect( page ).toHaveURL( /[?&]pg=2/ );
	await expect( titles( page ).first() ).not.toHaveText( first );
	const second = await titles( page ).first().textContent();

	await page.reload();
	await expect( sort( page ) ).toHaveValue( 'newest' );
	await expect(
		block( page ).locator( '.viglet-dumont-search__pages button' ).nth( 1 )
	).toHaveAttribute( 'aria-current', 'true' );
	await expect( titles( page ).first() ).toHaveText( second );
} );

test( 'Back undoes a filter, and Forward puts it back', async ( {
	page,
	seed,
} ) => {
	await page.goto( `/?page_id=${ seed.searchPageId }&q=workshop` );
	await expect( summary( page ) ).toHaveText( '4 results' );
	await seats40( page ).click();
	await expect( summary( page ) ).toHaveText( '3 results' );
	await expect( page ).toHaveURL( /fq%5B%5D=seats%3A40/ );

	await page.goBack();
	await expect( summary( page ) ).toHaveText( '4 results' );
	await expect( page ).not.toHaveURL( /fq%5B%5D/ );

	await page.goForward();
	await expect( summary( page ) ).toHaveText( '3 results' );
} );

test( 'a shared link opens the same filtered search, titled and kept out of indexes', async ( {
	page,
	seed,
} ) => {
	await page.goto(
		`/?page_id=${ seed.searchPageId }&q=workshop&fq%5B%5D=seats%3A40`
	);
	await expect( summary( page ) ).toHaveText( '3 results' );
	await expect( seats40( page ) ).toBeChecked();
	await expect(
		block( page ).locator( '.viglet-dumont-search__chip' )
	).toContainText( 'Seats: 40' );
	await expect( page ).toHaveTitle( /^workshop – / );
	expect( await robots( page ) ).toContain( 'noindex' );

	// The plain search page stays indexable.
	await page.goto( `/?page_id=${ seed.searchPageId }&q=workshop` );
	await expect( summary( page ) ).toHaveText( '4 results' );
	expect( ( await robots( page ) ) || '' ).not.toContain( 'noindex' );
} );

test( 'newest and oldest order by publication date', async ( {
	page,
	seed,
} ) => {
	await page.goto(
		`/?page_id=${ seed.searchPageId }&q=workshop&sort=newest`
	);
	await expect( titles( page ) ).toHaveText( [
		'Workshop on testing',
		'Workshop on blocks',
		'Workshop on facets',
		'Workshop on search',
	] );
	await sort( page ).selectOption( 'oldest' );
	await expect( titles( page ).first() ).toHaveText( 'Workshop on search' );
} );
