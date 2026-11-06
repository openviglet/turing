// Openings close on a day and leave search after it (D325, D308): the summer camp call, closed two
// days before the seed ran, is never found though its page is published; the library assistant
// post, saved with yesterday as its closing day, leaves the openings page, and moved forward it
// is back. Its day is put back afterwards.
import { expect, test } from '../fixtures.mjs';
import {
	ask,
	drainQueue,
	idOf,
	onSchool,
	titleOf,
	titlesAfterCommit,
	titlesOn,
} from './school.mjs';

/**
 * A day relative to today, as yyyy-mm-dd.
 *
 * @param {number} days Days from today.
 */
const day = ( days ) =>
	new Date( Date.now() + days * 86400000 ).toISOString().slice( 0, 10 );

let id = '';
let closes = '';

test.beforeAll( () => {
	id = idOf( 'library-assistant' );
	closes = ask( [ 'post', 'meta', 'get', id, 'closes_on' ] );
} );

test.afterAll( () => {
	onSchool( [ 'post', 'meta', 'update', id, 'closes_on', closes ] );
	onSchool( [ 'turing', 'index', `--ids=${ id }` ] );
} );

/**
 * Set the opening's closing day and save it, as the school's office would, then send the queue.
 *
 * @param {import('@playwright/test').Page} page The page.
 * @param {string}                          date Its last day.
 */
async function closeOn( page, date ) {
	onSchool( [ 'post', 'meta', 'update', id, 'closes_on', date ] );
	onSchool( [
		'post',
		'update',
		id,
		`--post_title=${ titleOf( 'library-assistant' ) }`,
	] );
	await drainQueue( page );
}

test( 'an opening already closed is not among the openings', async ( {
	page,
	look,
} ) => {
	const titles = await titlesOn( page, '/openings/', '*' );
	expect( titles ).toContain( titleOf( 'arts-scholarship' ) );
	expect( titles ).not.toContain( titleOf( 'summer-monitors' ) );
	expect(
		ask( [
			'post',
			'get',
			idOf( 'summer-monitors' ),
			'--field=post_status',
		] )
	).toBe( 'publish' );
	await look( 'demo-edu-openings' );
} );

test( 'an opening saved with yesterday as its last day leaves search, and moved on it is back', async ( {
	page,
} ) => {
	const title = titleOf( 'library-assistant' );
	expect( await titlesOn( page, '/openings/', '*' ) ).toContain( title );

	await closeOn( page, day( -1 ) );
	expect( await titlesAfterCommit( page, '/openings/', '*' ) ).not.toContain(
		title
	);

	await closeOn( page, day( 10 ) );
	expect( await titlesAfterCommit( page, '/openings/', '*' ) ).toContain(
		title
	);
} );
