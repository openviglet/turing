<?php
/**
 * Index All in the background (D331): WP-Cron sends it a few batches a run, a failed batch
 * waits and is tried again, and the PDFs and the sweep close a full one.
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Brain\Monkey\Functions;
use Viglet_Dumont_Operations;
use Viglet_Dumont_Reindex;
use WP_Post;
use WP_Query;

final class ReindexTest extends PluginTestCase {

	/** The options table, in memory. */
	private array $options = array();

	/** The transients, in memory. */
	private array $transients = array();

	/** The posts get_post finds, by id. */
	private array $posts = array();

	/** The cron events scheduled, each as [timestamp, hook]. */
	private array $scheduled = array();

	/** The status each import answers with, in order; $this->status once they run out. */
	private array $statuses = array();

	/** The document ids Turing holds under the site key `site-a`. */
	private array $held = array();

	/** A PDF file on disk, attached to post 1. */
	private string $file = '';

	protected function setUp(): void {
		parent::setUp();

		foreach ( array( 1, 2, 3, 4, 5 ) as $id ) {
			$this->posts[ $id ] = new WP_Post(
				array(
					'ID'          => $id,
					'post_title'  => "Post $id",
					'post_type'   => 'post',
					'post_status' => 'publish',
				)
			);
		}
		WP_Query::$ids = array( 1, 2, 3, 4, 5 );

		$this->file = (string) tempnam( sys_get_temp_dir(), 'pdf' );
		file_put_contents( $this->file, '%PDF-1.4 timetable' );
		$this->posts[20] = new WP_Post(
			array(
				'ID'             => 20,
				'post_type'      => 'attachment',
				'post_status'    => 'inherit',
				'post_mime_type' => 'application/pdf',
				'post_parent'    => 1,
				'post_title'     => 'Timetable',
			)
		);

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
				'wp_cache_delete'            => true,
				'wp_date'                    => static fn ( $format, $at ) => gmdate( 'Y-m-d H:i', $at ),
				'metadata_exists'            => true,
				'update_post_meta'           => true,
				'add_post_meta'              => true,
				'delete_post_meta'           => true,
				'get_attached_file'          => fn () => $this->file,
				'wp_get_attachment_url'      => 'https://site.example.org/wp-content/uploads/timetable.pdf',
				'wp_tempnam'                 => static fn () => (string) tempnam( sys_get_temp_dir(), 'zip' ),
				'wp_delete_file'             => static function ( $path ) {
					unlink( $path );
				},
			)
		);
		// The one PDF: attached to post 1, and among every PDF the reindex lists at the end.
		Functions\when( 'get_posts' )->alias(
			static fn ( array $args ) => ( 'inherit' === ( $args['post_status'] ?? '' ) || 1 === ( $args['post_parent'] ?? 0 ) ) ? array( 20 ) : array()
		);
		Functions\when( 'get_post' )->alias( fn ( $id ) => $this->posts[ (int) $id ] ?? null );
		Functions\when( 'get_option' )->alias( fn ( $name, $fallback = false ) => $this->options[ $name ] ?? $fallback );
		Functions\when( 'update_option' )->alias(
			function ( $name, $value ) {
				$this->options[ $name ] = $value;
				return true;
			}
		);
		Functions\when( 'delete_option' )->alias(
			function ( $name ) {
				unset( $this->options[ $name ] );
				return true;
			}
		);
		Functions\when( 'get_transient' )->alias( fn ( $name ) => $this->transients[ $name ] ?? false );
		Functions\when( 'set_transient' )->alias(
			function ( $name, $value ) {
				$this->transients[ $name ] = $value;
				return true;
			}
		);
		Functions\when( 'delete_transient' )->alias(
			function ( $name ) {
				unset( $this->transients[ $name ] );
				return true;
			}
		);
		Functions\when( 'wp_next_scheduled' )->alias(
			function ( $hook ) {
				$times = array_column( array_filter( $this->scheduled, static fn ( $event ) => $event[1] === $hook ), 0 );
				return empty( $times ) ? false : min( $times );
			}
		);
		Functions\when( 'wp_schedule_single_event' )->alias(
			function ( $timestamp, $hook ) {
				$this->scheduled[] = array( $timestamp, $hook );
				return true;
			}
		);
		Functions\when( 'wp_unschedule_event' )->alias(
			function ( $timestamp, $hook ) {
				$this->scheduled = array_values( array_filter( $this->scheduled, static fn ( $event ) => ! ( $event[0] === $timestamp && $event[1] === $hook ) ) );
				return true;
			}
		);
		Functions\when( 'wp_clear_scheduled_hook' )->alias(
			function ( $hook ) {
				$this->scheduled = array_values( array_filter( $this->scheduled, static fn ( $event ) => $event[1] !== $hook ) );
				return 0;
			}
		);
		Functions\when( 'wp_remote_request' )->alias(
			function ( string $url, array $args ) {
				$this->requests[] = array( $url, $args );
				if ( 'GET' === $args['method'] && str_contains( $url, '/search' ) ) {
					return array(
						'response' => array( 'code' => 200 ),
						'body'     => json_encode(
							array(
								'queryContext' => array( 'count' => count( $this->held ) ),
								'results'      => array( 'document' => array_map( static fn ( $id ) => array( 'fields' => array( 'id' => $id ) ), $this->held ) ),
							)
						),
					);
				}
				$code = str_contains( $url, '/api/sn/import' ) ? ( array_shift( $this->statuses ) ?? $this->status ) : 200;
				return array(
					'response' => array( 'code' => $code ),
					'body'     => '',
				);
			}
		);
	}

	protected function tearDown(): void {
		if ( is_file( $this->file ) ) {
			unlink( $this->file );
		}
		parent::tearDown();
	}

	private function reindex( array $overrides = array() ): Viglet_Dumont_Reindex {
		return new Viglet_Dumont_Reindex(
			$this->settings(
				$overrides + array(
					'site_key'   => 'site-a',
					'batch_size' => 1,
					'index_pdfs' => 1,
				)
			)
		);
	}

	/** The ids each post import carried, created or deleted, one list per import. */
	private function batches(): array {
		$batches = array();
		foreach ( $this->requests as $index => $request ) {
			if ( str_ends_with( $request[0], '/api/sn/import' ) ) {
				$batches[] = array_values(
					array_filter(
						array_map( static fn ( $item ) => $item['attributes']['id'], $this->body( $index )['snJobItems'] ),
						static fn ( $id ) => str_starts_with( $id, 'site-a:' )
					)
				);
			}
		}
		return $batches;
	}

	/** What reached Turing, in order: `posts`, `pdfs` or `search` per request. */
	private function calls(): array {
		$calls = array();
		foreach ( $this->requests as $request ) {
			if ( str_ends_with( $request[0], '/api/sn/import' ) ) {
				$calls[] = 'posts';
			} elseif ( str_ends_with( $request[0], '/api/sn/import/zip' ) ) {
				$calls[] = 'pdfs';
			} elseif ( str_contains( $request[0], '/search' ) ) {
				$calls[] = 'search';
			}
		}
		return array_values( array_unique( $calls ) );
	}

	/**
	 * Run WP-Cron until the reindex ends, each backoff let pass as the clock would.
	 *
	 * @param Viglet_Dumont_Reindex $reindex The reindex to drive.
	 */
	private function drive( Viglet_Dumont_Reindex $reindex ): void {
		$runs = 0;
		while ( $runs < 20 && null !== $reindex->tick() ) {
			++$runs;
			$this->wait_out_backoff();
		}
	}

	/** Let the run's backoff pass, as the clock would. */
	private function wait_out_backoff(): void {
		if ( isset( $this->options[ Viglet_Dumont_Reindex::OPTION ] ) ) {
			$this->options[ Viglet_Dumont_Reindex::OPTION ]['next_at'] = time() - 1;
		}
	}

	public function test_starting_records_the_run_and_schedules_it_without_sending_anything(): void {
		$run = $this->reindex()->start();

		$this->assertSame( 'running', $run['state'] );
		$this->assertSame( 'posts', $run['phase'] );
		$this->assertSame( 5, $run['total'] );
		$this->assertSame( 0, $run['offset'] );
		$this->assertSame( array( 1, 2, 3, 4, 5 ), $this->options[ Viglet_Dumont_Reindex::IDS_OPTION ] );
		$this->assertSame( array(), $this->requests );
		$this->assertSame( Viglet_Dumont_Reindex::CRON_HOOK, $this->scheduled[0][1] );
		$this->assertSame( $run['id'], $this->reindex()->report()['running']['id'] );
	}

	public function test_a_second_start_while_one_runs_is_refused(): void {
		$this->reindex()->start();

		$second = $this->reindex()->start();

		$this->assertInstanceOf( \WP_Error::class, $second );
		$this->assertSame( 'viglet_dumont_reindex_running', $second->get_error_code() );
	}

	public function test_a_type_that_is_not_indexed_is_refused(): void {
		$result = $this->reindex()->start( 'product' );

		$this->assertInstanceOf( \WP_Error::class, $result );
		$this->assertArrayNotHasKey( Viglet_Dumont_Reindex::OPTION, $this->options );
	}

	public function test_each_run_sends_a_few_batches_and_schedules_the_next(): void {
		$this->reindex()->start();
		$this->scheduled = array();

		$after = $this->reindex()->tick();

		$this->assertSame( array( array( 'site-a:post:1' ), array( 'site-a:post:2' ), array( 'site-a:post:3' ) ), $this->batches() );
		$this->assertSame( Viglet_Dumont_Reindex::STEPS_PER_RUN, $after['offset'] );
		$this->assertSame( 3, $after['sent'] );
		$this->assertSame( 'running', $after['state'] );
		$this->assertCount( 1, $this->scheduled );
		$this->assertLessThanOrEqual( time(), $this->scheduled[0][0] );

		$this->reindex()->tick();
		$this->assertSame( 5, $this->options[ Viglet_Dumont_Reindex::OPTION ]['offset'] );
	}

	public function test_a_full_reindex_ends_with_the_pdfs_and_the_sweep(): void {
		if ( ! class_exists( \ZipArchive::class ) ) {
			$this->markTestSkipped( 'No zip extension here.' );
		}
		$this->held = array( 'site-a:post:1', 'site-a:post:9' );
		$this->reindex()->start();

		$this->drive( $this->reindex() );

		$this->assertSame( array( 'posts', 'pdfs', 'search' ), $this->calls() );
		$this->assertArrayNotHasKey( Viglet_Dumont_Reindex::OPTION, $this->options );
		$this->assertArrayNotHasKey( Viglet_Dumont_Reindex::IDS_OPTION, $this->options );
		$last = $this->reindex()->report()['last'];
		$this->assertSame( 'done', $last['state'] );
		$this->assertSame( 5, $last['sent'] );
		$this->assertSame( 1, $last['pdfs'] );
		$this->assertSame( 1, $last['swept'] );
		$this->assertNull( $this->reindex()->report()['running'] );
		$this->assertSame( array(), $this->scheduled, 'nothing left to run' );
		// The orphan went last, in the sweep's delete.
		$batches = $this->batches();
		$this->assertSame( array( 'site-a:post:9' ), end( $batches ) );
	}

	public function test_one_type_sends_its_posts_pdfs_on_the_way_and_neither_lists_every_pdf_nor_sweeps(): void {
		$this->reindex( array( 'index_pdfs' => 0 ) )->start( 'post' );

		$this->drive( $this->reindex( array( 'index_pdfs' => 0 ) ) );

		$this->assertSame( array( 'posts' ), $this->calls() );
		$this->assertSame( 'done', $this->options[ Viglet_Dumont_Reindex::LAST_OPTION ]['state'] );
		$this->assertSame( 'post', WP_Query::$last_args['post_type'][0] );
	}

	public function test_a_failed_batch_waits_out_a_backoff_and_the_run_resumes_where_it_stopped(): void {
		$this->statuses = array( 503 );
		$this->reindex()->start();
		$this->scheduled = array();

		$before = time();
		$after  = $this->reindex()->tick();

		$this->assertCount( 1, $this->batches(), 'a failed batch ends the run' );
		$this->assertSame( 'running', $after['state'] );
		$this->assertSame( 0, $after['offset'] );
		$this->assertSame( 1, $after['attempts'] );
		$this->assertStringContainsString( '503', $after['error'] );
		$this->assertGreaterThanOrEqual( $before + Viglet_Dumont_Reindex::BASE_DELAY, $after['next_at'] );
		$this->assertSame( array( array( $after['next_at'], Viglet_Dumont_Reindex::CRON_HOOK ) ), $this->scheduled );

		// A run before the backoff passed sends nothing.
		$this->reindex()->tick();
		$this->assertCount( 1, $this->batches() );

		$this->wait_out_backoff();
		$resumed = $this->reindex()->tick();
		$this->assertSame( array( array( 'site-a:post:1' ), array( 'site-a:post:1' ), array( 'site-a:post:2' ), array( 'site-a:post:3' ) ), $this->batches() );
		$this->assertSame( 3, $resumed['offset'] );
		$this->assertSame( 0, $resumed['attempts'] );
		$this->assertSame( 0, $resumed['failed'] );
		$this->assertSame( '', $resumed['error'], 'the outage is over' );
	}

	public function test_a_batch_that_keeps_failing_counts_as_failed_and_the_run_goes_on_without_sweeping(): void {
		$this->statuses = array_fill( 0, Viglet_Dumont_Reindex::MAX_ATTEMPTS, 503 );
		$this->held     = array( 'site-a:post:9' );
		$this->reindex( array( 'index_pdfs' => 0 ) )->start();

		$this->drive( $this->reindex( array( 'index_pdfs' => 0 ) ) );

		$last = $this->options[ Viglet_Dumont_Reindex::LAST_OPTION ];
		$this->assertSame( 'failed', $last['state'] );
		$this->assertSame( 1, $last['failed'] );
		$this->assertSame( 4, $last['sent'] );
		$this->assertSame( 0, $last['swept'] );
		$this->assertNotSame( '', $last['sweep_error'] );
		$this->assertNotContains( 'search', $this->calls(), 'a run with failures never sweeps' );
	}

	public function test_a_post_turing_refuses_fails_alone_and_is_not_retried(): void {
		// 400 on a batch of two, then each post again on its own: the first goes, the second is refused.
		$this->statuses = array( 400, 200, 400 );
		$this->reindex(
			array(
				'batch_size' => 2,
				'index_pdfs' => 0,
			)
		)->start( 'post' );

		$after = $this->reindex(
			array(
				'batch_size' => 2,
				'index_pdfs' => 0,
			)
		)->tick();

		$this->assertSame( 4, $after['sent'] );
		$this->assertSame( 1, $after['failed'] );
		$this->assertSame( 0, $after['attempts'] );
		$this->assertSame( 5, $after['offset'] );
		$this->assertSame( array( array( 'site-a:post:1', 'site-a:post:2' ), array( 'site-a:post:1' ), array( 'site-a:post:2' ), array( 'site-a:post:3', 'site-a:post:4' ), array( 'site-a:post:5' ) ), $this->batches() );
	}

	public function test_cancel_stops_the_run_where_it_stands(): void {
		$this->reindex()->start();
		$this->reindex()->tick();

		$this->assertTrue( $this->reindex()->cancel() );

		$report = $this->reindex()->report();
		$this->assertNull( $report['running'] );
		$this->assertSame( 'cancelled', $report['last']['state'] );
		$this->assertSame( 3, $report['last']['offset'] );
		$this->assertSame( array(), $this->scheduled );

		// A run WP-Cron had already started finds nothing to do.
		$sent = count( $this->batches() );
		$this->assertNull( $this->reindex()->tick() );
		$this->assertCount( $sent, $this->batches() );
		$this->assertFalse( $this->reindex()->cancel(), 'nothing left to cancel' );
	}

	public function test_a_run_already_holding_the_lock_is_left_alone(): void {
		$this->reindex()->start();
		$this->transients[ Viglet_Dumont_Reindex::LOCK ] = 1;

		$this->reindex()->tick();

		$this->assertSame( array(), $this->batches() );
	}

	public function test_status_shows_the_running_reindex_and_the_last_one(): void {
		$this->reindex()->start();
		$this->reindex()->tick();

		$status = ( new Viglet_Dumont_Operations( $this->settings( array( 'site_key' => 'site-a' ) ) ) )->status();

		$running = $status['reindex']['running'];
		$this->assertSame( 3, $running['offset'] );
		$this->assertSame( 5, $running['total'] );
		$this->assertSame( 0, $running['failed'] );
		$this->assertFalse( $running['stalled'] );
		$this->assertNotSame( '', $running['started'] );
		$this->assertNull( $status['reindex']['last'] );

		$this->reindex()->cancel();
		$status = ( new Viglet_Dumont_Operations( $this->settings( array( 'site_key' => 'site-a' ) ) ) )->status();
		$this->assertNull( $status['reindex']['running'] );
		$this->assertSame( 'cancelled', $status['reindex']['last']['state'] );
	}

	public function test_a_run_nothing_has_moved_for_a_while_is_said_to_be_stalled(): void {
		$this->reindex()->start();
		$this->options[ Viglet_Dumont_Reindex::OPTION ]['started_at'] = time() - Viglet_Dumont_Reindex::STALL_AFTER - 60;

		$this->assertTrue( $this->reindex()->report()['running']['stalled'] );
	}
}
