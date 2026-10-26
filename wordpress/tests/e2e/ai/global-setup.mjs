// Before the live-LLM suite: the usual seed (global-setup.mjs), then the posts with known facts
// (fixtures/ai-seed.php), reindexed, and which AI features this stack can use, read with
// `wp dumont ai status` (D169) so each spec skips, with the reason, what is off.
import { writeFileSync } from 'node:fs';
import { join } from 'node:path';
import baseSetup, { seedFile, wp } from '../global-setup.mjs';
import { spent } from './cost.mjs';

export const aiFile = join( seedFile, '..', 'ai.json' );

export default async function aiSetup( config ) {
	await baseSetup( config );

	const printed = wp( [ 'eval-file', '/e2e/ai-seed.php' ] );
	const posts = JSON.parse( printed.slice( printed.indexOf( '{' ) ) );
	wp( [ 'dumont', 'reindex' ] );
	// Turing commits, then embeds for RAG; give it longer than the search seed needs.
	await new Promise( ( resolve ) => setTimeout( resolve, 15000 ) );

	// The command exits 1 when any feature is off, with the rows still printed.
	let status = '[]';
	try {
		status = wp( [ 'dumont', 'ai', 'status', '--format=json' ] );
	} catch ( error ) {
		status = String( error.stdout || '[]' );
	}
	const features = JSON.parse( status.slice( status.indexOf( '[' ) ) );

	// The ledger before the specs ask anything (cost.mjs): the teardown reports the difference.
	const before = await spent();

	writeFileSync( aiFile, JSON.stringify( { posts, features, before } ) );
}
