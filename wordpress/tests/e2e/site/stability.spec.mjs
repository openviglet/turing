// A search page opened with a query (D209): the server draws the first page, so the results are
// in the HTML before any script runs, and nothing moves once the script answers. Layout shift is
// read with a PerformanceObserver, as Lighthouse sums it.
import { expect, test } from '../fixtures.mjs';

test( 'the first page of results is in the HTML the server sends', async ( {
	page,
	seed,
} ) => {
	const html = await (
		await page.request.get( `/?page_id=${ seed.searchPageId }&q=workshop` )
	).text();
	expect( html ).toContain( '>Workshop on' );
	expect( html ).toContain( '>4 results<' );
} );

for ( const query of [
	'q=workshop',
	'q=*&sort=oldest',
	'q=workshop&fq%5B%5D=seats%3A40',
] ) {
	test( `a search page opened with ${ query } shifts under 0.1`, async ( {
		page,
		seed,
	} ) => {
		await page.addInitScript( () => {
			window.shifted = 0;
			new window.PerformanceObserver( ( list ) => {
				for ( const entry of list.getEntries() ) {
					if ( ! entry.hadRecentInput ) {
						window.shifted += entry.value;
					}
				}
			} ).observe( { type: 'layout-shift', buffered: true } );
		} );
		await page.goto( `/?page_id=${ seed.searchPageId }&${ query }` );
		await expect(
			page.locator( '.viglet-dumont-search__summary' )
		).toHaveText( /results?$/ );
		await page.waitForLoadState( 'networkidle' );
		expect( await page.evaluate( () => window.shifted ) ).toBeLessThan(
			0.1
		);
	} );
}
