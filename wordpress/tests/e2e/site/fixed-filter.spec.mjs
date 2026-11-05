// A search block with a fixed filter (D306) searches only inside it: a box set to always
// filter by type:page never lists a post, on the server's first paint and once its script
// searches, and the filter is no chip a visitor could remove.
import { expect, test } from '../fixtures.mjs';
import { wp } from '../global-setup.mjs';

let pageId = '';
let boxId = '';

/**
 * Publish a page and return its id.
 *
 * @param {string} title   Its title.
 * @param {string} content Its block markup.
 */
const publish = ( title, content ) =>
	wp( [
		'post',
		'create',
		'--post_type=page',
		'--post_status=publish',
		`--post_title=${ title }`,
		`--post_content=${ content }`,
		'--porcelain',
	] )
		.trim()
		.split( /\r?\n/ )
		.pop()
		.trim();

test.beforeAll( () => {
	pageId = publish(
		'Pages only',
		'<!-- wp:viglet-dumont/search {"filters":["type:page"]} /-->'
	);
	// A box with no results of its own (D208), scoped: its query goes to the search page.
	boxId = publish(
		'Search the pages',
		'<!-- wp:viglet-dumont/search {"filters":["type:page"]} --><!-- wp:viglet-dumont/search-box /--><!-- /wp:viglet-dumont/search -->'
	);
} );

test.afterAll( () => {
	wp( [ 'post', 'delete', pageId, boxId, '--force' ] );
} );

test( 'a box-only block takes its scope to the search page, said there and left by a link', async ( {
	page,
} ) => {
	await page.goto( `/?page_id=${ boxId }` );
	await expect(
		page.locator( '.viglet-dumont-search__scope' )
	).toContainText( 'In Pages' );
	const box = page.locator( '.viglet-dumont-search input[type="search"]' );
	await box.fill( 'search' );
	await box.press( 'Enter' );

	await expect( page ).toHaveURL( /[?&]scope(%5B%5D|\[\])=type(%3A|:)page/ );
	await expect(
		page.locator( '.viglet-dumont-search__results li' ).first()
	).toBeVisible();
	expect( ( await types( page ) ).every( ( t ) => 'Page' === t ) ).toBe(
		true
	);
	const scope = page.locator( '.viglet-dumont-search__scope' );
	await expect( scope ).toContainText( 'In Pages' );

	// The whole site: the same query, the scope gone, posts back.
	await scope.getByRole( 'link', { name: 'Search the whole site' } ).click();
	await expect( page ).not.toHaveURL( /scope/ );
	await expect( page ).toHaveURL( /[?&]q=search/ );
	await expect(
		page.locator( '.viglet-dumont-search__results li' ).first()
	).toBeVisible();
	expect( await types( page ) ).toContain( 'Post' );
} );

const types = ( page ) =>
	page
		.locator( '.viglet-dumont-search__results .viglet-dumont-search__type' )
		.allTextContents();

test( 'without its script, the first page lists no post', async ( {
	browser,
	baseURL,
} ) => {
	const context = await browser.newContext( {
		baseURL,
		javaScriptEnabled: false,
	} );
	const page = await context.newPage();
	await page.goto( `/?page_id=${ pageId }&q=search` );
	await expect(
		page.locator( '.viglet-dumont-search__results li' ).first()
	).toBeAttached();
	const found = await types( page );
	expect( found.length ).toBeGreaterThan( 0 );
	expect( found.every( ( type ) => 'Page' === type ) ).toBe( true );
	await context.close();
} );

test( 'its script searches only pages, and shows the scope as no chip', async ( {
	page,
} ) => {
	await page.goto( `/?page_id=${ pageId }` );
	const box = page.locator( '.viglet-dumont-search input[type="search"]' );
	await box.fill( 'workshop' );
	await box.press( 'Enter' );
	// Every seeded workshop is a post: inside type:page, none is found.
	await expect( page.locator( '.viglet-dumont-search__summary' ) ).toHaveText(
		/No results/
	);

	await box.fill( 'search' );
	await box.press( 'Enter' );
	await expect(
		page.locator( '.viglet-dumont-search__results li' ).first()
	).toBeVisible();
	expect( ( await types( page ) ).every( ( t ) => 'Page' === t ) ).toBe(
		true
	);
	await expect( page.locator( '.viglet-dumont-search__chip' ) ).toHaveCount(
		0
	);
	expect( new URL( page.url() ).searchParams.getAll( 'fq[]' ) ).toEqual( [] );
} );
