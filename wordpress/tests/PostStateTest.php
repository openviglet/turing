<?php
/**
 * What the Search column and the editor panel say about a post.
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Brain\Monkey\Functions;
use Viglet_Dumont_Mapping;
use Viglet_Dumont_Post_State;
use Viglet_Dumont_Queue;
use WP_Post;

final class PostStateTest extends PluginTestCase {

	/** The options table, in memory. */
	private array $options = array();

	/** Post meta of the post, by key. */
	private array $meta = array();

	private WP_Post $post;

	protected function setUp(): void {
		parent::setUp();

		$this->post = new WP_Post(
			array(
				'ID'          => 42,
				'post_type'   => 'post',
				'post_status' => 'publish',
			)
		);

		Functions\when( 'get_option' )->alias( fn ( $name, $fallback = false ) => $this->options[ $name ] ?? $fallback );
		Functions\when( 'get_post_meta' )->alias( fn ( $id, $key ) => $this->meta[ $key ] ?? '' );
		Functions\stubs(
			array(
				'wp_cache_delete'     => true,
				'wp_is_post_revision' => false,
				'human_time_diff'     => '2 mins',
			)
		);
	}

	private function describe(): array {
		return ( new Viglet_Dumont_Post_State( $this->settings() ) )->describe( $this->post );
	}

	public function test_a_post_sent_without_error_reads_indexed_and_when(): void {
		$this->meta[ Viglet_Dumont_Queue::STATE_META ] = array(
			'indexed_at' => time() - 120,
			'error'      => '',
		);

		$this->assertSame(
			array(
				'state'  => 'indexed',
				'label'  => 'Indexed 2 mins ago',
				'detail' => '',
			),
			$this->describe()
		);
	}

	public function test_a_waiting_job_reads_queued_with_its_last_error(): void {
		$this->options[ Viglet_Dumont_Queue::OPTION ] = array(
			42 => array(
				'action' => 'index',
				'failed' => false,
				'error'  => 'cURL error 7',
			),
		);

		$state = $this->describe();

		$this->assertSame( 'queued', $state['state'] );
		$this->assertSame( 'cURL error 7', $state['detail'] );
	}

	public function test_a_job_that_stopped_retrying_reads_failed(): void {
		$this->options[ Viglet_Dumont_Queue::OPTION ] = array(
			42 => array(
				'action' => 'index',
				'failed' => true,
				'error'  => 'Import failed with HTTP 401',
			),
		);

		$this->assertSame( 'failed', $this->describe()['state'] );
	}

	public function test_an_excluded_post_reads_excluded(): void {
		$this->meta[ Viglet_Dumont_Mapping::EXCLUDE_META ] = '1';

		$this->assertSame( 'excluded', $this->describe()['state'] );
	}

	public function test_a_draft_reads_not_published(): void {
		$this->post->post_status = 'draft';

		$this->assertSame( 'unpublished', $this->describe()['state'] );
	}

	public function test_a_published_post_with_no_record_reads_not_indexed_yet(): void {
		$this->assertSame( 'not_indexed', $this->describe()['state'] );
	}
}
