// Build my kit (D201) on the `woo` project's shop: a described plan staged by need, each
// product priced and stocked by the shop, and the kit put in the cart in one click. Turing's
// kit call is answered with a fixed reply so the suite needs no LLM; what the block does with
// a real one is Turing's own kit (T1159), read through the same call.
import { expect, test } from '../fixtures.mjs';

/**
 * The id WordPress gives a seeded thing, read through its REST API.
 *
 * @param {import('@playwright/test').Page} page  A page of the site.
 * @param {string}                          route The REST route, with its query.
 */
async function firstId( page, route ) {
	const response = await page.request.get( `/?rest_route=${ route }` );
	const rows = await response.json();
	return rows[ 0 ].id;
}

test( 'a described plan becomes a kit that goes in the cart in one click', async ( {
	page,
	look,
	axe,
} ) => {
	const pageId = await firstId( page, '/wp/v2/pages&slug=ask-the-shop' );
	const product = ( name ) =>
		firstId(
			page,
			`/wc/store/v1/products&search=${ encodeURIComponent( name ) }`
		);
	const [ rain, wool, linen ] = await Promise.all( [
		product( 'Rain jacket' ),
		product( 'Wool jacket' ),
		product( 'Linen jacket' ),
	] );
	const staged = ( id, title, reason ) => ( {
		rank: 1,
		id: `localhost:post:${ id }`,
		title,
		url: `/?p=${ id }`,
		reason,
		// The index says every one can be bought; the shop knows Linen cannot.
		available: true,
	} );
	const asked = [];
	await page.route( '**/copilot/available', ( route ) =>
		route.fulfill( { json: { available: true } } )
	);
	await page.route( /\/copilot\/kit$/, async ( route ) => {
		asked.push( route.request().postDataJSON() );
		await route.fulfill( {
			json: {
				summary:
					'Two days in the mountains need a shell and a warm layer.',
				// One need, three alternatives for it, the first of which the shop cannot sell.
				stages: [
					{
						gap: 'Waterproof shell',
						reason: 'Rain is likely in July.',
						query: 'waterproof jacket',
						products: [
							staged( linen, 'Linen jacket', 'Color: Red' ),
							staged( rain, 'Rain jacket', 'Color: Red' ),
							staged( wool, 'Wool jacket', 'Color: Blue' ),
						],
					},
				],
				unfilled: [ 'Shelter' ],
				guides: [
					{
						rank: 1,
						id: 'localhost:post:1',
						title: 'Packing for the mountains',
						url: '/?p=1',
					},
				],
				error: null,
			},
		} );
	} );

	await page.goto( `/?page_id=${ pageId }` );
	const block = page.locator( '.viglet-dumont-copilot' );
	const modes = block.locator( '.viglet-dumont-copilot__modes' );
	await expect(
		modes.getByRole( 'button', { name: 'Ask a question' } )
	).toHaveAttribute( 'aria-pressed', 'true' );
	await modes.getByRole( 'button', { name: 'Plan a kit' } ).click();
	await expect(
		modes.getByRole( 'button', { name: 'Plan a kit' } )
	).toHaveAttribute( 'aria-pressed', 'true' );

	await block
		.getByLabel( 'Describe your plan' )
		.fill( 'Two days crossing the mountains in July, first time' );
	await block
		.getByRole( 'button', { name: 'Build my kit', exact: true } )
		.click();

	const kit = block.locator( '.viglet-dumont-copilot__kit' );
	await expect(
		kit.locator( '.viglet-dumont-copilot__kit-summary' )
	).toHaveText( 'Two days in the mountains need a shell and a warm layer.' );
	// The plan, the stock field the WooCommerce mapping indexes, and journal posts as guides.
	expect( asked[ 0 ] ).toMatchObject( {
		plan: 'Two days crossing the mountains in July, first time',
		availabilityField: 'stock_status',
		availability: 'MARK',
		guideTypes: [ 'post' ],
	} );
	await expect(
		kit.locator( '.viglet-dumont-copilot__stage h3' )
	).toHaveText( [ 'Waterproof shell' ] );
	await expect(
		kit.locator( '.viglet-dumont-copilot__unfilled' )
	).toHaveText( 'Nothing in the catalog yet for: Shelter.' );
	await expect(
		kit.getByRole( 'link', { name: 'Packing for the mountains' } )
	).toBeVisible();

	// The shop's price, and its word on stock over the index's.
	const item = ( name ) =>
		kit
			.locator( '.viglet-dumont-copilot__kit-item' )
			.filter( { hasText: name } );
	await expect( item( 'Rain jacket' ).locator( 'strong' ) ).toHaveText(
		'$149.90'
	);
	await expect( item( 'Linen jacket' ) ).toContainText( 'Out of stock' );
	await expect(
		item( 'Linen jacket' ).getByRole( 'checkbox' )
	).toBeDisabled();
	// A stage is a choice (D240): the first product the shop can sell is ticked, past the
	// one it cannot, and the others wait to be swapped in.
	await expect( item( 'Rain jacket' ).getByRole( 'checkbox' ) ).toBeChecked();
	await expect(
		item( 'Wool jacket' ).getByRole( 'checkbox' )
	).not.toBeChecked();
	const add = kit.locator( '.viglet-dumont-copilot__kit-cart button' );
	await expect( add ).toHaveText( 'Add 1 product to the cart' );

	// A product ticked joins the cart.
	await item( 'Wool jacket' )
		.getByRole( 'checkbox', { name: 'Keep Wool jacket in the kit' } )
		.check();
	await expect( add ).toHaveText( 'Add 2 products to the cart' );

	await look( 'woo-kit' );
	await axe( 'the Build my kit mode', {
		include: '.viglet-dumont-copilot',
	} );

	await add.click();
	const status = kit.locator( '.viglet-dumont-copilot__kit-status' );
	await expect( status ).toContainText( 'Added 2 to your cart.' );
	const cart = await page.evaluate( async () =>
		( await window.fetch( '/?rest_route=/wc/store/v1/cart' ) ).json()
	);
	expect( cart.items.map( ( entry ) => entry.name ).sort() ).toEqual( [
		'Rain jacket',
		'Wool jacket',
	] );
	// The total is the cart's own, as WooCommerce computed it.
	const total = `$${ (
		Number( cart.totals.total_price ) /
		10 ** cart.totals.currency_minor_unit
	).toFixed( 2 ) }`;
	await expect( status ).toHaveText(
		`Added 2 to your cart. Cart total: ${ total }.`
	);
} );
