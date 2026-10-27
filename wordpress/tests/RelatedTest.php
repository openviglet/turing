<?php
/**
 * Related content from Turing's similar-documents search (D177).
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Brain\Monkey\Functions;
use Viglet_Dumont_Related;
use WP_Post;

final class RelatedTest extends PluginTestCase {

	/** The posts WordPress holds, by id. */
	private array $posts = array();

	/** What Turing answers to /search/similar, as document ids. */
	private array $similar = array();

	/** The transients set, by key. */
	private array $transients = array();

	protected function setUp(): void {
		parent::setUp();

		foreach ( array(
			1 => array( 'post_type' => 'post' ),
			2 => array( 'post_type' => 'post' ),
			3 => array( 'post_type' => 'page' ),
			4 => array(
				'post_type'   => 'post',
				'post_status' => 'draft',
			),
			5 => array(
				'post_type'     => 'post',
				'post_password' => 'secret',
			),
			6 => array( 'post_type' => 'post' ),
		) as $id => $fields ) {
			$this->posts[ $id ] = new WP_Post( array( 'ID' => $id ) + $fields );
		}
		Functions\when( 'get_post' )->alias( fn ( $id ) => $this->posts[ $id ] ?? null );
		Functions\when( 'get_transient' )->alias( fn ( $key ) => $this->transients[ $key ] ?? false );
		Functions\when( 'set_transient' )->alias(
			function ( $key, $value ) {
				$this->transients[ $key ] = $value;
				return true;
			}
		);
		Functions\when( 'wp_remote_request' )->alias(
			function ( string $url, array $args ) {
				$this->requests[] = array( $url, $args );
				return array(
					'response' => array( 'code' => $this->status ),
					'body'     => json_encode( array_map( static fn ( $id ) => array( 'id' => $id ), $this->similar ) ),
				);
			}
		);
	}

	private function related(): Viglet_Dumont_Related {
		return new Viglet_Dumont_Related( $this->settings( array( 'site_key' => 'site-a' ) ) );
	}

	/** @return int[] The ids of the posts returned. */
	private function ids( array $posts ): array {
		return array_map( static fn ( WP_Post $post ) => $post->ID, $posts );
	}

	public function test_only_this_sites_published_public_posts_in_turings_order_never_the_post_itself(): void {
		$this->similar = array( 'site-a:post:1', 'site-a:post:2', 'site-b:post:6', 'site-a:post:4', 'site-a:post:5', 'site-a:post:3', 'site-a:post:6', 'site-a:term:9' );

		$this->assertSame( array( 1, 3, 6 ), $this->ids( $this->related()->posts( 2, 5 ) ) );
		$this->assertStringContainsString( '/search/similar?', $this->requests[0][0] );
		$this->assertStringContainsString( 'id=site-a%3Apost%3A2', $this->requests[0][0] );
		$this->assertStringNotContainsString( 'mode=', $this->requests[0][0], 'Turing chooses unless told' );
		$this->assertStringContainsString( 'fq%5B%5D=site_key%3Asite-a', $this->requests[0][0], 'Turing keeps to this site' );
		$this->assertStringContainsString( 'rows=15', $this->requests[0][0], 'three asked for each one shown' );
	}

	public function test_same_type_and_count_are_kept(): void {
		$this->similar = array( 'site-a:post:3', 'site-a:post:6', 'site-a:post:1' );

		$this->assertSame( array( 6, 1 ), $this->ids( $this->related()->posts( 2, 5, true ) ) );
		$this->assertSame( array( 3 ), $this->ids( $this->related()->posts( 2, 1 ) ) );
	}

	public function test_turing_is_asked_once_per_post_and_mode(): void {
		$this->similar = array( 'site-a:post:6' );

		$this->related()->posts( 2, 3 );
		$this->related()->posts( 2, 3 );
		$this->assertCount( 1, $this->requests );

		$this->related()->posts( 2, 3, false, 'MLT' );
		$this->assertCount( 2, $this->requests );
		$this->assertStringContainsString( 'mode=MLT', $this->requests[1][0] );
	}

	public function test_a_refused_semantic_question_is_asked_again_by_shared_words(): void {
		$this->similar = array( 'site-a:post:6' );
		Functions\when( 'wp_remote_request' )->alias(
			function ( string $url, array $args ) {
				$this->requests[] = array( $url, $args );
				$vector           = str_contains( $url, 'mode=VECTOR' );
				return array(
					'response' => array( 'code' => $vector ? 500 : 200 ),
					'body'     => $vector ? '' : '[{"id":"site-a:post:6"}]',
				);
			}
		);

		$this->assertSame( array( 6 ), $this->ids( $this->related()->posts( 2, 3, false, 'VECTOR' ) ) );
		$this->assertStringContainsString( 'mode=MLT', $this->requests[1][0] );
	}

	public function test_turing_failing_or_not_knowing_the_post_gives_nothing(): void {
		$this->status = 500;
		$this->assertSame( array(), $this->related()->posts( 2, 3 ) );
		$this->assertSame( array(), $this->transients, 'a failure is not cached' );

		$this->status = 404;
		$this->assertSame( array(), $this->related()->posts( 1, 3 ) );
		$this->assertSame( array(), $this->related()->posts( 99, 3 ), 'no such post' );
	}

	/**
	 * The post being viewed, of a chosen type, with markup parsed from `$parsed` and template
	 * parts read from `$parts`: what append() sees on a block theme.
	 *
	 * @param array $parsed Each markup string's parsed blocks.
	 * @param array $parts  Each template part's markup, by `theme//slug`.
	 */
	private function viewing( array $parsed = array(), array $parts = array(), bool $in_content = false ): void {
		Functions\when( 'is_singular' )->justReturn( true );
		Functions\when( 'in_the_loop' )->justReturn( true );
		Functions\when( 'is_main_query' )->justReturn( true );
		Functions\when( 'has_block' )->justReturn( $in_content );
		Functions\when( 'get_stylesheet' )->justReturn( 'tarnfell' );
		Functions\when( 'parse_blocks' )->alias( static fn ( $markup ) => $parsed[ $markup ] ?? array() );
		Functions\when( 'get_block_template' )->alias(
			static fn ( $id ) => isset( $parts[ $id ] ) ? (object) array( 'content' => $parts[ $id ] ) : null
		);
		Functions\when( 'render_block' )->justReturn( '<related/>' );
	}

	private function block( string $name, array $attrs = array(), array $inner = array() ): array {
		return array(
			'blockName'   => $name,
			'attrs'       => $attrs,
			'innerBlocks' => $inner,
		);
	}

	private function append( ?string $template ): string {
		$GLOBALS['_wp_current_template_content'] = $template;
		try {
			return ( new Viglet_Dumont_Related( $this->settings( array( 'related_after' => array( 'post' ) ) ) ) )->append( '<p>Body</p>' );
		} finally {
			unset( $GLOBALS['_wp_current_template_content'] );
		}
	}

	public function test_a_classic_theme_or_a_template_without_the_block_gets_it_appended(): void {
		$this->viewing( array( 'single' => array( $this->block( 'core/post-content' ) ) ) );

		$this->assertSame( '<p>Body</p><related/>', $this->append( null ), 'no block template' );
		$this->assertSame( '<p>Body</p><related/>', $this->append( 'single' ) );
	}

	public function test_content_that_holds_the_block_gets_no_second_one(): void {
		$this->viewing( array(), array(), true );

		$this->assertSame( '<p>Body</p>', $this->append( null ) );
	}

	public function test_a_template_that_places_the_block_gets_no_second_one(): void {
		// Tarnfell's single.html: the block inside a group after the post content.
		$this->viewing(
			array(
				'single' => array(
					$this->block( 'core/post-content' ),
					$this->block( 'core/group', array(), array( $this->block( 'viglet-dumont/related' ) ) ),
				),
			)
		);

		$this->assertSame( '<p>Body</p>', $this->append( 'single' ) );
	}

	public function test_a_template_part_that_places_the_block_counts_as_the_template(): void {
		$this->viewing(
			array(
				'single'     => array( $this->block( 'core/template-part', array( 'slug' => 'after-post' ) ) ),
				'after-post' => array( $this->block( 'viglet-dumont/related' ) ),
				'footer'     => array( $this->block( 'core/paragraph' ) ),
				'loop'       => array( $this->block( 'core/template-part', array( 'slug' => 'loop' ) ) ),
				'named'      => array(
					$this->block(
						'core/template-part',
						array(
							'slug'  => 'footer',
							'theme' => 'kin',
						)
					),
				),
				'circular'   => array( $this->block( 'core/template-part', array( 'slug' => 'loop' ) ) ),
			),
			array(
				'tarnfell//after-post' => 'after-post',
				'tarnfell//loop'       => 'loop',
				'kin//footer'          => 'footer',
			)
		);

		$this->assertSame( '<p>Body</p>', $this->append( 'single' ) );
		$this->assertSame( '<p>Body</p><related/>', $this->append( 'named' ), 'a part of the theme it names, without the block' );
		$this->assertSame( '<p>Body</p><related/>', $this->append( 'circular' ), 'a part including itself is read once' );
	}
}
