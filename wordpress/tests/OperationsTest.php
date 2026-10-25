<?php
/**
 * The bulk operations behind `wp dumont`: index by ids, sweep the orphans, report status.
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Brain\Monkey\Functions;
use Viglet_Dumont_Operations;
use WP_Post;
use WP_Query;

final class OperationsTest extends PluginTestCase {

	/** The posts get_post finds, by id. */
	private array $posts = array();

	/** The document ids Turing holds under the site key `site-a`. */
	private array $held = array();

	protected function setUp(): void {
		parent::setUp();

		foreach ( array( 1, 2, 3 ) as $id ) {
			$this->posts[ $id ] = new WP_Post(
				array(
					'ID'          => $id,
					'post_title'  => "Post $id",
					'post_type'   => 'post',
					'post_status' => 'publish',
				)
			);
		}
		WP_Query::$ids = array( 1, 2, 3 );

		Functions\stubs(
			array(
				'strip_shortcodes'           => static fn ( $text ) => $text,
				'wp_strip_all_tags'          => static fn ( $text ) => strip_tags( $text ),
				'wp_trim_words'              => static fn ( $text ) => $text,
				'get_permalink'              => 'https://site.example.org/post/',
				'get_post_time'              => '2026-10-01T09:00:00+00:00',
				'get_post_modified_time'     => '2026-10-02T09:00:00+00:00',
				'get_bloginfo'               => 'Institutional',
				'get_the_post_thumbnail_url' => false,
				'wp_get_post_categories'     => array(),
				'wp_get_post_tags'           => array(),
				'get_the_author_meta'        => 'Ana',
				'is_multisite'               => false,
				'wp_is_post_revision'        => false,
				'get_post_meta'              => '',
			)
		);
		Functions\when( 'get_post' )->alias( fn ( $id ) => $this->posts[ $id ] ?? null );
		Functions\when( 'wp_remote_request' )->alias(
			function ( string $url, array $args ) {
				$this->requests[] = array( $url, $args );
				$body             = '';
				if ( 'GET' === $args['method'] && str_contains( $url, '/search' ) ) {
					$body = json_encode(
						array(
							'queryContext' => array( 'count' => count( $this->held ) ),
							'results'      => array(
								'document' => array_map( static fn ( $id ) => array( 'fields' => array( 'id' => $id ) ), $this->held ),
							),
						)
					);
				}
				return array(
					'response' => array( 'code' => $this->status ),
					'body'     => $body,
				);
			}
		);
	}

	private function operations(): Viglet_Dumont_Operations {
		return new Viglet_Dumont_Operations( $this->settings( array( 'site_key' => 'site-a' ) ) );
	}

	/** Every job item sent to the import endpoint, as [action, id]. */
	private function imported(): array {
		$items = array();
		foreach ( $this->requests as $index => $request ) {
			if ( str_ends_with( $request[0], '/api/sn/import' ) ) {
				foreach ( $this->body( $index )['snJobItems'] as $item ) {
					$items[] = array( $item['turSNJobAction'], $item['attributes']['id'] );
				}
			}
		}
		return $items;
	}

	public function test_index_creates_what_is_published_and_deletes_what_is_not(): void {
		$this->posts[2]->post_status = 'draft';
		unset( $this->posts[3] );

		$result = $this->operations()->index( array( 1, 2, 3 ) );

		$this->assertSame( 3, $result['sent'] );
		$this->assertSame(
			array(
				array( 'CREATE', 'site-a:post:1' ),
				array( 'DELETE', 'site-a:post:2' ),
				array( 'DELETE', 'site-a:post:3' ),
			),
			$this->imported()
		);
	}

	public function test_index_sends_in_batches_and_names_the_posts_of_a_failed_batch(): void {
		$this->status = 500;
		$done         = array();

		$result = ( new Viglet_Dumont_Operations(
			$this->settings(
				array(
					'site_key'   => 'site-a',
					'batch_size' => 2,
				)
			)
		) )
			->index(
				array( 1, 2, 3 ),
				static function ( int $count ) use ( &$done ) {
					$done[] = $count;
				}
			);

		$this->assertSame( array( 2, 1 ), $done );
		$this->assertSame( 0, $result['sent'] );
		$this->assertSame( array( 1, 2, 3 ), array_keys( $result['errors'] ) );
	}

	public function test_the_orphans_are_the_held_ids_wordpress_does_not_publish(): void {
		WP_Query::$ids = array( 1, 2 );
		$this->held    = array( 'site-a:post:1', 'site-a:post:2', 'site-a:post:9', 'site-a:page:1' );

		$this->assertSame( array( 'site-a:post:9', 'site-a:page:1' ), $this->operations()->orphans() );
	}

	public function test_a_dry_run_sweep_lists_without_deleting(): void {
		WP_Query::$ids = array( 1 );
		$this->held    = array( 'site-a:post:1', 'site-a:post:7' );

		$result = $this->operations()->sweep( true );

		$this->assertSame( array( 'site-a:post:7' ), $result['orphans'] );
		$this->assertSame( array(), $this->imported() );
	}

	public function test_a_sweep_deletes_only_the_orphans(): void {
		WP_Query::$ids = array( 1 );
		$this->held    = array( 'site-a:post:1', 'site-a:post:7' );

		$this->assertTrue( $this->operations()->sweep()['success'] );
		$this->assertSame( array( array( 'DELETE', 'site-a:post:7' ) ), $this->imported() );
	}

	public function test_a_sweep_that_cannot_list_turing_deletes_nothing(): void {
		$this->status = 500;

		$result = $this->operations()->sweep();

		$this->assertFalse( $result['success'] );
		$this->assertSame( array(), $this->imported() );
	}

	public function test_a_reindex_with_failures_does_not_sweep(): void {
		$this->status = 500;
		$this->held   = array( 'site-a:post:7' );

		$result = $this->operations()->reindex();

		$this->assertSame( 0, $result['swept'] );
		$this->assertNotSame( '', $result['sweep_error'] );
	}

	public function test_since_narrows_by_modification_date(): void {
		$this->operations()->find_post_ids( 'page', array(), '2026-10-01' );

		$this->assertSame( array( 'page' ), WP_Query::$last_args['post_type'] );
		$this->assertSame( '2026-10-01', WP_Query::$last_args['date_query'][0]['after'] );
	}
}
