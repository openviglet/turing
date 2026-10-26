// The filters a sentence names (D170), on the Ask page, with Turing's nl-filters answered by a
// fixed reading so the suite needs no LLM: the block offers the grounded filters on fields the
// visitor can see, applies one only on a click, and a lookup asks nothing. ai/refine.spec.mjs
// asks the real one.
import { expect, test } from '../fixtures.mjs';

const block = ( page ) => page.locator( '.viglet-dumont-search' );
const summary = ( page ) =>
	block( page ).locator( '.viglet-dumont-search__summary' );
const suggested = ( page ) =>
	block( page ).locator( '.viglet-dumont-search__chip--suggested' );
const inBlock = { include: '.viglet-dumont-search' };

const reading = {
	available: true,
	text: 'posts from the first days of january',
	q: '',
	filters: [
		{
			field: 'publication_date',
			values: [],
			grounded: true,
			confidence: 1,
		},
		{ field: 'seats', values: [ '40' ], grounded: false, confidence: 0.3 },
		{ field: 'author', values: [ 'admin' ], grounded: true, confidence: 1 },
	],
	fq: [
		'publication_date:[2026-01-01 TO 2026-01-06]',
		'seats:40',
		'author:admin',
	],
	dropped: [],
};

test( 'a sentence offers the filters it names, and a click applies one', async ( {
	page,
	seed,
	look,
	axe,
} ) => {
	const asked = [];
	await page.route( '**/nl-filters', async ( route ) => {
		asked.push( route.request().postDataJSON() );
		await route.fulfill( { json: reading } );
	} );
	await page.goto(
		`/?page_id=${ seed.askPageId }&q=${ encodeURIComponent(
			reading.text
		) }`
	);

	// Only the grounded filter on a field the visitor can filter by; never applied by itself.
	await expect( suggested( page ) ).toHaveCount( 1 );
	await expect( suggested( page ) ).toContainText(
		/Published: Jan 1, 2026 – Jan 6, 2026/
	);
	expect( asked ).toEqual( [ { text: reading.text, locale: 'en_US' } ] );
	await expect(
		block( page ).locator(
			'.viglet-dumont-search__chip:not(.viglet-dumont-search__chip--suggested)'
		)
	).toHaveCount( 0 );
	await look( 'search-block-suggested' );
	await axe( 'search block suggesting a filter', inBlock );

	await suggested( page ).click();
	await expect( summary( page ) ).toHaveText( '5 results' );
	await expect( suggested( page ) ).toHaveCount( 0 );
	const chip = block( page ).locator(
		'.viglet-dumont-search__applied .viglet-dumont-search__chip'
	);
	await expect( chip ).toContainText(
		'Published: Jan 1, 2026 – Jan 6, 2026'
	);
	// The words Turing read as the filter leave the query.
	await expect( page.locator( 'input[type="search"]' ) ).toHaveValue( '' );
	expect( new URL( page.url() ).searchParams.getAll( 'fq[]' ) ).toEqual( [
		'publication_date:[2026-01-01 TO 2026-01-06]',
	] );

	await chip.click();
	await expect( chip ).toHaveCount( 0 );
	await expect( summary( page ) ).not.toHaveText( '5 results' );
} );

test( 'a lookup asks for no reading', async ( { page, seed } ) => {
	let asked = 0;
	await page.route( '**/nl-filters', async ( route ) => {
		asked++;
		await route.fulfill( { json: reading } );
	} );
	await page.goto( `/?page_id=${ seed.askPageId }&q=workshop` );
	await expect( summary( page ) ).toHaveText( '4 results' );
	await page.waitForLoadState( 'networkidle' );
	expect( asked ).toBe( 0 );
	await expect( suggested( page ) ).toHaveCount( 0 );
} );

test( 'with no language model in Turing nothing is suggested', async ( {
	page,
	seed,
} ) => {
	await page.route( '**/nl-filters', ( route ) =>
		route.fulfill( {
			json: { ...reading, available: false, filters: [], fq: [] },
		} )
	);
	await page.goto(
		`/?page_id=${ seed.askPageId }&q=${ encodeURIComponent(
			reading.text
		) }`
	);
	await expect( summary( page ) ).toHaveText( /results/ );
	await page.waitForLoadState( 'networkidle' );
	await expect(
		block( page ).locator( '.viglet-dumont-search__suggested' )
	).toBeHidden();
} );
