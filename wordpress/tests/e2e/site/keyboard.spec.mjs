// The search block from the keyboard alone: every control reached with Tab, with its focus
// visible, and used with Enter or Space. Desktop only (playwright.config.mjs).
import { expect, test } from '../fixtures.mjs';

const block = ( page ) => page.locator( '.viglet-dumont-search' );
const summary = ( page ) =>
	block( page ).locator( '.viglet-dumont-search__summary' );
const inBlock = { include: '.viglet-dumont-search' };

test( 'search, filter, remove the filter and change page without a mouse', async ( {
	page,
	seed,
	keyboard,
} ) => {
	await page.goto( `/?page_id=${ seed.searchPageId }` );

	const input = block( page ).locator( 'input[type="search"]' );
	await keyboard.tabTo( input );
	await page.keyboard.type( 'workshop' );
	// Not Escape: on a search field the browser clears the value with it.
	await page.keyboard.press( 'Enter' );
	await expect( summary( page ) ).toHaveText( '4 results' );

	const facet = block( page )
		.locator( '.viglet-dumont-search__facets label' )
		.filter( { hasText: '40' } )
		.locator( 'input' );
	await keyboard.tabTo( facet );
	await page.keyboard.press( 'Space' );
	await expect( summary( page ) ).toHaveText( '3 results' );

	const chip = block( page ).locator( '.viglet-dumont-search__chip' );
	await keyboard.tabTo( chip );
	await page.keyboard.press( 'Enter' );
	await expect( summary( page ) ).toHaveText( '4 results' );

	await page.goto( `/?page_id=${ seed.searchPageId }&q=*` );
	const second = block( page )
		.locator( '.viglet-dumont-search__pages button' )
		.nth( 1 );
	await expect( second ).toBeVisible();
	await keyboard.tabTo( second, { max: 120 } );
	await page.keyboard.press( 'Enter' );
	await expect( second ).toHaveAttribute( 'aria-current', 'true' );
} );

test( 'the correction is reached and taken from the keyboard', async ( {
	page,
	seed,
	keyboard,
} ) => {
	await page.goto( `/?page_id=${ seed.searchPageId }&q=worskhop` );
	const correction = block( page ).locator(
		'.viglet-dumont-search__correction button'
	);
	await expect( correction ).toBeVisible();
	await keyboard.tabTo( correction );
	await page.keyboard.press( 'Enter' );
	await expect( summary( page ) ).toHaveText( '4 results' );
} );

test( 'the search box is a combobox: arrows choose a suggestion, Enter takes it, Escape closes', async ( {
	page,
	seed,
	keyboard,
	axe,
} ) => {
	await page.goto( `/?page_id=${ seed.searchPageId }` );
	const input = block( page ).locator( 'input[role="combobox"]' );
	await keyboard.tabTo( input );
	await page.keyboard.type( 'work' );
	await expect( input ).toHaveAttribute( 'aria-expanded', 'true' );
	const options = block( page ).locator( '[role="listbox"] [role="option"]' );
	await expect( options.filter( { hasText: 'workshop' } ) ).toBeVisible();
	// The typed part is bold in every suggestion that starts with it.
	await expect(
		options.filter( { hasText: 'workshop' } ).locator( 'strong' )
	).toHaveText( 'work' );
	await axe( 'search box with its list open', inBlock );

	// Down until the active option is "workshop"; the field names it as active.
	for ( let i = 0; i < 8; i++ ) {
		await page.keyboard.press( 'ArrowDown' );
		if (
			'workshop' ===
			( await options
				.and( page.locator( '[aria-selected="true"]' ) )
				.textContent()
				.catch( () => '' ) )
		) {
			break;
		}
	}
	const active = options.and( page.locator( '[aria-selected="true"]' ) );
	await expect( active ).toHaveText( 'workshop' );
	await expect( input ).toHaveAttribute(
		'aria-activedescendant',
		( await active.getAttribute( 'id' ) ) || 'missing'
	);
	await page.keyboard.press( 'Enter' );
	await expect( input ).toHaveValue( 'workshop' );
	await expect( summary( page ) ).toHaveText( '4 results' );
	await expect( input ).toHaveAttribute( 'aria-expanded', 'false' );

	// Escape closes an open list and keeps what was typed.
	await page.keyboard.press( 'Backspace' );
	await expect( input ).toHaveAttribute( 'aria-expanded', 'true' );
	await page.keyboard.press( 'Escape' );
	await expect( input ).toHaveAttribute( 'aria-expanded', 'false' );
	await expect( input ).toHaveValue( 'worksho' );
} );

test( 'an empty box offers the recent searches, and Shift+Delete forgets one', async ( {
	page,
	seed,
	keyboard,
} ) => {
	await page.goto( `/?page_id=${ seed.searchPageId }` );
	const input = block( page ).locator( 'input[role="combobox"]' );
	await keyboard.tabTo( input );
	await page.keyboard.type( 'seminar' );
	await page.keyboard.press( 'Enter' );
	await expect( summary( page ) ).toHaveText( /results/ );

	// The clear button empties the box and hands the focus back to it.
	await keyboard.tabTo(
		block( page ).locator( '.viglet-dumont-search__clear' )
	);
	await page.keyboard.press( 'Enter' );
	await expect( input ).toBeFocused();
	await expect( input ).toHaveValue( '' );

	await page.keyboard.press( 'ArrowDown' );
	const recent = block( page ).locator( '[role="option"]' );
	await expect( recent ).toHaveText( [ 'seminar' ] );
	await page.keyboard.press( 'Shift+Delete' );
	await expect( recent ).toHaveCount( 0 );
	await expect( input ).toHaveAttribute( 'aria-expanded', 'false' );
} );

test( 'results follow the typing without a submit, a half-typed word included', async ( {
	page,
	seed,
} ) => {
	await page.goto( `/?page_id=${ seed.searchPageId }` );
	await block( page )
		.locator( 'input[role="combobox"]' )
		.pressSequentially( 'worksh', { delay: 30 } );
	await expect( summary( page ) ).toHaveText( '4 results' );
	// The address keeps what was typed, not the prefix query sent for it.
	await expect( page ).toHaveURL( /q=worksh(&|$)/ );
} );
