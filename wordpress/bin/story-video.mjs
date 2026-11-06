// Assembles the demo video (D204) from the recorded story: `npm run e2e:story:record` leaves
// story.webm and chapters.json in tests/e2e/artifacts/story/, and this cuts one clip per
// chapter, puts a title card in the demo's brand before each, an intro and an outro around
// them, and the chapter names as captions in English and pt_BR. Every frame between the cards
// is the product running. Needs ffmpeg on the PATH.
//
// Narration (D246): a chapter's take in demo/narration/<language>/chapter-<n>.mp3 is read from
// its card on, one audio track per language; a take longer than the card and the clip holds
// the clip's last frame until it ends. `npm run story:narration` buys the takes; without
// them the video is silent, as before.
//
//   npm run e2e:story:record && npm run story:video
//
// Writes tests/e2e/artifacts/video/: story.mp4 (story.<language>.mp4 with --language), with a caption track per narrated language
// whose cues are what Ana says (D278); beside it story.<language>.srt and .vtt, and
// story.<language>.chapters.txt in YouTube's `0:00 Title` form; and story-30s.mp4, a cut for
// social posts that keeps the last seconds of each chapter, where its result shows, captioned
// with the chapter titles since it has no voice. Rendered files are release assets, never committed;
// the one exception is a WebP still per chapter, copied into dumont-site/public/wordpress/
// for the site's WordPress page.
// Refuses a recording that is missing a chapter or older than the story it was recorded from,
// so a re-record after a UI change is never skipped by accident.
//
// One chapter alone (D279): `--chapter <n>` writes story-chapter-<n>.<language>.mp4 instead, a
// clip under a minute for someone searching for that one thing: a title card, the chapter with
// its two lines, its result held still to the end of the second, and a card pointing at the full
// video and the demo store. Built from the same recording, so a new recording rebuilds the clips.
import { execFileSync, spawn, spawnSync } from 'node:child_process';
import { availableParallelism } from 'node:os';
import {
	copyFileSync,
	existsSync,
	mkdirSync,
	readdirSync,
	readFileSync,
	statSync,
	writeFileSync,
} from 'node:fs';
import { basename, dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { chromium } from '@playwright/test';
import { narration, narrationFor } from './narration-takes.mjs';

// Which video (D281): Ana's story, or with `--story developer` the developer walkthrough, its
// own recording, narration, cards and file names. The walkthrough is English only.
const story = process.argv.includes( '--story' )
	? process.argv[ process.argv.indexOf( '--story' ) + 1 ]
	: 'story';
const developer = 'developer' === story;
const { current, lines, takeFile } = narrationFor( story );
// The base of every file this writes: story.<lang>.mp4, or developer.<lang>.mp4.
const base = developer ? 'developer' : 'story';

const root = join( dirname( fileURLToPath( import.meta.url ) ), '..' );
// A vertical cut (D280) is a chapter clip from the phone-width recording, 9:16, its cards laid
// out for it and its captions burned in above the band a vertical player draws its controls on.
const vertical = process.argv.includes( '--vertical' );
let recordedFolder = vertical ? 'story-vertical' : 'story';
let recordCommand = vertical
	? 'npm run e2e:story:vertical'
	: 'npm run e2e:story:record';
if ( developer ) {
	recordedFolder = 'developer';
	recordCommand = 'npm run e2e:developer:record';
}
const recorded = join( root, 'tests', 'e2e', 'artifacts', recordedFolder );
// Each narrated language, as ffmpeg names it on the audio track.
// The video's language: its cards, and the only narration it carries when one is named.
const videoLanguage = process.argv.includes( '--language' )
	? process.argv[ process.argv.indexOf( '--language' ) + 1 ]
	: 'en';
const voices = Object.fromEntries(
	Object.entries( { en: 'eng', 'pt-BR': 'por' } ).filter(
		( [ code ] ) =>
			! process.argv.includes( '--language' ) || code === videoLanguage
	)
);
// One language's video is named for it (D276): the stores differ, so each language is its own
// recording and its own file, and assembling one never overwrites the other.
const videoSuffix = process.argv.includes( '--language' )
	? `.${ videoLanguage }`
	: '';
// The narration starts this long after its card shows, and ends this long before the next.
const voiceLead = 0.4;
const voiceTail = 0.5;
// The music bed under the voice: low enough that every word stays clear.
const musicVolume = 0.12;
const out = join( root, 'tests', 'e2e', 'artifacts', 'video' );
const size = vertical
	? { width: 1080, height: 1920 }
	: { width: 1920, height: 1080 };
const cardSeconds = 2.5;
const chapterCount = developer ? 6 : 8;
// The short cut: this much of the end of each chapter, between a short intro and outro.
const shortSeconds = 3;
// One chapter's clip (D279), when `--chapter` names one, and the length it stays under.
const clipChapter = process.argv.includes( '--chapter' )
	? Number( process.argv[ process.argv.indexOf( '--chapter' ) + 1 ] )
	: null;
const clipLimit = 58;
const clipClosingSeconds = 5;

// Each chapter's caption in Portuguese, by its number in demo/story.md. The English one is
// the spec's own step title.
const portuguese = {
	1: 'Chega: as buscas populares mostram o catálogo',
	2: 'Diz o que precisa, numa frase',
	3: 'Pergunta ao Assistente do site e recebe a resposta com as fontes',
	4: 'Compara três produtos lado a lado',
	5: 'Monta o kit e o põe no carrinho',
	6: 'Confere um item: Relacionados e uma pergunta sobre ele',
	7: 'Compra, em modo de teste',
	8: 'Bastidores: a busca nativa e as buscas dela nos insights',
};

/**
 * Stop with a reason a person can act on.
 *
 * @param {string} reason What is wrong and what to run.
 */
function refuse( reason ) {
	process.stderr.write( `story-video: ${ reason }\n` );
	process.exit( 1 );
}

const timeline = join( recorded, 'chapters.json' );
const webm = join( recorded, developer ? 'walkthrough.webm' : 'story.webm' );
if ( ! existsSync( timeline ) || ! existsSync( webm ) ) {
	refuse( `no recording yet: run \`${ recordCommand }\` first.` );
}
const {
	record,
	language: recordedLanguage,
	chapters,
} = JSON.parse( readFileSync( timeline, 'utf8' ) );
// The store the recording played in (D298): narration and cards in another language would be
// laid over its pages without a word, as the first English clips were over the pt-BR store.
if ( record && recordedLanguage !== videoLanguage ) {
	const recordIn =
		'pt-BR' === videoLanguage
			? `DEMO_LOCALE=pt_BR ${ recordCommand }`
			: recordCommand;
	refuse(
		recordedLanguage
			? `the recording is in ${ recordedLanguage }, not ${ videoLanguage }: run \`${ recordIn }\` first.`
			: `the recording does not say its language (it predates D298): run \`${ recordIn }\` again.`
	);
}
if ( ! record ) {
	refuse(
		`the last story run was the test, not the recording: run \`${ recordCommand }\`.`
	);
}
if ( vertical && null === clipChapter ) {
	refuse( '--vertical cuts one chapter: add `--chapter <n>`.' );
}
if ( chapters.length !== chapterCount ) {
	refuse(
		`the recording has ${ chapters.length } of ${ chapterCount } chapters: the story failed part way. Re-record it.`
	);
}
if (
	null !== clipChapter &&
	! (
		Number.isInteger( clipChapter ) &&
		clipChapter >= 1 &&
		clipChapter <= chapterCount
	)
) {
	refuse( `--chapter takes a chapter's number, 1 to ${ chapterCount }.` );
}
for ( const source of [
	...( developer
		? [ join( root, 'tests', 'e2e', 'developer', 'install.developer.mjs' ) ]
		: [
				join( root, 'demo', 'story.md' ),
				join( root, 'tests', 'e2e', 'story', 'shopper.story.mjs' ),
			] ),
] ) {
	if ( statSync( source ).mtimeMs > statSync( timeline ).mtimeMs ) {
		refuse(
			`${ source } changed after the recording: run \`${ recordCommand }\` again.`
		);
	}
}

/**
 * Run ffmpeg quietly, failing loudly.
 *
 * @param {string[]} args Its arguments.
 */
// STORY_TIMING=1 prints each encode that takes over two seconds, to see where a build goes.
const timing = !! process.env.STORY_TIMING;
const timed = ( started, args ) => {
	const took = ( Date.now() - started ) / 1000;
	if ( timing && took > 2 ) {
		process.stderr.write(
			`[timing] ${ took.toFixed( 1 ) }s ${ basename( String( args.at( -1 ) ) ) }\n`
		);
	}
};
const ffmpeg = ( args ) => {
	const started = Date.now();
	const done = execFileSync(
		'ffmpeg',
		[ '-y', '-loglevel', 'error', ...args ],
		{
			stdio: [ 'ignore', 'inherit', 'inherit' ],
		}
	);
	timed( started, args );
	return done;
};

/**
 * ffmpeg without waiting on it, for the pieces that do not depend on one another.
 *
 * @param {string[]} args Its arguments.
 * @return {Promise<void>} Settled when it exits; rejected on a failure.
 */
const ffmpegAsync = ( args ) =>
	new Promise( ( done, fail ) => {
		const started = Date.now();
		const run = spawn( 'ffmpeg', [ '-y', '-loglevel', 'error', ...args ], {
			stdio: [ 'ignore', 'inherit', 'inherit' ],
		} );
		run.on( 'error', fail );
		run.on( 'close', ( code ) => {
			timed( started, args );
			if ( 0 === code ) {
				done();
			} else {
				fail(
					new Error( `ffmpeg exited ${ code }: ${ args.at( -1 ) }` )
				);
			}
		} );
	} );

// How many encodes run at once. Most of a build is filters that use one core each (zoompan)
// run one after another, which left the rest of the machine idle; six at a time also stays
// under a consumer NVIDIA card's limit on concurrent encode sessions.
const parallel = Math.max(
	1,
	Math.min( 6, Math.floor( availableParallelism() / 4 ) )
);

/**
 * Run tasks with at most `parallel` of them at once, keeping their results in order.
 *
 * @param {Array<function(): Promise<string>>} tasks The tasks.
 * @return {Promise<Array<string>>} Their results (files), in the tasks' order.
 */
const pool = async ( tasks ) => {
	const results = new Array( tasks.length );
	let next = 0;
	const worker = async () => {
		while ( next < tasks.length ) {
			const n = next++;
			results[ n ] = await tasks[ n ]();
		}
	};
	await Promise.all(
		Array.from( { length: Math.min( parallel, tasks.length ) }, worker )
	);
	return results;
};

// The video encoder: NVIDIA's NVENC where this machine has one, several times faster than
// x264 on a build that encodes every card, clip and dissolve; x264 elsewhere. Every encode
// goes through `videoCodec()`, so the pieces stay one codec and still join without another
// encode. STORY_ENCODER=x264 or =nvenc forces one.
const nvenc = ( () => {
	const forced = process.env.STORY_ENCODER;
	if ( forced ) {
		return 'nvenc' === forced;
	}
	const probe = spawnSync(
		'ffmpeg',
		[
			'-hide_banner',
			'-loglevel',
			'error',
			'-f',
			'lavfi',
			'-i',
			'color=c=black:s=256x256:d=0.1',
			'-c:v',
			'h264_nvenc',
			'-f',
			'null',
			'-',
		],
		{ encoding: 'utf8' }
	);
	return 0 === probe.status;
} )();

/**
 * The encoder arguments, at a quality: x264's CRF, or NVENC's constant quality at the
 * same number, which lands at about the same size and look.
 *
 * @param {number} quality 18 to 20 here; lower is better.
 * @return {string[]} ffmpeg's video codec arguments.
 */
// A keyframe every half second exactly (15 frames at 30 fps), on every piece: a dissolve then
// re-encodes only the half second it blends and copies the rest, cut on a keyframe. Without
// it, a dissolve into a chapter re-encoded the whole chapter, twenty seconds a time.
const keyEvery = 15;
const videoCodec = ( quality ) =>
	nvenc
		? [
				'-c:v',
				'h264_nvenc',
				'-preset',
				'p5',
				'-rc',
				'vbr',
				'-cq',
				String( quality ),
				'-b:v',
				'0',
				'-g',
				String( keyEvery ),
				'-no-scenecut',
				'1',
			]
		: [
				'-c:v',
				'libx264',
				'-preset',
				'veryfast',
				'-crf',
				String( quality ),
				'-g',
				String( keyEvery ),
				'-keyint_min',
				String( keyEvery ),
				'-sc_threshold',
				'0',
			];

/**
 * A file's length in seconds.
 *
 * @param {string} file A media file.
 */
const seconds = ( file ) =>
	Number(
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

// The demo theme's own fonts, inlined so the card page needs no server.
const fonts = join( root, 'demo', 'theme', 'tarnfell', 'assets', 'fonts' );
const font = ( file ) =>
	`data:font/woff2;base64,${ readFileSync( join( fonts, file ) ).toString(
		'base64'
	) }`;
// The cards wear the thumbnail's look (demo/thumbnail), so the video opens as the picture that
// was clicked promised: its dark pine, a warm glow behind the words, the hills along the
// bottom, Barlow Condensed in capitals for titles and labels, Source Sans for what is read.
// Two ranges of hills, far and near, each a strip wider than the frame: on a card's video they
// slide at two speeds behind the words (parallax), and the card's still is their first frame.
const ridge = ( fill, points ) =>
	`data:image/svg+xml,${ encodeURIComponent(
		'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1920 420" preserveAspectRatio="none">' +
			`<polygon ${ fill } points="${ points }"/></svg>`
	) }`;
const hillsFar = ridge(
	'fill="#1B3A2A" fill-opacity=".75"',
	'0,420 0,250 230,170 410,235 650,110 880,220 1090,140 1330,245 1560,130 1760,215 1920,160 1920,420'
);
const hillsNear = ridge(
	'fill="#11261B"',
	'0,420 0,330 290,245 520,320 760,225 990,310 1210,235 1470,330 1710,255 1920,300 1920,420'
);
const hillsBox = {
	width: Math.round( size.width * 1.6 ),
	height: Math.round( size.height * ( vertical ? 0.22 : 0.34 ) ),
};
// How fast each range slides, in pixels a second: the near one faster, which reads as depth.
const drift = { far: 24, near: 64 };
const glow = `radial-gradient(ellipse 50% 55% at ${
	vertical ? '30% 40%' : '24% 46%'
}, rgba(245, 180, 58, .36), transparent 70%)`;
const pine = 'linear-gradient(160deg, #1F3F2E 0%, #173327 55%, #10271C 100%)';
const hillsAt = `${ hillsBox.width }px ${ hillsBox.height }px`;
const style = `
@font-face { font-family: Barlow; font-weight: 600; src: url(${ font(
	'barlow-condensed-latin-600-normal.woff2'
) }); }
@font-face { font-family: Barlow; font-weight: 700; src: url(${ font(
	'barlow-condensed-latin-700-normal.woff2'
) }); }
@font-face { font-family: Source; src: url(${ font(
	'source-sans-3-latin-wght-normal.woff2'
) }); }
html, body { margin: 0; width: ${ size.width }px; height: ${ size.height }px; }
body { display: flex; flex-direction: column; justify-content: center;
	padding: 0 ${ vertical ? 90 : 160 }px ${ vertical ? 120 : 60 }px;
	box-sizing: border-box;
	background:
		url("${ hillsNear }") left bottom / ${ hillsAt } no-repeat,
		url("${ hillsFar }") left bottom / ${ hillsAt } no-repeat,
		${ glow }, ${ pine };
	color: #F6F3EC; font-family: Source, sans-serif; }
/* The layers a card's video is put together from (parallax). */
body.layer-text { background: transparent; }
body.layer-backdrop { background: ${ glow }, ${ pine }; }
.ridge { width: ${ hillsBox.width }px; height: ${ hillsBox.height }px;
	background-size: 100% 100%; }
small { font-family: Barlow, sans-serif; font-weight: 600; font-size: ${
	vertical ? 42 : 46
}px; letter-spacing: .1em; text-transform: uppercase; color: #C9D8B6; }
h1 { font-family: Barlow, sans-serif; font-weight: 700; font-size: ${
	vertical ? 112 : 128
}px; line-height: 1; margin: 24px 0 28px; text-transform: uppercase; letter-spacing: .01em;
	text-shadow: 0 6px 28px rgba(0, 0, 0, .35); }
h1 em { color: #F5B43A; font-style: normal; }
p { font-size: ${ vertical ? 40 : 48 }px; margin: 0; color: #C9D8B6; overflow-wrap: anywhere; }
small.pill { align-self: flex-start; background: #F5B43A; color: #1F3B2D; border-radius: 999px;
	padding: 6px 26px 8px; letter-spacing: .08em; }
.badges { display: flex; gap: 20px; margin-top: 48px; }
.badge { font-family: Barlow, sans-serif; font-weight: 700; font-size: 52px; line-height: 1;
	padding: 16px 30px; border-radius: 14px; background: #F6F3EC; color: #1F3B2D;
	box-shadow: 0 10px 30px rgba(0, 0, 0, .25); }
.badge.hot { background: #C8551B; color: #FFF; }`;

/**
 * A card's markup.
 *
 * @param {string}                                                    kicker         The line above.
 * @param {string}                                                    title          The title.
 * @param {string}                                                    line           The line below.
 * @param {{pill?: boolean, badges?: Array<Array<(string|boolean)>>}} options        How the kicker and the
 *                                                                                   badges under the line read.
 * @param {boolean}                                                   options.pill   The kicker as an amber pill.
 * @param {Array<Array<(string|boolean)>>}                            options.badges [text, hot] pairs, as on the
 *                                                                                   thumbnail.
 */
const card = ( kicker, title, line, { pill = false, badges = [] } = {} ) => {
	const row = badges.length
		? `<div class="badges">${ badges
				.map(
					( [ text, hot ] ) =>
						`<span class="badge${ hot ? ' hot' : '' }">${ text }</span>`
				)
				.join( '' ) }</div>`
		: '';
	return (
		`<!doctype html><meta charset="utf-8"><style>${ style }</style>` +
		`<body><small class="${ pill ? 'pill' : '' }">${ kicker }</small><h1>${ title }</h1>` +
		`<p>${ line }</p>${ row }</body>`
	);
};

// The closing card's icons, drawn in the line's colour.
const icons = {
	store: '<path d="M4 9l2-5h12l2 5M4 9v11h16V9M4 9h16M9 20v-6h6v6"/>',
	docs: '<path d="M4 5a2 2 0 0 1 2-2h12v18H6a2 2 0 0 1-2-2zM4 17a2 2 0 0 1 2-2h12M8 7h6"/>',
	code: '<path d="M8 8l-4 4 4 4M16 8l4 4-4 4M14 5l-4 14"/>',
};

/**
 * The size of an address on the closing card: one alone is the card's point, as large as the
 * line allows; several share the width.
 *
 * @param {number} count How many addresses the card has.
 * @return {number} The font size, in pixels.
 */
const urlSize = ( count ) => {
	if ( 1 === count ) {
		return vertical ? 72 : 88;
	}
	return vertical ? 54 : 46;
};

/**
 * The closing card (D275): where to go next, one tile per address with what it is above it, so
 * three addresses read as three places rather than one paragraph. The first is the main one.
 *
 * @param {string}                                            kicker The line above.
 * @param {string}                                            title  The title.
 * @param {Array<{icon: string, label: string, url: string}>} links  The places, main one first.
 */
const linksCard = ( kicker, title, links ) =>
	`<!doctype html><meta charset="utf-8"><style>${ style }
	h1 { font-size: ${ vertical ? 104 : 112 }px; margin: 20px 0 ${ vertical ? 56 : 64 }px; }
	.links { display: grid; gap: ${ vertical ? 32 : 36 }px;
		grid-template-columns: ${ vertical ? '1fr' : `repeat(${ links.length }, 1fr)` }; }
	.link { background: rgba(246,243,236,.08); border: 2px solid rgba(246,243,236,.18);
		border-radius: 28px; padding: ${ vertical ? '36px 40px' : '40px 40px 44px' }; }
	.link.main { background: #C8551B; border-color: #C8551B; }
	.label { display: flex; align-items: center; gap: 18px; font-size: ${ vertical ? 36 : 34 }px;
		color: #C9D8B6; margin-bottom: 18px; }
	.main .label { color: #FBE3D3; }
	.label svg { width: 48px; height: 48px; flex: none; fill: none; stroke: currentColor;
		stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }
	.url { font-family: Barlow, sans-serif; font-size: ${ urlSize(
		links.length
	) }px; line-height: 1.15; color: #F6F3EC; overflow-wrap: anywhere; }
	${ 1 === links.length ? '.links { justify-items: start; } .link { padding: 44px 56px 52px; }' : '' }</style>` +
	`<body><small class="pill">${ kicker }</small><h1>${ title }</h1><div class="links">${ links
		.map(
			( { icon, label, url }, n ) =>
				`<div class="link${ 0 === n ? ' main' : '' }"><div class="label">` +
				`<svg viewBox="0 0 24 24">${ icons[ icon ] }</svg>${ label }</div>` +
				`<div class="url">${ url.replace( /\//g, '/<wbr>' ) }</div></div>`
		)
		.join( '' ) }</div></body>`;

// "3. Asks the Site assistant, and gets…" → 3 and its name.
const parts = chapters.map( ( { title } ) => {
	const [ , number, name ] = title.match( /^(\d+)\.\s*(.*)$/ );
	return { number, name };
} );

mkdirSync( join( out, 'parts' ), { recursive: true } );
const browser = await chromium.launch( {
	channel: process.env.E2E_CHANNEL || 'msedge',
} );
const page = await browser.newPage( { viewport: size } );
// The cards speak the video's language (`--language pt-BR`), as the store it shows does: a
// Portuguese video of Recanto da Serra with English cards reads as two demos stitched together.
// The developer walkthrough (D281) is English and closes on the code, the docs and the Playground.
const developerWords = {
	what: 'For developers and agencies',
	brand: 'Viglet Turing Search',
	line: 'Install, connect, index: a WordPress shop searching through Viglet Turing.',
	chapter: 'Step',
	store: 'On a WordPress that never had the plugin',
	name: ( number, name ) => name,
	tryIt: 'Code, docs and a demo',
	tryTitle: 'Viglet Turing Search',
	tryLinks: [
		{
			icon: 'code',
			label: 'The code, open source',
			url: 'github.com/openviglet/dumont',
		},
		{
			icon: 'docs',
			label: 'Install and configure',
			url: 'dumont.viglet.org/docs/connectors/wordpress',
		},
		{
			icon: 'store',
			label: 'The demo, nothing to install',
			url: 'dumont.viglet.org/wordpress',
		},
	],
	fullIt: 'See the whole setup',
	fullTitle: 'The full walkthrough',
};
const storyWords =
	'pt-BR' === videoLanguage
		? {
				what: 'Plugin open source para WordPress e WooCommerce',
				brand: 'Viglet Turing Search',
				// The opening card keeps the thumbnail's promise; the name is in the intro,
				// her lines and the closing card.
				openKicker: 'Para lojas WordPress e WooCommerce',
				openTitle: 'A busca que <em>entende</em>',
				badges: [
					[ 'WordPress', false ],
					[ '+ IA', true ],
				],
				line: 'Uma cliente, uma visita: de uma necessidade vaga a um carrinho pago.',
				chapter: 'Capítulo',
				store: 'Recanto da Serra, a loja de demonstração',
				name: ( number, name ) => portuguese[ number ] ?? name,
				tryIt: 'Experimente',
				tryTitle: 'A loja de demonstração no seu navegador',
				// One address: the story is about Viglet Turing, and its page leads on to the
				// demo, the docs and the code.
				tryLinks: [
					{
						icon: 'store',
						label: 'A loja de demonstração, a documentação e o código',
						url: 'turing.viglet.org/wordpress',
					},
				],
				fullIt: 'Veja a visita inteira',
				fullTitle: 'O vídeo completo e a loja de demonstração',
			}
		: {
				what: 'Open source plugin for WordPress and WooCommerce',
				brand: 'Viglet Turing Search',
				openKicker: 'For WordPress and WooCommerce shops',
				openTitle: 'Search that <em>understands</em>',
				badges: [
					[ 'WordPress', false ],
					[ '+ AI', true ],
				],
				line: 'One shopper, one visit: from a vague need to a paid-for cart.',
				chapter: 'Chapter',
				store: 'Tarnfell, the demo store',
				name: ( number, name ) => name,
				tryIt: 'Try it',
				tryTitle: 'The demo store in your browser',
				// One address: the story is about Viglet Turing, and its page leads on to the
				// demo, the docs and the code.
				tryLinks: [
					{
						icon: 'store',
						label: 'The demo store, the docs and the code',
						url: 'turing.viglet.org/wordpress',
					},
				],
				fullIt: 'See the whole visit',
				fullTitle: 'The full video and the demo store',
			};
const words = developer ? developerWords : storyWords;
// The opening says what this is, for a viewer who arrives from a search or watches muted; the
// closing says where to try it (D275).
const cards = [
	card(
		words.openKicker ?? words.what,
		words.openTitle ?? words.brand,
		words.line,
		{ badges: words.badges ?? [] }
	),
	...parts.map( ( { number, name } ) =>
		card(
			`${ words.chapter } ${ number }`,
			words.name( number, name ),
			words.store,
			{ pill: true }
		)
	),
	linksCard( words.tryIt, words.tryTitle, words.tryLinks ),
];
// A clip's own two cards (D279): it opens on what the chapter shows, under the product's name,
// for a viewer who has seen none of the rest, and closes on where the full video and the
// Playground are; the site's WordPress page carries both.
if ( null !== clipChapter ) {
	const { number, name } = parts[ clipChapter - 1 ];
	cards.push(
		card( words.brand, words.name( number, name ), words.what ),
		linksCard( words.fullIt, words.fullTitle, words.tryLinks )
	);
}
// `--cards` renders the cards alone, as previews beside the build's own, and stops: a look at
// a change to their design in seconds, without a build or touching one in progress.
const cardsOnly = process.argv.includes( '--cards' );
const layer = ( name ) =>
	join( out, 'parts', `${ cardsOnly ? 'preview-' : '' }${ name }.png` );
for ( const [ i, html ] of cards.entries() ) {
	await page.setContent( html );
	await page.evaluate( () => document.fonts.ready );
	await page.screenshot( { path: layer( `card-${ i }` ) } );
	// Its words alone, over nothing, for the parallax video to lay over the moving hills.
	await page.evaluate( () => document.body.classList.add( 'layer-text' ) );
	await page.screenshot( {
		path: layer( `card-${ i }-text` ),
		omitBackground: true,
	} );
}
// The layers behind every card's words: the pine and its glow, then each range of hills.
await page.setContent(
	`<!doctype html><meta charset="utf-8"><style>${ style }</style><body class="layer-backdrop"></body>`
);
await page.screenshot( { path: layer( 'card-backdrop' ) } );
for ( const [ name, image ] of [
	[ 'far', hillsFar ],
	[ 'near', hillsNear ],
] ) {
	await page.setContent(
		`<!doctype html><meta charset="utf-8"><style>${ style }
		html, body { background: transparent; width: auto; height: auto; padding: 0; }</style>` +
			`<body><div class="ridge" style="background-image:url('${ image }')"></div></body>`
	);
	await page.locator( '.ridge' ).screenshot( {
		path: layer( `card-hills-${ name }` ),
		omitBackground: true,
	} );
}
if ( cardsOnly ) {
	await browser.close();
	// The opening card as it moves (parallax), four seconds, to watch before a build.
	const moving = join( out, 'parts', 'preview-card-0.mp4' );
	ffmpeg(
		parallaxArgs(
			( name ) => join( out, 'parts', `preview-${ name }.png` ),
			0,
			4,
			moving
		)
	);
	process.stdout.write(
		`${ cards.length } cards in ${ join(
			out,
			'parts'
		) } (preview-card-*.png), the opening one moving in ${ moving }\n`
	);
	process.exit( 0 );
}

// The store's own pages, for what Ana says over the opening and closing cards (D261): she
// talks about her shop, so the shop is on screen. The demo stack must be up; without it the
// cards stay as they are.
// Each language's store has its own port (D283).
const storeUrl =
	process.env.STORE_URL ||
	( 'pt-BR' === videoLanguage
		? 'http://localhost:38083'
		: 'http://localhost:38082' );
const screens = [];
const shot = async ( path, scroll = 0 ) => {
	if ( null !== clipChapter ) {
		return; // A clip opens and closes on its cards alone.
	}
	try {
		await page.goto( new URL( path, storeUrl ).href, {
			waitUntil: 'networkidle',
		} );
		await page.evaluate( ( y ) => window.scrollTo( 0, y ), scroll );
		await page.waitForTimeout( 400 );
		const file = join( out, 'parts', `screen-${ screens.length }.png` );
		await page.screenshot( { path: file } );
		screens.push( file );
	} catch {
		// A page the store does not have, or no store at all: one shot fewer.
	}
};
await shot( '/' );
await shot( '/', 900 );
await shot( '/?post_type=product' );
const product = await page
	.locator(
		'a.woocommerce-LoopProduct-link, .wc-block-components-product-name, a[href*="/product/"], a[href*="/produto/"]'
	)
	.first()
	.getAttribute( 'href', { timeout: 3000 } )
	.catch( () => null );
if ( product ) {
	await shot( product );
}
await shot( '/?pagename=journal' );
await browser.close();

// Each card and clip encoded alike, so they join without another encode.
const encode = [
	'-vf',
	`scale=${ size.width }:${ size.height }:force_original_aspect_ratio=decrease,pad=${ size.width }:${ size.height }:(ow-iw)/2:(oh-ih)/2,fps=30,format=yuv420p`,
	...videoCodec( 20 ),
	'-an',
];
const still = ( i, length = cardSeconds ) => {
	// Named by its length too: the closing card is held twice, at two lengths (D275).
	const file = join(
		out,
		'parts',
		length === cardSeconds
			? `card-${ i }.mp4`
			: `card-${ i }-held-${ length.toFixed( 2 ) }.mp4`
	);
	const part = ( name ) => join( out, 'parts', `${ name }.png` );
	if ( ! existsSync( part( `card-${ i }-text` ) ) ) {
		ffmpeg( [
			'-loop',
			'1',
			'-t',
			length.toFixed( 3 ),
			'-i',
			part( `card-${ i }` ),
			...encode,
			file,
		] );
		return file;
	}
	ffmpeg( parallaxArgs( part, i, length, file ) );
	return file;
};

/**
 * A card as a parallax video: the pine and its glow still, the far hills sliding slowly, the
 * near ones faster, the words still over them. Its first frame is the card's still, so a
 * dissolve lands on it.
 *
 * @param {function(string): string} part   A layer's file, by name.
 * @param {number}                   i      The card's index.
 * @param {number}                   length How long it plays, in seconds.
 * @param {string}                   file   The video to write.
 * @return {string[]} ffmpeg's arguments.
 */
function parallaxArgs( part, i, length, file ) {
	const held = [ '-loop', '1', '-t', length.toFixed( 3 ), '-i' ];
	const slide = ( speed ) =>
		`x='-min(t*${ speed },${ hillsBox.width - size.width })':y=${
			size.height - hillsBox.height
		}`;
	return [
		...held,
		part( 'card-backdrop' ),
		...held,
		part( 'card-hills-far' ),
		...held,
		part( 'card-hills-near' ),
		...held,
		part( `card-${ i }-text` ),
		'-filter_complex',
		`[0:v][1:v]overlay=${ slide( drift.far ) }:shortest=1[a];` +
			`[a][2:v]overlay=${ slide( drift.near ) }:shortest=1[b];` +
			'[b][3:v]overlay=0:0:shortest=1,fps=30,format=yuv420p[v]',
		'-map',
		'[v]',
		...videoCodec( 20 ),
		'-an',
		file,
	];
}

/**
 * A segment cut from a file, encoded like the rest.
 *
 * @param {string}   name  The part's file name.
 * @param {string[]} input ffmpeg's input arguments, seek included.
 */
const cutJob = ( name, input ) => {
	const file = join( out, 'parts', name );
	return { file, args: [ ...input, ...encode, file ] };
};
const cut = ( name, input ) => {
	const { file, args } = cutJob( name, input );
	ffmpeg( args );
	return file;
};

/**
 * One frame of a clip, held still for a while: the screen Ana talks about (D261).
 *
 * @param {string} clip   The clip.
 * @param {number} at     Where the frame is, in seconds into the clip.
 * @param {number} length How long it holds.
 * @param {string} name   The held still's file name, without a suffix.
 */
const freeze = ( clip, at, length, name ) => {
	const frame = join( out, 'parts', `${ name }.png` );
	ffmpeg( [ '-ss', at.toFixed( 3 ), '-i', clip, '-frames:v', '1', frame ] );
	const file = join( out, 'parts', `${ name }.mp4` );
	ffmpeg( [
		'-loop',
		'1',
		'-t',
		length.toFixed( 3 ),
		'-i',
		frame,
		...encode,
		file,
	] );
	return file;
};

/**
 * Join segments without another encode.
 *
 * @param {string[]} segments The parts, in order.
 * @param {string}   name     The joined file's name.
 */
const concat = ( segments, name ) => {
	const list = join( out, 'parts', `${ name }.txt` );
	writeFileSync(
		list,
		segments
			.map( ( file ) => `file '${ file.replace( /\\/g, '/' ) }'` )
			.join( '\n' )
	);
	const file = join( out, 'parts', name );
	ffmpeg( [ '-f', 'concat', '-safe', '0', '-i', list, '-c', 'copy', file ] );
	return file;
};

const stamp = ( t ) => {
	const ms = Math.round( t * 1000 );
	const pad = ( n, w = 2 ) => String( n ).padStart( w, '0' );
	return `${ pad( Math.floor( ms / 3600000 ) ) }:${ pad(
		Math.floor( ms / 60000 ) % 60
	) }:${ pad( Math.floor( ms / 1000 ) % 60 ) },${ pad( ms % 1000, 3 ) }`;
};

/**
 * Write a caption file: one cue per chapter, in a language.
 *
 * @param {Array<{from: number, to: number, chapter: number}>} cues     When each chapter shows.
 * @param {'en'|'pt-BR'}                                       language Which caption.
 * @param {string}                                             file     Where to write it.
 */
const subtitles = ( cues, language, file ) => {
	writeFileSync(
		file,
		cues
			.map( ( cue, i ) => {
				const { number, name } = parts[ cue.chapter ];
				const text =
					'en' === language || developer
						? name
						: portuguese[ number ];
				return `${ i + 1 }\n${ stamp( cue.from ) } --> ${ stamp(
					cue.to
				) }\n${ number }. ${ text }\n`;
			} )
			.join( '\n' )
	);
	return file;
};

// A caption's lines: at most two of about this many characters, so a cue reads at a glance.
// Narrower on a vertical cut, whose frame is a little over half as wide.
const captionWidth = vertical ? 26 : 42;

/**
 * A narrated line as caption cues (D278): its stage directions (`[excited]`) out, its words in
 * cues of two short lines, each cue on screen for its share of the take, by its characters.
 *
 * @param {{at: number, length: number, text: string}} placed A take and when it starts.
 * @return {Array<{from: number, to: number, text: string}>} The cues.
 */
const captionCues = ( { at, length, text } ) => {
	const spokenWords = String( text || '' )
		.replace( /\[[^\]]*\]\s*/g, '' )
		.split( /\s+/ )
		.filter( Boolean );
	const rows = [];
	for ( const word of spokenWords ) {
		const last = rows.length ? rows.at( -1 ) : null;
		if ( null !== last && `${ last } ${ word }`.length <= captionWidth ) {
			rows[ rows.length - 1 ] = `${ last } ${ word }`;
		} else {
			rows.push( word );
		}
	}
	const blocks = [];
	for ( let i = 0; i < rows.length; i += 2 ) {
		blocks.push( rows.slice( i, i + 2 ).join( '\n' ) );
	}
	const total = blocks.reduce( ( sum, block ) => sum + block.length, 0 ) || 1;
	let from = at;
	return blocks.map( ( block ) => {
		const to = from + ( length * block.length ) / total;
		const cue = { from, to, text: block };
		from = to;
		return cue;
	} );
};

/**
 * Write one language's narration as captions, SRT for YouTube and WebVTT for an HTML <track>.
 *
 * @param {Array<{at: number, length: number, text: string}>} placed   The language's takes.
 * @param {string}                                            language Which language.
 * @param {string}                                            name     The video's base name.
 * @return {string} The SRT file.
 */
const narrationCaptions = ( placed, language, name ) => {
	const cues = [ ...placed ]
		.sort( ( a, b ) => a.at - b.at )
		.flatMap( captionCues );
	const srt = join( out, `${ name }.${ language }.srt` );
	writeFileSync(
		srt,
		cues
			.map(
				( cue, i ) =>
					`${ i + 1 }\n${ stamp( cue.from ) } --> ${ stamp( cue.to ) }\n${
						cue.text
					}\n`
			)
			.join( '\n' )
	);
	writeFileSync(
		join( out, `${ name }.${ language }.vtt` ),
		'WEBVTT\n\n' +
			cues
				.map(
					( cue ) =>
						`${ stamp( cue.from ).replace( ',', '.' ) } --> ${ stamp(
							cue.to
						).replace( ',', '.' ) }\n${ cue.text }\n`
				)
				.join( '\n' )
	);
	return srt;
};

/**
 * The chapters as YouTube reads them from a description, `00:00 Title` a line, beside the video
 * in each of its languages: the chapter titles are a list there, not captions (D278).
 *
 * @param {Array<{from: number, chapter: number}>} cues     When each chapter starts.
 * @param {string}                                 language Which language.
 * @param {string}                                 name     The video's base name.
 */
const chapterList = ( cues, language, name ) => {
	const at = ( s ) =>
		`${ Math.floor( s / 60 ) }:${ String( Math.floor( s % 60 ) ).padStart(
			2,
			'0'
		) }`;
	const titled = cues.map( ( cue ) => {
		const { number, name: title } = parts[ cue.chapter ];
		return `${ at( cue.from ) } ${ number }. ${
			'pt-BR' === language && ! developer ? portuguese[ number ] : title
		}`;
	} );
	writeFileSync(
		join( out, `${ name }.${ language }.chapters.txt` ),
		[
			`0:00 ${ 'pt-BR' === language ? 'Abertura' : 'Intro' }`,
			...titled,
		].join( '\n' ) + '\n'
	);
};

/**
 * The finished file: the joined video with its caption tracks. A narrated video's captions are
 * its narration, one track per narrated language; one with no voice keeps the chapter titles,
 * in English and Portuguese.
 *
 * @param {string}                                             joined   The joined video.
 * @param {Array<{from: number, to: number, chapter: number}>} cues     When each chapter shows.
 * @param {string}                                             name     The file's base name.
 * @param {Array<{file: string, language: string}>}            tracks   The narration, one track per language.
 * @param {Array<{file: string, language: string}>}            captions The narration's captions, one per language.
 */
const finish = ( joined, cues, name, tracks = [], captions = [] ) => {
	const subtitleTracks = captions.length
		? captions
		: [
				{
					file: subtitles(
						cues,
						'en',
						join( out, `${ name }.en.srt` )
					),
					language: 'eng',
				},
				{
					file: subtitles(
						cues,
						'pt-BR',
						join( out, `${ name }.pt-BR.srt` )
					),
					language: 'por',
				},
			];
	const file = join( out, `${ name }${ videoSuffix }.mp4` );
	ffmpeg( [
		'-i',
		joined,
		...subtitleTracks.flatMap( ( track ) => [ '-i', track.file ] ),
		...tracks.flatMap( ( track ) => [ '-i', track.file ] ),
		'-map',
		'0:v',
		...tracks.flatMap( ( track, i ) => [
			'-map',
			`${ i + 1 + subtitleTracks.length }:a`,
		] ),
		...subtitleTracks.flatMap( ( track, i ) => [
			'-map',
			String( i + 1 ),
		] ),
		'-c:v',
		'copy',
		'-c:a',
		'aac',
		'-b:a',
		'160k',
		'-c:s',
		'mov_text',
		...tracks.flatMap( ( track, i ) => [
			`-metadata:s:a:${ i }`,
			`language=${ track.language }`,
		] ),
		...subtitleTracks.flatMap( ( track, i ) => [
			`-metadata:s:s:${ i }`,
			`language=${ track.language }`,
		] ),
		file,
	] );
	return file;
};

const texts = Object.fromEntries( lines() );
// The music bed: demo/narration/music.mp3, or another track to try with `--music <file>`.
const music = process.argv.includes( '--music' )
	? process.argv[ process.argv.indexOf( '--music' ) + 1 ]
	: join( narration, 'music.mp3' );
if ( process.argv.includes( '--music' ) && ! existsSync( music ) ) {
	refuse( `--music names no file: ${ music }` );
}

/**
 * A line's takes, by language: only one that says the script's current line (an older take
 * stays out of the video), with its length.
 *
 * @param {string} name The line, as narration-takes.mjs names it.
 */
const takes = ( name ) =>
	Object.keys( voices ).flatMap( ( language ) => {
		const file = takeFile( language, name );
		const text = texts[ name ]?.[ language ];
		return current( file, text )
			? [ { language, file, length: seconds( file ), text } ]
			: [];
	} );

/**
 * How long a line needs the screen: its longest take, with a breath before and after.
 *
 * @param {Array<{length: number}>} lineTakes The line's takes.
 */
const needs = ( lineTakes ) =>
	lineTakes.length
		? Math.max( ...lineTakes.map( ( take ) => take.length ) ) +
			voiceLead +
			voiceTail
		: 0;

// The music bed's level (D291): up between Ana's lines, down to `musicVolume` under each, with
// a short ramp either side so it never jumps.
const duck = { between: 0.3, ramp: 0.4 };

/**
 * How far the music is pulled down at a moment, 0 to 1: a take's span with its ramps.
 *
 * @param {Array<{at: number, length: number}>} placed The takes.
 * @param {number}                              t      The moment, in seconds.
 */
const ducked = ( placed, t ) =>
	Math.max(
		0,
		...placed.map( ( { at, length } ) =>
			Math.min(
				1,
				Math.max(
					0,
					Math.min(
						( t - at + duck.ramp ) / duck.ramp,
						( at + length + duck.ramp - t ) / duck.ramp
					)
				)
			)
		)
	);

/**
 * The music's gain at a moment, as `ducked()` sets it.
 *
 * @param {Array<{at: number, length: number}>} placed The takes.
 * @param {number}                              t      The moment, in seconds.
 */
const musicGain = ( placed, t ) =>
	duck.between - ( duck.between - musicVolume ) * ducked( placed, t );

/**
 * `musicGain()` as an ffmpeg expression in `t`, for the volume filter.
 *
 * @param {Array<{at: number, length: number}>} placed The takes.
 */
const duckExpression = ( placed ) => {
	const dips = placed.map(
		( { at, length } ) =>
			`clip(min((t-${ ( at - duck.ramp ).toFixed( 3 ) })/${
				duck.ramp
			},(${ ( at + length + duck.ramp ).toFixed( 3 ) }-t)/${
				duck.ramp
			}),0,1)`
	);
	// Single-quoted in the filtergraph, so its commas need no escaping.
	const dip = dips.length
		? dips.reduce( ( a, b ) => `max(${ a },${ b })` )
		: '0';
	return `${ duck.between }-${ ( duck.between - musicVolume ).toFixed(
		3
	) }*(${ dip })`;
};

// The mix's loudness target (D291): YouTube's playback level, with a true-peak ceiling. What
// each language's mix measured is printed beside the video's length, so a regression shows.
const loudness = { target: -14, peak: -1, measured: {} };

/**
 * Run ffmpeg to measure, and read the JSON loudnorm prints at the end of its log.
 *
 * @param {string[]} args Its input and filter arguments.
 * @return {Object} loudnorm's report: input_i, input_tp, input_lra, input_thresh, target_offset.
 */
const measure = ( args ) => {
	const log = spawnSync(
		'ffmpeg',
		[ '-hide_banner', ...args, '-f', 'null', '-' ],
		{ encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 }
	).stderr;
	// The report is the last brace block naming input_i; ffmpeg can print more after it.
	const reports = log.match( /\{[^{}]*"input_i"[^{}]*\}/g );
	if ( ! reports ) {
		refuse( `loudnorm printed no report:\n${ log.slice( -2000 ) }` );
	}
	return JSON.parse( reports.at( -1 ) );
};

// The level `musicVolume` and the ducking were set against: the first bed's own loudness. Any
// other track is brought to it first, so a louder master does not sit louder under the voice.
const musicReference = -18;
let musicTrimDb = null;

/**
 * The gain, in dB, that brings the music bed to `musicReference`, measured once.
 */
const musicTrim = () => {
	if ( null === musicTrimDb ) {
		const measured = Number(
			measure( [ '-i', music, '-af', 'loudnorm=print_format=json' ] )
				.input_i
		);
		musicTrimDb = Number.isFinite( measured )
			? Number( ( musicReference - measured ).toFixed( 2 ) )
			: 0;
	}
	return musicTrimDb;
};

// Sound that comes with a piece of picture, mixed as recorded where the piece plays: the
// opening animation's own (demo/intro). The music bed waits for it to end.
const sounds = [];
let musicFrom = 0;

/**
 * One language's narration as a single track the video's length, each take placed where its
 * screen shows, over the music bed when there is one.
 *
 * @param {string}                            language Which language.
 * @param {Array<{at: number, file: string}>} placed   Its takes and when each starts.
 * @param {number}                            total    The video's length in seconds.
 */
const voiceTrack = ( language, placed, total ) => {
	const file = join( out, 'parts', `voice.${ language }.m4a` );
	const inputs = placed.flatMap( ( { file: take } ) => [ '-i', take ] );
	const graph = placed.map(
		( { at }, i ) =>
			`[${ i }:a]adelay=${ Math.round( at * 1000 ) }:all=1[d${ i }]`
	);
	let labels = placed.map( ( _, i ) => `[d${ i }]` ).join( '' );
	let count = placed.length;
	for ( const [ n, sound ] of sounds.entries() ) {
		inputs.push(
			'-ss',
			sound.from.toFixed( 3 ),
			'-t',
			sound.length.toFixed( 3 ),
			'-i',
			sound.file
		);
		graph.push(
			`[${ count }:a]afade=t=out:st=${ Math.max(
				0,
				sound.length - 0.6
			).toFixed( 3 ) }:d=0.6,adelay=${ Math.round(
				sound.at * 1000
			) }:all=1[s${ n }]`
		);
		labels += `[s${ n }]`;
		count++;
	}
	if ( existsSync( music ) ) {
		// Ducked under each line and up between them (D291), faded in and out, looped for as long
		// as the video runs; from where the opening's own sound ends, when it has one.
		const bedLength = total - musicFrom;
		const fadeOut = Math.max( 0, bedLength - 3 ).toFixed( 3 );
		const onBed = placed.map( ( take ) => ( {
			...take,
			at: take.at - musicFrom,
		} ) );
		inputs.push( '-stream_loop', '-1', '-i', music );
		graph.push(
			`[${ count }:a]atrim=0:${ bedLength.toFixed( 3 ) },` +
				`volume=${ musicTrim() }dB,` +
				`volume='${ duckExpression( onBed ) }':eval=frame,afade=t=in:d=2,` +
				`afade=t=out:st=${ fadeOut }:d=3,` +
				`adelay=${ Math.round( musicFrom * 1000 ) }:all=1[m]`
		);
		labels += '[m]';
		count++;
	}
	graph.push(
		`${ labels }amix=inputs=${ count }:normalize=0,` +
			`apad=whole_dur=${ total.toFixed( 3 ) }[mix]`
	);
	// To YouTube's playback loudness (D291): measure the mix, raise it by the difference, and hold
	// its peaks under the ceiling with a limiter. loudnorm alone stopped at -15.1 here, its
	// linear mode refusing a gain that crosses the ceiling and its dynamic one limiting short of
	// the target, so the gain is applied by hand and corrected once by what the limiter took.
	const target = `I=${ loudness.target }:TP=${ loudness.peak }:LRA=11`;
	const report = ( args ) =>
		Number(
			measure( [
				...args,
				'-af',
				`loudnorm=${ target }:print_format=json`,
			] ).input_i
		);
	const mixed = join( out, 'parts', `mix.${ language }.wav` );
	ffmpeg( [
		...inputs,
		'-filter_complex',
		graph.join( ';' ),
		'-map',
		'[mix]',
		'-t',
		total.toFixed( 3 ),
		mixed,
	] );
	// The limiter holds sample peaks, and AAC overshoots them (measured: a limit at -2.5 dBFS
	// encoded to -1.1 dBTP): a margin of 1.5 dB keeps the encoded file under the ceiling.
	const ceiling = 10 ** ( ( loudness.peak - 1.5 ) / 20 );
	const render = ( gain ) =>
		ffmpeg( [
			'-i',
			mixed,
			'-af',
			`volume=${ gain.toFixed( 2 ) }dB,` +
				`alimiter=limit=${ ceiling.toFixed( 3 ) }:level=false:attack=5:release=80,` +
				'aresample=48000',
			'-c:a',
			'aac',
			'-b:a',
			'160k',
			file,
		] );
	let gain = loudness.target - report( [ '-i', mixed ] );
	render( gain );
	let measured = report( [ '-i', file ] );
	// The more the limiter works, the more each step loses: a few corrections reach the target.
	for (
		let step = 0;
		step < 4 && Math.abs( loudness.target - measured ) > 0.3;
		step++
	) {
		gain += loudness.target - measured;
		render( gain );
		measured = report( [ '-i', file ] );
	}
	loudness.measured[ language ] = measured;
	return file;
};

/**
 * Where a chapter's best moment is in its clip: the highlight the spec marked, or just before
 * its end.
 *
 * @param {number} i The chapter's index.
 */
const best = ( i ) => {
	const { start, end, highlight } = chapters[ i ];
	return Math.max( 0, ( highlight ?? end - 0.8 ) - start );
};

// The video: the opening card, then each chapter's card and clip, then the closing card. A
// card holds for as long as Ana's line over it runs, and her line about a chapter's result is
// spoken over that moment held still, so she always talks about what is on screen (D261).
const spoken = Object.fromEntries(
	Object.keys( voices ).map( ( language ) => [ language, [] ] )
);
const cues = [];
let clock = 0;

/**
 * Place a line's takes from a moment of the video on.
 *
 * @param {number}                                  at        When its screen starts.
 * @param {Array<{language: string, file: string}>} lineTakes The line's takes.
 */
const say = ( at, lineTakes ) => {
	for ( const take of lineTakes ) {
		spoken[ take.language ].push( {
			at: at + voiceLead,
			file: take.file,
			length: take.length,
			text: take.text,
		} );
	}
};

/**
 * Where a take falls silent between phrases, in seconds from its start.
 *
 * @param {string} file The take.
 */
const pauses = ( file ) =>
	[
		...spawnSync(
			'ffmpeg',
			[
				'-hide_banner',
				'-i',
				file,
				'-af',
				'silencedetect=n=-35dB:d=0.3',
				'-f',
				'null',
				'-',
			],
			{ encoding: 'utf8' }
		).stderr.matchAll( /silence_end: ([\d.]+)/g ),
	].map( ( match ) => Number( match[ 1 ] ) );

/**
 * A still with slow motion, a zoom in and a drift sideways, encoded like the rest.
 *
 * @param {string} image  The still.
 * @param {number} length How long it plays.
 * @param {number} n      Which shot it is, so the drift changes side from one to the next.
 * @param {string} name   The shot's file name.
 */
const kenBurns = ( image, length, n, name ) => {
	const file = join( out, 'parts', name );
	const frames = Math.max( 1, Math.round( length * 30 ) );
	const side = [ 0, 1, 0.5 ][ n % 3 ];
	ffmpeg( [
		'-loop',
		'1',
		'-i',
		image,
		'-vf',
		// zoompan crops at whole pixels of its input, so each frame's position and size round;
		// at 2x those steps showed as a shake on a slow zoom. At 4x a step is a quarter of an
		// output pixel and the move reads smooth.
		`scale=${ size.width * 4 }:${ size.height * 4 }:force_original_aspect_ratio=increase:flags=lanczos,` +
			`crop=${ size.width * 4 }:${ size.height * 4 },` +
			`zoompan=z='1+0.12*on/${ frames }':x='(iw-iw/zoom)*${ side }':` +
			`y='(ih-ih/zoom)/2':d=${ frames }:s=${ size.width }x${ size.height }:fps=30,` +
			'format=yuv420p',
		'-frames:v',
		String( frames ),
		...videoCodec( 20 ),
		'-an',
		file,
	] );
	return file;
};

/**
 * A stretch of footage, cut and framed like the rest: from a few seconds in, where a clip has
 * settled, looped if it is shorter than the shot.
 *
 * @param {string} video  The footage.
 * @param {number} length How long it plays.
 * @param {string} name   The shot's file name.
 */
const footage = ( video, length, name ) => {
	const file = join( out, 'parts', name );
	ffmpeg( [
		'-stream_loop',
		'-1',
		'-ss',
		'2',
		'-i',
		video,
		'-t',
		length.toFixed( 3 ),
		'-vf',
		`scale=${ size.width }:${ size.height }:force_original_aspect_ratio=increase,` +
			`crop=${ size.width }:${ size.height },fps=30,format=yuv420p`,
		...videoCodec( 20 ),
		'-an',
		file,
	] );
	return file;
};

/**
 * One shot of a card's sequence: footage plays, a still moves.
 *
 * @param {string} source The footage or the still.
 * @param {number} length How long it plays.
 * @param {number} n      Which shot it is.
 * @param {string} name   The shot's file name.
 */
const shotOf = ( source, length, n, name ) =>
	source.endsWith( '.mp4' )
		? footage( source, length, name )
		: kenBurns( source, length, n, name );

// The title holds this long, then what Ana says is shown, a shot to each phrase or so.
const titleSeconds = 3;
// A shot holds at least this long, cut at the first pause after it.
const shotSeconds = 4;

// The video's timeline (D290): each piece in order, with what an editor needs to rebuild it,
// its track, the file it comes from, where in that file it starts, and its length as rendered.
// The MP4 joins its files and `--project` writes it out for DaVinci Resolve, so the two are
// read from one list and cannot disagree.
const videoPieces = [];

/**
 * Put a rendered piece on the timeline.
 *
 * @param {string}                                                       file  The piece, encoded for the join.
 * @param {{track: string, source: string, at: number, still?: boolean}} piece Where it comes from: V1 the recording
 *                                                                             and its held frames, V2 the cards, V3 the shots.
 */
const place = ( file, piece ) => {
	videoPieces.push( { ...piece, file, length: seconds( file ) } );
};

/**
 * A card as the editor gets it: the PNG, held.
 *
 * @param {number} i The card's index.
 */
const cardPiece = ( i ) => ( {
	track: 'V2',
	source: join( out, 'parts', `card-${ i }.png` ),
	at: 0,
	still: true,
} );

/**
 * A card, held for as long as its line runs: its title, then, while she goes on, the shots
 * given, each changing at a pause in her voice.
 *
 * @param {number}   i     The card's index.
 * @param {string}   name  The line spoken over it.
 * @param {string[]} shots Stills to show after the title.
 */
const titleCard = ( i, name, shots = [] ) => {
	const lineTakes = takes( name );
	const length = Math.max( cardSeconds, needs( lineTakes ) );
	say( clock, lineTakes );
	clock += length;
	if ( ! shots.length || length < titleSeconds + shotSeconds ) {
		place( still( i, length ), cardPiece( i ) );
		return;
	}
	// The cuts follow the longest take's pauses, as they fall on the video.
	const longest = lineTakes.reduce( ( a, b ) =>
		b.length > a.length ? b : a
	);
	const cuts = [];
	let last = titleSeconds;
	for ( const pause of pauses( longest.file ) ) {
		const at = pause + voiceLead;
		if ( at - last >= shotSeconds && length - at >= 2 ) {
			cuts.push( at );
			last = at;
		}
	}
	place( still( i, titleSeconds ), cardPiece( i ) );
	const bounds = [ titleSeconds, ...cuts, length ];
	for ( let n = 0; n < bounds.length - 1; n++ ) {
		const shotFile = shotOf(
			shots[ n % shots.length ],
			bounds[ n + 1 ] - bounds[ n ],
			n,
			`card-${ i }-shot-${ n }.mp4`
		);
		// A shot is placed as rendered: its zoom and drift, or its trim of looped footage.
		place( shotFile, { track: 'V3', source: shotFile, at: 0 } );
	}
};

// The opening and closing talk about the shop and the hills: the photos bought for it
// (demo/photos, D265) between the shop's own pages, so the screen is not only screenshots.
const photos = [ 'guide', 'product' ].flatMap( ( kind ) => {
	const dir = join( root, 'demo', 'photos', kind );
	return existsSync( dir )
		? readdirSync( dir )
				.filter( ( name ) => name.endsWith( '.webp' ) )
				.map( ( name ) => join( dir, name ) )
		: [];
} );
// The opening animation, made with Gemini (demo/intro/intro.json says where it came from and
// where its usable part starts). Not committed, as the footage is not.
const introFile = join( root, 'demo', 'intro', 'intro.mp4' );
const introMeta = join( root, 'demo', 'intro', 'intro.json' );
// Footage of the hills from Pixabay (`npm run demo:broll`), where it was fetched.
const brollDir = join( root, 'demo', 'broll' );
const clips = existsSync( brollDir )
	? readdirSync( brollDir )
			.filter( ( name ) => name.endsWith( '.mp4' ) )
			.map( ( name ) => join( brollDir, name ) )
	: [];
// Footage first, then a photo, then one of the shop's pages, in turn.
const broll = [];
for (
	let n = 0;
	n < Math.max( clips.length, photos.length, screens.length );
	n++
) {
	for ( const source of [
		clips[ n ],
		photos[ n ],
		n < 3 ? screens[ n ] : null,
	] ) {
		if ( source ) {
			broll.push( source );
		}
	}
}

// Where a vertical cut's captions sit, in libass's units for an SRT (288 to the frame's height):
// above the bottom fifth, where Shorts, Reels and TikTok draw the title, the buttons and the
// progress bar (D280). On a box, since they sit over the page's own text.
const verticalCaptionStyle =
	'FontName=Arial,FontSize=11,Bold=1,PrimaryColour=&H00FFFFFF,OutlineColour=&H50000000,' +
	'BorderStyle=3,Outline=1.2,Shadow=0,Alignment=2,MarginL=14,MarginR=14,MarginV=64';

/**
 * The finished vertical cut (D280): the narration's captions burned into the picture, since a
 * vertical player places a caption track over its own controls, then the voice; the SRT and
 * WebVTT stay beside it.
 *
 * @param {string}                                                             joined   The joined cut.
 * @param {string}                                                             name     The cut's base name.
 * @param {Array<[string, Array<{at: number, length: number, text: string}>]>} narrated Each language's takes.
 * @param {number}                                                             total    Its length in seconds.
 * @return {string} The finished file.
 */
const verticalCut = ( joined, name, narrated, total ) => {
	let picture = joined;
	if ( narrated.length ) {
		const [ language, placed ] = narrated[ 0 ];
		narrationCaptions( placed, language, name );
		picture = join( out, 'parts', `${ name }-captioned.mp4` );
		// Run from the captions' folder: a Windows path inside a filter needs escaping that a
		// bare file name does not.
		execFileSync(
			'ffmpeg',
			[
				'-y',
				'-loglevel',
				'error',
				'-i',
				joined,
				'-vf',
				`subtitles=${ name }.${ language }.srt:force_style='${ verticalCaptionStyle }'`,
				...videoCodec( 20 ),
				'-an',
				picture,
			],
			{ cwd: out, stdio: [ 'ignore', 'inherit', 'inherit' ] }
		);
	}
	const file = join( out, `${ name }${ videoSuffix }.mp4` );
	const tracks = narrated.map( ( [ language, placed ] ) =>
		voiceTrack( language, placed, total )
	);
	ffmpeg( [
		'-i',
		picture,
		...tracks.flatMap( ( track ) => [ '-i', track ] ),
		'-map',
		'0:v',
		...tracks.flatMap( ( _, i ) => [ '-map', `${ i + 1 }:a` ] ),
		'-c:v',
		'copy',
		'-c:a',
		'aac',
		'-b:a',
		'160k',
		...narrated.flatMap( ( [ language ], i ) => [
			`-metadata:s:a:${ i }`,
			`language=${ voices[ language ] }`,
		] ),
		file,
	] );
	return file;
};

// The push-in (D292): the most the frame zooms, so text stays sharp from a 1080p source; the
// room kept around the element; and how long the move takes either way: slow, while Ana talks,
// up to `slow` seconds each way when the step lasts long enough, never under `ease`. A focus
// with less time than both short moves and a hold is left at the full frame. `over` is how much
// the recording is enlarged before zoompan, which crops at whole pixels of its input: at 1x
// those steps showed as a shake on a slow move.
const pushIn = {
	cap: 1.6,
	margin: 1.25,
	ease: 0.8,
	slow: 2.5,
	hold: 0.5,
	over: 2,
};

/**
 * A chapter's clip with the frame following the action (D292): for each element the story
 * marked (`focus` in chapters.json), it eases in on that element's box while the step runs and
 * back out before the next. A vertical cut is already a phone's page, so it is left as it is.
 *
 * @param {string}                                                                                                                file    The chapter's clip.
 * @param {{start: number, end: number, focus?: Array<{at: number, box: {x: number, y: number, width: number, height: number}}>}} chapter Its timings.
 * @param {string}                                                                                                                name    The followed clip's file name.
 * @return {string} The followed clip, or the clip when nothing is to follow.
 */
/**
 * A chapter's push-ins (D292): one window per focused element it holds long enough, with its
 * zoom and centre, in seconds from the chapter's start. Read by the MP4's zoompan and by the
 * Resolve project's transform keyframes alike.
 *
 * @param {{start: number, end: number, focus?: Array<{at: number, box: Object}>}} chapter Its timings.
 * @return {Array<{from: number, to: number, zoom: number, cx: number, cy: number}>} The windows.
 */
const pushWindows = ( { start, end, focus = [] } ) =>
	vertical
		? []
		: focus
				.map( ( { at, box }, n ) => ( { at, box, n } ) )
				// A mark with no box is a release: it only ends the window before it.
				.filter( ( { box } ) => box )
				.map( ( { at, box, n } ) => ( {
					from: at - start,
					to: ( focus[ n + 1 ]?.at ?? end ) - start,
					zoom: Math.min(
						pushIn.cap,
						size.width / ( box.width * pushIn.margin ),
						size.height / ( box.height * pushIn.margin )
					),
					cx: Math.min(
						size.width,
						Math.max( 0, box.x + box.width / 2 )
					),
					cy: Math.min(
						size.height,
						Math.max( 0, box.y + box.height / 2 )
					),
				} ) )
				.filter(
					( w ) =>
						w.zoom >= 1.1 &&
						w.to - w.from >= 2 * pushIn.ease + pushIn.hold
				)
				.map( ( w ) => ( {
					...w,
					ease: Math.min(
						pushIn.slow,
						Math.max(
							pushIn.ease,
							( w.to - w.from - pushIn.hold ) / 2
						)
					),
				} ) );

/**
 * The push-in at a moment, as zoompan draws it: the zoom, and the point of the page at the
 * frame's centre, clamped so the frame never leaves the page.
 *
 * @param {Array<{from: number, to: number, zoom: number, cx: number, cy: number}>} windows The chapter's windows.
 * @param {number}                                                                  t       Seconds from its start.
 * @return {{zoom: number, x: number, y: number}} The zoom and the centre shown.
 */
const pushAt = ( windows, t ) => {
	const weights = windows.map( ( { from, to, ease } ) => {
		const ramp = Math.min(
			1,
			Math.max( 0, Math.min( ( t - from ) / ease, ( to - t ) / ease ) )
		);
		return ramp * ramp * ( 3 - 2 * ramp );
	} );
	const zoom =
		1 +
		windows.reduce( ( s, w, n ) => s + ( w.zoom - 1 ) * weights[ n ], 0 );
	const centre = ( axis, half ) =>
		half +
		windows.reduce(
			( s, w, n ) => s + ( w[ axis ] - half ) * weights[ n ],
			0
		);
	const shown = ( axis, extent ) => {
		const half = extent / zoom / 2;
		return Math.min(
			extent - half,
			Math.max( half, centre( axis, extent / 2 ) )
		);
	};
	return {
		zoom,
		x: shown( 'cx', size.width ),
		y: shown( 'cy', size.height ),
	};
};

const follow = ( file, chapter, name ) => {
	const job = followJob( file, chapter, name );
	if ( ! job ) {
		return file;
	}
	ffmpeg( job.args );
	return job.file;
};

/**
 * The push-ins' encode for a chapter's clip, without running it: null when it has none.
 *
 * @param {string} file    The chapter's clip.
 * @param {Object} chapter Its timings.
 * @param {string} name    The followed clip's file name.
 * @return {?{file: string, args: string[]}} The followed clip and ffmpeg's arguments.
 */
function followJob( file, chapter, name ) {
	const windows = pushWindows( chapter );
	if ( ! windows.length ) {
		return null;
	}
	// Each window's weight at the frame's time, 0 to 1, eased (smoothstep).
	const time = 'in/30';
	const weight = ( { from, to, ease } ) => {
		const ramp = `clip(min((${ time }-${ from.toFixed( 3 ) })/${ ease.toFixed(
			3
		) },(${ to.toFixed( 3 ) }-${ time })/${ ease.toFixed( 3 ) }),0,1)`;
		return `(${ ramp }*${ ramp }*(3-2*${ ramp }))`;
	};
	const sum = ( term ) => windows.map( term ).join( '+' );
	const zoom = `1+${ sum(
		( w ) => `${ ( w.zoom - 1 ).toFixed( 4 ) }*${ weight( w ) }`
	) }`;
	// Centres in the enlarged input's pixels.
	const { over } = pushIn;
	const centre = ( axis, half ) =>
		`${ half * over }+${ sum(
			( w ) =>
				`${ ( ( w[ axis ] - half ) * over ).toFixed( 1 ) }*${ weight( w ) }`
		) }`;
	const followed = join( out, 'parts', name );
	return {
		file: followed,
		args: [
			'-i',
			file,
			'-vf',
			`scale=${ size.width * over }:${ size.height * over }:flags=lanczos,` +
				`zoompan=z='${ zoom }':` +
				`x='clip(${ centre( 'cx', size.width / 2 ) }-iw/zoom/2,0,iw-iw/zoom)':` +
				`y='clip(${ centre( 'cy', size.height / 2 ) }-ih/zoom/2,0,ih-ih/zoom)':` +
				`d=1:s=${ size.width }x${ size.height }:fps=30,format=yuv420p`,
			...videoCodec( 20 ),
			'-an',
			followed,
		],
	};
}

// A wait the story marked (`skip` in chapters.json, from `skipping()` in the spec): an AI answer
// being written, which a viewer should not sit through. A moment of it is kept either side, so
// the cut reads as the answer arriving rather than a jump; a wait shorter than a second stays.
const skipKeep = { before: 0.6, after: 0.3, least: 1 };

/**
 * The stretches of a chapter's clip the video keeps, in seconds from its start: all of it, less
 * the waits it marked.
 *
 * @param {{start: number, skip?: Array<{from: number, to: number}>}} chapter Its timings.
 * @param {number}                                                    length  Its clip's length.
 * @return {Array<Array<number>>} The kept stretches, in order.
 */
const kept = ( { start, skip = [] }, length ) => {
	const stretches = [];
	let from = 0;
	for ( const wait of skip ) {
		const a = wait.from - start + skipKeep.before;
		const b = Math.min( length, wait.to - start - skipKeep.after );
		if ( b - a >= skipKeep.least && a > from ) {
			stretches.push( [ from, a ] );
			from = b;
		}
	}
	stretches.push( [ from, length ] );
	return stretches;
};

/**
 * Where a moment of the chapter falls once its waits are cut.
 *
 * @param {Array<Array<number>>} stretches The kept stretches.
 * @param {number}               t         Seconds from the chapter's start, as recorded.
 * @return {number} Seconds from the chapter's start, as the video plays it.
 */
const along = ( stretches, t ) =>
	stretches.reduce(
		( sum, [ a, b ] ) => sum + Math.max( 0, Math.min( b, t ) - a ),
		0
	);

/**
 * A chapter's clip with its waits cut out, and where its moments land in it.
 *
 * @param {string} clip The chapter's clip.
 * @param {number} i    The chapter's index.
 * @param {string} name The trimmed clip's base name.
 * @return {{file: string, into: function(number): number}} The clip, and its time map.
 */
const trimmed = ( clip, i, name ) => {
	const stretches = kept( chapters[ i ], seconds( clip ) );
	const into = ( t ) => along( stretches, t );
	if ( 1 === stretches.length ) {
		return { file: clip, into };
	}
	const pieces = stretches.map( ( [ a, b ], n ) =>
		cut( `${ name }-${ n }.mp4`, [
			'-ss',
			a.toFixed( 3 ),
			'-t',
			( b - a ).toFixed( 3 ),
			'-i',
			clip,
		] )
	);
	return { file: concat( pieces, `${ name }.mp4` ), into };
};

/**
 * One chapter as a clip of its own (D279): its card, the action with her line over it, the result
 * held while she talks about it, and the closing card. Where that runs past the limit, the stretch
 * between her line and the result, where nothing is said, plays faster until it fits.
 *
 * @param {number} i The chapter's index.
 */
const chapterClip = ( i ) => {
	const { start, end, voice, result: told } = chapters[ i ];
	const name = `story-chapter-${ i + 1 }${ vertical ? '-vertical' : '' }`;
	const { file: clip, into } = trimmed(
		follow(
			cut( `clip-${ i + 1 }.mp4`, [
				'-ss',
				start.toFixed( 2 ),
				'-to',
				end.toFixed( 2 ),
				'-i',
				webm,
			] ),
			chapters[ i ],
			`clip-${ i + 1 }-followed.mp4`
		),
		i,
		`clip-${ i + 1 }-trimmed`
	);
	// Where the result is: the moment she starts on it, when the story spoke it over the action,
	// else the moment held still while she does.
	const at = Math.min(
		into( told ? told.at - start : best( i ) ),
		seconds( clip ) - 0.1
	);
	const lineTakes = voice ? takes( voice.name ) : [];
	const lineAt = voice ? into( Math.max( 0, voice.at - start ) ) : 0;
	// Played as recorded up to where her line ends.
	const spokenUntil = Math.min( at, lineAt + needs( lineTakes ) );
	const result = takes( `chapter-${ i + 1 }-result` );
	// Spoken over the action, the result plays out as recorded; else it is held still.
	let held = result.length ? needs( result ) : cardSeconds;
	if ( told ) {
		held = seconds( clip ) - at;
	}
	const fixed = cardSeconds + spokenUntil + held + clipClosingSeconds;
	const silent = at - spokenUntil;
	if ( fixed + 1 > clipLimit ) {
		refuse(
			`chapter ${ i + 1 }'s lines alone run ${ fixed.toFixed(
				1
			) } s, past a clip's ${ clipLimit } s: shorten them in demo/narration/script.json.`
		);
	}
	const speed = Math.max( 1, silent / ( clipLimit - fixed ) );

	const opening = still( cards.length - 2 );
	let clipClock = cardSeconds;
	const pieces = [ opening ];
	say( clipClock + lineAt, lineTakes );
	if ( spokenUntil > 0 ) {
		pieces.push(
			cut( `clip-${ i + 1 }-a.mp4`, [
				'-t',
				spokenUntil.toFixed( 3 ),
				'-i',
				clip,
			] )
		);
		clipClock += seconds( pieces.at( -1 ) );
	}
	if ( silent > 0.05 ) {
		// The encode's own filter, after the stretch is sped up.
		const file = join( out, 'parts', `clip-${ i + 1 }-fast.mp4` );
		ffmpeg( [
			'-ss',
			spokenUntil.toFixed( 3 ),
			'-to',
			at.toFixed( 3 ),
			'-i',
			clip,
			...encode.map( ( arg, n ) =>
				encode[ n - 1 ] === '-vf'
					? `setpts=(PTS-STARTPTS)/${ speed.toFixed( 3 ) },${ arg }`
					: arg
			),
			file,
		] );
		pieces.push( file );
		clipClock += seconds( file );
	}
	pieces.push(
		told
			? cut( `clip-${ i + 1 }-result.mp4`, [
					'-ss',
					at.toFixed( 3 ),
					'-i',
					clip,
				] )
			: freeze( clip, at, held, `clip-${ i + 1 }-result` )
	);
	say( clipClock, result );
	clipClock += held;
	pieces.push( still( cards.length - 1, clipClosingSeconds ) );
	clipClock += clipClosingSeconds;

	const joined = concat( pieces, `${ name }-joined.mp4` );
	const total = seconds( joined );
	const narrated = Object.entries( spoken ).filter(
		( [ , placed ] ) => placed.length
	);
	if ( vertical ) {
		return {
			file: verticalCut( joined, name, narrated, total ),
			total,
			speed,
		};
	}
	const file = finish(
		joined,
		[
			{
				from: cardSeconds,
				to: clipClock - clipClosingSeconds,
				chapter: i,
			},
		],
		name,
		narrated.map( ( [ language, placed ] ) => ( {
			language: voices[ language ],
			file: voiceTrack( language, placed, total ),
		} ) ),
		narrated.map( ( [ language, placed ] ) => ( {
			language: voices[ language ],
			file: narrationCaptions( placed, language, name ),
		} ) )
	);
	return { file, total, speed };
};

if ( null !== clipChapter ) {
	const { file, total, speed } = chapterClip( clipChapter - 1 );
	process.stdout.write(
		`${ file }: ${ total.toFixed( 1 ) } s` +
			( speed > 1
				? `, the stretch after her line at ${ speed.toFixed( 2 ) }x\n`
				: '\n' )
	);
	process.exit( 0 );
}

// The opening animation (demo/intro): the brands joining, before the opening card, with its
// own sound; the card dissolves in over its end. Ana's story only, and only where it is on disk.
if ( ! developer && existsSync( introFile ) ) {
	const { from = 0 } = JSON.parse( readFileSync( introMeta, 'utf8' ) );
	const opening = cut( 'opening.mp4', [
		'-ss',
		from.toFixed( 3 ),
		'-i',
		introFile,
	] );
	const length = seconds( opening );
	place( opening, { track: 'V3', source: opening, at: 0 } );
	sounds.push( { file: introFile, from, at: clock, length } );
	clock += length;
	musicFrom = clock;
}
// Each chapter's clip and its push-ins, the build's heaviest encodes, made side by side before
// the timeline: they depend on nothing but the recording, and run one after another they kept
// one core busy for minutes while the others idled.
const chapterClips = await pool(
	chapters.map( ( { start, end }, i ) => async () => {
		const cutting = cutJob( `chapter-${ i + 1 }.mp4`, [
			'-ss',
			start.toFixed( 2 ),
			'-to',
			end.toFixed( 2 ),
			'-i',
			webm,
		] );
		await ffmpegAsync( cutting.args );
		const following = followJob(
			cutting.file,
			chapters[ i ],
			`chapter-${ i + 1 }-followed.mp4`
		);
		if ( ! following ) {
			return cutting.file;
		}
		await ffmpegAsync( following.args );
		return following.file;
	} )
);

// The developer walkthrough opens and closes on its cards alone: the hills belong to Ana's shop.
titleCard( 0, 'intro', developer ? [] : broll );
for ( const [ i, { start } ] of chapters.entries() ) {
	const clip = chapterClips[ i ];
	// The chapter's card is its title alone: Ana's line about it is spoken over the action
	// itself, where the recording marked it (`speak()` in the story), its end on the Enter.
	place( still( i + 1 ), cardPiece( i + 1 ) );
	clock += cardSeconds;
	const from = clock;
	const { voice, result: told } = chapters[ i ];
	// The waits the story marked are cut (`skip`); every moment after one moves up by it.
	const stretches = kept( chapters[ i ], seconds( clip ) );
	const into = ( t ) => along( stretches, t );
	if ( voice ) {
		say(
			from + into( Math.max( 0, voice.at - start ) ),
			takes( voice.name )
		);
	}
	// Where the chapter's clip starts in the recording, as the cut above seeked it.
	const inRecording = Number( start.toFixed( 2 ) );
	const result = takes( `chapter-${ i + 1 }-result` );
	// Her line about the result is spoken over the action where the story said it (`result`,
	// from `tell()`), or else over the result held still (`highlight()`).
	if ( told && result.length ) {
		say( from + into( told.at - start ), result );
	}
	let at = result.length && ! told ? best( i ) : null;
	if ( null !== at ) {
		// Held inside a kept stretch, never inside a cut wait.
		const [ , last ] =
			stretches.find( ( [ a, b ] ) => at >= a && at < b ) ??
			stretches.findLast( ( [ a ] ) => a <= at ) ??
			stretches[ 0 ];
		at = Math.min( at, last - 0.1, seconds( clip ) - 0.1 );
	}
	let n = 0;
	for ( const [ a, b ] of stretches ) {
		const stops = null !== at && at > a && at < b ? [ a, at, b ] : [ a, b ];
		for ( let k = 0; k + 1 < stops.length; k++ ) {
			const [ p, q ] = [ stops[ k ], stops[ k + 1 ] ];
			const piece =
				1 === stretches.length && 2 === stops.length
					? clip
					: cut( `chapter-${ i + 1 }-${ n++ }.mp4`, [
							'-ss',
							p.toFixed( 3 ),
							'-t',
							( q - p ).toFixed( 3 ),
							'-i',
							clip,
						] );
			place( piece, {
				track: 'V1',
				source: webm,
				at: inRecording + Number( p.toFixed( 3 ) ),
				chapter: i,
				inChapter: Number( p.toFixed( 3 ) ),
			} );
			clock += seconds( piece );
			if ( q === at ) {
				// The result held while she talks about it; the rest follows.
				const length = needs( result );
				const held = freeze(
					clip,
					at,
					length,
					`chapter-${ i + 1 }-result`
				);
				place( held, {
					track: 'V1',
					source: held.replace( /\.mp4$/, '.png' ),
					at: 0,
					still: true,
				} );
				say( clock, result );
				clock += length;
			}
		}
	}
	cues.push( { from, to: clock, chapter: i } );
}
titleCard( cards.length - 1, 'outro', developer ? [] : [ ...broll ].reverse() );
// The video ends on the closing card, alone and long enough to read its two addresses: under
// the outro it gives way to the footage after its title (D275).
const closingSeconds = 6;
place(
	still( cards.length - 1, closingSeconds ),
	cardPiece( cards.length - 1 )
);
clock += closingSeconds;

// One still per chapter, its result on screen, for the site's WordPress page (D197): taken
// from the recording, so the page shows what the story ran, and copied into the site.
const stills = join( out, 'stills' );
const site = join( root, '..', 'dumont-site', 'public', 'wordpress' );
mkdirSync( stills, { recursive: true } );
mkdirSync( site, { recursive: true } );
for ( const i of chapters.keys() ) {
	const file = join( stills, `chapter-${ i + 1 }.webp` );
	ffmpeg( [
		'-ss',
		best( i ).toFixed( 2 ),
		'-i',
		join( out, 'parts', `chapter-${ i + 1 }.mp4` ),
		'-frames:v',
		'1',
		'-vf',
		'scale=1280:-1',
		'-c:v',
		'libwebp',
		'-quality',
		'80',
		file,
	] );
	// The site's WordPress page is English: another language's recording keeps its stills here.
	if ( 'en' === videoLanguage && ! developer ) {
		copyFileSync( file, join( site, `chapter-${ i + 1 }.webp` ) );
	}
}

// Dissolves (D291) where a card meets the recording or a shot, and between the shots: the
// still crossfades over the first or last half second of the clip beside it. The still goes on
// through the blend instead of overlapping the clip, so no piece moves and every take stays where
// it was placed. Inside a chapter the cuts stay hard: they are the product responding.
const dissolveSeconds = 0.5;

/**
 * A stretch of a clip with a still crossfading over one end of it, encoded.
 *
 * @param {string}     file   The clip.
 * @param {number}     from   Where the stretch starts in it, on a keyframe.
 * @param {number}     length How long the stretch is.
 * @param {string}     image  The still.
 * @param {'in'|'out'} end    'out' fades the still out over the stretch's start, 'in' fades it
 *                            in over its end.
 * @param {string}     name   The blended stretch's file name.
 */
const blendSpan = async ( file, from, length, image, end, name ) => {
	const fade =
		'out' === end
			? `fade=t=out:st=0:d=${ dissolveSeconds }:alpha=1`
			: `fade=t=in:st=${ Math.max( 0, length - dissolveSeconds ).toFixed(
					3
				) }:d=${ dissolveSeconds }:alpha=1`;
	const blended = join( out, 'parts', name );
	await ffmpegAsync( [
		...( from > 0 ? [ '-ss', from.toFixed( 3 ) ] : [] ),
		'-t',
		length.toFixed( 3 ),
		'-i',
		file,
		'-loop',
		'1',
		'-t',
		length.toFixed( 3 ),
		'-i',
		image,
		'-filter_complex',
		`[1:v]scale=${ size.width }:${ size.height },fps=30,format=yuva420p,${ fade }[still];` +
			'[0:v][still]overlay=shortest=1,format=yuv420p[v]',
		'-map',
		'[v]',
		...videoCodec( 20 ),
		'-an',
		blended,
	] );
	return blended;
};

/**
 * A stretch of a clip copied as encoded, cut on its keyframes.
 *
 * @param {string}  file The clip.
 * @param {number}  from Where it starts, on a keyframe.
 * @param {?number} to   Where it ends, on a keyframe; null for the clip's end.
 * @param {string}  name The copy's file name.
 */
const copySpan = async ( file, from, to, name ) => {
	const copied = join( out, 'parts', name );
	await ffmpegAsync( [
		...( from > 0 ? [ '-ss', from.toFixed( 3 ) ] : [] ),
		'-i',
		file,
		...( null === to ? [] : [ '-t', ( to - from ).toFixed( 3 ) ] ),
		'-c',
		'copy',
		'-an',
		'-avoid_negative_ts',
		'make_zero',
		copied,
	] );
	return copied;
};

/**
 * A piece with its dissolves (D291): the still fading out over its first half second and in
 * over its last. Only the half second blended is encoded again; the rest is copied, cut on the
 * keyframe every piece carries each half second. A piece too short to split is blended whole.
 *
 * @param {string}                      file The piece.
 * @param {{out: ?string, in: ?string}} with The stills it dissolves from and into.
 * @param {number}                      n    The piece's index, for the files' names.
 * @return {Promise<string[]>} The piece, as the files that join into it.
 */
const dissolvePiece = async ( file, { out: from, in: into }, n ) => {
	const step = keyEvery / 30;
	let pieces = [ file ];
	if ( from ) {
		const length = seconds( file );
		pieces =
			length >= 2 * step
				? [
						await blendSpan(
							file,
							0,
							step,
							from,
							'out',
							`dissolve-${ n }-out-head.mp4`
						),
						await copySpan(
							file,
							step,
							null,
							`dissolve-${ n }-out-rest.mp4`
						),
					]
				: [
						await blendSpan(
							file,
							0,
							length,
							from,
							'out',
							`dissolve-${ n }-out.mp4`
						),
					];
	}
	if ( into ) {
		const last = pieces.pop();
		const length = seconds( last );
		const keyAt = Math.floor( ( length - dissolveSeconds ) / step ) * step;
		if ( keyAt > 0 ) {
			pieces.push(
				await copySpan( last, 0, keyAt, `dissolve-${ n }-in-head.mp4` ),
				await blendSpan(
					last,
					keyAt,
					length - keyAt,
					into,
					'in',
					`dissolve-${ n }-in-tail.mp4`
				)
			);
		} else {
			pieces.push(
				await blendSpan(
					last,
					0,
					length,
					into,
					'in',
					`dissolve-${ n }-in.mp4`
				)
			);
		}
	}
	return pieces;
};

/**
 * A clip's last frame, as a still to dissolve from.
 *
 * @param {string} file The clip.
 * @param {number} n    The piece's index, for the file's name.
 */
const lastFrame = ( file, n ) => {
	const frame = join( out, 'parts', `dissolve-${ n }-last.png` );
	ffmpeg( [ '-sseof', '-0.1', '-i', file, '-frames:v', '1', frame ] );
	return frame;
};

// Which still each piece dissolves from at its start and into at its end, decided first;
// then the pieces are blended side by side, each one's two blends in order (out, then in).
// A start's fade leaves a piece's last frame as it was, so it is read from the piece itself.
const dissolves = videoPieces.map( () => ( { out: null, in: null } ) );
for ( let n = 0; n < videoPieces.length - 1; n++ ) {
	const [ from, to ] = [ videoPieces[ n ], videoPieces[ n + 1 ] ];
	if ( 'V1' === from.track && 'V1' === to.track ) {
		continue;
	}
	if ( to.still ) {
		// Into a card: the card fades in over the end of what plays before it.
		dissolves[ n ].in = to.source;
	} else {
		// Out of a card or a shot: it fades out over the start of what comes next.
		dissolves[ n + 1 ].out = from.still
			? from.source
			: lastFrame( from.file, n );
	}
}
const joinedFiles = (
	await pool(
		videoPieces.map(
			( piece, n ) => () => dissolvePiece( piece.file, dissolves[ n ], n )
		)
	)
).flat();
const joined = concat( joinedFiles, 'joined.mp4' );
const joinedLength = seconds( joined );
const narrated = Object.entries( spoken ).filter(
	( [ , placed ] ) => placed.length
);
for ( const language of Object.keys( voices ) ) {
	chapterList( cues, language, base );
}
const video = finish(
	joined,
	cues,
	base,
	narrated.map( ( [ language, placed ] ) => ( {
		language: voices[ language ],
		file: voiceTrack( language, placed, joinedLength ),
	} ) ),
	narrated.map( ( [ language, placed ] ) => ( {
		language: voices[ language ],
		file: narrationCaptions( placed, language, base ),
	} ) )
);

/**
 * The video as a DaVinci Resolve project (D290): `story.<lang>.project/`, an FCPXML read from the
 * same timeline as the MP4, beside a media folder holding every file it points at. V1 is the
 * recording cut by chapter with the held results as stills, V2 the cards, V3 the opening and
 * closing shots, A1 each of Ana's takes where the video places it, A2 the music bed at the
 * video's level; each chapter is a marker. The recording is transcoded once to H.264, since
 * Playwright's VP8 WebM is not something every editor reads. Paths are absolute file URLs to
 * that media folder, which is what Resolve's import resolves; a moved folder is relinked there.
 *
 * @param {number} total The video's length in seconds.
 * @return {string} The project file.
 */
const writeProject = ( total ) => {
	const dir = join( out, `${ base }${ videoSuffix }.project` );
	const media = join( dir, 'media' );
	mkdirSync( media, { recursive: true } );
	const fps = 30;
	const time = ( s ) => `${ Math.max( 0, Math.round( s * fps ) ) }/${ fps }s`;
	const xml = ( text ) =>
		String( text )
			.replace( /&/g, '&amp;' )
			.replace( /</g, '&lt;' )
			.replace( />/g, '&gt;' )
			.replace( /"/g, '&quot;' );
	const url = ( file ) =>
		'file:///' +
		file
			.replace( /\\/g, '/' )
			.replace( /^\//, '' )
			.split( '/' )
			.map( ( part, n ) =>
				0 === n && /^[A-Za-z]:$/.test( part )
					? part
					: encodeURIComponent( part )
			)
			.join( '/' );

	// Every source copied into the project's media folder once, the recording transcoded.
	const assets = new Map();
	const asset = ( source, kind ) => {
		if ( assets.has( source ) ) {
			return assets.get( source );
		}
		let file;
		if ( source === webm ) {
			file = join( media, 'recording.mp4' );
			ffmpeg( [
				'-i',
				webm,
				'-vf',
				`fps=${ fps },format=yuv420p`,
				...videoCodec( 18 ),
				'-an',
				file,
			] );
		} else {
			file = join(
				media,
				`${ assets.size }-${ source.split( /[\\/]/ ).pop() }`
			);
			copyFileSync( source, file );
		}
		const entry = {
			id: `r${ assets.size + 3 }`,
			name: file.split( /[\\/]/ ).pop(),
			file,
			kind,
			length: 'image' === kind ? 0 : seconds( file ),
		};
		assets.set( source, entry );
		return entry;
	};

	// The spine: V1 pieces directly, the shots and the cards over a gap on their own lanes. A
	// gap's own time starts at gapStart, not 0, so a card can begin before its gap does: a
	// connected clip's offset cannot be negative, and a card fades in over what plays before it
	// (D300).
	const gapStart = 10;
	let offset = 0;
	const spine = videoPieces.map( ( piece ) => {
		const item = {
			offset,
			length: piece.length,
			// The recording starts at its in point, a card (on the spine since D300) at 0, a
			// shot's gap at gapStart.
			start: { V1: piece.at, V2: 0 }[ piece.track ] ?? gapStart,
			asset: asset( piece.source, piece.still ? 'image' : 'video' ),
			piece,
			anchored: [],
			markers: [],
		};
		offset += piece.length;
		return item;
	} );
	// What starts at a moment of the video hangs from the spine item playing then, at that
	// item's own time.
	const holder = ( at ) =>
		spine.find( ( item ) => at < item.offset + item.length ) ??
		spine.at( -1 );
	const local = ( item, at ) => item.start + ( at - item.offset );

	for ( const [ language, placed ] of Object.entries( spoken ) ) {
		for ( const take of placed ) {
			const item = holder( take.at );
			item.anchored.push(
				`<asset-clip ref="${
					asset( take.file, 'audio' ).id
				}" lane="-1" offset="${ time(
					local( item, take.at )
				) }" start="0s" duration="${ time(
					take.length
				) }" name="${ xml(
					`${ language } ${ take.file.split( /[\\/]/ ).pop() }`
				) }" audioRole="dialogue"/>`
			);
		}
	}
	if ( existsSync( music ) ) {
		// One clip the video's length, looped and faded as the MP4's bed is: placed as a clip per
		// loop, Resolve spread the pieces over both audio tracks among the takes.
		const looped = join( out, 'parts', 'music-bed.m4a' );
		ffmpeg( [
			'-stream_loop',
			'-1',
			'-i',
			music,
			'-t',
			total.toFixed( 3 ),
			'-af',
			`volume=${ musicTrim() }dB,afade=t=in:d=2,afade=t=out:st=${ Math.max(
				0,
				total - 3
			).toFixed( 3 ) }:d=3`,
			'-c:a',
			'aac',
			'-b:a',
			'192k',
			looped,
		] );
		const bed = asset( looped, 'audio' );
		// The ducking as volume keyframes (D291), the same envelope the MP4 renders, so it can be
		// moved in Fairlight: a point where each ramp starts and ends, read off `musicGain()`.
		const placed =
			Object.values( spoken ).find( ( takes_ ) => takes_.length ) ?? [];
		const turns = placed
			.flatMap( ( { at, length } ) => [
				at - duck.ramp,
				at,
				at + length,
				at + length + duck.ramp,
			] )
			.sort( ( a, b ) => a - b );
		const db = ( t ) =>
			`${ ( 20 * Math.log10( musicGain( placed, t ) ) ).toFixed( 1 ) }dB`;
		const until = Math.min( bed.length, total );
		const keys = [
			0,
			...turns.filter( ( t ) => t > 0 && t < until ),
			until,
		]
			.map(
				( t ) =>
					`<keyframe time="${ time( t ) }" value="${ db( t ) }"/>`
			)
			.join( '' );
		const item = holder( 0 );
		item.anchored.push(
			`<asset-clip ref="${ bed.id }" lane="-2" offset="${ time(
				local( item, 0 )
			) }" start="0s" duration="${ time(
				until
			) }" name="music" audioRole="music"><adjust-volume><param name="amount"><keyframeAnimation>${ keys }</keyframeAnimation></param></adjust-volume></asset-clip>`
		);
	}
	// No captions here: Resolve 21 drops FCPXML <caption> on import (tried, D293), and its scripting
	// API will not import an .srt. The .srt beside the project is imported by hand.
	for ( const cue of cues ) {
		const { number, name } = parts[ cue.chapter ];
		const item = holder( cue.from );
		item.markers.push(
			`<marker start="${ time(
				local( item, cue.from )
			) }" duration="1/${ fps }s" value="${ xml(
				`${ number }. ${ words.name( number, name ) }`
			) }"/>`
		);
	}

	/**
	 * A keyframed parameter, `<param>` with its keyframes in the clip's own time.
	 *
	 * @param {string}                  name The parameter.
	 * @param {Array<[number, string]>} keys Each key's time in the clip and its value.
	 */
	const keyframed = ( name, keys ) =>
		`<param name="${ name }"><keyframeAnimation>${ keys
			// One key per frame: the last one written for a frame is the one kept.
			.filter(
				( [ t ], n ) =>
					n === keys.length - 1 ||
					Math.round( t * fps ) !==
						Math.round( keys[ n + 1 ][ 0 ] * fps )
			)
			.map(
				( [ t, value ] ) =>
					`<keyframe time="${ time( t ) }" value="${ value }"/>`
			)
			.join( '' ) }</keyframeAnimation></param>`;

	// The push-ins (D292) as transform keyframes on a recording clip: the same curve the MP4's
	// zoompan draws, sampled where each move starts and ends. Position is in FCPXML's units, a
	// hundredth of the frame's height, with y up.
	const pushKeys = ( item ) => {
		const { chapter, inChapter } = item.piece;
		if ( undefined === chapter ) {
			return '';
		}
		const windows = pushWindows( chapters[ chapter ] );
		if ( ! windows.length ) {
			return '';
		}
		const until = inChapter + item.length;
		const moments = [
			inChapter,
			...windows.flatMap( ( w ) => [
				w.from,
				w.from + w.ease / 2,
				w.from + w.ease,
				w.to - w.ease,
				w.to - w.ease / 2,
				w.to,
			] ),
			until,
		]
			.filter( ( t ) => t >= inChapter && t <= until )
			.sort( ( a, b ) => a - b )
			// One key per frame: two windows that meet would key the same frame twice.
			.filter(
				( t, n, all ) =>
					0 === n ||
					Math.round( t * fps ) !== Math.round( all[ n - 1 ] * fps )
			);
		const scale = [];
		const position = [];
		for ( const t of moments ) {
			const { zoom, x, y } = pushAt( windows, t );
			const at = item.start + ( t - inChapter );
			const unit = 100 / size.height;
			scale.push( [
				at,
				`${ zoom.toFixed( 4 ) } ${ zoom.toFixed( 4 ) }`,
			] );
			position.push( [
				at,
				`${ ( -zoom * ( x - size.width / 2 ) * unit ).toFixed( 3 ) } ${ (
					zoom *
					( y - size.height / 2 ) *
					unit
				).toFixed( 3 ) }`,
			] );
		}
		return `<adjust-transform>${ keyframed(
			'position',
			position
		) }${ keyframed( 'scale', scale ) }</adjust-transform>`;
	};

	const clip = ( item ) => {
		const tag = 'image' === item.asset.kind ? 'video' : 'asset-clip';
		const inner =
			pushKeys( item ) + [ ...item.anchored, ...item.markers ].join( '' );
		return `<${ tag } ref="${ item.asset.id }" name="${ xml(
			item.asset.name
		) }" offset="${ time( item.offset ) }" start="${ time(
			item.start
		) }" duration="${ time( item.length ) }"${
			inner ? `>${ inner }</${ tag }>` : '/>'
		}`;
	};

	/**
	 * A shot on its lane over its gap: rendered to its exact length, it has no handles for a
	 * transition, so it stays off the spine.
	 *
	 * @param {Object} item The gap's item.
	 */
	const laneClip = ( item ) =>
		`<asset-clip ref="${ item.asset.id }" name="${ xml(
			item.asset.name
		) }" lane="1" offset="${ time(
			gapStart
		) }" start="0s" duration="${ time( item.length ) }"/>`;

	// The dissolves (D300) as Cross Dissolve transitions on the spine, wherever a card meets the
	// recording: Resolve 21 imported opacity keyframes on a card (adjust-blend) and did nothing
	// with them. The recording has handles both sides of every cut and a card is a still, so
	// the transition centres on the edit. A card beside a shot keeps its cut: a shot has none.
	const half = Math.round( ( dissolveSeconds * fps ) / 2 );
	const recording = ( item ) =>
		'V1' === item.piece.track && ! item.piece.still;
	const isCard = ( item ) => 'V2' === item.piece.track;
	const spineXml = spine
		.map( ( item, n ) => {
			const before = spine[ n - 1 ];
			const transition =
				before &&
				( ( isCard( before ) && recording( item ) ) ||
					( recording( before ) && isCard( item ) ) )
					? `<transition name="Cross Dissolve" offset="${
							Math.round( item.offset * fps ) - half
						}/${ fps }s" duration="${ 2 * half }/${ fps }s"/>\n`
					: '';
			if ( 'V3' !== item.piece.track ) {
				return transition + clip( item );
			}
			return `<gap name="Gap" offset="${ time(
				item.offset
			) }" start="${ time( gapStart ) }" duration="${ time(
				item.length
			) }">${ laneClip( item ) }${ [
				...item.anchored,
				...item.markers,
			].join( '' ) }</gap>`;
		} )
		.join( '\n' );
	// A gap's own time starts at 0 and a V1 clip's at its in point, so `local()` already
	// placed what hangs from each in that item's time.

	const resources = [
		`<format id="r1" name="FFVideoFormat${ size.height }p${ fps }" frameDuration="1/${ fps }s" width="${ size.width }" height="${ size.height }"/>`,
		`<format id="r2" name="FFVideoFormatRateUndefined" width="${ size.width }" height="${ size.height }"/>`,
		...[ ...assets.values() ].map( ( entry ) => {
			const media_ = `<media-rep kind="original-media" src="${ xml(
				url( entry.file )
			) }"/>`;
			if ( 'audio' === entry.kind ) {
				return `<asset id="${ entry.id }" name="${ xml(
					entry.name
				) }" start="0s" duration="${ time(
					entry.length
				) }" hasAudio="1" audioSources="1" audioChannels="2" audioRate="48000">${ media_ }</asset>`;
			}
			return `<asset id="${ entry.id }" name="${ xml(
				entry.name
			) }" start="0s" duration="${ time( entry.length ) }" hasVideo="1" format="${
				'image' === entry.kind ? 'r2' : 'r1'
			}">${ media_ }</asset>`;
		} ),
	];
	const file = join( dir, `${ base }${ videoSuffix }.fcpxml` );
	writeFileSync(
		file,
		'<?xml version="1.0" encoding="UTF-8"?>\n<!DOCTYPE fcpxml>\n' +
			'<fcpxml version="1.10">\n<resources>\n' +
			resources.join( '\n' ) +
			'\n</resources>\n<library>\n<event name="Viglet Turing Search demo">\n' +
			`<project name="story${ videoSuffix }">\n` +
			`<sequence format="r1" duration="${ time(
				offset
			) }" tcStart="0s" tcFormat="NDF" audioLayout="stereo" audioRate="48k">\n<spine>\n` +
			spineXml +
			'\n</spine>\n</sequence>\n</project>\n</event>\n</library>\n</fcpxml>\n'
	);
	// What FCPXML cannot carry (D293): demo/resolve/finish.lua, run inside Resolve, imports this
	// timeline and the captions and queues the render. Written here with this project's paths.
	const srt = join( out, `${ base }.${ videoLanguage }.srt` );
	const captions = join( dir, `${ base }.${ videoLanguage }.srt` );
	if ( existsSync( srt ) ) {
		copyFileSync( srt, captions );
	}
	writeFileSync(
		join( dir, 'finish.lua' ),
		readFileSync( join( root, 'demo', 'resolve', 'finish.lua' ), 'utf8' )
			.replace( '__NAME__', `${ base }${ videoSuffix }` )
			.replace( '__FCPXML__', file )
			.replace( '__SRT__', captions )
			.replace( '__RENDER_DIR__', dir )
	);
	return file;
};

// The short cut: each chapter's last seconds, where its result shows, with no cards between.
const intro = 2;
const short = [
	cut( 'short-intro.mp4', [
		'-t',
		String( intro ),
		'-i',
		join( out, 'parts', 'card-0.mp4' ),
	] ),
];
const shortCues = [];
clock = intro;
for ( const i of chapters.keys() ) {
	// The seconds leading up to its best moment, and a little after.
	const clip = cut( `short-${ i + 1 }.mp4`, [
		'-ss',
		Math.max( 0, best( i ) + 0.8 - shortSeconds ).toFixed( 2 ),
		'-t',
		String( shortSeconds ),
		'-i',
		join( out, 'parts', `chapter-${ i + 1 }.mp4` ),
	] );
	short.push( clip );
	const length = seconds( clip );
	shortCues.push( { from: clock, to: clock + length, chapter: i } );
	clock += length;
}
short.push( join( out, 'parts', `card-${ cards.length - 1 }.mp4` ) );
const teaser = finish(
	concat( short, 'joined-30s.mp4' ),
	shortCues,
	`${ base }-30s`
);

const length = ( file ) => {
	const total = seconds( file );
	return `${ Math.floor( total / 60 ) }m${ String(
		Math.round( total % 60 )
	).padStart( 2, '0' ) }s`;
};
const project = process.argv.includes( '--project' )
	? writeProject( joinedLength )
	: null;
process.stdout.write(
	`${ video }: ${ length( video ) }, ${ chapters.length } chapters, ` +
		Object.entries( loudness.measured )
			.map( ( [ language, lufs ] ) => `${ language } mix ${ lufs } LUFS` )
			.join( ', ' ) +
		` (target ${ loudness.target })\n` +
		`${ teaser }: ${ length( teaser ) }\n` +
		"the narration's captions (SRT and WebVTT) and the chapter list beside the video, " +
		'the captions inside it as tracks\n' +
		( project
			? `${ project }: the same timeline as a DaVinci Resolve project, its media beside it\n`
			: '' )
);
