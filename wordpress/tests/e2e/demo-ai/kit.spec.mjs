// Build my kit (D201) on the demo store, asked of Turing's real kit call: each curated plan
// (demo/theme/tarnfell/kit-plans.json) stages only products the shop seeded, marks what it
// cannot sell, covers the needs the plan implies, and goes in the cart at WooCommerce's total.
// Part of the live-LLM suite, never the free one; woo/kit.spec.mjs checks the block with a
// fixed reply.
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { expect, test } from '../fixtures.mjs';
import { record } from '../ai/helpers.mjs';
import { demoLocale } from '../demo/curated.mjs';

const { plans } = JSON.parse(
	readFileSync(
		join( 'demo', 'theme', 'tarnfell', 'kit-plans.json' ),
		'utf8'
	)
);

/**
 * The post id a Turing document id names, as the block reads it (search.js postIdOf).
 *
 * @param {string} documentId The id, `<host>:post:<id>`.
 */
const postIdOf = ( documentId ) =>
	Number( String( documentId ).match( /:post:(\d+)$/ )?.[ 1 ] || 0 );

test.describe.configure( { mode: 'serial' } );

test.beforeEach( async ( { page } ) => {
	test.skip( 'en_US' !== demoLocale(), `the demo speaks ${ demoLocale() }` );
	await page.goto( '/showcase/' );
	const on = await page
		.locator( '.viglet-dumont-copilot__modes' )
		.waitFor( { state: 'visible', timeout: 15_000 } )
		.then( () => true )
		.catch( () => false );
	test.skip(
		! on,
		"the demo's copilot is off: start the stack with OPENAI_API_KEY and reindex the demo"
	);
} );

for ( const { plan, gaps } of plans ) {
	test( `"${ plan }" stages seeded, sellable products for each need`, async ( {
		page,
	} ) => {
		const block = page.locator( '.viglet-dumont-copilot' );
		await block
			.locator( '.viglet-dumont-copilot__modes' )
			.getByRole( 'button', { name: 'Plan a kit' } )
			.click();
		await block.getByLabel( 'Describe your plan' ).fill( plan );
		const started = Date.now();
		const [ response ] = await Promise.all( [
			page.waitForResponse( /\/copilot\/kit$/, { timeout: 90_000 } ),
			block
				.getByRole( 'button', { name: 'Build my kit', exact: true } )
				.click(),
		] );
		const kit = await response.json();
		record(
			`demo kit: ${ plan }`,
			plan,
			JSON.stringify( kit, null, 2 ),
			Date.now() - started
		);
		expect( kit.error ).toBeFalsy();
		expect( kit.stages?.length ).toBeGreaterThan( 0 );

		// Every staged id is a product the shop seeded, never a guide or an invented id.
		const staged = kit.stages.flatMap( ( stage ) => stage.products || [] );
		expect( staged.length ).toBeGreaterThan( 0 );
		const shop = new Map();
		for ( const product of staged ) {
			const id = postIdOf( product.id );
			expect( id, `${ product.id } names a post` ).toBeGreaterThan( 0 );
			const reply = await page.request.get(
				`/?rest_route=/wc/store/v1/products/${ id }`
			);
			expect(
				reply.ok(),
				`${ product.title } (${ id }) is a product of the shop`
			).toBe( true );
			shop.set( id, await reply.json() );
		}

		// Each need the plan implies is a stage, in the model's words, its search or its products'.
		for ( const { need, matches } of gaps ) {
			const pattern = new RegExp( matches, 'i' );
			expect(
				kit.stages.some( ( stage ) =>
					pattern.test(
						[
							stage.gap,
							stage.query,
							...( stage.products || [] ).map( ( p ) => p.title ),
						].join( ' ' )
					)
				),
				`the kit covers ${ need }`
			).toBe( true );
		}

		// What the shop cannot sell is marked and kept out; the rest is offered for the cart.
		const view = block.locator( '.viglet-dumont-copilot__kit' );
		await expect(
			view.locator( '.viglet-dumont-copilot__stage' )
		).toHaveCount( kit.stages.length );
		const items = view.locator( '.viglet-dumont-copilot__kit-item' );
		await expect( items ).toHaveCount( staged.length );
		const sellable = staged.filter(
			( product ) =>
				false !== product.available &&
				shop.get( postIdOf( product.id ) ).is_purchasable &&
				shop.get( postIdOf( product.id ) ).is_in_stock
		);
		for ( const product of staged ) {
			if ( ! sellable.includes( product ) ) {
				await expect(
					items.filter( { hasText: product.title } ).first()
				).toContainText( 'Out of stock' );
			}
		}
		// A stage is a choice (D240): its first sellable product is ticked, and only it.
		const chosen = kit.stages
			.map( ( stage ) =>
				( stage.products || [] ).find( ( p ) => sellable.includes( p ) )
			)
			.filter( Boolean );
		test.skip( ! chosen.length, 'nothing in this kit can be bought' );
		const add = view.locator( '.viglet-dumont-copilot__kit-cart button' );
		await expect( add ).toHaveText(
			1 === chosen.length
				? 'Add 1 product to the cart'
				: `Add ${ chosen.length } products to the cart`
		);

		// Prices and the total are WooCommerce's, never the model's.
		await add.click();
		const status = view.locator( '.viglet-dumont-copilot__kit-status' );
		await expect( status ).toContainText(
			`Added ${ chosen.length } to your cart.`
		);
		const cart = await page.evaluate( async () =>
			( await window.fetch( '/?rest_route=/wc/store/v1/cart' ) ).json()
		);
		// By name: a product sold in colours goes in as one of its variations (D245).
		expect(
			cart.items.map( ( entry ) => entry.name ).sort(),
			'the cart holds one product per need'
		).toEqual( chosen.map( ( p ) => p.title ).sort() );
		// A kit runs past a thousand, so the total is grouped as the shop's locale writes it.
		const total = new Intl.NumberFormat( 'en-US', {
			style: 'currency',
			currency: cart.totals.currency_code,
		} ).format(
			Number( cart.totals.total_price ) /
				10 ** cart.totals.currency_minor_unit
		);
		await expect( status ).toHaveText(
			`Added ${ chosen.length } to your cart. Cart total: ${ total }.`
		);
	} );
}
