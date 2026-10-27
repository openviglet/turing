// The Search insights screen (D178): a search that found nothing shows up with "Write about
// this", and a period with no searches says so rather than showing zeros.
import { expect, test } from '../fixtures.mjs';

const turingURL = process.env.TURING_URL || 'http://localhost:2700';

test( 'a search that found nothing is listed, with a draft to write about it', async ( {
	page,
	look,
	axe,
} ) => {
	// A visitor's search, as the block sends it, for a word no post holds. One term, searched
	// three times: Turing lists the period's failed searches by count with ties oldest first
	// (Turing T1148), so a new one-off term falls past the cap once a day of runs logged 20.
	const term = 'zzinsightgap';
	for ( let i = 0; i < 3; i++ ) {
		const searched = await page.request.get(
			`${ turingURL }/api/sn/Sample/search?q=${ term }&_setlocale=en_US`
		);
		expect( searched.ok() ).toBe( true );
	}

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

	// "Add a synonym" opens Search tuning with the search already in the field.
	await row
		.getByRole( 'link', { name: `Add a synonym: “${ term }”` } )
		.click();
	await expect(
		page.getByLabel( 'Words that mean the same, separated by commas' )
	).toHaveValue( `${ term }, ` );
} );

test( 'the dashboard shows the week’s searches', async ( { page, axe } ) => {
	await page.goto( '/wp-admin/' );
	const widget = page.locator( '#viglet_dumont_insights' );
	await expect(
		widget.getByRole( 'heading', { name: 'Site search this week' } )
	).toBeVisible();
	await expect(
		widget.getByRole( 'heading', { name: 'Top searches' } )
	).toBeVisible();
	await expect(
		widget.getByRole( 'link', { name: 'All search insights' } )
	).toHaveAttribute( 'href', /page=viglet-dumont-insights/ );
	await axe( 'the Search insights dashboard widget', {
		include: '#viglet_dumont_insights',
	} );
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
