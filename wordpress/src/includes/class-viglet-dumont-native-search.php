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
			array( 'wp-blocks', 'wp-element', 'wp-block-editor', 'wp-components', 'wp-i18n' ),
			VIGLET_DUMONT_VERSION,
			true
		);
		wp_set_script_translations( 'viglet-dumont-search-block-editor', 'viglet-dumont', VIGLET_DUMONT_DIR . 'languages' );
		wp_register_style( 'viglet-dumont-search-block', VIGLET_DUMONT_URL . 'blocks/search/style.css', array(), VIGLET_DUMONT_VERSION );

		register_block_type( VIGLET_DUMONT_DIR . 'blocks/search' );
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
	 * @return array{url: string, site: string, locale: string, siteKey: string, facets: array<int, array{field: string, label: string, type: string, multi: bool}>}
	 */
	public function public_config(): array {
		$facets = array();
		foreach ( ( new Viglet_Dumont_Mapping( $this->settings ) )->all() as $type_mapping ) {
			foreach ( $type_mapping['fields'] as $field ) {
				if ( $field['facet'] && ! isset( $facets[ $field['field'] ] ) ) {
					$facets[ $field['field'] ] = array(
						'field' => $field['field'],
						'label' => '' !== $field['facet_label'] ? $field['facet_label'] : $field['field'],
						'type'  => $field['type'],
						'multi' => $field['multi'],
					);
				}
			}
		}

		return array(
			'url'     => untrailingslashit( '' !== (string) $this->settings['public_url'] ? $this->settings['public_url'] : $this->settings['server_url'] ),
			'site'    => (string) $this->settings['site_name'],
			'locale'  => (string) $this->settings['locale'],
			'siteKey' => ( new Viglet_Dumont_Indexer( $this->settings ) )->site_key(),
			'facets'  => array_values( $facets ),
		);
	}
}
