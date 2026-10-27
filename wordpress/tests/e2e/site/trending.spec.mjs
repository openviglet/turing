// Popular searches under the box (D202): the week's searches that find something become chips,
// one that finds nothing never does, and a chip searches for itself. The page is made here and
// removed again, so no other spec's screenshots see it in the theme's menu.
import { authFile } from '../../../playwright.config.mjs';
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
			// Out of the index: its text would be one more result for the other specs.
			'--meta_input={"_viglet_dumont_exclude":"1"}',
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

test( 'spellings of one want show as one chip, under the most searched', async ( {
	page,
} ) => {
	// D213: the grouping the daily job stores, fixed here rather than asked of a model. Two
	// chips that both answer stand in for "raincoat" and "rain jacket".
	const chips = page.getByRole( 'navigation', { name: 'Popular searches' } );
	await page.goto( `/?page_id=${ pageId }` );
	await expect(
		chips.getByRole( 'link', { name: 'seminar' } )
	).toBeVisible();
	const [ first, second ] = (
		await chips.getByRole( 'link' ).allTextContents()
	)
		.map( ( name ) => name.trim() )
		.filter( ( name ) => [ 'seminar', 'workshop' ].includes( name ) );
	expect( second ).toBeTruthy();

	const fresh = () =>
		wp( [ 'transient', 'delete', 'viglet_dumont_trending' ] );
	try {
		wp( [
			'option',
			'update',
			'viglet_dumont_trending_groups',
			JSON.stringify( [ [ 'seminar', 'workshop' ] ] ),
			'--format=json',
		] );
		wp( [
			'option',
			'patch',
			'update',
			'viglet_dumont_settings',
			'generative_ai',
			'1',
		] );
		fresh();
		await page.reload();
		await expect(
			chips.getByRole( 'link', { name: first, exact: true } )
		).toBeVisible();
		await expect(
			chips.getByRole( 'link', { name: second, exact: true } )
		).toHaveCount( 0 );
	} finally {
		wp( [
			'option',
			'patch',
			'update',
			'viglet_dumont_settings',
			'generative_ai',
			'0',
		] );
		wp( [ 'option', 'delete', 'viglet_dumont_trending_groups' ] );
		fresh();
	}
} );

test.describe( 'as the site owner', () => {
	test.use( { storageState: authFile } );

	test( 'a word blocked in Settings never shows as a chip', async ( {
		page,
	} ) => {
		const settings = '/wp-admin/admin.php?page=viglet-dumont';
		const save = async ( words ) => {
			await page.goto( settings );
			await page.click( '.viglet-dumont-tab[data-tab="search"]' );
			await page.getByLabel( 'Never show' ).fill( words );
			await page.getByRole( 'button', { name: 'Save Settings' } ).click();
			await expect( page.getByLabel( 'Never show' ) ).toHaveValue(
				words
			);
		};
		try {
			await save( 'Seminar' );
			await page.goto( `/?page_id=${ pageId }` );
			const chips = page.getByRole( 'navigation', {
				name: 'Popular searches',
			} );
			await expect(
				chips.getByRole( 'link', { name: 'workshop' } )
			).toBeVisible();
			await expect(
				chips.getByRole( 'link', { name: 'seminar' } )
			).toHaveCount( 0 );
		} finally {
			await save( '' );
		}
	} );
} );
