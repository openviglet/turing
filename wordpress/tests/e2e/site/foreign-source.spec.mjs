// A document another source sent under the plugin's site key survives the plugin's Index All,
// sweep and Delete All, and Health counts it (D322): a catalog given the same key shares the
// search, and the plugin never deletes what it did not send.
// Desktop only: it deletes and reindexes the whole site.
import { expect, test } from '../fixtures.mjs';
import { wp } from '../global-setup.mjs';

const foreignId = 'oai:repo.example:d322-1';

/**
 * The last line WP-CLI printed, so a notice before it is no answer.
 *
 * @param {string[]} args The wp arguments.
 */
const ask = ( args ) => wp( args ).trim().split( /\r?\n/ ).pop().trim();

/**
 * PHP that sends one job item for the foreign document, as another source would: its own id
 * shape and `source_apps`, this site's `site_key`.
 *
 * @param {string} action CREATE or DELETE.
 */
const foreign = ( action ) => `
$settings = viglet_dumont_get_settings();
$indexer  = new Viglet_Dumont_Indexer( $settings );
$result   = ( new Viglet_Dumont_API_Client( $settings ) )->import( array( array(
	'locale'         => $settings['locale'],
	'turSNJobAction' => '${ action }',
	'siteNames'      => array( $settings['site_name'] ),
	'attributes'     => '${ action }' === 'CREATE'
		? array(
			'id'          => '${ foreignId }',
			'title'       => 'Harvested thesis on tidal marshes',
			'text'        => 'A record another connector sent under this site key.',
			'type'        => 'thesis',
			'site_key'    => $indexer->site_key(),
			'source_apps' => 'catalog',
		)
		: array( 'id' => '${ foreignId }', 'source_apps' => 'catalog' ),
) ) );
echo $result['success'] ? 'ok' : 'failed';
`;

/** PHP that prints 1 when Turing holds the foreign document under this site key, else 0. */
const held = `
$indexer = new Viglet_Dumont_Indexer( viglet_dumont_get_settings() );
echo in_array( '${ foreignId }', $indexer->list_site_ids() ?? array(), true ) ? 1 : 0;
`;

test.describe( 'another source under the site key', () => {
	test.beforeAll( ( {}, testInfo ) => {
		if ( 'site' !== testInfo.project.name ) {
			return;
		}
		expect( ask( [ 'eval', foreign( 'CREATE' ) ] ) ).toBe( 'ok' );
	} );

	test.afterAll( ( {}, testInfo ) => {
		if ( 'site' !== testInfo.project.name ) {
			return;
		}
		try {
			ask( [ 'eval', foreign( 'DELETE' ) ] );
		} catch {}
		// Delete All emptied the site: put its posts back for the specs after this one.
		wp( [ 'dumont', 'reindex' ] );
	} );

	test( 'survives Index All, the sweep and Delete All, and Health counts it', async ( {}, testInfo ) => {
		test.skip(
			'site' !== testInfo.project.name,
			'Desktop only: it deletes and reindexes the whole site.'
		);
		test.setTimeout( 300_000 );

		await expect
			.poll( () => ask( [ 'eval', held ] ), { timeout: 60_000 } )
			.toBe( '1' );
		expect(
			Number(
				ask( [
					'eval',
					'echo ( new Viglet_Dumont_Operations() )->status()["foreign"];',
				] )
			)
		).toBeGreaterThanOrEqual( 1 );

		// The sweep never names it, not even as a candidate.
		expect( wp( [ 'dumont', 'sweep', '--dry-run' ] ) ).not.toContain(
			foreignId
		);
		// Index All ends with the sweep.
		wp( [ 'dumont', 'reindex' ] );
		expect( ask( [ 'eval', held ] ) ).toBe( '1' );

		expect(
			ask( [
				'eval',
				'echo ( new Viglet_Dumont_Indexer( viglet_dumont_get_settings() ) )->delete_all() ? "ok" : "failed";',
			] )
		).toBe( 'ok' );
		// Delete All emptied the site's own posts; the foreign record is still there.
		await expect
			.poll(
				() =>
					ask( [
						'eval',
						'echo count( ( new Viglet_Dumont_Indexer( viglet_dumont_get_settings() ) )->list_site_ids() ?? array() );',
					] ),
				{ timeout: 60_000 }
			)
			.toBe( '1' );
		expect( ask( [ 'eval', held ] ) ).toBe( '1' );
	} );
} );
