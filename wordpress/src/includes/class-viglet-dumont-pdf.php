<?php
/**
 * Viglet Dumont PDF files
 *
 * The PDFs in the media library as documents of their own (D309): Turing reads their text
 * through Tika when they arrive in a zip, so their words are found like a post's.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Indexes, and removes, the PDFs the posts in search attach or link.
 *
 * A PDF is in search when "Index PDF files" is on, a post that is in search itself holds it
 * (attaches it, or links it in its content, D319), and it is this site's own upload under
 * the size cap: the plugin never fetches a file from another host.
 *
 * The posts that link a PDF are recorded on the attachment, one `LINKED_META` row per post,
 * so a post leaving search knows which PDFs it held and whether another post still holds them.
 */
class Viglet_Dumont_Pdf {

	/** The largest PDF sent, in bytes; the `viglet_dumont_pdf_max_bytes` filter changes it. */
	public const MAX_BYTES = 20 * 1024 * 1024;

	/** The `type` a PDF's document carries, and so the facet value it shows under. */
	public const TYPE = 'pdf';

	/** On a PDF attachment, one row per post whose content links it (D319). */
	public const LINKED_META = '_viglet_dumont_linked_from';

	/** On a post, set once the PDFs it holds were sent; gone when the post leaves search. */
	public const HELD_META = '_viglet_dumont_pdfs_held';

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
	 * Whether PDFs are indexed at all.
	 */
	public function enabled(): bool {
		return ! empty( $this->settings['index_pdfs'] );
	}

	/**
	 * Whether an attachment is a PDF that belongs in search.
	 *
	 * @param WP_Post|null $attachment The attachment, or anything get_post() gave.
	 */
	public function indexable( $attachment ): bool {
		if ( ! $this->enabled() || ! $attachment instanceof WP_Post || 'attachment' !== $attachment->post_type || 'application/pdf' !== $attachment->post_mime_type ) {
			return false;
		}
		if ( null === $this->holder( $attachment ) ) {
			return false;
		}
		$file = get_attached_file( $attachment->ID );
		if ( ! is_string( $file ) || ! is_readable( $file ) ) {
			return false;
		}

		/**
		 * The largest PDF sent to Turing, in bytes.
		 *
		 * @param int $bytes 20 MB.
		 */
		return (int) filesize( $file ) <= (int) apply_filters( 'viglet_dumont_pdf_max_bytes', self::MAX_BYTES );
	}

	/**
	 * The first post in search that holds a PDF: its parent, else a post that links it.
	 *
	 * @param WP_Post $attachment The PDF.
	 */
	public function holder( WP_Post $attachment ): ?WP_Post {
		$ids     = array_map( 'intval', (array) get_post_meta( $attachment->ID, self::LINKED_META, false ) );
		$ids     = array_unique( array_filter( array_merge( array( (int) $attachment->post_parent ), $ids ) ) );
		$indexer = new Viglet_Dumont_Indexer( $this->settings );
		foreach ( $ids as $id ) {
			$post = get_post( $id );
			if ( $post instanceof WP_Post && 'attachment' !== $post->post_type && $indexer->should_index( $post ) ) {
				return $post;
			}
		}

		return null;
	}

	/**
	 * The PDF attachments a post's content links: `href` and `src` addresses under this site's
	 * uploads, absolute, scheme-relative or root-relative. Another host's file is never one.
	 *
	 * @param WP_Post $post The post.
	 * @return int[] The attachment ids.
	 */
	public function linked_ids( WP_Post $post ): array {
		if ( ! preg_match_all( '/\b(?:href|src)\s*=\s*(["\'])(.+?)\1/i', (string) $post->post_content, $matches ) ) {
			return array();
		}
		$base     = untrailingslashit( wp_get_upload_dir()['baseurl'] );
		$relative = (string) preg_replace( '#^https?:#i', '', $base );
		$home     = wp_parse_url( home_url() );
		$origin   = '//' . ( $home['host'] ?? '' ) . ( isset( $home['port'] ) ? ':' . $home['port'] : '' );
		$ids      = array();
		foreach ( $matches[2] as $url ) {
			$url = (string) preg_replace( '/[?#].*$/', '', html_entity_decode( $url, ENT_QUOTES ) );
			if ( str_starts_with( $url, '/' ) && ! str_starts_with( $url, '//' ) ) {
				$url = $origin . $url;
			}
			$url = (string) preg_replace( '#^https?:#i', '', $url );
			if ( '' === $base || 0 !== stripos( $url, $relative . '/' ) ) {
				continue;
			}
			$id   = attachment_url_to_postid( $base . substr( $url, strlen( $relative ) ) );
			$file = $id > 0 ? get_post( $id ) : null;
			if ( $file instanceof WP_Post && 'attachment' === $file->post_type && 'application/pdf' === $file->post_mime_type ) {
				$ids[] = $id;
			}
		}

		return array_values( array_unique( $ids ) );
	}

	/**
	 * Bring the PDFs in step with posts just sent to search or just removed from it (D319).
	 *
	 * A post sent records the PDFs its content links; the first time it is sent (or the first
	 * after it left search) its attached and linked PDFs go with it, later only the ones it
	 * newly links. A post removed, or one that stopped linking a PDF, lets go of it, and a PDF
	 * no other post in search holds leaves search too.
	 *
	 * @param WP_Post[] $sent    The posts now in search.
	 * @param int[]     $removed The posts now out of it.
	 * @param bool      $send    Send the PDFs that came into search; false when Index All
	 *                           sends every PDF afterwards anyway.
	 * @return array{sent: int, removed: int}
	 */
	public function settle( array $sent, array $removed, bool $send = true ): array {
		$outcome = array(
			'sent'    => 0,
			'removed' => 0,
		);
		if ( ! $this->enabled() ) {
			return $outcome;
		}
		$in  = array();
		$out = array();
		foreach ( $sent as $post ) {
			[ $gained, $lost ] = $this->follow( $post );
			$in                = array_merge( $in, $gained );
			$out               = array_merge( $out, $lost );
		}
		foreach ( $removed as $post_id ) {
			$out = array_merge( $out, $this->release( (int) $post_id ) );
		}

		// Read once every post above is settled: a PDF one post let go of, another may hold.
		$in  = array_values( array_filter( array_map( 'get_post', array_unique( $in ) ), array( $this, 'indexable' ) ) );
		$out = array_values( array_filter( array_unique( $out ), fn ( $id ) => ! $this->indexable( get_post( $id ) ) ) );
		if ( $send ) {
			foreach ( array_chunk( $in, 10 ) as $chunk ) {
				if ( $this->index( $chunk )['success'] ) {
					$outcome['sent'] += count( $chunk );
				}
			}
		}
		if ( array() !== $out ) {
			$indexer = new Viglet_Dumont_Indexer( $this->settings );
			$result  = ( new Viglet_Dumont_API_Client( $this->settings ) )->deindex( array_map( array( $indexer, 'build_delete_item' ), $out ) );
			if ( $result['success'] ) {
				$outcome['removed'] = count( $out );
			}
		}

		return $outcome;
	}

	/**
	 * Record the PDFs a post in search links now, and say which may have come into search
	 * with it and which it let go of.
	 *
	 * @param WP_Post $post The post sent.
	 * @return array{0: int[], 1: int[]} The PDFs to send, and the ones it no longer links.
	 */
	private function follow( WP_Post $post ): array {
		$now    = $this->linked_ids( $post );
		$before = $this->linking( $post->ID );
		$gained = array_diff( $now, $before );
		$lost   = array_diff( $before, $now );
		foreach ( $gained as $id ) {
			add_post_meta( $id, self::LINKED_META, $post->ID );
		}
		foreach ( $lost as $id ) {
			delete_post_meta( $id, self::LINKED_META, $post->ID );
		}
		// Its first time in search, or the first since it left: everything it holds goes.
		if ( ! metadata_exists( 'post', $post->ID, self::HELD_META ) ) {
			$gained = array_merge( $now, $this->children( $post->ID ) );
			update_post_meta( $post->ID, self::HELD_META, 1 );
		}

		return array( array_values( $gained ), array_values( $lost ) );
	}

	/**
	 * Let go of the PDFs a post that left search attached or linked.
	 *
	 * @param int $post_id The post removed.
	 * @return int[] The PDFs it held.
	 */
	private function release( int $post_id ): array {
		$linked = $this->linking( $post_id );
		foreach ( $linked as $id ) {
			delete_post_meta( $id, self::LINKED_META, $post_id );
		}
		delete_post_meta( $post_id, self::HELD_META );

		return array_merge( $linked, $this->children( $post_id ) );
	}

	/**
	 * Before a post is deleted for good, which leaves its attachments with no parent, record
	 * its PDFs as linked from it, so the removal that follows still finds them.
	 *
	 * @param int $post_id The post being deleted.
	 */
	public function hold_children( int $post_id ): void {
		foreach ( $this->children( $post_id ) as $id ) {
			if ( ! in_array( $post_id, array_map( 'intval', (array) get_post_meta( $id, self::LINKED_META, false ) ), true ) ) {
				add_post_meta( $id, self::LINKED_META, $post_id );
			}
		}
	}

	/**
	 * The PDFs a post's content was recorded as linking.
	 *
	 * @param int $post_id The post.
	 * @return int[]
	 */
	private function linking( int $post_id ): array {
		return array_map(
			'intval',
			get_posts(
				array(
					'post_type'      => 'attachment',
					'post_mime_type' => 'application/pdf',
					'post_status'    => 'any',
					'numberposts'    => -1,
					'fields'         => 'ids',
					// One exact row per link, read only when a post enters or leaves search.
					'meta_key'       => self::LINKED_META, // phpcs:ignore WordPress.DB.SlowDBQuery.slow_db_query_meta_key
					'meta_value'     => (string) $post_id, // phpcs:ignore WordPress.DB.SlowDBQuery.slow_db_query_meta_value
				)
			)
		);
	}

	/**
	 * The PDFs attached to a post.
	 *
	 * @param int $post_id The post.
	 * @return int[]
	 */
	private function children( int $post_id ): array {
		return array_map(
			'intval',
			get_posts(
				array(
					'post_type'      => 'attachment',
					'post_mime_type' => 'application/pdf',
					'post_status'    => 'any',
					'post_parent'    => $post_id,
					'numberposts'    => -1,
					'fields'         => 'ids',
				)
			)
		);
	}

	/**
	 * A PDF's document: its title, address and the post that holds it, with its text left to Turing, which
	 * reads the file named in `text` out of the zip.
	 *
	 * @param WP_Post $attachment The PDF.
	 * @return array The attributes.
	 */
	public function document( WP_Post $attachment ): array {
		$indexer = new Viglet_Dumont_Indexer( $this->settings );
		$parent  = $this->holder( $attachment );

		return array(
			'id'                => $indexer->build_doc_id( $attachment->ID ),
			'title'             => '' !== $attachment->post_title ? $attachment->post_title : basename( (string) get_attached_file( $attachment->ID ) ),
			'text'              => 'file://' . self::file_name( $attachment ),
			'abstract'          => wp_strip_all_tags( (string) $attachment->post_excerpt ),
			'url'               => (string) wp_get_attachment_url( $attachment->ID ),
			'type'              => self::TYPE,
			'section'           => $parent instanceof WP_Post ? $parent->post_title : '',
			'publication_date'  => get_post_time( 'c', true, $attachment ),
			'modification_date' => get_post_modified_time( 'c', true, $attachment ),
			'site'              => get_bloginfo( 'name' ),
			'site_key'          => $indexer->site_key(),
		);
	}

	/**
	 * Send PDFs to Turing, with their files.
	 *
	 * @param WP_Post[] $attachments The PDFs, each one indexable().
	 * @return array{success: bool, message: string}
	 */
	public function index( array $attachments ): array {
		if ( array() === $attachments ) {
			return array(
				'success' => true,
				'message' => '',
			);
		}
		$client = new Viglet_Dumont_API_Client( $this->settings );
		$items  = array();
		$files  = array();
		foreach ( $attachments as $attachment ) {
			$items[]                                 = $client->build_create_item( $this->document( $attachment ) );
			$files[ self::file_name( $attachment ) ] = (string) get_attached_file( $attachment->ID );
		}
		$result = $client->import_files( $items, $files );

		return array(
			'success' => $result['success'],
			'message' => $result['message'],
		);
	}

	/**
	 * Bring one attachment's place in search up to date: indexed when it belongs there,
	 * removed otherwise (a PDF that was in search and no longer is, or never was).
	 *
	 * @param int $attachment_id The attachment.
	 */
	public function sync( int $attachment_id ): void {
		$attachment = get_post( $attachment_id );
		if ( ! $attachment instanceof WP_Post || 'application/pdf' !== $attachment->post_mime_type ) {
			return;
		}
		if ( $this->indexable( $attachment ) ) {
			$this->index( array( $attachment ) );
		} elseif ( $this->enabled() ) {
			( new Viglet_Dumont_Indexer( $this->settings ) )->deindex_post( $attachment_id );
		}
	}

	/**
	 * Take a deleted attachment out of search.
	 *
	 * @param int $attachment_id The attachment.
	 */
	public function remove( int $attachment_id ): void {
		$attachment = get_post( $attachment_id );
		if ( $this->enabled() && $attachment instanceof WP_Post && 'application/pdf' === $attachment->post_mime_type ) {
			( new Viglet_Dumont_Indexer( $this->settings ) )->deindex_post( $attachment_id );
		}
	}

	/**
	 * Every PDF in search, sent ten at a time, for a reindex.
	 *
	 * @return array{success: bool, message: string, sent: int}
	 */
	public function index_all(): array {
		if ( ! $this->enabled() ) {
			return array(
				'success' => true,
				'message' => '',
				'sent'    => 0,
			);
		}
		$ids  = get_posts(
			array(
				'post_type'      => 'attachment',
				'post_mime_type' => 'application/pdf',
				'post_status'    => 'inherit',
				'numberposts'    => -1,
				'fields'         => 'ids',
			)
		);
		$pdfs = array_values( array_filter( array_map( 'get_post', $ids ), array( $this, 'indexable' ) ) );
		foreach ( array_chunk( $pdfs, 10 ) as $chunk ) {
			$result = $this->index( $chunk );
			if ( ! $result['success'] ) {
				return $result + array( 'sent' => 0 );
			}
		}

		return array(
			'success' => true,
			'message' => '',
			'sent'    => count( $pdfs ),
		);
	}

	/**
	 * A `file:<meta key>` source's value (D309): the PDF the post's meta names, by this site's
	 * upload URL or by attachment id, handed to the import as a file Turing reads, and the
	 * `file://` name that stands for its text. Null for anything else: another host's file, a
	 * file that is no PDF or is over the size cap.
	 *
	 * @param WP_Post $post The post.
	 * @param string  $meta The meta key.
	 */
	public static function file_reference( WP_Post $post, string $meta ): ?string {
		$value = get_post_meta( $post->ID, $meta, true );
		$id    = is_numeric( $value ) ? (int) $value : ( is_string( $value ) && '' !== $value ? attachment_url_to_postid( $value ) : 0 );
		$file  = $id > 0 ? get_post( $id ) : null;
		if ( ! $file instanceof WP_Post || 'attachment' !== $file->post_type || 'application/pdf' !== $file->post_mime_type ) {
			return null;
		}
		$path = get_attached_file( $id );
		/** This filter is documented in indexable(). */
		$cap = (int) apply_filters( 'viglet_dumont_pdf_max_bytes', self::MAX_BYTES );
		if ( ! is_string( $path ) || ! is_readable( $path ) || (int) filesize( $path ) > $cap ) {
			return null;
		}
		$name = $post->ID . '-' . $id . '.pdf';
		Viglet_Dumont_API_Client::attach_file( $name, $path );

		return 'file://' . $name;
	}

	/**
	 * The name a PDF goes by inside the zip: its id, so two files called the same never meet.
	 *
	 * @param WP_Post $attachment The PDF.
	 */
	private static function file_name( WP_Post $attachment ): string {
		return $attachment->ID . '.pdf';
	}
}
