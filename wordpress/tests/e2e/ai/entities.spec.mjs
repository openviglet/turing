// People, organisations and places from Turing's entity extraction (D310, Turing T1228)
// against a real LLM: a post naming a person by a variant, an institution and two places is
// found under the person's preferred name and under a place, through the facets the plugin
// offers. Skipped, with the reason, while answers are off (no LLM). The fixture puts the site
// back afterwards.
import { expect, test } from '../fixtures.mjs';
import { wp } from '../global-setup.mjs';
import { feature } from './helpers.mjs';

/**
 * The JSON line the fixture printed last.
 *
 * @param {string} step `run` or `cleanup`.
 */
const live = ( step ) =>
	JSON.parse(
		wp( [ 'eval-file', '/e2e/entities-live.php', step ] )
			.trim()
			.split( /\r?\n/ )
			.pop()
	);

test.beforeEach( () => {
	const answer = feature( 'answer' );
	test.skip( ! answer.on, `no language model in Turing: ${ answer.reason }` );
} );

test.afterAll( () => {
	try {
		live( 'cleanup' );
	} catch {}
} );

test( 'a person named by a variant and a place become facets the post is found under', async () => {
	test.setTimeout( 240_000 );

	const { doc, entities, finds } = live( 'run' );

	// The text says "Lima, Ana Paula"; the thesaurus folds it into the preferred name.
	expect( entities.entity_person ).toContain( 'Ana Paula Lima' );
	expect( entities.entity_person ).not.toContain( 'Lima, Ana Paula' );
	expect( entities.entity_place ).toEqual(
		expect.arrayContaining( [ 'Recife' ] )
	);
	expect( finds[ 'entity_person:Ana Paula Lima' ] ).toContain( doc );
	expect( finds[ 'entity_place:Recife' ] ).toContain( doc );
} );
