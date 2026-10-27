// Product comparison (D200), on the `woo` project's stack (fixtures/woo-seed.php): two jackets
// ticked in the search results open side by side, the rows where they differ marked.
import { expect, test } from '../fixtures.mjs';

const block = ( page ) => page.locator( '.viglet-dumont-search' );
const card = ( page, name ) =>
	block( page )
		.locator( '.viglet-dumont-search__results li' )
		.filter( { hasText: name } );
const jackets = ( seed ) =>
	`/?page_id=${ seed.searchPageId }&q=jacket&fq[]=type:product`;

test( 'two jackets ticked in the results compare side by side, their differences marked', async ( {
	page,
	seed,
	look,
	axe,
} ) => {
	await page.goto( jackets( seed ) );
	const bar = block( page ).locator( '.viglet-dumont-search__compare-bar' );
	await card( page, 'Rain jacket' )
		.getByRole( 'checkbox', { name: 'Compare Rain jacket' } )
		.check();
	// One is not a comparison.
	await expect( bar ).toBeHidden();
	await card( page, 'Wool jacket' )
		.getByRole( 'checkbox', { name: 'Compare Wool jacket' } )
		.check();
	await expect( bar.getByRole( 'link' ) ).toHaveText( 'Compare 2 products' );

	await bar.getByRole( 'link' ).click();
	const table = page.locator( '.viglet-dumont-compare__table' );
	await expect( table.locator( 'thead th' ) ).toHaveText( [
		'Rain jacket',
		'Wool jacket',
	] );
	const row = ( label ) =>
		table.locator( 'tbody tr' ).filter( {
			has: page.locator( 'th', { hasText: label } ),
		} );
	await expect( row( 'Price' ) ).toHaveClass( /differs/ );
	await expect( row( 'Price' ).locator( 'td' ) ).toHaveText( [
		/149\.90/,
		/89\.00/,
	] );
	await expect( row( 'Color' ) ).toHaveClass( /differs/ );
	await expect( row( 'Color' ).locator( 'td' ) ).toHaveText( [
		'Red',
		'Blue',
	] );
	// Both in stock: the same, so not marked.
	await expect( row( 'Availability' ) ).not.toHaveClass( /differs/ );
	await look( 'woo-compare' );
	await axe( 'the compare block', { include: '.viglet-dumont-compare' } );

	// The choice is kept for the tab: back in the results, both are still ticked.
	await page.goBack();
	await expect(
		card( page, 'Wool jacket' ).getByRole( 'checkbox' )
	).toBeChecked();
} );

/**
 * A jacket's id, read through the Store API.
 *
 * @param {import('@playwright/test').Page} page A page of the site.
 * @param {string}                          name The jacket's name.
 */
async function productId( page, name ) {
	const response = await page.request.get(
		`/?rest_route=/wc/store/v1/products&search=${ encodeURIComponent(
			name
		) }`
	);
	return ( await response.json() )[ 0 ].id;
}

test( 'one jacket ticked in the results and one in a product page’s Related compare together', async ( {
	page,
	seed,
} ) => {
	// D212: Related's product cards tick into the list the search cards use.
	await page.goto( jackets( seed ) );
	await card( page, 'Wool jacket' )
		.getByRole( 'checkbox', { name: 'Compare Wool jacket' } )
		.check();

	await page.goto( `/?p=${ await productId( page, 'Rain jacket' ) }` );
	const related = page.locator( '.wp-block-viglet-dumont-related' );
	const boxes = related.getByRole( 'checkbox', { name: /^Compare / } );
	await expect( boxes.first() ).toBeVisible();
	const bar = related.locator( '.viglet-dumont-related__compare-bar' );
	await expect( bar ).toBeHidden();
	const other = boxes.and(
		related.locator( 'input:not([aria-label="Compare Wool jacket"])' )
	);
	const name = ( await other.first().getAttribute( 'aria-label' ) ).replace(
		/^Compare /,
		''
	);
	await other.first().check();
	await expect( bar.getByRole( 'link' ) ).toHaveText( 'Compare 2 products' );

	await bar.getByRole( 'link' ).click();
	await expect(
		page.locator( '.viglet-dumont-compare__table thead th' )
	).toHaveText( [ 'Wool jacket', name ] );
} );

test( 'a product the copilot cites is ticked for the search results too', async ( {
	page,
	seed,
} ) => {
	// D212: the copilot's product items tick into the same list.
	const rain = await productId( page, 'Rain jacket' );
	await page.route( '**/copilot/available', ( route ) =>
		route.fulfill( { json: { available: true } } )
	);
	await page.route( /\/copilot$/, ( route ) =>
		route.fulfill( {
			json: {
				available: true,
				answer: 'The Rain jacket is waterproof [1].',
				citations: [
					{
						rank: 1,
						id: `localhost:post:${ rain }`,
						title: 'Rain jacket',
						url: `/?p=${ rain }`,
					},
				],
				totalHits: 1,
			},
		} )
	);
	await page.goto( '/?pagename=ask-the-shop' );
	const copilot = page.locator( '.viglet-dumont-copilot' );
	await copilot
		.getByLabel( 'What are you looking for?' )
		.fill( 'a waterproof jacket' );
	await copilot.getByRole( 'button', { name: 'Ask', exact: true } ).click();
	await copilot
		.getByRole( 'checkbox', { name: 'Compare Rain jacket' } )
		.check();
	// One is not a comparison.
	await expect(
		copilot.locator( '.viglet-dumont-copilot__compare-bar' )
	).toBeHidden();

	await page.goto( jackets( seed ) );
	await expect(
		card( page, 'Rain jacket' ).getByRole( 'checkbox' )
	).toBeChecked();
} );

test( 'the compare page without products says how to choose them', async ( {
	page,
} ) => {
	await page.goto( '/?pagename=compare-products' );
	await expect( page.locator( '.viglet-dumont-compare__empty' ) ).toHaveText(
		/Tick Compare on two to four products/
	);
} );

// The summary above the table (D200). Turing's answer is replayed here; the live one is the
// `ai` project's (ai/compare.spec.mjs).
const answerWith = ( page, json ) =>
	page.route(
		( url ) => decodeURIComponent( url.href ).includes( 'compare/summary' ),
		( route ) => route.fulfill( { json } )
	);

const compareJackets = async ( page ) => {
	const ids = await page.evaluate( async () => {
		const response = await window.fetch(
			'/?rest_route=/wp/v2/product&search=jacket&per_page=10&_fields=id,title'
		);
		return ( await response.json() ).map( ( p ) => p.id );
	} );
	await page.goto(
		`/?pagename=compare-products&compare=${ ids.join( ',' ) }`
	);
};

test( 'a summary Turing wrote shows above the table, labelled as AI', async ( {
	page,
} ) => {
	await page.goto( '/' );
	await answerWith( page, {
		summary: 'Both jackets are in stock; they differ in price and color.',
		error: '',
	} );
	await compareJackets( page );
	const summary = page.locator( '.viglet-dumont-compare__summary' );
	await expect( summary ).toBeVisible();
	await expect( summary ).toContainText(
		'Summary written by AI from the table below'
	);
	await expect(
		summary.locator( '.viglet-dumont-compare__summary-text' )
	).toHaveText(
		'Both jackets are in stock; they differ in price and color.'
	);
} );

test( 'without a summary the table stands alone', async ( { page } ) => {
	await page.goto( '/' );
	await answerWith( page, {
		summary: '',
		error: 'No default LLM is configured.',
	} );
	await compareJackets( page );
	await expect(
		page.locator( '.viglet-dumont-compare__table' )
	).toBeVisible();
	await page.waitForLoadState( 'networkidle' );
	await expect(
		page.locator( '.viglet-dumont-compare__summary' )
	).toBeHidden();
} );
