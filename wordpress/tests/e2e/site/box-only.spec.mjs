// A search block holding only its search box (D208), as a hero places it: Enter, or a picked
// suggestion, takes the visitor to the full search page with the query. The page is made here
// and removed again, so no other spec's screenshots see it in the theme's menu.
import { wp } from '../global-setup.mjs';
import { expect, test } from '../fixtures.mjs';

let pageId = 0;

test.beforeAll( () => {
	pageId = Number(
		wp( [
			'post',
			'create',
			'--post_type=page',
			'--post_status=publish',
			'--post_title=Hero',
			'--post_content=<!-- wp:viglet-dumont/search --><!-- wp:viglet-dumont/search-box /--><!-- /wp:viglet-dumont/search -->',
			// Out of the index: its text would be one more result for the other specs.
			'--meta_input={"_viglet_dumont_exclude":"1"}',
			'--porcelain',
		] ).trim()
	);
} );

test.afterAll( () => {
	if ( pageId ) {
		wp( [ 'post', 'delete', String( pageId ), '--force' ] );
	}
} );

const box = ( page ) =>
	page.locator( '.viglet-dumont-search' ).getByRole( 'combobox' );

test( 'Enter in a box-only search block opens the search page with the results', async ( {
	page,
	seed,
} ) => {
	await page.goto( `/?page_id=${ pageId }` );
	await box( page ).fill( 'workshop' );
	await box( page ).press( 'Enter' );

	await expect( page ).toHaveURL(
		new RegExp( `page_id=${ seed.searchPageId }.*q=workshop` )
	);
	await expect(
		page.locator( '.viglet-dumont-search__results li' ).first()
	).toContainText( /workshop/i );
} );

test( 'a suggestion picked in a box-only block opens the search page for it', async ( {
	page,
	seed,
} ) => {
	await page.goto( `/?page_id=${ pageId }` );
	await box( page ).pressSequentially( 'work' );
	const option = page.getByRole( 'option' ).first();
	await expect( option ).toBeVisible();
	const picked = ( await option.textContent() ).trim();
	await option.click();

	await expect( page ).toHaveURL(
		new RegExp(
			`page_id=${ seed.searchPageId }.*q=${ encodeURIComponent(
				picked
			).replace( /%20/g, '\\+' ) }`
		)
	);
} );
