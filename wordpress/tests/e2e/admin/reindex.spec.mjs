// Index All runs in the background (D331): the Settings button only starts it, WP-Cron sends
// it a few batches a run, so the page that started it can be left at once, and Health shows the
// run's progress, a Cancel button and the last run that ended.
import { expect, test } from '../fixtures.mjs';
import { setSetting, wp } from '../global-setup.mjs';

// The plugin's part of the screen; WordPress's own menu and bar are not under test.
const plugin = { include: '#wpbody-content' };

/**
 * The last line WP-CLI printed, so a notice before it is no answer.
 *
 * @param {string[]} args The wp arguments.
 */
const ask = ( args ) => wp( args ).trim().split( /\r?\n/ ).pop().trim();

/** The running reindex and the last one that ended, as Health reads them. */
const report = () =>
	JSON.parse(
		ask( [
			'eval',
			'echo wp_json_encode( ( new Viglet_Turing_Reindex() )->report() );',
		] )
	);

/** One WP-Cron pass, as the stack's cron service or a visit runs it. */
const cron = () => {
	try {
		wp( [ 'cron', 'event', 'run', '--due-now', '--quiet' ] );
	} catch {
		// Another pass held the lock or ran the event first: the next one goes on.
	}
};

/**
 * The Reindex card on Health.
 *
 * @param {import('@playwright/test').Page} page The page.
 */
const reindexCard = ( page ) =>
	page.locator( '.components-card' ).filter( {
		has: page.getByRole( 'heading', { name: 'Reindex', exact: true } ),
	} );

/**
 * Start Index All from Settings › Actions, for the given content.
 *
 * @param {import('@playwright/test').Page} page The page.
 * @param {string}                          type `all`, `post` or `page`.
 */
async function startFromSettings( page, type ) {
	await page.goto( '/wp-admin/admin.php?page=viglet-turing#actions' );
	await page.selectOption( '#viglet-turing-index-type', type );
	await page.click( '#viglet-turing-index-all' );
	const message = page.locator(
		'#viglet-turing-index-progress .viglet-turing-progress__message'
	);
	await expect( message ).toContainText( 'you can leave this page' );
	await expect(
		message.getByRole( 'link', { name: 'Search health' } )
	).toHaveAttribute( 'href', /page=viglet-turing-health/ );
	await expect( page.locator( '#viglet-turing-index-all' ) ).toBeDisabled();
}

test.describe( 'Index All in the background', () => {
	test.describe.configure( { mode: 'serial' } );

	/** The batch size the site had, put back afterwards. */
	let batchSize = null;

	test.beforeAll( () => {
		wp( [ 'eval', '( new Viglet_Turing_Reindex() )->cancel();' ] );
		batchSize = JSON.parse(
			ask( [
				'eval',
				"$s = (array) get_option( 'viglet_turing_settings', array() ); echo wp_json_encode( $s['batch_size'] ?? null );",
			] )
		);
		// One post a batch, so the run takes several WP-Cron passes and is seen half-way.
		setSetting( 'batch_size', 1 );
	} );

	test.afterAll( () => {
		wp( [ 'eval', '( new Viglet_Turing_Reindex() )->cancel();' ] );
		setSetting( 'batch_size', batchSize );
		// The site indexed as the specs found it.
		wp( [ 'turing', 'reindex' ] );
	} );

	test( 'goes on after the page that started it is left, and Health follows it to the end', async ( {
		page,
		look,
		axe,
	} ) => {
		test.setTimeout( 300_000 );
		await startFromSettings( page, 'all' );
		// Leave at once: nothing in the tab sends anything.
		await page.goto( '/wp-admin/index.php' );

		const total = report().running.total;
		expect( total ).toBeGreaterThan( 3 );

		cron();
		await page.goto( '/wp-admin/admin.php?page=viglet-turing-health' );
		const card = reindexCard( page );
		await expect( card ).toContainText(
			new RegExp( `\\d+ of ${ total } posts done, 0 failed` )
		);
		await expect(
			card.getByRole( 'button', { name: 'Cancel reindex' } )
		).toBeVisible();
		await look( 'health-reindex-running' );
		await axe( 'Health, a reindex running', plugin );

		// WP-Cron takes it to the end; Health stays open and reads it again on its own.
		await expect
			.poll(
				() => {
					cron();
					return report().running;
				},
				{ timeout: 240_000, intervals: [ 1_000 ] }
			)
			.toBeNull();
		const last = report().last;
		expect( last.state ).toBe( 'done' );
		expect( last.sent ).toBe( total );

		await expect( card ).toContainText( 'Last reindex: Done', {
			timeout: 20_000,
		} );
		await expect( card ).toContainText(
			`${ total } sent, 0 failed, of ${ total } posts.`
		);
		await expect(
			card.getByRole( 'button', { name: 'Cancel reindex' } )
		).toHaveCount( 0 );
		await look( 'health-reindex-done' );
	} );

	test( 'Cancel on Health stops it where it stands', async ( { page } ) => {
		test.setTimeout( 120_000 );
		await startFromSettings( page, 'post' );

		await page.goto( '/wp-admin/admin.php?page=viglet-turing-health' );
		const card = reindexCard( page );
		await card.getByRole( 'button', { name: 'Cancel reindex' } ).click();
		await expect(
			page
				.locator( '.components-notice' )
				.filter( { hasText: 'Reindex cancelled.' } )
		).toBeVisible();
		await expect( card ).toContainText( 'Last reindex: Cancelled' );
		await expect(
			card.getByRole( 'button', { name: 'Cancel reindex' } )
		).toHaveCount( 0 );

		expect( report().running ).toBeNull();
		expect( report().last.state ).toBe( 'cancelled' );
		// Nothing is left for WP-Cron to run.
		expect(
			ask( [
				'eval',
				'var_export( wp_next_scheduled( Viglet_Turing_Reindex::CRON_HOOK ) );',
			] )
		).toBe( 'false' );

		// A Settings page opened now offers the button again.
		await page.goto( '/wp-admin/admin.php?page=viglet-turing#actions' );
		await expect(
			page.locator( '#viglet-turing-index-all' )
		).toBeEnabled();
	} );
} );
