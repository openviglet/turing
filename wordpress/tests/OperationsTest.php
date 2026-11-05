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

	/** How many of the held documents another source sent (D322). */
	private int $foreign = 0;

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
							// A search narrowed to this plugin's source_apps leaves out what another source sent.
							'queryContext' => array( 'count' => count( $this->held ) - ( str_contains( urldecode( $url ), 'source_apps:' ) ? $this->foreign : 0 ) ),
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

	public function test_a_batch_turing_did_not_answer_in_time_is_unconfirmed_not_failed(): void {
		// D256: Turing finished the import after the client stopped waiting.
		Functions\when( 'wp_remote_request' )->justReturn( new \WP_Error( 'http_request_failed', 'cURL error 28: Operation timed out after 30001 milliseconds' ) );

		$result = $this->operations()->index( array( 1, 2, 3 ) );

		$this->assertSame( 0, $result['sent'] );
		$this->assertSame( 3, $result['unconfirmed'] );
		$this->assertSame( 0, $result['failed'] );
		$this->assertSame( array(), $result['errors'] );
	}

	public function test_the_orphans_are_the_held_ids_wordpress_does_not_publish(): void {
		WP_Query::$ids = array( 1, 2 );
		$this->held    = array( 'site-a:post:1', 'site-a:post:2', 'site-a:post:9', 'site-a:post:x' );

		$this->assertSame( array( 'site-a:post:9', 'site-a:post:x' ), $this->operations()->orphans() );
	}

	public function test_a_document_another_source_sent_under_the_site_key_is_never_swept(): void {
		// D322: a catalog given this site key shares the search; the plugin did not send it.
		WP_Query::$ids = array( 1 );
		$this->held    = array( 'site-a:post:1', 'site-a:post:7', 'oai:repo.example:123/4', 'site-a:page:1' );

		$this->assertTrue( $this->operations()->sweep()['success'] );
		$this->assertSame( array( array( 'DELETE', 'site-a:post:7' ) ), $this->imported() );
	}

	public function test_status_counts_the_documents_another_source_sent_under_the_site_key(): void {
		$this->held    = array( 'site-a:post:1', 'oai:repo.example:123/4', 'oai:repo.example:123/5' );
		$this->foreign = 2;
		Functions\stubs( array( 'wp_cache_delete' => true ) );

		$this->assertSame( 2, $this->operations()->status()['foreign'] );
	}

	public function test_status_names_a_spent_ai_budget_and_its_reset_date(): void {
		// D332: Health reads it from here while the record is live.
		$quota   = array(
			'resetsAt' => '2099-12-01T00:00:00Z',
			'detail'   => 'The monthly AI budget of this site is spent.',
			'seen_at'  => time(),
		);
		$options = array( 'viglet_dumont_ai_quota' => $quota );
		Functions\when( 'get_option' )->alias( static fn ( $name, $fallback = false ) => $options[ $name ] ?? $fallback );
		Functions\stubs(
			array(
				'wp_cache_delete' => true,
				'wp_date'         => static fn ( $format, $at ) => gmdate( 'Y-m-d', $at ),
			)
		);

		$status = $this->operations()->status()['ai_quota'];

		$this->assertSame( 'AI budget exhausted until 2099-12-01.', $status['message'] );
		$this->assertSame( 'The monthly AI budget of this site is spent.', $status['detail'] );
	}

	public function test_status_has_no_ai_budget_line_while_none_is_known_spent(): void {
		Functions\stubs( array( 'wp_cache_delete' => true ) );

		$this->assertNull( $this->operations()->status()['ai_quota'] );
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

	public function test_status_counts_as_published_only_what_the_index_would_hold(): void {
		// D284: a post kept out of the index is never in Turing, so it is not a difference.
		Functions\when( 'get_post_meta' )->alias( static fn ( $id, $key ) => 2 === $id && '_viglet_dumont_exclude' === $key ? '1' : '' );
		$this->held = array( 'site-a:post:1', 'site-a:post:3' );
		Functions\stubs( array( 'wp_cache_delete' => true ) );

		$counts = $this->operations()->status()['types']['post'];

		$this->assertSame( 2, $counts['published'] );
		$this->assertSame( $counts['published'], $counts['indexed'] );
	}

	public function test_since_narrows_by_modification_date(): void {
		$this->operations()->find_post_ids( 'page', array(), '2026-10-01' );

		$this->assertSame( array( 'page' ), WP_Query::$last_args['post_type'] );
		$this->assertSame( '2026-10-01', WP_Query::$last_args['date_query'][0]['after'] );
	}
}
