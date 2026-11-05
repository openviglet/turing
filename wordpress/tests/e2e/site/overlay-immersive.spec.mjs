// The immersive instant search (D311): recent searches before anything is typed, results
// grouped by type, axe-clean on a desktop and on a phone, and a type's "See all" opening the
// results page filtered to that type. The layout setting is put back afterwards.
import { expect, test } from '../fixtures.mjs';
import { wp } from '../global-setup.mjs';

const overlay = ( page ) => page.locator( 'dialog.viglet-dumont-overlay' );

test.beforeAll( ( {}, testInfo ) => {
	if ( 'site' !== testInfo.project.name ) {
		return;
	}
	wp( [
		'option',
		'patch',
		'insert',
		'viglet_dumont_settings',
		'overlay_layout',
		'immersive',
	] );
} );

test.afterAll( ( {}, testInfo ) => {
	if ( 'site' !== testInfo.project.name ) {
		return;
	}
	wp( [
		'option',
		'patch',
		'delete',
		'viglet_dumont_settings',
		'overlay_layout',
	] );
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
			'viglet-dumont-recent-searches',
			JSON.stringify( [ 'workshop' ] )
		)
	);
	await page.keyboard.press( 'Control+K' );
	await expect( overlay( page ) ).toHaveClass( /--immersive/ );
	await expect(
		overlay( page ).getByRole( 'group', { name: 'Recent searches' } )
	).toBeVisible();

	await overlay( page )
		.getByRole( 'option', { name: 'workshop', exact: true } )
		.click();
	const posts = overlay( page ).getByRole( 'group', { name: 'Posts' } );
	await expect( posts.getByRole( 'option' ).first() ).toContainText(
		'Workshop on'
	);
	await look( 'overlay-immersive' );
	await axe( 'the immersive instant search', {
		include: 'dialog.viglet-dumont-overlay',
	} );

	await posts.getByRole( 'option', { name: 'See all Posts' } ).click();
	await expect( page ).toHaveURL( /[?&]q=workshop/ );
	expect( new URL( page.url() ).searchParams.getAll( 'fq[]' ) ).toEqual( [
		'type:post',
	] );
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
		include: 'dialog.viglet-dumont-overlay',
	} );
} );
