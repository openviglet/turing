// A hung Turing slows only the search (D328): with the plugin pointed at an address that never
// answers, a page with the search block and a post with Related each render within a few
// seconds, the next view at once, and the theme's search falls back to WordPress's own at once.
// The address is put back afterwards. Desktop only: it changes the site's settings.
import { expect, test } from '../fixtures.mjs';
import { wp } from '../global-setup.mjs';

// TEST-NET-1 (RFC 5737): routed nowhere, so a connection hangs until the client gives up.
const blackhole = 'http://192.0.2.1:2700';
let saved = '';

/** PHP that clears the "Turing is down" mark and every cached Related answer. */
const clearMarks = `delete_transient( Viglet_Dumont_API_Client::DOWN_TRANSIENT );
global $wpdb;
$wpdb->query( "DELETE FROM {$wpdb->options} WHERE option_name LIKE '\\\\_transient%viglet\\\\_dumont\\\\_related\\\\_%'" );`;

/**
 * The seconds one server-side render of a URL takes.
 *
 * @param {import('@playwright/test').APIRequestContext} request The request context.
 * @param {string}                                       url     The page.
 */
async function seconds( request, url ) {
	const start = Date.now();
	const response = await request.get( url, { timeout: 60_000 } );
	expect( response.status() ).toBe( 200 );
	return ( Date.now() - start ) / 1000;
}

test.beforeAll( ( {}, testInfo ) => {
	if ( 'site' !== testInfo.project.name ) {
		return;
	}
	saved = wp( [
		'eval',
		`echo ( (array) get_option( 'viglet_dumont_settings', array() ) )['server_url'] ?? '';`,
	] )
		.trim()
		.split( /\r?\n/ )
		.pop()
		.trim();
	wp( [ 'eval', clearMarks ] );
	wp( [
		'eval',
		`$s = (array) get_option( 'viglet_dumont_settings', array() ); $s['server_url'] = ${ JSON.stringify(
			blackhole
		) }; update_option( 'viglet_dumont_settings', $s );`,
	] );
} );

test.afterAll( ( {}, testInfo ) => {
	if ( 'site' !== testInfo.project.name ) {
		return;
	}
	const restore = saved
		? `$s['server_url'] = ${ JSON.stringify( saved ) };`
		: `unset( $s['server_url'] );`;
	wp( [
		'eval',
		`$s = (array) get_option( 'viglet_dumont_settings', array() ); ${ restore } update_option( 'viglet_dumont_settings', $s );`,
	] );
	wp( [ 'eval', clearMarks ] );
} );

test( 'a hung Turing costs one page a few seconds and the next ones nothing', async ( {
	request,
	seed,
}, testInfo ) => {
	test.skip( 'site' !== testInfo.project.name, 'desktop only' );
	test.setTimeout( 180_000 );

	// The first page read waits the render timeout once, then marks Turing down.
	expect(
		await seconds( request, `/?page_id=${ seed.searchPageId }&q=workshop` )
	).toBeLessThan( 6 );
	// Every later page skips it: Related, the search block again, the theme's search.
	expect(
		await seconds( request, `/?p=${ seed.relatedPostId }` )
	).toBeLessThan( 3 );
	expect(
		await seconds( request, `/?page_id=${ seed.searchPageId }&q=workshop` )
	).toBeLessThan( 3 );
	const start = Date.now();
	const theme = await request.get( '/?s=workshop', { timeout: 60_000 } );
	expect( ( Date.now() - start ) / 1000 ).toBeLessThan( 3 );
	// WordPress answered the search itself.
	expect( await theme.text() ).toMatch( /workshop/i );
} );
