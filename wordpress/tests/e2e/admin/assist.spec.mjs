// The editor panel's Assist section (D171): what the site already has near this post, links to
// related posts the author can add, and an excerpt drafted by Turing. It never changes the post
// on its own, so the specs leave without saving.
import { expect, test } from '../fixtures.mjs';

/**
 * Open the post in the editor with the Search panel showing.
 *
 * @param {import('@playwright/test').Page} page The page.
 * @param {Object}                          seed What global-setup seeded.
 */
async function openPanel( page, seed ) {
	await page.goto( `/wp-admin/post.php?post=${ seed.postId }&action=edit` );
	const guide = page
		.getByRole( 'dialog' )
		.getByRole( 'button', { name: 'Close' } );
	if ( await guide.isVisible( { timeout: 5000 } ).catch( () => false ) ) {
		await guide.click();
	}
	// The panel lives in the post's settings sidebar, as in posts.spec.mjs.
	const panelToggle = page.getByRole( 'button', {
		name: 'Search',
		exact: true,
	} );
	if ( ! ( await panelToggle.isVisible().catch( () => false ) ) ) {
		await page
			.getByRole( 'button', { name: 'Settings', exact: true } )
			.click();
		await page.getByRole( 'tab', { name: 'Post' } ).click();
	}
	const panel = page.locator( '.viglet-dumont-search-panel' );
	if (
		! ( await panel
			.getByRole( 'button', { name: 'Find related posts' } )
			.isVisible()
			.catch( () => false ) )
	) {
		await panelToggle.click();
	}
	return panel;
}

test( 'Assist finds related posts, and "Add a link" puts one in the post', async ( {
	page,
	seed,
	look,
	axe,
} ) => {
	const panel = await openPanel( page, seed );

	await panel.getByRole( 'button', { name: 'Find related posts' } ).click();
	const assist = panel.locator( '.viglet-dumont-assist' );
	await expect(
		assist.getByRole( 'heading', { name: 'Link ideas' } )
	).toBeVisible();
	const ideas = assist
		.locator( '.viglet-dumont-assist__list' )
		.last()
		.locator( 'li' );
	await expect( ideas.first() ).toBeVisible();
	// Turing's similar documents for "Workshop on search": the other workshops are among
	// them (its lexical similarity may rank another "search" page first).
	await expect( ideas.filter( { hasText: 'Workshop on' } ) ).not.toHaveCount(
		0
	);
	const href = await ideas.first().locator( 'a' ).getAttribute( 'href' );
	await look( 'editor-assist' );
	await axe( "the editor panel's Assist section", {
		include: '.viglet-dumont-assist',
	} );

	await ideas
		.first()
		.getByRole( 'button', { name: /^Add a link to/ } )
		.click();
	await expect(
		page
			.frameLocator( 'iframe[name="editor-canvas"]' )
			.locator( `p a[href="${ href }"]` )
	).toBeVisible();
} );

// The draft itself is Turing's (ai/assist.spec.mjs asks the real model): here the plugin's
// route answers with fixed drafts, so the spec checks what the panel does with them.
test( 'a drafted excerpt goes into the excerpt field only when the author uses it', async ( {
	page,
	seed,
	axe,
} ) => {
	const drafts = [ 'A first draft.', 'A second draft.' ];
	const asked = [];
	await page.route(
		( url ) => decodeURIComponent( url.href ).includes( '/excerpt' ),
		async ( route ) => {
			asked.push( route.request().postDataJSON() );
			await route.fulfill( {
				json: { excerpt: drafts[ asked.length - 1 ], error: '' },
			} );
		}
	);
	const panel = await openPanel( page, seed );
	const excerpt = () =>
		page.evaluate( () =>
			window.wp.data
				.select( 'core/editor' )
				.getEditedPostAttribute( 'excerpt' )
		);
	const before = await excerpt();

	await panel.getByRole( 'button', { name: 'Draft an excerpt' } ).click();
	const draft = panel.locator( '.viglet-dumont-assist__draft' );
	await expect( draft ).toHaveText( 'A first draft.' );
	expect( await excerpt() ).toBe( before );

	await panel.getByRole( 'button', { name: 'Draft another' } ).click();
	await expect( draft ).toHaveText( 'A second draft.' );
	expect( asked ).toEqual( [ { again: false }, { again: true } ] );
	await axe( "the editor panel's excerpt draft", {
		include: '.viglet-dumont-assist__excerpt',
	} );

	await panel.getByRole( 'button', { name: 'Use this excerpt' } ).click();
	expect( await excerpt() ).toBe( 'A second draft.' );
	await expect(
		panel.getByText( 'In the excerpt field. Save the post to keep it.' )
	).toBeVisible();
} );
