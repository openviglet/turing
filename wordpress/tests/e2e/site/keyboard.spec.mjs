// The search block from the keyboard alone: every control reached with Tab, with its focus
// visible, and used with Enter or Space. Desktop only (playwright.config.mjs).
import { expect, test } from '../fixtures.mjs';

const block = ( page ) => page.locator( '.viglet-dumont-search' );
const summary = ( page ) =>
	block( page ).locator( '.viglet-dumont-search__summary' );

test( 'search, filter, remove the filter and change page without a mouse', async ( {
	page,
	seed,
	keyboard,
} ) => {
	await page.goto( `/?page_id=${ seed.searchPageId }` );

	const input = block( page ).locator( 'input[type="search"]' );
	await keyboard.tabTo( input );
	await page.keyboard.type( 'workshop' );
	// Not Escape: on a search field the browser clears the value with it.
	await page.keyboard.press( 'Enter' );
	await expect( summary( page ) ).toHaveText( '4 results' );

	const facet = block( page )
		.locator( '.viglet-dumont-search__facets label' )
		.filter( { hasText: '40' } )
		.locator( 'input' );
	await keyboard.tabTo( facet );
	await page.keyboard.press( 'Space' );
	await expect( summary( page ) ).toHaveText( '3 results' );

	const chip = block( page ).locator( '.viglet-dumont-search__chip' );
	await keyboard.tabTo( chip );
	await page.keyboard.press( 'Enter' );
	await expect( summary( page ) ).toHaveText( '4 results' );

	await page.goto( `/?page_id=${ seed.searchPageId }&q=*` );
	const second = block( page )
		.locator( '.viglet-dumont-search__pages button' )
		.nth( 1 );
	await expect( second ).toBeVisible();
	await keyboard.tabTo( second, { max: 120 } );
	await page.keyboard.press( 'Enter' );
	await expect( second ).toHaveAttribute( 'aria-current', 'true' );
} );

test( 'the correction is reached and taken from the keyboard', async ( {
	page,
	seed,
	keyboard,
} ) => {
	await page.goto( `/?page_id=${ seed.searchPageId }&q=worskhop` );
	const correction = block( page ).locator(
		'.viglet-dumont-search__correction button'
	);
	await expect( correction ).toBeVisible();
	await keyboard.tabTo( correction );
	await page.keyboard.press( 'Enter' );
	await expect( summary( page ) ).toHaveText( '4 results' );
} );
