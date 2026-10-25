// Related content from Turing's similar-documents search (D177): after a post (the seed puts
// it after every post), its siblings and never itself, in the HTML the server sends, with a
// click reported like a search result's; a page Turing finds nothing like gets no box.
import { expect, test } from '../fixtures.mjs';

const related = ( page ) => page.locator( '.wp-block-viglet-dumont-related' );

test( 'a workshop post is followed by the other workshops, drawn on the server', async ( {
	page,
	request,
	seed,
	look,
	axe,
} ) => {
	await page.goto( `/?p=${ seed.postId }` );
	const title = ( await page.title() ).split( ' – ' )[ 0 ];
	const box = related( page );
	await expect(
		box.getByRole( 'heading', { name: 'Related posts' } )
	).toBeVisible();

	const titles = box.locator( '.viglet-dumont-related__title' );
	await expect( titles ).toHaveCount( 3 );
	const shown = ( await titles.allTextContents() ).map( ( t ) => t.trim() );
	expect( shown ).not.toContain( title );
	// Alike by shared words: the other workshops lead (a page about search may join them).
	expect(
		shown.filter( ( t ) => /^Workshop on /.test( t ) ).length
	).toBeGreaterThanOrEqual( 2 );
	await look( 'related' );
	await axe( 'the related content block', {
		include: '.wp-block-viglet-dumont-related',
	} );

	// In the HTML itself, for crawlers and visitors without JavaScript.
	const html = await ( await request.get( `/?p=${ seed.postId }` ) ).text();
	expect( html ).toContain( 'viglet-dumont-related__title' );
	expect( html.match( /viglet-dumont-related__title/g ) ).toHaveLength( 3 );
} );

test( 'opening a related post reports it to Turing with its rank', async ( {
	page,
	seed,
} ) => {
	await page.goto( `/?p=${ seed.postId }` );
	const links = related( page ).locator( '.viglet-dumont-related__title a' );
	await expect( links ).toHaveCount( 3 );

	const sent = page.waitForRequest( /\/api\/sn\/[^/]+\/click$/ );
	const second = ( await links.nth( 1 ).textContent() ).trim();
	await links.nth( 1 ).click();
	await expect( page ).toHaveTitle( new RegExp( `^${ second }` ) );
	const click = ( await sent ).postDataJSON();
	expect( click ).toMatchObject( { term: '', position: 2 } );
	expect( click.documentId ).toMatch( /:post:\d+$/ );
} );

test( 'a page Turing finds nothing like gets no related box at all', async ( {
	page,
	seed,
} ) => {
	await page.goto( `/?page_id=${ seed.lonelyPageId }` );
	await expect( page.getByText( 'Marsupials of Rottnest.' ) ).toBeVisible();
	await expect( related( page ) ).toHaveCount( 0 );
} );
