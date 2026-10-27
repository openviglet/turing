<?php
/**
 * Viglet Dumont Related
 *
 * Related content from Turing's similar-documents search (D177): the posts Turing finds most
 * like the one being read, for the Related content block and for the automatic placement
 * after a post's content.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Asks Turing for a post's neighbours and keeps the ones that are this site's published posts.
 */
class Viglet_Dumont_Related {

	/** How long one post's neighbours are kept before Turing is asked again: an hour. */
	public const CACHE_TTL = 3600;

	/** The most Turing is asked for at once. */
	private const MAX_ROWS = 50;

	/**
	 * How many neighbours are asked for each one shown: the rest absorb what WordPress drops
	 * (unpublished, another type), never another site's, which Turing filters out (T1132).
	 */
	private const ROWS_PER_SHOWN = 3;

	/**
	 * The plugin settings.
	 *
	 * @var array
	 */
	private array $settings;

	/**
	 * Build over the given settings, or over the stored ones.
	 *
	 * @param array|null $settings The plugin settings; null reads them from the options table.
	 */
	public function __construct( ?array $settings = null ) {
		$this->settings = $settings ?? viglet_dumont_get_settings();
	}

	/**
	 * Put the Related content block after the content of the chosen post types.
	 */
	public function register(): void {
		if ( ! empty( $this->settings['related_after'] ) ) {
			add_filter( 'the_content', array( $this, 'append' ), 20 );
		}
	}

	/**
	 * The post's content with the Related content block after it (`the_content`): only on
	 * the post being viewed, of a chosen type, and only when the content does not hold the
	 * block already.
	 *
	 * @param string $content The post's content.
	 * @return string
	 */
	public function append( string $content ): string {
		$types = (array) ( $this->settings['related_after'] ?? array() );
		if ( ! is_singular( $types ) || ! in_the_loop() || ! is_main_query() || has_block( 'viglet-dumont/related' ) ) {
			return $content;
		}

		return $content . render_block(
			array(
				'blockName'    => 'viglet-dumont/related',
				'attrs'        => array(),
				'innerBlocks'  => array(),
				'innerHTML'    => '',
				'innerContent' => array(),
			)
		);
	}

	/**
	 * The published posts Turing finds most like this one, best first. Never the post itself,
	 * never another site's document (Turing filters by this site's key), and an empty list
	 * when Turing does not answer or the post is not indexed.
	 *
	 * @param int    $post_id   The post being read.
	 * @param int    $count     How many to return.
	 * @param bool   $same_type Only posts of the same post type.
	 * @param string $mode      `MLT` (shared words), `VECTOR` (meaning, on a site with
	 *                          embeddings) or empty for Turing's choice.
	 * @return WP_Post[]
	 */
	public function posts( int $post_id, int $count, bool $same_type = false, string $mode = '' ): array {
		$post = get_post( $post_id );
		if ( ! $post instanceof WP_Post || $count < 1 ) {
			return array();
		}

		$ids   = $this->neighbours( $post_id, $mode, min( self::MAX_ROWS, $count * self::ROWS_PER_SHOWN ) );
		$posts = array();
		foreach ( $ids as $id ) {
			$candidate = get_post( $id );
			if (
				$candidate instanceof WP_Post
				&& $candidate->ID !== $post_id
				&& 'publish' === $candidate->post_status
				&& '' === $candidate->post_password
				&& ( ! $same_type || $candidate->post_type === $post->post_type )
			) {
				$posts[] = $candidate;
			}
			if ( count( $posts ) >= $count ) {
				break;
			}
		}

		return $posts;
	}

	/**
	 * This site's post ids among the post's neighbours in Turing, in Turing's order, cached.
	 * Turing filters by this site's key (T1132), so only posts and terms are left to tell apart.
	 *
	 * @param int    $post_id The post.
	 * @param string $mode    The similarity, as for posts().
	 * @param int    $rows    How many to ask Turing for.
	 * @return int[]
	 */
	private function neighbours( int $post_id, string $mode, int $rows ): array {
		$key    = 'viglet_dumont_related_' . md5( $post_id . '|' . $mode . '|' . $rows . '|' . $this->settings['site_name'] . '|' . $this->settings['locale'] );
		$cached = get_transient( $key );
		if ( is_array( $cached ) ) {
			return $cached;
		}

		$indexer = new Viglet_Dumont_Indexer( $this->settings );
		$similar = ( new Viglet_Dumont_API_Client( $this->settings ) )->similar(
			$indexer->build_doc_id( $post_id ),
			$rows,
			$mode,
			array( 'site_key:' . $indexer->site_key() )
		);
		// A site without embeddings may refuse a semantic question: ask by shared words.
		if ( is_wp_error( $similar ) && 'VECTOR' === $mode ) {
			return $this->neighbours( $post_id, 'MLT', $rows );
		}
		if ( is_wp_error( $similar ) ) {
			return array();
		}

		$prefix = $indexer->site_key() . ':post:';
		$ids    = array();
		foreach ( $similar as $document ) {
			$doc_id = (string) $document['id'];
			$rest   = substr( $doc_id, strlen( $prefix ) );
			if ( str_starts_with( $doc_id, $prefix ) && ctype_digit( $rest ) ) {
				$ids[] = (int) $rest;
			}
		}
		set_transient( $key, $ids, self::CACHE_TTL );

		return $ids;
	}
}
