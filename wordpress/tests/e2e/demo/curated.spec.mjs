// The demo storefront's curated searches (D194), on the `demo` project's stack (Tarnfell, its
// catalog from demo/seed/catalog.php): each search in demo/theme/tarnfell/curated.json shows one feature,
// and here each still returns what it is there to show, so the demo cannot rot silently. Each
// language's searches are checked when the demo speaks it (DEMO_LOCALE, D217).
import { expect, test } from '../fixtures.mjs';
import { curated, demoLocale, localeOf } from './curated.mjs';

const block = ( page ) => page.locator( '.viglet-dumont-search' );

for ( const search of curated ) {
	test( `${ localeOf( search ) } "${ search.query }": ${
		search.shows
	}`, async ( { page } ) => {
		test.skip(
			localeOf( search ) !== demoLocale(),
			`The demo speaks ${ demoLocale() }: DEMO_LOCALE=${ localeOf(
				search
			) } run-docker.cmd demo checks this one.`
		);
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
		// Every result shows its picture (D218): drawn by demo/seed/images.php.
		const cards = block( page ).locator(
			'.viglet-dumont-search__results > li'
		);
		for ( const card of await cards.all() ) {
			await expect(
				card.locator( '.viglet-dumont-search__image' )
			).toHaveAttribute( 'src', /\.webp$/ );
		}
		if ( search.count ) {
			await expect(
				block( page ).locator( '.viglet-dumont-search__summary' )
			).toHaveText( new RegExp( `^${ search.count } ` ) );
		}
	} );
}
