// A site that also searches a sibling site of the same SN site (D303): the main site lists the
// second one in "Also search these sites", so its search block, its theme's search page, its
// instant search and its Related block with `sites: all` find the second site's documents,
// marked with the label and linked to the second site; the second site, which lists nobody,
// still finds only its own. Runs only with E2E_MULTISITE=1. The setting and the Related block
// put on each site's first post are taken back afterwards.
import { readFileSync } from 'node:fs';
import { expect, test } from '../fixtures.mjs';
import { networkSeedFile, setSetting, wp } from '../global-setup.mjs';

const [ main, second ] = JSON.parse( readFileSync( networkSeedFile, 'utf8' ) );
const only = 'Only on the second site';
const label = 'Second site';

/**
 * The last line WP-CLI printed for a site, so a notice before it is no answer.
 *
 * @param {string}   url  The site.
 * @param {string[]} args The wp arguments.
 */
const ask = ( url, args ) => {
	const lines = wp( [ `--url=${ url }`, ...args ], 'cli-ms' )
		.trim()
		.split( /\r?\n/ );
	return lines[ lines.length - 1 ].trim();
};

const secondKey = ask( second.url, [
	'eval',
	'echo ( new Viglet_Turing_Indexer() )->site_key();',
] );

/**
 * Put a Related block reading every site in scope at the end of a site's first post ("Hello
 * world!", which both sites have and Turing finds alike), or take it back. Written straight
 * to the database, so the post is not indexed again; its neighbours are asked afresh.
 *
 * @param {string}  url The site.
 * @param {boolean} on  Add the block, or remove it.
 */
const relatedAllSites = ( url, on ) =>
	wp(
		[
			`--url=${ url }`,
			'eval',
			`global $wpdb; $block = '<!-- wp:viglet-turing/related {"sites":"all","count":6} /-->'; ` +
				`$content = str_replace( "\\n\\n" . $block, '', (string) get_post_field( 'post_content', 1 ) ); ` +
				`$wpdb->update( $wpdb->posts, array( 'post_content' => ${
					on ? '$content . "\\n\\n" . $block' : '$content'
				} ), array( 'ID' => 1 ) ); clean_post_cache( 1 ); ` +
				`$wpdb->query( "DELETE FROM {$wpdb->options} WHERE option_name LIKE '\\\\_transient%viglet\\\\_turing\\\\_related\\\\_%'" );`,
		],
		'cli-ms'
	);

test.beforeAll( () => {
	setSetting(
		'search_site_keys',
		`${ secondKey } | ${ label } | ${ second.url }`,
		[ `--url=${ main.url }` ],
		'cli-ms'
	);
	relatedAllSites( main.url, true );
	relatedAllSites( second.url, true );
} );

test.afterAll( () => {
	setSetting( 'search_site_keys', null, [ `--url=${ main.url }` ], 'cli-ms' );
	relatedAllSites( main.url, false );
	relatedAllSites( second.url, false );
} );

const block = ( page ) => page.locator( '.viglet-turing-search' );
const results = ( page ) =>
	block( page ).locator( '.viglet-turing-search__results li' );
const summary = ( page ) =>
	block( page ).locator( '.viglet-turing-search__summary' );
const titles = ( page ) =>
	results( page ).locator( '.viglet-turing-search__title a' );

test( 'the main site’s search block finds the second site’s post, marked and linked there', async ( {
	page,
	look,
} ) => {
	await page.goto(
		`${ main.url }?page_id=${ main.searchPageId }&q=${ encodeURIComponent(
			only
		) }`
	);
	const card = results( page ).filter( { hasText: only } ).first();
	await expect( card ).toBeVisible();
	await expect( card.locator( '.viglet-turing-search__origin' ) ).toHaveText(
		label
	);
	await expect(
		card.locator( '.viglet-turing-search__title a' )
	).toHaveAttribute( 'href', new RegExp( `^${ second.url }` ) );
	await look( 'network-sibling-block' );

	// Its own posts still answer, and none of them carries a badge.
	await page.goto(
		`${ main.url }?page_id=${ main.searchPageId }&q=workshop`
	);
	await expect( summary( page ) ).toHaveText( '4 results' );
	await expect(
		results( page ).locator( '.viglet-turing-search__origin:visible' )
	).toHaveCount( 0 );
} );

test( 'the site facet narrows the main site’s search to the second site and back', async ( {
	page,
} ) => {
	await page.goto( `${ main.url }?page_id=${ main.searchPageId }&q=*` );
	const site = block( page )
		.locator( '.viglet-turing-search__facet' )
		.filter( { has: page.locator( 'summary', { hasText: 'Site' } ) } );
	const theirs = site
		.locator( 'label' )
		.filter( { hasText: label } )
		.locator( 'input' );
	await expect( theirs ).toBeVisible();
	const all = await summary( page ).textContent();

	await theirs.click();
	await expect( summary( page ) ).not.toHaveText( all );
	await expect( theirs ).toBeChecked();
	await expect( titles( page ) ).toContainText( [ only ] );
	// Every result is the second site's now.
	const shown = await results( page ).count();
	await expect(
		results( page ).locator( '.viglet-turing-search__origin:visible' )
	).toHaveCount( shown );
	const chip = block( page ).locator( '.viglet-turing-search__chip' );
	await expect( chip ).toContainText( `Site: ${ label }` );
	expect( new URL( page.url() ).searchParams.getAll( 'fq[]' ) ).toEqual( [
		`site_key:${ secondKey }`,
	] );

	await chip.click();
	await expect( summary( page ) ).toHaveText( all );
} );

test( 'the main site’s theme search lists the second site’s post, linked there', async ( {
	page,
} ) => {
	await page.goto( `${ main.url }?s=${ encodeURIComponent( only ) }` );
	const link = page.locator( 'main' ).getByRole( 'link', { name: only } );
	await expect( link.first() ).toBeVisible();
	await expect( link.first() ).toHaveAttribute(
		'href',
		new RegExp( `^${ second.url }` )
	);
} );

test( 'the main site’s instant search answers with the second site’s post, named by its site', async ( {
	page,
} ) => {
	await page.goto( `${ main.url }?page_id=${ main.corePageId }` );
	await page.keyboard.press( 'Control+K' );
	const overlay = page.locator( 'dialog.viglet-turing-overlay' );
	await expect( overlay ).toBeVisible();
	await page.keyboard.type( 'Only on the second' );
	const option = overlay.getByRole( 'option' ).filter( { hasText: only } );
	await expect( option ).toBeVisible();
	await expect(
		option.locator( '.viglet-turing-overlay__meta' )
	).toContainText( label );
} );

test( 'the main site’s Related block with every site lists the second site’s post', async ( {
	page,
} ) => {
	await page.goto( `${ main.url }?p=1` );
	const remote = page.locator( '.viglet-turing-related__item--remote' );
	await expect( remote.first() ).toBeVisible();
	await expect(
		remote.first().locator( '.viglet-turing-related__origin' )
	).toHaveText( label );
	await expect( remote.first().locator( 'a' ) ).toHaveAttribute(
		'href',
		new RegExp( `^${ second.url }` )
	);
} );

test( 'the second site, which lists nobody, still finds only its own, in every search', async ( {
	page,
} ) => {
	// The block.
	await page.goto(
		`${ second.url }?page_id=${ second.searchPageId }&q=workshop`
	);
	await expect( summary( page ) ).toHaveText( /results|No results/ );
	expect(
		( await titles( page ).allTextContents() ).filter( ( t ) =>
			t.startsWith( 'Workshop on' )
		)
	).toEqual( [] );
	await page.goto( `${ second.url }?page_id=${ second.searchPageId }&q=*` );
	await expect( titles( page ) ).toContainText( [ only ] );
	await expect(
		block( page )
			.locator( '.viglet-turing-search__facet summary' )
			.filter( { hasText: /^Site/ } )
	).toHaveCount( 0 );

	// The theme's search page.
	await page.goto( `${ second.url }?s=workshop` );
	await expect(
		page.locator( 'main' ).getByRole( 'link', { name: /^Workshop on/ } )
	).toHaveCount( 0 );

	// The instant search.
	await page.goto( `${ second.url }?page_id=${ second.corePageId }` );
	await page.keyboard.press( 'Control+K' );
	const overlay = page.locator( 'dialog.viglet-turing-overlay' );
	await expect( overlay ).toBeVisible();
	await page.keyboard.type( 'workshop' );
	await expect(
		overlay.locator( '.viglet-turing-overlay__status' )
	).not.toBeEmpty();
	await expect(
		overlay.getByRole( 'option' ).filter( { hasText: /^Workshop on/ } )
	).toHaveCount( 0 );

	// Related, asked for every site: the second site has no sibling to add.
	await page.goto( `${ second.url }?p=1` );
	await expect(
		page.locator( '.viglet-turing-related__item--remote' )
	).toHaveCount( 0 );
} );
