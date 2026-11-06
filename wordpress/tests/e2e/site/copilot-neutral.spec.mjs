// The copilot block on a site with no WooCommerce (D323): it names this site, never a catalog,
// and offers no kit unless switched on; switched on with `stageTypes` "event", its plan asks
// Turing to stage events, cites no posts and reads no stock. Turing's copilot is answered with
// fixed replies so the suite needs no LLM.
// Desktop only: it creates and deletes pages.
import { expect, test } from '../fixtures.mjs';
import { wp } from '../global-setup.mjs';

const pages = {};

/**
 * A published page holding one copilot block, and its id.
 *
 * @param {string} slug       The page's slug.
 * @param {Object} attributes The block's attributes.
 */
const createPage = ( slug, attributes ) =>
	wp( [
		'post',
		'create',
		'--post_type=page',
		'--post_status=publish',
		`--post_name=${ slug }`,
		`--post_title=${ slug }`,
		`--post_content=<!-- wp:viglet-dumont/copilot ${ JSON.stringify(
			attributes
		) } /-->`,
		'--porcelain',
	] )
		.trim()
		.split( /\r?\n/ )
		.pop()
		.trim();

test.beforeAll( ( {}, testInfo ) => {
	if ( 'site' !== testInfo.project.name ) {
		return;
	}
	pages.plain = createPage( 'copilot-neutral', {} );
	pages.plan = createPage( 'copilot-plan', {
		kit: true,
		stageTypes: 'event',
	} );
} );

test.afterAll( ( {}, testInfo ) => {
	if ( 'site' !== testInfo.project.name ) {
		return;
	}
	for ( const id of Object.values( pages ) ) {
		try {
			wp( [ 'post', 'delete', id, '--force' ] );
		} catch {}
	}
} );

test.beforeEach( async ( { page }, testInfo ) => {
	test.skip(
		'site' !== testInfo.project.name,
		'Desktop only: it creates and deletes pages.'
	);
	await page.route( '**/copilot/available', ( route ) =>
		route.fulfill( { json: { available: true } } )
	);
} );

test( 'without a shop the block names this site and offers no kit', async ( {
	page,
} ) => {
	await page.goto( `/?page_id=${ pages.plain }` );
	const block = page.locator( '.viglet-dumont-copilot' );

	await expect(
		block.getByRole( 'heading', { name: 'Ask this site' } )
	).toBeVisible();
	await expect( block ).not.toContainText( /catalog|kit/i, {
		useInnerText: true,
	} );
	await expect( block.getByRole( 'group', { name: 'Mode' } ) ).toHaveCount(
		0
	);
} );

test( 'a plan switched on stages the types the block names', async ( {
	page,
} ) => {
	const asked = [];
	await page.route( '**/copilot/kit', async ( route ) => {
		asked.push( route.request().postDataJSON() );
		await route.fulfill( {
			json: {
				summary: 'Two events cover the first week.',
				stages: [
					{
						gap: 'Orientation',
						reason: 'Meet the faculty.',
						products: [
							{
								id: 'localhost:post:9001',
								title: 'Welcome evening',
								url: '/?p=9001',
								reason: 'Opens the term.',
							},
						],
					},
				],
				unfilled: [],
				guides: [],
			},
		} );
	} );
	await page.goto( `/?page_id=${ pages.plan }` );
	const block = page.locator( '.viglet-dumont-copilot' );

	await block.getByRole( 'button', { name: 'Make a plan' } ).click();
	await block
		.getByRole( 'textbox', { name: 'Describe your plan' } )
		.fill( 'My first week as a new student' );
	await block.getByRole( 'button', { name: 'Build my plan' } ).click();

	await expect(
		block.getByRole( 'link', { name: 'Welcome evening' } )
	).toBeVisible();
	expect( asked ).toHaveLength( 1 );
	expect( asked[ 0 ].productTypes ).toEqual( [ 'event' ] );
	expect( asked[ 0 ] ).not.toHaveProperty( 'guideTypes' );
	expect( asked[ 0 ] ).not.toHaveProperty( 'availabilityField' );
	// What a visitor reads: the cart's button sits hidden in the markup, never shown here.
	await expect( block ).not.toContainText( /catalog|cart|kit/i, {
		useInnerText: true,
	} );
} );

// D332: Turing refuses the copilot with 429 once the site's AI budget is spent. The block says
// answers are paused instead of failing, and tells this WordPress, whose server asks Turing
// itself (the local Turing has a budget left, so nothing is recorded).
test( 'a spent AI budget reads as paused, and the browser reports it', async ( {
	page,
	problems,
} ) => {
	problems.allow( /\/copilot$/ );
	problems.allow( /status of 429/ );
	await page.route( /\/copilot$/, ( route ) =>
		route.fulfill( {
			status: 429,
			contentType: 'application/problem+json',
			json: {
				type: 'https://turing.example.org/problems/quota-exhausted',
				title: 'AI budget exhausted',
				detail: 'The monthly AI budget of this site is spent. Resets on December 1.',
				resetsAt: '2099-12-01T00:00:00Z',
			},
		} )
	);
	await page.goto( `/?page_id=${ pages.plain }` );
	const block = page.locator( '.viglet-dumont-copilot' );
	const reported = page.waitForResponse( /ai-paused/ );

	// On a page of its own the box asks about that page (D242).
	await block.getByRole( 'textbox' ).fill( 'When does the library open?' );
	await block.getByRole( 'button', { name: 'Ask' } ).click();

	// The 429's own reset date, which the SDK carries on the error (Turing T1234, D336).
	await expect(
		block.locator( '.viglet-dumont-copilot__status' )
	).toHaveText( /^Answers are paused until .*2099.*; search still works\.$/ );
	const report = await reported;
	expect( report.request().method() ).toBe( 'POST' );
	expect( report.status() ).toBe( 202 );
	await expect( block ).not.toContainText( 'could not answer' );
} );

test.afterAll( ( {}, testInfo ) => {
	if ( 'site' !== testInfo.project.name ) {
		return;
	}
	// The report's ten-minute throttle, so the site is left as it was found.
	try {
		wp( [ 'transient', 'delete', 'viglet_dumont_ai_paused_report' ] );
	} catch {}
} );
