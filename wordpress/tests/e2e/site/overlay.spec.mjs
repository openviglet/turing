// The instant-search overlay: opened with Ctrl+K or from the theme's own Search block,
// driven from the keyboard, closed with Escape and the focus handed back. Desktop only
// (playwright.config.mjs).
import { expect, test } from '../fixtures.mjs';

const overlay = ( page ) => page.locator( 'dialog.viglet-dumont-overlay' );
const input = ( page ) => overlay( page ).getByRole( 'combobox' );
const options = ( page ) => overlay( page ).getByRole( 'option' );

test( 'Ctrl+K opens it, it answers as you type, and Up/Down plus Enter open a result', async ( {
	page,
	seed,
	look,
	axe,
} ) => {
	await page.goto( `/?page_id=${ seed.corePageId }` );
	await page.keyboard.press( 'Control+K' );
	await expect( overlay( page ) ).toBeVisible();
	await expect( input( page ) ).toBeFocused();

	await page.keyboard.type( 'worksh' );
	await expect( options( page ) ).toHaveCount( 4 );
	await expect( options( page ).first() ).toContainText( 'Workshop on' );
	await look( 'overlay' );
	await axe( 'the instant-search overlay', {
		include: 'dialog.viglet-dumont-overlay',
	} );

	await page.keyboard.press( 'ArrowDown' );
	await expect( options( page ).first() ).toHaveAttribute(
		'aria-selected',
		'true'
	);
	await expect( input( page ) ).toHaveAttribute(
		'aria-activedescendant',
		'viglet-dumont-overlay-result-0'
	);
	const title = await options( page )
		.first()
		.locator( '.viglet-dumont-overlay__title' )
		.textContent();
	await page.keyboard.press( 'Enter' );
	await expect( page ).toHaveTitle( new RegExp( `^${ title }` ) );
} );

test( "the theme's Search block opens it, and Enter takes the query to the full search page", async ( {
	page,
	seed,
} ) => {
	await page.goto( `/?page_id=${ seed.corePageId }` );
	const core = page.locator( 'form[data-viglet-dumont-overlay]' );
	await expect( core ).toHaveCount( 1 );
	await core.locator( 'input[type="search"]' ).click();
	await expect( overlay( page ) ).toBeVisible();

	await page.keyboard.type( 'workshop' );
	await expect( options( page ) ).toHaveCount( 4 );
	await page.keyboard.press( 'Enter' );
	// The page holding the Turing search block, found without any setting.
	await expect( page ).toHaveURL(
		new RegExp( `page_id=${ seed.searchPageId }.*q=workshop` )
	);
	await expect( page.locator( '.viglet-dumont-search__summary' ) ).toHaveText(
		'4 results'
	);
} );

test( 'Escape closes it and gives the focus back where it was', async ( {
	page,
	seed,
	keyboard,
} ) => {
	await page.goto( `/?page_id=${ seed.corePageId }` );
	const button = page
		.locator( 'form[data-viglet-dumont-overlay]' )
		.getByRole( 'button', { name: 'Search' } );
	await keyboard.tabTo( button );
	await page.keyboard.press( 'Control+K' );
	await expect( overlay( page ) ).toBeVisible();
	await page.keyboard.press( 'Escape' );
	await expect( overlay( page ) ).toBeHidden();
	await expect( button ).toBeFocused();
} );
