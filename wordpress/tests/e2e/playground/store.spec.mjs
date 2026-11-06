// The WordPress Playground demo store (D239): the published blueprint boots WordPress,
// WooCommerce, the plugin, the Tarnfell theme and its catalog in the browser, searches the
// hosted demo Turing, and a result opens the product it names in this store, since the seed
// pins each product's id and the hosted index carries the same ids.
import { expect, test } from '@playwright/test';

const blueprint =
	process.env.PLAYGROUND_BLUEPRINT ||
	'https://dumont.viglet.org/playground/blueprint.json';

test( 'the blueprint boots the store, and a search opens the product it names', async ( {
	page,
} ) => {
	await page.goto(
		`https://playground.wordpress.net/?blueprint-url=${ encodeURIComponent(
			blueprint
		) }`
	);
	// Playground draws the site in an iframe named wp, nested in its own remote frame, and
	// serves it from wpsandbox.wordpress.net/scope:…/ (the origin Turing must allow).
	await expect
		.poll(
			() =>
				page.frame( { name: 'wp' } )?.url().includes( '/scope:' ) ??
				false,
			{ timeout: 8 * 60 * 1000, intervals: [ 5000 ] }
		)
		.toBe( true );
	const site = page.frame( { name: 'wp' } );
	await expect( site.locator( 'body.home' ) ).toBeVisible( {
		timeout: 8 * 60 * 1000,
	} );

	// The search page searches the hosted Turing from the visitor's browser.
	await site
		.locator( 'body' )
		.evaluate( () =>
			window.location.assign(
				new URL( '?pagename=search-gear&q=tent', window.location.href )
					.href
			)
		);
	const title = site
		.locator(
			'.viglet-turing-search__results .viglet-turing-search__title a'
		)
		.filter( { hasText: /tent/i } )
		.first();
	await expect( title ).toBeVisible();
	const name = ( await title.textContent() ).trim();
	// A result indexed by another WordPress opens here by its id (siteUrl()).
	expect( await title.getAttribute( 'href' ) ).toMatch( /[?&]p=\d+/ );

	await title.click();
	await expect( site.locator( 'h1' ).first() ).toHaveText( name );
} );
