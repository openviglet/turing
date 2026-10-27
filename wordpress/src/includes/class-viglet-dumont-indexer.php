<?php
/**
 * Viglet Dumont Indexer
 *
 * Handles indexing WordPress content into Turing using the TurSNJobItems format.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Turns posts into Turing job items and sends them.
 */
class Viglet_Dumont_Indexer {

	/**
	 * The client the job items go through.
	 *
	 * @var Viglet_Dumont_API_Client
	 */
	private Viglet_Dumont_API_Client $client;

	/**
	 * The plugin settings this indexer was built with.
	 *
	 * @var array
	 */
	private array $settings;

	/**
	 * The per-post-type field mapping.
	 *
	 * @var Viglet_Dumont_Mapping
	 */
	private Viglet_Dumont_Mapping $mapping;

	/**
	 * Build an indexer over the given settings, or over the stored ones.
	 *
	 * @param array|null $settings The plugin settings; null reads them from the options table.
	 */
	public function __construct( ?array $settings = null ) {
		$this->settings = $settings ?? viglet_dumont_get_settings();
		$this->client   = new Viglet_Dumont_API_Client( $this->settings );
		$this->mapping  = new Viglet_Dumont_Mapping( $this->settings );
	}

	/**
	 * Send the mapped fields' manifest to Turing when it changed since the last one.
	 *
	 * @param bool $force Send even when the same manifest went through.
	 * @return array{sent: bool, success: bool, message: string}
	 */
	public function provision( bool $force = false ): array {
		return $this->mapping->provision( $this->client, $force );
	}

	/**
	 * Check if a post should be indexed based on settings.
	 *
	 * @param WP_Post $post The candidate post.
	 */
	public function should_index( WP_Post $post ): bool {
		if ( 'publish' !== $post->post_status ) {
			return false;
		}

		if ( wp_is_post_revision( $post->ID ) ) {
			return false;
		}

		$indexable_types = viglet_dumont_get_indexable_types();
		if ( ! in_array( $post->post_type, $indexable_types, true ) ) {
			return false;
		}

		$exclude_ids = $this->get_excluded_ids();
		if ( in_array( $post->ID, $exclude_ids, true ) ) {
			return false;
		}

		if ( $this->mapping->excludes_type( $post->post_type ) ) {
			return false;
		}

		return ! get_post_meta( $post->ID, Viglet_Dumont_Mapping::EXCLUDE_META, true );
	}

	/**
	 * Index a single post.
	 *
	 * @param WP_Post $post The post to index.
	 */
	public function index_post( WP_Post $post ): array {
		return $this->client->import( array( $this->build_create_item( $post ) ) );
	}

	/**
	 * Remove a post from the index.
	 *
	 * @param int $post_id The post to remove.
	 */
	public function deindex_post( int $post_id ): array {
		return $this->client->deindex( array( $this->build_delete_item( $post_id ) ) );
	}

	/**
	 * The job item that indexes a post.
	 *
	 * @param WP_Post $post The post to index.
	 */
	public function build_create_item( WP_Post $post ): array {
		return $this->client->build_create_item( $this->build_attributes( $post ) );
	}

	/**
	 * The job item that removes a post from the index.
	 *
	 * @param int $post_id The post to remove.
	 */
	public function build_delete_item( int $post_id ): array {
		return $this->client->build_delete_item( $this->build_doc_id( $post_id ) );
	}

	/**
	 * Index a batch of posts. Returns progress info.
	 *
	 * @param string $post_type Post type to index.
	 * @param int    $offset    Offset to start from.
	 * @return array{indexed: int, total: int, offset: int, done: bool, percent: float, message: string, error?: bool} `error` is set when Turing refused or did not answer the import, and the offset is not advanced.
	 */
	public function index_batch( string $post_type = 'post', int $offset = 0 ): array {
		$batch_size = (int) $this->settings['batch_size'];
		if ( $batch_size <= 0 ) {
			$batch_size = 50;
		}

		if ( 0 === $offset ) {
			$this->provision();
		}

		$types = ( 'all' === $post_type ) ? viglet_dumont_get_indexable_types() : array( $post_type );

		$args = array(
			'post_type'      => $types,
			'post_status'    => 'publish',
			'posts_per_page' => $batch_size,
			'offset'         => $offset,
			'orderby'        => 'ID',
			'order'          => 'ASC',
			'no_found_rows'  => false,
		);

		$exclude_ids = $this->get_excluded_ids();
		if ( ! empty( $exclude_ids ) ) {
			$args['post__not_in'] = $exclude_ids;
		}

		$query     = new WP_Query( $args );
		$total     = (int) $query->found_posts;
		$job_items = array();
		$result    = array( 'success' => true );

		if ( $query->have_posts() ) {
			foreach ( $query->posts as $post ) {
				$job_items[] = $this->build_create_item( $post );
				// Index All is also the migration: the copy indexed under the id used before the
				// site key (D148) goes in the same call. Deleting an id that is not there is a no-op.
				$job_items[] = $this->client->build_delete_item( $this->legacy_doc_id( $post->ID ) );
			}

			if ( ! empty( $job_items ) ) {
				$result = $this->client->import( $job_items );
			}
		}

		wp_reset_postdata();

		if ( ! $result['success'] ) {
			return array(
				'indexed' => 0,
				'total'   => $total,
				'offset'  => $offset,
				'done'    => false,
				'percent' => $total > 0 ? round( ( $offset / $total ) * 100, 1 ) : 0,
				'error'   => true,
				'message' => $result['message'],
			);
		}

		$indexed    = count( $query->posts );
		$new_offset = $offset + $indexed;
		$done       = $new_offset >= $total;
		$percent    = $total > 0 ? round( ( $new_offset / $total ) * 100, 1 ) : 100;

		return array(
			'indexed' => $indexed,
			'total'   => $total,
			'offset'  => $new_offset,
			'done'    => $done,
			'percent' => min( $percent, 100 ),
			'message' => $done
				/* translators: %d: how many documents were indexed. */
				? sprintf( __( 'Indexing complete. %d documents indexed.', 'viglet-dumont' ), $new_offset )
				/* translators: 1: documents indexed so far, 2: documents to index in all. */
				: sprintf( __( 'Indexed %1$d of %2$d documents...', 'viglet-dumont' ), $new_offset, $total ),
		);
	}

	/**
	 * Delete this site's documents from Turing, and only this site's: the ones whose
	 * `site_key` is this site key, so other sites sharing the SN site keep theirs.
	 *
	 * Turing's import deletes by id or by type and by nothing else, so the ids are listed
	 * through search (`fq[]=site_key:<key>`) and removed by id until Turing deletes by field
	 * (T1112). Documents indexed before the
	 * site key carry no `site_key` and are not found here; Index All removes those.
	 */
	public function delete_all(): bool {
		$this->provision();
		// One delete by the field this site's documents share (Turing T1112), not a listing
		// paged through and deleted one id at a time.
		$result = $this->client->deindex( array( $this->client->build_delete_by_fields( array( 'site_key' => $this->site_key() ) ) ) );

		return $result['success'];
	}

	/**
	 * Every document id Turing holds under this site key, or null when the search failed.
	 *
	 * @return array<int, string>|null
	 */
	public function list_site_ids(): ?array {
		$rows = 100;
		$ids  = array();

		// A ceiling, not a limit anyone should meet: 1000 pages of 100 is a site of 100k documents.
		for ( $page = 1; $page <= 1000; $page++ ) {
			$response = $this->client->search( '*', $page, $rows, array( 'fq[]' => 'site_key:' . $this->site_key() ) );
			if ( is_wp_error( $response ) ) {
				return null;
			}

			$documents = $response['results']['document'] ?? array();
			foreach ( $documents as $document ) {
				if ( isset( $document['fields']['id'] ) ) {
					$ids[] = (string) $document['fields']['id'];
				}
			}

			$total = (int) ( $response['queryContext']['count'] ?? 0 );
			if ( count( $documents ) < $rows || count( $ids ) >= $total ) {
				break;
			}
		}

		return array_values( array_unique( $ids ) );
	}

	/**
	 * The key that starts every document id and fills the `site_key` field: the configured
	 * one, or this site's host and path (so each site of a multisite network has its own).
	 */
	public function site_key(): string {
		$configured = self::sanitize_site_key( (string) ( $this->settings['site_key'] ?? '' ) );
		if ( '' !== $configured ) {
			return $configured;
		}

		$home = wp_parse_url( home_url() );
		$key  = self::sanitize_site_key( ( $home['host'] ?? '' ) . ( $home['path'] ?? '' ) );

		return '' !== $key ? $key : 'WordPress';
	}

	/**
	 * A site key as ids and filter queries can carry it: lower case letters, digits, dots,
	 * dashes and underscores, anything else turned into a dash.
	 *
	 * @param string $key The key as typed or derived.
	 */
	public static function sanitize_site_key( string $key ): string {
		$key = strtolower( trim( $key ) );
		$key = (string) preg_replace( '/[^a-z0-9._-]+/', '-', $key );

		return trim( $key, '-.' );
	}

	/**
	 * Build document attributes from a WordPress post.
	 * Maps to Turing's standard TurSNFieldName constants.
	 *
	 * @param WP_Post $post The post to describe.
	 */
	private function build_attributes( WP_Post $post ): array {
		$content = $post->post_content;
		// phpcs:ignore WordPress.NamingConventions.PrefixAllGlobals.NonPrefixedHooknameFound -- core's own filter, applied so the index sees the rendered body.
		$content = apply_filters( 'the_content', $content );
		$content = wp_strip_all_tags( strip_shortcodes( self::strip_non_content( (string) $content ) ) );
		$content = preg_replace( '/\s+/', ' ', trim( $content ) );

		$attributes = array(
			'id'                => $this->build_doc_id( $post->ID ),
			'site_key'          => $this->site_key(),
			'title'             => $post->post_title,
			'text'              => $content,
			'abstract'          => wp_trim_words( $content, 55, '...' ),
			'url'               => get_permalink( $post->ID ),
			'type'              => $post->post_type,
			'author'            => get_the_author_meta( 'display_name', (int) $post->post_author ),
			'publication_date'  => get_post_time( 'c', true, $post ),
			'modification_date' => get_post_modified_time( 'c', true, $post ),
			'site'              => get_bloginfo( 'name' ),
			'image'             => $this->get_post_thumbnail_url( $post->ID ),
			// What Search tuning's "sticky posts first" boost matches (D172).
			'sticky'            => 'post' === $post->post_type && is_sticky( $post->ID ) ? 'true' : 'false',
		);

		if ( $this->settings['index_categories'] ) {
			$categories = wp_get_post_categories( $post->ID, array( 'fields' => 'names' ) );
			if ( ! empty( $categories ) && ! is_wp_error( $categories ) ) {
				$attributes['categories'] = $categories;
				$attributes['section']    = $categories[0] ?? '';
			}
		}

		if ( $this->settings['index_tags'] ) {
			$tags = wp_get_post_tags( $post->ID, array( 'fields' => 'names' ) );
			if ( ! empty( $tags ) && ! is_wp_error( $tags ) ) {
				$attributes['tags'] = $tags;
			}
		}

		if ( $this->settings['index_comments'] ) {
			$comments = $this->get_post_comments( $post->ID );
			if ( ! empty( $comments ) ) {
				$attributes['comments'] = $comments;
			}
		}

		$custom_fields = $this->get_custom_field_names();
		foreach ( $custom_fields as $field ) {
			$value = get_post_meta( $post->ID, $field, true );
			if ( ! empty( $value ) ) {
				$attributes[ $field ] = $value;
			}
		}

		$attributes = array_merge( $attributes, $this->mapping->values( $post ) );

		/**
		 * Filters the document a post becomes, just before it is sent to Turing.
		 *
		 * @param array   $attributes The document's fields.
		 * @param WP_Post $post       The post.
		 */
		return apply_filters( 'viglet_dumont_document', $attributes, $post );
	}

	/**
	 * Remove what a rendered body shows no reader: scripts, styles, forms, inline SVG, and any
	 * element marked hidden (the `hidden` attribute, `aria-hidden="true"`, `screen-reader-text`).
	 *
	 * Blocks render UI around the content (a button's label, an icon's title, a "skip to"
	 * link), and once the tags are stripped that text reads as body copy to the index.
	 *
	 * @param string $html The rendered body.
	 */
	public static function strip_non_content( string $html ): string {
		$html = preg_replace( '@<(script|style|noscript|template|svg|form|button|select)\b[^>]*>.*?</\1>@si', '', $html ) ?? $html;

		if ( '' === trim( $html ) || ! class_exists( 'DOMDocument' ) ) {
			return $html;
		}

		$dom      = new DOMDocument();
		$previous = libxml_use_internal_errors( true );
		$loaded   = $dom->loadHTML( '<?xml encoding="utf-8"?><div id="viglet-dumont-root">' . $html . '</div>', LIBXML_HTML_NOIMPLIED | LIBXML_HTML_NODEFDTD );
		libxml_clear_errors();
		libxml_use_internal_errors( $previous );
		if ( ! $loaded ) {
			return $html;
		}

		$xpath  = new DOMXPath( $dom );
		$hidden = $xpath->query( '//*[@hidden or @aria-hidden="true" or contains(concat(" ", normalize-space(@class), " "), " screen-reader-text ")]' );
		if ( false !== $hidden ) {
			foreach ( iterator_to_array( $hidden ) as $node ) {
				$node->parentNode?->removeChild( $node ); // phpcs:ignore WordPress.NamingConventions.ValidVariableName.UsedPropertyNotSnakeCase -- DOM's own property.
			}
		}

		$root = $dom->getElementById( 'viglet-dumont-root' );
		if ( null === $root ) {
			return $html;
		}

		$out = '';
		foreach ( $root->childNodes as $child ) { // phpcs:ignore WordPress.NamingConventions.ValidVariableName.UsedPropertyNotSnakeCase -- DOM's own property.
			$out .= $dom->saveHTML( $child );
		}

		return $out;
	}

	/**
	 * The document id: `<site key>:post:<post ID>`, unique across every site sharing the SN site.
	 *
	 * @param int $post_id The post.
	 */
	public function build_doc_id( int $post_id ): string {
		return $this->site_key() . ':post:' . $post_id;
	}

	/**
	 * The id a post was indexed under before the site key, which Index All removes.
	 *
	 * @param int $post_id The post.
	 */
	private function legacy_doc_id( int $post_id ): string {
		if ( is_multisite() ) {
			return get_current_blog_id() . '-' . $post_id;
		}
		return 'wp-' . $post_id;
	}

	/**
	 * Get post thumbnail URL.
	 *
	 * @param int $post_id The post.
	 */
	private function get_post_thumbnail_url( int $post_id ): string {
		$url = get_the_post_thumbnail_url( $post_id, 'large' );
		return is_string( $url ) ? $url : '';
	}

	/**
	 * Get approved comments for a post.
	 *
	 * @param int $post_id The post.
	 */
	private function get_post_comments( int $post_id ): array {
		$comments = get_comments(
			array(
				'post_id' => $post_id,
				'status'  => 'approve',
				'number'  => 100,
			)
		);

		$texts = array();
		foreach ( $comments as $comment ) {
			$text = wp_strip_all_tags( $comment->comment_content );
			if ( ! empty( $text ) ) {
				$texts[] = $text;
			}
		}

		return $texts;
	}

	/**
	 * Get custom field names to index.
	 */
	private function get_custom_field_names(): array {
		$fields = $this->settings['index_custom_fields'] ?? '';
		if ( empty( $fields ) ) {
			return array();
		}

		return array_map( 'trim', explode( ',', $fields ) );
	}

	/**
	 * Get excluded post IDs.
	 */
	private function get_excluded_ids(): array {
		$ids = $this->settings['exclude_ids'] ?? '';
		if ( empty( $ids ) ) {
			return array();
		}

		return array_map( 'intval', array_filter( array_map( 'trim', explode( ',', $ids ) ) ) );
	}
}
