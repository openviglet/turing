// The demo storefront's curated searches (D194), on the `demo` project's stack (Tarnfell, its
// catalog from demo/seed/catalog.php): each search in demo/theme/tarnfell/curated.json shows one feature,
// and here each still returns what it is there to show, so the demo cannot rot silently.
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { expect, test } from '../fixtures.mjs';

const curated = JSON.parse(
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
);

const block = ( page ) => page.locator( '.viglet-dumont-search' );

for ( const search of curated ) {
	test( `"${ search.query }": ${ search.shows }`, async ( { page } ) => {
		const params = new URLSearchParams( { q: search.query } );
		if ( search.filter ) {
			params.append( 'fq[]', search.filter );
		}
		params.set( 'pagename', 'search-gear' );
		await page.goto( `/?${ params }` );

		if ( search.correction ) {
			await expect(
				block( page ).locator( '.viglet-dumont-search__correction' )
			).toContainText( search.correction );
			return;
		}
		await expect(
			block( page ).locator( '.viglet-dumont-search__title a' ).first()
		).toHaveText( new RegExp( `^${ search.first }\\b` ) );
		if ( search.then ) {
			await expect(
				block( page )
					.locator( '.viglet-dumont-search__title a' )
					.nth( 1 )
			).toHaveText( new RegExp( `^${ search.then }\\b` ) );
		}
		if ( search.count ) {
			await expect(
				block( page ).locator( '.viglet-dumont-search__summary' )
			).toHaveText( `${ search.count } results` );
		}
	} );
}
