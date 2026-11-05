<?php
/**
 * The PDFs a post links in its content, in search while a post in search holds them, and gone
 * with the last one (D319).
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Brain\Monkey\Functions;
use Viglet_Dumont_Pdf;
use WP_Post;
use ZipArchive;

final class PdfLinksTest extends PluginTestCase {

	private const UPLOADS = 'https://site.example.org/wp-content/uploads';

	/** The posts get_post() knows, by id. */
	private array $posts = array();

	/** Post meta, by post id and key, each a list of rows. */
	private array $meta = array();

	/** The addresses attachment_url_to_postid() was asked about. */
	private array $resolved = array();

	/** A small file standing for the PDF. */
	private string $file = '';

	protected function setUp(): void {
		parent::setUp();

		$this->file = (string) tempnam( sys_get_temp_dir(), 'pdf' );
		file_put_contents( $this->file, '%PDF-1.4 forms' );

		$this->posts = array(
			10 => $this->post( 10, 'post', 'Academic office' ),
			11 => $this->post( 11, 'page', 'Forms', '<p><a href="' . self::UPLOADS . '/2026/11/forms.pdf">Forms</a></p>' ),
			12 => $this->post( 12, 'post', 'Enrolment', '<a href="/wp-content/uploads/2026/11/forms.pdf?v=2">The forms</a>' ),
			20 => $this->attachment( 20, 10, 'Regulations' ),
			30 => $this->attachment( 30, 0, 'Enrolment forms' ),
		);
		Functions\when( 'get_post' )->alias( fn ( $id ) => $this->posts[ (int) $id ] ?? null );
		Functions\when( 'get_attached_file' )->alias( fn () => $this->file );
		Functions\when( 'get_post_meta' )->alias(
			function ( $id, $key, $single ) {
				$rows = $this->meta[ (int) $id ][ $key ] ?? array();
				return $single ? ( $rows[0] ?? '' ) : $rows;
			}
		);
		Functions\when( 'add_post_meta' )->alias(
			function ( $id, $key, $value ) {
				$this->meta[ (int) $id ][ $key ][] = $value;
				return true;
			}
		);
		Functions\when( 'update_post_meta' )->alias(
			function ( $id, $key, $value ) {
				$this->meta[ (int) $id ][ $key ] = array( $value );
				return true;
			}
		);
		Functions\when( 'delete_post_meta' )->alias(
			function ( $id, $key, $value = '' ) {
				$rows                            = $this->meta[ (int) $id ][ $key ] ?? array();
				$this->meta[ (int) $id ][ $key ] = '' === $value ? array() : array_values( array_filter( $rows, static fn ( $row ) => (string) $row !== (string) $value ) );
				if ( array() === $this->meta[ (int) $id ][ $key ] ) {
					unset( $this->meta[ (int) $id ][ $key ] );
				}
				return true;
			}
		);
		Functions\when( 'metadata_exists' )->alias( fn ( $type, $id, $key ) => isset( $this->meta[ (int) $id ][ $key ] ) );
		Functions\when( 'get_posts' )->alias(
			function ( array $args ) {
				$ids = array();
				foreach ( $this->posts as $id => $post ) {
					if ( 'attachment' !== $post->post_type ) {
						continue;
					}
					if ( isset( $args['post_parent'] ) && (int) $args['post_parent'] !== (int) $post->post_parent ) {
						continue;
					}
					if ( isset( $args['meta_key'] ) && ! in_array( (string) $args['meta_value'], array_map( 'strval', $this->meta[ $id ][ $args['meta_key'] ] ?? array() ), true ) ) {
						continue;
					}
					$ids[] = $id;
				}
				return $ids;
			}
		);
		Functions\when( 'wp_get_upload_dir' )->justReturn( array( 'baseurl' => self::UPLOADS ) );
		Functions\when( 'attachment_url_to_postid' )->alias(
			function ( $url ) {
				$this->resolved[] = $url;
				return self::UPLOADS . '/2026/11/forms.pdf' === $url ? 30 : 0;
			}
		);
		Functions\stubs(
			array(
				'wp_is_post_revision'    => false,
				'wp_get_attachment_url'  => self::UPLOADS . '/2026/11/forms.pdf',
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

	private function post( int $id, string $type, string $title, string $content = '' ): WP_Post {
		return new WP_Post(
			array(
				'ID'           => $id,
				'post_type'    => $type,
				'post_status'  => 'publish',
				'post_title'   => $title,
				'post_content' => $content,
			)
		);
	}

	private function attachment( int $id, int $parent_id, string $title ): WP_Post {
		return new WP_Post(
			array(
				'ID'             => $id,
				'post_type'      => 'attachment',
				'post_status'    => 'inherit',
				'post_mime_type' => 'application/pdf',
				'post_parent'    => $parent_id,
				'post_title'     => $title,
			)
		);
	}

	private function pdf(): Viglet_Dumont_Pdf {
		return new Viglet_Dumont_Pdf( $this->settings( array( 'index_pdfs' => 1 ) ) );
	}

	/** The ids the DELETE items among the sent requests name. */
	private function deleted(): array {
		$ids = array();
		foreach ( $this->requests as [ $url, $args ] ) {
			if ( str_ends_with( $url, '/api/sn/import' ) ) {
				foreach ( json_decode( $args['body'], true )['snJobItems'] as $item ) {
					if ( 'DELETE' === $item['turSNJobAction'] ) {
						$ids[] = $item['attributes']['id'];
					}
				}
			}
		}
		return $ids;
	}

	public function test_a_pdf_linked_not_attached_comes_into_search_with_the_post_that_links_it(): void {
		if ( ! class_exists( ZipArchive::class ) ) {
			$this->markTestSkipped( 'No zip extension here.' );
		}
		$this->assertFalse( $this->pdf()->indexable( $this->posts[30] ), 'no post holds it yet' );

		$this->pdf()->settle( array( $this->posts[11] ), array() );

		$this->assertSame( array( 11 ), $this->meta[30][ Viglet_Dumont_Pdf::LINKED_META ] );
		$this->assertTrue( $this->pdf()->indexable( $this->posts[30] ) );
		$this->assertSame( 'Forms', $this->pdf()->document( $this->posts[30] )['section'], 'the linking post is its section' );
		$this->assertStringEndsWith( '/api/sn/import/zip', $this->requests[0][0], 'sent as a zip with its file' );

		// Sent again unchanged, the post sends nothing more: the PDF is in search already.
		$this->requests = array();
		$this->pdf()->settle( array( $this->posts[11] ), array() );
		$this->assertSame( array(), $this->requests );
	}

	public function test_a_root_relative_link_is_this_sites_and_another_hosts_pdf_never_is(): void {
		$this->assertSame( array( 30 ), $this->pdf()->linked_ids( $this->posts[12] ) );

		$this->resolved = array();
		$foreign        = $this->post( 13, 'post', 'Elsewhere', '<a href="https://elsewhere.example/wp-content/uploads/2026/11/forms.pdf">x</a><img src="//elsewhere.example/a.pdf">' );
		$this->assertSame( array(), $this->pdf()->linked_ids( $foreign ) );
		$this->assertSame( array(), $this->resolved, "another host's address is never even looked up" );
	}

	public function test_a_post_leaving_search_takes_its_only_pdfs_with_it(): void {
		if ( ! class_exists( ZipArchive::class ) ) {
			$this->markTestSkipped( 'No zip extension here.' );
		}
		$this->pdf()->settle( array( $this->posts[10], $this->posts[11] ), array() );
		$this->requests = array();

		$this->posts[10]->post_status = 'draft';
		$this->posts[11]->post_status = 'draft';
		$this->pdf()->settle( array(), array( 10, 11 ) );

		$this->assertEqualsCanonicalizing( array( 'site.example.org:post:20', 'site.example.org:post:30' ), $this->deleted(), 'the attached one and the linked one' );
		$this->assertArrayNotHasKey( Viglet_Dumont_Pdf::LINKED_META, $this->meta[30] ?? array() );
		$this->assertArrayNotHasKey( Viglet_Dumont_Pdf::HELD_META, $this->meta[11] ?? array() );
	}

	public function test_a_pdf_another_post_in_search_still_links_stays(): void {
		if ( ! class_exists( ZipArchive::class ) ) {
			$this->markTestSkipped( 'No zip extension here.' );
		}
		$this->pdf()->settle( array( $this->posts[11], $this->posts[12] ), array() );
		$this->requests = array();

		$this->posts[11]->post_status = 'draft';
		$this->pdf()->settle( array(), array( 11 ) );

		$this->assertSame( array(), $this->deleted() );
		$this->assertTrue( $this->pdf()->indexable( $this->posts[30] ) );
		$this->assertSame( 'Enrolment', $this->pdf()->holder( $this->posts[30] )->post_title );
	}

	public function test_a_post_that_stops_linking_a_pdf_lets_go_of_it(): void {
		if ( ! class_exists( ZipArchive::class ) ) {
			$this->markTestSkipped( 'No zip extension here.' );
		}
		$this->pdf()->settle( array( $this->posts[11] ), array() );
		$this->requests = array();

		$this->posts[11]->post_content = '<p>No forms this term.</p>';
		$this->pdf()->settle( array( $this->posts[11] ), array() );

		$this->assertSame( array( 'site.example.org:post:30' ), $this->deleted() );
	}

	public function test_a_post_deleted_for_good_still_takes_its_attached_pdfs(): void {
		$this->pdf()->hold_children( 10 );
		// Deleting the post leaves its attachment without a parent, and the post gone.
		$this->posts[20]->post_parent = 0;
		unset( $this->posts[10] );

		$this->pdf()->settle( array(), array( 10 ) );

		$this->assertSame( array( 'site.example.org:post:20' ), $this->deleted() );
	}
}
