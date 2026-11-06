<?php
/**
 * The document a post becomes and the payloads that send or remove it.
 *
 * @package Viglet_Turing
 */

namespace Viglet\Turing\Tests;

use Brain\Monkey\Functions;
use Viglet_Turing_Indexer;
use WP_Post;

final class IndexerTest extends PluginTestCase {

	private WP_Post $post;

	protected function setUp(): void {
		parent::setUp();

		$this->post = new WP_Post(
			array(
				'ID'           => 42,
				'post_author'  => '7',
				'post_title'   => 'Open call',
				'post_content' => "<p>Applications   close\n on <b>Friday</b>.</p>",
				'post_type'    => 'post',
			)
		);

		Functions\stubs(
			array(
				'strip_shortcodes'           => static fn ( $text ) => $text,
				'wp_strip_all_tags'          => static fn ( $text ) => strip_tags( $text ),
				'wp_trim_words'              => static fn ( $text ) => $text,
				'get_permalink'              => 'https://site.example.org/open-call/',
				'get_post_time'              => '2026-10-01T09:00:00+00:00',
				'get_post_modified_time'     => '2026-10-02T09:00:00+00:00',
				'get_bloginfo'               => 'Institutional',
				'get_the_post_thumbnail_url' => false,
				'wp_get_post_categories'     => array( 'News', 'Calls' ),
				'wp_get_post_tags'           => array( 'jobs' ),
				'get_post_meta'              => '',
				'is_multisite'               => false,
				'is_sticky'                  => true,
			)
		);
		Functions\expect( 'get_the_author_meta' )->with( 'display_name', 7 )->andReturn( 'Ana' );
	}

	public function test_a_post_becomes_one_create_item_posted_to_import(): void {
		( new Viglet_Turing_Indexer( $this->settings() ) )->index_post( $this->post );

		$this->assertCount( 1, $this->requests );
		$this->assertSame( 'https://turing.example.org/api/sn/import', $this->requests[0][0] );
		$this->assertSame( 'POST', $this->requests[0][1]['method'] );

		$items = $this->body()['snJobItems'];
		$this->assertCount( 1, $items );
		$this->assertSame( 'CREATE', $items[0]['turSNJobAction'] );
		$this->assertSame( 'pt_BR', $items[0]['locale'] );
		$this->assertSame( array( 'Sample Site' ), $items[0]['siteNames'] );
	}

	public function test_the_document_carries_the_post_fields(): void {
		( new Viglet_Turing_Indexer( $this->settings() ) )->index_post( $this->post );

		$attributes = $this->body()['snJobItems'][0]['attributes'];
		$this->assertSame( 'site.example.org:post:42', $attributes['id'] );
		$this->assertSame( 'site.example.org', $attributes['site_key'] );
		$this->assertSame( 'Open call', $attributes['title'] );
		$this->assertSame( 'Applications close on Friday.', $attributes['text'] );
		$this->assertSame( 'https://site.example.org/open-call/', $attributes['url'] );
		$this->assertSame( 'post', $attributes['type'] );
		$this->assertSame( 'Ana', $attributes['author'] );
		$this->assertSame( '2026-10-01T09:00:00+00:00', $attributes['publication_date'] );
		$this->assertSame( '2026-10-02T09:00:00+00:00', $attributes['modification_date'] );
		$this->assertSame( 'Institutional', $attributes['site'] );
		$this->assertSame( '', $attributes['image'] );
		$this->assertSame( 'true', $attributes['sticky'] );
		$this->assertSame( array( 'News', 'Calls' ), $attributes['categories'] );
		$this->assertSame( 'News', $attributes['section'] );
		$this->assertSame( array( 'jobs' ), $attributes['tags'] );
		$this->assertSame( 'wordpress', $attributes['source_apps'] );
	}

	public function test_categories_and_tags_are_left_out_when_disabled(): void {
		$settings = $this->settings(
			array(
				'index_categories' => 0,
				'index_tags'       => 0,
			)
		);

		( new Viglet_Turing_Indexer( $settings ) )->index_post( $this->post );

		$attributes = $this->body()['snJobItems'][0]['attributes'];
		$this->assertArrayNotHasKey( 'categories', $attributes );
		$this->assertArrayNotHasKey( 'section', $attributes );
		$this->assertArrayNotHasKey( 'tags', $attributes );
	}

	public function test_a_declared_custom_field_is_indexed_only_when_it_has_a_value(): void {
		Functions\when( 'get_post_meta' )->alias(
			static fn ( $id, $key ) => 'deadline' === $key ? '2026-11-30' : ''
		);

		( new Viglet_Turing_Indexer( $this->settings( array( 'index_custom_fields' => 'deadline, salary' ) ) ) )
			->index_post( $this->post );

		$attributes = $this->body()['snJobItems'][0]['attributes'];
		$this->assertSame( '2026-11-30', $attributes['deadline'] );
		$this->assertArrayNotHasKey( 'salary', $attributes );
	}

	public function test_deindexing_a_post_sends_one_delete_item_for_its_id(): void {
		( new Viglet_Turing_Indexer( $this->settings() ) )->deindex_post( 42 );

		$item = $this->body()['snJobItems'][0];
		$this->assertSame( 'DELETE', $item['turSNJobAction'] );
		$this->assertSame(
			array(
				'id'          => 'site.example.org:post:42',
				'source_apps' => 'wordpress',
			),
			$item['attributes']
		);
	}

	public function test_a_configured_site_key_starts_the_id(): void {
		( new Viglet_Turing_Indexer( $this->settings( array( 'site_key' => 'Example Projetos' ) ) ) )->deindex_post( 42 );

		$this->assertSame( 'example-projetos:post:42', $this->body()['snJobItems'][0]['attributes']['id'] );
	}

	public function test_a_subdirectory_site_derives_its_key_from_host_and_path(): void {
		Functions\when( 'home_url' )->justReturn( 'https://example.org:8080/projetos/' );

		$this->assertSame( 'example.org-projetos', ( new Viglet_Turing_Indexer( $this->settings() ) )->site_key() );
	}

	/**
	 * One seam names what a search reads (D303): the own key, until Turing lets two values of
	 * site_key be alternatives (T1223) and the sibling keys join it.
	 */
	public function test_a_search_is_scoped_to_the_own_site_key(): void {
		$indexer = new Viglet_Turing_Indexer( $this->settings( array( 'site_key' => 'site-a' ) ) );

		$this->assertSame( 'site_key:site-a', $indexer->own_filter() );
		$this->assertSame( array( 'site_key:site-a' ), $indexer->scope_filters() );
		// One key needs no operator: the request is what it always was.
		$this->assertSame( array( 'fq[]' => array( 'site_key:site-a', 'type:post' ) ), $indexer->scope_query( array( 'type:post' ) ) );
	}

	/**
	 * A site that also searches its siblings (D303) reads its own key and theirs as
	 * alternatives, while the filter every write and own-only read takes stays its own.
	 */
	public function test_the_scope_holds_the_siblings_as_alternatives_and_the_own_filter_does_not(): void {
		$indexer = new Viglet_Turing_Indexer(
			$this->settings(
				array(
					'site_key'         => 'site-a',
					'search_site_keys' => 'site-b | Site B, site-c',
				)
			)
		);

		$this->assertSame( array( 'site-a', 'site-b', 'site-c' ), $indexer->scope_keys() );
		$this->assertSame( array( 'site_key:site-a', 'site_key:site-b', 'site_key:site-c' ), $indexer->scope_filters() );
		$this->assertSame(
			array(
				'fq[]'   => array( 'site_key:site-a', 'site_key:site-b', 'site_key:site-c', 'type:post' ),
				'fqi.op' => 'OR',
			),
			$indexer->scope_query( array( 'type:post' ) )
		);
		$this->assertSame( 'site_key:site-a', $indexer->own_filter() );
		$this->assertSame( 'Site B', $indexer->sibling_of( 'site-b:post:4' )['label'] );
		$this->assertSame( 'site-c', $indexer->sibling_of( 'site-c:post:1' )['label'], 'the label defaults to the key' );
		$this->assertNull( $indexer->sibling_of( 'site-a:post:4' ) );
		$this->assertNull( $indexer->sibling_of( 'site-bb:post:4' ) );
	}

	/**
	 * The site facet narrows the scope to the sites picked; a site_key filter naming a key out
	 * of scope is dropped, so a filter in the URL never widens a search past it.
	 */
	public function test_a_picked_site_narrows_the_scope_and_a_key_out_of_scope_is_dropped(): void {
		$indexer = new Viglet_Turing_Indexer(
			$this->settings(
				array(
					'site_key'         => 'site-a',
					'search_site_keys' => 'site-b, site-c',
				)
			)
		);

		$this->assertSame( array( 'fq[]' => array( 'site_key:site-b', 'type:post' ) ), $indexer->scope_query( array( 'site_key:site-b', 'type:post' ) ) );
		$this->assertSame(
			array(
				'fq[]'   => array( 'site_key:site-a', 'site_key:site-c' ),
				'fqi.op' => 'OR',
			),
			$indexer->scope_query( array( 'site_key:site-a', 'site_key:site-c' ) )
		);
		$this->assertSame(
			array(
				'fq[]'   => array( 'site_key:site-a', 'site_key:site-b', 'site_key:site-c' ),
				'fqi.op' => 'OR',
			),
			$indexer->scope_query( array( 'site_key:elsewhere' ) )
		);
	}

	/**
	 * An empty sibling key is nothing, and a sibling listing this site's own key, or one
	 * listed twice, adds nothing: the scope is the own key alone and has no operator.
	 */
	public function test_an_empty_or_own_or_repeated_sibling_adds_nothing(): void {
		$indexer = new Viglet_Turing_Indexer(
			$this->settings(
				array(
					'site_key'         => 'site-a',
					'search_site_keys' => array( array( 'key' => '' ), array( 'key' => '  ' ), array( 'key' => 'Site-A' ), 'site-a' ),
				)
			)
		);

		$this->assertSame( array(), $indexer->sibling_sites() );
		$this->assertSame( array( 'site_key:site-a' ), $indexer->scope_filters() );
		$this->assertArrayNotHasKey( 'fqi.op', $indexer->scope_query() );
	}

	/** Writes stay on the own key (D303): delete-all and the sweep's listing never name a sibling. */
	public function test_delete_all_and_the_listing_touch_only_the_own_key_with_siblings_listed(): void {
		Functions\when( 'get_transient' )->justReturn( false );
		$indexer = new Viglet_Turing_Indexer(
			$this->settings(
				array(
					'site_key'         => 'site-a',
					'search_site_keys' => 'site-b',
				)
			)
		);

		$indexer->delete_all();
		$indexer->list_site_ids();

		$this->assertNotEmpty( $this->requests );
		foreach ( $this->requests as $request ) {
			$this->assertStringNotContainsString( 'site-b', $request[0] . ( $request[1]['body'] ?? '' ) );
			$this->assertStringNotContainsString( 'fqi.op', $request[0] );
			$this->assertSame( 'secret', $request[1]['headers']['Key'], 'writes keep the key' );
		}
		$imports = array_keys( array_filter( $this->requests, static fn ( $r ) => str_ends_with( $r[0], '/api/sn/import' ) ) );
		$this->assertSame( 'site-a', $this->body( $imports[0] )['snJobItems'][0]['attributes']['site_key'] );
	}

	/**
	 * Two sites share one SN site: delete-all names this site's key and the plugin's source,
	 * in one DELETE by fields (Turing T1112), and lists nothing first.
	 */
	public function test_delete_all_removes_only_this_site_keys_documents(): void {
		$deleted = ( new Viglet_Turing_Indexer( $this->settings( array( 'site_key' => 'site-a' ) ) ) )->delete_all();

		$this->assertTrue( $deleted );
		$imports = array_keys( array_filter( $this->requests, static fn ( $r ) => str_ends_with( $r[0], '/api/sn/import' ) ) );
		$this->assertCount( 1, $imports );
		$this->assertCount( 0, array_filter( $this->requests, static fn ( $r ) => str_contains( $r[0], '/search' ) ), 'nothing listed' );
		$items = $this->body( $imports[0] )['snJobItems'];
		$this->assertCount( 1, $items );
		$this->assertSame( 'DELETE', $items[0]['turSNJobAction'] );
		$this->assertSame( 'site-a', $items[0]['attributes']['site_key'] );
		$this->assertArrayNotHasKey( 'id', $items[0]['attributes'] );
		$this->assertArrayHasKey( 'source_apps', $items[0]['attributes'] );
	}

	public function test_delete_all_reports_a_failed_delete(): void {
		$this->status = 500;

		$this->assertFalse( ( new Viglet_Turing_Indexer( $this->settings() ) )->delete_all() );
	}

	public function test_a_legacy_id_keeps_the_pre_site_key_shape(): void {
		Functions\when( 'is_multisite' )->justReturn( true );
		Functions\when( 'get_current_blog_id' )->justReturn( 3 );

		$method = new \ReflectionMethod( Viglet_Turing_Indexer::class, 'legacy_doc_id' );

		$this->assertSame( '3-42', $method->invoke( new Viglet_Turing_Indexer( $this->settings() ), 42 ) );
	}
}
