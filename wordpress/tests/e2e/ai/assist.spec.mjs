// The editor's excerpt draft (D171) against a real LLM: Turing's summary of the grant post
// keeps its fact, and using it fills the excerpt field without saving the post.
import { expect, test } from '../fixtures.mjs';
import { ai, record } from './helpers.mjs';

test( 'the excerpt drafted for the grant post keeps its deadline', async ( {
	page,
	look,
} ) => {
	await page.goto(
		`/wp-admin/post.php?post=${ ai().posts.grant }&action=edit`
	);
	const guide = page
		.getByRole( 'dialog' )
		.getByRole( 'button', { name: 'Close' } );
	if ( await guide.isVisible( { timeout: 5000 } ).catch( () => false ) ) {
		await guide.click();
	}
	const panel = page.locator( '.viglet-dumont-search-panel' );
	if ( ! ( await panel.isVisible().catch( () => false ) ) ) {
		await page
			.getByRole( 'button', { name: 'Settings', exact: true } )
			.click();
		await page.getByRole( 'tab', { name: 'Post' } ).click();
	}
	const draftButton = panel.getByRole( 'button', {
		name: 'Draft an excerpt',
	} );
	if ( ! ( await draftButton.isVisible().catch( () => false ) ) ) {
		await page
			.getByRole( 'button', { name: 'Search', exact: true } )
			.click();
	}

	const started = Date.now();
	await draftButton.click();
	const draft = panel.locator( '.viglet-dumont-assist__draft' );
	await expect( draft ).toHaveText( /14(th)? March|March 14/i, {
		timeout: 60000,
	} );
	record(
		'excerpt draft: grant',
		'(the grant post)',
		await draft.textContent(),
		Date.now() - started
	);
	await look( 'ai-excerpt-draft' );

	await panel.getByRole( 'button', { name: 'Use this excerpt' } ).click();
	expect(
		await page.evaluate( () =>
			window.wp.data
				.select( 'core/editor' )
				.getEditedPostAttribute( 'excerpt' )
		)
	).toBe( await draft.textContent() );
} );

test( 'the grant post is scored for the audience persona of the site, with a reason', async ( {
	page,
	look,
} ) => {
	await page.goto(
		`/wp-admin/post.php?post=${ ai().posts.grant }&action=edit`
	);
	const guide = page
		.getByRole( 'dialog' )
		.getByRole( 'button', { name: 'Close' } );
	if ( await guide.isVisible( { timeout: 5000 } ).catch( () => false ) ) {
		await guide.click();
	}
	const panel = page.locator( '.viglet-dumont-search-panel' );
	if ( ! ( await panel.isVisible().catch( () => false ) ) ) {
		await page
			.getByRole( 'button', { name: 'Settings', exact: true } )
			.click();
		await page.getByRole( 'tab', { name: 'Post' } ).click();
	}
	const check = panel.getByRole( 'button', { name: 'Check audience fit' } );
	if ( ! ( await check.isVisible().catch( () => false ) ) ) {
		await page
			.getByRole( 'button', { name: 'Search', exact: true } )
			.click();
	}

	const started = Date.now();
	await check.click();
	const score = panel.locator( '.viglet-dumont-assist__fit li strong' );
	await expect( score ).toHaveText( /^Grant applicant: \d{1,3}%$/, {
		timeout: 60000,
	} );
	const answer = await panel
		.locator( '.viglet-dumont-assist__fit > ul > li' )
		.first()
		.textContent();
	record(
		'audience fit: grant',
		'(the grant post)',
		answer,
		Date.now() - started
	);
	await look( 'ai-audience-fit' );
	// A score with no reason tells the author nothing to change.
	expect( answer.replace( /^Grant applicant: \d+%/, '' ).trim() ).not.toBe(
		''
	);
} );
