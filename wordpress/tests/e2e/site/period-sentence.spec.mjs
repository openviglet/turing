// A period read from a sentence with no language model (D324): on an agenda, "em <month>"
// offers the month on the event date, and taking it lists only what happens that month. Turing's
// nl-filters answers that it has no model, so the reading is the block's own. The mapping and
// the meta are put back afterwards. Desktop only: it reindexes twice.
import { expect, test } from '../fixtures.mjs';
import { wp } from '../global-setup.mjs';

const ptMonths = [
	'janeiro',
	'fevereiro',
	'março',
	'abril',
	'maio',
	'junho',
	'julho',
	'agosto',
	'setembro',
	'outubro',
	'novembro',
	'dezembro',
];

/**
 * The last line WP-CLI printed, so a notice before it is no answer.
 *
 * @param {string[]} args The wp arguments.
 */
const ask = ( args ) => wp( args ).trim().split( /\r?\n/ ).pop().trim();

const now = new Date();
// The 15th of next month, and of the month after: far from any edge of either.
const nextMonth = new Date( now.getFullYear(), now.getMonth() + 1, 15 );
const later = new Date( now.getFullYear(), now.getMonth() + 2, 15 );
/**
 * A day as yyyy-mm-dd.
 *
 * @param {Date} d The day.
 */
const ymd = ( d ) =>
	`${ d.getFullYear() }-${ String( d.getMonth() + 1 ).padStart(
		2,
		'0'
	) }-15`;

let saved = null;
let inMonth = '';
let outside = '';

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
		$m['post']['fields'][] = array( 'source' => 'meta:event_day', 'field' => 'event_day', 'type' => 'DATE', 'facet_label' => 'When', 'range' => true, 'periods' => 'agenda' );
		update_option( 'viglet_dumont_mapping', $m );`,
	] );
	wp( [
		'post',
		'meta',
		'update',
		String( seed.postId ),
		'event_day',
		ymd( nextMonth ),
	] );
	wp( [
		'post',
		'meta',
		'update',
		String( seed.relatedPostId ),
		'event_day',
		ymd( later ),
	] );
	inMonth = ask( [
		'post',
		'get',
		String( seed.postId ),
		'--field=post_title',
	] );
	outside = ask( [
		'post',
		'get',
		String( seed.relatedPostId ),
		'--field=post_title',
	] );
	wp( [ 'dumont', 'reindex' ] );
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
	for ( const id of [ seed.postId, seed.relatedPostId ] ) {
		wp( [ 'post', 'meta', 'delete', String( id ), 'event_day' ] );
	}
	wp( [ 'dumont', 'reindex' ] );
} );

test( 'a month named in a sentence filters the agenda to it, with no language model', async ( {
	page,
	seed,
}, testInfo ) => {
	test.skip( 'site' !== testInfo.project.name, 'desktop only' );
	await page.route( '**/nl-filters', ( route ) =>
		route.fulfill( { json: { available: false } } )
	);
	const block = page.locator( '.viglet-dumont-search' );
	const sentence = `em ${ ptMonths[ nextMonth.getMonth() ] }`;
	await page.goto(
		`/?page_id=${ seed.askPageId }&q=${ encodeURIComponent( sentence ) }`
	);

	const chip = block.locator( '.viglet-dumont-search__chip--suggested' );
	await expect( chip ).toHaveCount( 1 );
	await expect( chip ).toContainText( /When: \w{3} 1, \d{4} – / );
	await chip.click();

	await expect(
		block.locator( '.viglet-dumont-search__summary' )
	).toHaveText( /result/ );
	const titles = await block
		.locator(
			'.viglet-dumont-search__results .viglet-dumont-search__title'
		)
		.allTextContents();
	expect( titles ).toContain( inMonth );
	expect( titles ).not.toContain( outside );
} );
