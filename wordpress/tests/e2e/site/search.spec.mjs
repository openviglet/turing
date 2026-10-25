// The visitor's side: the Turing search block, and the theme's own search page answered by
// Turing. Anonymous: no stored session.
import { expect, test } from '../fixtures.mjs';

const block = ( page ) => page.locator( '.viglet-dumont-search' );
const results = ( page ) =>
	block( page ).locator( '.viglet-dumont-search__results li' );
const summary = ( page ) =>
	block( page ).locator( '.viglet-dumont-search__summary' );

test( "the block answers a query with this site's results, as plain text", async ( {
	page,
	seed,
	look,
} ) => {
	await page.goto( `/?page_id=${ seed.searchPageId }&q=workshop` );
	await expect( summary( page ) ).toHaveText( '4 results' );
	await expect( results( page ) ).toHaveCount( 4 );
	await expect( results( page ).first().locator( 'a' ) ).not.toContainText(
		'<mark>'
	);
	await look( 'search-block-workshop' );
} );

test( 'a facet narrows the results, shows as a removable filter, and removing it restores them', async ( {
	page,
	seed,
	look,
} ) => {
	await page.goto( `/?page_id=${ seed.searchPageId }&q=workshop` );
	await expect( summary( page ) ).toHaveText( '4 results' );

	// click(), not check(): the facets re-render from the new answer.
	await block( page )
		.locator( '.viglet-dumont-search__facets label' )
		.filter( { hasText: '40' } )
		.locator( 'input' )
		.click();
	await expect( summary( page ) ).toHaveText( '3 results' );
	const chip = block( page ).locator( '.viglet-dumont-search__chip' );
	await expect( chip ).toHaveCount( 1 );
	await expect( chip ).toContainText( 'Seats: 40' );
	await look( 'search-block-facet' );

	await chip.click();
	await expect( summary( page ) ).toHaveText( '4 results' );
	await expect( chip ).toHaveCount( 0 );
} );

test( 'typing suggests, and picking a suggestion searches it', async ( {
	page,
	seed,
} ) => {
	await page.goto( `/?page_id=${ seed.searchPageId }` );
	await block( page )
		.locator( 'input[type="search"]' )
		.pressSequentially( 'work', { delay: 50 } );
	const suggestion = block( page )
		.locator( '.viglet-dumont-search__suggestions button' )
		.filter( { hasText: 'workshop' } );
	await expect( suggestion ).toBeVisible();
	await suggestion.click();
	await expect( summary( page ) ).toHaveText( '4 results' );
	await expect( page ).toHaveURL( /q=workshop/ );
} );

test( 'a misspelling offers the correction, and taking it searches again', async ( {
	page,
	seed,
	look,
} ) => {
	await page.goto( `/?page_id=${ seed.searchPageId }&q=worskhop` );
	const correction = block( page ).locator(
		'.viglet-dumont-search__correction'
	);
	await expect( correction ).toBeVisible();
	await expect( correction.locator( 'button' ) ).toHaveText( 'workshop' );
	await look( 'search-block-did-you-mean' );

	await correction.locator( 'button' ).click();
	await expect( summary( page ) ).toHaveText( '4 results' );
} );

test( 'everything spans pages, and the next page shows other results', async ( {
	page,
	seed,
	look,
} ) => {
	await page.goto( `/?page_id=${ seed.searchPageId }&q=*` );
	const pages = block( page ).locator(
		'.viglet-dumont-search__pages button'
	);
	await expect( pages ).toHaveCount( 2 );
	await expect( pages.first() ).toHaveAttribute( 'aria-current', 'true' );
	const first = await results( page ).first().textContent();

	await pages.nth( 1 ).click();
	await expect( pages.nth( 1 ) ).toHaveAttribute( 'aria-current', 'true' );
	await expect( results( page ).first() ).not.toHaveText( first );
	await look( 'search-block-page-2' );
} );

test( "the theme's own search page shows Turing's results inside the theme", async ( {
	page,
	look,
} ) => {
	// WordPress's database search matches nothing for "*"; Turing matches everything.
	await page.goto( '/?s=*' );
	await expect(
		page.locator( 'header.wp-block-template-part' )
	).toBeVisible();
	await expect(
		page.locator( 'main .wp-block-post-title' ).first()
	).toBeVisible();
	await look( 'theme-search' );
} );
