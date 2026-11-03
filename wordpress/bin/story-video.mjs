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
// A vertical cut (D280) is a chapter clip from the phone-width recording, 9:16, its cards laid
// out for it and its captions burned in above the band a vertical player draws its controls on.
const vertical = process.argv.includes( '--vertical' );
const recorded = join(
	root,
	'tests',
	'e2e',
	'artifacts',
	vertical ? 'story-vertical' : 'story'
);
const recordCommand = vertical
	? 'npm run e2e:story:vertical'
	: 'npm run e2e:story:record';
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
const chapterCount = 8;
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
const webm = join( recorded, 'story.webm' );
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
	join( root, 'demo', 'story.md' ),
	join( root, 'tests', 'e2e', 'story', 'shopper.story.mjs' ),
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
html, body { margin: 0; width: ${ size.width }px; height: ${ size.height }px; }
body { display: flex; flex-direction: column; justify-content: center;
	padding: 0 ${ vertical ? 90 : 160 }px;
	box-sizing: border-box; background: linear-gradient(160deg, #1F3B2D 0%, #2E5E73 100%);
	color: #F6F3EC; font-family: Source, sans-serif; }
small { font-size: ${ vertical ? 40 : 44 }px; letter-spacing: .08em; text-transform: uppercase;
	color: #C9D8B6; }
h1 { font-family: Barlow, sans-serif; font-size: ${ vertical ? 116 : 132 }px; line-height: 1.05;
	margin: 24px 0; }
p { font-size: ${ vertical ? 40 : 48 }px; margin: 0; color: #C9D8B6; overflow-wrap: anywhere; }`;

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
				fullIt: 'Veja a visita inteira',
				fullTitle: 'O vídeo completo e a loja de demonstração',
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
				fullIt: 'See the whole visit',
				fullTitle: 'The full video and the demo store',
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
// A clip's own two cards (D279): it opens on what the chapter shows, under the product's name,
// for a viewer who has seen none of the rest, and closes on where the full video and the
// Playground are; the site's WordPress page carries both.
if ( null !== clipChapter ) {
	const { number, name } = parts[ clipChapter - 1 ];
	cards.push(
		card( words.brand, words.name( number, name ), words.what ),
		card( words.fullIt, words.fullTitle, words.tryLine )
	);
}
for ( const [ i, html ] of cards.entries() ) {
	await page.setContent( html );
	await page.evaluate( () => document.fonts.ready );
	await page.screenshot( { path: join( out, 'parts', `card-${ i }.png` ) } );
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
			'pt-BR' === language ? portuguese[ number ] : title
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
		// Ducked under each line and up between them (D291), faded in and out, looped for as long
		// as the video runs.
		const fadeOut = Math.max( 0, total - 3 ).toFixed( 3 );
		inputs.push( '-stream_loop', '-1', '-i', music );
		graph.push(
			`[${ placed.length }:a]atrim=0:${ total.toFixed( 3 ) },` +
				`volume='${ duckExpression( placed ) }':eval=frame,afade=t=in:d=2,` +
				`afade=t=out:st=${ fadeOut }:d=3[m]`
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
				'-c:v',
				'libx264',
				'-preset',
				'veryfast',
				'-crf',
				'20',
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
// room kept around the element; and how long the move takes either way. A focus with less time
// than both moves and a short hold is left at the full frame.
const pushIn = { cap: 1.6, margin: 1.25, ease: 0.8, hold: 0.5 };

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
const follow = ( file, { start, end, focus = [] }, name ) => {
	if ( vertical ) {
		return file;
	}
	const windows = focus
		.map( ( { at, box }, n ) => ( {
			from: at - start,
			to: ( focus[ n + 1 ]?.at ?? end ) - start,
			zoom: Math.min(
				pushIn.cap,
				size.width / ( box.width * pushIn.margin ),
				size.height / ( box.height * pushIn.margin )
			),
			cx: Math.min( size.width, Math.max( 0, box.x + box.width / 2 ) ),
			cy: Math.min( size.height, Math.max( 0, box.y + box.height / 2 ) ),
		} ) )
		.filter(
			( w ) =>
				w.zoom >= 1.1 && w.to - w.from >= 2 * pushIn.ease + pushIn.hold
		);
	if ( ! windows.length ) {
		return file;
	}
	// Each window's weight at the frame's time, 0 to 1, eased (smoothstep).
	const time = 'in/30';
	const weight = ( { from, to } ) => {
		const ramp = `clip(min((${ time }-${ from.toFixed( 3 ) })/${
			pushIn.ease
		},(${ to.toFixed( 3 ) }-${ time })/${ pushIn.ease }),0,1)`;
		return `(${ ramp }*${ ramp }*(3-2*${ ramp }))`;
	};
	const sum = ( term ) => windows.map( term ).join( '+' );
	const zoom = `1+${ sum(
		( w ) => `${ ( w.zoom - 1 ).toFixed( 4 ) }*${ weight( w ) }`
	) }`;
	const centre = ( axis, half ) =>
		`${ half }+${ sum(
			( w ) => `${ ( w[ axis ] - half ).toFixed( 1 ) }*${ weight( w ) }`
		) }`;
	const followed = join( out, 'parts', name );
	ffmpeg( [
		'-i',
		file,
		'-vf',
		`zoompan=z='${ zoom }':` +
			`x='clip(${ centre( 'cx', size.width / 2 ) }-iw/zoom/2,0,iw-iw/zoom)':` +
			`y='clip(${ centre( 'cy', size.height / 2 ) }-ih/zoom/2,0,ih-ih/zoom)':` +
			`d=1:s=${ size.width }x${ size.height }:fps=30,format=yuv420p`,
		'-c:v',
		'libx264',
		'-preset',
		'veryfast',
		'-crf',
		'20',
		'-an',
		followed,
	] );
	return followed;
};

/**
 * One chapter as a clip of its own (D279): its card, the action with her line over it, the result
 * held while she talks about it, and the closing card. Where that runs past the limit, the stretch
 * between her line and the result, where nothing is said, plays faster until it fits.
 *
 * @param {number} i The chapter's index.
 */
const chapterClip = ( i ) => {
	const { start, end, voice } = chapters[ i ];
	const name = `story-chapter-${ i + 1 }${ vertical ? '-vertical' : '' }`;
	const clip = follow(
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
	);
	const at = Math.min( best( i ), seconds( clip ) - 0.1 );
	const lineTakes = voice ? takes( voice.name ) : [];
	const lineAt = voice ? Math.max( 0, voice.at - start ) : 0;
	// Played as recorded up to where her line ends.
	const spokenUntil = Math.min( at, lineAt + needs( lineTakes ) );
	const result = takes( `chapter-${ i + 1 }-result` );
	const held = result.length ? needs( result ) : cardSeconds;
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
	pieces.push( freeze( clip, at, held, `clip-${ i + 1 }-result` ) );
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

titleCard( 0, 'intro', broll );
for ( const [ i, { start, end } ] of chapters.entries() ) {
	const clip = follow(
		cut( `chapter-${ i + 1 }.mp4`, [
			'-ss',
			start.toFixed( 2 ),
			'-to',
			end.toFixed( 2 ),
			'-i',
			webm,
		] ),
		chapters[ i ],
		`chapter-${ i + 1 }-followed.mp4`
	);
	// The chapter's card is its title alone: Ana's line about it is spoken over the action
	// itself, where the recording marked it (`speak()` in the story), its end on the Enter.
	place( still( i + 1 ), cardPiece( i + 1 ) );
	clock += cardSeconds;
	const from = clock;
	const { voice } = chapters[ i ];
	if ( voice ) {
		say( from + Math.max( 0, voice.at - start ), takes( voice.name ) );
	}
	// Where the chapter's clip starts in the recording, as the cut above seeked it.
	const inRecording = Number( start.toFixed( 2 ) );
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
		place( before, { track: 'V1', source: webm, at: inRecording } );
		clock += seconds( before );
		const length = needs( result );
		const held = freeze( clip, at, length, `chapter-${ i + 1 }-result` );
		place( held, {
			track: 'V1',
			source: held.replace( /\.mp4$/, '.png' ),
			at: 0,
			still: true,
		} );
		say( clock, result );
		clock += length;
		const after = cut( `chapter-${ i + 1 }-b.mp4`, [
			'-ss',
			at.toFixed( 3 ),
			'-i',
			clip,
		] );
		place( after, {
			track: 'V1',
			source: webm,
			at: inRecording + Number( at.toFixed( 3 ) ),
		} );
		clock += seconds( after );
	} else {
		place( clip, { track: 'V1', source: webm, at: inRecording } );
		clock += seconds( clip );
	}
	cues.push( { from, to: clock, chapter: i } );
}
titleCard( cards.length - 1, 'outro', [ ...broll ].reverse() );
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
	if ( 'en' === videoLanguage ) {
		copyFileSync( file, join( site, `chapter-${ i + 1 }.webp` ) );
	}
}

// Dissolves (D291) where a card meets the recording or a shot, and between the shots: the
// still crossfades over the first or last half second of the clip beside it. The still goes on
// through the blend instead of overlapping the clip, so no piece moves and every take stays where
// it was placed. Inside a chapter the cuts stay hard: they are the product responding.
const dissolveSeconds = 0.5;

/**
 * A clip with a still crossfading over one end of it.
 *
 * @param {string}     file  The clip.
 * @param {string}     image The still.
 * @param {'in'|'out'} end   'out' fades the still out over the clip's start, 'in' fades it in
 *                           over the clip's end.
 * @param {number}     n     The piece's index, for the file's name.
 */
const blend = ( file, image, end, n ) => {
	const length = seconds( file );
	const fade =
		'out' === end
			? `fade=t=out:st=0:d=${ dissolveSeconds }:alpha=1`
			: `fade=t=in:st=${ ( length - dissolveSeconds ).toFixed(
					3
				) }:d=${ dissolveSeconds }:alpha=1`;
	const blended = join( out, 'parts', `dissolve-${ n }-${ end }.mp4` );
	ffmpeg( [
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
		'-c:v',
		'libx264',
		'-preset',
		'veryfast',
		'-crf',
		'20',
		'-an',
		blended,
	] );
	return blended;
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

const joinedFiles = videoPieces.map( ( piece ) => piece.file );
for ( let n = 0; n < videoPieces.length - 1; n++ ) {
	const [ from, to ] = [ videoPieces[ n ], videoPieces[ n + 1 ] ];
	if ( 'V1' === from.track && 'V1' === to.track ) {
		continue;
	}
	if ( to.still ) {
		// Into a card: the card fades in over the end of what plays before it.
		joinedFiles[ n ] = blend( joinedFiles[ n ], to.source, 'in', n );
	} else {
		// Out of a card or a shot: it fades out over the start of what comes next.
		const image = from.still
			? from.source
			: lastFrame( joinedFiles[ n ], n );
		joinedFiles[ n + 1 ] = blend(
			joinedFiles[ n + 1 ],
			image,
			'out',
			n + 1
		);
	}
}
const joined = concat( joinedFiles, 'joined.mp4' );
const joinedLength = seconds( joined );
const narrated = Object.entries( spoken ).filter(
	( [ , placed ] ) => placed.length
);
for ( const language of Object.keys( voices ) ) {
	chapterList( cues, language, 'story' );
}
const video = finish(
	joined,
	cues,
	'story',
	narrated.map( ( [ language, placed ] ) => ( {
		language: voices[ language ],
		file: voiceTrack( language, placed, joinedLength ),
	} ) ),
	narrated.map( ( [ language, placed ] ) => ( {
		language: voices[ language ],
		file: narrationCaptions( placed, language, 'story' ),
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
	const dir = join( out, `story${ videoSuffix }.project` );
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
				'-c:v',
				'libx264',
				'-preset',
				'veryfast',
				'-crf',
				'18',
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

	// The spine: V1 pieces directly, V2 and V3 pieces over a gap on their own lane.
	let offset = 0;
	const spine = videoPieces.map( ( piece ) => {
		const item = {
			offset,
			length: piece.length,
			start: 'V1' === piece.track ? piece.at : 0,
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
		const bed = asset( music, 'audio' );
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
		for ( let at = 0; at < total; at += bed.length ) {
			const until = Math.min( at + bed.length, total );
			const keys = [
				at,
				...turns.filter( ( t ) => t > at && t < until ),
				until,
			]
				.map(
					( t ) =>
						`<keyframe time="${ time( t - at ) }" value="${ db( t ) }"/>`
				)
				.join( '' );
			const item = holder( at );
			item.anchored.push(
				`<asset-clip ref="${ bed.id }" lane="-2" offset="${ time(
					local( item, at )
				) }" start="0s" duration="${ time(
					until - at
				) }" name="music" audioRole="music"><adjust-volume><param name="amount"><keyframeAnimation>${ keys }</keyframeAnimation></param></adjust-volume></asset-clip>`
			);
		}
	}
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

	const clip = ( item, lane ) => {
		const tag = 'image' === item.asset.kind ? 'video' : 'asset-clip';
		const at = lane ? ` lane="${ lane }" offset="0s"` : '';
		const inner = lane
			? ''
			: [ ...item.anchored, ...item.markers ].join( '' );
		return `<${ tag } ref="${ item.asset.id }" name="${ xml(
			item.asset.name
		) }"${ at }${
			lane ? '' : ` offset="${ time( item.offset ) }"`
		} start="${ time( item.start ) }" duration="${ time(
			item.length
		) }"${ inner ? `>${ inner }</${ tag }>` : '/>' }`;
	};
	const spineXml = spine
		.map( ( item ) => {
			if ( 'V1' === item.piece.track ) {
				return clip( item );
			}
			const lane = 'V2' === item.piece.track ? 1 : 2;
			return `<gap name="Gap" offset="${ time(
				item.offset
			) }" start="0s" duration="${ time( item.length ) }">${ clip(
				item,
				lane
			) }${ [ ...item.anchored, ...item.markers ].join( '' ) }</gap>`;
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
	const file = join( dir, `story${ videoSuffix }.fcpxml` );
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
	const srt = join( out, `story.${ videoLanguage }.srt` );
	const captions = join( dir, `story.${ videoLanguage }.srt` );
	if ( existsSync( srt ) ) {
		copyFileSync( srt, captions );
	}
	writeFileSync(
		join( dir, 'finish.lua' ),
		readFileSync( join( root, 'demo', 'resolve', 'finish.lua' ), 'utf8' )
			.replace( '__NAME__', `story${ videoSuffix }` )
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
	'story-30s'
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
