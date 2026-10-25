// The search block in the block editor: it previews Turing's real answer, and its settings,
// changed and saved there, shape the published page: where the filters sit, list or grid,
// which parts each result shows, and its style.
import { wp } from '../global-setup.mjs';
import { expect, test } from '../fixtures.mjs';

// The site specs compare screenshots of this page: give it back its defaults.
test.afterEach( ( { seed } ) => {
	wp( [
		'post',
		'update',
		String( seed.searchPageId ),
		'--post_content=<!-- wp:viglet-dumont/search /-->',
	] );
} );

test( 'layout, parts and style chosen in the editor reach the published page', async ( {
	page,
	seed,
	look,
} ) => {
	await page.goto(
		`/wp-admin/post.php?post=${ seed.searchPageId }&action=edit`
	);
	const guide = page
		.getByRole( 'dialog' )
		.getByRole( 'button', { name: 'Close' } );
	if ( await guide.isVisible( { timeout: 5000 } ).catch( () => false ) ) {
		await guide.click();
	}

	const canvas = page.frameLocator( 'iframe[name="editor-canvas"]' );
	const inEditor = canvas.locator( '[data-type="viglet-dumont/search"]' );
	// The editor previews what visitors get: Turing's answer, as cards.
	await expect(
		inEditor.locator( '.viglet-dumont-search__result' ).first()
	).toBeVisible();
	await expect(
		inEditor.locator( '.viglet-dumont-search__summary' )
	).toHaveText( /^\d+ results$/ );
	await inEditor.click();
	const settings = page.getByRole( 'button', {
		name: 'Settings',
		exact: true,
	} );
	if ( 'false' === ( await settings.getAttribute( 'aria-pressed' ) ) ) {
		await settings.click();
	}
	const inspector = page.locator( '.block-editor-block-inspector' );
	await inspector
		.getByLabel( 'Where the filters sit' )
		.selectOption( 'right' );
	await inspector.getByLabel( 'Results as' ).selectOption( 'grid' );
	await inspector.getByLabel( 'Show the snippet' ).click();
	await inspector.getByRole( 'tab', { name: 'Styles' } ).click();
	await inspector.getByRole( 'button', { name: 'Cards' } ).click();
	await look( 'block-editor-settings' );

	await page
		.getByRole( 'region', { name: 'Editor top bar' } )
		.getByRole( 'button', { name: 'Save', exact: true } )
		.click();
	await expect(
		page.locator( '.components-snackbar' ).filter( { hasText: 'updated' } )
	).toBeVisible();

	await page.goto( `/?page_id=${ seed.searchPageId }&q=workshop` );
	const block = page.locator( '.viglet-dumont-search' );
	await expect( block ).toHaveClass( /viglet-dumont-search--facets-right/ );
	await expect( block ).toHaveClass( /viglet-dumont-search--grid/ );
	await expect( block ).toHaveClass( /is-style-cards/ );
	await expect(
		block.locator( '.viglet-dumont-search__result' ).first()
	).toBeVisible();
	await expect(
		block.locator( '.viglet-dumont-search__snippet' )
	).toHaveCount( 0 );
	// Right of the results, as chosen.
	const facets = await block
		.locator( '.viglet-dumont-search__facets' )
		.boundingBox();
	const results = await block
		.locator( '.viglet-dumont-search__results' )
		.boundingBox();
	expect( facets.x ).toBeGreaterThan( results.x );
	await look( 'block-published-settings' );
} );
