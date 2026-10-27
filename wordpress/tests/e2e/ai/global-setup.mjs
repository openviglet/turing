// Before the live-LLM suite: the usual seed (global-setup.mjs), then the posts with known facts
// (fixtures/ai-seed.php), reindexed, and which AI features this stack can use, read with
// `wp dumont ai status` (D169) so each spec skips, with the reason, what is off.
import { writeFileSync } from 'node:fs';
import { join } from 'node:path';
import baseSetup, { seedFile, wp } from '../global-setup.mjs';
import { setting, spent } from './cost.mjs';

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

	await audiencePersona();

	// The ledger before the specs ask anything (cost.mjs): the teardown reports the difference.
	const before = await spent();

	writeFileSync( aiFile, JSON.stringify( { posts, features, before } ) );
}

const turingURL = process.env.TURING_URL || 'http://localhost:2700';
const persona = {
	name: 'Grant applicant',
	description:
		'A researcher applying for funding, who wants deadlines and eligibility stated plainly.',
	personaKind: 'AUDIENCE',
	enabled: 1,
};

/**
 * One audience persona on the agent the SN site answers through, for the editor's audience
 * fit (D171). Kept between runs: found by name, so a second run adds nothing.
 */
async function audiencePersona() {
	const turing = ( path, init = {} ) =>
		fetch( `${ turingURL }${ path }`, {
			...init,
			headers: {
				Key: setting( 'api_key' ),
				'Content-Type': 'application/json',
			},
		} ).then( ( response ) => {
			if ( ! response.ok ) {
				throw new Error(
					`Turing ${ path } answered ${ response.status }`
				);
			}
			return response.json();
		} );
	const site = encodeURIComponent( setting( 'site_name' ) );
	const listed = await turing( `/api/sn/${ site }/personas` );
	if ( listed.some( ( p ) => p.name === persona.name ) ) {
		return;
	}
	const kept = ( await turing( '/api/persona' ) ).find(
		( p ) => p.name === persona.name
	);
	const id =
		kept?.id ??
		(
			await turing( '/api/persona', {
				method: 'POST',
				body: JSON.stringify( persona ),
			} )
		).id;
	// The site answers through the Default AI Agent (D186), the stack's only one.
	const [ agent ] = await turing( '/api/ai-agent' );
	await turing( `/api/ai-agent/${ agent.id }`, {
		method: 'PUT',
		body: JSON.stringify( {
			...agent,
			personas: [ ...( agent.personas || [] ), { id } ],
		} ),
	} );
}
