// Turing's generative calls on an SN site the network's two sites share (D287): the copilot and
// Build my kit are asked from a visitor's browser, with no key, so each site sends its site key
// as `filters` (Turing T1213), and what comes back cites and stages its own documents only. Each
// site is asked about the other's documents, which an unfiltered call would find. Runs only with
// E2E_MULTISITE=1, against a Turing that carries T1213 and has a language model for the copilot.
import { expect, test } from '../fixtures.mjs';
import { networkSites, wp } from '../global-setup.mjs';

const turing = process.env.TURING_URL || 'http://localhost:2700';
const snSite = encodeURIComponent( process.env.SN_MS_SITE || 'Network' );
const locale = process.env.TURING_LOCALE || 'en_US';

const [ main, second ] = networkSites.map( ( url ) => {
	const lines = wp(
		[
			`--url=${ url }`,
			'eval',
			'echo ( new Viglet_Dumont_Indexer() )->site_key();',
		],
		'cli-ms'
	)
		.trim()
		.split( /\r?\n/ );
	return lines[ lines.length - 1 ].trim();
} );

// One question both sites are asked: the main site's workshops answer it, so the main site cites
// some, and the second site, which has none, must cite none of the main site's.
const question = 'Which workshops are there on search and facets?';
const asked = [
	{ siteKey: main, holds: true },
	{ siteKey: second, holds: false },
];

test.beforeEach( async ( { request } ) => {
	const probe = await request.get(
		`${ turing }/api/sn/${ snSite }/copilot/available`
	);
	test.skip(
		! ( await probe.json() ).available,
		'the copilot needs a language model on this Turing'
	);
} );

for ( const { siteKey, holds } of asked ) {
	test( `the copilot asked on ${ siteKey } cites its own documents only`, async ( {
		request,
	} ) => {
		const response = await request.post(
			`${ turing }/api/sn/${ snSite }/copilot`,
			{
				data: {
					messages: [ { role: 'user', content: question } ],
					locale,
					filters: { site_key: [ siteKey ] },
				},
			}
		);
		expect( response.status() ).toBe( 200 );
		const ids = ( ( await response.json() ).citations || [] ).map( ( c ) =>
			String( c.id )
		);
		if ( holds ) {
			expect( ids.length ).toBeGreaterThan( 0 );
		}
		for ( const id of ids ) {
			expect( id ).toMatch( new RegExp( `^${ siteKey }:` ) );
		}
	} );

	test( `the kit planned on ${ siteKey } stages its own documents only`, async ( {
		request,
	} ) => {
		const response = await request.post(
			`${ turing }/api/sn/${ snSite }/copilot/kit`,
			{
				data: {
					plan: question,
					locale,
					productTypes: [ 'post' ],
					guideTypes: [],
					filters: { site_key: [ siteKey ] },
				},
				timeout: 180_000,
			}
		);
		expect( response.status() ).toBe( 200 );
		const kit = await response.json();
		const ids = ( kit.stages || [] )
			.flatMap( ( stage ) => stage.products || [] )
			.map( ( product ) => String( product.id ) );
		if ( holds ) {
			expect( ids.length ).toBeGreaterThan( 0 );
		}
		for ( const id of ids ) {
			expect( id ).toMatch( new RegExp( `^${ siteKey }:` ) );
		}
	} );
}
