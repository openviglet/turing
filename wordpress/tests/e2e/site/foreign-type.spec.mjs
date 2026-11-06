// A record another source sent under the plugin's site key, typed as a catalog types it
// (`dc:type`, several values), reads by its first value (D327): Fields lists the type, and the
// label and card date set for it there are what the search page's card shows, both as the
// server draws it and once the script has searched itself. The option and the record are
// removed afterwards. Desktop only: it writes to the shared index.
import { expect, test } from '../fixtures.mjs';
import { wp } from '../global-setup.mjs';

const foreignId = 'oai:repo.example:d327-1';
// A word no seeded post holds, so the record is the only result.
const word = 'saltmarshology';

/**
 * The last line WP-CLI printed, so a notice before it is no answer.
 *
 * @param {string[]} args The wp arguments.
 */
const ask = ( args ) => wp( args ).trim().split( /\r?\n/ ).pop().trim();

/**
 * PHP that sends one job item for the foreign record, as another source would: its own id
 * shape and `source_apps`, this site's `site_key`, a list for `type` and a date of its own.
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
			'id'               => '${ foreignId }',
			'title'            => 'Field notes on ${ word }',
			'text'             => 'A harvested record about ${ word }, sent by another connector.',
			'type'             => array( 'Article', 'Thesis' ),
			'publication_date' => '2020-01-10T00:00:00Z',
			'defense_date'     => '2024-03-15T00:00:00Z',
			'site_key'         => $indexer->site_key(),
			'source_apps'      => 'catalog',
		)
		: array( 'id' => '${ foreignId }', 'source_apps' => 'catalog' ),
) ) );
echo $result['success'] ? 'ok' : 'failed';
`;

const block = ( page ) => page.locator( '.viglet-dumont-search' );
const card = ( page ) =>
	block( page )
		.locator( '.viglet-dumont-search__results li' )
		.filter( { hasText: word } );

test.describe( 'a type another source sent', () => {
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
		try {
			wp( [ 'option', 'delete', 'viglet_dumont_foreign_types' ] );
		} catch {}
	} );

	test( 'shows the label and the card date Fields set for its first value', async ( {
		page,
		seed,
	}, testInfo ) => {
		test.skip(
			'site' !== testInfo.project.name,
			'Desktop only: it writes to the shared index.'
		);
		test.setTimeout( 120_000 );

		// Fields lists the type by its first value, with the record's date among its fields.
		await expect
			.poll(
				() =>
					ask( [
						'eval',
						`$found = ( new Viglet_Dumont_Mapping() )->discover_foreign( new Viglet_Dumont_API_Client() );
						$article = array_values( array_filter( is_array( $found ) ? $found : array(), fn ( $t ) => 'Article' === $t['type'] ) );
						echo $article && in_array( 'defense_date', $article[0]['dates'], true ) ? 'listed' : 'missing';`,
					] ),
				{ timeout: 60_000 }
			)
			.toBe( 'listed' );

		// The setting the Fields screen saves: Article's badge and card date.
		expect(
			ask( [
				'eval',
				`$saved = ( new Viglet_Dumont_Mapping() )->save_foreign( array( 'Article' => array( 'label' => 'Journal article', 'card_date' => 'defense_date' ) ) );
				echo $saved['Article']['card_date'] ?? 'none';`,
			] )
		).toBe( 'defense_date' );

		await page.goto( `/?page_id=${ seed.searchPageId }&q=${ word }` );
		await expect( card( page ) ).toHaveCount( 1 );
		await expect(
			card( page ).locator( '.viglet-dumont-search__type' )
		).toHaveText( 'Journal article' );
		await expect( card( page ).locator( 'time' ) ).toHaveAttribute(
			'datetime',
			/^2024-03-15T00:00:00/
		);
		await expect( card( page ).locator( 'time' ) ).toContainText( '2024' );

		// The script's own search, typed into an empty page, draws the same card.
		await page.goto( `/?page_id=${ seed.searchPageId }` );
		const box = block( page ).getByRole( 'combobox' );
		await box.fill( word );
		await box.press( 'Enter' );
		await expect( card( page ) ).toHaveCount( 1 );
		await expect(
			card( page ).locator( '.viglet-dumont-search__type' )
		).toHaveText( 'Journal article' );
		await expect( card( page ).locator( 'time' ) ).toHaveAttribute(
			'datetime',
			/^2024-03-15T00:00:00/
		);
	} );
} );
