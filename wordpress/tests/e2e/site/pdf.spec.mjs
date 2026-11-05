// A PDF attached to a post in search is found by a sentence inside it (D309): Turing reads
// its text from the zip the plugin sends, and deleting the attachment takes it out again.
// Desktop only: it uploads, indexes and deletes.
import { expect, test } from '../fixtures.mjs';
import { setSetting, wp } from '../global-setup.mjs';

const sentence = 'Quokka enrolment clause';

/**
 * The last line WP-CLI printed, so a notice before it is no answer.
 *
 * @param {string[]} args The wp arguments.
 */
const ask = ( args ) => wp( args ).trim().split( /\r?\n/ ).pop().trim();

/**
 * PHP that uploads a one-page PDF whose only text is a sentence, its cross-reference table
 * counted, and prints the attachment's id.
 *
 * @param {string} text   The sentence.
 * @param {number} parent The post it is attached to; 0 for none.
 * @param {string} title  The attachment's title.
 */
const uploadPdf = ( text, parent, title ) => `
$text = ${ JSON.stringify( text ) };
$objects = array(
	'<< /Type /Catalog /Pages 2 0 R >>',
	'<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
	'<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>',
	null,
	'<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
);
$stream = "BT /F1 18 Tf 72 700 Td ($text) Tj ET";
$objects[3] = '<< /Length ' . strlen( $stream ) . " >>\\nstream\\n$stream\\nendstream";
$pdf = "%PDF-1.4\\n";
$offsets = array();
foreach ( $objects as $i => $object ) {
	$offsets[] = strlen( $pdf );
	$pdf .= ( $i + 1 ) . " 0 obj\\n$object\\nendobj\\n";
}
$xref = strlen( $pdf );
$pdf .= "xref\\n0 " . ( count( $objects ) + 1 ) . "\\n0000000000 65535 f \\n";
foreach ( $offsets as $offset ) {
	$pdf .= sprintf( "%010d 00000 n \\n", $offset );
}
$pdf .= 'trailer << /Size ' . ( count( $objects ) + 1 ) . " /Root 1 0 R >>\\nstartxref\\n$xref\\n%%EOF";
// Uploaded as an editor would, in the same run: each WP-CLI run is a container of its own.
require_once ABSPATH . 'wp-admin/includes/file.php';
require_once ABSPATH . 'wp-admin/includes/media.php';
require_once ABSPATH . 'wp-admin/includes/image.php';
$tmp = wp_tempnam( 'upload.pdf' );
file_put_contents( $tmp, $pdf );
echo media_handle_sideload( array( 'name' => 'upload.pdf', 'tmp_name' => $tmp ), ${ parent }, ${ JSON.stringify( title ) } );
`;

let attachment = '';

test.beforeAll( ( { seed }, testInfo ) => {
	if ( 'site' !== testInfo.project.name ) {
		return;
	}
	setSetting( 'index_pdfs', '1' );
	attachment = ask( [
		'eval',
		uploadPdf( sentence, seed.postId, 'Enrolment rules' ),
	] );
} );

test.afterAll( ( {}, testInfo ) => {
	if ( 'site' !== testInfo.project.name ) {
		return;
	}
	if ( attachment ) {
		try {
			wp( [ 'post', 'delete', attachment, '--force' ] );
		} catch {}
	}
	setSetting( 'index_pdfs', null );
} );

test.beforeEach( ( {}, testInfo ) => {
	test.skip( 'site' !== testInfo.project.name, 'desktop only' );
} );

/**
 * The titles the search page lists for the sentence, once Turing has had time to commit.
 *
 * @param {import('@playwright/test').Page} page The page.
 * @param {Object}                          seed The seeded ids.
 */
async function titlesFor( page, seed ) {
	await page.waitForTimeout( 4000 );
	await page.goto(
		`/?page_id=${ seed.searchPageId }&q=${ encodeURIComponent( sentence ) }`
	);
	await expect( page.locator( '.viglet-dumont-search__summary' ) ).toHaveText(
		/result/
	);
	return page
		.locator(
			'.viglet-dumont-search__results .viglet-dumont-search__title'
		)
		.allTextContents();
}

test( "a PDF's inner sentence finds it, and deleting the attachment removes it", async ( {
	page,
	seed,
} ) => {
	expect( await titlesFor( page, seed ) ).toContain( 'Enrolment rules' );
	await expect(
		page
			.locator( '.viglet-dumont-search__result' )
			.filter( { hasText: 'Enrolment rules' } )
			.locator( '.viglet-dumont-search__type' )
	).toHaveText( 'PDF' );

	wp( [ 'post', 'delete', attachment, '--force' ] );
	attachment = '';
	expect( await titlesFor( page, seed ) ).not.toContain( 'Enrolment rules' );
} );

test( "a PDF a post's meta names puts its words in the post's own text", async ( {
	page,
	seed,
} ) => {
	const words = 'Wombat thesis appendix';
	const paper = ask( [ 'eval', uploadPdf( words, 0, 'Wombat paper' ) ] );
	const url = ask( [ 'eval', `echo wp_get_attachment_url( ${ paper } );` ] );
	const saved = ask( [
		'option',
		'get',
		'viglet_dumont_mapping',
		'--format=json',
	] );
	const title = ask( [
		'post',
		'get',
		String( seed.postId ),
		'--field=post_title',
	] );
	try {
		wp( [
			'eval',
			`$m = get_option( 'viglet_dumont_mapping' );
			$m['post']['fields'][] = array( 'source' => 'file:paper', 'field' => 'paper_text', 'type' => 'TEXT' );
			update_option( 'viglet_dumont_mapping', $m );`,
		] );
		wp( [ 'post', 'meta', 'update', String( seed.postId ), 'paper', url ] );
		wp( [ 'dumont', 'reindex' ] );

		await page.waitForTimeout( 4000 );
		await page.goto(
			`/?page_id=${ seed.searchPageId }&q=${ encodeURIComponent( words ) }`
		);
		await expect(
			page.locator( '.viglet-dumont-search__results li' ).first()
		).toBeVisible();
		expect(
			await page
				.locator(
					'.viglet-dumont-search__results .viglet-dumont-search__title'
				)
				.allTextContents()
		).toContain( title );
	} finally {
		wp( [
			'option',
			'update',
			'viglet_dumont_mapping',
			saved,
			'--format=json',
		] );
		wp( [ 'post', 'meta', 'delete', String( seed.postId ), 'paper' ] );
		wp( [ 'post', 'delete', paper, '--force' ] );
		wp( [ 'dumont', 'reindex' ] );
	}
} );
