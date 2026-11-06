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
// The phone-width recording (D280) keeps its own folder, so it never replaces the desktop one.
const output = ( vertical ) =>
	join( 'tests', 'e2e', 'artifacts', vertical ? 'story-vertical' : 'story' );
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
		// The product it is asked of: the Helvellyn mountain shell, waterproof.
		checkSku: 'TF-02',
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
		// A jaqueta de montanha Helvellyn, impermeável.
		checkSku: 'TF-02',
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
		) }' ); $t = new Viglet_Turing_Tuning();` +
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
	demo( [ 'transient', 'delete', 'viglet_turing_trending' ] );
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
	// The dot is moved by a transform on its own layer, never by left/top, so a move is drawn by
	// the compositor every frame without a layout. The ring inside it is what grows on a click.
	let place = null;
	window.addEventListener( 'DOMContentLoaded', () => {
		const dot = document.createElement( 'div' );
		dot.setAttribute( 'aria-hidden', 'true' );
		dot.style.cssText =
			'position:fixed;z-index:2147483647;left:0;top:0;pointer-events:none;' +
			'will-change:transform;transform:translate3d(-40px,-40px,0)';
		const ring = document.createElement( 'div' );
		ring.style.cssText =
			'width:22px;height:22px;margin:-11px 0 0 -11px;border-radius:50%;' +
			'background:rgba(232,93,4,.45);border:2px solid #E85D04;box-sizing:border-box;' +
			'transition:transform .15s';
		dot.append( ring );
		document.body.append( dot );
		place = ( x, y ) => {
			dot.style.transform = `translate3d(${ x }px,${ y }px,0)`;
		};
		document.addEventListener( 'mousemove', ( event ) =>
			place( event.clientX, event.clientY )
		);
		document.addEventListener( 'mousedown', () => {
			ring.style.transform = 'scale(1.8)';
		} );
		document.addEventListener( 'mouseup', () => {
			ring.style.transform = '';
		} );
	} );
	// A hand's move from one point to another, drawn by the page at its own frame rate: a
	// minimum-jerk curve (quick away, settling onto the target) along a slight arc, as a wrist
	// moves. The story calls it, then moves the real mouse once to where the dot already is.
	window.__storyGlide = ( from, to, ms ) =>
		new Promise( ( done ) => {
			if ( ! place ) {
				done();
				return;
			}
			const dx = to.x - from.x;
			const dy = to.y - from.y;
			const bend = 0.12;
			const control = {
				x: from.x + dx / 2 - dy * bend,
				y: from.y + dy / 2 + dx * bend,
			};
			const t0 = performance.now();
			const step = ( now ) => {
				const t = Math.min( 1, ( now - t0 ) / ms );
				const s = t * t * t * ( 10 - 15 * t + 6 * t * t );
				const u = 1 - s;
				place(
					u * u * from.x + 2 * u * s * control.x + s * s * to.x,
					u * u * from.y + 2 * u * s * control.y + s * s * to.y
				);
				if ( t < 1 ) {
					window.requestAnimationFrame( step );
				} else {
					done();
				}
			};
			window.requestAnimationFrame( step );
		} );
}

/**
 * Lays each page out at a phone's width inside a larger, mobile-emulated viewport (D280): the
 * meta viewport says how many CSS pixels wide the page is, and the browser scales that up to
 * fill the screen, so the recording is a phone's layout at the frame's full resolution. Run in
 * the page before its own scripts.
 *
 * @param {number} width The layout width, in CSS pixels.
 */
function layoutAt( width ) {
	const content = `width=${ width }, initial-scale=${
		window.innerWidth / width
	}`;
	const set = () => {
		let meta = document.querySelector( 'meta[name="viewport"]' );
		if ( ! meta && document.head ) {
			meta = document.createElement( 'meta' );
			meta.name = 'viewport';
			document.head.append( meta );
		}
		if ( meta && meta.content !== content ) {
			meta.content = content;
		}
	};
	// As soon as the theme's own tag is parsed, before the first layout, and again if a script
	// writes it back.
	new window.MutationObserver( set ).observe( document, {
		childList: true,
		subtree: true,
		attributes: true,
		attributeFilter: [ 'content' ],
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
	const block = page.locator( '.viglet-turing-search' );
	const titles = block.locator( '.viglet-turing-search__title a' );
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
	// What the frame should follow (D292): the element a step acts on, where it is on screen as
	// the step starts, for the video to push in on it. A recording keeps it; a test does not.
	let focused = [];
	const focus = async ( locator ) => {
		if ( ! record ) {
			return;
		}
		const box = await locator.boundingBox().catch( () => null );
		if ( box ) {
			focused.push( { at: now(), box } );
		}
	};
	// A slow scroll until the element sits above the bottom of the window, eased at both ends,
	// so a long result is read on the way down rather than jumped over by a click's own scroll.
	// About 200 px a second, between 3 and 12 s; or, given a deadline (a line's end), over the
	// time left to it, so she talks while it scrolls. `whole` shows all of the element: its
	// bottom, but never past its top, for one taller than the window.
	const glide = async (
		locator,
		{ whole = false, deadline = null } = {}
	) => {
		const to = await locator.evaluate( ( el, all ) => {
			const box = el.getBoundingClientRect();
			const bottom =
				box.bottom + window.scrollY - window.innerHeight + 160;
			return all
				? Math.min( bottom, box.top + window.scrollY - 24 )
				: bottom;
		}, whole );
		const paced = deadline ? ( deadline - now() - 1 ) * 1000 : 0;
		await page.evaluate(
			( [ target, given ] ) =>
				new Promise( ( done ) => {
					const from = window.scrollY;
					const distance = Math.max( 0, target ) - from;
					const ms = given
						? Math.min( 20_000, Math.max( 3_000, given ) )
						: Math.min(
								12_000,
								Math.max(
									3_000,
									( Math.abs( distance ) / 200 ) * 1000
								)
							);
					const t0 = performance.now();
					const step = ( t ) => {
						const k = Math.min( 1, ( t - t0 ) / ms );
						const eased =
							k < 0.5
								? 2 * k * k
								: 1 - Math.pow( -2 * k + 2, 2 ) / 2;
						window.scrollTo( {
							top: from + distance * eased,
							behavior: 'instant',
						} );
						if ( k < 1 ) {
							window.requestAnimationFrame( step );
						} else {
							done();
						}
					};
					window.requestAnimationFrame( step );
				} ),
			[ to, record ? paced : 0 ]
		);
	};
	// The pointer moved to a control before it is used, so a viewer's eye follows it to what is
	// about to happen; a click alone teleports it there. The page draws the move (`__storyGlide`)
	// at its own frame rate: driven from here, step by step, each step waited on a round trip
	// and the dot stuttered. Its length grows with the distance, as a hand's does.
	let cursor = { x: 960, y: 540 };
	const reach = async ( locator ) => {
		let box = await locator.boundingBox();
		if ( ! record || ! box ) {
			return;
		}
		// Out of the window: the page scrolls to it slowly first, never a jump.
		const { height } = page.viewportSize();
		if ( box.y < 0 || box.y + box.height > height ) {
			await glide( locator, { whole: true } );
			box = await locator.boundingBox();
		}
		const to = { x: box.x + box.width / 2, y: box.y + box.height / 2 };
		const distance = Math.hypot( to.x - cursor.x, to.y - cursor.y );
		const ms = Math.min( 900, Math.max( 350, 250 + distance * 0.45 ) );
		await page.evaluate(
			( [ from, target, length ] ) =>
				window.__storyGlide?.( from, target, length ),
			[ cursor, to, ms ]
		);
		await page.mouse.move( to.x, to.y );
		cursor = to;
		// A beat on the target before the click, as a hand settles.
		await page.waitForTimeout( 150 );
	};
	// A wait a viewer should not sit through (an AI answer being written): recorded as `skip`,
	// for the video to cut. The wait itself still happens, and is checked as any other.
	let skipped = [];
	const skipping = async ( wait ) => {
		const from = now();
		await wait();
		skipped.push( { from, to: now() } );
	};
	// Her line about the result, spoken over the action instead of over the result held still
	// (`result` in chapters.json): returns when it ends, as `speak()` does.
	let told = null;
	const tell = ( name ) => {
		const length = record ? takeLength( name ) : 0;
		told = { name, at: now() };
		return now() + ( length ? length + 0.4 : 0 );
	};
	// Back to the full frame: ends the push-in on the last focused element here instead of at
	// the chapter's end, for a step whose result is the whole page.
	const release = () => {
		if ( record ) {
			focused.push( { at: now() } );
		}
	};
	const chapter = ( title, body ) =>
		test.step( title, async () => {
			const start = now();
			highlighted = null;
			voiced = null;
			focused = [];
			skipped = [];
			told = null;
			await body();
			if ( null === highlighted ) {
				await highlight();
			}
			chapters.push( {
				title,
				start,
				highlight: highlighted,
				...( voiced ? { voice: voiced } : {} ),
				...( focused.length ? { focus: focused } : {} ),
				...( skipped.length ? { skip: skipped } : {} ),
				...( told ? { result: told } : {} ),
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
	const { layoutWidth } = test.info().project.metadata ?? {};
	if ( layoutWidth ) {
		await page.addInitScript( layoutAt, layoutWidth );
	}

	await chapter(
		'1. Arrives: popular searches show the catalog',
		async () => {
			// The home page, the shop's front door: its box offers the popular searches, and a
			// chip opens the search page on its results. Opened on its own, an empty search page
			// was the video's first screen.
			await page.goto( '/' );
			const line = speak( 'chapter-1' );
			const chips = page
				.locator( '.tarnfell-hero' )
				.getByRole( 'navigation', { name: ui.popular } );
			const chip = chips.getByRole( 'link' ).first();
			await expect( chip ).toBeVisible();
			// Her line ends as the pointer arrives on the chip.
			await until( line - 1.4 );
			const name = ( await chip.textContent() ).trim();
			// A chip searches for itself, on the search page the box sends its searches to.
			await reach( chip );
			await chip.click();
			await page.waitForURL( /search-gear/ );
			await expect( block.locator( 'input[type="search"]' ) ).toHaveValue(
				name
			);
			await expect( titles.first() ).toBeVisible();
			await linger();
		}
	);

	await chapter( '2. Says what she needs, in a sentence', async () => {
		// Typed into the home page's box, under its photo and its promise, rather than on an
		// empty search page; Enter takes it to the search page with the results.
		await page.goto( '/' );
		const box = page
			.locator( '.tarnfell-hero input[type="search"]' )
			.first();
		await focus( box );
		await reach( box );
		await box.click();
		await type( box, say.need, speak( 'chapter-2' ) );
		await box.press( 'Enter' );
		// The result is the whole page (the facet it understood, the answer, the list), so the
		// frame goes back out as she presses Enter rather than pushing in on the answer.
		release();
		await expect(
			block.locator( '.viglet-turing-search__understood' )
		).toContainText( say.understood );
		await expect( titles.first() ).toHaveText( say.needFound );
		await answered( block.locator( '.viglet-turing-search__answer-text' ) );
		// The chapter ends on its result: every search the story makes is one Ana talks about
		// (D289). A typo searched as corrected is site/search.spec.mjs's to show.
		await highlight();
	} );

	await chapter(
		'3. Asks the Site assistant, and gets an answer with its sources',
		async () => {
			const open = page
				.locator( '.viglet-turing-assistant.is-floating' )
				.getByRole( 'button' )
				.first();
			await reach( open );
			await open.click();
			const dialog = page.getByRole( 'dialog' );
			await focus( dialog );
			const question = dialog.getByRole( 'textbox' );
			await reach( question );
			await type( question, say.question, speak( 'chapter-3' ) );
			const send = dialog.locator( 'form button[type="submit"]' );
			await reach( send );
			await send.click();
			// The answer grows the dialog past the box measured while it was empty, so a push-in
			// on it cut the dialog's top: the whole dialog is shown at the full frame instead.
			release();
			const answer = dialog
				.locator( '.viglet-turing-assistant__message' )
				.nth( 1 );
			await expect(
				answer
					.locator( '.viglet-turing-assistant__feedback button' )
					.first()
			).toBeVisible( { timeout: 90_000 } );
			await expect(
				answer.locator( '.viglet-turing-assistant__source a' )
			).toContainText( [ say.cites ] );
			await answered(
				answer.locator( '.viglet-turing-assistant__text' )
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
		// At a phone's width (D280) the filters are a drawer behind a button, opened as a
		// shopper on a phone would and closed again once the category is chosen.
		const drawer = block.locator( '.viglet-turing-search__filters-toggle' );
		const phone = await drawer.isVisible();
		if ( phone ) {
			await reach( drawer );
			await drawer.click();
		}
		const facet = block
			.locator( '.viglet-turing-search__facet' )
			.filter( {
				has: page.locator( 'summary', { hasText: facetLabel } ),
			} )
			.locator( 'label' )
			.filter( { hasText: category } )
			.locator( 'input' );
		await reach( facet );
		await facet.click();
		if ( phone ) {
			await page.keyboard.press( 'Escape' );
		}
		await expect(
			block
				.locator( '.viglet-turing-search__chip' )
				.filter( { hasText: category } )
		).toBeVisible();
		const boxes = block
			.locator( '.viglet-turing-search__results' )
			.getByRole( 'checkbox', { name: ui.compareBox } );
		await expect( boxes.nth( 2 ) ).toBeVisible();
		const comparing = speak( 'chapter-4' );
		await linger( 1.5 );
		for ( let i = 0; i < 3; i++ ) {
			await reach( boxes.nth( i ) );
			await boxes.nth( i ).check();
		}
		const bar = block.locator( '.viglet-turing-search__compare-bar' );
		await expect( bar.getByRole( 'link' ) ).toHaveText( ui.compareBar );
		await until( comparing - 1 );
		await reach( bar.getByRole( 'link' ) );
		await bar.getByRole( 'link' ).click();
		await expect(
			page.locator( '.viglet-turing-compare__table thead th' )
		).toHaveCount( 3 );
		const summary = page.locator( '.viglet-turing-compare__summary-text' );
		await answered( summary );
		// No push-in: Ana talks about the table under the summary, which a push-in on the
		// summary left out of the frame.
		// The table's prices reach the model as text, not HTML entities (D249).
		await expect( summary ).not.toContainText( '&#' );
	} );

	const copilot = page.locator( '.viglet-turing-copilot' );
	const kit = copilot.locator( '.viglet-turing-copilot__kit' );

	await chapter( '5. Builds the kit and puts it in the cart', async () => {
		// The kit has a page of its own, which opens on the plan: no search beside it, and the
		// room for the kit it builds.
		await page.goto( '/plan-a-kit/' );
		const plan = copilot.getByLabel( ui.plan );
		await focus( plan );
		await reach( plan );
		await type( plan, say.plan, speak( 'chapter-5' ) );
		const build = copilot.getByRole( 'button', {
			name: ui.build,
			exact: true,
		} );
		await reach( build );
		await build.click();
		// The kit is longer than the window: shown at the full frame, read top to bottom.
		release();
		// The kit takes seconds to be written: the video cuts the wait.
		await skipping( async () => {
			await expect(
				kit.locator( '.viglet-turing-copilot__stage' ).first()
			).toBeVisible( { timeout: 90_000 } );
			await answered(
				kit.locator( '.viglet-turing-copilot__kit-summary' )
			);
			await expect(
				kit.locator( '.viglet-turing-copilot__kit-item' )
			).not.toHaveCount( 0 );
		} );
		await kit.scrollIntoViewIfNeeded();
		const add = kit.locator( '.viglet-turing-copilot__kit-cart button' );
		await expect( add ).toBeEnabled();
		// She talks about the kit as soon as it is there, while the list goes by, down to the
		// cart button the click would otherwise jump to; it is pressed as she finishes.
		const telling = tell( 'chapter-5-result' );
		await linger( 1 );
		await glide( add, { deadline: telling - 1.2 } );
		await reach( add );
		await until( telling );
		await add.click();
		await expect(
			kit.locator( '.viglet-turing-copilot__kit-status' )
		).toContainText( ui.cartTotal );
	} );

	await chapter(
		'6. Checks one item: Related, and a question about it',
		async () => {
			// Always the same product, the one her question and line are about: a waterproof
			// jacket. Taken from the kit, it was whatever the model staged first that run, a
			// backpack asked whether it keeps her dry. Found by its SKU, the same in both stores.
			const [ jacket ] = await (
				await page.request.get(
					`/?rest_route=/wc/store/v1/products&sku=${ say.checkSku }`
				)
			).json();
			expect( jacket?.permalink ).toBeTruthy();
			await page.goto( jacket.permalink );
			const looking = speak( 'chapter-6' );
			await expect(
				page
					.locator( '.wp-block-viglet-turing-related' )
					.getByRole( 'link' )
			).not.toHaveCount( 0 );
			await linger( 2.5 );
			const asking = copilot.getByLabel( ui.question );
			await focus( asking );
			await reach( asking );
			await type( asking, say.check, looking );
			const ask = copilot.getByRole( 'button', {
				name: ui.ask,
				exact: true,
			} );
			await reach( ask );
			await ask.click();
			// The answer grows the panel past the window's bottom, so a push-in on it cut the
			// panel off: back to the full frame, and down until the whole panel shows.
			release();
			const answer = copilot
				.locator( '.viglet-turing-copilot__answer' )
				.first();
			await answered( answer );
			// Down to the panel's end: its cited products and Compare are what the answer
			// leads to, and an answer citing two of them makes the panel taller than the
			// window, so keeping its top in view cut them off. The answer passes on the way.
			await glide( copilot );
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
		// The address is not the story: filled at once, as a browser's autofill would.
		await field( form.email ).fill( 'shopper@example.org' );
		await field( form.first ).fill( 'Ana' );
		await field( form.last ).fill( 'Fell' );
		await field( form.address ).fill( form.street ?? '1 Lake Road' );
		await field( form.city ).fill( form.town ?? 'Portland' );
		await field( form.state[ 0 ] ).selectOption( form.state[ 1 ] );
		await field( form.postcode[ 0 ] ).fill( form.postcode[ 1 ] );
		// The demo's one payment method, offline: an order is placed and nothing is charged.
		const payment = page.getByRole( 'radio', { name: form.payment } );
		await reach( payment );
		await payment.check();
		const place = page.getByRole( 'button', { name: form.place } );
		await reach( place );
		await until( paying );
		await place.click();
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
					.locator( '.viglet-turing-search__title a' )
					.first()
			).toBeVisible();
			await until( speak( 'chapter-8' ) );
			await linger( 1.5 );

			// Logging in is not the story: the video cuts from the comparison to the insights.
			await skipping( async () => {
				await page.goto( '/wp-login.php' );
				await page.fill( '#user_login', 'admin' );
				await page.fill( '#user_pass', 'admin' );
				await Promise.all( [
					page.waitForURL( /wp-admin/ ),
					page.click( '#wp-submit' ),
				] );
				await page.goto(
					'/wp-admin/admin.php?page=viglet-turing-insights'
				);
				await expect( page.getByLabel( ui.period ) ).toBeVisible();
			} );
			// Her secret weapon is told over the walk through the insights to the synonym, at a
			// pace a viewer can follow: the pointer goes to each control before it is used, and
			// the page scrolls down to each card rather than jumping there.
			const telling = tell( 'chapter-8-result' );
			await linger( 2 );
			const period = page.getByLabel( ui.period );
			await reach( period );
			await period.selectOption( 'today' );
			const card = ( name ) =>
				page.locator( '.components-card' ).filter( {
					has: page.getByRole( 'heading', { name } ),
				} );
			await expect(
				card( ui.top ).locator( 'tbody tr' )
			).not.toHaveCount( 0 );
			await linger( 1.5 );
			await glide( card( ui.top ), { whole: true } );
			await linger( 1.5 );
			// The search as Turing logged it, which can differ from how she typed it.
			const logged = say.logged;
			await glide( card( ui.failed ), { whole: true } );
			const row = card( ui.failed )
				.locator( 'tr' )
				.filter( { hasText: logged } );
			const add = row.getByRole( 'link', {
				name: ui.addSynonym( logged ),
			} );
			await reach( add );
			await linger( 0.8 );
			await add.click();
			const field = page.getByLabel( ui.synonymField );
			await expect( field ).toHaveValue( `${ logged }, ` );
			await field.scrollIntoViewIfNeeded();
			await reach( field );
			// Typed over her line, leaving time to reach the button as she ends.
			await type( field, say.synonym, telling - 3 );
			const save = page.getByRole( 'button', { name: ui.saveSynonym } );
			await reach( save );
			await save.click();
			await expect(
				page.getByRole( 'button', {
					name: ui.removeSynonym( say.synonym ),
				} )
			).toBeVisible();
			await until( telling );
		}
	);

	const out = output( !! test.info().project.metadata?.vertical );
	mkdirSync( out, { recursive: true } );
	writeFileSync(
		join( out, 'chapters.json' ),
		// The language the store spoke (D298): the video refuses narration in another one.
		JSON.stringify(
			{ record, language: narrationLanguage(), chapters },
			null,
			2
		)
	);
	const video = page.video();
	await page.close();
	await video?.saveAs( join( out, 'story.webm' ) );
} );
