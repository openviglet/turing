// The immersive instant search against a real LLM (D311): a question that rests gets
// Turing's answer above the grouped results, and the highlighted result shows in the preview
// pane beside them. Skipped, with the reason, while the site's answers are off.
import { expect, test } from '../fixtures.mjs';
import { setSetting } from '../global-setup.mjs';
import { feature, record } from './helpers.mjs';

const overlay = ( page ) => page.locator( 'dialog.viglet-dumont-overlay' );

test.beforeAll( () => {
	setSetting( 'overlay_layout', 'immersive' );
	setSetting( 'overlay_answers', 1 );
} );

test.afterAll( () => {
	setSetting( 'overlay_layout', null );
	setSetting( 'overlay_answers', null );
} );

test.beforeEach( () => {
	const answer = feature( 'answer' );
	test.skip( ! answer.on, `answers are off: ${ answer.reason }` );
} );

test( 'a question gets the answer on top, and the preview shows the highlighted result', async ( {
	page,
	look,
} ) => {
	const question = 'When do applications for the research grant close?';
	await page.goto( '/' );
	await page.keyboard.press( 'Control+K' );
	await expect( overlay( page ) ).toHaveClass( /--immersive/ );
	const started = Date.now();
	await page.keyboard.type( question );

	const answer = overlay( page ).locator( '.viglet-dumont-overlay__answer' );
	await expect( answer ).toHaveText( /14(th)? March|March 14/i, {
		timeout: 60_000,
	} );
	record(
		'instant search answer: grant deadline',
		question,
		await answer.textContent(),
		Date.now() - started
	);

	await page.keyboard.press( 'ArrowDown' );
	await expect(
		overlay( page ).locator( '.viglet-dumont-overlay__preview' )
	).toBeVisible();
	await look( 'ai-overlay-answer' );
} );
