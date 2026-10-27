// The demo's "Compare searches" page (D195): WordPress's own search beside Turing's on one query.
// For every example the page offers, Turing's column holds the result it is curated for, and
// WordPress's column is read, not assumed: curated.json records what it shows (`native`, its
// first result, or null for nothing), so a page that stopped showing a difference fails.
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { expect, test } from '../fixtures.mjs';

const examples = JSON.parse(
	readFileSync(
		join(
			import.meta.dirname,
			'..',
			'..',
			'..',
			'demo',
			'theme',
			'tarnfell',
			'curated.json'
		),
		'utf8'
	)
).filter( ( example ) => 'native' in example );

test( 'the page offers each comparable curated search as a link', async ( {
	page,
	axe,
} ) => {
	await page.goto( '/?pagename=compare-searches' );
	await expect( page.locator( '.tarnfell-compare__examples a' ) ).toHaveText(
		examples.map( ( example ) => example.query )
	);
	await axe( 'the compare page', { include: '.tarnfell-compare' } );
} );

for ( const example of examples ) {
	test( `"${ example.query }" side by side`, async ( { page } ) => {
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
			await expect( native ).toContainText( 'No results found.' );
		} else {
			await expect( native.locator( 'li' ).first() ).toHaveText(
				new RegExp( `^${ example.native }\\b` )
			);
		}
	} );
}
