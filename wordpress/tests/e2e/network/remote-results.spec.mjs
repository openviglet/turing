// A site that indexes into a shared search sends its searches to the results page another
// site shows (D304): with "Results page on another site" set on the second site, Enter in
// its instant search lands on the main site's search page with the query. Runs only with
// E2E_MULTISITE=1.
import { readFileSync } from 'node:fs';
import { expect, test } from '../fixtures.mjs';
import { networkSeedFile, wp } from '../global-setup.mjs';

const [ main, second ] = JSON.parse( readFileSync( networkSeedFile, 'utf8' ) );
const results = `${ main.url }?page_id=${ main.searchPageId }`;

/**
 * Set, or with no value remove, the second site's results page on another site.
 *
 * @param {string} [url] The results page.
 */
const sendTo = ( url ) =>
	wp(
		[
			`--url=${ second.url }`,
			'option',
			'patch',
			// `insert` sets a key the stored settings may not have yet.
			url ? 'insert' : 'delete',
			'viglet_dumont_settings',
			'search_url',
			...( url ? [ url ] : [] ),
		],
		'cli-ms'
	);

test.beforeAll( () => sendTo( results ) );
test.afterAll( () => sendTo() );

test( "the second site's instant search sends Enter to the main site's results page", async ( {
	page,
} ) => {
	await page.goto( second.url );
	await page.keyboard.press( 'Control+K' );
	const overlay = page.locator( 'dialog.viglet-dumont-overlay' );
	await expect( overlay ).toBeVisible();

	await page.keyboard.type( 'workshop' );
	await page.keyboard.press( 'Enter' );

	// The main site's page, by its permalink once WordPress redirects `?page_id=` to it.
	const mainSite = main.url.replace( /[.?/]/g, '\\$&' );
	await expect( page ).toHaveURL(
		new RegExp( `^${ mainSite }(?!second/).*[?&]q=workshop$` )
	);
	await expect( page.locator( '.viglet-dumont-search__summary' ) ).toHaveText(
		'4 results'
	);
} );
