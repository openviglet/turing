// The Ask the catalog block (D173) on the `woo` project's shop: the copilot's answer, its
// cited products with the shop's price, and one put in the cart with a click. Turing's
// copilot is answered with a fixed reply so the suite needs no LLM; what the block does with
// a real one is Turing's own copilot, read through the same call.
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

test( 'the copilot answers with products a click puts in the cart', async ( {
	page,
	look,
	axe,
} ) => {
	const pageId = await firstId( page, '/wp/v2/pages&slug=ask-the-shop' );
	const rain = await firstId(
		page,
		'/wc/store/v1/products&search=Rain%20jacket'
	);
	const asked = [];
	await page.route( '**/copilot/available', ( route ) =>
		route.fulfill( { json: { available: true } } )
	);
	await page.route( /\/copilot$/, async ( route ) => {
		asked.push( route.request().postDataJSON() );
		await route.fulfill( {
			json: {
				available: true,
				answer: 'The Rain jacket is waterproof and on sale [1].',
				citations: [
					{
						rank: 1,
						id: `localhost:post:${ rain }`,
						title: 'Rain jacket',
						url: `/?p=${ rain }`,
					},
				],
				totalHits: 1,
			},
		} );
	} );

	await page.goto( `/?page_id=${ pageId }` );
	const block = page.locator( '.viglet-dumont-copilot' );
	await block
		.getByLabel( 'What are you looking for?' )
		.fill( 'a waterproof jacket' );
	await block.getByRole( 'button', { name: 'Ask', exact: true } ).click();

	await expect(
		block.locator( '.viglet-dumont-copilot__answer' )
	).toHaveText( 'The Rain jacket is waterproof and on sale [1].' );
	const item = block.locator( '.viglet-dumont-copilot__item' );
	await expect( item ).toHaveCount( 1 );
	// The shop's own price for it, the regular one struck through.
	await expect( item.locator( 'strong' ) ).toHaveText( '$149.90' );
	await expect( item.locator( 'del' ) ).toContainText( '$199.00' );
	expect( asked[ 0 ].messages ).toEqual( [
		{ role: 'user', content: 'a waterproof jacket' },
	] );
	await look( 'woo-copilot' );
	await axe( 'the Ask the catalog block', {
		include: '.viglet-dumont-copilot',
	} );

	await item
		.getByRole( 'button', { name: 'Add to cart: Rain jacket' } )
		.click();
	await expect( item.getByRole( 'status' ) ).toHaveText(
		'Added to your cart.'
	);
	const cart = await page.evaluate( async () =>
		( await window.fetch( '/?rest_route=/wc/store/v1/cart' ) ).json()
	);
	expect( cart.items.map( ( entry ) => entry.name ) ).toContain(
		'Rain jacket'
	);

	// A follow-up carries the conversation so far.
	await block.getByLabel( 'What are you looking for?' ).fill( 'cheaper?' );
	await block.getByRole( 'button', { name: 'Ask', exact: true } ).click();
	await expect( block.locator( '.viglet-dumont-copilot__turn' ) ).toHaveCount(
		2
	);
	expect( asked[ 1 ].messages.map( ( m ) => m.role ) ).toEqual( [
		'user',
		'assistant',
		'user',
	] );
} );

test( 'without a language model for the copilot the block stays hidden', async ( {
	page,
} ) => {
	const pageId = await firstId( page, '/wp/v2/pages&slug=ask-the-shop' );
	await page.route( '**/copilot/available', ( route ) =>
		route.fulfill( { json: { available: false } } )
	);
	await page.goto( `/?page_id=${ pageId }` );
	await page.waitForLoadState( 'networkidle' );
	await expect( page.locator( '.viglet-dumont-copilot' ) ).toBeHidden();
} );
