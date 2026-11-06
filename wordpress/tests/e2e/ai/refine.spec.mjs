// The filters a sentence names (D170) against a real LLM: a date phrase becomes a Published
// chip that narrows the results, and a keyword lookup gets none. Skipped, with the reason,
// while D169's probe reports natural-language filters off.
import { expect, test } from '../fixtures.mjs';
import { feature, record } from './helpers.mjs';

const block = ( page ) => page.locator( '.viglet-dumont-search' );
const suggested = ( page ) =>
	block( page ).locator( '.viglet-dumont-search__chip--suggested' );

test.beforeEach( () => {
	const nl = feature( 'nl_filters' );
	test.skip( ! nl.on, `natural-language filters are off: ${ nl.reason }` );
} );

test( 'a date phrase is offered as a Published filter that narrows the results', async ( {
	page,
	seed,
	look,
} ) => {
	const sentence = 'posts about wordpress published this year';
	const started = Date.now();
	await page.goto(
		`/?page_id=${ seed.askPageId }&q=${ encodeURIComponent( sentence ) }`
	);
	const chip = suggested( page ).filter( { hasText: 'Published:' } );
	await expect( chip ).toHaveCount( 1 );
	record(
		'nl filters: this year',
		sentence,
		await chip.textContent(),
		Date.now() - started
	);
	await look( 'ai-suggested-filters' );

	await chip.click();
	await expect(
		block( page ).locator(
			'.viglet-dumont-search__applied .viglet-dumont-search__chip'
		)
	).toContainText( 'Published:' );
	await expect( page.locator( 'input[type="search"]' ) ).not.toHaveValue(
		sentence
	);
} );

test( 'a keyword lookup gets no suggested filter', async ( { page, seed } ) => {
	await page.goto( `/?page_id=${ seed.askPageId }&q=workshop` );
	await page.waitForLoadState( 'networkidle' );
	await expect( suggested( page ) ).toHaveCount( 0 );
} );
