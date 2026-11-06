// Where each post stands: the Search column in the post list and the block editor's panel.
import { expect, openPostSettings, test } from '../fixtures.mjs';

test( 'the post list has a Search column saying where each post stands', async ( {
	page,
	look,
} ) => {
	await page.goto( '/wp-admin/edit.php' );
	await expect( page.locator( 'th#viglet_turing' ) ).toHaveText( 'Search' );
	await expect(
		page.locator( '.viglet-turing-state--indexed' ).first()
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
	await openPostSettings( page );
	const panel = page.locator( '.viglet-turing-search-panel' );
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
		include: '.viglet-turing-search-panel',
	} );
} );

test( 'a reindex Turing refuses shows Turing’s reason in the panel', async ( {
	page,
	seed,
	problems,
} ) => {
	// D330: the server answers 502 with the post's state and Turing's reason, as it does when
	// Turing refuses the document; the panel used to print an empty message instead.
	const reason = 'Import failed with HTTP 400: field seats is not a number';
	// The refusal is the case under test, so its 502 is not a failure of the page.
	problems.allow( /reindex|502/ );
	await page.route(
		( url ) =>
			decodeURIComponent( url.href ).includes(
				'/viglet-turing/v1/post/'
			) && decodeURIComponent( url.href ).includes( '/reindex' ),
		( route ) =>
			route.fulfill( {
				status: 502,
				json: {
					state: 'failed',
					label: 'Not indexed',
					detail: reason,
					message: reason,
				},
			} )
	);
	await page.goto( `/wp-admin/post.php?post=${ seed.postId }&action=edit` );
	const guide = page
		.getByRole( 'dialog' )
		.getByRole( 'button', { name: 'Close' } );
	if ( await guide.isVisible( { timeout: 5000 } ).catch( () => false ) ) {
		await guide.click();
	}
	await openPostSettings( page );
	const panel = page.locator( '.viglet-turing-search-panel' );
	if (
		! ( await panel
			.getByText( 'Exclude from search' )
			.isVisible()
			.catch( () => false ) )
	) {
		await page
			.getByRole( 'button', { name: 'Search', exact: true } )
			.click();
	}

	await panel.getByRole( 'button', { name: 'Reindex now' } ).click();

	await expect( panel ).toContainText( 'Not indexed' );
	await expect( panel ).toContainText( reason );
} );
