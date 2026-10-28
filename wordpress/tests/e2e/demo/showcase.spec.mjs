// The showcase (D179), on the `demo` project's stack: one page holding every Turing block, each
// under the feature it shows, and opened with ?tour=1 the "How it works" markers (D195) number
// them. Turing's copilot and chat are answered as on, so the page shows what a stack with a
// language model shows; what they say is ai/*.spec.mjs.
import { expect, test } from '../fixtures.mjs';
import { demoLocale } from './curated.mjs';

const words = {
	en_US: {
		marker: 'Turing feature',
		features: [ 'Search as you type', 'Ask the catalog', 'Ask this site' ],
		heading: 'Search, with an answer',
	},
	pt_BR: {
		marker: 'Recurso do Turing',
		features: [
			'Busca enquanto digita',
			'Pergunte ao catálogo',
			'Pergunte a este site',
		],
		heading: 'Busca, com resposta',
	},
};
const say = () => words[ demoLocale() ] || words.en_US;

test( 'the showcase holds every Turing block, each named and numbered by the tour', async ( {
	page,
	look,
} ) => {
	await page.route( '**/copilot/available', ( route ) =>
		route.fulfill( { json: { available: true } } )
	);
	await page.route( '**/chat/enabled', ( route ) =>
		route.fulfill( { json: { enabled: true } } )
	);
	await page.goto( '/showcase/?tour=1' );

	await expect(
		page.getByRole( 'heading', { level: 2, name: say().heading } )
	).toBeVisible();
	await expect( page.locator( '.viglet-dumont-search__form' ) ).toBeVisible();
	await expect( page.locator( '.viglet-dumont-copilot' ) ).toBeVisible();
	await expect( page.locator( '.viglet-dumont-assistant' ) ).toBeVisible();

	// The markers number the features in the order the page shows them.
	const markers = page.locator( '.tarnfell-tour__marker' );
	const labels = await markers.evaluateAll( ( buttons ) =>
		buttons.map( ( b ) => b.getAttribute( 'aria-label' ) )
	);
	const { marker, features } = say();
	for ( const feature of features ) {
		expect( labels ).toContainEqual(
			expect.stringMatching(
				new RegExp( `^${ marker } \\d+: ${ feature }$` )
			)
		);
	}
	await look( 'demo-showcase' );
} );
