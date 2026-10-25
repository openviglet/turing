// The admin screens built with @wordpress/components: Setup, Fields and Health.
import { expect, test } from '../fixtures.mjs';

test( 'Setup tests the connection and every step passes', async ( {
	page,
	look,
} ) => {
	await page.goto( '/wp-admin/admin.php?page=viglet-dumont-setup' );
	await expect(
		page.getByRole( 'heading', { name: '1. Connection' } )
	).toBeVisible();
	await look( 'setup' );

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
} );

test( 'Fields shows the seats facet in Turing and previews a document', async ( {
	page,
	look,
} ) => {
	await page.goto( '/wp-admin/admin.php?page=viglet-dumont-fields' );
	const post = page.locator( '.components-card' ).filter( {
		has: page.getByRole( 'heading', { name: 'post', exact: true } ),
	} );
	await expect( post.locator( 'input[value="seats"]' ) ).toBeVisible();
	await expect( post.locator( 'tbody tr' ).first() ).toContainText(
		'In Turing'
	);

	await post.getByRole( 'button', { name: 'Preview document' } ).click();
	const json = post.locator( 'pre' );
	await expect( json ).toContainText( '"site_key"' );
	await expect( json ).toContainText( '"seats"' );
	await look( 'fields' );
} );

test( 'Health shows published and indexed counts that agree, and the events', async ( {
	page,
	look,
} ) => {
	await page.goto( '/wp-admin/admin.php?page=viglet-dumont-health' );
	// The notice, not WordPress's screen-reader echo of it.
	await expect(
		page
			.locator( '.components-notice' )
			.filter( { hasText: 'Connection successful' } )
	).toBeVisible();

	const rows = page
		.locator( '.components-card' )
		.filter( { hasText: 'Documents' } )
		.locator( 'tbody tr' );
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
} );
