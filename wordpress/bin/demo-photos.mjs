// The demo store's photos (D265): one realistic photo per product and per guide, bought from
// Ideogram through polyweave (`python -m polyweave picture.buy`), which holds the spend to the
// budget in polyweave.toml and records each photo's provenance beside it. The seed imports a
// photo where one exists and draws its own picture where none does (demo/seed/images.php), so
// a clone without them still seeds. A photo is bought only when it is missing or its prompt
// changed: running this again spends nothing.
//
//   npm run demo:photos                       every product and guide
//   npm run demo:photos -- --only <slug>      one, to look at before buying the rest
//
// It reads the catalog from the demo's Turing (the English SN site the seed indexed), so the
// demo stack must be up and seeded in English once. Needs IDEOGRAM_API_KEY and polyweave.
import { execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join( dirname( fileURLToPath( import.meta.url ) ), '..' );
const turing = process.env.TURING_URL || 'http://localhost:2700';
const site = process.env.SN_DEMO_SITE || 'Demo';
const arg = ( name ) =>
	process.argv.includes( name )
		? process.argv[ process.argv.indexOf( name ) + 1 ]
		: null;
const only = arg( '--only' );

const plain = ( text ) =>
	String( text ?? '' )
		.replace( /<[^>]+>/g, '' )
		.replace( /\s+/g, ' ' )
		.trim();

// The colour names the seed uses, as a photographer would say them.
const colours = {
	Sky: 'sky blue',
	Moss: 'moss green',
	Ember: 'burnt orange',
	Slate: 'slate grey',
	Glacier: 'pale icy blue',
	Sand: 'sand beige',
	Black: 'black',
	Brown: 'brown',
	Red: 'red',
	Blue: 'blue',
	Green: 'green',
	Grey: 'grey',
	Navy: 'navy blue',
};

// Clothing is photographed laid flat: a studio shot of a jacket alone came back filled out by
// an invisible body and floating, which reads as strange. Gear is shown where it is used.
const clothing = [
	'Waterproof jackets',
	'Insulated jackets',
	'Fleece and midlayers',
];

/**
 * What a photo shows: clothing as a flat lay, gear on the hill, a guide as an outdoor scene. No
 * text, no logos and no real brand, so the photos can ship with the repository's demo.
 *
 * @param {Object} doc A document of the index.
 */
function prompt( doc ) {
	const title = plain( doc.title );
	const about = plain( doc.abstract );
	if ( 'product' === doc.type ) {
		const colour = [ doc.pa_color ].flat().filter( Boolean )[ 0 ];
		const tint = colour
			? `, in ${ colours[ colour ] ?? colour.toLowerCase() }`
			: '';
		const category = [ doc.product_cat ].flat()[ 0 ];
		const scene =
			clothing.includes( category ) || /trousers/i.test( title )
				? 'Flat lay product photograph: the garment neatly laid flat on a pale natural ' +
					'wooden table, photographed from directly above, sleeves and hood arranged ' +
					'naturally, soft even daylight. Nothing inside it: not worn, not on a mannequin, ' +
					'not floating.'
				: 'Product photograph outdoors: the item resting on a mossy rock beside a mountain ' +
					'trail in green fells, soft natural daylight, shallow depth of field with the ' +
					'item in sharp focus and the landscape softly blurred behind. Nobody in the frame.';
		return (
			`${ scene } The item is ${ title }${ tint }. ${ about } Photorealistic, high ` +
			'detail. No text, no labels, no logos, no brand names.'
		);
	}
	return (
		`Editorial outdoor photograph for a hiking magazine article titled "${ title }". ${ about } ` +
		'Natural light in the green fells and lakes of northern England, realistic, candid, ' +
		'warm and inviting, shot on a full-frame camera. No text, no logos, no brand names.'
	);
}

/**
 * Whether a photo already shows this prompt.
 *
 * @param {string} file The photo.
 * @param {string} text What it must show.
 */
function current( file, text ) {
	const record = `${ file }.prov.json`;
	return (
		existsSync( file ) &&
		existsSync( record ) &&
		JSON.parse( readFileSync( record, 'utf8' ) ).prompt === text
	);
}

const response = await fetch(
	`${ turing }/api/sn/${ encodeURIComponent(
		site
	) }/search?q=*&rows=200&_setlocale=en_US`
);
const docs = ( await response.json() ).results.document
	.map( ( d ) => d.fields )
	.filter( ( d ) => [ 'product', 'post' ].includes( d.type ) );

/**
 * Run a polyweave purchase and return its answer, which polyweave prints on stdout whether it
 * bought or refused: a refusal (402, credits run out) exits non-zero and is still an answer.
 *
 * @param {string}   command The command.
 * @param {string[]} args    Its arguments.
 */
function buy( command, args ) {
	try {
		return execFileSync( command, args, { encoding: 'utf8' } );
	} catch ( error ) {
		return (
			error.stdout ||
			'{"refused":{"code":"unknown","message":"no answer"}}'
		);
	}
}

let bought = 0;
for ( const doc of docs ) {
	const slug = new URL( doc.url ).pathname
		.split( '/' )
		.filter( Boolean )
		.pop();
	// WordPress's own first post is no part of the shop (setup.php removes it, D264).
	if ( [ 'hello-world', 'sample-page' ].includes( slug ) ) {
		continue;
	}
	if ( only && slug !== only ) {
		continue;
	}
	const kind = 'product' === doc.type ? 'product' : 'guide';
	const relative = `demo/photos/${ kind }/${ slug }.png`;
	const file = join( root, relative );
	const webp = file.replace( /\.png$/, '.webp' );
	const text = prompt( doc );
	if ( current( file, text ) && existsSync( webp ) ) {
		continue;
	}
	mkdirSync( dirname( file ), { recursive: true } );
	const answer = JSON.parse(
		buy( 'python', [
			'-m',
			'polyweave',
			'picture.buy',
			'--root',
			root,
			'--prompt',
			text,
			'--model',
			'4.0',
			// A photo keeps its background: polyweave cuts one out by default (PW667).
			'--transparent',
			'false',
			'--aspect_ratio',
			'product' === kind ? '1x1' : '16x9',
			'--out',
			relative,
			'--json',
		] )
	);
	if ( answer.refused ) {
		process.stderr.write(
			`${ relative }: ${ answer.refused.code }: ${ answer.refused.message }\n`
		);
		process.exit( 1 );
	}
	// The copy the repository keeps and the seed reads, at a web shop's size: 800 px on its long
	// side, WebP. The bought PNG (2 to 5 MB) stays out of git.
	execFileSync( 'ffmpeg', [
		'-y',
		'-loglevel',
		'error',
		'-i',
		file,
		'-vf',
		"scale='min(800,iw)':'min(800,ih)':force_original_aspect_ratio=decrease",
		'-quality',
		'80',
		webp,
	] );
	bought++;
	process.stdout.write( `${ relative }\n` );
}
process.stdout.write(
	bought ? `${ bought } photo(s) bought\n` : 'every photo is current\n'
);
