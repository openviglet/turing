// A spent AI budget on the overlay's answer and the Site assistant (D336): each says answers are
// paused until the reset date this site recorded, instead of going quiet or failing. Turing's
// answer and chat are made to refuse with 429, and the record the server keeps from a 429 is
// set directly, since the local Turing has no budget to spend. Desktop only: it changes
// settings and makes a page, and puts both back.
import { expect, test } from '../fixtures.mjs';
import { setSetting, wp } from '../global-setup.mjs';

let pageId = '';

const quota = {
	type: 'https://turing.example.org/problems/quota-exhausted',
	title: 'AI budget exhausted',
	detail: 'The monthly AI budget of this site is spent.',
	resetsAt: '2099-12-01T00:00:00Z',
};

test.beforeAll( ( {}, testInfo ) => {
	if ( 'site' !== testInfo.project.name ) {
		return;
	}
	setSetting( 'overlay_layout', 'immersive' );
	setSetting( 'overlay_answers', 1 );
	wp( [
		'eval',
		`update_option( Viglet_Turing_AI_Quota::OPTION, array( 'resetsAt' => '${ quota.resetsAt }', 'detail' => '${ quota.detail }', 'seen_at' => time() ), false );`,
	] );
	pageId = wp( [
		'post',
		'create',
		'--post_type=page',
		'--post_status=publish',
		'--post_title=Ask us while paused',
		'--post_content=<!-- wp:viglet-turing/assistant /-->',
		'--meta_input={"_viglet_turing_exclude":"1"}',
		'--porcelain',
	] )
		.trim()
		.split( /\r?\n/ )
		.pop()
		.trim();
} );

test.afterAll( ( {}, testInfo ) => {
	if ( 'site' !== testInfo.project.name ) {
		return;
	}
	setSetting( 'overlay_layout', null );
	setSetting( 'overlay_answers', null );
	wp( [
		'eval',
		'delete_option( Viglet_Turing_AI_Quota::OPTION ); delete_transient( "viglet_turing_ai_paused_report" );',
	] );
	if ( pageId ) {
		wp( [ 'post', 'delete', pageId, '--force' ] );
	}
} );

test.beforeEach( ( { problems }, testInfo ) => {
	test.skip( 'site' !== testInfo.project.name, 'desktop only' );
	// The refusals are the case under test.
	problems.allow( /status of 429/ );
	problems.allow( /\/answer\?|\/chat\/conversation/ );
} );

test( 'the overlay says answers are paused until the reset date', async ( {
	page,
	seed,
} ) => {
	await page.route( /\/answer\?/, ( route ) =>
		route.fulfill( {
			status: 429,
			contentType: 'application/problem+json',
			json: quota,
		} )
	);
	await page.goto( `/?page_id=${ seed.corePageId }` );
	await page.keyboard.press( 'Control+K' );
	const overlay = page.locator( 'dialog.viglet-turing-overlay' );
	await overlay
		.getByRole( 'combobox' )
		.or( overlay.locator( 'input' ) )
		.first()
		.fill( 'when is the next workshop?' );

	const answer = overlay.locator( '.viglet-turing-overlay__answer' );
	await expect( answer ).toHaveText(
		/^Answers are paused until .*2099.*; search still works\.$/
	);
} );

test( 'the assistant says answers are paused until the reset date', async ( {
	page,
} ) => {
	await page.route( '**/chat/enabled', ( route ) =>
		route.fulfill( { json: { enabled: true } } )
	);
	await page.route( '**/chat/conversation', ( route ) =>
		route.fulfill( {
			status: 429,
			contentType: 'application/problem+json',
			json: quota,
		} )
	);
	await page.goto( `/?page_id=${ pageId }` );
	await page.getByRole( 'button', { name: 'Ask this site' } ).click();
	const dialog = page.getByRole( 'dialog', { name: 'Ask this site' } );
	await dialog.getByLabel( 'Your question' ).fill( 'When does it open?' );
	await dialog.getByRole( 'button', { name: 'Send' } ).click();

	await expect( dialog ).toContainText(
		/Answers are paused until .*2099.*; search still works\./
	);
	await expect( dialog ).not.toContainText( 'could not answer' );
} );
