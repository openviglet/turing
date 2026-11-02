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
// Writes tests/e2e/artifacts/video/: story.mp4 (both caption tracks), story.en.srt and
// story.pt-BR.srt, and story-30s.mp4, a cut for social posts that keeps the last seconds of
// each chapter, where its result shows. Rendered files are release assets, never committed;
// the one exception is a WebP still per chapter, copied into dumont-site/public/wordpress/
// for the site's WordPress page.
// Refuses a recording that is missing a chapter or older than the story it was recorded from,
// so a re-record after a UI change is never skipped by accident.
import { execFileSync, spawnSync } from 'node:child_process';
import {
	copyFileSync,
	existsSync,
	mkdirSync,
	readdirSync,
	readFileSync,
	statSync,
	writeFileSync,
} from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { chromium } from '@playwright/test';
import { current, lines, narration, takeFile } from './narration-takes.mjs';

const root = join( dirname( fileURLToPath( import.meta.url ) ), '..' );
const recorded = join( root, 'tests', 'e2e', 'artifacts', 'story' );
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
// The narration starts this long after its card shows, and ends this long before the next.
const voiceLead = 0.4;
const voiceTail = 0.5;
// The music bed under the voice: low enough that every word stays clear.
const musicVolume = 0.12;
const out = join( root, 'tests', 'e2e', 'artifacts', 'video' );
const size = { width: 1920, height: 1080 };
const cardSeconds = 2.5;
const chapterCount = 8;
// The short cut: this much of the end of each chapter, between a short intro and outro.
const shortSeconds = 3;

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
const webm = join( recorded, 'story.webm' );
if ( ! existsSync( timeline ) || ! existsSync( webm ) ) {
	refuse( 'no recording yet: run `npm run e2e:story:record` first.' );
}
const { record, chapters } = JSON.parse( readFileSync( timeline, 'utf8' ) );
if ( ! record ) {
	refuse(
		'the last story run was the test, not the recording: run `npm run e2e:story:record`.'
	);
}
if ( chapters.length !== chapterCount ) {
	refuse(
		`the recording has ${ chapters.length } of ${ chapterCount } chapters: the story failed part way. Re-record it.`
	);
}
for ( const source of [
	join( root, 'demo', 'story.md' ),
	join( root, 'tests', 'e2e', 'story', 'shopper.story.mjs' ),
] ) {
	if ( statSync( source ).mtimeMs > statSync( timeline ).mtimeMs ) {
		refuse(
			`${ source } changed after the recording: run \`npm run e2e:story:record\` again.`
		);
	}
}

/**
 * Run ffmpeg quietly, failing loudly.
 *
 * @param {string[]} args Its arguments.
 */
const ffmpeg = ( args ) =>
	execFileSync( 'ffmpeg', [ '-y', '-loglevel', 'error', ...args ], {
		stdio: [ 'ignore', 'inherit', 'inherit' ],
	} );

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
const style = `
@font-face { font-family: Barlow; font-weight: 700; src: url(${ font(
	'barlow-condensed-latin-700-normal.woff2'
) }); }
@font-face { font-family: Source; src: url(${ font(
	'source-sans-3-latin-wght-normal.woff2'
) }); }
html, body { margin: 0; width: 1920px; height: 1080px; }
body { display: flex; flex-direction: column; justify-content: center; padding: 0 160px;
	box-sizing: border-box; background: linear-gradient(160deg, #1F3B2D 0%, #2E5E73 100%);
	color: #F6F3EC; font-family: Source, sans-serif; }
small { font-size: 44px; letter-spacing: .08em; text-transform: uppercase; color: #C9D8B6; }
h1 { font-family: Barlow, sans-serif; font-size: 132px; line-height: 1.05; margin: 24px 0; }
p { font-size: 48px; margin: 0; color: #C9D8B6; }`;

/**
 * A card's markup.
 *
 * @param {string} kicker The line above.
 * @param {string} title  The title.
 * @param {string} line   The line below.
 */
const card = ( kicker, title, line ) =>
	`<!doctype html><meta charset="utf-8"><style>${ style }</style>` +
	`<small>${ kicker }</small><h1>${ title }</h1><p>${ line }</p>`;

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
const words =
	'pt-BR' === videoLanguage
		? {
				what: 'Plugin open source para WordPress e WooCommerce',
				brand: 'Viglet Turing Search',
				line: 'Uma cliente, uma visita: de uma necessidade vaga a um carrinho pago.',
				chapter: 'Capítulo',
				store: 'Recanto da Serra, a loja de demonstração',
				name: ( number, name ) => portuguese[ number ] ?? name,
				tryIt: 'Experimente',
				tryTitle: 'A loja de demonstração no seu navegador',
				tryLine:
					'openviglet.github.io/dumont/wordpress · sem instalar nada<br>github.com/openviglet/dumont',
			}
		: {
				what: 'Open source plugin for WordPress and WooCommerce',
				brand: 'Viglet Turing Search',
				line: 'One shopper, one visit: from a vague need to a paid-for cart.',
				chapter: 'Chapter',
				store: 'Tarnfell, the demo store',
				name: ( number, name ) => name,
				tryIt: 'Try it',
				tryTitle: 'The demo store in your browser',
				tryLine:
					'openviglet.github.io/dumont/wordpress · nothing to install<br>github.com/openviglet/dumont',
			};
// The opening says what this is, for a viewer who arrives from a search or watches muted; the
// closing says where to try it (D275).
const cards = [
	card( words.what, words.brand, words.line ),
	...parts.map( ( { number, name } ) =>
		card(
			`${ words.chapter } ${ number }`,
			words.name( number, name ),
			words.store
		)
	),
	card( words.tryIt, words.tryTitle, words.tryLine ),
];
for ( const [ i, html ] of cards.entries() ) {
	await page.setContent( html );
	await page.evaluate( () => document.fonts.ready );
	await page.screenshot( { path: join( out, 'parts', `card-${ i }.png` ) } );
}

// The store's own pages, for what Ana says over the opening and closing cards (D261): she
// talks about her shop, so the shop is on screen. The demo stack must be up; without it the
// cards stay as they are.
const storeUrl = process.env.STORE_URL || 'http://localhost:38082';
const screens = [];
const shot = async ( path, scroll = 0 ) => {
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
	'-c:v',
	'libx264',
	'-preset',
	'veryfast',
	'-crf',
	'20',
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
	ffmpeg( [
		'-loop',
		'1',
		'-t',
		length.toFixed( 3 ),
		'-i',
		join( out, 'parts', `card-${ i }.png` ),
		...encode,
		file,
	] );
	return file;
};

/**
 * A segment cut from a file, encoded like the rest.
 *
 * @param {string}   name  The part's file name.
 * @param {string[]} input ffmpeg's input arguments, seek included.
 */
const cut = ( name, input ) => {
	const file = join( out, 'parts', name );
	ffmpeg( [ ...input, ...encode, file ] );
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
				const text = 'en' === language ? name : portuguese[ number ];
				return `${ i + 1 }\n${ stamp( cue.from ) } --> ${ stamp(
					cue.to
				) }\n${ number }. ${ text }\n`;
			} )
			.join( '\n' )
	);
	return file;
};

/**
 * The finished file: the joined video with both caption tracks, English first.
 *
 * @param {string}                                             joined The joined video.
 * @param {Array<{from: number, to: number, chapter: number}>} cues   When each chapter shows.
 * @param {string}                                             name   The file's base name.
 * @param {Array<{file: string, language: string}>}            tracks The narration, one track per language.
 */
const finish = ( joined, cues, name, tracks = [] ) => {
	const en = subtitles( cues, 'en', join( out, `${ name }.en.srt` ) );
	const pt = subtitles( cues, 'pt-BR', join( out, `${ name }.pt-BR.srt` ) );
	const file = join( out, `${ name }.mp4` );
	ffmpeg( [
		'-i',
		joined,
		'-i',
		en,
		'-i',
		pt,
		...tracks.flatMap( ( track ) => [ '-i', track.file ] ),
		'-map',
		'0:v',
		...tracks.flatMap( ( track, i ) => [ '-map', `${ i + 3 }:a` ] ),
		'-map',
		'1',
		'-map',
		'2',
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
		'-metadata:s:s:0',
		'language=eng',
		'-metadata:s:s:1',
		'language=por',
		file,
	] );
	return file;
};

const texts = Object.fromEntries( lines() );
const music = join( narration, 'music.mp3' );

/**
 * A line's takes, by language: only one that says the script's current line (an older take
 * stays out of the video), with its length.
 *
 * @param {string} name The line, as narration-takes.mjs names it.
 */
const takes = ( name ) =>
	Object.keys( voices ).flatMap( ( language ) => {
		const file = takeFile( language, name );
		return current( file, texts[ name ]?.[ language ] )
			? [ { language, file, length: seconds( file ) } ]
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
	if ( existsSync( music ) ) {
		// Quiet under the voice, faded in and out, looped for as long as the video runs.
		const fadeOut = Math.max( 0, total - 3 ).toFixed( 3 );
		inputs.push( '-stream_loop', '-1', '-i', music );
		graph.push(
			`[${ placed.length }:a]atrim=0:${ total.toFixed( 3 ) },` +
				`volume=${ musicVolume },afade=t=in:d=2,` +
				`afade=t=out:st=${ fadeOut }:d=3[m]`
		);
		labels += '[m]';
		count++;
	}
	graph.push(
		`${ labels }amix=inputs=${ count }:normalize=0,` +
			`apad=whole_dur=${ total.toFixed( 3 ) }[a]`
	);
	ffmpeg( [
		...inputs,
		'-filter_complex',
		graph.join( ';' ),
		'-map',
		'[a]',
		'-t',
		total.toFixed( 3 ),
		'-c:a',
		'aac',
		'-b:a',
		'160k',
		file,
	] );
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
const segments = [];
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
		spoken[ take.language ].push( { at: at + voiceLead, file: take.file } );
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
		`scale=${ size.width * 2 }:${ size.height * 2 }:force_original_aspect_ratio=increase,` +
			`crop=${ size.width * 2 }:${ size.height * 2 },` +
			`zoompan=z='1+0.12*on/${ frames }':x='(iw-iw/zoom)*${ side }':` +
			`y='(ih-ih/zoom)/2':d=${ frames }:s=${ size.width }x${ size.height }:fps=30,` +
			'format=yuv420p',
		'-frames:v',
		String( frames ),
		'-c:v',
		'libx264',
		'-preset',
		'veryfast',
		'-crf',
		'20',
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
		'-c:v',
		'libx264',
		'-preset',
		'veryfast',
		'-crf',
		'20',
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
		segments.push( still( i, length ) );
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
	segments.push( still( i, titleSeconds ) );
	const bounds = [ titleSeconds, ...cuts, length ];
	for ( let n = 0; n < bounds.length - 1; n++ ) {
		segments.push(
			shotOf(
				shots[ n % shots.length ],
				bounds[ n + 1 ] - bounds[ n ],
				n,
				`card-${ i }-shot-${ n }.mp4`
			)
		);
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

titleCard( 0, 'intro', broll );
for ( const [ i, { start, end } ] of chapters.entries() ) {
	const clip = cut( `chapter-${ i + 1 }.mp4`, [
		'-ss',
		start.toFixed( 2 ),
		'-to',
		end.toFixed( 2 ),
		'-i',
		webm,
	] );
	// The chapter's card is its title alone: Ana's line about it is spoken over the action
	// itself, where the recording marked it (`speak()` in the story), its end on the Enter.
	segments.push( still( i + 1 ) );
	clock += cardSeconds;
	const from = clock;
	const { voice } = chapters[ i ];
	if ( voice ) {
		say( from + Math.max( 0, voice.at - start ), takes( voice.name ) );
	}
	const result = takes( `chapter-${ i + 1 }-result` );
	if ( result.length ) {
		// Up to the result, the result held while she talks about it, then the rest.
		const at = Math.min( best( i ), seconds( clip ) - 0.1 );
		const before = cut( `chapter-${ i + 1 }-a.mp4`, [
			'-t',
			at.toFixed( 3 ),
			'-i',
			clip,
		] );
		segments.push( before );
		clock += seconds( before );
		const length = needs( result );
		segments.push(
			freeze( clip, at, length, `chapter-${ i + 1 }-result` )
		);
		say( clock, result );
		clock += length;
		const after = cut( `chapter-${ i + 1 }-b.mp4`, [
			'-ss',
			at.toFixed( 3 ),
			'-i',
			clip,
		] );
		segments.push( after );
		clock += seconds( after );
	} else {
		segments.push( clip );
		clock += seconds( clip );
	}
	cues.push( { from, to: clock, chapter: i } );
}
titleCard( cards.length - 1, 'outro', [ ...broll ].reverse() );
// The video ends on the closing card, alone and long enough to read its two addresses: under
// the outro it gives way to the footage after its title (D275).
const closingSeconds = 6;
segments.push( still( cards.length - 1, closingSeconds ) );
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
	if ( 'en' === videoLanguage ) {
		copyFileSync( file, join( site, `chapter-${ i + 1 }.webp` ) );
	}
}

const joined = concat( segments, 'joined.mp4' );
const joinedLength = seconds( joined );
const video = finish(
	joined,
	cues,
	'story',
	Object.entries( spoken )
		.filter( ( [ , placed ] ) => placed.length )
		.map( ( [ language, placed ] ) => ( {
			language: voices[ language ],
			file: voiceTrack( language, placed, joinedLength ),
		} ) )
);

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
	'story-30s'
);

const length = ( file ) => {
	const total = seconds( file );
	return `${ Math.floor( total / 60 ) }m${ String(
		Math.round( total % 60 )
	).padStart( 2, '0' ) }s`;
};
process.stdout.write(
	`${ video }: ${ length( video ) }, ${ chapters.length } chapters\n` +
		`${ teaser }: ${ length( teaser ) }\n` +
		'captions in English and pt_BR beside each, and inside as two tracks\n'
);
