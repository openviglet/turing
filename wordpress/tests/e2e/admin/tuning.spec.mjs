// Search tuning (D172): a synonym added in wp-admin makes the site's search find through it,
// and a promoted post heads the results for its search. Both are removed again from the
// screen; afterEach also clears whatever a failed run left behind.
import { wp } from '../global-setup.mjs';
import { expect, test } from '../fixtures.mjs';

const screen = '/wp-admin/admin.php?page=viglet-dumont-tuning';

test.afterEach( () => {
	wp( [
		'eval',
		'$t = new Viglet_Dumont_Tuning(); $all = $t->all();' +
			' foreach ( $all["synonyms"] as $s ) { if ( $s["ours"] ) { $t->remove_synonym( $s["id"] ); } }' +
			' foreach ( $all["spotlights"] as $s ) { if ( $s["ours"] ) { $t->remove_spotlight( $s["id"] ); } }',
	] );
} );

const summary = ( page ) => page.locator( '.viglet-dumont-search__summary' );

test( 'a synonym added here makes the search find through it, until it is removed', async ( {
	page,
	seed,
	look,
	axe,
} ) => {
	await page.goto( screen );
	// Watch "talk" first, so the change shows as before and after.
	const tryIt = page
		.locator( '.components-card' )
		.filter( { hasText: 'Try it' } );
	await tryIt.getByLabel( 'A search to watch' ).fill( 'talk' );
	await tryIt.getByRole( 'button', { name: 'Try', exact: true } ).click();
	await expect( tryIt.getByRole( 'heading', { name: 'Now' } ) ).toBeVisible();

	const card = page
		.locator( '.components-card' )
		.filter( { hasText: 'Synonyms' } );
	await card
		.getByLabel( 'Words that mean the same, separated by commas' )
		.fill( 'lecture, talk' );
	await card.getByRole( 'button', { name: 'Add synonym' } ).click();
	await expect( card.locator( 'tbody tr' ) ).toHaveText( [
		/lecture = talk/,
	] );
	await expect(
		tryIt.getByRole( 'heading', { name: 'Before' } )
	).toBeVisible();
	await expect( tryIt.locator( 'ol' ).last().locator( 'li' ) ).toHaveText( [
		/Lecture on/,
		/Lecture on/,
	] );
	await look( 'tuning' );
	await axe( 'Search tuning', { include: '#wpbody-content' } );

	await page.goto( `/?page_id=${ seed.searchPageId }&q=talk` );
	await expect( summary( page ) ).toHaveText( '2 results' );
	await expect( page.locator( '.viglet-dumont-search__title a' ) ).toHaveText(
		[ /Lecture on/, /Lecture on/ ]
	);

	await page.goto( screen );
	await card.getByRole( 'button', { name: 'Remove lecture, talk' } ).click();
	await expect( card.locator( 'tbody tr' ) ).toHaveCount( 0 );
	await page.goto( `/?page_id=${ seed.searchPageId }&q=talk` );
	await expect( summary( page ) ).toHaveText(
		'No results found. Try different keywords.'
	);
} );

test( 'a post promoted for a search heads its results as featured', async ( {
	page,
	seed,
	look,
} ) => {
	await page.goto( screen );
	const card = page
		.locator( '.components-card' )
		.filter( { hasText: 'Promoted results' } );
	await card.getByLabel( 'When visitors search for' ).fill( 'workshop' );
	await card.getByLabel( 'Find a post to show first' ).fill( 'Docker' );
	await card
		.getByRole( 'button', { name: 'Show Course on Docker first' } )
		.click();
	await card.getByRole( 'button', { name: 'Promote' } ).click();
	await expect( card.locator( 'tbody tr' ) ).toHaveText( [
		/workshop.*Course on Docker/,
	] );

	await page.goto( `/?page_id=${ seed.searchPageId }&q=workshop` );
	const featured = page.locator( '.viglet-dumont-search__spotlights li' );
	await expect( featured ).toHaveCount( 1 );
	await expect( featured ).toContainText( 'Featured' );
	await expect( featured.locator( 'a' ) ).toHaveText( 'Course on Docker' );
	await look( 'search-block-featured' );
	// The promoted post comes before the results, which are unchanged.
	await expect( summary( page ) ).toHaveText( '4 results' );

	await page.goto( screen );
	await card
		.getByRole( 'button', { name: 'Remove the promotion for workshop' } )
		.click();
	await expect( card.locator( 'tbody tr' ) ).toHaveCount( 0 );
} );

test( "suggestions come from Turing's mining, without wildcard or half-typed variants", async ( {
	page,
} ) => {
	await page.goto( screen );
	const card = page
		.locator( '.components-card' )
		.filter( { hasText: 'Synonyms' } );
	await card
		.getByRole( 'button', { name: 'Suggest from recent searches' } )
		.click();
	// This stack's log holds the as-you-type searches, which mining turns into "work* = work"
	// (Turing T1125): none of that may be offered.
	await expect(
		card
			.getByText( 'No suggestions from recent searches.' )
			.or( card.getByRole( 'list', { name: 'Suggested synonyms' } ) )
	).toBeVisible();
	await expect( card.getByText( /\*/ ) ).toHaveCount( 0 );
} );

test( 'the editor\'s "Promote for a search" opens the form with the post chosen', async ( {
	page,
	seed,
} ) => {
	await page.goto( `/wp-admin/post.php?post=${ seed.postId }&action=edit` );
	const guide = page
		.getByRole( 'dialog' )
		.getByRole( 'button', { name: 'Close' } );
	if ( await guide.isVisible( { timeout: 5000 } ).catch( () => false ) ) {
		await guide.click();
	}
	const promote = page.getByRole( 'link', { name: 'Promote for a search…' } );
	if ( ! ( await promote.isVisible().catch( () => false ) ) ) {
		await page
			.getByRole( 'button', { name: 'Settings', exact: true } )
			.click();
		await page.getByRole( 'tab', { name: 'Post' } ).click();
	}
	await expect( promote ).toHaveAttribute(
		'href',
		new RegExp( `promote=${ seed.postId }$` )
	);

	await page.goto(
		`/wp-admin/admin.php?page=viglet-dumont-tuning&promote=${ seed.postId }`
	);
	const card = page
		.locator( '.components-card' )
		.filter( { hasText: 'Promoted results' } );
	await expect(
		card.getByRole( 'list', { name: 'Shown first' } ).locator( 'li' )
	).toHaveText( [ /Workshop on search/ ] );
} );
