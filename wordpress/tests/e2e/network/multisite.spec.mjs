// A multisite network (run-docker.cmd multisite): two sites of one network share one SN
// site, each under its own site key, and each one's search block finds only its own
// documents. Runs only with E2E_MULTISITE=1 (the network project in playwright.config.mjs).
import { readFileSync } from 'node:fs';
import { expect, test } from '../fixtures.mjs';
import { networkSeedFile } from '../global-setup.mjs';

const [ main, second ] = JSON.parse( readFileSync( networkSeedFile, 'utf8' ) );
const only = 'Only on the second site';

const titles = ( page ) =>
	page.locator( '.viglet-dumont-search__results li a' ).allTextContents();

test( 'the second site finds its own post and none of the main site’s', async ( {
	page,
	look,
} ) => {
	await page.goto( `${ second.url }?page_id=${ second.searchPageId }&q=*` );
	await expect(
		page.locator( '.viglet-dumont-search__results li' ).first()
	).toBeVisible();
	const found = await titles( page );
	await look( 'network-second-site' );
	expect( found ).toContain( only );
	expect( found.filter( ( t ) => t.startsWith( 'Workshop on' ) ) ).toEqual(
		[]
	);
} );

test( 'the main site finds its own posts and never the second site’s', async ( {
	page,
	look,
} ) => {
	await page.goto(
		`${ main.url }?page_id=${ main.searchPageId }&q=workshop`
	);
	await expect( page.locator( '.viglet-dumont-search__summary' ) ).toHaveText(
		'4 results'
	);

	// The second site's post, asked for by its own title: it would rank first if it leaked.
	await page.goto(
		`${ main.url }?page_id=${ main.searchPageId }&q=${ encodeURIComponent(
			only
		) }`
	);
	await expect( page.locator( '.viglet-dumont-search__summary' ) ).toHaveText(
		/results|No results/
	);
	await look( 'network-main-site' );
	expect( await titles( page ) ).not.toContain( only );
} );
