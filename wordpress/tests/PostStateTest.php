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

	/**
	 * Answer each Turing path with its body; anything else is a 404.
	 *
	 * @param array<string, string> $bodies Path fragment to JSON body.
	 */
	private function turing( array $bodies ): void {
		Functions\when( 'wp_strip_all_tags' )->alias( 'strip_tags' );
		Functions\when( 'wp_remote_request' )->alias(
			function ( string $url, array $args ) use ( $bodies ) {
				$this->requests[] = array( $url, $args );
				foreach ( $bodies as $path => $body ) {
					if ( str_contains( $url, $path ) ) {
						return array(
							'response' => array( 'code' => 200 ),
							'body'     => $body,
						);
					}
				}
				return array(
					'response' => array( 'code' => 404 ),
					'body'     => '',
				);
			}
		);
	}

	public function test_assist_compares_an_indexed_post_by_turings_similar_and_duplicate_searches(): void {
		$this->turing(
			array(
				'/search/similar'    => json_encode(
					array(
						array(
							'id'    => 'site.example.org:post:7',
							'title' => 'Workshop on <mark>facets</mark>',
							'url'   => 'https://site.example.org/?p=7',
						),
						array(
							'id'    => 'site.example.org:post:42',
							'title' => 'This post',
							'url'   => 'https://site.example.org/?p=42',
						),
					)
				),
				'/search/duplicates' => json_encode(
					array(
						'duplicate' => true,
						'members'   => array(
							array(
								'id'    => 'site.example.org:post:42',
								'title' => 'This post',
								'url'   => 'https://site.example.org/?p=42',
							),
							array(
								'id'    => 'site.example.org:post:9',
								'title' => 'The same, again',
								'url'   => 'https://site.example.org/?p=9',
							),
						),
					)
				),
			)
		);

		$assist = ( new Viglet_Dumont_Post_State( $this->settings() ) )->assist( $this->post );

		$this->assertSame( 'similar', $assist['basis'] );
		$this->assertSame(
			array(
				array(
					'title' => 'Workshop on facets',
					'url'   => 'https://site.example.org/?p=7',
				),
			),
			$assist['links'],
			'the post itself is never suggested, and the highlight is dropped'
		);
		$this->assertSame( 'The same, again', $assist['covered'][0]['title'] );
		$this->assertStringContainsString( 'id=site.example.org%3Apost%3A42', $this->requests[0][0] );
	}

	public function test_assist_compares_a_draft_not_yet_indexed_by_its_title(): void {
		$this->post->post_title = 'Workshop on facets';
		$this->turing(
			array(
				'/search?' => json_encode(
					array(
						'results' => array(
							'document' => array(
								array(
									'fields' => array(
										'id'    => 'site.example.org:post:7',
										'title' => 'Workshop on facets',
										'url'   => 'https://site.example.org/?p=7',
									),
								),
							),
						),
					)
				),
			)
		);

		$assist = ( new Viglet_Dumont_Post_State( $this->settings() ) )->assist( $this->post );

		$this->assertSame( 'title', $assist['basis'] );
		$this->assertSame( 'https://site.example.org/?p=7', $assist['covered'][0]['url'] );
		$this->assertSame( $assist['covered'], $assist['links'] );
		$search = end( $this->requests )[0];
		$this->assertStringContainsString( 'q=Workshop+on+facets', $search );
		$this->assertStringContainsString( 'fq%5B%5D=site_key%3Asite.example.org', $search );
	}

	public function test_an_excerpt_is_drafted_from_the_saved_post_in_the_sites_language(): void {
		$this->post->post_title   = 'Research grant';
		$this->post->post_content = '<p>Applications close on 14 March.</p>[gallery]';
		Functions\stubs(
			array(
				'strip_shortcodes' => static fn ( $text ) => str_replace( '[gallery]', '', $text ),
				'get_locale'       => 'pt_BR',
			)
		);
		$this->turing(
			array(
				'/api/v2/summary' => json_encode(
					array(
						'success' => true,
						'content' => '"As inscriÃ§Ãµes fecham em 14 de marÃ§o."',
					)
				),
			)
		);

		$state = new Viglet_Dumont_Post_State( $this->settings() );
		$draft = $state->draft_excerpt( $this->post );

		$this->assertSame(
			array(
				'excerpt' => 'As inscriÃ§Ãµes fecham em 14 de marÃ§o.',
				'error'   => '',
			),
			$draft,
			'the quotes a model wraps a sentence in are dropped'
		);
		[ $url, $args ] = $this->requests[0];
		$body           = json_decode( $args['body'], true );
		$this->assertSame( 'https://turing.example.org/api/v2/summary', $url );
		$this->assertSame( "Research grant\n\nApplications close on 14 March.", $body['data'] );
		$this->assertStringContainsString( 'pt_BR', $body['systemPrompt'] );
		$this->assertSame( 'secret', $args['headers']['Key'] );

		$state->draft_excerpt( $this->post, true );
		$this->assertStringContainsString( 'regenerate=true', $this->requests[1][0] );
		$this->assertSame( $body['cacheKey'], json_decode( $this->requests[1][1]['body'], true )['cacheKey'] );
	}

	public function test_an_empty_post_or_a_failed_model_gives_a_reason_and_no_draft(): void {
		Functions\stubs(
			array(
				'strip_shortcodes' => static fn ( $text ) => $text,
				'get_locale'       => 'en_US',
			)
		);
		$this->turing(
			array(
				'/api/v2/summary' => json_encode(
					array(
						'success' => false,
						'error'   => 'No default LLM is configured',
					)
				),
			)
		);
		$state = new Viglet_Dumont_Post_State( $this->settings() );

		$empty = $state->draft_excerpt( $this->post );
		$this->assertSame( '', $empty['excerpt'] );
		$this->assertNotSame( '', $empty['error'] );
		$this->assertSame( array(), $this->requests, 'an empty post costs no call' );

		$this->post->post_content = 'Some text.';
		$this->assertSame(
			array(
				'excerpt' => '',
				'error'   => 'No default LLM is configured',
			),
			$state->draft_excerpt( $this->post )
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

	public function test_the_saved_post_is_judged_against_each_audience_persona_of_the_site(): void {
		$this->post->post_title   = 'Research grant';
		$this->post->post_content = '<p>Applications close on 14 March.</p>';
		Functions\when( 'strip_shortcodes' )->returnArg();
		$this->turing(
			array(
				'/content-fit' => json_encode(
					array(
						'personaId' => 'p1',
						'fitScore'  => 72.6,
						'summary'   => 'Clear deadline, little on eligibility.',
						'fits'      => array( 'States the deadline.' ),
						'misfits'   => array(
							array(
								'span'       => 'Applications close on 14 March.',
								'reason'     => 'missing-context',
								'suggestion' => 'Applications from early-career researchers close on 14 March.',
							),
						),
					)
				),
				'/personas'    => json_encode(
					array(
						array(
							'id'   => 'p1',
							'name' => 'Grant applicant',
						),
					)
				),
			)
		);

		$fit = ( new Viglet_Dumont_Post_State( $this->settings() ) )->audience_fit( $this->post );

		$this->assertSame(
			array(
				'fits'  => array(
					array(
						'persona' => 'Grant applicant',
						'score'   => 73,
						'summary' => 'Clear deadline, little on eligibility.',
						'misfits' => array(
							array(
								'reason'     => 'Missing context',
								'span'       => 'Applications close on 14 March.',
								'suggestion' => 'Applications from early-career researchers close on 14 March.',
							),
						),
					),
				),
				'error' => '',
			),
			$fit
		);
		$this->assertSame( 'https://turing.example.org/api/sn/Sample+Site/persona/p1/content-fit', str_replace( '%20', '+', $this->requests[1][0] ) );
		$this->assertSame( "Research grant\n\nApplications close on 14 March.", json_decode( $this->requests[1][1]['body'], true )['content'] );
	}

	public function test_a_site_without_personas_offers_no_audience_fit(): void {
		$this->turing( array( '/personas' => '[]' ) );

		$this->assertSame( array(), ( new Viglet_Dumont_Post_State( $this->settings() ) )->personas() );
	}
}
