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
 * Indexes, and removes, the PDF attachments of the posts in search.
 *
 * A PDF is in search when "Index PDF files" is on, it is attached to a post that is in search
 * itself, and it is this site's own upload under the size cap: the plugin never fetches a
 * file from another host.
 */
class Viglet_Dumont_Pdf {

	/** The largest PDF sent, in bytes; the `viglet_dumont_pdf_max_bytes` filter changes it. */
	public const MAX_BYTES = 20 * 1024 * 1024;

	/** The `type` a PDF's document carries, and so the facet value it shows under. */
	public const TYPE = 'pdf';

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
		$parent = $attachment->post_parent ? get_post( (int) $attachment->post_parent ) : null;
		if ( ! $parent instanceof WP_Post || ! ( new Viglet_Dumont_Indexer( $this->settings ) )->should_index( $parent ) ) {
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
	 * A PDF's document: its title, address and parent, with its text left to Turing, which
	 * reads the file named in `text` out of the zip.
	 *
	 * @param WP_Post $attachment The PDF.
	 * @return array The attributes.
	 */
	public function document( WP_Post $attachment ): array {
		$indexer = new Viglet_Dumont_Indexer( $this->settings );
		$parent  = get_post( (int) $attachment->post_parent );

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
