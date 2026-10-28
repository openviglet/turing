// Turing's generative AI on the demo store (D198), on the `demo` project's stack: the Site
// assistant waits in a corner of every page, and stays out of sight while Turing's chat is off.
// Turing's probe is answered here, so the spec needs no LLM key.
import { expect, test } from '../fixtures.mjs';

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
