<?php
/**
 * Viglet Dumont Native Search
 *
 * Turing answers WordPress's own search query, so the theme's search template renders the
 * results: a block theme keeps its header, footer and styles, and a classic theme its
 * search.php. Nothing is printed by the plugin.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Replaces the main search query's database lookup with Turing's ranking, through
 * `posts_pre_query`, keeping WordPress's pagination and templates.
 */
class Viglet_Dumont_Native_Search {

	/**
	 * The plugin settings this search runs with.
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
	 * Hook the main query.
	 */
	public function register(): void {
		add_filter( 'posts_pre_query', array( $this, 'posts_pre_query' ), 10, 2 );
	}

	/**
	 * Register the script modules a theme imports to search from the browser:
	 * `@viglet/turing-sdk` (the SDK, as published) and `viglet-dumont/search` (the SDK
	 * configured for this site). Nothing is loaded until a theme enqueues or imports them.
	 */
	public static function register_modules(): void {
		$asset = VIGLET_DUMONT_DIR . 'admin/build/turing-sdk.asset.php';
		if ( ! file_exists( $asset ) ) {
			return;
		}
		$sdk = include $asset;

		wp_register_script_module( '@viglet/turing-sdk', VIGLET_DUMONT_URL . 'admin/build/turing-sdk.js', array(), $sdk['version'] );
		wp_register_script_module(
			'viglet-dumont/search',
			VIGLET_DUMONT_URL . 'public/search.js',
			array( '@viglet/turing-sdk', '@wordpress/interactivity' ),
			VIGLET_DUMONT_VERSION
		);
	}

	/**
	 * Keep a refined search out of search engines (`wp_robots`): a page holding the Turing
	 * search block, opened with filters, a results page or a sort in its URL (search-block.js
	 * writes `fq[]`, `pg` and `sort`), is one of endless variations of that page. The plain
	 * page stays indexable.
	 *
	 * @param array $robots The robots directives WordPress is about to print.
	 * @return array The directives, with noindex on a refined search.
	 */
	public static function robots( array $robots ): array {
		// A read-only look at the URL: nothing is changed, so there is no nonce to check.
		// phpcs:ignore WordPress.Security.NonceVerification.Recommended
		$refined = isset( $_GET['fq'] ) || isset( $_GET['pg'] ) || isset( $_GET['sort'] );
		if ( $refined && is_singular() && has_block( 'viglet-dumont/search', get_queried_object() ) ) {
			$robots['noindex'] = true;
			$robots['follow']  = true;
		}

		return $robots;
	}

	/**
	 * Register the Turing search block (blocks/search): a search box with suggestions,
	 * did-you-mean, facets, results and pages, built on the modules above. A site with its
	 * own search blocks turns it off with the `viglet_dumont_register_blocks` filter.
	 */
	public static function register_blocks(): void {
		/**
		 * Filters whether the plugin's own search block is registered.
		 *
		 * @param bool $register False to leave the search UI to the theme's blocks.
		 */
		if ( ! apply_filters( 'viglet_dumont_register_blocks', true ) || ! file_exists( VIGLET_DUMONT_DIR . 'admin/build/turing-sdk.asset.php' ) ) {
			return;
		}

		wp_register_script_module(
			'viglet-dumont/search-block',
			VIGLET_DUMONT_URL . 'public/search-block.js',
			array( '@wordpress/interactivity', 'viglet-dumont/search' ),
			VIGLET_DUMONT_VERSION
		);
		wp_register_script(
			'viglet-dumont-search-block-editor',
			VIGLET_DUMONT_URL . 'admin/js/search-block-editor.js',
			array( 'wp-blocks', 'wp-element', 'wp-block-editor', 'wp-components', 'wp-data', 'wp-i18n', 'wp-server-side-render' ),
			VIGLET_DUMONT_VERSION,
			true
		);
		wp_set_script_translations( 'viglet-dumont-search-block-editor', 'viglet-dumont', VIGLET_DUMONT_DIR . 'languages' );
		wp_register_style( 'viglet-dumont-search-block', VIGLET_DUMONT_URL . 'blocks/search/style.css', array(), VIGLET_DUMONT_VERSION );

		register_block_type( VIGLET_DUMONT_DIR . 'blocks/search' );
		// Its parts, as inner blocks a theme can reorder (D185).
		foreach ( array_keys( Viglet_Dumont_Search_Block::PARTS ) as $part ) {
			register_block_type( VIGLET_DUMONT_DIR . 'blocks/search-parts/' . $part );
		}

		// Related content (D177): drawn on the server; its module only reports clicks.
		wp_register_script_module(
			'viglet-dumont/related-block',
			VIGLET_DUMONT_URL . 'public/related.js',
			array( '@wordpress/interactivity', 'viglet-dumont/search' ),
			VIGLET_DUMONT_VERSION
		);
		wp_register_script(
			'viglet-dumont-related-block-editor',
			VIGLET_DUMONT_URL . 'admin/js/related-block-editor.js',
			array( 'wp-blocks', 'wp-element', 'wp-block-editor', 'wp-components', 'wp-data', 'wp-i18n', 'wp-server-side-render' ),
			VIGLET_DUMONT_VERSION,
			true
		);
		wp_set_script_translations( 'viglet-dumont-related-block-editor', 'viglet-dumont', VIGLET_DUMONT_DIR . 'languages' );
		wp_register_style( 'viglet-dumont-related-block', VIGLET_DUMONT_URL . 'blocks/related/style.css', array(), VIGLET_DUMONT_VERSION );

		register_block_type( VIGLET_DUMONT_DIR . 'blocks/related' );
	}

	/**
	 * The instant-search overlay (public/overlay.js): Ctrl/⌘+K anywhere on the site, and the
	 * theme's own Search block, open a search that answers as the visitor types. Enter takes
	 * the query to the full search page.
	 */
	public function register_overlay(): void {
		if ( ! file_exists( VIGLET_DUMONT_DIR . 'admin/build/turing-sdk.asset.php' ) ) {
			return;
		}
		add_action( 'wp_enqueue_scripts', array( $this, 'enqueue_overlay' ) );
		add_filter( 'render_block_core/search', array( self::class, 'mark_search_form' ) );
	}

	/**
	 * Load the overlay on every front-end page, with its labels and where Enter goes.
	 */
	public function enqueue_overlay(): void {
		wp_register_script_module(
			'viglet-dumont/overlay',
			VIGLET_DUMONT_URL . 'public/overlay.js',
			array( '@wordpress/interactivity', 'viglet-dumont/search' ),
			VIGLET_DUMONT_VERSION
		);
		wp_enqueue_script_module( 'viglet-dumont/overlay' );
		wp_enqueue_style( 'viglet-dumont-overlay', VIGLET_DUMONT_URL . 'public/overlay.css', array(), VIGLET_DUMONT_VERSION );
		wp_interactivity_config(
			'viglet-dumont/overlay',
			array_merge(
				$this->search_page(),
				array(
					'labels' => array(
						'title'    => __( 'Search this site', 'viglet-dumont' ),
						'search'   => __( 'Search', 'viglet-dumont' ),
						'close'    => __( 'Close', 'viglet-dumont' ),
						'results'  => __( 'Results', 'viglet-dumont' ),
						'loading'  => __( 'Searching…', 'viglet-dumont' ),
						// A count's forms for one and for more, which overlay.js picks between (D184).
						'found'    => array(
							/* translators: %d: how many results Turing found. */
							'one'   => _n( '%d result', '%d results', 1, 'viglet-dumont' ),
							/* translators: %d: how many results Turing found. */
							'other' => _n( '%d result', '%d results', 2, 'viglet-dumont' ),
						),
						'none'     => __( 'No results found. Try different keywords.', 'viglet-dumont' ),
						'error'    => __( 'Search is unavailable right now.', 'viglet-dumont' ),
						'all'      => __( 'See all results', 'viglet-dumont' ),
						'hint'     => __( 'Enter: all results · Up and Down: choose one · Esc: close', 'viglet-dumont' ),
						'shortcut' => __( 'Search (Ctrl+K)', 'viglet-dumont' ),
					),
				)
			)
		);
	}

	/**
	 * Where the overlay sends a query: the chosen page, else the first published page holding
	 * the Turing search block (it reads `q`), else the theme's search page (`s`).
	 *
	 * @return array{searchPage: string, queryParam: string}
	 */
	public function search_page(): array {
		$page_id = (int) ( $this->settings['search_page'] ?? 0 );
		if ( $page_id <= 0 || 'publish' !== get_post_status( $page_id ) ) {
			$found   = get_posts(
				array(
					'post_type'   => 'page',
					'post_status' => 'publish',
					'numberposts' => 1,
					'fields'      => 'ids',
					// The first page made for it, not whichever was edited last.
					'orderby'     => 'ID',
					'order'       => 'ASC',
					// The block's comment delimiter, as one term (a search splits on spaces).
					's'           => 'wp:viglet-dumont/search',
				)
			);
			$page_id = $found ? (int) $found[0] : 0;
		}

		return $page_id > 0
			? array(
				'searchPage' => (string) get_permalink( $page_id ),
				'queryParam' => 'q',
			)
			: array(
				'searchPage' => home_url( '/' ),
				'queryParam' => 's',
			);
	}

	/**
	 * Whether the search block is being drawn for the block editor's preview (ServerSideRender
	 * asks the block renderer endpoint with `context=edit`), where its script does not run.
	 */
	public static function is_editor_preview(): bool {
		// A read-only look at the request: nothing is changed, so there is no nonce to check.
		// phpcs:ignore WordPress.Security.NonceVerification.Recommended
		return defined( 'REST_REQUEST' ) && REST_REQUEST && 'edit' === sanitize_key( wp_unslash( $_GET['context'] ?? '' ) );
	}

	/**
	 * The search block's state for the editor's preview: Turing's answer to `*` for this site,
	 * in the shape search-block.js keeps, with the values its getters would derive. The
	 * server-side directive processing then draws it with the block's own markup.
	 *
	 * @param int   $rows   How many results to show.
	 * @param array $labels The block's labels: noneLabel, errorLabel, typeLabels.
	 * @return array State to merge over the block's own.
	 */
	public function preview_state( int $rows, array $labels ): array {
		$site_key = ( new Viglet_Dumont_Indexer( $this->settings ) )->site_key();
		$response = ( new Viglet_Dumont_API_Client( $this->settings ) )->search(
			'*',
			1,
			$rows,
			array( 'fq[]' => array( 'site_key:' . $site_key ) )
		);
		if ( ! is_array( $response ) ) {
			return array(
				'status'  => 'error',
				'summary' => $labels['errorLabel'],
				'isError' => true,
			);
		}

		$results = array();
		foreach ( (array) ( $response['results']['document'] ?? array() ) as $document ) {
			$fields    = (array) ( $document['fields'] ?? array() );
			$url       = (string) ( $fields['url'] ?? '' );
			$date      = $fields['publication_date'] ?? null;
			$timestamp = is_numeric( $date ) ? (int) ( $date / 1000 ) : ( $date ? strtotime( (string) $date ) : false );
			$parts     = wp_parse_url( $url );
			$results[] = array(
				'url'   => $url,
				'title' => wp_strip_all_tags( (string) ( $fields['title'] ?? $url ) ),
				'type'  => $labels['typeLabels'][ $fields['type'] ?? '' ] ?? (string) ( $fields['type'] ?? '' ),
				'date'  => $timestamp ? wp_date( get_option( 'date_format' ), $timestamp ) : '',
				'iso'   => $timestamp ? gmdate( 'c', $timestamp ) : '',
				'crumb' => implode( ' › ', array_filter( array_merge( array( $parts['host'] ?? '' ), explode( '/', $parts['path'] ?? '' ) ) ) ),
				'image' => (string) ( $fields['image'] ?? '' ),
			);
		}

		$facets = array();
		foreach ( (array) ( $response['widget']['facet'] ?? array() ) as $facet ) {
			if ( 'site_key' === ( $facet['name'] ?? '' ) || empty( $facet['facets'] ) ) {
				continue;
			}
			$facets[] = array(
				'name'  => (string) $facet['name'],
				'label' => (string) ( $facet['label']['text'] ?? $facet['name'] ),
				'items' => array_map(
					static fn ( $value ) => array(
						'label'    => (string) ( $value['label'] ?? '' ),
						'count'    => (int) ( $value['count'] ?? 0 ),
						'selected' => false,
						'extra'    => false,
					),
					array_slice( (array) $facet['facets'], 0, 5 )
				),
			);
		}

		$total = (int) ( $response['queryContext']['count'] ?? count( $results ) );

		return array(
			'status'     => 'success',
			'query'      => '',
			'results'    => $results,
			'facets'     => $facets,
			'total'      => $total,
			/* translators: %d: how many results Turing found. */
			'summary'    => $total > 0 ? sprintf( _n( '%d result', '%d results', $total, 'viglet-dumont' ), $total ) : $labels['noneLabel'],
			'hasFacets'  => ! empty( $facets ),
			'hasResults' => $total > 0,
			'isEmpty'    => 0 === $total,
		);
	}

	/**
	 * Mark the theme's Search block form so the overlay opens from it (`render_block_core/search`).
	 *
	 * @param string $html The block's markup.
	 * @return string The markup, its form carrying `data-viglet-dumont-overlay`.
	 */
	public static function mark_search_form( string $html ): string {
		$tags = new WP_HTML_Tag_Processor( $html );
		if ( $tags->next_tag( array( 'tag_name' => 'form' ) ) ) {
			$tags->set_attribute( 'data-viglet-dumont-overlay', '' );
		}

		return $tags->get_updated_html();
	}

	/**
	 * Hand the public config to the modules (read there with getConfig( 'viglet-dumont' )).
	 * It is printed only on a page that loads the Interactivity API.
	 */
	public static function provide_config(): void {
		wp_interactivity_config( 'viglet-dumont', viglet_dumont_public_config() );
	}

	/**
	 * Answer the front end's main search query from Turing. Any other query, or Turing not
	 * answering, returns null and WordPress searches its database as it always did.
	 *
	 * @param array|null $posts Posts another filter already supplied, or null.
	 * @param WP_Query   $query The query.
	 * @return array|null
	 */
	public function posts_pre_query( $posts, WP_Query $query ) {
		if ( null !== $posts || is_admin() || ! $query->is_main_query() || ! $query->is_search() ) {
			return $posts;
		}

		$per_page = (int) $query->get( 'posts_per_page' );
		$per_page = $per_page > 0 ? $per_page : (int) get_option( 'posts_per_page', 10 );
		$page     = max( 1, (int) $query->get( 'paged' ) );

		$found = $this->search( (string) $query->get( 's' ), $page, $per_page );
		if ( null === $found ) {
			return null;
		}

		$query->found_posts   = $found['total'];
		$query->max_num_pages = (int) ceil( $found['total'] / $per_page );

		if ( 'ids' === $query->get( 'fields' ) ) {
			return $found['ids'];
		}

		return empty( $found['ids'] ) ? array() : get_posts(
			array(
				'post__in'       => $found['ids'],
				'orderby'        => 'post__in',
				'post_type'      => 'any',
				'posts_per_page' => count( $found['ids'] ),
			)
		);
	}

	/**
	 * One page of Turing's answer for this site's documents, as post ids in Turing's order,
	 * with the total; null when Turing could not answer.
	 *
	 * A shared SN site holds other sites' documents too, so the search is filtered to this
	 * site key, and an id that is not `<site key>:post:<ID>` is skipped.
	 *
	 * @param string $query    What the visitor typed.
	 * @param int    $page     The page, from 1.
	 * @param int    $per_page Results per page.
	 * @return array{ids: int[], total: int}|null
	 */
	public function search( string $query, int $page, int $per_page ): ?array {
		$indexer  = new Viglet_Dumont_Indexer( $this->settings );
		$client   = new Viglet_Dumont_API_Client( $this->settings );
		$response = $client->search( '' !== trim( $query ) ? $query : '*', $page, $per_page, array( 'fq[]' => 'site_key:' . $indexer->site_key() ) );
		if ( is_wp_error( $response ) ) {
			return null;
		}

		$prefix = $indexer->site_key() . ':post:';
		$ids    = array();
		foreach ( (array) ( $response['results']['document'] ?? array() ) as $document ) {
			$doc_id = (string) ( $document['fields']['id'] ?? '' );
			if ( str_starts_with( $doc_id, $prefix ) && ctype_digit( substr( $doc_id, strlen( $prefix ) ) ) ) {
				$ids[] = (int) substr( $doc_id, strlen( $prefix ) );
			}
		}

		return array(
			'ids'   => $ids,
			'total' => (int) ( $response['queryContext']['count'] ?? count( $ids ) ),
		);
	}

	/**
	 * What a browser needs to search this site through Turing itself: never the API key.
	 * Facets are the mapped fields marked as facets, with their labels and types.
	 *
	 * `analytics` is there only when the site reports something: whether clicks go to Turing,
	 * whether events go to Google Analytics (and which measurement), and whether the visitor's
	 * statistics consent is needed first (see analytics()).
	 *
	 * @return array{url: string, site: string, locale: string, siteKey: string, facets: array<int, array{field: string, label: string, type: string, multi: bool}>, analytics?: array{clicks: bool, ga: bool, measurementId: string, requireConsent: bool}}
	 */
	public function public_config(): array {
		$facets = array();
		foreach ( ( new Viglet_Dumont_Mapping( $this->settings ) )->all() as $type_mapping ) {
			foreach ( $type_mapping['fields'] as $field ) {
				$label = '' !== $field['facet_label'] ? $field['facet_label'] : $field['field'];
				if ( $field['facet'] && ! isset( $facets[ $field['field'] ] ) ) {
					$facets[ $field['field'] ] = array(
						'field' => $field['field'],
						'label' => $label,
						'type'  => $field['type'],
						'multi' => $field['multi'],
					);
				}
				// A range facet filters as `<field>_range:<range label>`, so the chip names it too.
				$range = Viglet_Dumont_Mapping::range_facet_name( $field['field'] );
				if ( $field['range'] && ! isset( $facets[ $range ] ) ) {
					$facets[ $range ] = array(
						'field' => $range,
						'label' => $label,
						'type'  => $field['type'],
						'multi' => false,
						'range' => true,
					);
				}
			}//end foreach
		}//end foreach

		$config    = array(
			'url'     => untrailingslashit( '' !== (string) $this->settings['public_url'] ? $this->settings['public_url'] : $this->settings['server_url'] ),
			'site'    => (string) $this->settings['site_name'],
			'locale'  => (string) $this->settings['locale'],
			'siteKey' => ( new Viglet_Dumont_Indexer( $this->settings ) )->site_key(),
			'facets'  => array_values( $facets ),
		);
		$analytics = $this->analytics();
		if ( null !== $analytics ) {
			$config['analytics'] = $analytics;
		}
		// A product result's price and "Add to cart" (D173).
		$shop = Viglet_Dumont_WooCommerce::shop_config();
		if ( null !== $shop ) {
			$config['shop'] = $shop;
		}

		return $config;
	}

	/**
	 * What the browser reports about searches, or null when the site reports nothing.
	 *
	 * Events carry the query, a document id and a rank: nothing about the visitor, and no
	 * cookie of the plugin's is set. Even so, nothing is sent before the visitor allows statistics. A consent
	 * plugin speaking the WP Consent API answers that in the browser; without one the
	 * `viglet_dumont_tracking_consent` filter does, and turning reporting on counts as the
	 * site's answer unless the filter says otherwise.
	 *
	 * @return array{clicks: bool, ga: bool, measurementId: string, requireConsent: bool}|null
	 */
	public function analytics(): ?array {
		$clicks = ! empty( $this->settings['track_clicks'] );
		$ga     = ! empty( $this->settings['analytics_ga'] );
		if ( ! $clicks && ! $ga ) {
			return null;
		}

		/**
		 * Filters whether visitors count as consenting to statistics when no consent plugin
		 * speaks the WP Consent API. Ignored when one does: the visitor's choice decides.
		 *
		 * @param bool $consent False to send nothing until a consent plugin is installed.
		 */
		$consent = (bool) apply_filters( 'viglet_dumont_tracking_consent', true );

		return array(
			'clicks'         => $clicks,
			'ga'             => $ga,
			'measurementId'  => $ga ? (string) $this->settings['ga_measurement_id'] : '',
			'requireConsent' => function_exists( 'wp_has_consent' ) || ! $consent,
		);
	}

	/**
	 * A GA4 measurement id (`G-` and letters or digits), upper-cased; anything else is empty.
	 *
	 * @param string $id What was typed.
	 */
	public static function sanitize_measurement_id( string $id ): string {
		$id = strtoupper( trim( $id ) );

		return 1 === preg_match( '/^G-[A-Z0-9]{4,20}$/', $id ) ? $id : '';
	}
}
