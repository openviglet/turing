// Speaks the demo video's narration (D246): one take per chapter and language, from
// demo/narration/script.json, bought from ElevenLabs through polyweave (`python -m polyweave
// sound.speak`), which holds the spend to the budget in polyweave.toml and records each take's
// provenance beside it. A take is bought only when it is missing or its line, voice or model
// changed, so running this again spends nothing. `npm run story:video` mixes the takes in.
//
//   npm run story:narration
//
// Needs ELEVENLABS_API_KEY and polyweave (`pip install polyweave-gamekit`). This loop is what
// polyweave's voice.lines does for a game's table; it goes when voice.lines takes a plain
// lines file (polyweave PW665).
import { execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join( dirname( fileURLToPath( import.meta.url ) ), '..' );
const narration = join( root, 'demo', 'narration' );
const script = JSON.parse(
	readFileSync( join( narration, 'script.json' ), 'utf8' )
);

/**
 * Whether a take already says this line, in this voice and model.
 *
 * @param {string} file The take.
 * @param {string} text The line it must say.
 */
function current( file, text ) {
	const record = `${ file }.prov.json`;
	if ( ! existsSync( file ) || ! existsSync( record ) ) {
		return false;
	}
	const prov = JSON.parse( readFileSync( record, 'utf8' ) );
	return (
		prov.prompt === text &&
		prov.details?.voice === script.voice &&
		prov.engine?.model === script.model
	);
}

let bought = 0;
for ( const [ chapter, lines ] of Object.entries( script.chapters ) ) {
	for ( const [ language, text ] of Object.entries( lines ) ) {
		const relative = `demo/narration/${ language }/chapter-${ chapter }.mp3`;
		const file = join( root, relative );
		if ( current( file, text ) ) {
			continue;
		}
		mkdirSync( dirname( file ), { recursive: true } );
		const answer = JSON.parse(
			execFileSync(
				'python',
				[
					'-m',
					'polyweave',
					'sound.speak',
					'--root',
					root,
					'--text',
					text,
					'--voice',
					script.voice,
					'--model',
					script.model,
					'--rung',
					'paid',
					'--out',
					relative,
					'--json',
				],
				{ encoding: 'utf8' }
			)
		);
		bought++;
		process.stdout.write(
			`${ relative }: ${ answer.measured.duration.toFixed( 1 ) }s\n`
		);
	}
}
process.stdout.write(
	bought ? `${ bought } take(s) bought\n` : 'every take is current\n'
);
