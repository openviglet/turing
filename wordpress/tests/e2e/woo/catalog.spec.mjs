// A WooCommerce shop's products in the search block (D173), on the `woo` project's stack
// (fixtures/woo-seed.php): three jackets, filtered by a price range, by stock and by color.
import { expect, test } from '../fixtures.mjs';

const block = ( page ) => page.locator( '.viglet-dumont-search' );
const summary = ( page ) =>
	block( page ).locator( '.viglet-dumont-search__summary' );
const results = ( page ) =>
	block( page ).locator( '.viglet-dumont-search__results li' );
const facet = ( page, label ) =>
	block( page )
		.locator( '.viglet-dumont-search__facet' )
		.filter( { has: page.locator( 'summary', { hasText: label } ) } );
const value = ( page, label, text ) =>
	facet( page, label )
		.locator( 'label' )
		.filter( { hasText: text } )
		.locator( 'input' );
// The jackets alone: WooCommerce's own Cart page says "jacket" nowhere, but a search for
// "jacket" may still match it on other fields, so the specs filter to the product type.
const jackets = ( seed ) =>
	`/?page_id=${ seed.searchPageId }&q=jacket&fq[]=type:product`;

test( 'a price range narrows the jackets to the ones it holds', async ( {
	page,
	seed,
	look,
	axe,
} ) => {
	await page.goto( jackets( seed ) );
	await expect( summary( page ) ).toHaveText( '3 results' );
	await expect( facet( page, 'Price' ).locator( 'label' ) ).toHaveText( [
		/Up to 50\s*1/,
		/50 – 100\s*1/,
		/100 or more\s*1/,
	] );
	await look( 'woo-catalog' );
	await axe( 'a shop in the search block', {
		include: '.viglet-dumont-search',
	} );

	// The type badge reads WooCommerce's name for it, never Turing's highlight markup.
	await expect(
		results( page ).first().locator( '.viglet-dumont-search__type' )
	).toHaveText( 'Product' );

	await value( page, 'Price', '50 – 100' ).click();
	await expect( summary( page ) ).toHaveText( '1 result' );
	await expect( results( page ) ).toContainText( 'Wool jacket' );
	await expect(
		block( page )
			.locator( '.viglet-dumont-search__chip' )
			.filter( { hasText: 'Price:' } )
	).toHaveText( /Price: 50 – 100/ );
} );

test( 'a product card shows its price and adds the product to the cart', async ( {
	page,
	seed,
	look,
	axe,
	problems,
} ) => {
	await page.goto( jackets( seed ) );
	const rain = results( page ).filter( { hasText: 'Rain jacket' } );
	// On sale: the regular price struck through before the price it sells at.
	await expect( rain.locator( '.viglet-dumont-search__price' ) ).toHaveText(
		/Was\s*\$199\.00\s*\$149\.90/
	);
	await look( 'woo-cards' );
	await axe( 'product cards', { include: '.viglet-dumont-search__results' } );

	await rain
		.getByRole( 'button', { name: 'Add to cart: Rain jacket' } )
		.click();
	await expect(
		rain.locator( '.viglet-dumont-search__cart-status' )
	).toHaveText( 'Added to your cart.' );
	// The cart this visitor's session holds, as WooCommerce reads it.
	const cart = await page.evaluate( async () =>
		( await window.fetch( '/?rest_route=/wc/store/v1/cart' ) ).json()
	);
	expect( cart.items.map( ( item ) => item.name ) ).toContain(
		'Rain jacket'
	);

	// Out of stock: WooCommerce refuses with a 400 and says why, and the card shows it.
	problems.allow( /cart\/add-item|status of 400/ );
	const linen = results( page ).filter( { hasText: 'Linen jacket' } );
	await linen
		.getByRole( 'button', { name: 'Add to cart: Linen jacket' } )
		.click();
	await expect(
		linen.locator( '.viglet-dumont-search__cart-status' )
	).not.toHaveText( /Added|^$/ );
} );

test( 'a product sold in colours adds the colour the shop can sell', async ( {
	page,
	seed,
} ) => {
	// D245: WooCommerce adds a variation, never the product it belongs to; Red is sold out.
	await page.goto(
		`/?page_id=${ seed.searchPageId }&q=hat&fq[]=type:product`
	);
	const hat = results( page ).filter( { hasText: 'Wool hat' } );
	// The shop's price is filled in once the block has taken over the card it rendered.
	await expect( hat.locator( '.viglet-dumont-search__price' ) ).toHaveText(
		/\$25\.00/
	);
	await hat.getByRole( 'button', { name: 'Add to cart: Wool hat' } ).click();
	await expect(
		hat.locator( '.viglet-dumont-search__cart-status' )
	).toHaveText( 'Added to your cart.' );
	const cart = await page.evaluate( async () =>
		( await window.fetch( '/?rest_route=/wc/store/v1/cart' ) ).json()
	);
	const item = cart.items.find( ( entry ) => 'Wool hat' === entry.name );
	expect( item?.variation.map( ( v ) => v.value ) ).toEqual( [ 'Blue' ] );
} );

test( 'a cart status shown before the block takes over its cards is still shown after', async ( {
	page,
	seed,
} ) => {
	// D250: the block replaces the cards it rendered on the server once its own search
	// answers. That answer is held back here, so the add lands first and the take-over after.
	let asked;
	const searched = new Promise( ( resolve ) => ( asked = resolve ) );
	let release;
	const held = new Promise( ( resolve ) => ( release = resolve ) );
	await page.route( /\/api\/sn\/[^/]+\/search\/query/, async ( route ) => {
		asked();
		await held;
		await route.continue();
	} );
	await page.goto( jackets( seed ) );
	await searched;
	const wool = results( page ).filter( { hasText: 'Wool jacket' } );
	await wool
		.getByRole( 'button', { name: 'Add to cart: Wool jacket' } )
		.click();
	const status = wool.locator( '.viglet-dumont-search__cart-status' );
	await expect( status ).toHaveText( 'Added to your cart.' );

	const answered = page.waitForResponse( /\/api\/sn\/[^/]+\/search\/query/ );
	release();
	await answered;
	await expect( status ).toHaveText( 'Added to your cart.' );
} );

test( 'out of stock is a facet value, and color is one too', async ( {
	page,
	seed,
} ) => {
	await page.goto( jackets( seed ) );
	await expect( summary( page ) ).toHaveText( '3 results' );

	await value( page, 'Stock', 'Out of stock' ).click();
	await expect( summary( page ) ).toHaveText( '1 result' );
	await expect( results( page ) ).toContainText( 'Linen jacket' );

	await page.goto( jackets( seed ) );
	await value( page, 'Color', 'Red' ).click();
	await expect( summary( page ) ).toHaveText( '2 results' );
} );
