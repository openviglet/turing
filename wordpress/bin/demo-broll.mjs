// The demo video's B-roll (D261): free footage of the hills, fetched from Pixabay for what Ana
// says over the opening and closing, where the shop's own pages alone read as screenshots. Each
// clip is downloaded (Pixabay asks for local copies, never hotlinks) with a record beside it of
// where it came from, who made it and its licence, so the credits can name it. A clip already
// fetched is not fetched again. polyweave has no stock footage yet (PW672), so this is a script.
//
//   PIXABAY_API_KEY=... npm run demo:broll
//
// The clips are not committed (several MB each); their .json records are.
import {
	createWriteStream,
	existsSync,
	mkdirSync,
	writeFileSync,
} from 'node:fs';
import { dirname, join } from 'node:path';
import { Readable } from 'node:stream';
import { pipeline } from 'node:stream/promises';
import { fileURLToPath } from 'node:url';

const root = join( dirname( fileURLToPath( import.meta.url ) ), '..' );
const out = join( root, 'demo', 'broll' );
const key = process.env.PIXABAY_API_KEY;
if ( ! key ) {
	process.stderr.write(
		'demo-broll: set PIXABAY_API_KEY (pixabay.com/api/docs shows yours).\n'
	);
	process.exit( 1 );
}

// What the opening and closing are about: the hills a mountain-gear shop sells for.
const searches = [
	'hiking mountain trail',
	'hiker rain mountain',
	'camping tent mountains',
	'mountain lake landscape',
	'hiking boots trail',
	'mountain summit view',
];

mkdirSync( out, { recursive: true } );
let fetched = 0;
for ( const query of searches ) {
	const url = new URL( 'https://pixabay.com/api/videos/' );
	url.search = new URLSearchParams( {
		key,
		q: query,
		per_page: '5',
		safesearch: 'true',
		min_width: '1280',
	} );
	const response = await fetch( url );
	if ( ! response.ok ) {
		process.stderr.write(
			`demo-broll: Pixabay answered ${ response.status } for "${ query }".\n`
		);
		process.exit( 1 );
	}
	const hit = ( await response.json() ).hits?.find(
		( candidate ) => candidate.duration >= 8
	);
	if ( ! hit ) {
		process.stdout.write( `"${ query }": nothing long enough\n` );
		continue;
	}
	const file = join( out, `pixabay-${ hit.id }.mp4` );
	if ( ! existsSync( file ) ) {
		// The 1920 rendition where there is one, else the 1280 one.
		const rendition =
			hit.videos.large?.url && hit.videos.large.width >= 1920
				? hit.videos.large
				: hit.videos.medium;
		const video = await fetch( rendition.url );
		await pipeline(
			Readable.fromWeb( video.body ),
			createWriteStream( file )
		);
		fetched++;
	}
	writeFileSync(
		`${ file }.json`,
		JSON.stringify(
			{
				source: hit.pageURL,
				author: hit.user,
				licence:
					'Pixabay Content License (https://pixabay.com/service/license-summary/)',
				query,
				duration: hit.duration,
				tags: hit.tags,
			},
			null,
			'\t'
		) + '\n'
	);
	process.stdout.write(
		`"${ query }": pixabay-${ hit.id } by ${ hit.user }\n`
	);
}
process.stdout.write(
	fetched ? `${ fetched } clip(s) fetched\n` : 'every clip is here\n'
);
