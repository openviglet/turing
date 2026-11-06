// The Site assistant (D175) against Turing's real chat (D174): asked through its own dialog, it
// answers from the post that holds the fact and lists it, and sends a follow-up with the
// conversation so far. The page is made here and removed again.
import { wp } from '../global-setup.mjs';
import { expect, test } from '../fixtures.mjs';
import { feature, record } from './helpers.mjs';

let pageId = 0;

test.beforeAll( () => {
	pageId = Number(
		wp( [
			'post',
			'create',
			'--post_type=page',
			'--post_status=publish',
			'--post_title=Ask the assistant',
			'--post_content=<!-- wp:viglet-dumont/assistant /-->',
			'--meta_input={"_viglet_dumont_exclude":"1"}',
			'--porcelain',
		] ).trim()
	);
} );

test.afterAll( () => {
	if ( pageId ) {
		wp( [ 'post', 'delete', String( pageId ), '--force' ] );
	}
} );

/**
 * Ask in the dialog and wait for the whole answer.
 *
 * @param {import('@playwright/test').Locator} dialog   The assistant's dialog.
 * @param {string}                             question The question.
 * @return {Promise<{text: string, ms: number}>} The answer and how long it took.
 */
async function ask( dialog, question ) {
	const messages = dialog.locator( '.viglet-dumont-assistant__message' );
	const before = await messages.count();
	await dialog.getByLabel( 'Your question' ).fill( question );
	const started = Date.now();
	await dialog.getByRole( 'button', { name: 'Send' } ).click();
	const answer = messages.nth( before + 1 );
	// Rating is offered once the answer has finished streaming.
	await expect( answer.getByRole( 'button', { name: 'Yes' } ) ).toBeVisible( {
		timeout: 60_000,
	} );
	return {
		text: (
			await answer
				.locator( '.viglet-dumont-assistant__text' )
				.textContent()
		).trim(),
		ms: Date.now() - started,
	};
}

test( 'the assistant answers from the post that holds the fact, lists it, and keeps the conversation', async ( {
	page,
} ) => {
	const chat = feature( 'chat' );
	test.skip( ! chat.on, `chat is off: ${ chat.reason }` );
	const site = feature( 'answer' );
	test.skip(
		! site.on,
		`the site has no GenAI config, so chat cannot read its posts: ${ site.reason }`
	);

	const asked = [];
	page.on( 'request', ( request ) => {
		if ( request.url().endsWith( '/chat/conversation' ) ) {
			asked.push( request.postDataJSON() );
		}
	} );
	await page.goto( `/?page_id=${ pageId }` );
	await page.getByRole( 'button', { name: 'Ask this site' } ).click();
	const dialog = page.getByRole( 'dialog', { name: 'Ask this site' } );

	const question = 'When do applications for the research grant close?';
	const first = await ask( dialog, question );
	record( 'assistant: grant deadline', question, first.text, first.ms );
	expect( first.text ).toMatch( /14(th)? March|March 14/i );
	await expect(
		dialog.getByRole( 'link', { name: 'Research grant 2026' } )
	).not.toHaveCount( 0 );

	// "it" is the grant, so the follow-up goes with the conversation so far, in the same one.
	const followUp = 'Where do I send it?';
	const second = await ask( dialog, followUp );
	record( 'assistant: follow-up', followUp, second.text, second.ms );
	expect( asked[ 1 ].messages.map( ( m ) => m.role ) ).toEqual( [
		'user',
		'assistant',
		'user',
	] );
	expect( asked[ 1 ].conversationId ).toBe( asked[ 0 ].conversationId );
	// Turing retrieves on the latest message alone, so "it" finds no post until it rewrites
	// a follow-up into a standalone question (Turing T1157); then this answer names the
	// Office of Research. Recorded for the reviewer until then, not asserted.

	// The links are the URLs Turing's chunks hold. A stack that moved port, or a post deleted
	// and seeded again, can leave older chunks behind (Turing T1158), so their origin is not
	// asserted here; that the agent opens only this site's pages is site/assistant.spec.mjs.
} );
