// A period read from a sentence on the agenda (D325, D324): "debates in <next month>" ("debates em
// <mês>" on Escola Lumen) offers that month on the event's day, and taking it leaves the debate
// that month and not the one the month after. Turing's nl-filters is answered as having no
// model, as on this stack, so the reading is the block's own.
import { expect, test } from '../fixtures.mjs';
import { say, titleOf } from './school.mjs';

test( 'a month named in a sentence narrows the agenda to the debate that month', async ( {
	page,
	look,
} ) => {
	await page.route( '**/nl-filters', ( route ) =>
		route.fulfill( { json: { available: false } } )
	);
	// The seed puts one debate on the 15th of next month and the other a month later.
	const now = new Date();
	const next = new Date( now.getFullYear(), now.getMonth() + 1, 15 );
	const sentence = `debates ${ say().in } ${
		say().months[ next.getMonth() ]
	}`;
	await page.goto( `/agenda/?q=${ encodeURIComponent( sentence ) }` );

	const block = page.locator( '.viglet-turing-search' );
	const chip = block.locator( '.viglet-turing-search__chip--suggested' );
	await expect( chip ).toHaveCount( 1 );
	await expect( chip ).toContainText( `${ say().when }:` );
	await chip.click();

	await expect(
		block.locator( '.viglet-turing-search__summary' )
	).toHaveText( /result/ );
	await expect(
		block.locator(
			'.viglet-turing-search__results .viglet-turing-search__title'
		)
	).toHaveText( [ titleOf( 'debate-car-free' ) ] );
	await look( 'demo-edu-sentence-period' );
} );
