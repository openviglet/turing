// The immersive instant search (D311): recent searches before anything is typed, results
// grouped by type, axe-clean on a desktop and on a phone, and a type's "See all" opening the
// results page filtered to that type. The layout setting is put back afterwards.
import { expect, test } from '../fixtures.mjs';
import { setSetting, wp } from '../global-setup.mjs';

const overlay = ( page ) => page.locator( 'dialog.viglet-turing-overlay' );

// The Explore row's stored links: a page on this site, another site, and one the overlay must
// drop on the way out (its URL is no http(s) address nor a path on this site).
const explore = `array(
	array( 'label' => 'Sample page', 'url' => '/?page_id=2' ),
	array( 'label' => 'Partner library', 'url' => 'https://library.example.org/' ),
	array( 'label' => 'Unsafe', 'url' => 'javascript:void(0)' ),
)`;

test.beforeAll( ( {}, testInfo ) => {
	if ( 'site' !== testInfo.project.name ) {
		return;
	}
	setSetting( 'overlay_layout', 'immersive' );
	wp( [
		'eval',
		`$s = (array) get_option( 'viglet_turing_settings', array() ); $s['overlay_explore'] = ${ explore }; update_option( 'viglet_turing_settings', $s );`,
	] );
} );

test.afterAll( ( {}, testInfo ) => {
	if ( 'site' !== testInfo.project.name ) {
		return;
	}
	setSetting( 'overlay_layout', null );
	setSetting( 'overlay_explore', null );
} );

test.beforeEach( ( {}, testInfo ) => {
	test.skip(
		'site' !== testInfo.project.name,
		'desktop only: it sets its own phone size'
	);
} );

test( 'it starts from recent searches, groups results by type and opens a type in full', async ( {
	page,
	seed,
	axe,
	look,
} ) => {
	await page.goto( `/?page_id=${ seed.corePageId }` );
	await page.evaluate( () =>
		window.localStorage.setItem(
			'viglet-turing-recent-searches',
			JSON.stringify( [ 'workshop' ] )
		)
	);
	await page.keyboard.press( 'Control+K' );
	await expect( overlay( page ) ).toHaveClass( /--immersive/ );
	await expect(
		overlay( page ).getByRole( 'group', { name: 'Recent searches' } )
	).toBeVisible();

	await overlay( page )
		.getByRole( 'group', { name: 'Recent searches' } )
		.getByRole( 'option', { name: 'workshop', exact: true } )
		.click();
	const posts = overlay( page ).getByRole( 'group', { name: 'Posts' } );
	await expect( posts.getByRole( 'option' ).first() ).toContainText(
		'Workshop on'
	);
	// The highlighted result in the preview pane, and Ctrl+Enter later opens the full page.
	await page.keyboard.press( 'ArrowDown' );
	await expect(
		overlay( page ).locator( '.viglet-turing-overlay__preview' )
	).toContainText( 'Workshop on' );
	await look( 'overlay-immersive' );
	await axe( 'the immersive instant search', {
		include: 'dialog.viglet-turing-overlay',
	} );

	await posts.getByRole( 'option', { name: 'See all Posts' } ).click();
	await expect( page ).toHaveURL( /[?&]q=workshop/ );
	expect( new URL( page.url() ).searchParams.getAll( 'fq[]' ) ).toEqual( [
		'type:post',
	] );
} );

test( 'the Explore row offers the site’s links before anything is typed, reached by Tab', async ( {
	page,
	seed,
	axe,
} ) => {
	await page.goto( `/?page_id=${ seed.corePageId }` );
	await page.keyboard.press( 'Control+K' );
	const row = overlay( page ).getByRole( 'navigation', { name: 'Explore' } );
	await expect( row ).toBeVisible();
	await expect( row.getByRole( 'link' ) ).toHaveText( [
		'Sample page',
		'Partner library',
	] );
	await expect(
		row.getByRole( 'link', { name: 'Partner library' } )
	).toHaveAttribute( 'href', 'https://library.example.org/' );
	await axe( 'the immersive instant search with its Explore row', {
		include: 'dialog.viglet-turing-overlay',
	} );

	// From the box, Tab passes the close button and lands on the first link.
	await page.keyboard.press( 'Tab' );
	await page.keyboard.press( 'Tab' );
	await expect(
		row.getByRole( 'link', { name: 'Sample page' } )
	).toBeFocused();

	// Typing gives the room to the results; clearing the box brings the row back.
	await overlay( page ).getByRole( 'combobox' ).fill( 'workshop' );
	await expect( row ).toBeHidden();
	await overlay( page ).getByRole( 'combobox' ).fill( '' );
	await expect( row ).toBeVisible();

	await row.getByRole( 'link', { name: 'Sample page' } ).click();
	await expect( page ).toHaveURL( /[?&]page_id=2\b/ );
} );

test( 'on a phone it fills the screen and stays axe-clean', async ( {
	page,
	seed,
	axe,
} ) => {
	await page.setViewportSize( { width: 390, height: 844 } );
	await page.goto( `/?page_id=${ seed.corePageId }` );
	await page.keyboard.press( 'Control+K' );
	await page.keyboard.type( 'workshop' );
	await expect(
		overlay( page ).getByRole( 'group', { name: 'Posts' } )
	).toBeVisible();
	const box = await overlay( page ).boundingBox();
	expect( Math.round( box.width ) ).toBe( 390 );
	await axe( 'the immersive instant search on a phone', {
		include: 'dialog.viglet-turing-overlay',
	} );
} );
