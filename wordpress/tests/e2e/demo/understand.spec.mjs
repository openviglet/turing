// Text to filters (D199) on the demo storefront: its search page applies the filters a
// sentence names. Turing's nl-filters is answered as unavailable, so what is checked here is
// the part read in code, which works with no language model at all: a price marked as money,
// and a size grounded on the demo's own Size facet. Each language's sentences are checked
// when the demo speaks it (DEMO_LOCALE, D217).
import { expect, test } from '../fixtures.mjs';
import { demoLocale } from './curated.mjs';

const block = ( page ) => page.locator( '.viglet-dumont-search' );

const sentences = [
	{
		locale: 'en_US',
		query: 'waterproof jacket size M under $200',
		fq: [ 'pa_size:M', 'price:[* TO 200]' ],
		understood: 'Understood: Size: M, Price: ≤ 200',
		left: 'waterproof jacket',
	},
	{
		locale: 'en_US',
		query: 'hiking boot size 42 between $100 and $250',
		fq: [ 'pa_size:42', 'price:[100 TO 250]' ],
		understood: 'Understood: Size: 42, Price: 100 – 250',
		left: 'hiking boot',
	},
	// A size the catalog does not carry sets nothing: it is never invented.
	{
		locale: 'en_US',
		query: 'jacket size XXXL',
		fq: [],
	},
	{
		locale: 'pt_BR',
		query: 'jaqueta impermeável tamanho M até R$ 1.000',
		fq: [ 'pa_size:M', 'price:[* TO 1000]' ],
		understood: 'Entendido: Tamanho: M, Price: ≤ 1000',
		left: 'jaqueta impermeável',
	},
];

for ( const sentence of sentences ) {
	test( `${ sentence.locale } "${ sentence.query }" applies ${
		sentence.fq.join( ' and ' ) || 'nothing'
	}`, async ( { page } ) => {
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
		const understood = block( page ).locator(
			'.viglet-dumont-search__understood'
		);
		if ( ! sentence.fq.length ) {
			await expect(
				block( page ).locator( '.viglet-dumont-search__summary' )
			).not.toBeEmpty();
			await expect( understood ).toBeHidden();
			expect(
				new URL( page.url() ).searchParams.getAll( 'fq[]' )
			).toEqual( [] );
			return;
		}
		await expect( understood ).toContainText( sentence.understood );
		await expect( page.locator( 'input[type="search"]' ) ).toHaveValue(
			sentence.left
		);
		expect( new URL( page.url() ).searchParams.getAll( 'fq[]' ) ).toEqual(
			expect.arrayContaining( sentence.fq )
		);
		await expect(
			block( page ).locator( '.viglet-dumont-search__title a' ).first()
		).toBeVisible();
	} );
}
