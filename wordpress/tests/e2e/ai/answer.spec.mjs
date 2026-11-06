// The search block's answer panel (D166) against a real LLM: a question gets the answer with
// the post that holds the fact among its sources, and a question nothing answers gets no
// panel. Skipped, with the reason, while the site's answers are off (on the local stack,
// until Turing T1116 lets the manifest turn them on).
import { expect, test } from '../fixtures.mjs';
import { ai, feature, record } from './helpers.mjs';

const panel = ( page ) => page.locator( '.viglet-dumont-search__answer' );

test.beforeEach( () => {
	const answer = feature( 'answer' );
	test.skip( ! answer.on, `answers are off: ${ answer.reason }` );
} );

test( 'a question gets the cited answer, its source the post holding the fact', async ( {
	page,
	seed,
	look,
} ) => {
	const question = 'When do applications for the research grant close?';
	const started = Date.now();
	await page.goto(
		`/?page_id=${ seed.askPageId }&q=${ encodeURIComponent( question ) }`
	);
	const text = panel( page ).locator( '.viglet-dumont-search__answer-text' );
	await expect( text ).toHaveText( /14(th)? March|March 14/i );
	record(
		'answer panel: grant deadline',
		question,
		await text.textContent(),
		Date.now() - started
	);
	await expect(
		panel( page ).locator( `a[href$="?p=${ ai().posts.grant }"]` )
	).toBeVisible();
	await look( 'ai-answer-panel' );
} );

test( 'a question nothing on the site answers gets no panel', async ( {
	page,
	seed,
} ) => {
	await page.goto(
		`/?page_id=${ seed.askPageId }&q=${ encodeURIComponent(
			'How much does a ticket to the photography contest cost?'
		) }`
	);
	await page.waitForLoadState( 'networkidle' );
	await expect( panel( page ) ).toBeHidden();
} );
