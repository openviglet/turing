// The Search insights summary (D178) against a real LLM: Turing's summary of the stack's
// search log names what visitors searched for most, which every default run logs.
import { expect, test } from '../fixtures.mjs';
import { record } from './helpers.mjs';

test( "the period's summary names its top search", async ( { page, look } ) => {
	await page.goto( '/wp-admin/admin.php?page=viglet-dumont-insights' );
	await page.getByLabel( 'Period' ).selectOption( 'this-month' );
	const card = page.locator( '.components-card' ).filter( {
		has: page.getByRole( 'heading', { name: 'Summary' } ),
	} );

	const started = Date.now();
	await card.getByRole( 'button', { name: 'Summarize this period' } ).click();
	const summary = card.locator( '.viglet-dumont-insights__summary' );
	await expect( summary ).toHaveText( /workshop/i, { timeout: 60000 } );
	record(
		'insights summary: last 30 days',
		'(the search log)',
		await summary.textContent(),
		Date.now() - started
	);
	await look( 'ai-insights-summary' );
} );
