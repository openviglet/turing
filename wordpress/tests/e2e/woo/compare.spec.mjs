// Product comparison (D200), on the `woo` project's stack (fixtures/woo-seed.php): two jackets
// ticked in the search results open side by side, the rows where they differ marked.
import { expect, test } from '../fixtures.mjs';

const block = ( page ) => page.locator( '.viglet-dumont-search' );
const card = ( page, name ) =>
	block( page )
		.locator( '.viglet-dumont-search__results li' )
		.filter( { hasText: name } );
const jackets = ( seed ) =>
	`/?page_id=${ seed.searchPageId }&q=jacket&fq[]=type:product`;

test( 'two jackets ticked in the results compare side by side, their differences marked', async ( {
	page,
	seed,
	look,
	axe,
} ) => {
	await page.goto( jackets( seed ) );
	const bar = block( page ).locator( '.viglet-dumont-search__compare-bar' );
	await card( page, 'Rain jacket' )
		.getByRole( 'checkbox', { name: 'Compare Rain jacket' } )
		.check();
	// One is not a comparison.
	await expect( bar ).toBeHidden();
	await card( page, 'Wool jacket' )
		.getByRole( 'checkbox', { name: 'Compare Wool jacket' } )
		.check();
	await expect( bar.getByRole( 'link' ) ).toHaveText( 'Compare 2 products' );

	await bar.getByRole( 'link' ).click();
	const table = page.locator( '.viglet-dumont-compare__table' );
	await expect( table.locator( 'thead th' ) ).toHaveText( [
		'Rain jacket',
		'Wool jacket',
	] );
	const row = ( label ) =>
		table.locator( 'tbody tr' ).filter( {
			has: page.locator( 'th', { hasText: label } ),
		} );
	await expect( row( 'Price' ) ).toHaveClass( /differs/ );
	await expect( row( 'Price' ).locator( 'td' ) ).toHaveText( [
		/149\.90/,
		/89\.00/,
	] );
	await expect( row( 'Color' ) ).toHaveClass( /differs/ );
	await expect( row( 'Color' ).locator( 'td' ) ).toHaveText( [
		'Red',
		'Blue',
	] );
	// Both in stock: the same, so not marked.
	await expect( row( 'Availability' ) ).not.toHaveClass( /differs/ );
	await look( 'woo-compare' );
	await axe( 'the compare block', { include: '.viglet-dumont-compare' } );

	// The choice is kept for the tab: back in the results, both are still ticked.
	await page.goBack();
	await expect(
		card( page, 'Wool jacket' ).getByRole( 'checkbox' )
	).toBeChecked();
} );

test( 'the compare page without products says how to choose them', async ( {
	page,
} ) => {
	await page.goto( '/?pagename=compare-products' );
	await expect( page.locator( '.viglet-dumont-compare__empty' ) ).toHaveText(
		/Tick Compare on two to four products/
	);
} );
