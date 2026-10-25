// Where each post stands: the Search column in the post list and the block editor's panel.
import { expect, test } from '../fixtures.mjs';

test( 'the post list has a Search column saying where each post stands', async ( {
	page,
	look,
} ) => {
	await page.goto( '/wp-admin/edit.php' );
	await expect( page.locator( 'th#viglet_dumont' ) ).toHaveText( 'Search' );
	await expect(
		page.locator( '.viglet-dumont-state--indexed' ).first()
	).toContainText( 'Indexed' );
	await look( 'posts-list' );
} );

test( 'the block editor has a Search panel with the state, Exclude and Reindex now', async ( {
	page,
	seed,
	look,
	axe,
} ) => {
	await page.goto( `/wp-admin/post.php?post=${ seed.postId }&action=edit` );

	// The editor greets a first visit with a guide; it is not what is under test.
	const guide = page
		.getByRole( 'dialog' )
		.getByRole( 'button', { name: 'Close' } );
	if ( await guide.isVisible( { timeout: 5000 } ).catch( () => false ) ) {
		await guide.click();
	}

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
			.getByText( 'Exclude from search' )
			.isVisible()
			.catch( () => false ) )
	) {
		await panelToggle.click();
	}

	await expect( panel.getByText( 'Exclude from search' ) ).toBeVisible();
	await expect( panel.locator( 'strong' ) ).toContainText( /Indexed|Queued/ );

	await panel.getByRole( 'button', { name: 'Reindex now' } ).click();
	await expect( panel.locator( 'strong' ) ).toContainText( 'Indexed' );
	await look( 'editor-panel' );
	await axe( "the editor's Search panel", {
		include: '.viglet-dumont-search-panel',
	} );
} );
