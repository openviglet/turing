// The Ask the catalog block (D173) against Turing's real copilot (D174): asked through its own
// form, it answers from the site's posts and cites the one holding the fact. The page is made
// here and removed again, so the default suite never sees it.
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
			'--post_title=Ask the copilot',
			'--post_content=<!-- wp:viglet-dumont/copilot /-->',
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

test( 'the copilot answers from the post that holds the fact, and cites it', async ( {
	page,
} ) => {
	const copilot = feature( 'copilot' );
	test.skip( ! copilot.on, `the copilot is off: ${ copilot.reason }` );

	await page.goto( `/?page_id=${ pageId }` );
	const block = page.locator( '.viglet-dumont-copilot' );
	await expect( block ).toBeVisible();

	const question = 'When do applications for the research grant close?';
	await block.getByLabel( 'What are you looking for?' ).fill( question );
	const started = Date.now();
	await block.getByRole( 'button', { name: 'Ask', exact: true } ).click();
	const answer = block.locator( '.viglet-dumont-copilot__answer' ).first();
	await expect( answer ).not.toBeEmpty( { timeout: 60_000 } );
	const text = ( await answer.textContent() ).trim();
	record( 'copilot: grant deadline', question, text, Date.now() - started );

	expect( text ).toMatch( /14(th)? March|March 14/i );
	await expect(
		block.locator( '.viglet-dumont-copilot__item a', {
			hasText: 'Research grant 2026',
		} )
	).not.toHaveCount( 0 );
} );
