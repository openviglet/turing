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

test( 'a summary is asked for on a click, and another on the next', async ( {
	page,
	look,
} ) => {
	// The default suite spends no LLM call: the plugin's answer is replayed (the live one is
	// the `ai` project's, ai/insights.spec.mjs).
	const asked = [];
	await page.route(
		( url ) =>
			decodeURIComponent( url.href ).includes(
				'viglet-dumont/v1/insights/summary'
			),
		( route ) => {
			asked.push( decodeURIComponent( route.request().url() ) );
			return route.fulfill( {
				json: {
					summary: `Visitors looked for workshops (${ asked.length }).`,
					error: '',
				},
			} );
		}
	);
	await page.goto( '/wp-admin/admin.php?page=viglet-dumont-insights' );
	const card = page.locator( '.components-card' ).filter( {
		has: page.getByRole( 'heading', { name: 'Summary' } ),
	} );
	await expect( card ).toContainText( 'Each summary is an LLM call.' );
	expect( asked ).toHaveLength( 0 );

	await card.getByRole( 'button', { name: 'Summarize this period' } ).click();
	await expect(
		card.locator( '.viglet-dumont-insights__summary' )
	).toHaveText( 'Visitors looked for workshops (1).' );
	await look( 'insights-summary' );

	await card.getByRole( 'button', { name: 'Write another' } ).click();
	await expect(
		card.locator( '.viglet-dumont-insights__summary' )
	).toHaveText( 'Visitors looked for workshops (2).' );
	expect( asked[ 0 ] ).not.toContain( 'again' );
	expect( asked[ 1 ] ).toContain( 'again=1' );
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

test( 'metrics turned off in Turing are said, not shown as a quiet period', async ( {
	page,
	look,
} ) => {
	// This stack keeps metrics; the report as the plugin answers when Turing does not.
	await page.route(
		( url ) =>
			decodeURIComponent( url.href ).includes(
				'viglet-dumont/v1/insights'
			),
		( route ) =>
			route.fulfill( {
				json: {
					period: 'this-week',
					searches: 0,
					clicks: 0,
					ctr: 0,
					top: [],
					failed: [],
					unopened: [],
					metricsOff: true,
				},
			} )
	);
	await page.goto( '/wp-admin/admin.php?page=viglet-dumont-insights' );
	const notices = page.locator( '.components-notice' );
	await expect(
		notices.filter( { hasText: 'Turing is not keeping search metrics' } )
	).toBeVisible();
	await expect(
		notices.filter( { hasText: 'No searches in this period yet.' } )
	).toHaveCount( 0 );
	await look( 'insights-metrics-off' );
} );

test( 'each top search reads against the period before: up, down, steady or new', async ( {
	page,
} ) => {
	const term = ( name, searches, previous ) => ( {
		term: name,
		searches,
		clicks: 0,
		ctr: 0,
		lastSeen: '',
		previous,
	} );
	await page.route(
		( url ) =>
			decodeURIComponent( url.href ).includes(
				'viglet-dumont/v1/insights'
			),
		( route ) =>
			route.fulfill( {
				json: {
					period: 'this-week',
					searches: 50,
					clicks: 0,
					ctr: 0,
					top: [
						term( 'boots', 14, 10 ),
						term( 'tents', 6, 10 ),
						term( 'stoves', 10, 10 ),
						term( 'kayaks', 3, 0 ),
					],
					failed: [],
					unopened: [],
				},
			} )
	);
	await page.goto( '/wp-admin/admin.php?page=viglet-dumont-insights' );
	const rows = page
		.locator( '.components-card' )
		.filter( {
			has: page.getByRole( 'heading', { name: 'Top searches' } ),
		} )
		.locator( 'tbody tr' );
	await expect( rows.locator( 'td' ).last() ).toHaveText( 'New' );
	await expect( rows ).toHaveText( [
		/boots.*Up 40%/,
		/tents.*Down 40%/,
		/stoves.*Steady/,
		/kayaks.*New/,
	] );

	// All time has no period before it.
	await page.getByLabel( 'Period' ).selectOption( 'all-time' );
	await expect(
		page.getByRole( 'columnheader', { name: 'Against the period before' } )
	).toHaveCount( 0 );
} );
