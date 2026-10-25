// The plugin on a pt_BR site (the pt-br project switches the stack's language): every admin
// screen and the search block speak Portuguese, and no string the .po translates shows up
// in English. A leak is an English source string, from viglet-dumont-pt_BR.po, found in the
// plugin's own region of the page.
import { readFileSync } from 'node:fs';
import { expect, test } from '../fixtures.mjs';

/**
 * The .po's entries as { msgid, msgstr }, continuation lines joined.
 *
 * @param {string} source The .po file's text.
 */
function entries( source ) {
	const found = [];
	let current = null;
	let field = null;
	const unquote = ( s ) => JSON.parse( s );
	for ( const line of source.split( /\r?\n/ ) ) {
		const start = line.match( /^(msgid|msgstr) (".*")$/ );
		if ( start ) {
			if ( 'msgid' === start[ 1 ] ) {
				current = { msgid: '', msgstr: '' };
				found.push( current );
			}
			field = start[ 1 ];
			current[ field ] = unquote( start[ 2 ] );
		} else if ( current && field && /^".*"$/.test( line ) ) {
			current[ field ] += unquote( line );
		} else {
			field = null;
		}
	}
	return found;
}

const translated = entries(
	readFileSync( 'src/languages/viglet-dumont-pt_BR.po', 'utf8' )
).filter( ( e ) => e.msgid && e.msgstr && e.msgstr !== e.msgid );

// What would read as a leak: a phrase of two words or more, with no placeholder, whose
// Portuguese differs. A single word (Search, Status) is too often also a proper name.
const leaks = translated
	.map( ( e ) => e.msgid )
	.filter(
		( s ) => ! s.includes( '%' ) && s.trim().split( /\s+/ ).length > 1
	);
const portuguese = translated.map( ( e ) => e.msgstr );

/**
 * Check one region: it holds some Portuguese and no English the .po translates.
 *
 * @param {import('@playwright/test').Page} page
 * @param {string}                          selector The plugin's region.
 * @param {string}                          name     The screen, for the message.
 * @param {string[]}                        skip     Parts of the region to leave out.
 */
async function speaksPortuguese( page, selector, name, skip = [] ) {
	const text = (
		await page
			.locator( selector )
			.first()
			.evaluate( ( region, left ) => {
				// innerText reads only what renders, so the skipped parts are hidden for the read.
				const hidden = left.flatMap( ( s ) => [
					...region.querySelectorAll( s ),
				] );
				hidden.forEach( ( el ) => ( el.style.display = 'none' ) );
				const read = region.innerText;
				hidden.forEach( ( el ) => ( el.style.display = '' ) );
				return read;
			}, skip )
	).replace( /\s+/g, ' ' );
	expect(
		leaks.filter( ( s ) => text.includes( s ) ),
		`${ name }: English strings the .po translates`
	).toEqual( [] );
	expect(
		portuguese.some( ( s ) => text.includes( s ) ),
		`${ name }: some Portuguese`
	).toBe( true );
}

const screens = [
	[
		'Settings, connection',
		'/wp-admin/admin.php?page=viglet-dumont#connection',
	],
	[ 'Settings, indexing', '/wp-admin/admin.php?page=viglet-dumont#indexing' ],
	[ 'Settings, search', '/wp-admin/admin.php?page=viglet-dumont#search' ],
	[ 'Settings, actions', '/wp-admin/admin.php?page=viglet-dumont#actions' ],
	[ 'Setup', '/wp-admin/admin.php?page=viglet-dumont-setup' ],
	[ 'Fields', '/wp-admin/admin.php?page=viglet-dumont-fields' ],
	[ 'Search tuning', '/wp-admin/admin.php?page=viglet-dumont-tuning' ],
	[
		'Health',
		'/wp-admin/admin.php?page=viglet-dumont-health',
		// An event's Result is the message stored in the language it was written in, until D180;
		// only the events table has a fourth column.
		[ 'tbody td:nth-child(4)' ],
	],
];

for ( const [ name, url, skip ] of screens ) {
	test( `${ name } is in Portuguese`, async ( { page, look } ) => {
		await page.goto( url );
		// The React screens render after the page loads, and Settings hides its other tabs;
		// wait for the first heading that shows.
		await expect(
			page.locator( '#wpbody-content :is(h2, h3):visible' ).first()
		).toBeVisible();
		await page.waitForLoadState( 'networkidle' );
		await look( `pt-br-${ name.replace( /\W+/g, '-' ).toLowerCase() }` );
		await speaksPortuguese( page, '#wpbody-content', name, skip );
	} );
}

test( 'the search block is in Portuguese, results and filters included', async ( {
	page,
	seed,
	look,
} ) => {
	await page.goto( `/?page_id=${ seed.searchPageId }&q=workshop` );
	const block = page.locator( '.viglet-dumont-search' );
	await expect(
		block.locator( '.viglet-dumont-search__summary' )
	).toHaveText( '4 resultados' );
	await block
		.locator( '.viglet-dumont-search__facets label' )
		.filter( { hasText: '40' } )
		.locator( 'input' )
		.click();
	await expect(
		block.locator( '.viglet-dumont-search__chip' )
	).toBeVisible();
	await look( 'pt-br-search-block' );
	await speaksPortuguese( page, '.viglet-dumont-search', 'search block' );
} );
