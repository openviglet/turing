// After the live-LLM suite: delete the posts with known facts again and send the deletions to
// Turing now, so the default suite finds the stack as its own seed left it (its page counts
// and screenshots depend on that). Runs even when a spec failed.
import { existsSync, readFileSync } from 'node:fs';
import { wp } from '../global-setup.mjs';
import { aiFile } from './global-setup.mjs';

export default async function aiTeardown() {
	if ( ! existsSync( aiFile ) ) {
		return;
	}
	const { posts } = JSON.parse( readFileSync( aiFile, 'utf8' ) );
	const ids = Object.values( posts || {} ).map( String );
	if ( ids.length ) {
		wp( [ 'post', 'delete', ...ids, '--force' ] );
		wp( [ 'dumont', 'queue', 'run' ] );
	}
}
