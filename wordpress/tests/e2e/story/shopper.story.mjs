// One shopper's story (D203): demo/story.md played chapter by chapter on the demo storefront,
// each chapter a step of one test so the video follows a single visit. Turing is asked for
// real, language model included; nothing is replayed. Leaves tests/e2e/artifacts/story/story.webm.
import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { expect, test } from '../fixtures.mjs';
import { wp } from '../global-setup.mjs';
import { demoLocale } from '../demo/curated.mjs';

const turingURL = process.env.TURING_URL || 'http://localhost:2700';
const out = join( 'tests', 'e2e', 'artifacts', 'story' );
const demo = ( args ) => wp( args, 'cli-demo' );

// What she searches for (demo/story.md) and the words the pages answer in, in each language
// the demo speaks (DEMO_LOCALE, D217, D244).
const words = {
	en_US: {
		popular: 'rain jacket',
		need: 'light rain jacket under $400',
		needFound: /jacket|waterproof/i,
		misspelt: 'helvelyn',
		// A word the shop does not use. It must never find anything, or it leaves the
		// insights' "found nothing" list for the day and the story cannot replay: so chapter 8
		// saves the synonym and does not search with it.
		nothing: 'bothy bag',
		// One of the curated prompts (demo/theme/tarnfell/ai-prompts.json), and the guide it cites.
		question: 'What size backpack do I need for a night in a hut?',
		cites: 'What size backpack do you need?',
		compare: 'waterproof jacket',
		plan: 'Two days crossing the mountains in July, first time',
		check: 'Will this keep me dry on a wet day in the hills?',
		// A word WordPress's own search finds nothing for (curated.json, `native: null`).
		backstage: 'raincoat',
		synonym: 'bothy bag, bivvy bag',
		ui: {
			popular: 'Popular searches',
			compareBox: /^Compare /,
			compareBar: 'Compare 3 products',
			planMode: 'Plan a kit',
			plan: 'Describe your plan',
			build: 'Build my kit',
			cartTotal: 'Cart total:',
			question: 'What are you looking for?',
			ask: 'Ask',
			nativeNothing: 'No results found.',
			period: 'Period',
			top: 'Top searches',
			failed: 'Searches that found nothing',
			addSynonym: ( word ) => `Add a synonym: “${ word }”`,
			synonymField: 'Words that mean the same, separated by commas',
			saveSynonym: 'Add synonym',
			removeSynonym: ( terms ) => `Remove ${ terms }`,
		},
		checkout: {
			email: 'Email address',
			first: 'First name',
			last: 'Last name',
			address: 'Address',
			city: 'City',
			state: [ 'State', 'OR' ],
			postcode: [ 'ZIP Code', '97201' ],
			payment: /^Check payments?$/,
			place: 'Place Order',
		},
	},
	pt_BR: {
		popular: 'jaqueta de chuva',
		need: 'jaqueta de chuva leve até R$ 400',
		needFound: /jaqueta|impermeável/i,
		misspelt: 'helvelyn',
		nothing: 'abrigo de emergência',
		// How Turing's metrics log it: accents folded (Turing T1179).
		logged: 'abrigo de emergencia',
		question: 'Que mochila eu levo para uma noite num refúgio?',
		cites: 'De que tamanho deve ser sua mochila?',
		compare: 'jaqueta impermeável',
		plan: 'Dois dias atravessando a montanha em julho, primeira vez',
		check: 'Isso me mantém seca num dia de chuva na montanha?',
		backstage: 'puffer',
		synonym: 'abrigo de emergência, bivaque',
		ui: {
			popular: 'Buscas populares',
			compareBox: /^Comparar /,
			compareBar: 'Comparar 3 produtos',
			planMode: 'Planejar um kit',
			plan: 'Descreva seu plano',
			build: 'Montar meu kit',
			cartTotal: 'Total do carrinho:',
			question: 'O que você procura?',
			ask: 'Perguntar',
			nativeNothing: 'Nenhum resultado encontrado.',
			period: 'Período',
			top: 'Buscas mais feitas',
			failed: 'Buscas que não encontraram nada',
			addSynonym: ( word ) => `Adicionar um sinônimo: “${ word }”`,
			synonymField:
				'Palavras com o mesmo sentido, separadas por vírgulas',
			saveSynonym: 'Adicionar sinônimo',
			removeSynonym: ( terms ) => `Remover ${ terms }`,
		},
		// WooCommerce's own Portuguese, read off a pt_BR demo; the store still sells in the US.
		checkout: {
			email: 'Endereço de e-mail',
			first: 'Nome',
			last: 'Sobrenome',
			address: 'Endereço',
			city: 'Cidade',
			state: [ 'Estado', 'OR' ],
			postcode: [ 'CEP', '97201' ],
			payment: /^Pagamentos? por cheques?$/i,
			place: /^Finalizar pedido$/i,
		},
	},
};
const story = () => words[ demoLocale() ];

// The synonym chapter 8 saves, removed again so the story replays on the same stack. Only
// that one: the demo's own synonyms are the plugin's too (demo/seed/catalog.php).
const dropSynonym = () =>
	demo( [
		'eval',
		'$t = new Viglet_Dumont_Tuning(); $all = $t->all();' +
			` foreach ( $all["synonyms"] as $s ) { if ( in_array( ${ JSON.stringify(
				story().nothing
			) }, $s["terms"], true ) ) { $t->remove_synonym( $s["id"] ); } }`,
	] );

test.beforeAll( async ( { request } ) => {
	test.skip(
		! words[ demoLocale() ],
		`the story has no words for ${ demoLocale() }`
	);
	dropSynonym();
	// Another visitor before her, three times, so a fresh stack has a chip to show. Which
	// chips show is the log's: any that find something.
	for ( let i = 0; i < 3; i++ ) {
		await request.get(
			`${ turingURL }/api/sn/Demo/search?q=${ encodeURIComponent(
				story().popular
			) }&_setlocale=${ demoLocale() }`
		);
	}
	// The chips as the log reads now, not as an earlier visit cached them.
	demo( [ 'transient', 'delete', 'viglet_dumont_trending' ] );
} );

test.afterAll( () => {
	if ( words[ demoLocale() ] ) {
		dropSynonym();
	}
} );

/**
 * Draws the pointer, which a headless recording leaves out, and rings it on a click. Run in
 * the page before its own scripts.
 */
function pointer() {
	window.addEventListener( 'DOMContentLoaded', () => {
		const dot = document.createElement( 'div' );
		dot.setAttribute( 'aria-hidden', 'true' );
		dot.style.cssText =
			'position:fixed;z-index:2147483647;left:-40px;top:-40px;width:22px;height:22px;' +
			'margin:-11px 0 0 -11px;border-radius:50%;background:rgba(232,93,4,.45);' +
			'border:2px solid #E85D04;pointer-events:none;transition:transform .15s';
		document.body.append( dot );
		document.addEventListener( 'mousemove', ( event ) => {
			dot.style.left = `${ event.clientX }px`;
			dot.style.top = `${ event.clientY }px`;
		} );
		document.addEventListener( 'mousedown', () => {
			dot.style.transform = 'scale(1.8)';
		} );
		document.addEventListener( 'mouseup', () => {
			dot.style.transform = '';
		} );
	} );
}

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
	const say = story();
	const { ui } = say;

	// Recorded for the video (playwright.story-record.config.mjs, D204): typing shows, the
	// pointer is drawn, each chapter lingers on its result, and where each one starts and ends
	// in story.webm goes to chapters.json for bin/story-video.mjs to cut. A chapter whose best
	// moment is not its last calls `highlight()` there: the site's still and the short cut
	// are taken at it, and a recording lingers on it.
	const record = !! test.info().project.metadata?.record;
	const started = Date.now();
	const now = () => ( Date.now() - started ) / 1000;
	const chapters = [];
	let highlighted = null;
	const highlight = async () => {
		if ( record ) {
			await page.waitForTimeout( 1500 );
		}
		highlighted = now() - 0.8;
	};
	const chapter = ( title, body ) =>
		test.step( title, async () => {
			const start = now();
			highlighted = null;
			await body();
			if ( null === highlighted ) {
				await highlight();
			}
			chapters.push( {
				title,
				start,
				highlight: highlighted,
				end: now(),
			} );
		} );
	const type = async ( field, text ) => {
		if ( ! record ) {
			return field.fill( text );
		}
		await field.fill( '' );
		return field.pressSequentially( text, { delay: 55 } );
	};
	if ( record ) {
		await page.addInitScript( pointer );
	}

	await chapter(
		'1. Arrives: popular searches show the catalog',
		async () => {
			await page.goto( '/?pagename=search-gear' );
			const chips = page.getByRole( 'navigation', {
				name: ui.popular,
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
		}
	);

	await chapter( '2. Says what she needs, in a sentence', async () => {
		await search( page, say.need );
		await expect(
			block.locator( '.viglet-dumont-search__understood' )
		).toContainText( 'Price: ≤ 400' );
		await expect( titles.first() ).toHaveText( say.needFound );
		await highlight();

		await search( page, say.misspelt );
		await expect(
			block.locator( '.viglet-dumont-search__correction' )
		).toContainText( 'helvellyn' );

		await search( page, say.nothing );
		await expect(
			block.locator( '.viglet-dumont-search__summary' )
		).not.toBeEmpty();
		await expect( titles ).toHaveCount( 0 );
	} );

	await chapter(
		'3. Asks the Site assistant, and gets an answer with its sources',
		async () => {
			await page
				.locator( '.viglet-dumont-assistant.is-floating' )
				.getByRole( 'button' )
				.first()
				.click();
			const dialog = page.getByRole( 'dialog' );
			await type( dialog.getByRole( 'textbox' ), say.question );
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
			).toContainText( [ say.cites ] );
			await highlight();
			await page.keyboard.press( 'Escape' );
		}
	);

	await chapter( '4. Compares three jackets side by side', async () => {
		await search( page, say.compare );
		const boxes = block
			.locator( '.viglet-dumont-search__results' )
			.getByRole( 'checkbox', { name: ui.compareBox } );
		await expect( boxes.nth( 2 ) ).toBeVisible();
		for ( let i = 0; i < 3; i++ ) {
			await boxes.nth( i ).check();
		}
		const bar = block.locator( '.viglet-dumont-search__compare-bar' );
		await expect( bar.getByRole( 'link' ) ).toHaveText( ui.compareBar );
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

	await chapter( '5. Builds the kit and puts it in the cart', async () => {
		await page.goto( '/showcase/' );
		await copilot
			.locator( '.viglet-dumont-copilot__modes' )
			.getByRole( 'button', { name: ui.planMode } )
			.click();
		await type( copilot.getByLabel( ui.plan ), say.plan );
		await copilot
			.getByRole( 'button', { name: ui.build, exact: true } )
			.click();
		await expect(
			kit.locator( '.viglet-dumont-copilot__stage' ).first()
		).toBeVisible( { timeout: 90_000 } );
		const add = kit.locator( '.viglet-dumont-copilot__kit-cart button' );
		await expect( add ).toBeEnabled();
		await add.click();
		await expect(
			kit.locator( '.viglet-dumont-copilot__kit-status' )
		).toContainText( ui.cartTotal );
		item = await kit
			.locator( '.viglet-dumont-copilot__kit-item:not(.is-out) a' )
			.first()
			.getAttribute( 'href' );
	} );

	await chapter(
		'6. Checks one item: Related, and a question about it',
		async () => {
			await page.goto( item );
			await expect(
				page
					.locator( '.wp-block-viglet-dumont-related' )
					.getByRole( 'link' )
			).not.toHaveCount( 0 );
			await type( copilot.getByLabel( ui.question ), say.check );
			await copilot
				.getByRole( 'button', { name: ui.ask, exact: true } )
				.click();
			await expect(
				copilot.locator( '.viglet-dumont-copilot__answer' ).first()
			).not.toBeEmpty( { timeout: 90_000 } );
		}
	);

	await chapter( '7. Buys, in test mode', async () => {
		const form = say.checkout;
		await page.goto( '/checkout/' );
		// Exact: "Nome" is also inside "Sobrenome".
		const field = ( name ) => page.getByLabel( name, { exact: true } );
		await field( form.email ).fill( 'shopper@example.org' );
		await field( form.first ).fill( 'Ana' );
		await field( form.last ).fill( 'Fell' );
		await field( form.address ).fill( '1 Lake Road' );
		await field( form.city ).fill( 'Portland' );
		await field( form.state[ 0 ] ).selectOption( form.state[ 1 ] );
		await field( form.postcode[ 0 ] ).fill( form.postcode[ 1 ] );
		// The demo's one payment method, offline: an order is placed and nothing is charged.
		await page.getByRole( 'radio', { name: form.payment } ).check();
		await page.getByRole( 'button', { name: form.place } ).click();
		await expect( page ).toHaveURL( /order-received/, {
			timeout: 60_000,
		} );
	} );

	await chapter(
		'8. Backstage: the native search, then her searches in insights',
		async () => {
			await page.goto(
				`/?pagename=compare-searches&q=${ encodeURIComponent(
					say.backstage
				) }`
			);
			await expect(
				page.locator( '.tarnfell-compare__native' )
			).toContainText( ui.nativeNothing );
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
			await page.goto(
				'/wp-admin/admin.php?page=viglet-dumont-insights'
			);
			await page.getByLabel( ui.period ).selectOption( 'today' );
			const card = ( name ) =>
				page.locator( '.components-card' ).filter( {
					has: page.getByRole( 'heading', { name } ),
				} );
			await expect(
				card( ui.top ).locator( 'tbody tr' )
			).not.toHaveCount( 0 );
			// The search as Turing logged it, which can differ from how she typed it.
			const logged = say.logged ?? say.nothing;
			const row = card( ui.failed )
				.locator( 'tr' )
				.filter( { hasText: logged } );
			await row
				.getByRole( 'link', { name: ui.addSynonym( logged ) } )
				.click();
			const field = page.getByLabel( ui.synonymField );
			await expect( field ).toHaveValue( `${ logged }, ` );
			await type( field, say.synonym );
			await page.getByRole( 'button', { name: ui.saveSynonym } ).click();
			await expect(
				page.getByRole( 'button', {
					name: ui.removeSynonym( say.synonym ),
				} )
			).toBeVisible();
		}
	);

	mkdirSync( out, { recursive: true } );
	writeFileSync(
		join( out, 'chapters.json' ),
		JSON.stringify( { record, chapters }, null, 2 )
	);
	const video = page.video();
	await page.close();
	await video?.saveAs( join( out, 'story.webm' ) );
} );
