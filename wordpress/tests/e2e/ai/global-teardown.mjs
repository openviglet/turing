// After the live-LLM suite: delete the posts with known facts again and send the deletions to
// Turing now, and wait until they have left its index, so the default suite finds the stack
// as its own seed left it (its page counts and screenshots depend on that). Runs even when a spec failed. Then what the run spent,
// from Turing's ledger (cost.mjs): written beside the answers, printed, and a run over budget
// fails.
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { wp } from '../global-setup.mjs';
import { budget, difference, overBudget, setting, spent } from './cost.mjs';
import { aiFile } from './global-setup.mjs';

export default async function aiTeardown() {
	if ( ! existsSync( aiFile ) ) {
		return;
	}
	const { posts, before } = JSON.parse( readFileSync( aiFile, 'utf8' ) );
	const ids = Object.values( posts || {} ).map( String );
	if ( ids.length ) {
		wp( [ 'post', 'delete', ...ids, '--force' ] );
		drainQueue();
		await leaveIndex( ids );
	}

	if ( ! before ) {
		return;
	}
	const run = difference( before, await spent() );
	const dir = join( 'tests', 'e2e', 'artifacts', 'ai' );
	mkdirSync( dir, { recursive: true } );
	writeFileSync(
		join( dir, 'cost.json' ),
		JSON.stringify( { ...run, budget }, null, 2 )
	);
	// eslint-disable-next-line no-console -- the run's bill, read at the end of the output.
	console.log(
		`LLM spend: ${ run.tokens } tokens, $${ run.usd }, ${ run.requests } calls (budget ${ budget.tokens } tokens, $${ budget.usd })`
	);
	const over = overBudget( run );
	if ( over.length ) {
		throw new Error( `The live-LLM run spent ${ over.join( ' and ' ) }.` );
	}
}

const turingURL = process.env.TURING_URL || 'http://localhost:2700';

/**
 * Wait until Turing no longer answers with these posts (D174). The queue is sent before
 * Turing commits the deletions, and a default run started right after would count them.
 *
 * @param {string[]} ids The deleted posts' ids.
 */
async function leaveIndex( ids ) {
	const site = encodeURIComponent( setting( 'site_name' ) );
	const locale = encodeURIComponent( setting( 'locale' ) );
	const gone = ( id ) => ( doc ) =>
		! String( doc.fields?.id || '' ).endsWith( `:post:${ id }` );
	for ( let tries = 0; tries < 30; tries++ ) {
		const response = await fetch(
			`${ turingURL }/api/sn/${ site }/search?q=*&rows=500&_setlocale=${ locale }`
		);
		const docs = ( await response.json() ).results?.document || [];
		if ( ids.every( ( id ) => docs.every( gone( id ) ) ) ) {
			return;
		}
		await new Promise( ( resolve ) => setTimeout( resolve, 2000 ) );
	}
	throw new Error(
		'The seeded posts were still in the index a minute later.'
	);
}

/**
 * Send the queued deletions now. The cron container's worker may hold the queue's lock, and
 * `queue run` then exits 1 ("Another worker is sending the queue"); that worker is sending
 * the same jobs, so wait for the queue to empty rather than fail a green run.
 */
function drainQueue() {
	for ( let tries = 0; tries < 30; tries++ ) {
		try {
			wp( [ 'dumont', 'queue', 'run' ] );
			return;
		} catch ( error ) {
			if (
				! /Another worker/.test(
					String( error.stderr || error.stdout || error )
				)
			) {
				throw error;
			}
		}
		Atomics.wait(
			new Int32Array( new SharedArrayBuffer( 4 ) ),
			0,
			0,
			2000
		);
	}
	throw new Error( 'The queue stayed locked for a minute.' );
}
