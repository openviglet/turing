// One shopper's story (D203): demo/story.md played chapter by chapter on the demo storefront,
// each chapter a step of one test so the video follows a single visit. Turing is asked for
// real, language model included; nothing is replayed. Leaves tests/e2e/artifacts/story/story.webm.
import { mkdirSync } from 'node:fs';
import { join } from 'node:path';
import { expect, test } from '../fixtures.mjs';
import { wp } from '../global-setup.mjs';
import { demoLocale } from '../demo/curated.mjs';

const turingURL = process.env.TURING_URL || 'http://localhost:2700';
const out = join( 'tests', 'e2e', 'artifacts', 'story' );
const demo = ( args ) => wp( args, 'cli-demo' );

// What she searches for, from demo/story.md.
const story = {
	popular: 'rain jacket',
	need: 'light rain jacket under $400',
	misspelt: 'helvelyn',
	nothing: 'packraft',
	// One of the curated prompts (demo/theme/tarnfell/ai-prompts.json), and the guide it cites.
	question: 'What size backpack do I need for a night in a hut?',
	cites: 'What size backpack do you need?',
	compare: 'waterproof jacket',
	plan: 'Two days crossing the mountains in July, first time',
	check: 'Will this keep me dry on a wet day in the hills?',
	backstage: 'raincoat',
	synonym: 'packraft, dry bag',
};

// The synonym chapter 8 saves, removed again so the story replays on the same stack. Only
// that one: the demo's own synonyms are the plugin's too (demo/seed/catalog.php).
const dropSynonym = () =>
	demo( [
		'eval',
		'$t = new Viglet_Dumont_Tuning(); $all = $t->all();' +
			' foreach ( $all["synonyms"] as $s ) { if ( in_array( "packraft", $s["terms"], true ) ) { $t->remove_synonym( $s["id"] ); } }',
	] );

test.beforeAll( async ( { request } ) => {
	test.skip(
		'en_US' !== demoLocale(),
		`the story is played in English; the demo speaks ${ demoLocale() }`
	);
	dropSynonym();
	// Another visitor before her, three times, so a fresh stack has a chip to show. Which
	// chips show is the log's: any that find something.
	for ( let i = 0; i < 3; i++ ) {
		await request.get(
			`${ turingURL }/api/sn/Demo/search?q=${ encodeURIComponent(
				story.popular
			) }&_setlocale=en_US`
		);
	}
	// The chips as the log reads now, not as an earlier visit cached them.
	demo( [ 'transient', 'delete', 'viglet_dumont_trending' ] );
} );

test.afterAll( () => {
	if ( 'en_US' === demoLocale() ) {
		dropSynonym();
	}
} );

/**
 * Search the shop's search page for a query, as a link from anywhere would.
 *
 * @param {import('@playwright/test').Page} page The visit.
 * @param {string}                          q    The query.
 */
const search = ( page, q ) =>
	page.goto( `/?pagename=search-gear&q=${ encodeURIComponent( q ) }` );

test( 'a shopper goes from a vague need to a paid-for cart', async ( {
	page,
} ) => {
	const block = page.locator( '.viglet-dumont-search' );
	const titles = block.locator( '.viglet-dumont-search__title a' );

	await test.step( '1. Arrives: popular searches show the catalog', async () => {
		await page.goto( '/?pagename=search-gear' );
		const chips = page.getByRole( 'navigation', {
			name: 'Popular searches',
		} );
		const chip = chips.getByRole( 'link' ).first();
		await expect( chip ).toBeVisible();
		const name = ( await chip.textContent() ).trim();
		// A chip searches for itself, and the chips step aside for the results.
		await chip.click();
		await expect( block.locator( 'input[type="search"]' ) ).toHaveValue(
			name
		);
		await expect( titles.first() ).toBeVisible();
	} );

	await test.step( '2. Says what she needs, in a sentence', async () => {
		await search( page, story.need );
		await expect(
			block.locator( '.viglet-dumont-search__understood' )
		).toContainText( 'Price: ≤ 400' );
		await expect( titles.first() ).toHaveText( /jacket|waterproof/i );

		await search( page, story.misspelt );
		await expect(
			block.locator( '.viglet-dumont-search__correction' )
		).toContainText( 'helvellyn' );

		await search( page, story.nothing );
		await expect(
			block.locator( '.viglet-dumont-search__summary' )
		).not.toBeEmpty();
		await expect( titles ).toHaveCount( 0 );
	} );

	await test.step( '3. Asks the Site assistant, and gets an answer with its sources', async () => {
		await page
			.locator( '.viglet-dumont-assistant.is-floating' )
			.getByRole( 'button' )
			.first()
			.click();
		const dialog = page.getByRole( 'dialog' );
		await dialog.getByRole( 'textbox' ).fill( story.question );
		await dialog.locator( 'form button[type="submit"]' ).click();
		const answer = dialog
			.locator( '.viglet-dumont-assistant__message' )
			.nth( 1 );
		await expect(
			answer
				.locator( '.viglet-dumont-assistant__feedback button' )
				.first()
		).toBeVisible( { timeout: 90_000 } );
		await expect(
			answer.locator( '.viglet-dumont-assistant__source a' )
		).toContainText( [ story.cites ] );
		await page.keyboard.press( 'Escape' );
	} );

	await test.step( '4. Compares three jackets side by side', async () => {
		await search( page, story.compare );
		const boxes = block
			.locator( '.viglet-dumont-search__results' )
			.getByRole( 'checkbox', { name: /^Compare / } );
		await expect( boxes.nth( 2 ) ).toBeVisible();
		for ( let i = 0; i < 3; i++ ) {
			await boxes.nth( i ).check();
		}
		const bar = block.locator( '.viglet-dumont-search__compare-bar' );
		await expect( bar.getByRole( 'link' ) ).toHaveText(
			'Compare 3 products'
		);
		await bar.getByRole( 'link' ).click();
		await expect(
			page.locator( '.viglet-dumont-compare__table thead th' )
		).toHaveCount( 3 );
		await expect(
			page.locator( '.viglet-dumont-compare__summary-text' )
		).not.toBeEmpty( { timeout: 90_000 } );
	} );

	const copilot = page.locator( '.viglet-dumont-copilot' );
	const kit = copilot.locator( '.viglet-dumont-copilot__kit' );
	let item = '';

	await test.step( '5. Builds the kit and puts it in the cart', async () => {
		await page.goto( '/showcase/' );
		await copilot
			.locator( '.viglet-dumont-copilot__modes' )
			.getByRole( 'button', { name: 'Plan a kit' } )
			.click();
		await copilot.getByLabel( 'Describe your plan' ).fill( story.plan );
		await copilot
			.getByRole( 'button', { name: 'Build my kit', exact: true } )
			.click();
		await expect(
			kit.locator( '.viglet-dumont-copilot__stage' ).first()
		).toBeVisible( { timeout: 90_000 } );
		const add = kit.locator( '.viglet-dumont-copilot__kit-cart button' );
		await expect( add ).toBeEnabled();
		await add.click();
		await expect(
			kit.locator( '.viglet-dumont-copilot__kit-status' )
		).toContainText( 'Cart total:' );
		item = await kit
			.locator( '.viglet-dumont-copilot__kit-item:not(.is-out) a' )
			.first()
			.getAttribute( 'href' );
	} );

	await test.step( '6. Checks one item: Related, and a question about it', async () => {
		await page.goto( item );
		await expect(
			page
				.locator( '.wp-block-viglet-dumont-related' )
				.getByRole( 'link' )
		).not.toHaveCount( 0 );
		await copilot
			.getByLabel( 'What are you looking for?' )
			.fill( story.check );
		await copilot
			.getByRole( 'button', { name: 'Ask', exact: true } )
			.click();
		await expect(
			copilot.locator( '.viglet-dumont-copilot__answer' ).first()
		).not.toBeEmpty( { timeout: 90_000 } );
	} );

	await test.step( '7. Buys, in test mode', async () => {
		await page.goto( '/checkout/' );
		await page.getByLabel( 'Email address' ).fill( 'shopper@example.org' );
		await page.getByLabel( 'First name' ).fill( 'Ana' );
		await page.getByLabel( 'Last name' ).fill( 'Fell' );
		await page
			.getByLabel( 'Address', { exact: true } )
			.fill( '1 Lake Road' );
		await page.getByLabel( 'City' ).fill( 'Portland' );
		await page.getByLabel( 'State' ).selectOption( 'OR' );
		await page.getByLabel( 'ZIP Code' ).fill( '97201' );
		// The demo's one payment method, offline: an order is placed and nothing is charged.
		await page.getByRole( 'radio', { name: /^Check payments?$/ } ).check();
		await page.getByRole( 'button', { name: 'Place Order' } ).click();
		await expect( page ).toHaveURL( /order-received/, {
			timeout: 60_000,
		} );
	} );

	await test.step( '8. Backstage: the native search, then her searches in insights', async () => {
		await page.goto(
			`/?pagename=compare-searches&q=${ encodeURIComponent(
				story.backstage
			) }`
		);
		await expect(
			page.locator( '.tarnfell-compare__native' )
		).toContainText( 'No results found.' );
		await expect(
			page
				.locator( '.tarnfell-compare__turing' )
				.locator( '.viglet-dumont-search__title a' )
				.first()
		).toBeVisible();

		await page.goto( '/wp-login.php' );
		await page.fill( '#user_login', 'admin' );
		await page.fill( '#user_pass', 'admin' );
		await Promise.all( [
			page.waitForURL( /wp-admin/ ),
			page.click( '#wp-submit' ),
		] );
		await page.goto( '/wp-admin/admin.php?page=viglet-dumont-insights' );
		await page.getByLabel( 'Period' ).selectOption( 'today' );
		const card = ( name ) =>
			page.locator( '.components-card' ).filter( {
				has: page.getByRole( 'heading', { name } ),
			} );
		await expect(
			card( 'Top searches' ).locator( 'tbody tr' )
		).not.toHaveCount( 0 );
		const row = card( 'Searches that found nothing' )
			.locator( 'tr' )
			.filter( { hasText: story.nothing } );
		await row
			.getByRole( 'link', {
				name: `Add a synonym: “${ story.nothing }”`,
			} )
			.click();
		const words = page.getByLabel(
			'Words that mean the same, separated by commas'
		);
		await expect( words ).toHaveValue( `${ story.nothing }, ` );
		await words.fill( story.synonym );
		await page.getByRole( 'button', { name: 'Add synonym' } ).click();
		await expect(
			page.getByRole( 'button', {
				name: `Remove ${ story.synonym }`,
			} )
		).toBeVisible();

		// Her failed search now finds the dry bags.
		await search( page, story.nothing );
		await expect( titles.first() ).toHaveText( /dry bag/i );
	} );

	mkdirSync( out, { recursive: true } );
	const video = page.video();
	await page.close();
	await video?.saveAs( join( out, 'story.webm' ) );
} );
