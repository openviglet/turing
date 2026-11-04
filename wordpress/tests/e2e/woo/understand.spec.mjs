// Text to filters (D199) on the `woo` project's shop: with "Apply them instead of suggesting"
// on, the filters a sentence names are applied, a line says what was understood, and Undo puts
// the search back. A price is read in code, so it filters with no language model at all.
// Turing's nl-filters is answered by a fixed reading so the suite needs no LLM.
import { expect, test } from '../fixtures.mjs';

const block = ( page ) => page.locator( '.viglet-dumont-search' );
const summary = ( page ) =>
	block( page ).locator( '.viglet-dumont-search__summary' );
const understood = ( page ) =>
	block( page ).locator( '.viglet-dumont-search__understood' );
const titles = ( page ) =>
	block( page ).locator( '.viglet-dumont-search__title a' );
const box = ( page ) => page.locator( 'input[type="search"]' );

test( 'a price a sentence names filters with no language model, and Undo puts it back', async ( {
	page,
	look,
	axe,
} ) => {
	await page.route( '**/nl-filters', ( route ) =>
		route.fulfill( { json: { available: false } } )
	);
	await page.goto(
		`/?pagename=find-gear&q=${ encodeURIComponent( 'jacket under $100' ) }`
	);

	await expect( understood( page ) ).toContainText(
		'Understood: Price: ≤ 100'
	);
	await expect( summary( page ) ).toHaveText( '2 results' );
	await expect( titles( page ) ).toHaveText( [
		/Wool jacket|Linen jacket/,
		/Wool jacket|Linen jacket/,
	] );
	// The words the price was read from leave the query.
	await expect( box( page ) ).toHaveValue( 'jacket' );
	expect( new URL( page.url() ).searchParams.getAll( 'fq[]' ) ).toContain(
		'price:[* TO 100]'
	);
	await look( 'woo-understood' );
	await axe( 'the search block saying what it understood', {
		include: '.viglet-dumont-search',
	} );

	// Undo is the search as typed: every word back as a keyword, which here finds nothing,
	// and that is the search this feature exists to spare the shopper. Since D258 the block
	// then lists close matches, any of the words, and says so.
	await understood( page ).getByRole( 'button', { name: 'Undo' } ).click();
	await expect( box( page ) ).toHaveValue( 'jacket under $100' );
	await expect( understood( page ) ).toBeHidden();
	await expect(
		block( page ).locator( '.viglet-dumont-search__close-matches' )
	).toContainText( 'Showing close matches' );
	expect( new URL( page.url() ).searchParams.getAll( 'fq[]' ) ).not.toContain(
		'price:[* TO 100]'
	);
} );

test( "Turing's reading and the price read in code apply together", async ( {
	page,
} ) => {
	const read = [];
	await page.route( '**/nl-filters', ( route ) => {
		read.push( route.request().postDataJSON() );
		return route.fulfill( {
			json: {
				available: true,
				text: 'red jacket under $200',
				// Turing read the colour and left the price in the words.
				q: 'jacket under $200',
				filters: [
					{
						field: 'pa_color',
						values: [ 'Red' ],
						grounded: true,
						confidence: 1,
					},
				],
				fq: [ 'pa_color:Red' ],
				dropped: [],
			},
		} );
	} );
	await page.goto(
		`/?pagename=find-gear&q=${ encodeURIComponent(
			'red jacket under $200'
		) }`
	);

	await expect( understood( page ) ).toContainText(
		'Understood: Color: Red, Price: ≤ 200'
	);
	// Its values are checked against this site's documents only (D287).
	expect( Object.keys( read[ 0 ].filters ) ).toEqual( [ 'site_key' ] );
	await expect( summary( page ) ).toHaveText( '2 results' );
	await expect( titles( page ) ).toHaveText( [
		/Rain jacket|Linen jacket/,
		/Rain jacket|Linen jacket/,
	] );
	await expect( box( page ) ).toHaveValue( 'jacket' );
} );

test( 'a price Turing read but the block dropped leaves none of its words behind', async ( {
	page,
} ) => {
	// D285: Turing takes only the amount out of its query, and its price comes back ungrounded,
	// so the block applies its own reading of "under $100" instead.
	await page.route( '**/nl-filters', ( route ) =>
		route.fulfill( {
			json: {
				available: true,
				text: 'jacket under $100',
				q: 'jacket under $',
				filters: [
					{
						field: 'price',
						values: [],
						rangeEnd: '100',
						sourceWords: [ '100' ],
						grounded: false,
						range: true,
						confidence: 1,
					},
				],
				fq: [ 'price:[* TO 100]' ],
				dropped: [],
			},
		} )
	);
	await page.goto(
		`/?pagename=find-gear&q=${ encodeURIComponent( 'jacket under $100' ) }`
	);

	await expect( understood( page ) ).toContainText(
		'Understood: Price: ≤ 100'
	);
	await expect( box( page ) ).toHaveValue( 'jacket' );
	await expect( summary( page ) ).toHaveText( '2 results' );
	await expect(
		block( page ).locator( '.viglet-dumont-search__close-matches' )
	).toBeHidden();
} );

/**
 * The structured searches the block sends to Turing, in order, as their JSON bodies.
 *
 * @param {import('@playwright/test').Page} page The page.
 * @return {Object[]} Filled as the page searches.
 */
function searches( page ) {
	const sent = [];
	page.on( 'request', ( request ) => {
		if (
			'POST' === request.method() &&
			/\/api\/sn\/[^/]+\/search\/query/.test( request.url() )
		) {
			sent.push( request.postDataJSON() );
		}
	} );
	return sent;
}

test( 'a sentence whose filters are read is counted once, refined, never as typed', async ( {
	page,
} ) => {
	// D267: the typed words find nothing, and counted they would read as a failed search.
	await page.route( '**/nl-filters', ( route ) =>
		route.fulfill( { json: { available: false } } )
	);
	const sent = searches( page );
	await page.goto(
		`/?pagename=find-gear&q=${ encodeURIComponent( 'jacket under $100' ) }`
	);
	await expect( summary( page ) ).toHaveText( '2 results' );

	const typed = sent.filter( ( body ) => 'jacket under $100' === body.q );
	// The page of results, not the one-row probes the block makes beside it.
	const refined = sent.filter(
		( body ) => 'jacket' === body.q && 'standard' === body.mode
	);
	expect( typed.length ).toBeGreaterThan( 0 );
	expect( typed.every( ( body ) => false === body.populateMetrics ) ).toBe(
		true
	);
	expect( refined ).toHaveLength( 1 );
	expect( refined[ 0 ] ).not.toHaveProperty( 'populateMetrics' );
	// D294: the one-row probe that learns the unfiltered facets is never counted.
	const probes = sent.filter( ( body ) => 1 === body.rows );
	expect( probes.length ).toBeGreaterThan( 0 );
	expect( probes.every( ( body ) => false === body.populateMetrics ) ).toBe(
		true
	);
} );

test( 'a sentence nothing is read from is counted as typed', async ( {
	page,
} ) => {
	await page.route( '**/nl-filters', ( route ) =>
		route.fulfill( { json: { available: false } } )
	);
	const sent = searches( page );
	const typed = 'jacket under 2 kg for hiking';
	await page.goto(
		`/?pagename=find-gear&q=${ encodeURIComponent( typed ) }`
	);
	await expect
		.poll( () =>
			sent.some(
				( body ) => typed === body.q && ! ( 'populateMetrics' in body )
			)
		)
		.toBe( true );
	expect( sent[ 0 ] ).toMatchObject( { q: typed, populateMetrics: false } );
} );

test( 'an amount that is not money is not read as a price', async ( {
	page,
} ) => {
	await page.route( '**/nl-filters', ( route ) =>
		route.fulfill( { json: { available: false } } )
	);
	await page.goto(
		`/?pagename=find-gear&q=${ encodeURIComponent(
			'jacket under 2 kg for hiking'
		) }`
	);
	await expect( summary( page ) ).toBeVisible();
	await expect( understood( page ) ).toBeHidden();
	expect( new URL( page.url() ).searchParams.getAll( 'fq[]' ) ).toEqual( [] );
} );
