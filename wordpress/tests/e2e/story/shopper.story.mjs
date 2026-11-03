// One shopper's story (D203): demo/story.md played chapter by chapter on the demo storefront,
// each chapter a step of one test so the video follows a single visit. Turing is asked for
// real, language model included; nothing is replayed. Leaves tests/e2e/artifacts/story/story.webm.
import { execFileSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { expect, test } from '../fixtures.mjs';
import { wp } from '../global-setup.mjs';
import { demoLocale } from '../demo/curated.mjs';
import { current, lines, takeFile } from '../../../bin/narration-takes.mjs';
import { demoCli } from '../../../playwright.config.mjs';

const turingURL = process.env.TURING_URL || 'http://localhost:2700';
const out = join( 'tests', 'e2e', 'artifacts', 'story' );
const demo = ( args ) => wp( args, demoCli );

// What she searches for (demo/story.md) and the words the pages answer in, in each language
// the demo speaks (DEMO_LOCALE, D217, D244).
const words = {
	en_US: {
		popular: 'rain jacket',
		// Measured on the demo stack: the price is understood, both results are tents within
		// it, and the answer names the two-person one with its price and cites it.
		need: 'a two-person tent under $400',
		understood: 'Price: ≤ 400',
		needFound: /tent/i,
		// No failed search of her own (D289): chapter 8 fixes one from the store's history
		// (demo/seed/searches.php), a word the shop does not use. It must never find anything,
		// or it leaves the "found nothing" list and the story cannot replay: so chapter 8 saves
		// the synonym and does not search with it.
		logged: 'bothy bag',
		// One of the curated prompts (demo/theme/tarnfell/ai-prompts.json), and the guide it cites.
		question: 'What size backpack do I need for a night in a hut?',
		cites: 'What size backpack do you need?',
		compare: 'waterproof jacket',
		// The category she narrows to first, so she compares three jackets (D257).
		compareIn: [ 'Category', 'Waterproof jackets' ],
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
			// On a product's page the box names the product (D242).
			question: /^Ask about /,
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
		// The store sells in reais (D268): R$ 2.200 keeps the same two tents $400 does.
		need: 'uma barraca para duas pessoas até R$ 2.200',
		understood: 'Preço: ≤ 2200',
		needFound: /barraca/i,
		// No failed search of her own: chapter 8 fixes one from the store's history
		// (demo/seed/searches.php), a word the shop does not use for its insulated bottle. It was
		// "bastão de caminhada" until Turing T1196 matched a singular to its plural.
		logged: 'cantil',
		question: 'Que mochila eu levo para uma noite num refúgio?',
		cites: 'De que tamanho deve ser sua mochila?',
		// Measured on the pt_BR demo: three jackets of different kinds to compare. "jaqueta
		// impermeável" offers one, the accent dropping the others (Turing T1194).
		compare: 'jaqueta',
		compareIn: [ 'Categoria', 'Jaquetas impermeáveis' ],
		plan: 'Dois dias atravessando a montanha em julho, primeira vez',
		check: 'Isso me mantém seca num dia de chuva na montanha?',
		backstage: 'puffer',
		synonym: 'cantil, garrafa térmica',
		ui: {
			popular: 'Buscas populares',
			compareBox: /^Comparar /,
			compareBar: 'Comparar 3 produtos',
			planMode: 'Planejar um kit',
			plan: 'Descreva seu plano',
			build: 'Montar meu kit',
			cartTotal: 'Total do carrinho:',
			question: /^Pergunte sobre /,
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
		// WooCommerce's own Portuguese, read off a pt_BR demo, at a Brazilian address (D268).
		checkout: {
			email: 'Endereço de e-mail',
			first: 'Nome',
			last: 'Sobrenome',
			address: 'Endereço',
			city: 'Cidade',
			street: 'Avenida Paulista, 1000',
			town: 'São Paulo',
			state: [ 'Estado', 'SP' ],
			postcode: [ 'CEP', '01310-100' ],
			payment: /^Pagamentos? por cheques?$/i,
			place: /^Finalizar pedido$/i,
		},
	},
};
const story = () => words[ demoLocale() ];

// How long each of Ana's lines runs, in the store's language (D261): the recording paces the
// action to them. A line with no current take runs 0 s, and the action does not wait.
const narrationLanguage = () => ( 'pt_BR' === demoLocale() ? 'pt-BR' : 'en' );
const takeLength = ( name ) => {
	const file = takeFile( narrationLanguage(), name );
	const text = Object.fromEntries( lines() )[ name ]?.[ narrationLanguage() ];
	if ( ! current( file, text ) ) {
		return 0;
	}
	return Number(
		execFileSync( 'ffprobe', [
			'-v',
			'error',
			'-show_entries',
			'format=duration',
			'-of',
			'default=noprint_wrappers=1:nokey=1',
			file,
		] )
			.toString()
			.trim()
	);
};

// The synonym chapter 8 saves, removed again so the story replays on the same stack. Only
// that one: the demo's own synonyms are the plugin's too (demo/seed/catalog.php).
// The term goes to PHP as JSON with its accents escaped: through the Windows command line a raw
// "bastão" arrives mangled, matches nothing, and each run left its synonym behind.
const ascii = ( text ) =>
	JSON.stringify( text ).replace(
		/[^ -~]/g,
		( c ) => `\\u${ c.charCodeAt( 0 ).toString( 16 ).padStart( 4, '0' ) }`
	);
const dropSynonym = () =>
	demo( [
		'eval',
		`$term = json_decode( '${ ascii(
			story().logged
		) }' ); $t = new Viglet_Dumont_Tuning();` +
			' foreach ( $t->all()["synonyms"] as $s ) { if ( in_array( $term, $s["terms"], true ) ) { $t->remove_synonym( $s["id"] ); } }',
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
	// The browser's red spell-check underline reads, in a video, as the shop marking her words
	// wrong (D295). An input does not inherit spellcheck from the document in Edge, so each
	// field is turned off as it takes the focus, before a word is typed into it.
	document.addEventListener(
		'focusin',
		( event ) => {
			if ( 'spellcheck' in event.target ) {
				event.target.spellcheck = false;
			}
		},
		true
	);
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
	let voiced = null;
	// A viewer needs time to read what Turing put on screen: the recording holds on every
	// result, and longest on the moment a chapter is about.
	const linger = async ( seconds = 3.5 ) => {
		if ( record ) {
			await page.waitForTimeout( seconds * 1000 );
		}
	};
	const highlight = async () => {
		await linger( 5 );
		highlighted = now() - 2.5;
	};
	// What the video shows is checked, recorded or not: an AI answer is text a shopper can
	// read, never empty and never the model's apology for finding nothing (Turing T1189 put
	// sources and no answer on screen, and the recording kept it).
	const answered = async ( locator ) => {
		await expect( locator ).toHaveText( /\S{3,}(\s+\S+){5,}/, {
			timeout: 90_000,
		} );
		await expect( locator ).not.toHaveText(
			/not available|I'm sorry|could not answer|não (está|há) dispon|desculp/i
		);
	};
	const chapter = ( title, body ) =>
		test.step( title, async () => {
			const start = now();
			highlighted = null;
			voiced = null;
			await body();
			if ( null === highlighted ) {
				await highlight();
			}
			chapters.push( {
				title,
				start,
				highlight: highlighted,
				...( voiced ? { voice: voiced } : {} ),
				end: now(),
			} );
		} );
	// Ana talks over the action, not before it (D261): a chapter's line starts where `speak()`
	// is called, and the action it describes is paced to end as she does, so Enter is pressed as
	// her last word lands. Her take's length is read from the narration the video will carry; a
	// run that is not recorded waits for nothing.
	const speak = ( name ) => {
		const length = record ? takeLength( name ) : 0;
		voiced = { name, at: now() };
		return now() + ( length ? length + 0.4 : 0 );
	};
	const until = async ( deadline ) => {
		const left = ( deadline ?? 0 ) - now();
		if ( record && left > 0 ) {
			await page.waitForTimeout( left * 1000 );
		}
	};
	const type = async ( field, text, deadline = null ) => {
		if ( ! record ) {
			return field.fill( text );
		}
		await field.fill( '' );
		// Typed over what is left of her line, a little ahead of its end; never slower than
		// a person reads, never faster than one types.
		const left = deadline ? ( deadline - now() - 0.8 ) * 1000 : 0;
		const delay = deadline
			? Math.min( 220, Math.max( 40, left / Math.max( 1, text.length ) ) )
			: 55;
		await field.pressSequentially( text, { delay } );
		return until( deadline );
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
			await until( speak( 'chapter-1' ) );
			const name = ( await chip.textContent() ).trim();
			// A chip searches for itself, and the chips step aside for the results.
			await chip.click();
			await expect( block.locator( 'input[type="search"]' ) ).toHaveValue(
				name
			);
			await expect( titles.first() ).toBeVisible();
			await linger();
		}
	);

	await chapter( '2. Says what she needs, in a sentence', async () => {
		await page.goto( '/?pagename=search-gear' );
		const box = block.locator( 'input[type="search"]' ).first();
		await type( box, say.need, speak( 'chapter-2' ) );
		await box.press( 'Enter' );
		await expect(
			block.locator( '.viglet-dumont-search__understood' )
		).toContainText( say.understood );
		await expect( titles.first() ).toHaveText( say.needFound );
		await answered( block.locator( '.viglet-dumont-search__answer-text' ) );
		// The chapter ends on its result: every search the story makes is one Ana talks about
		// (D289). A typo searched as corrected is site/search.spec.mjs's to show.
		await highlight();
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
			await type(
				dialog.getByRole( 'textbox' ),
				say.question,
				speak( 'chapter-3' )
			);
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
			await answered(
				answer.locator( '.viglet-dumont-assistant__text' )
			);
			await highlight();
			await page.keyboard.press( 'Escape' );
		}
	);

	await chapter( '4. Compares three products side by side', async () => {
		await search( page, say.compare );
		// Narrowed to the category first: the filters are part of the chapter, and the three
		// compared are jackets, never a jacket beside gloves and trousers (D257).
		const [ facetLabel, category ] = say.compareIn;
		await block
			.locator( '.viglet-dumont-search__facet' )
			.filter( {
				has: page.locator( 'summary', { hasText: facetLabel } ),
			} )
			.locator( 'label' )
			.filter( { hasText: category } )
			.locator( 'input' )
			.click();
		await expect(
			block
				.locator( '.viglet-dumont-search__chip' )
				.filter( { hasText: category } )
		).toBeVisible();
		const boxes = block
			.locator( '.viglet-dumont-search__results' )
			.getByRole( 'checkbox', { name: ui.compareBox } );
		await expect( boxes.nth( 2 ) ).toBeVisible();
		const comparing = speak( 'chapter-4' );
		await linger( 1.5 );
		for ( let i = 0; i < 3; i++ ) {
			await boxes.nth( i ).check();
		}
		const bar = block.locator( '.viglet-dumont-search__compare-bar' );
		await expect( bar.getByRole( 'link' ) ).toHaveText( ui.compareBar );
		await until( comparing );
		await bar.getByRole( 'link' ).click();
		await expect(
			page.locator( '.viglet-dumont-compare__table thead th' )
		).toHaveCount( 3 );
		const summary = page.locator( '.viglet-dumont-compare__summary-text' );
		await answered( summary );
		// The table's prices reach the model as text, not HTML entities (D249).
		await expect( summary ).not.toContainText( '&#' );
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
		await type(
			copilot.getByLabel( ui.plan ),
			say.plan,
			speak( 'chapter-5' )
		);
		await copilot
			.getByRole( 'button', { name: ui.build, exact: true } )
			.click();
		await expect(
			kit.locator( '.viglet-dumont-copilot__stage' ).first()
		).toBeVisible( { timeout: 90_000 } );
		await answered( kit.locator( '.viglet-dumont-copilot__kit-summary' ) );
		await expect(
			kit.locator( '.viglet-dumont-copilot__kit-item' )
		).not.toHaveCount( 0 );
		await kit.scrollIntoViewIfNeeded();
		await linger( 6 );
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
			const looking = speak( 'chapter-6' );
			await expect(
				page
					.locator( '.wp-block-viglet-dumont-related' )
					.getByRole( 'link' )
			).not.toHaveCount( 0 );
			await linger( 2.5 );
			await type( copilot.getByLabel( ui.question ), say.check, looking );
			await copilot
				.getByRole( 'button', { name: ui.ask, exact: true } )
				.click();
			const answer = copilot
				.locator( '.viglet-dumont-copilot__answer' )
				.first();
			await answered( answer );
			// The question is about the product on the page (D242): the answer cites it.
			const product = (
				await page.locator( 'h1' ).first().textContent()
			).trim();
			await expect(
				copilot.getByRole( 'link', { name: product, exact: true } )
			).not.toHaveCount( 0 );
		}
	);

	await chapter( '7. Buys, in test mode', async () => {
		const form = say.checkout;
		await page.goto( '/checkout/' );
		const paying = speak( 'chapter-7' );
		// Exact: "Nome" is also inside "Sobrenome".
		const field = ( name ) => page.getByLabel( name, { exact: true } );
		await field( form.email ).fill( 'shopper@example.org' );
		await field( form.first ).fill( 'Ana' );
		await field( form.last ).fill( 'Fell' );
		await field( form.address ).fill( form.street ?? '1 Lake Road' );
		await field( form.city ).fill( form.town ?? 'Portland' );
		await field( form.state[ 0 ] ).selectOption( form.state[ 1 ] );
		await field( form.postcode[ 0 ] ).fill( form.postcode[ 1 ] );
		// The demo's one payment method, offline: an order is placed and nothing is charged.
		await page.getByRole( 'radio', { name: form.payment } ).check();
		await until( paying );
		await page.getByRole( 'button', { name: form.place } ).click();
		await expect( page ).toHaveURL( /order-received/, {
			timeout: 60_000,
		} );
		await linger();
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
			await until( speak( 'chapter-8' ) );
			await linger( 1.5 );

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
			await linger();
			// The search as Turing logged it, which can differ from how she typed it.
			const logged = say.logged;
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
