// Turing's generative AI on the demo store (D198), on the `demo` project's stack: the Site
// assistant waits in a corner of every page, and stays out of sight while Turing's chat is off.
// Turing's probe is answered here, so the spec needs no LLM key.
import { expect, test } from '../fixtures.mjs';
import { demoLocale } from './curated.mjs';

for ( const path of [ '/', '/?pagename=search-gear', '/journal/' ] ) {
	test( `the Site assistant is in the corner of ${ path }`, async ( {
		page,
	} ) => {
		await page.route( '**/chat/enabled', ( route ) =>
			route.fulfill( { json: { enabled: true } } )
		);
		await page.goto( path );
		const corner = page.locator( '.viglet-dumont-assistant.is-floating' );
		await expect( corner ).toBeVisible();
		await expect( corner.getByRole( 'button' ) ).toHaveAttribute(
			'aria-haspopup',
			'dialog'
		);
	} );
}

test( 'with chat off in Turing, the corner stays empty', async ( { page } ) => {
	await page.route( '**/chat/enabled', ( route ) =>
		route.fulfill( { json: { enabled: false, reason: 'MISSING_LLM' } } )
	);
	await page.goto( '/' );
	await page.waitForLoadState( 'networkidle' );
	await expect(
		page.locator( '.viglet-dumont-assistant.is-floating' )
	).toBeHidden();
} );

// D242: on a product's page every turn names the product, which Turing reads first (T1178,
// through the SDK's chat controller, T1187); off one, the turn asks the whole site.
test( 'on a product page the Site assistant asks about that product', async ( {
	page,
} ) => {
	test.skip( 'en_US' !== demoLocale(), 'the labels are checked in English' );
	await page.route( '**/chat/enabled', ( route ) =>
		route.fulfill( { json: { enabled: true } } )
	);
	const asked = [];
	await page.route( '**/chat/conversation', ( route ) => {
		asked.push( route.request().postDataJSON() );
		return route.fulfill( {
			status: 200,
			headers: { 'Content-Type': 'text/event-stream' },
			body: `data: ${ JSON.stringify( {
				role: 'assistant',
				content: 'It is.',
			} ) }\n\n`,
		} );
	} );
	const ask = async () => {
		const corner = page.locator( '.viglet-dumont-assistant.is-floating' );
		await corner.locator( '.viglet-dumont-assistant__open' ).click();
		await corner
			.getByLabel( 'Your question' )
			.fill( 'Will it keep me dry?' );
		await corner.getByRole( 'button', { name: 'Send' } ).click();
		await expect(
			corner.locator( '.viglet-dumont-assistant__message' ).last()
		).toHaveText( 'It is.' );
	};

	await page.goto( '/' );
	const product = await page.evaluate( async () => {
		const rows = await (
			await window.fetch(
				'/?rest_route=/wc/store/v1/products&per_page=1'
			)
		).json();
		return { id: rows[ 0 ].id, url: rows[ 0 ].permalink };
	} );
	await page.goto( product.url );
	await ask();
	expect( asked[ 0 ].documentId ).toMatch(
		new RegExp( `:post:${ product.id }$` )
	);

	await page.goto( '/journal/' );
	await ask();
	expect( asked[ 1 ] ).not.toHaveProperty( 'documentId' );
} );
