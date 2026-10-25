// Every admin action from the keyboard alone: reached with Tab, its focus visible, and used
// with Enter or Space.
import { expect, test } from '../fixtures.mjs';

const settingsUrl = '/wp-admin/admin.php?page=viglet-dumont';

test( 'Settings: each tab opens from the keyboard, and Test Connection runs', async ( {
	page,
	keyboard,
} ) => {
	await page.goto( settingsUrl );
	for ( const tab of [ 'indexing', 'search', 'actions' ] ) {
		await keyboard.tabTo(
			page.locator( `.viglet-dumont-tab[data-tab="${ tab }"]` )
		);
		await page.keyboard.press( 'Enter' );
		await expect( page.locator( `#panel-${ tab }` ) ).toBeVisible();
	}

	await keyboard.tabTo( page.locator( '#viglet-dumont-action-ping' ) );
	await page.keyboard.press( 'Enter' );
	await expect(
		page.locator( '#viglet-dumont-action-ping-result' )
	).toContainText( 'Connection successful' );
} );

test( 'Setup: save and test the connection from the keyboard', async ( {
	page,
	keyboard,
} ) => {
	await page.goto( '/wp-admin/admin.php?page=viglet-dumont-setup' );
	await keyboard.tabTo(
		page.getByRole( 'button', { name: 'Save and test the connection' } ),
		{ max: 120 }
	);
	await page.keyboard.press( 'Enter' );
	await expect( page.locator( '#viglet-dumont-setup li' ) ).toHaveCount( 5 );
} );

test( 'Fields: preview a document from the keyboard', async ( {
	page,
	keyboard,
} ) => {
	await page.goto( '/wp-admin/admin.php?page=viglet-dumont-fields' );
	const post = page.locator( '.components-card' ).filter( {
		has: page.getByRole( 'heading', { name: 'post', exact: true } ),
	} );
	const preview = post.getByRole( 'button', { name: 'Preview document' } );
	await expect( preview ).toBeVisible();
	await keyboard.tabTo( preview, { max: 200 } );
	await page.keyboard.press( 'Enter' );
	await expect( post.locator( 'pre' ) ).toContainText( '"site_key"' );
} );

test( "the editor's Search panel: Reindex now from the keyboard", async ( {
	page,
	seed,
	keyboard,
} ) => {
	await page.goto( `/wp-admin/post.php?post=${ seed.postId }&action=edit` );
	const guide = page
		.getByRole( 'dialog' )
		.getByRole( 'button', { name: 'Close' } );
	if ( await guide.isVisible( { timeout: 5000 } ).catch( () => false ) ) {
		await page.keyboard.press( 'Escape' );
	}
	const panel = page.locator( '.viglet-dumont-search-panel' );
	const reindex = panel.getByRole( 'button', { name: 'Reindex now' } );
	await expect( reindex ).toBeVisible();
	// The settings sidebar comes after the canvas, so this is the editor's longest Tab walk.
	await keyboard.tabTo( reindex, { max: 200 } );
	await page.keyboard.press( 'Enter' );
	await expect( panel.locator( 'strong' ) ).toContainText( 'Indexed' );
} );
