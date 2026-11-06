// A date's range facet with Agenda periods (D307): a post whose day is three days away is
// under "Next 7 days" and "Upcoming", and one from five weeks ago under "Past" only. The
// mapping and the meta are put back afterwards. Desktop only: it reindexes twice.
import { expect, test } from '../fixtures.mjs';
import { wp } from '../global-setup.mjs';

/**
 * The last line WP-CLI printed, so a notice before it is no answer.
 *
 * @param {string[]} args The wp arguments.
 */
const ask = ( args ) => wp( args ).trim().split( /\r?\n/ ).pop().trim();

/**
 * A day relative to today, as yyyy-mm-dd.
 *
 * @param {number} days Days from today.
 */
const day = ( days ) =>
	new Date( Date.now() + days * 86400000 ).toISOString().slice( 0, 10 );

let saved = null;
let soon = '';
let past = '';
let today = '';

test.beforeAll( ( { seed }, testInfo ) => {
	if ( 'site' !== testInfo.project.name ) {
		return;
	}
	saved = ask( [
		'option',
		'get',
		'viglet_turing_mapping',
		'--format=json',
	] );
	wp( [
		'eval',
		`$m = get_option( 'viglet_turing_mapping' );
		$m['post']['fields'][] = array( 'source' => 'meta:event_day', 'field' => 'event_day', 'type' => 'DATE', 'facet_label' => 'When', 'range' => true, 'periods' => 'agenda' );
		update_option( 'viglet_turing_mapping', $m );`,
	] );
	wp( [
		'post',
		'meta',
		'update',
		String( seed.postId ),
		'event_day',
		day( 3 ),
	] );
	wp( [
		'post',
		'meta',
		'update',
		String( seed.relatedPostId ),
		'event_day',
		day( -35 ),
	] );
	// A day-only date is midnight UTC, the instant Past used to end on (D333).
	wp( [
		'post',
		'meta',
		'update',
		String( seed.templatedPostId ),
		'event_day',
		day( 0 ),
	] );
	today = ask( [
		'post',
		'get',
		String( seed.templatedPostId ),
		'--field=post_title',
	] );
	soon = ask( [
		'post',
		'get',
		String( seed.postId ),
		'--field=post_title',
	] );
	past = ask( [
		'post',
		'get',
		String( seed.relatedPostId ),
		'--field=post_title',
	] );
	wp( [ 'turing', 'reindex' ] );
} );

test.afterAll( ( { seed }, testInfo ) => {
	if ( 'site' !== testInfo.project.name ) {
		return;
	}
	wp( [
		'option',
		'update',
		'viglet_turing_mapping',
		saved,
		'--format=json',
	] );
	for ( const id of [
		seed.postId,
		seed.relatedPostId,
		seed.templatedPostId,
	] ) {
		wp( [ 'post', 'meta', 'delete', String( id ), 'event_day' ] );
	}
	wp( [ 'turing', 'reindex' ] );
} );

test.beforeEach( ( {}, testInfo ) => {
	test.skip( 'site' !== testInfo.project.name, 'desktop only' );
} );

/**
 * The titles the search page lists under one period.
 *
 * @param {import('@playwright/test').Page} page   The page.
 * @param {Object}                          seed   The seeded ids.
 * @param {string}                          period The period's label.
 */
async function under( page, seed, period ) {
	await page.goto(
		`/?page_id=${ seed.searchPageId }&q=*&fq[]=${ encodeURIComponent(
			`event_day_range:${ period }`
		) }`
	);
	await expect( page.locator( '.viglet-turing-search__summary' ) ).toHaveText(
		/result/
	);
	return page
		.locator(
			'.viglet-turing-search__results .viglet-turing-search__title'
		)
		.allTextContents();
}

test( 'an agenda puts a day this week under Next 7 days and Upcoming, and last month under Past', async ( {
	page,
	seed,
} ) => {
	for ( const period of [ 'Next 7 days', 'Upcoming' ] ) {
		const titles = await under( page, seed, period );
		expect( titles, period ).toContain( soon );
		expect( titles, period ).not.toContain( past );
	}
	const before = await under( page, seed, 'Past' );
	expect( before ).toContain( past );
	expect( before ).not.toContain( soon );
	// D333: today's event is Today, Next 7 days and Upcoming, never Past.
	expect( before ).not.toContain( today );
	for ( const period of [ 'Today', 'Next 7 days', 'Upcoming' ] ) {
		expect( await under( page, seed, period ), period ).toContain( today );
	}
	expect( await under( page, seed, 'Today' ) ).not.toContain( soon );
} );
