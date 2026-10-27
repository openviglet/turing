<?php
/**
 * The document a post becomes and the payloads that send or remove it.
 *
 * @package Viglet_Dumont
 */

namespace Viglet\Dumont\Tests;

use Brain\Monkey\Functions;
use Viglet_Dumont_Indexer;
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
		( new Viglet_Dumont_Indexer( $this->settings() ) )->index_post( $this->post );

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
		( new Viglet_Dumont_Indexer( $this->settings() ) )->index_post( $this->post );

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

		( new Viglet_Dumont_Indexer( $settings ) )->index_post( $this->post );

		$attributes = $this->body()['snJobItems'][0]['attributes'];
		$this->assertArrayNotHasKey( 'categories', $attributes );
		$this->assertArrayNotHasKey( 'section', $attributes );
		$this->assertArrayNotHasKey( 'tags', $attributes );
	}

	public function test_a_declared_custom_field_is_indexed_only_when_it_has_a_value(): void {
		Functions\when( 'get_post_meta' )->alias(
			static fn ( $id, $key ) => 'deadline' === $key ? '2026-11-30' : ''
		);

		( new Viglet_Dumont_Indexer( $this->settings( array( 'index_custom_fields' => 'deadline, salary' ) ) ) )
			->index_post( $this->post );

		$attributes = $this->body()['snJobItems'][0]['attributes'];
		$this->assertSame( '2026-11-30', $attributes['deadline'] );
		$this->assertArrayNotHasKey( 'salary', $attributes );
	}

	public function test_deindexing_a_post_sends_one_delete_item_for_its_id(): void {
		( new Viglet_Dumont_Indexer( $this->settings() ) )->deindex_post( 42 );

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
		( new Viglet_Dumont_Indexer( $this->settings( array( 'site_key' => 'FESPSP Projetos' ) ) ) )->deindex_post( 42 );

		$this->assertSame( 'fespsp-projetos:post:42', $this->body()['snJobItems'][0]['attributes']['id'] );
	}

	public function test_a_subdirectory_site_derives_its_key_from_host_and_path(): void {
		Functions\when( 'home_url' )->justReturn( 'https://example.org:8080/projetos/' );

		$this->assertSame( 'example.org-projetos', ( new Viglet_Dumont_Indexer( $this->settings() ) )->site_key() );
	}

	/**
	 * Two sites share one SN site: delete-all names this site's key and the plugin's source,
	 * in one DELETE by fields (Turing T1112), and lists nothing first.
	 */
	public function test_delete_all_removes_only_this_site_keys_documents(): void {
		$deleted = ( new Viglet_Dumont_Indexer( $this->settings( array( 'site_key' => 'site-a' ) ) ) )->delete_all();

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

		$this->assertFalse( ( new Viglet_Dumont_Indexer( $this->settings() ) )->delete_all() );
	}

	public function test_a_legacy_id_keeps_the_pre_site_key_shape(): void {
		Functions\when( 'is_multisite' )->justReturn( true );
		Functions\when( 'get_current_blog_id' )->justReturn( 3 );

		$method = new \ReflectionMethod( Viglet_Dumont_Indexer::class, 'legacy_doc_id' );

		$this->assertSame( '3-42', $method->invoke( new Viglet_Dumont_Indexer( $this->settings() ), 42 ) );
	}
}
