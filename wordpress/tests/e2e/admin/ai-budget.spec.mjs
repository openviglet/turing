// D332: while Turing says the site's AI budget is spent, Health names it and its reset date.
// The local Turing has no LLM and so no budget to spend: the record Turing's 429 leaves is
// written here directly, and removed afterwards.
import { expect, test } from '../fixtures.mjs';
import { wp } from '../global-setup.mjs';

const option = 'viglet_dumont_ai_quota';

test.beforeAll( () => {
	wp( [
		'eval',
		`update_option( '${ option }', array( 'resetsAt' => '2099-12-01T00:00:00Z', 'detail' => 'The monthly AI budget of this site is spent.', 'seen_at' => time() ), false );`,
	] );
} );

test.afterAll( () => {
	try {
		wp( [ 'option', 'delete', option ] );
	} catch {}
} );

test( 'Health names a spent AI budget, its reset date and Turing’s words', async ( {
	page,
	look,
	axe,
} ) => {
	await page.goto( '/wp-admin/admin.php?page=viglet-dumont-health' );
	const card = page
		.locator( '.components-card' )
		.filter( { hasText: 'AI features' } );
	const notice = card.locator( '.components-notice' );

	await expect( notice ).toContainText( 'AI budget exhausted until' );
	// The reset date as the site writes one, in its own time zone.
	await expect( notice ).toContainText( /2099|December 1|November 30/ );
	await expect( notice ).toContainText(
		'The monthly AI budget of this site is spent.'
	);
	await look( 'health-ai-budget' );
	await axe( 'Health, AI budget', { include: '#wpbody-content' } );
} );
