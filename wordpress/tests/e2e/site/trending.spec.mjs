// Popular searches under the box (D202): the week's searches that find something become chips,
// one that finds nothing never does, and a chip searches for itself. The page is made here and
// removed again, so no other spec's screenshots see it in the theme's menu.
import { wp } from '../global-setup.mjs';
import { expect, test } from '../fixtures.mjs';

const turingURL = process.env.TURING_URL || 'http://localhost:2700';
let pageId = 0;

test.beforeAll( async ( { request } ) => {
	// Three searches each this week: one Turing answers, one it cannot.
	for ( const q of [ 'seminar', 'zzpopularnothing' ] ) {
		for ( let i = 0; i < 3; i++ ) {
			await request.get(
				`${ turingURL }/api/sn/Sample/search?q=${ q }&_setlocale=en_US`
			);
		}
	}
	pageId = Number(
		wp( [
			'post',
			'create',
			'--post_type=page',
			'--post_status=publish',
			'--post_title=Popular',
			'--post_content=<!-- wp:viglet-dumont/search {"showTrending":true} /-->',
			'--porcelain',
		] ).trim()
	);
	// The chips as the log reads now, not as an earlier run cached them.
	wp( [ 'transient', 'delete', 'viglet_dumont_trending' ] );
} );

test.afterAll( () => {
	if ( pageId ) {
		wp( [ 'post', 'delete', String( pageId ), '--force' ] );
	}
} );

test( 'popular searches that find something become chips, and a chip searches for itself', async ( {
	page,
	look,
} ) => {
	await page.goto( `/?page_id=${ pageId }` );
	const chips = page.getByRole( 'navigation', { name: 'Popular searches' } );
	await expect(
		chips.getByRole( 'link', { name: 'seminar' } )
	).toBeVisible();
	await expect(
		chips.getByRole( 'link', { name: 'zzpopularnothing' } )
	).toHaveCount( 0 );
	await look( 'trending' );

	await chips.getByRole( 'link', { name: 'seminar' } ).click();
	await expect(
		page.locator( '.viglet-dumont-search__title a' ).first()
	).toHaveText( /Seminar on/ );
	// Once a search is on the page, the chips step aside.
	await expect( chips ).toBeHidden();
} );
