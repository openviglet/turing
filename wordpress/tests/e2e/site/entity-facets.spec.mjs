// The people and places Turing finds in a post (D310) are facets the search block offers by
// their labels, and picking one finds the post. The local Turing runs no language model, so
// the seeded post is sent once with its `entity_person` and `entity_place` values already in
// it, as Turing writes them after reading the text. The settings, the entity facets and the
// post's document are put back afterwards. Desktop only: it changes the site's manifest.
import { expect, test } from '../fixtures.mjs';
import { wp } from '../global-setup.mjs';

/**
 * The last line WP-CLI printed, so a notice before it is no answer.
 *
 * @param {string[]} args The wp arguments.
 */
const ask = ( args ) => wp( args ).trim().split( /\r?\n/ ).pop().trim();

const person = 'Marta Vilar';
const place = 'Porto Claro';

/** The options this spec changes, as they were. */
const saved = {};
let title = '';

/**
 * PHP that sends the seeded post's own document with the entity values in it.
 *
 * @param {number} id The post.
 */
const withEntities = ( id ) => `
$settings = viglet_turing_get_settings();
$item     = ( new Viglet_Turing_Indexer( $settings ) )->build_create_item( get_post( ${ id } ) );
$item['attributes']['entity_person'] = array( '${ person }' );
$item['attributes']['entity_place']  = array( '${ place }' );
$result = ( new Viglet_Turing_API_Client( $settings ) )->import( array( $item ) );
echo $result['success'] ? 'ok' : 'failed: ' . $result['message'];
`;

const options = [
	'viglet_turing_settings',
	'viglet_turing_entities',
	'viglet_turing_provisioned',
];

test.beforeAll( ( { seed }, testInfo ) => {
	if ( 'site' !== testInfo.project.name ) {
		return;
	}
	for ( const name of options ) {
		try {
			saved[ name ] = ask( [ 'option', 'get', name, '--format=json' ] );
		} catch {
			saved[ name ] = null;
		}
	}
	title = ask( [
		'post',
		'get',
		String( seed.postId ),
		'--field=post_title',
	] );
	wp( [
		'eval',
		`$s = get_option( 'viglet_turing_settings' );
		$s['generative_ai'] = 1;
		$s['entity_extraction'] = 1;
		update_option( 'viglet_turing_settings', $s );
		update_option( 'viglet_turing_entities', array( 'organization' => array( 'offered' => false ) ) );`,
	] );
	// The manifest declares the two fields, labelled, before a document carries them.
	expect(
		ask( [
			'eval',
			'$r = ( new Viglet_Turing_Indexer() )->provision( true ); echo $r["success"] ? "ok" : "failed: " . $r["message"];',
		] )
	).toBe( 'ok' );
	expect( ask( [ 'eval', withEntities( seed.postId ) ] ) ).toBe( 'ok' );
} );

test.afterAll( ( { seed }, testInfo ) => {
	if ( 'site' !== testInfo.project.name ) {
		return;
	}
	for ( const name of options ) {
		if ( null === saved[ name ] ) {
			wp( [ 'option', 'delete', name ] );
		} else {
			wp( [ 'option', 'update', name, saved[ name ], '--format=json' ] );
		}
	}
	// The post's document as the plugin sends it, without the values put in above.
	wp( [
		'eval',
		`( new Viglet_Turing_Operations() )->index( array( ${ seed.postId } ) );`,
	] );
} );

test.beforeEach( ( {}, testInfo ) => {
	test.skip( 'site' !== testInfo.project.name, 'desktop only' );
} );

const block = ( page ) => page.locator( '.viglet-turing-search' );
const facet = ( page, label ) =>
	block( page )
		.locator( '.viglet-turing-search__facet' )
		.filter( { has: page.locator( 'summary', { hasText: label } ) } );
const titles = ( page ) =>
	block( page )
		.locator(
			'.viglet-turing-search__results .viglet-turing-search__title'
		)
		.allTextContents();

test( 'the people and places in a post are facets by their labels, and each finds it', async ( {
	page,
	seed,
} ) => {
	await page.goto( `/?page_id=${ seed.searchPageId }&q=*` );
	await expect( facet( page, 'People' ) ).toBeVisible();
	await expect( facet( page, 'Places' ) ).toBeVisible();
	// Fields left Organisations out of the block.
	await expect( facet( page, 'Organisations' ) ).toHaveCount( 0 );

	await facet( page, 'People' )
		.locator( 'label' )
		.filter( { hasText: person } )
		.click();
	await expect( page ).toHaveURL( /entity_person/ );
	await expect(
		block( page ).locator( '.viglet-turing-search__summary' )
	).toHaveText( '1 result' );
	expect( await titles( page ) ).toEqual( [ title ] );
	// The applied filter is named by the facet's label, not its field.
	await expect(
		block( page ).locator( '.viglet-turing-search__applied' )
	).toContainText( `People: ${ person }` );

	await page.goto(
		`/?page_id=${ seed.searchPageId }&q=*&fq[]=${ encodeURIComponent( `entity_place:${ place }` ) }`
	);
	await expect(
		block( page ).locator( '.viglet-turing-search__summary' )
	).toHaveText( '1 result' );
	expect( await titles( page ) ).toEqual( [ title ] );
} );
