<?php
/**
 * A save only queues the post; the worker sends the queue with retry and backoff.
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Brain\Monkey\Functions;
use Viglet_Dumont_Queue;
use WP_Post;

final class QueueTest extends PluginTestCase {

	/** The options table, in memory. */
	private array $options = array();

	/** Post meta, as [post id][key] => value. */
	private array $meta = array();

	/** The transients, in memory. */
	private array $transients = array();

	/** The posts get_post finds, by id. */
	private array $posts = array();

	/** The cron events scheduled, each as [timestamp, hook]. */
	private array $scheduled = array();

	/** The status each request answers with, in order; $this->status once they run out. */
	private array $statuses = array();

	/** Run before each request is answered, to act as a save arriving mid-send. */
	private $during_send = null;

	protected function setUp(): void {
		parent::setUp();

		foreach ( array( 1, 2 ) as $id ) {
			$this->posts[ $id ] = new WP_Post(
				array(
					'ID'           => $id,
					'post_author'  => '1',
					'post_title'   => "Post $id",
					'post_content' => 'Body',
					'post_type'    => 'post',
					'post_status'  => 'publish',
				)
			);
		}

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
				'wp_cache_delete'            => true,
			)
		);

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
		Functions\when( 'get_post' )->alias( fn ( $id ) => $this->posts[ $id ] ?? null );
		Functions\when( 'get_post_meta' )->alias( fn ( $id, $key ) => $this->meta[ $id ][ $key ] ?? '' );
		Functions\when( 'update_post_meta' )->alias(
			function ( $id, $key, $value ) {
				$this->meta[ $id ][ $key ] = $value;
				return true;
			}
		);
		Functions\when( 'wp_next_scheduled' )->alias(
			function () {
				$times = array_column( $this->scheduled, 0 );
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
			function ( $timestamp ) {
				$this->scheduled = array_values( array_filter( $this->scheduled, static fn ( $event ) => $event[0] !== $timestamp ) );
				return true;
			}
		);
		Functions\when( 'wp_remote_request' )->alias(
			function ( string $url, array $args ) {
				$this->requests[] = array( $url, $args );
				if ( $this->during_send ) {
					( $this->during_send )();
				}
				return array(
					'response' => array( 'code' => array_shift( $this->statuses ) ?? $this->status ),
					'body'     => '',
				);
			}
		);
	}

	private function queue(): Viglet_Dumont_Queue {
		return new Viglet_Dumont_Queue( $this->settings( array( 'auto_deindex' => 1 ) ) );
	}

	private function actions( int $request = 0 ): array {
		return array_column( $this->body( $request )['snJobItems'], 'turSNJobAction' );
	}

	public function test_saving_a_post_twice_queues_one_job_and_sends_nothing(): void {
		$this->queue()->enqueue( 1, 'index' );
		$this->queue()->enqueue( 1, 'delete' );

		$this->assertSame( array(), $this->requests );
		$jobs = $this->queue()->jobs();
		$this->assertCount( 1, $jobs );
		$this->assertSame( 'delete', $jobs[1]['action'] );
		$this->assertCount( 1, $this->scheduled );
		$this->assertSame( Viglet_Dumont_Queue::CRON_HOOK, $this->scheduled[0][1] );
	}

	public function test_the_worker_sends_the_queue_in_one_import_and_empties_it(): void {
		$this->queue()->enqueue( 1, 'index' );
		$this->queue()->enqueue( 2, 'delete' );

		$result = $this->queue()->process();

		$this->assertCount( 1, $this->requests );
		$this->assertSame( 'https://turing.example.org/api/sn/import', $this->requests[0][0] );
		$this->assertSame( array( 'CREATE', 'DELETE' ), $this->actions() );
		$this->assertSame( 2, $result['sent'] );
		$this->assertSame( 0, $result['remaining'] );
		$this->assertArrayNotHasKey( Viglet_Dumont_Queue::OPTION, $this->options );
		$this->assertIsInt( $this->meta[1][ Viglet_Dumont_Queue::STATE_META ]['indexed_at'] );
		$this->assertSame( '', $this->meta[1][ Viglet_Dumont_Queue::STATE_META ]['error'] );
	}

	public function test_an_outage_keeps_the_job_and_backs_it_off(): void {
		$this->status = 503;
		$this->queue()->enqueue( 1, 'index' );
		// WP-Cron unschedules a single event before it runs its hook.
		$this->scheduled = array();

		$before = time();
		$result = $this->queue()->process();

		$this->assertSame( 1, $result['failed'] );
		$this->assertStringContainsString( '503', $result['errors'][1] );
		$job = $this->queue()->jobs()[1];
		$this->assertSame( 1, $job['attempts'] );
		$this->assertGreaterThanOrEqual( $before + Viglet_Dumont_Queue::BASE_DELAY, $job['next_at'] );
		$this->assertStringContainsString( '503', $job['error'] );
		$this->assertFalse( $job['failed'] );
		$this->assertStringContainsString( '503', $this->meta[1][ Viglet_Dumont_Queue::STATE_META ]['error'] );
		$this->assertSame( array( array( $job['next_at'], Viglet_Dumont_Queue::CRON_HOOK ) ), $this->scheduled );
	}

	public function test_a_job_waiting_out_its_backoff_is_not_sent_until_the_cli_forces_it(): void {
		$this->status = 503;
		$this->queue()->enqueue( 1, 'index' );
		$this->queue()->process();

		$this->queue()->process();
		$this->assertCount( 1, $this->requests );

		$this->queue()->process( true );
		$this->assertCount( 2, $this->requests );
	}

	public function test_a_refused_batch_is_resent_item_by_item_so_one_bad_post_does_not_block_the_rest(): void {
		$this->statuses = array( 400, 200, 400 );
		$this->queue()->enqueue( 1, 'index' );
		$this->queue()->enqueue( 2, 'index' );

		$result = $this->queue()->process();

		$this->assertCount( 3, $this->requests );
		$this->assertSame( 1, $result['sent'] );
		$this->assertSame( 1, $result['failed'] );
		$this->assertSame( array( 2 ), array_keys( $this->queue()->jobs() ) );
	}

	public function test_a_server_error_fails_the_batch_without_resending_each_item(): void {
		$this->status = 500;
		$this->queue()->enqueue( 1, 'index' );
		$this->queue()->enqueue( 2, 'index' );

		$this->queue()->process();

		$this->assertCount( 1, $this->requests );
		$this->assertCount( 2, $this->queue()->jobs() );
	}

	public function test_a_job_stops_retrying_after_the_last_attempt_until_it_is_replayed(): void {
		$this->status = 503;
		$this->queue()->enqueue( 1, 'index' );
		for ( $i = 0; $i < Viglet_Dumont_Queue::MAX_ATTEMPTS; $i++ ) {
			$this->queue()->process( true );
		}

		$this->assertTrue( $this->queue()->jobs()[1]['failed'] );
		$this->assertSame(
			array(
				'pending' => 0,
				'failed'  => 1,
			),
			$this->queue()->counts()
		);

		$this->queue()->process( true );
		$this->assertCount( Viglet_Dumont_Queue::MAX_ATTEMPTS, $this->requests );

		$this->assertSame( 1, $this->queue()->retry_failed() );
		$this->status = 200;
		$this->queue()->process();
		$this->assertSame( array(), $this->queue()->jobs() );
	}

	public function test_an_index_job_for_a_post_unpublished_since_is_sent_as_a_delete(): void {
		$this->queue()->enqueue( 1, 'index' );
		$this->posts[1]->post_status = 'draft';

		$this->queue()->process();

		$this->assertSame( array( 'DELETE' ), $this->actions() );
	}

	public function test_a_save_that_arrives_while_the_job_is_being_sent_is_kept(): void {
		$this->queue()->enqueue( 1, 'index' );
		$this->during_send = function () {
			$this->during_send = null;
			$this->queue()->enqueue( 1, 'delete' );
		};

		$this->queue()->process();

		$jobs = $this->queue()->jobs();
		$this->assertCount( 1, $jobs );
		$this->assertSame( 'delete', $jobs[1]['action'] );
	}

	public function test_a_second_worker_sends_nothing_while_the_first_holds_the_lock(): void {
		$this->queue()->enqueue( 1, 'index' );
		$this->transients[ Viglet_Dumont_Queue::LOCK ] = 1;

		$result = $this->queue()->process();

		$this->assertTrue( $result['locked'] );
		$this->assertSame( array(), $this->requests );
	}
}
