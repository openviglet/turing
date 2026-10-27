// "How it works" (D195), on the `demo` project's stack: with ?tour=1 each Turing feature on the
// page gets a numbered marker. At phone width, every marker is reached with Tab, opens its note
// with Enter, and the note covers nothing of the feature it describes; Escape closes it and
// gives the focus back.
import { expect, test } from '../fixtures.mjs';

test.use( { viewport: { width: 390, height: 844 } } );

const boxes = ( a, b ) =>
	a.x < b.x + b.width &&
	b.x < a.x + a.width &&
	a.y < b.y + b.height &&
	b.y < a.y + a.height;

async function walk( page, expected ) {
	const markers = page.locator( '.tarnfell-tour__marker' );
	await expect( markers ).toHaveCount( expected.length );
	for ( let i = 0; i < expected.length; i++ ) {
		const marker = markers.nth( i );
		await expect( marker ).toHaveAccessibleName(
			`Turing feature ${ i + 1 }: ${ expected[ i ] }`
		);
		// Reached with the keyboard: Tab until this marker has the focus.
		for (
			let tabs = 0;
			tabs < 80 &&
			! ( await marker.evaluate(
				( el ) => el === el.ownerDocument.activeElement
			) );
			tabs++
		) {
			await page.keyboard.press( 'Tab' );
		}
		await expect( marker ).toBeFocused();
		await page.keyboard.press( 'Enter' );
		const note = page.locator(
			`#${ await marker.getAttribute( 'aria-controls' ) }`
		);
		await expect( note ).toBeVisible();
		await expect(
			note.getByRole( 'link', { name: 'How it works' } )
		).toBeVisible();
		// The feature is the element after the marker's wrapper.
		const feature = await marker.evaluate( ( el ) => {
			const r =
				el.parentElement.nextElementSibling.getBoundingClientRect();
			return { x: r.x, y: r.y, width: r.width, height: r.height };
		} );
		expect(
			boxes( await note.boundingBox(), feature ),
			'the note covers nothing of its feature'
		).toBe( false );
		await page.keyboard.press( 'Escape' );
		await expect( note ).toBeHidden();
		await expect( marker ).toBeFocused();
	}
}

test( 'the search page names its search box and its filters', async ( {
	page,
	axe,
} ) => {
	await page.goto( '/?pagename=search-gear&q=jacket&tour=1' );
	await expect(
		page.locator( '.viglet-dumont-search__title a' ).first()
	).toBeVisible();
	await walk( page, [ 'Search as you type', 'Filters from the catalog' ] );
	await axe( 'the tour', { include: '.tarnfell-tour' } );
} );

test( 'a product page names Related, and the catalog copilot where it shows', async ( {
	page,
} ) => {
	await page.goto( '/?pagename=search-gear&q=helvellyn+daypack' );
	const product = await page
		.locator( '.viglet-dumont-search__title a' )
		.first()
		.getAttribute( 'href' );
	const url = new URL( product );
	url.searchParams.set( 'tour', '1' );
	await page.goto( url.pathname + url.search );
	// The copilot shows only where the site has a language model; its marker with it.
	const copilot = await page
		.locator( '.wp-block-viglet-dumont-copilot' )
		.isVisible()
		.catch( () => false );
	await walk( page, [
		'Related, by meaning',
		...( copilot ? [ 'Ask the catalog' ] : [] ),
	] );
} );

test( 'a page without ?tour loads no tour', async ( { page } ) => {
	await page.goto( '/?pagename=search-gear&q=jacket' );
	await expect( page.locator( '.tarnfell-tour__marker' ) ).toHaveCount( 0 );
	expect(
		await page.evaluate( () =>
			[ ...document.scripts ].some( ( s ) => s.src.includes( 'tour.js' ) )
		)
	).toBe( false );
} );
