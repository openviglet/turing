// The compare block's summary (D200) against a real LLM: two of the woo seed's jackets, and a
// summary that says nothing the table does not. The plugin drops a summary holding a figure
// the table lacks, so a summary that shows has passed that check; this one reads it again.
import { wp } from '../global-setup.mjs';
import { expect, test } from '../fixtures.mjs';
import { record } from './helpers.mjs';

test.beforeAll( () => {
	wp( [ 'plugin', 'activate', 'woocommerce' ] );
	wp( [ 'eval-file', '/e2e/woo-seed.php' ] );
} );

// The default stack again: the next run's global setup would also see to it.
test.afterAll( () => {
	try {
		wp( [ 'plugin', 'deactivate', 'woocommerce' ] );
	} catch {}
} );

test( "two jackets' summary names nothing the table does not", async ( {
	page,
	look,
} ) => {
	await page.goto( '/' );
	const ids = await page.evaluate( async () => {
		const response = await window.fetch(
			'/?rest_route=/wp/v2/product&search=jacket&per_page=10&_fields=id,title'
		);
		return ( await response.json() )
			.filter( ( p ) => /^(Rain|Wool) jacket$/.test( p.title.rendered ) )
			.map( ( p ) => p.id );
	} );
	expect( ids ).toHaveLength( 2 );

	const started = Date.now();
	await page.goto(
		`/?pagename=compare-products&compare=${ ids.join( ',' ) }`
	);
	const text = page.locator( '.viglet-dumont-compare__summary-text' );
	await expect( text ).not.toBeEmpty( { timeout: 60000 } );
	const summary = await text.textContent();
	record(
		'compare: two jackets',
		'(the table)',
		summary,
		Date.now() - started
	);
	await look( 'ai-compare' );

	const table = await page
		.locator( '.viglet-dumont-compare__table' )
		.textContent();
	for ( const figure of summary.match( /\d+(?:[.,]\d+)*/g ) || [] ) {
		expect( table, `the summary's ${ figure }` ).toContain( figure );
	}
	expect( summary ).not.toMatch( /\b(best|better|recommend|winner)\b/i );
} );
