<?php
/**
 * Viglet Dumont Indexer
 *
 * Handles indexing WordPress content into Turing using the TurSNJobItems format.
 */

if ( ! defined( 'ABSPATH' ) ) {
    exit;
}

class Viglet_Dumont_Indexer {

    private Viglet_Dumont_API_Client $client;
    private array $settings;

    public function __construct( ?array $settings = null ) {
        $this->settings = $settings ?? viglet_dumont_get_settings();
        $this->client   = new Viglet_Dumont_API_Client( $this->settings );
    }

    /**
     * Check if a post should be indexed based on settings.
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

        return true;
    }

    /**
     * Index a single post.
     */
    public function index_post( WP_Post $post ): array {
        $attributes = $this->build_attributes( $post );
        $job_item   = $this->client->build_create_item( $attributes );

        return $this->client->import( array( $job_item ) );
    }

    /**
     * Remove a post from the index.
     */
    public function deindex_post( int $post_id ): array {
        $doc_id   = $this->build_doc_id( $post_id );
        $job_item = $this->client->build_delete_item( $doc_id );

        return $this->client->deindex( array( $job_item ) );
    }

    /**
     * Index a batch of posts. Returns progress info.
     *
     * @param string $post_type Post type to index.
     * @param int    $offset    Offset to start from.
     * @return array{indexed: int, total: int, offset: int, done: bool, percent: float}
     */
    public function index_batch( string $post_type = 'post', int $offset = 0 ): array {
        $batch_size = (int) $this->settings['batch_size'] ?: 50;

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

        if ( $query->have_posts() ) {
            foreach ( $query->posts as $post ) {
                $attributes  = $this->build_attributes( $post );
                $job_items[] = $this->client->build_create_item( $attributes );
            }

            if ( ! empty( $job_items ) ) {
                $this->client->import( $job_items );
            }
        }

        wp_reset_postdata();

        $new_offset = $offset + count( $job_items );
        $done       = $new_offset >= $total;
        $percent    = $total > 0 ? round( ( $new_offset / $total ) * 100, 1 ) : 100;

        return array(
            'indexed'  => count( $job_items ),
            'total'    => $total,
            'offset'   => $new_offset,
            'done'     => $done,
            'percent'  => min( $percent, 100 ),
            'message'  => $done
                ? sprintf( __( 'Indexing complete. %d documents indexed.', 'viglet-dumont' ), $new_offset )
                : sprintf( __( 'Indexed %d of %d documents...', 'viglet-dumont' ), $new_offset, $total ),
        );
    }

    /**
     * Delete all indexed WordPress content from Turing.
     */
    public function delete_all(): bool {
        $job_item = array(
            'locale'          => $this->settings['locale'] ?? 'en_US',
            'turSNJobAction'  => 'DELETE',
            'siteNames'       => array( $this->settings['site_name'] ),
            'attributes'      => array(
                'id'          => '*',
                'type'        => '*',
                'source_apps' => $this->settings['source_app'] ?? 'wordpress',
            ),
        );

        $result = $this->client->deindex( array( $job_item ) );

        return $result['success'];
    }

    /**
     * Build document attributes from a WordPress post.
     * Maps to Turing's standard TurSNFieldName constants.
     */
    private function build_attributes( WP_Post $post ): array {
        $content = $post->post_content;
        $content = apply_filters( 'the_content', $content );
        $content = wp_strip_all_tags( strip_shortcodes( $content ) );
        $content = preg_replace( '/\s+/', ' ', trim( $content ) );

        $attributes = array(
            'id'                => $this->build_doc_id( $post->ID ),
            'title'             => $post->post_title,
            'text'              => $content,
            'abstract'          => wp_trim_words( $content, 55, '...' ),
            'url'               => get_permalink( $post->ID ),
            'type'              => $post->post_type,
            'author'            => get_the_author_meta( 'display_name', $post->post_author ),
            'publication_date'  => get_post_time( 'c', true, $post ),
            'modification_date' => get_post_modified_time( 'c', true, $post ),
            'site'              => get_bloginfo( 'name' ),
            'image'             => $this->get_post_thumbnail_url( $post->ID ),
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

        return $attributes;
    }

    /**
     * Build a unique document ID for Turing.
     */
    private function build_doc_id( int $post_id ): string {
        if ( is_multisite() ) {
            return get_current_blog_id() . '-' . $post_id;
        }
        return 'wp-' . $post_id;
    }

    /**
     * Get post thumbnail URL.
     */
    private function get_post_thumbnail_url( int $post_id ): string {
        $url = get_the_post_thumbnail_url( $post_id, 'large' );
        return $url ?: '';
    }

    /**
     * Get approved comments for a post.
     */
    private function get_post_comments( int $post_id ): array {
        $comments = get_comments( array(
            'post_id' => $post_id,
            'status'  => 'approve',
            'number'  => 100,
        ) );

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
