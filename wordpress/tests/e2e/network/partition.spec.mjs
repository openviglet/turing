// Each site of the network holds its own API key, bound to its site key (D190): asked
// straight to Turing, the main site's key never reads, lists or deletes a document of the
// second site. Runs only with E2E_MULTISITE=1, against a Turing with partitioned keys on
// every route the plugin calls (Turing T1209).
import { expect, test } from '../fixtures.mjs';
import { networkSites, wp } from '../global-setup.mjs';

const turing = process.env.TURING_URL || 'http://localhost:2700';
const snSite = process.env.SN_MS_SITE || 'Network';
const locale = process.env.TURING_LOCALE || 'en_US';

/**
 * The last line WP-CLI printed for a site, so a notice before it is no answer.
 *
 * @param {string}   url  The site.
 * @param {string[]} args The wp arguments.
 */
const ask = ( url, args ) => {
	const lines = wp( [ `--url=${ url }`, ...args ], 'cli-ms' )
		.trim()
		.split( /\r?\n/ );
	return lines[ lines.length - 1 ].trim();
};

const sites = networkSites.map( ( url ) => ( {
	key: ask( url, [ 'option', 'pluck', 'viglet_turing_settings', 'api_key' ] ),
	siteKey: ask( url, [
		'eval',
		'echo ( new Viglet_Turing_Indexer() )->site_key();',
	] ),
} ) );
const [ main, second ] = sites;

/**
 * Every document a key's search returns, as its id and site key.
 *
 * @param {import('@playwright/test').APIRequestContext} request The request context.
 * @param {string}                                       key     The API key.
 */
async function everything( request, key ) {
	const response = await request.post(
		`${ turing }/api/sn/${ encodeURIComponent( snSite ) }/search/query`,
		{
			headers: { Key: key },
			data: { q: '*', locale, rows: 200 },
		}
	);
	expect( response.status() ).toBe( 200 );
	const body = await response.json();
	return ( body.results?.document || [] ).map( ( document ) => ( {
		id: String( document.fields?.id ),
		siteKey: String( document.fields?.site_key ),
	} ) );
}

test( 'the two sites hold different keys, each bound to its own site key', () => {
	expect( main.key ).not.toBe( '' );
	expect( second.key ).not.toBe( '' );
	expect( main.key ).not.toBe( second.key );
	expect( main.siteKey ).not.toBe( second.siteKey );
} );

test( 'a site’s key lists only its own documents', async ( { request } ) => {
	for ( const site of sites ) {
		const found = await everything( request, site.key );
		expect( found.length ).toBeGreaterThan( 0 );
		expect( [ ...new Set( found.map( ( d ) => d.siteKey ) ) ] ).toEqual( [
			site.siteKey,
		] );
	}
} );

test( 'the main site’s key neither reads nor deletes a document of the second site', async ( {
	request,
} ) => {
	const [ theirs ] = await everything( request, second.key );
	expect( theirs ).toBeTruthy();
	const read = ( key ) =>
		request.get(
			`${ turing }/api/sn/${ encodeURIComponent(
				snSite
			) }/search/document`,
			{
				headers: { Key: key },
				params: { id: theirs.id, _setlocale: locale },
			}
		);

	expect( ( await read( main.key ) ).status() ).toBe( 404 );

	const deleted = await request.post( `${ turing }/api/sn/import`, {
		headers: { Key: main.key },
		data: {
			turingDocuments: [
				{
					turSNJobAction: 'DELETE',
					siteNames: [ snSite ],
					locale,
					attributes: { id: theirs.id },
				},
			],
		},
	} );
	expect( deleted.status() ).toBe( 403 );

	// The refused delete left it in place: its own site still reads it.
	expect( ( await read( second.key ) ).status() ).toBe( 200 );
} );
