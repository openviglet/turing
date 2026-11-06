// Shared by the live-LLM specs: what the global setup found, a way to ask Turing's chat from
// the visitor's browser and time it, and a record of every answer for the reviewer.
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { aiFile } from './global-setup.mjs';

let state = null;

/**
 * What the global setup found: the seeded post ids and the AI features' rows. Read on first
 * use, since Playwright loads the specs before the global setup has written it.
 */
export function ai() {
	state = state || JSON.parse( readFileSync( aiFile, 'utf8' ) );
	return state;
}

/**
 * Whether D169's probe reports a feature on, and why not when it is off.
 *
 * @param {string} name answer, chat, nl_filters or copilot.
 * @return {{on: boolean, reason: string}} The row.
 */
export function feature( name ) {
	const row = ai().features.find( ( f ) => f.feature === name );
	return {
		on: !! row?.on,
		reason: row
			? [ row.reason, row.fix ].filter( Boolean ).join( ' ' )
			: 'not reported',
	};
}

const answersDir = join( 'tests', 'e2e', 'artifacts', 'ai' );
const answers = [];

/**
 * Keep what the model said, beside the screens, so a reviewer reads it.
 *
 * @param {string} name     The test.
 * @param {string} question What was asked.
 * @param {string} answer   What came back.
 * @param {number} ms       How long it took.
 */
export function record( name, question, answer, ms ) {
	answers.push( { name, question, answer, ms } );
	mkdirSync( answersDir, { recursive: true } );
	writeFileSync(
		join( answersDir, 'answers.json' ),
		JSON.stringify( answers, null, 2 )
	);
}

/**
 * The times recorded so far, in milliseconds.
 */
export const times = () => answers.map( ( a ) => a.ms );

/**
 * Ask the SN site's chat from the page, the way a visitor's browser would: Turing's public URL
 * and site come from the plugin's own config on the page.
 *
 * @param {import('@playwright/test').Page} page     A page of the site.
 * @param {string}                          question The question.
 * @return {Promise<{text: string, ms: number}>} The answer and how long it took.
 */
export function askChat( page, question ) {
	return page.evaluate( async ( q ) => {
		const data = JSON.parse(
			document.getElementById(
				'wp-script-module-data-@wordpress/interactivity'
			)?.textContent || '{}'
		);
		const { url, site, locale } = data.config?.[ 'viglet-dumont' ] || {};
		const started = window.performance.now();
		const response = await window.fetch(
			`${ url }/api/sn/${ encodeURIComponent(
				site
			) }/chat?q=${ encodeURIComponent( q ) }&_setlocale=${ locale }`
		);
		const body = await response.json();
		return {
			text: String( body.text || '' ),
			ms: window.performance.now() - started,
		};
	}, question );
}
