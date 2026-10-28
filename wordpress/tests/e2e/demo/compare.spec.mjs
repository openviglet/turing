// The demo's "Compare searches" page (D195): WordPress's own search beside Turing's on one query.
// For every example the page offers, Turing's column holds the result it is curated for, and
// WordPress's column is read, not assumed: curated.json records what it shows (`native`, one of
// its results, or null for nothing), so a page that stopped showing a difference fails. The page
// offers the examples in the demo's language (D217).
import { expect, test } from '../fixtures.mjs';
import { curated, demoLocale, localeOf } from './curated.mjs';

const examples = curated.filter( ( example ) => 'native' in example );

test( 'the page offers each comparable curated search as a link', async ( {
	page,
	axe,
} ) => {
	await page.goto( '/?pagename=compare-searches' );
	await expect( page.locator( '.tarnfell-compare__examples a' ) ).toHaveText(
		examples
			.filter( ( example ) => localeOf( example ) === demoLocale() )
			.map( ( example ) => example.query )
	);
	await axe( 'the compare page', { include: '.tarnfell-compare' } );
} );

for ( const example of examples ) {
	test( `${ localeOf( example ) } "${
		example.query
	}" side by side`, async ( { page } ) => {
		test.skip(
			localeOf( example ) !== demoLocale(),
			`The demo speaks ${ demoLocale() }.`
		);
		await page.goto(
			`/?pagename=compare-searches&q=${ encodeURIComponent(
				example.query
			) }`
		);
		const native = page.locator( '.tarnfell-compare__native' );
		const turing = page.locator( '.tarnfell-compare__turing' );
		await expect(
			turing.locator( '.viglet-dumont-search__title a' ).first()
		).toHaveText( new RegExp( `^${ example.first }\\b` ) );
		if ( null === example.native ) {
			await expect( native ).toContainText(
				'pt_BR' === demoLocale()
					? 'Nenhum resultado encontrado.'
					: 'No results found.'
			);
		} else {
			// Among its results, not first: WordPress breaks ties by date, and seeded posts
			// share one.
			await expect(
				native.locator( 'li' ).filter( {
					hasText: new RegExp( `^${ example.native }\\b` ),
				} )
			).not.toHaveCount( 0 );
		}
	} );
}
