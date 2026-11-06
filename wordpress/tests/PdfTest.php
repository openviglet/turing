<?php
/**
 * The PDFs of the posts in search, as documents of their own (D309).
 *
 * @package Viglet_Turing
 */

namespace Viglet\Turing\Tests;

use Brain\Monkey\Functions;
use Viglet_Turing_API_Client;
use Viglet_Turing_Pdf;
use WP_Post;
use ZipArchive;

final class PdfTest extends PluginTestCase {

	/** The posts get_post() knows, by id. */
	private array $posts = array();

	/** A small file standing for the PDF. */
	private string $file = '';

	protected function setUp(): void {
		parent::setUp();

		$this->file = (string) tempnam( sys_get_temp_dir(), 'pdf' );
		file_put_contents( $this->file, '%PDF-1.4 regulations' );

		$this->posts = array(
			10 => new WP_Post(
				array(
					'ID'          => 10,
					'post_type'   => 'post',
					'post_status' => 'publish',
					'post_title'  => 'Academic office',
				)
			),
			20 => new WP_Post(
				array(
					'ID'             => 20,
					'post_type'      => 'attachment',
					'post_status'    => 'inherit',
					'post_mime_type' => 'application/pdf',
					'post_parent'    => 10,
					'post_title'     => 'Regulations',
				)
			),
		);
		Functions\when( 'get_post' )->alias( fn ( $id ) => $this->posts[ (int) $id ] ?? null );
		Functions\when( 'get_attached_file' )->alias( fn () => $this->file );
		Functions\stubs(
			array(
				'wp_is_post_revision'    => false,
				'get_post_meta'          => '',
				'wp_get_attachment_url'  => 'https://site.example.org/wp-content/uploads/regulations.pdf',
				'wp_strip_all_tags'      => static fn ( $text ) => strip_tags( $text ),
				'get_post_time'          => '2026-11-01T00:00:00+00:00',
				'get_post_modified_time' => '2026-11-02T00:00:00+00:00',
				'get_bloginfo'           => 'Institutional',
				'wp_tempnam'             => static fn () => (string) tempnam( sys_get_temp_dir(), 'zip' ),
				'wp_delete_file'         => static function ( $path ) {
					unlink( $path );
				},
			)
		);
	}

	protected function tearDown(): void {
		if ( is_file( $this->file ) ) {
			unlink( $this->file );
		}
		parent::tearDown();
	}

	private function pdf( array $overrides = array( 'index_pdfs' => 1 ) ): Viglet_Turing_Pdf {
		return new Viglet_Turing_Pdf( $this->settings( $overrides ) );
	}

	public function test_a_pdf_attached_to_a_post_in_search_is_in_search_and_nothing_else_is(): void {
		$this->assertTrue( $this->pdf()->indexable( $this->posts[20] ) );
		$this->assertFalse( $this->pdf( array() )->indexable( $this->posts[20] ), 'only when turned on' );
		$this->assertFalse( $this->pdf()->indexable( $this->posts[10] ), 'a post is no PDF' );

		$this->posts[10]->post_status = 'draft';
		$this->assertFalse( $this->pdf()->indexable( $this->posts[20] ), 'its post out of search, it is out too' );
		$this->posts[10]->post_status = 'publish';

		Functions\when( 'apply_filters' )->alias( static fn ( $hook, $value ) => 'viglet_turing_pdf_max_bytes' === $hook ? 4 : $value );
		$this->assertFalse( $this->pdf()->indexable( $this->posts[20] ), 'over the size cap' );
	}

	public function test_a_pdf_document_names_its_file_for_turing_to_read_and_its_post_as_section(): void {
		$document = $this->pdf()->document( $this->posts[20] );

		$this->assertSame( 'site.example.org:post:20', $document['id'] );
		$this->assertSame( 'Regulations', $document['title'] );
		$this->assertSame( 'file://20.pdf', $document['text'] );
		$this->assertSame( 'pdf', $document['type'] );
		$this->assertSame( 'Academic office', $document['section'] );
	}

	/** The zip carries export.json, whose items name the file it also carries. */
	public function test_pdfs_go_to_turing_as_a_zip_of_the_export_and_the_files(): void {
		if ( ! class_exists( ZipArchive::class ) ) {
			$this->markTestSkipped( 'No zip extension here.' );
		}

		$result = $this->pdf()->index( array( $this->posts[20] ) );

		$this->assertTrue( $result['success'] );
		[ $url, $args ] = $this->requests[0];
		$this->assertStringEndsWith( '/api/sn/import/zip', $url );
		$this->assertStringStartsWith( 'multipart/form-data; boundary=', $args['headers']['Content-Type'] );
		$this->assertSame( 'secret', $args['headers']['Key'] );

		preg_match( '/\r\n\r\n(PK.*)\r\n--/s', $args['body'], $part );
		$zip = (string) tempnam( sys_get_temp_dir(), 'zip' );
		file_put_contents( $zip, $part[1] );
		$archive = new ZipArchive();
		$archive->open( $zip );
		$export = json_decode( (string) $archive->getFromName( 'export.json' ), true );
		$this->assertSame( 'file://20.pdf', $export['snJobItems'][0]['attributes']['text'] );
		$this->assertSame( '%PDF-1.4 regulations', $archive->getFromName( '20.pdf' ) );
		$archive->close();
		unlink( $zip );
	}

	public function test_a_refused_zip_import_is_a_failure(): void {
		if ( ! class_exists( ZipArchive::class ) ) {
			$this->markTestSkipped( 'No zip extension here.' );
		}
		Functions\when( 'wp_remote_request' )->justReturn(
			array(
				'response' => array( 'code' => 200 ),
				'body'     => 'false',
			)
		);

		$this->assertFalse( ( new Viglet_Turing_API_Client( $this->settings() ) )->import_files( array( array() ), array() )['success'] );
	}

	/**
	 * A `file:` source names this site's PDF by URL, and the import that sends the post takes
	 * the file along as a zip, so its text lands in the post's own field (D309).
	 */
	public function test_a_meta_named_pdf_goes_with_its_post_and_another_hosts_does_not(): void {
		if ( ! class_exists( ZipArchive::class ) ) {
			$this->markTestSkipped( 'No zip extension here.' );
		}
		Functions\when( 'get_post_meta' )->justReturn( 'https://site.example.org/wp-content/uploads/regulations.pdf' );
		Functions\when( 'attachment_url_to_postid' )->alias( static fn ( $url ) => str_starts_with( $url, 'https://site.example.org/' ) ? 20 : 0 );

		$reference = Viglet_Turing_Pdf::file_reference( $this->posts[10], 'paper' );
		$this->assertSame( 'file://10-20.pdf', $reference );

		$client = new Viglet_Turing_API_Client( $this->settings() );
		$client->import( array( $client->build_create_item( array( 'paper_text' => $reference ) ) ) );
		$this->assertStringEndsWith( '/api/sn/import/zip', $this->requests[0][0] );

		// Sent once: the next import of the same item is plain JSON again.
		$client->import( array( $client->build_create_item( array( 'paper_text' => $reference ) ) ) );
		$this->assertStringEndsWith( '/api/sn/import', $this->requests[1][0] );

		Functions\when( 'get_post_meta' )->justReturn( 'https://elsewhere.example/paper.pdf' );
		$this->assertNull( Viglet_Turing_Pdf::file_reference( $this->posts[10], 'paper' ), "another host's file is never fetched" );
	}

	public function test_a_deleted_pdf_leaves_search(): void {
		$this->pdf()->remove( 20 );

		$this->assertSame( 'DELETE', json_decode( $this->requests[0][1]['body'], true )['snJobItems'][0]['turSNJobAction'] );
	}
}
