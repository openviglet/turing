// The grounded answer panel (a block with "Answer questions" on): it asks Turing only for a
// question, draws the answer only with the sources it cites, and records the visitor's
// verdict. Turing's answer stream is replayed here from a recording of its events; the run
// against a real LLM is the `ai` project's (D174). Desktop only (playwright.config.mjs).
import { expect, test } from '../fixtures.mjs';

const panel = ( page ) => page.locator( '.viglet-dumont-search__answer' );
const answerRoute = '**/api/sn/*/answer?**';

/**
 * A Server-Sent Events body as Turing's answer endpoint streams it.
 *
 * @param {Array<[string, string]>} events Event name and data, in order.
 */
const stream = ( events ) =>
	events
		.map( ( [ name, data ] ) => `event:${ name }\ndata:${ data }\n\n` )
		.join( '' );

const cited = stream( [
	[
		'sources',
		JSON.stringify( [
			{
				sourceId: 'localhost:post:9',
				title: 'Workshop on facets',
				url: 'http://localhost:8080/?p=9',
				footnote: 1,
			},
		] ),
	],
	[ 'token', 'Facets are the subject of ' ],
	[ 'token', 'the Workshop on facets [1].' ],
	[
		'citations',
		JSON.stringify( [
			{
				rank: 1,
				title: 'Workshop on facets',
				url: 'http://localhost:8080/?p=9',
			},
		] ),
	],
] );

test( 'an answer written in Markdown shows as lists and emphasis, and its markup stays text', async ( {
	page,
	seed,
} ) => {
	// D247. A newline inside an event's data travels as one `data:` line per line.
	const markdown = [
		'Two sessions cover it:',
		'',
		'- **Workshop on facets**, in March',
		'- *Seminar on search*',
		'',
		'<script>window.answerRan = 1</script> See [1].',
	].join( '\n' );
	const body =
		`event:sources\ndata:${ JSON.stringify( [
			{
				sourceId: 'localhost:post:9',
				title: 'Workshop on facets',
				url: 'http://localhost:8080/?p=9',
				footnote: 1,
			},
		] ) }\n\n` +
		`event:token\n${ markdown
			.split( '\n' )
			.map( ( line ) => `data:${ line }` )
			.join( '\n' ) }\n\n`;
	await page.route( answerRoute, ( route ) =>
		route.fulfill( {
			status: 200,
			contentType: 'text/event-stream',
			body,
		} )
	);
	await page.goto(
		`/?page_id=${ seed.askPageId }&q=${ encodeURIComponent(
			'which sessions cover facets?'
		) }`
	);
	const text = panel( page ).locator( '.viglet-dumont-search__answer-text' );
	await expect( text.locator( 'li' ) ).toHaveText( [
		'Workshop on facets, in March',
		'Seminar on search',
	] );
	await expect( text.locator( 'strong' ) ).toHaveText( 'Workshop on facets' );
	await expect( text.locator( 'em' ) ).toHaveText( 'Seminar on search' );
	await expect( text.locator( 'p' ).last() ).toHaveText(
		'<script>window.answerRan = 1</script> See [1].'
	);
	await expect( text.locator( 'script' ) ).toHaveCount( 0 );
	expect( await page.evaluate( () => window.answerRan ) ).toBeUndefined();
} );

test( 'a question gets the cited answer above the results, and a verdict is recorded', async ( {
	page,
	seed,
	look,
	axe,
} ) => {
	await page.route( answerRoute, ( route ) =>
		route.fulfill( {
			status: 200,
			contentType: 'text/event-stream',
			body: cited,
		} )
	);
	const signals = [];
	await page.route( '**/api/sn/*/answer/signal', ( route ) => {
		signals.push( route.request().postDataJSON() );
		return route.fulfill( { status: 204 } );
	} );

	await page.goto(
		`/?page_id=${ seed.askPageId }&q=${ encodeURIComponent(
			'which workshop covers facets?'
		) }`
	);
	await expect( panel( page ) ).toBeVisible();
	await expect(
		panel( page ).locator( '.viglet-dumont-search__answer-text' )
	).toHaveText( 'Facets are the subject of the Workshop on facets [1].' );
	await expect( panel( page ).getByRole( 'heading' ) ).toHaveText(
		'Answer generated from this site'
	);
	const source = panel( page ).getByRole( 'link', {
		name: 'Workshop on facets',
	} );
	await expect( source ).toHaveAttribute(
		'href',
		'http://localhost:8080/?p=9'
	);
	await look( 'search-block-answer' );
	await axe( 'the answer panel', {
		include: '.viglet-dumont-search__answer',
	} );

	await panel( page ).getByRole( 'button', { name: 'Yes' } ).click();
	await expect( panel( page ).getByRole( 'status' ) ).toHaveText(
		'Thank you.'
	);
	await expect
		.poll( () => signals.map( ( s ) => s.signal ) )
		.toContain( 'THUMBS_UP' );
} );

test( 'a lookup is not a question: Turing is not asked and no panel shows', async ( {
	page,
	seed,
} ) => {
	let asked = 0;
	await page.route( answerRoute, ( route ) => {
		asked++;
		return route.fulfill( {
			status: 200,
			contentType: 'text/event-stream',
			body: cited,
		} );
	} );
	await page.goto( `/?page_id=${ seed.askPageId }&q=workshop` );
	await expect( page.locator( '.viglet-dumont-search__summary' ) ).toHaveText(
		'4 results'
	);
	expect( asked ).toBe( 0 );
	await expect( panel( page ) ).toBeHidden();
} );

test( 'an answer that cites nothing is never drawn', async ( {
	page,
	seed,
} ) => {
	await page.route( answerRoute, ( route ) =>
		route.fulfill( {
			status: 200,
			contentType: 'text/event-stream',
			body: stream( [
				[ 'sources', '[]' ],
				[ 'token', 'Something the site never said.' ],
			] ),
		} )
	);
	await page.goto(
		`/?page_id=${ seed.askPageId }&q=${ encodeURIComponent(
			'what is the meaning of life?'
		) }`
	);
	await expect( page.locator( '.viglet-dumont-search__summary' ) ).toHaveText(
		/results|No results/
	);
	await page.waitForLoadState( 'networkidle' );
	await expect( panel( page ) ).toBeHidden();
} );

test( 'a site whose AI is off shows no panel and no error', async ( {
	page,
	seed,
} ) => {
	// Turing's own answer when AI is off, replayed (D191): a stack started with
	// OPENAI_API_KEY answers through its Default AI Agent, binding or not (Turing T622).
	await page.route( answerRoute, ( route ) =>
		route.fulfill( {
			status: 200,
			contentType: 'text/event-stream',
			body: stream( [
				[
					'error',
					JSON.stringify( {
						message:
							'AI is not enabled for site: Sample (NO_AGENT)',
					} ),
				],
			] ),
		} )
	);
	await page.goto(
		`/?page_id=${ seed.askPageId }&q=${ encodeURIComponent(
			'which workshop covers facets?'
		) }`
	);
	await expect( page.locator( '.viglet-dumont-search__summary' ) ).toHaveText(
		/results/
	);
	await page.waitForLoadState( 'networkidle' );
	await expect( panel( page ) ).toBeHidden();
} );
