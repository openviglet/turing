// Close matches (D258) on the demo storefront: a sentence whose every word, all required, finds
// nothing is searched again for any of them, with a notice, so the page never says there is no
// product while the answer above it recommends one. Turing's nl-filters is answered as
// unavailable, so the sentence's words are the whole search. Each language's sentences are
// checked when the demo speaks it (DEMO_LOCALE, D217); each was measured on the demo to find
// nothing as typed.
import { expect, test } from '../fixtures.mjs';
import { demoLocale } from './curated.mjs';

const block = ( page ) => page.locator( '.viglet-dumont-search' );

const sentences = [
	{
		locale: 'en_US',
		query: 'waterproof hiking boots for wet hills',
		finds: 'Scafell waterproof hiking boot',
		notice: 'Showing close matches',
	},
	{
		locale: 'en_US',
		query: 'warm jacket for a cold night camping',
		finds: 'Blencathra down jacket',
		notice: 'Showing close matches',
	},
	{
		locale: 'pt_BR',
		query: 'bota de trilha impermeável para morros molhados',
		finds: 'Bota de trilha impermeável Scafell',
		notice: 'Mostrando resultados próximos',
	},
];

for ( const sentence of sentences ) {
	test( `${ sentence.locale } "${ sentence.query }" lists close matches`, async ( {
		page,
	} ) => {
		test.skip(
			sentence.locale !== demoLocale(),
			`The demo speaks ${ demoLocale() }: DEMO_LOCALE=${ sentence.locale } run-docker.cmd demo checks this one.`
		);
		await page.route( '**/nl-filters', ( route ) =>
			route.fulfill( { json: { available: false } } )
		);
		await page.goto(
			`/?pagename=search-gear&q=${ encodeURIComponent( sentence.query ) }`
		);
		await expect(
			block( page ).locator( '.viglet-dumont-search__close-matches' )
		).toContainText( sentence.notice );
		await expect(
			block( page ).locator( '.viglet-dumont-search__results' )
		).toContainText( sentence.finds );
		// The box keeps the words as the shopper typed them, never the OR it searched.
		await expect( page.locator( 'input[type="search"]' ) ).toHaveValue(
			sentence.query
		);
		await expect(
			block( page ).locator( '.viglet-dumont-search__empty' )
		).toBeHidden();
	} );
}

test( 'a search that finds something as typed is no close match', async ( {
	page,
} ) => {
	// A product's own name, the same in either language the demo speaks.
	await page.goto(
		`/?pagename=search-gear&q=${ encodeURIComponent( 'Scafell' ) }`
	);
	await expect(
		block( page ).locator( '.viglet-dumont-search__results li' ).first()
	).toBeVisible();
	await expect(
		block( page ).locator( '.viglet-dumont-search__close-matches' )
	).toBeHidden();
} );
