// Viglet Dumont → Settings: its four tabs, and the Actions tab's buttons and overview.
import { expect, test } from '../fixtures.mjs';

const settingsUrl = '/wp-admin/admin.php?page=viglet-dumont';

test( 'every settings tab opens and shows its panel', async ( {
	page,
	look,
} ) => {
	await page.goto( settingsUrl );
	for ( const tab of [ 'connection', 'indexing', 'search', 'actions' ] ) {
		await page.click( `.viglet-dumont-tab[data-tab="${ tab }"]` );
		await expect( page.locator( `#panel-${ tab }` ) ).toBeVisible();
		await look( `settings-${ tab }` );
	}
} );

test( 'the Content Overview adds up: one card per post type and their total', async ( {
	page,
	look,
} ) => {
	await page.goto( `${ settingsUrl }#actions` );
	const cards = page.locator(
		'#viglet-dumont-stats-grid .viglet-dumont-stat'
	);
	await expect( cards.first() ).toBeVisible();

	const numbers = await cards
		.locator( '.viglet-dumont-stat__number' )
		.allTextContents();
	const labels = await cards
		.locator( '.viglet-dumont-stat__label' )
		.allTextContents();
	await look( 'settings-actions-overview' );

	expect( numbers.join( ' ' ) ).not.toMatch( /NaN|undefined/ );
	expect( labels.join( ' ' ) ).not.toMatch( /undefined/ );
	const [ total, ...types ] = numbers.map( Number );
	expect( types.length ).toBeGreaterThan( 0 );
	expect( total ).toBe( types.reduce( ( sum, n ) => sum + n, 0 ) );
} );

test( 'Test Connection reaches Turing', async ( { page } ) => {
	await page.goto( `${ settingsUrl }#actions` );
	await page.click( '#viglet-dumont-action-ping' );
	await expect(
		page.locator( '#viglet-dumont-action-ping-result' )
	).toContainText( 'Connection successful' );
} );
