// A type's result cards show one of its mapped DATE fields as their date (D305): a post
// whose day-only meta says 2026-11-20 shows that day, written the pt_BR way, and not the
// day before (a day-only date is midnight UTC, and the stack's visitors are west of it).
// The mapping and the meta are put back afterwards, so no other spec sees them.
import { expect, test } from '../fixtures.mjs';
import { wp } from '../global-setup.mjs';

/**
 * The last line WP-CLI printed, so a notice before it is no answer.
 *
 * @param {string[]} args The wp arguments.
 */
const ask = ( args ) => wp( args ).trim().split( /\r?\n/ ).pop().trim();

let saved = null;

test.beforeAll( ( { seed } ) => {
	saved = ask( [
		'option',
		'get',
		'viglet_dumont_mapping',
		'--format=json',
	] );
	wp( [
		'eval',
		`$m = get_option( 'viglet_dumont_mapping' );
		$m['post']['fields'][] = array( 'source' => 'meta:event_day', 'field' => 'event_day', 'type' => 'DATE', 'facet_label' => 'Dia', 'card' => true );
		$m['post']['card_date'] = 'event_day';
		update_option( 'viglet_dumont_mapping', $m );`,
	] );
	wp( [
		'post',
		'meta',
		'update',
		String( seed.postId ),
		'event_day',
		'2026-11-20',
	] );
	wp( [ 'dumont', 'reindex' ] );
} );

test.afterAll( ( { seed } ) => {
	wp( [
		'option',
		'update',
		'viglet_dumont_mapping',
		saved,
		'--format=json',
	] );
	wp( [ 'post', 'meta', 'delete', String( seed.postId ), 'event_day' ] );
	wp( [ 'dumont', 'reindex' ] );
} );

test( "a type's card date is its mapped DATE field, in pt_BR", async ( {
	page,
	seed,
} ) => {
	const title = ask( [
		'post',
		'get',
		String( seed.postId ),
		'--field=post_title',
	] );
	await page.goto(
		`/?page_id=${ seed.searchPageId }&q=${ encodeURIComponent( title ) }`
	);
	const result = page
		.locator( '.viglet-dumont-search__result' )
		.filter( { hasText: title } )
		.first();

	// "20 de nov. de 2026" from the script, "20 de novembro de 2026" on the first paint.
	await expect( result.locator( 'time' ) ).toHaveText( /^20 de nov/ );
	await expect( result.locator( '.viglet-dumont-search__field' ) ).toHaveText(
		/Dia:?\s*20 de nov/
	);
} );
