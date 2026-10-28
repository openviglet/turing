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
	// and that is the search this feature exists to spare the shopper.
	await understood( page ).getByRole( 'button', { name: 'Undo' } ).click();
	await expect( box( page ) ).toHaveValue( 'jacket under $100' );
	await expect( understood( page ) ).toBeHidden();
	await expect( summary( page ) ).toHaveText(
		'No results found. Try different keywords.'
	);
	expect( new URL( page.url() ).searchParams.getAll( 'fq[]' ) ).not.toContain(
		'price:[* TO 100]'
	);
} );

test( "Turing's reading and the price read in code apply together", async ( {
	page,
} ) => {
	await page.route( '**/nl-filters', ( route ) =>
		route.fulfill( {
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
		} )
	);
	await page.goto(
		`/?pagename=find-gear&q=${ encodeURIComponent(
			'red jacket under $200'
		) }`
	);

	await expect( understood( page ) ).toContainText(
		'Understood: Color: Red, Price: ≤ 200'
	);
	await expect( summary( page ) ).toHaveText( '2 results' );
	await expect( titles( page ) ).toHaveText( [
		/Rain jacket|Linen jacket/,
		/Rain jacket|Linen jacket/,
	] );
	await expect( box( page ) ).toHaveValue( 'jacket' );
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
