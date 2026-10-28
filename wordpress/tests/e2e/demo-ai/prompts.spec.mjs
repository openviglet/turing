// Turing's generative AI on the demo store (D198), asked real questions through the Site
// assistant on the showcase: each curated prompt (demo/theme/tarnfell/ai-prompts.json) in the
// demo's language is answered from the guide it names, and a question about something the
// store does not sell gets no invented price. Part of the live-LLM suite, never the free one.
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { expect, test } from '../fixtures.mjs';
import { record } from '../ai/helpers.mjs';
import { demoLocale } from '../demo/curated.mjs';

const { prompts, declined } = JSON.parse(
	readFileSync(
		join( 'demo', 'theme', 'tarnfell', 'ai-prompts.json' ),
		'utf8'
	)
);
const language = ( prompt ) => prompt.locale || 'en_US';

test.describe.configure( { mode: 'serial' } );

test.beforeEach( async ( { page } ) => {
	await page.goto( '/showcase/' );
	const assistant = page.locator( '.viglet-dumont-assistant' );
	const on = await assistant
		.waitFor( { state: 'visible', timeout: 15_000 } )
		.then( () => true )
		.catch( () => false );
	test.skip(
		! on,
		"the demo's Turing chat is off: start the stack with OPENAI_API_KEY and reindex the demo"
	);
	await assistant.getByRole( 'button' ).first().click();
} );

/**
 * Ask in the dialog and wait for the whole answer and the posts it used.
 *
 * @param {import('@playwright/test').Page} page     The showcase.
 * @param {string}                          question The question.
 */
async function ask( page, question ) {
	const dialog = page.getByRole( 'dialog' );
	await dialog.getByRole( 'textbox' ).fill( question );
	const started = Date.now();
	await dialog.locator( 'form button[type="submit"]' ).click();
	const answer = dialog
		.locator( '.viglet-dumont-assistant__message' )
		.nth( 1 );
	await expect(
		answer.locator( '.viglet-dumont-assistant__feedback button' ).first()
	).toBeVisible( { timeout: 90_000 } );
	return {
		text: (
			await answer
				.locator( '.viglet-dumont-assistant__text' )
				.textContent()
		).trim(),
		sources: await answer
			.locator( '.viglet-dumont-assistant__source a' )
			.allTextContents(),
		ms: Date.now() - started,
	};
}

for ( const prompt of prompts ) {
	test( `${ language( prompt ) } "${ prompt.question }" cites ${ prompt.cite }`, async ( {
		page,
	} ) => {
		test.skip(
			language( prompt ) !== demoLocale(),
			`the demo speaks ${ demoLocale() }`
		);
		const { text, sources, ms } = await ask( page, prompt.question );
		record( `demo: ${ prompt.question }`, prompt.question, text, ms );
		expect( sources ).toContain( prompt.cite );
		expect( text ).toMatch( new RegExp( prompt.mentions, 'i' ) );
	} );
}

test( `an off-catalog question gets no invented price: "${ declined.question }"`, async ( {
	page,
} ) => {
	test.skip( 'en_US' !== demoLocale(), `the demo speaks ${ demoLocale() }` );
	const { text, ms } = await ask( page, declined.question );
	record( 'demo: off catalog', declined.question, text, ms );
	expect( text, declined.why ).not.toMatch(
		/[$€£]\s?\d|R\$\s?\d|\d+([.,]\d+)?\s?(dollars|euros|pounds|reais)/i
	);
} );
