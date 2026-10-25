// The search block's parts as inner blocks (D185): a page that moves the pages above the
// results draws them in that order, and the parts still make one search, driven by the one
// store, with the filters beside the results.
import { expect, test } from '../fixtures.mjs';

const block = ( page ) => page.locator( '.viglet-dumont-search' );
const summary = ( page ) =>
	block( page ).locator( '.viglet-dumont-search__summary' );

test( 'the parts render in the order the page gives them, as one search', async ( {
	page,
	seed,
	look,
	axe,
} ) => {
	await page.goto( `/?page_id=${ seed.partsPageId }&q=*&sort=oldest` );
	await expect( summary( page ) ).toHaveText( /^\d+ results$/ );

	const pages = block( page ).locator( '.viglet-dumont-search__pages' );
	const results = block( page ).locator( '.viglet-dumont-search__results' );
	await expect( pages ).toBeVisible();
	// The pages come first in the document, as the page arranged them.
	expect(
		await block( page ).evaluate( ( root ) =>
			[
				...root.querySelectorAll(
					'.viglet-dumont-search__pages, .viglet-dumont-search__results'
				),
			].map( ( part ) => part.tagName )
		)
	).toEqual( [ 'NAV', 'OL' ] );

	// The filters sit beside the results, not above them.
	const facets = block( page ).locator( '.viglet-dumont-search__facets' );
	await expect( facets ).toBeVisible();
	const [ left, right ] = await Promise.all( [
		facets.boundingBox(),
		results.boundingBox(),
	] );
	expect( left.x + left.width ).toBeLessThanOrEqual( right.x );
	await look( 'search-parts' );
	await axe( 'the search block arranged as parts', {
		include: '.viglet-dumont-search',
	} );

	// One store drives every part: a search typed in the box fills the results and the
	// filters, and a filter narrows them.
	await block( page ).locator( 'input[type="search"]' ).fill( 'workshop' );
	await page.keyboard.press( 'Enter' );
	await expect( summary( page ) ).toHaveText( '4 results' );
	await facets
		.locator( 'label' )
		.filter( { hasText: '40' } )
		.locator( 'input' )
		.click();
	await expect( summary( page ) ).toHaveText( '3 results' );
	await expect(
		block( page ).locator( '.viglet-dumont-search__chip' )
	).toBeVisible();
} );
