// After the live-LLM suite: delete the posts with known facts again and send the deletions to
// Turing now, so the default suite finds the stack as its own seed left it (its page counts
// and screenshots depend on that). Runs even when a spec failed. Then what the run spent,
// from Turing's ledger (cost.mjs): written beside the answers, printed, and a run over budget
// fails.
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { wp } from '../global-setup.mjs';
import { budget, difference, overBudget, spent } from './cost.mjs';
import { aiFile } from './global-setup.mjs';

export default async function aiTeardown() {
	if ( ! existsSync( aiFile ) ) {
		return;
	}
	const { posts, before } = JSON.parse( readFileSync( aiFile, 'utf8' ) );
	const ids = Object.values( posts || {} ).map( String );
	if ( ids.length ) {
		wp( [ 'post', 'delete', ...ids, '--force' ] );
		wp( [ 'dumont', 'queue', 'run' ] );
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
