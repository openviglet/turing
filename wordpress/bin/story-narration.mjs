// Speaks the demo video's narration (D246, D261): one take per line and language, from
// demo/narration/script.json, bought from ElevenLabs through polyweave (`python -m polyweave
// sound.speak`), which holds the spend to the budget in polyweave.toml and records each take's
// provenance beside it. A take is bought only when it is missing or its line, voice or model
// changed, so running this again spends nothing. `npm run story:video` mixes the takes in.
//
//   npm run story:narration
//   npm run story:narration -- --language pt-BR    one language's takes only
//
// Needs ELEVENLABS_API_KEY and polyweave (`pip install polyweave-gamekit`). This loop is what
// polyweave's voice.lines does for a game's table; it goes when voice.lines takes a plain
// lines file (polyweave PW665).
import { execFileSync } from 'node:child_process';
import { mkdirSync } from 'node:fs';
import { dirname } from 'node:path';
import { current, lines, root, script, takeFile } from './narration-takes.mjs';

const only = process.argv.includes( '--language' )
	? process.argv[ process.argv.indexOf( '--language' ) + 1 ]
	: null;

let bought = 0;
for ( const [ name, texts ] of lines() ) {
	for ( const [ language, text ] of Object.entries( texts ) ) {
		if ( only && language !== only ) {
			continue;
		}
		const file = takeFile( language, name );
		const relative = `demo/narration/${ language }/${ name }.mp3`;
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
