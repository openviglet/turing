// Assembles the demo video (D204) from the recorded story: `npm run e2e:story:record` leaves
// story.webm and chapters.json in tests/e2e/artifacts/story/, and this cuts one clip per
// chapter, puts a title card in the demo's brand before each, an intro and an outro around
// them, and the chapter names as English captions. Every frame between the cards is the
// product running. Needs ffmpeg on the PATH.
//
//   npm run e2e:story:record && npm run story:video
//
// Writes tests/e2e/artifacts/video/story.mp4 and story.en.srt; rendered files are release
// assets, never committed. Refuses a recording that is missing a chapter or older than the
// story it was recorded from, so a re-record after a UI change is never skipped by accident.
import { execFileSync } from 'node:child_process';
import {
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

const segments = [ still( 0 ) ];
const captions = [];
let clock = cardSeconds;
for ( const [ i, { start, end } ] of chapters.entries() ) {
	segments.push( still( i + 1 ) );
	clock += cardSeconds;
	const clip = join( out, 'parts', `chapter-${ i + 1 }.mp4` );
	ffmpeg( [
		'-ss',
		start.toFixed( 2 ),
		'-to',
		end.toFixed( 2 ),
		'-i',
		webm,
		...encode,
		clip,
	] );
	segments.push( clip );
	const length = seconds( clip );
	captions.push( {
		from: clock,
		to: clock + length,
		text: `${ parts[ i ].number }. ${ parts[ i ].name }`,
	} );
	clock += length;
}
segments.push( still( cards.length - 1 ) );

const list = join( out, 'parts', 'list.txt' );
writeFileSync(
	list,
	segments
		.map( ( file ) => `file '${ file.replace( /\\/g, '/' ) }'` )
		.join( '\n' )
);
const joined = join( out, 'parts', 'joined.mp4' );
ffmpeg( [ '-f', 'concat', '-safe', '0', '-i', list, '-c', 'copy', joined ] );

const stamp = ( t ) => {
	const ms = Math.round( t * 1000 );
	const pad = ( n, w = 2 ) => String( n ).padStart( w, '0' );
	return `${ pad( Math.floor( ms / 3600000 ) ) }:${ pad(
		Math.floor( ms / 60000 ) % 60
	) }:${ pad( Math.floor( ms / 1000 ) % 60 ) },${ pad( ms % 1000, 3 ) }`;
};
const srt = join( out, 'story.en.srt' );
writeFileSync(
	srt,
	captions
		.map(
			( c, i ) =>
				`${ i + 1 }\n${ stamp( c.from ) } --> ${ stamp( c.to ) }\n${
					c.text
				}\n`
		)
		.join( '\n' )
);
const video = join( out, 'story.mp4' );
ffmpeg( [
	'-i',
	joined,
	'-i',
	srt,
	'-c',
	'copy',
	'-c:s',
	'mov_text',
	'-metadata:s:s:0',
	'language=eng',
	video,
] );

const total = seconds( video );
process.stdout.write(
	`${ video }: ${ Math.floor( total / 60 ) }m${ String(
		Math.round( total % 60 )
	).padStart(
		2,
		'0'
	) }s, ${ chapters.length } chapters, captions in ${ srt }\n`
);
