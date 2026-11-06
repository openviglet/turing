// Edits stopped by an outage longer than the retries are said and replayed (D329): while a
// queued edit has stopped retrying every admin screen but Health warns, linking to it, and once
// Turing answers again the worker sends it with no click.
import { expect, test } from '../fixtures.mjs';
import { wp } from '../global-setup.mjs';

/**
 * The last line WP-CLI printed, so a notice before it is no answer.
 *
 * @param {string[]} args The wp arguments.
 */
const ask = ( args ) => wp( args ).trim().split( /\r?\n/ ).pop().trim();

test( 'a stopped edit is warned about, then replayed once Turing answers', async ( {
	page,
	seed,
} ) => {
	test.setTimeout( 180_000 );
	// The job as the worker leaves it after its last failed attempt.
	wp( [
		'eval',
		`update_option( Viglet_Turing_Queue::OPTION, array( ${ seed.postId } => array( 'action' => 'index', 'queued_at' => time() - 4 * HOUR_IN_SECONDS, 'attempts' => Viglet_Turing_Queue::MAX_ATTEMPTS, 'next_at' => time() - 60, 'error' => 'HTTP 503', 'failed' => true ) ), false );`,
	] );

	await page.goto( '/wp-admin/index.php' );
	const notice = page.locator( '.notice-warning', {
		hasText: 'has not reached search',
	} );
	await expect( notice ).toBeVisible();
	await expect(
		notice.getByRole( 'link', { name: 'Search health' } )
	).toHaveAttribute( 'href', /page=viglet-turing-health/ );

	// Turing is up: the worker asks, replays the edit and sends it, with no click.
	wp( [ 'turing', 'queue', 'run' ] );
	await expect
		.poll(
			() =>
				ask( [
					'eval',
					'echo count( ( new Viglet_Turing_Queue() )->jobs() );',
				] ),
			{ timeout: 60_000 }
		)
		.toBe( '0' );

	await page.goto( '/wp-admin/index.php' );
	await expect(
		page.locator( '.notice-warning', { hasText: 'has not reached search' } )
	).toHaveCount( 0 );
} );
