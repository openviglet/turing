<?php
/**
 * Viglet Dumont Search
 *
 * Handles search requests using the Turing search API.
 */

if ( ! defined( 'ABSPATH' ) ) {
    exit;
}

class Viglet_Dumont_Search {

    private Viglet_Dumont_API_Client $client;
    private array $settings;

    public function __construct( ?array $settings = null ) {
        $this->settings = $settings ?? viglet_dumont_get_settings();
        $this->client   = new Viglet_Dumont_API_Client( $this->settings );
    }

    /**
     * Execute search and render results page.
     */
    public function render(): void {
        $query    = get_search_query();
        $page     = max( 1, (int) ( $_GET['paged'] ?? 1 ) );
        $per_page = (int) $this->settings['results_per_page'] ?: 10;
        $sort     = sanitize_text_field( $_GET['sort'] ?? '' );
        $fq       = isset( $_GET['fq'] ) ? array_map( 'sanitize_text_field', (array) $_GET['fq'] ) : array();

        $params = array();
        if ( $sort ) {
            $params['sort'] = $sort;
        }
        if ( ! empty( $fq ) ) {
            $params['fq[]'] = $fq;
        }

        $results = $this->client->search( $query, $page, $per_page, $params );

        $data = array(
            'query'        => $query,
            'page'         => $page,
            'per_page'     => $per_page,
            'results'      => array(),
            'total'        => 0,
            'total_pages'  => 0,
            'facets'       => array(),
            'did_you_mean' => '',
            'error'        => null,
        );

        if ( is_wp_error( $results ) ) {
            $data['error'] = $results->get_error_message();
        } elseif ( is_array( $results ) ) {
            $data = $this->parse_results( $results, $data );
        }

        $this->render_template( $data );
    }

    /**
     * Parse Turing search response into template data.
     */
    private function parse_results( array $response, array $data ): array {
        if ( isset( $response['results']['document'] ) ) {
            $data['results'] = $response['results']['document'];
        }

        if ( isset( $response['results']['queryContext']['count'] ) ) {
            $data['total'] = (int) $response['results']['queryContext']['count'];
        }

        $data['total_pages'] = $data['total'] > 0
            ? (int) ceil( $data['total'] / $data['per_page'] )
            : 0;

        if ( isset( $response['results']['widget']['facet'] ) ) {
            $data['facets'] = $response['results']['widget']['facet'];
        }

        if ( isset( $response['results']['widget']['spellCheck']['correctedText'] ) ) {
            $data['did_you_mean'] = $response['results']['widget']['spellCheck']['correctedText'];
        }

        return $data;
    }

    /**
     * Render the search results template.
     */
    private function render_template( array $data ): void {
        get_header();

        $template = VIGLET_DUMONT_DIR . 'templates/search-results.php';
        if ( file_exists( $template ) ) {
            extract( $data, EXTR_SKIP );
            include $template;
        }

        get_footer();
    }
}
