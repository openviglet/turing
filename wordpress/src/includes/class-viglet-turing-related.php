<?php
/**
 * Viglet Turing Related
 *
 * Related content from Turing's similar-documents search (D177): the posts Turing finds most
 * like the one being read, for the Related content block and for the automatic placement
 * after a post's content.
 *
 * @package Viglet_Turing
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Asks Turing for a post's neighbours and keeps the ones that are this site's published posts.
 */
class Viglet_Turing_Related {

	/** How long one post's neighbours are kept before Turing is asked again: an hour. */
	public const CACHE_TTL = 3600;

	/**
	 * What a post's thumbnail may carry when the block prints it: core's attachment image tag,
	 * srcset and sizes included, which wp_kses_post() would strip.
	 */
	public const IMAGE_HTML = array(
		'img' => array(
			'src'           => true,
			'srcset'        => true,
			'sizes'         => true,
			'alt'           => true,
			'class'         => true,
			'width'         => true,
			'height'        => true,
			'loading'       => true,
			'decoding'      => true,
			'fetchpriority' => true,
			'style'         => true,
		),
	);

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
		$this->settings = $settings ?? viglet_turing_get_settings();
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
	 * the post being viewed, of a chosen type, and only when neither the content nor the
	 * block template drawing it holds the block already (D210). Never into an excerpt being
	 * built from the content (D222): `wp_trim_excerpt()` runs `the_content` inside
	 * `get_the_excerpt`, and would keep the box's titles and dates as the post's summary.
	 *
	 * @param string $content The post's content.
	 * @return string
	 */
	public function append( string $content ): string {
		$types = (array) ( $this->settings['related_after'] ?? array() );
		if ( doing_filter( 'get_the_excerpt' ) || ! is_singular( $types ) || ! in_the_loop() || ! is_main_query() || has_block( 'viglet-turing/related' ) || $this->template_holds_block() ) {
			return $content;
		}

		return $content . render_block(
			array(
				'blockName'    => 'viglet-turing/related',
				'attrs'        => array(),
				'innerBlocks'  => array(),
				'innerHTML'    => '',
				'innerContent' => array(),
			)
		);
	}

	/**
	 * Whether the block template being rendered places the Related content block, itself or
	 * in a template part it includes. The template is drawn before the content filter runs
	 * and a Related block after the post content has not drawn yet, so the template's own
	 * markup is read rather than a flag the block would set. False on a classic theme.
	 *
	 * @return bool
	 */
	private function template_holds_block(): bool {
		global $_wp_current_template_content;

		return is_string( $_wp_current_template_content )
			&& $this->blocks_hold( parse_blocks( $_wp_current_template_content ) );
	}

	/**
	 * Whether these parsed blocks, their inner blocks or the template parts they name hold the
	 * Related content block.
	 *
	 * @param array    $blocks The parsed blocks.
	 * @param string[] $parts  The template parts already read, so a part including itself ends.
	 * @return bool
	 */
	private function blocks_hold( array $blocks, array &$parts = array() ): bool {
		foreach ( $blocks as $block ) {
			$name = $block['blockName'] ?? '';
			if ( 'viglet-turing/related' === $name ) {
				return true;
			}
			if ( 'core/template-part' === $name && ! empty( $block['attrs']['slug'] ) ) {
				$id = ( $block['attrs']['theme'] ?? get_stylesheet() ) . '//' . $block['attrs']['slug'];
				if ( ! in_array( $id, $parts, true ) ) {
					$parts[] = $id;
					$part    = get_block_template( $id, 'wp_template_part' );
					if ( $part && $this->blocks_hold( parse_blocks( (string) $part->content ), $parts ) ) {
						return true;
					}
				}
			}
			if ( $this->blocks_hold( $block['innerBlocks'] ?? array(), $parts ) ) {
				return true;
			}
		}

		return false;
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
		return array_values( array_filter( $this->related( $post_id, $count, $same_type, $mode ), static fn ( $item ) => $item instanceof WP_Post ) );
	}

	/**
	 * What the Related content block lists, best first: this site's published posts, and with
	 * `$all_sites` the documents of the sites it also searches (D303, `sites: all`), each as
	 * its id, title, address, type and the label of its site. Never the post itself.
	 *
	 * @param int    $post_id   The post being read.
	 * @param int    $count     How many to return.
	 * @param bool   $same_type Only posts of the same post type.
	 * @param string $mode      As for posts().
	 * @param bool   $all_sites The sibling sites' documents too.
	 * @return array<int, WP_Post|array{id: string, title: string, url: string, type: string, origin: string}>
	 */
	public function related( int $post_id, int $count, bool $same_type = false, string $mode = '', bool $all_sites = false ): array {
		$post = get_post( $post_id );
		if ( ! $post instanceof WP_Post || $count < 1 ) {
			return array();
		}

		$all_sites = $all_sites && array() !== ( new Viglet_Turing_Indexer( $this->settings ) )->sibling_sites();
		$rows      = min( self::MAX_ROWS, $count * self::ROWS_PER_SHOWN );
		$items     = array();
		foreach ( $this->neighbours( $post_id, $mode, $rows, $all_sites ) as $neighbour ) {
			if ( is_array( $neighbour ) ) {
				if ( ! $same_type || $neighbour['type'] === $post->post_type ) {
					$items[] = $neighbour;
				}
			} else {
				$candidate = get_post( $neighbour );
				if (
					$candidate instanceof WP_Post
					&& $candidate->ID !== $post_id
					&& 'publish' === $candidate->post_status
					&& '' === $candidate->post_password
					&& ( ! $same_type || $candidate->post_type === $post->post_type )
				) {
					$items[] = $candidate;
				}
			}
			if ( count( $items ) >= $count ) {
				break;
			}
		}//end foreach

		return $items;
	}

	/**
	 * The post's neighbours in Turing, in Turing's order, cached: this site's as post ids, and
	 * with `$all_sites` a sibling's as the document Turing described.
	 *
	 * Turing filters by this site's key (T1132), so only posts and terms are left to tell
	 * apart. Across sites the scope goes as the search sends it, one `site_key` per site with
	 * `fqi.op=OR` (Turing T1235, D337), without the key: a key bound to this site's
	 * partition (D190) sees only its own.
	 *
	 * @param int    $post_id   The post.
	 * @param string $mode      The similarity, as for posts().
	 * @param int    $rows      How many to ask Turing for.
	 * @param bool   $all_sites The sibling sites' documents too.
	 * @return array<int, int|array{id: string, title: string, url: string, type: string, origin: string}>
	 */
	private function neighbours( int $post_id, string $mode, int $rows, bool $all_sites = false ): array {
		$indexer = new Viglet_Turing_Indexer( $this->settings );
		$scope   = $all_sites ? implode( ',', $indexer->scope_keys() ) : '';
		$key     = 'viglet_turing_related_' . md5( $post_id . '|' . $mode . '|' . $rows . '|' . $this->settings['site_name'] . '|' . $this->settings['locale'] . '|' . $scope );
		$cached  = get_transient( $key );
		if ( is_array( $cached ) ) {
			return $cached;
		}

		// A page read (D328): a hung Turing costs this view a few seconds, the next ones nothing.
		$client  = ( new Viglet_Turing_API_Client( $this->settings ) )->for_render();
		$similar = $all_sites
			? $client->for_scope( $indexer )->similar( $indexer->build_doc_id( $post_id ), $rows, $mode, $indexer->scope_query() )
			: $client->similar( $indexer->build_doc_id( $post_id ), $rows, $mode, array( $indexer->own_filter() ) );
		// A site without embeddings may refuse a semantic question: ask by shared words. A
		// Turing that did not answer at all is not asked again.
		if ( is_wp_error( $similar ) && 'VECTOR' === $mode && ! get_transient( Viglet_Turing_API_Client::DOWN_TRANSIENT ) ) {
			return $this->neighbours( $post_id, 'MLT', $rows, $all_sites );
		}
		if ( is_wp_error( $similar ) ) {
			return array();
		}

		$prefix = $indexer->site_key() . ':post:';
		$found  = array();
		foreach ( $similar as $document ) {
			$doc_id = (string) $document['id'];
			$rest   = substr( $doc_id, strlen( $prefix ) );
			if ( str_starts_with( $doc_id, $prefix ) && ctype_digit( $rest ) ) {
				$found[] = (int) $rest;
				continue;
			}
			$sibling = $all_sites ? $indexer->sibling_of( $doc_id ) : null;
			if ( null !== $sibling && '' !== (string) $document['url'] ) {
				$found[] = array(
					'id'     => $doc_id,
					'title'  => html_entity_decode( wp_strip_all_tags( (string) $document['title'] ) ),
					'url'    => (string) $document['url'],
					'type'   => (string) ( $document['type'] ?? '' ),
					'origin' => $sibling['label'],
				);
			}
		}
		set_transient( $key, $found, self::CACHE_TTL );

		return $found;
	}
}
