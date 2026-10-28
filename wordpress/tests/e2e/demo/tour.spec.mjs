// "How it works" (D195), on the `demo` project's stack: with ?tour=1 each Turing feature on the
// page gets a numbered marker. At phone width, every marker is reached with Tab, opens its note
// with Enter, and the note covers nothing of the feature it describes; Escape closes it and
// gives the focus back.
import { expect, test } from '../fixtures.mjs';
import { demoLocale } from './curated.mjs';

test.use( { viewport: { width: 390, height: 844 } } );

// What the tour says and searches for, in each language the demo speaks (D217, D223).
const words = {
	en_US: {
		marker: 'Turing feature',
		docs: 'How it works',
		jacket: 'jacket',
		daypack: 'helvellyn daypack',
		productType: 'Product',
		search: 'Search as you type',
		filters: 'Filters from the catalog',
		related: 'Related, by meaning',
		copilot: 'Ask the catalog',
	},
	pt_BR: {
		marker: 'Recurso do Turing',
		docs: 'Como funciona',
		jacket: 'jaqueta',
		daypack: 'helvellyn mochila',
		productType: 'Produto',
		search: 'Busca enquanto digita',
		filters: 'Filtros vindos do catálogo',
		related: 'Relacionados pelo sentido',
		copilot: 'Pergunte ao catálogo',
	},
};
const say = () => words[ demoLocale() ] || words.en_US;

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
			`${ say().marker } ${ i + 1 }: ${ expected[ i ] }`
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
			note.getByRole( 'link', { name: say().docs } )
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
	await page.goto(
		`/?pagename=search-gear&q=${ encodeURIComponent( say().jacket ) }&tour=1`
	);
	await expect(
		page.locator( '.viglet-dumont-search__title a' ).first()
	).toBeVisible();
	await walk( page, [ say().search, say().filters ] );
	await axe( 'the tour', { include: '.tarnfell-tour' } );
} );

test( 'a product page names the catalog copilot and Related, in page order', async ( {
	page,
} ) => {
	// Answered as on, so the copilot shows as on a stack with a language model; its marker
	// is placed once it does (D179).
	await page.route( '**/copilot/available', ( route ) =>
		route.fulfill( { json: { available: true } } )
	);
	await page.goto(
		`/?pagename=search-gear&q=${ encodeURIComponent( say().daypack ) }`
	);
	// The first product, not the first result: a journal guide that names the same daypack
	// (and matches "daypack" through the backpack synonym) may rank above it.
	const product = await page
		.locator(
			'.viglet-dumont-search__results .viglet-dumont-search__result'
		)
		.filter( {
			has: page.locator( '.viglet-dumont-search__type', {
				hasText: say().productType,
			} ),
		} )
		.first()
		.locator( '.viglet-dumont-search__title a' )
		.getAttribute( 'href' );
	const url = new URL( product );
	url.searchParams.set( 'tour', '1' );
	await page.goto( url.pathname + url.search );
	await expect(
		page.locator( '.wp-block-viglet-dumont-copilot' )
	).toBeVisible();
	await walk( page, [ say().copilot, say().related ] );
} );

test( 'a page without ?tour loads no tour', async ( { page } ) => {
	await page.goto(
		`/?pagename=search-gear&q=${ encodeURIComponent( say().jacket ) }`
	);
	await expect( page.locator( '.tarnfell-tour__marker' ) ).toHaveCount( 0 );
	expect(
		await page.evaluate( () =>
			[ ...document.scripts ].some( ( s ) => s.src.includes( 'tour.js' ) )
		)
	).toBe( false );
} );
