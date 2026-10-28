// From a search to the cart with the keyboard alone (D193): type in the search box, Enter, Tab
// to a product's "Add to cart", Enter. Every stop on the way is a control a sighted keyboard
// user can see focused, and nothing traps the focus.
import { expect, test } from '../fixtures.mjs';

test( 'a shopper searches and adds a jacket to the cart without a mouse', async ( {
	page,
	seed,
} ) => {
	await page.goto( `/?page_id=${ seed.searchPageId }` );
	const box = page.locator( '.viglet-dumont-search' ).getByRole( 'combobox' );
	await box.focus();
	await page.keyboard.type( 'jacket' );
	await page.keyboard.press( 'Enter' );
	await expect( page.locator( '.viglet-dumont-search__summary' ) ).toHaveText(
		/\d+ results?/
	);

	const target = 'Add to cart: Rain jacket';
	let focused = '';
	for ( let tabs = 0; tabs < 60 && focused !== target; tabs++ ) {
		await page.keyboard.press( 'Tab' );
		focused = await page.locator( 'body' ).evaluate( ( body ) => {
			const el = body.ownerDocument.activeElement;
			return (
				el?.getAttribute( 'aria-label' ) ||
				el?.textContent?.trim() ||
				''
			);
		} );
	}
	expect( focused, 'reached by Tab from the search box' ).toBe( target );
	// The focus shows: an outline or a ring, not the bare button.
	const ring = await page.locator( 'body' ).evaluate( ( body ) => {
		const style = window.getComputedStyle(
			body.ownerDocument.activeElement
		);
		return 'none' !== style.outlineStyle || 'none' !== style.boxShadow;
	} );
	expect( ring ).toBe( true );

	await page.keyboard.press( 'Enter' );
	const rain = page
		.locator( '.viglet-dumont-search__results li' )
		.filter( { hasText: 'Rain jacket' } );
	await expect(
		rain.locator( '.viglet-dumont-search__cart-status' )
	).toHaveText( 'Added to your cart.' );
	const cart = await page.evaluate( async () =>
		( await window.fetch( '/?rest_route=/wc/store/v1/cart' ) ).json()
	);
	expect( cart.items.map( ( item ) => item.name ) ).toContain(
		'Rain jacket'
	);
} );
