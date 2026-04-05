<?php
/**
 * Viglet Dumont API Client
 *
 * Communicates with the Turing REST API for content indexing and search.
 * Uses the modern /api/sn/import JSON endpoint instead of the legacy Solr XML API.
 */

if ( ! defined( 'ABSPATH' ) ) {
    exit;
}

class Viglet_Dumont_API_Client {

    private string $server_url;
    private string $site_name;
    private string $api_key;
    private string $locale;
    private string $source_app;

    public function __construct( ?array $settings = null ) {
        if ( null === $settings ) {
            $settings = viglet_dumont_get_settings();
        }

        $this->server_url = untrailingslashit( $settings['server_url'] );
        $this->site_name  = $settings['site_name'];
        $this->api_key    = $settings['api_key'] ?? '';
        $this->locale     = $settings['locale'] ?? 'en_US';
        $this->source_app = $settings['source_app'] ?? 'wordpress';
    }

    /**
     * Test connectivity with the Turing server.
     */
    public function ping(): array {
        $response = $this->request( 'GET', '/api/sn/' . urlencode( $this->site_name ) . '/search', array(
            'q'    => '*',
            'rows' => 0,
        ) );

        if ( is_wp_error( $response ) ) {
            return array(
                'success' => false,
                'message' => $response->get_error_message(),
            );
        }

        $code = wp_remote_retrieve_response_code( $response );

        return array(
            'success' => ( $code >= 200 && $code < 400 ),
            'message' => ( $code >= 200 && $code < 400 )
                ? __( 'Connection successful', 'viglet-dumont' )
                : sprintf( __( 'Server returned HTTP %d', 'viglet-dumont' ), $code ),
            'code'    => $code,
        );
    }

    /**
     * Import job items into Turing (index documents).
     *
     * @param array $job_items Array of TurSNJobItem-compatible arrays.
     * @return array{success: bool, message: string}
     */
    public function import( array $job_items ): array {
        $payload = array( 'snJobItems' => $job_items );

        $response = $this->request( 'POST', '/api/sn/import', null, $payload );

        if ( is_wp_error( $response ) ) {
            return array(
                'success' => false,
                'message' => $response->get_error_message(),
            );
        }

        $code = wp_remote_retrieve_response_code( $response );

        return array(
            'success' => ( $code >= 200 && $code < 300 ),
            'message' => ( $code >= 200 && $code < 300 )
                ? __( 'Documents imported successfully', 'viglet-dumont' )
                : sprintf(
                    __( 'Import failed with HTTP %d: %s', 'viglet-dumont' ),
                    $code,
                    wp_remote_retrieve_body( $response )
                ),
        );
    }

    /**
     * Delete documents from Turing index.
     *
     * @param array $job_items Array of TurSNJobItem-compatible arrays with DELETE action.
     * @return array{success: bool, message: string}
     */
    public function deindex( array $job_items ): array {
        $payload = array( 'snJobItems' => $job_items );

        $response = $this->request( 'POST', '/api/sn/import', null, $payload );

        if ( is_wp_error( $response ) ) {
            return array(
                'success' => false,
                'message' => $response->get_error_message(),
            );
        }

        $code = wp_remote_retrieve_response_code( $response );

        return array(
            'success' => ( $code >= 200 && $code < 300 ),
            'message' => ( $code >= 200 && $code < 300 )
                ? __( 'Documents removed successfully', 'viglet-dumont' )
                : sprintf( __( 'Deindex failed with HTTP %d', 'viglet-dumont' ), $code ),
        );
    }

    /**
     * Search the Turing index.
     *
     * @param string $query   Search query.
     * @param int    $page    Page number (1-based).
     * @param int    $rows    Results per page.
     * @param array  $params  Additional query parameters.
     * @return array|WP_Error Search results or error.
     */
    public function search( string $query, int $page = 1, int $rows = 10, array $params = array() ) {
        $query_params = array_merge( array(
            'q'          => $query,
            'p'          => $page,
            'rows'       => $rows,
            '_setlocale' => $this->locale,
        ), $params );

        $response = $this->request(
            'GET',
            '/api/sn/' . urlencode( $this->site_name ) . '/search',
            $query_params
        );

        if ( is_wp_error( $response ) ) {
            return $response;
        }

        $code = wp_remote_retrieve_response_code( $response );
        $body = wp_remote_retrieve_body( $response );

        if ( $code < 200 || $code >= 300 ) {
            return new WP_Error( 'search_failed', sprintf(
                __( 'Search failed with HTTP %d', 'viglet-dumont' ),
                $code
            ) );
        }

        $data = json_decode( $body, true );
        if ( json_last_error() !== JSON_ERROR_NONE ) {
            return new WP_Error( 'invalid_response', __( 'Invalid JSON response from server', 'viglet-dumont' ) );
        }

        return $data;
    }

    /**
     * Build a TurSNJobItem for creating/updating a document.
     *
     * @param array $attributes Document attributes (id, title, text, url, etc.).
     * @return array TurSNJobItem-compatible array.
     */
    public function build_create_item( array $attributes ): array {
        return array(
            'locale'          => $this->locale,
            'turSNJobAction'  => 'CREATE',
            'siteNames'       => array( $this->site_name ),
            'attributes'      => array_merge( $attributes, array(
                'source_apps' => $this->source_app,
            ) ),
        );
    }

    /**
     * Build a TurSNJobItem for deleting a document.
     *
     * @param string $doc_id Document ID to delete.
     * @return array TurSNJobItem-compatible array.
     */
    public function build_delete_item( string $doc_id ): array {
        return array(
            'locale'          => $this->locale,
            'turSNJobAction'  => 'DELETE',
            'siteNames'       => array( $this->site_name ),
            'attributes'      => array(
                'id'          => $doc_id,
                'source_apps' => $this->source_app,
            ),
        );
    }

    /**
     * Make an HTTP request to the Turing server.
     *
     * @param string     $method HTTP method.
     * @param string     $path   API path.
     * @param array|null $query  Query parameters.
     * @param array|null $body   Request body (JSON-encoded).
     * @return array|WP_Error Response or error.
     */
    private function request( string $method, string $path, ?array $query = null, ?array $body = null ) {
        $url = $this->server_url . $path;

        if ( $query ) {
            $url = add_query_arg( $query, $url );
        }

        $args = array(
            'method'  => $method,
            'timeout' => 30,
            'headers' => array(
                'Accept'       => 'application/json',
                'Content-Type' => 'application/json; charset=utf-8',
            ),
        );

        if ( ! empty( $this->api_key ) ) {
            $args['headers']['Key'] = $this->api_key;
        }

        if ( $body && in_array( $method, array( 'POST', 'PUT', 'PATCH' ), true ) ) {
            $args['body'] = wp_json_encode( $body );
        }

        return wp_remote_request( $url, $args );
    }

    public function get_site_name(): string {
        return $this->site_name;
    }

    public function get_server_url(): string {
        return $this->server_url;
    }
}
