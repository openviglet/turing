// A PDF attached to a post in search is found by a sentence inside it (D309): Turing reads
// its text from the zip the plugin sends, and deleting the attachment takes it out again.
// Desktop only: it uploads, indexes and deletes.
import { expect, test } from '../fixtures.mjs';
import { wp } from '../global-setup.mjs';

const sentence = 'Quokka enrolment clause';

/**
 * The last line WP-CLI printed, so a notice before it is no answer.
 *
 * @param {string[]} args The wp arguments.
 */
const ask = ( args ) => wp( args ).trim().split( /\r?\n/ ).pop().trim();

// A one-page PDF whose only text is the sentence, its cross-reference table counted.
const writePdf = `
$text = ${ JSON.stringify( sentence ) };
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
$tmp = wp_tempnam( 'quokka-enrolment.pdf' );
file_put_contents( $tmp, $pdf );
echo media_handle_sideload( array( 'name' => 'quokka-enrolment.pdf', 'tmp_name' => $tmp ), (int) getenv( 'PARENT' ), 'Enrolment rules' );
`;

let attachment = '';

test.beforeAll( ( { seed }, testInfo ) => {
	if ( 'site' !== testInfo.project.name ) {
		return;
	}
	wp( [
		'option',
		'patch',
		'insert',
		'viglet_dumont_settings',
		'index_pdfs',
		'1',
	] );
	attachment = ask( [
		'eval',
		writePdf.replace( "getenv( 'PARENT' )", String( seed.postId ) ),
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
	wp( [
		'option',
		'patch',
		'delete',
		'viglet_dumont_settings',
		'index_pdfs',
	] );
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
