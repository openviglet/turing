<?php
/**
 * Viglet Dumont Search
 *
 * Handles search requests using the Turing search API.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Answers the site's search page from Turing.
 */
class Viglet_Dumont_Search {

	/**
	 * The client searches go through.
	 *
	 * @var Viglet_Dumont_API_Client
	 */
	private Viglet_Dumont_API_Client $client;

	/**
	 * The plugin settings this search was built with.
	 *
	 * @var array
	 */
	private array $settings;

	/**
	 * Build a search over the given settings, or over the stored ones.
	 *
	 * @param array|null $settings The plugin settings; null reads them from the options table.
	 */
	public function __construct( ?array $settings = null ) {
		$this->settings = $settings ?? viglet_dumont_get_settings();
		$this->client   = new Viglet_Dumont_API_Client( $this->settings );
	}

	/**
	 * Execute search and render results page.
	 */
	public function render(): void {
		// A search page is a public GET that changes nothing, so there is no nonce to verify.
		// phpcs:disable WordPress.Security.NonceVerification.Recommended
		$query    = get_search_query();
		$page     = max( 1, absint( wp_unslash( $_GET['paged'] ?? 1 ) ) );
		$per_page = (int) $this->settings['results_per_page'];
		if ( $per_page <= 0 ) {
			$per_page = 10;
		}
		$sort = sanitize_text_field( wp_unslash( $_GET['sort'] ?? '' ) );
		$fq   = isset( $_GET['fq'] ) ? array_map( 'sanitize_text_field', (array) wp_unslash( $_GET['fq'] ) ) : array();
		// phpcs:enable WordPress.Security.NonceVerification.Recommended

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
	 *
	 * @param array $response The decoded Turing search response.
	 * @param array $data     The template data to fill.
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
	 *
	 * @param array $data The template data, read by the template as plain variables.
	 */
	private function render_template( array $data ): void {
		get_header();

		$template = VIGLET_DUMONT_DIR . 'templates/search-results.php';
		if ( file_exists( $template ) ) {
			// phpcs:ignore WordPress.PHP.DontExtract.extract_extract -- the template reads each key as a variable of its own.
			extract( $data, EXTR_SKIP );
			include $template;
		}

		get_footer();
	}
}
