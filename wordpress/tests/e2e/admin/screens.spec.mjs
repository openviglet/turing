// The admin screens built with @wordpress/components: Setup, Fields and Health, each checked by
// axe and compared with its stored screenshot.
import { expect, test } from '../fixtures.mjs';

// The plugin's part of the screen; WordPress's own menu and bar are not under test.
const plugin = { include: '#wpbody-content' };

test( 'Setup tests the connection and every step passes', async ( {
	page,
	look,
	axe,
} ) => {
	await page.goto( '/wp-admin/admin.php?page=viglet-dumont-setup' );
	await expect(
		page.getByRole( 'heading', { name: '1. Connection' } )
	).toBeVisible();
	await look( 'setup' );
	await axe( 'Setup', plugin );
	await expect( page.locator( '#wpbody-content' ) ).toHaveScreenshot(
		'setup.png'
	);

	await page
		.getByRole( 'button', { name: 'Save and test the connection' } )
		.click();
	const steps = page.locator( '#viglet-dumont-setup li' );
	await expect( steps ).toHaveCount( 5 );
	await expect( steps.filter( { hasText: '✗' } ) ).toHaveCount( 0 );
	await expect(
		page.getByRole( 'heading', { name: '3. First index' } )
	).toBeVisible();
	await look( 'setup-tested' );
	await axe( 'Setup, tested', plugin );
} );

test( 'Fields shows the seats facet in Turing and previews a document', async ( {
	page,
	look,
	axe,
} ) => {
	await page.goto( '/wp-admin/admin.php?page=viglet-dumont-fields' );
	const post = page.locator( '.components-card' ).filter( {
		has: page.getByRole( 'heading', { name: 'post', exact: true } ),
	} );
	await expect( post.locator( 'input[value="seats"]' ) ).toBeVisible();
	await expect( post.locator( 'tbody tr' ).first() ).toContainText(
		'In Turing'
	);

	// A number field with ranges lists them, each editable, under its row.
	const ranges = post.locator( 'fieldset' ).filter( { hasText: 'Ranges' } );
	await expect( ranges.getByLabel( 'To' ).first() ).toHaveValue( '30' );
	await expect( ranges.getByLabel( 'From' ).nth( 1 ) ).toHaveValue( '30' );

	await post.getByRole( 'button', { name: 'Preview document' } ).click();
	const json = post.locator( 'pre' );
	await expect( json ).toContainText( '"site_key"' );
	await expect( json ).toContainText( '"seats"' );
	await look( 'fields' );
	await axe( 'Fields', plugin );
	await expect( page.locator( '#wpbody-content' ) ).toHaveScreenshot(
		'fields.png',
		{ mask: [ json ] }
	);
} );

test( 'Health checks the AI features on demand, and every one that is off says why', async ( {
	page,
	look,
	axe,
} ) => {
	await page.goto( '/wp-admin/admin.php?page=viglet-dumont-health' );
	const card = page
		.locator( '.components-card' )
		.filter( { hasText: 'AI features' } );
	await card.getByRole( 'button', { name: 'Check AI features' } ).click();
	const rows = card.locator( 'tbody tr' );
	await expect( rows ).toHaveCount( 4 );
	await expect( rows.locator( 'th' ) ).toHaveText( [
		'Answers with sources',
		'Chat',
		'Natural-language filters',
		'Catalog copilot',
	] );
	for ( const row of await rows.all() ) {
		const [ state, why ] = await row.locator( 'td' ).allTextContents();
		expect( [ 'On', 'Off' ] ).toContain( state );
		if ( 'Off' === state ) {
			expect( why, 'an off feature gives its reason' ).not.toBe( '—' );
		}
	}
	await look( 'health-ai' );
	await axe( 'Health, AI features', plugin );
} );

test( 'Health shows published and indexed counts that agree, and the events', async ( {
	page,
	look,
	axe,
} ) => {
	await page.goto( '/wp-admin/admin.php?page=viglet-dumont-health' );
	// The notice, not WordPress's screen-reader echo of it.
	await expect(
		page
			.locator( '.components-notice' )
			.filter( { hasText: 'Connection successful' } )
	).toBeVisible();

	// By heading: an event can say "documents" too (a sweep's "3 documents ... removed").
	const card = ( title ) =>
		page.locator( '.components-card' ).filter( {
			has: page.getByRole( 'heading', { name: title, exact: true } ),
		} );
	const rows = card( 'Documents' ).locator( 'tbody tr' );
	await expect( rows.first() ).toBeVisible();
	for ( const row of await rows.all() ) {
		const [ published, indexed ] = await row
			.locator( 'td' )
			.allTextContents();
		expect(
			indexed,
			`indexed count for ${ await row.locator( 'th' ).textContent() }`
		).toBe( published );
	}

	await expect(
		page.getByRole( 'heading', { name: 'Last events' } )
	).toBeVisible();
	await look( 'health' );
	await axe( 'Health', plugin );
	// The counts and the queue change with every run, and the events table grows; the cards'
	// layout is what is compared.
	await expect( card( 'Connection' ) ).toHaveScreenshot(
		'health-connection.png'
	);
	await expect( card( 'Documents' ) ).toHaveScreenshot(
		'health-documents.png',
		{ mask: [ card( 'Documents' ).locator( 'tbody td' ) ] }
	);
} );
