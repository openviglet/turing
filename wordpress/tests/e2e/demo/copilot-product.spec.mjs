// The catalog copilot on a product's page (D242): the box names the product, and the question
// goes to Turing with the product's document, which Turing answers about first (Turing T1178).
// Turing's copilot is answered here, so the spec needs no language model.
import { expect, test } from '../fixtures.mjs';
import { demoLocale } from './curated.mjs';

const words = {
	en_US: { ask: 'Ask about', button: 'Ask' },
	pt_BR: { ask: 'Pergunte sobre', button: 'Perguntar' },
};

test( 'on a product page the copilot asks about that product', async ( {
	page,
} ) => {
	const say = words[ demoLocale() ];
	test.skip( ! say, `no words for ${ demoLocale() }` );
	await page.route( '**/copilot/available', ( route ) =>
		route.fulfill( { json: { available: true } } )
	);
	const asked = [];
	await page.route( /\/copilot$/, ( route ) => {
		asked.push( route.request().postDataJSON() );
		return route.fulfill( {
			json: { available: true, answer: 'It is.', citations: [] },
		} );
	} );

	await page.goto( '/' );
	const product = await page.evaluate( async () => {
		const rows = await (
			await window.fetch(
				'/?rest_route=/wc/store/v1/products&per_page=1'
			)
		).json();
		return {
			id: rows[ 0 ].id,
			name: rows[ 0 ].name,
			url: rows[ 0 ].permalink,
		};
	} );
	await page.goto( product.url );

	const copilot = page.locator( '.viglet-dumont-copilot' );
	const box = copilot.getByLabel( `${ say.ask } ${ product.name }` );
	await expect( box ).toBeVisible();
	await box.fill( 'Will this keep me dry?' );
	await copilot
		.getByRole( 'button', { name: say.button, exact: true } )
		.click();
	await expect(
		copilot.locator( '.viglet-dumont-copilot__answer' )
	).toHaveText( 'It is.' );
	expect( asked[ 0 ].documentId ).toMatch(
		new RegExp( `:post:${ product.id }$` )
	);
} );

test( 'off a product page the copilot asks the whole catalog', async ( {
	page,
} ) => {
	test.skip( 'en_US' !== demoLocale(), 'the label is checked in English' );
	await page.route( '**/copilot/available', ( route ) =>
		route.fulfill( { json: { available: true } } )
	);
	const asked = [];
	await page.route( /\/copilot$/, ( route ) => {
		asked.push( route.request().postDataJSON() );
		return route.fulfill( {
			json: { available: true, answer: 'Several.', citations: [] },
		} );
	} );
	await page.goto( '/showcase/' );
	const copilot = page.locator( '.viglet-dumont-copilot' );
	await copilot
		.getByLabel( 'What are you looking for?' )
		.fill( 'A warm jacket' );
	await copilot.getByRole( 'button', { name: 'Ask', exact: true } ).click();
	await expect(
		copilot.locator( '.viglet-dumont-copilot__answer' )
	).toHaveText( 'Several.' );
	expect( asked[ 0 ] ).not.toHaveProperty( 'documentId' );
} );
