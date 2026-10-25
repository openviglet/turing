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
