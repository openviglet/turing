// A school's cards say what a visitor came for (D325, D305): an event's card shows the day it
// happens and its poster, mapped on the event type, and an opening's the day it closes; neither
// shows the day it was posted, which is the seed's own day.
import { expect, test } from '../fixtures.mjs';
import { ask, idOf, titleOf, titlesOn } from './school.mjs';

/**
 * The card a page lists for a title.
 *
 * @param {import('@playwright/test').Page} page  The page.
 * @param {string}                          title The result's title.
 */
const card = ( page, title ) =>
	page
		.locator( '.viglet-turing-search__result' )
		.filter( { hasText: title } )
		.first();

test( "an event's card shows its day and its poster", async ( { page } ) => {
	const id = idOf( 'choir-concert' );
	const day = ask( [ 'post', 'meta', 'get', id, 'event_day' ] );
	const title = titleOf( 'choir-concert' );
	await titlesOn( page, '/agenda/', title );

	const result = card( page, title );
	await expect( result.locator( 'time' ) ).toHaveAttribute(
		'datetime',
		new RegExp( `^${ day }` )
	);
	await expect( result.locator( 'img' ) ).toHaveAttribute(
		'src',
		/lumen-poster-choir-concert/
	);
} );

test( "an opening's card shows the day it closes", async ( { page } ) => {
	const id = idOf( 'arts-scholarship' );
	const closes = ask( [ 'post', 'meta', 'get', id, 'closes_on' ] );
	const posted = ask( [ 'post', 'get', id, '--field=post_date' ] ).slice(
		0,
		10
	);
	const title = titleOf( 'arts-scholarship' );
	await titlesOn( page, '/openings/', title );

	const time = card( page, title ).locator( 'time' );
	await expect( time ).toHaveAttribute(
		'datetime',
		new RegExp( `^${ closes }` )
	);
	expect( closes ).not.toBe( posted );
} );
