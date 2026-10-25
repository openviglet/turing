// The editor panel's Assist section (D171): what the site already has near this post, and
// links to related posts the author can add. It never changes the post on its own, so the
// spec leaves without saving.
import { expect, test } from '../fixtures.mjs';

test( 'Assist finds related posts, and "Add a link" puts one in the post', async ( {
	page,
	seed,
	look,
	axe,
} ) => {
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
