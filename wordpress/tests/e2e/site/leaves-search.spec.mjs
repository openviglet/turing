// A post leaves search when its type's "Leaves search on" date has passed (D308): saved with
// yesterday's date it is not found, and moved forward and saved again it is back. The
// mapping and the meta are put back afterwards. Desktop only: it reindexes.
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
let title = '';

/**
 * Set the post's date and save it, as an editor would, then let the queue run.
 *
 * @param {number} postId The post.
 * @param {string} date   Its last day in search.
 */
function saveWith( postId, date ) {
	wp( [ 'post', 'meta', 'update', String( postId ), 'deadline', date ] );
	wp( [ 'post', 'update', String( postId ), `--post_title=${ title }` ] );
	wp( [ 'dumont', 'queue', 'run' ] );
}

/**
 * Whether the search page finds the post by its title.
 *
 * @param {import('@playwright/test').Page} page The page.
 * @param {Object}                          seed The seeded ids.
 */
async function found( page, seed ) {
	// Turing commits within a few seconds of an import or a delete.
	await page.waitForTimeout( 3000 );
	await page.goto(
		`/?page_id=${ seed.searchPageId }&q=${ encodeURIComponent( title ) }`
	);
	await expect( page.locator( '.viglet-dumont-search__summary' ) ).toHaveText(
		/result/
	);
	return (
		await page
			.locator(
				'.viglet-dumont-search__results .viglet-dumont-search__title'
			)
			.allTextContents()
	).includes( title );
}

test.beforeAll( ( { seed }, testInfo ) => {
	if ( 'site' !== testInfo.project.name ) {
		return;
	}
	saved = ask( [
		'option',
		'get',
		'viglet_dumont_mapping',
		'--format=json',
	] );
	wp( [
		'eval',
		`$m = get_option( 'viglet_dumont_mapping' );
		$m['post']['fields'][] = array( 'source' => 'meta:deadline', 'field' => 'deadline', 'type' => 'DATE' );
		$m['post']['expires'] = 'deadline';
		update_option( 'viglet_dumont_mapping', $m );`,
	] );
	title = ask( [
		'post',
		'get',
		String( seed.postId ),
		'--field=post_title',
	] );
} );

test.afterAll( ( { seed }, testInfo ) => {
	if ( 'site' !== testInfo.project.name ) {
		return;
	}
	wp( [
		'option',
		'update',
		'viglet_dumont_mapping',
		saved,
		'--format=json',
	] );
	wp( [ 'post', 'meta', 'delete', String( seed.postId ), 'deadline' ] );
	wp( [ 'dumont', 'index', `--ids=${ seed.postId }` ] );
} );

test.beforeEach( ( {}, testInfo ) => {
	test.skip( 'site' !== testInfo.project.name, 'desktop only' );
} );

test( 'a post whose date has passed is not found, and moved forward it is back', async ( {
	page,
	seed,
} ) => {
	saveWith( seed.postId, day( -1 ) );
	expect( await found( page, seed ) ).toBe( false );

	saveWith( seed.postId, day( 5 ) );
	expect( await found( page, seed ) ).toBe( true );
} );
