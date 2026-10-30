// Assembles the demo video (D204) from the recorded story: `npm run e2e:story:record` leaves
// story.webm and chapters.json in tests/e2e/artifacts/story/, and this cuts one clip per
// chapter, puts a title card in the demo's brand before each, an intro and an outro around
// them, and the chapter names as captions in English and pt_BR. Every frame between the cards
// is the product running. Needs ffmpeg on the PATH.
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
import { execFileSync } from 'node:child_process';
import {
	copyFileSync,
	existsSync,
	mkdirSync,
	readFileSync,
	statSync,
	writeFileSync,
} from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { chromium } from '@playwright/test';

const root = join( dirname( fileURLToPath( import.meta.url ) ), '..' );
const recorded = join( root, 'tests', 'e2e', 'artifacts', 'story' );
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
	4: 'Compara três jaquetas lado a lado',
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
const cards = [
	card(
		'Viglet Dumont for WordPress',
		'One shopper, one visit',
		'From a vague need to a paid-for cart, on Turing search and AI.'
	),
	...parts.map( ( { number, name } ) =>
		card( `Chapter ${ number }`, name, 'Tarnfell, the demo store' )
	),
	card(
		'Try it',
		'openviglet.github.io/dumont',
		'Open the demo store, or install the plugin from the release.'
	),
];
for ( const [ i, html ] of cards.entries() ) {
	await page.setContent( html );
	await page.evaluate( () => document.fonts.ready );
	await page.screenshot( { path: join( out, 'parts', `card-${ i }.png` ) } );
}
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
const still = ( i ) => {
	const file = join( out, 'parts', `card-${ i }.mp4` );
	ffmpeg( [
		'-loop',
		'1',
		'-t',
		String( cardSeconds ),
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
 */
const finish = ( joined, cues, name ) => {
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
		'-map',
		'0',
		'-map',
		'1',
		'-map',
		'2',
		'-c',
		'copy',
		'-c:s',
		'mov_text',
		'-metadata:s:s:0',
		'language=eng',
		'-metadata:s:s:1',
		'language=por',
		file,
	] );
	return file;
};

// The video: intro, then each chapter's card and clip, then the outro.
const segments = [ still( 0 ) ];
const cues = [];
let clock = cardSeconds;
for ( const [ i, { start, end } ] of chapters.entries() ) {
	segments.push( still( i + 1 ) );
	clock += cardSeconds;
	const clip = cut( `chapter-${ i + 1 }.mp4`, [
		'-ss',
		start.toFixed( 2 ),
		'-to',
		end.toFixed( 2 ),
		'-i',
		webm,
	] );
	segments.push( clip );
	const length = seconds( clip );
	cues.push( { from: clock, to: clock + length, chapter: i } );
	clock += length;
}
segments.push( still( cards.length - 1 ) );

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
	copyFileSync( file, join( site, `chapter-${ i + 1 }.webp` ) );
}

const video = finish( concat( segments, 'joined.mp4' ), cues, 'story' );

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
