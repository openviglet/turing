// The Search insights screen (D178): a search that found nothing shows up with "Write about
// this", and a period with no searches says so rather than showing zeros.
import { expect, test } from '../fixtures.mjs';

const turingURL = process.env.TURING_URL || 'http://localhost:2700';

test( 'a search that found nothing is listed, with a draft to write about it', async ( {
	page,
	look,
	axe,
} ) => {
	// A visitor's search, as the block sends it, for a word no post holds.
	const term = `zzinsight${ Date.now() }`;
	const searched = await page.request.get(
		`${ turingURL }/api/sn/Sample/search?q=${ term }&_setlocale=en_US`
	);
	expect( searched.ok() ).toBe( true );

	await page.goto( '/wp-admin/admin.php?page=viglet-dumont-insights' );
	await page.getByLabel( 'Period' ).selectOption( 'today' );
	const failed = page.locator( '.components-card' ).filter( {
		has: page.getByRole( 'heading', {
			name: 'Searches that found nothing',
		} ),
	} );
	const row = failed.locator( 'tr' ).filter( { hasText: term } );
	await expect( row ).toHaveCount( 1 );
	await expect(
		row.getByRole( 'link', { name: `Write about this: “${ term }”` } )
	).toHaveAttribute( 'href', `post-new.php?post_title=${ term }` );
	// The block's searches while typing end in its wildcard and are never listed.
	await expect( page.locator( 'th', { hasText: /\*$/ } ) ).toHaveCount( 0 );
	await look( 'insights' );
	await axe( 'Search insights', { include: '#wpbody-content' } );
} );

test( 'a period with no searches says so', async ( { page } ) => {
	await page.route(
		( url ) =>
			decodeURIComponent( url.href ).includes(
				'viglet-dumont/v1/insights'
			),
		( route ) =>
			route.fulfill( {
				json: {
					period: 'today',
					searches: 0,
					clicks: 0,
					ctr: 0,
					top: [],
					failed: [],
					unopened: [],
				},
			} )
	);
	await page.goto( '/wp-admin/admin.php?page=viglet-dumont-insights' );
	// The notice, not WordPress's screen-reader echo of it.
	await expect(
		page
			.locator( '.components-notice' )
			.filter( { hasText: 'No searches in this period yet.' } )
	).toBeVisible();
	await expect( page.locator( 'table' ) ).toHaveCount( 0 );
} );
