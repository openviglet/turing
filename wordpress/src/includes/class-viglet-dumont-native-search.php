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
		// phpcs:ignore WordPress.Security.NonceVerification.Recommended -- a read-only look at the URL.
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
		wp_set_script_translations( 'viglet-dumont-search-block-editor', 'viglet-turing-search', VIGLET_DUMONT_DIR . 'languages' );
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
		wp_set_script_translations( 'viglet-dumont-related-block-editor', 'viglet-turing-search', VIGLET_DUMONT_DIR . 'languages' );
		wp_register_style( 'viglet-dumont-related-block', VIGLET_DUMONT_URL . 'blocks/related/style.css', array(), VIGLET_DUMONT_VERSION );

		register_block_type( VIGLET_DUMONT_DIR . 'blocks/related' );

		// Ask the catalog (D173): a conversation with Turing's catalog copilot.
		wp_register_script_module(
			'viglet-dumont/copilot-block',
			VIGLET_DUMONT_URL . 'public/copilot.js',
			array( '@wordpress/interactivity', 'viglet-dumont/search' ),
			VIGLET_DUMONT_VERSION
		);
		wp_register_script(
			'viglet-dumont-copilot-block-editor',
			VIGLET_DUMONT_URL . 'admin/js/copilot-block-editor.js',
			array( 'wp-blocks', 'wp-element', 'wp-block-editor', 'wp-components', 'wp-i18n' ),
			VIGLET_DUMONT_VERSION,
			true
		);
		wp_set_script_translations( 'viglet-dumont-copilot-block-editor', 'viglet-turing-search', VIGLET_DUMONT_DIR . 'languages' );
		// The editor speaks as the site does (D323): a shop's catalog and kit, or this site and a plan.
		wp_add_inline_script(
			'viglet-dumont-copilot-block-editor',
			'window.vigletDumontCopilot = ' . wp_json_encode( array( 'shop' => Viglet_Dumont_WooCommerce::active() ) ) . ';',
			'before'
		);
		wp_register_style( 'viglet-dumont-copilot-block', VIGLET_DUMONT_URL . 'blocks/copilot/style.css', array(), VIGLET_DUMONT_VERSION );

		register_block_type( VIGLET_DUMONT_DIR . 'blocks/copilot' );

		// Site assistant (D175): a chat with Turing over this site's posts.
		wp_register_script_module(
			'viglet-dumont/assistant-block',
			VIGLET_DUMONT_URL . 'public/assistant.js',
			array( '@wordpress/interactivity', 'viglet-dumont/search' ),
			VIGLET_DUMONT_VERSION
		);
		wp_register_script(
			'viglet-dumont-assistant-block-editor',
			VIGLET_DUMONT_URL . 'admin/js/assistant-block-editor.js',
			array( 'wp-blocks', 'wp-element', 'wp-block-editor', 'wp-components', 'wp-i18n' ),
			VIGLET_DUMONT_VERSION,
			true
		);
		wp_set_script_translations( 'viglet-dumont-assistant-block-editor', 'viglet-turing-search', VIGLET_DUMONT_DIR . 'languages' );
		wp_register_style( 'viglet-dumont-assistant-block', VIGLET_DUMONT_URL . 'blocks/assistant/style.css', array(), VIGLET_DUMONT_VERSION );

		register_block_type( VIGLET_DUMONT_DIR . 'blocks/assistant' );

		// Compare products (D200): a table drawn on the server; its module only asks for the summary.
		wp_register_script_module(
			'viglet-dumont/compare-block',
			VIGLET_DUMONT_URL . 'public/compare.js',
			array(),
			VIGLET_DUMONT_VERSION
		);
		wp_register_script(
			'viglet-dumont-compare-block-editor',
			VIGLET_DUMONT_URL . 'admin/js/compare-block-editor.js',
			array( 'wp-blocks', 'wp-element', 'wp-block-editor', 'wp-components', 'wp-i18n' ),
			VIGLET_DUMONT_VERSION,
			true
		);
		wp_set_script_translations( 'viglet-dumont-compare-block-editor', 'viglet-turing-search', VIGLET_DUMONT_DIR . 'languages' );
		wp_register_style( 'viglet-dumont-compare-block', VIGLET_DUMONT_URL . 'blocks/compare/style.css', array(), VIGLET_DUMONT_VERSION );

		register_block_type( VIGLET_DUMONT_DIR . 'blocks/compare' );
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
		// Each type's name, one and many, for an immersive overlay's groups (D311).
		$types = array();
		foreach ( viglet_dumont_get_indexable_types() as $type ) {
			$object = get_post_type_object( $type );
			if ( $object ) {
				$types[ $type ] = (string) $object->labels->name;
			}
		}
		$types[ Viglet_Dumont_Pdf::TYPE ] = __( 'PDF files', 'viglet-turing-search' );
		// A type another source sent (D327) groups under the label Fields gave it.
		$types    += ( new Viglet_Dumont_Mapping( $this->settings ) )->foreign_labels();
		$immersive = 'immersive' === ( $this->settings['overlay_layout'] ?? '' );

		wp_interactivity_config(
			'viglet-dumont/overlay',
			array_merge(
				$this->search_page(),
				array(
					'layout'     => $immersive ? 'immersive' : 'list',
					'typeLabels' => $types,
					// The immersive zero state's popular searches (D202), and whether a question
					// gets Turing's answer on top: opt-in, as each answer is an LLM call.
					'popular'    => $immersive ? ( new Viglet_Dumont_Trending() )->chips() : array(),
					'answers'    => $immersive && ! empty( $this->settings['overlay_answers'] ),
					'labels'     => array(
						'popular'  => __( 'Popular searches', 'viglet-turing-search' ),
						'answer'   => __( 'Answer', 'viglet-turing-search' ),
						'preview'  => __( 'Preview', 'viglet-turing-search' ),
						/* translators: %s: a type's name, e.g. "Events". */
						'seeAll'   => __( 'See all %s', 'viglet-turing-search' ),
						'recent'   => __( 'Recent searches', 'viglet-turing-search' ),
						'title'    => __( 'Search this site', 'viglet-turing-search' ),
						'search'   => __( 'Search', 'viglet-turing-search' ),
						'close'    => __( 'Close', 'viglet-turing-search' ),
						'results'  => __( 'Results', 'viglet-turing-search' ),
						'loading'  => __( 'Searching…', 'viglet-turing-search' ),
						// A count's forms for one and for more, which overlay.js picks between (D184).
						'found'    => array(
							/* translators: %d: how many results Turing found. */
							'one'   => _n( '%d result', '%d results', 1, 'viglet-turing-search' ),
							/* translators: %d: how many results Turing found. */
							'other' => _n( '%d result', '%d results', 2, 'viglet-turing-search' ),
						),
						'none'     => __( 'No results found. Try different keywords.', 'viglet-turing-search' ),
						'error'    => __( 'Search is unavailable right now.', 'viglet-turing-search' ),
						'all'      => __( 'See all results', 'viglet-turing-search' ),
						'hint'     => __( 'Enter: all results · Up and Down: choose one · Esc: close', 'viglet-turing-search' ),
						'shortcut' => __( 'Search (Ctrl+K)', 'viglet-turing-search' ),
					),
				)
			)
		);
	}

	/**
	 * Where a query typed on this site goes: the results page on another site when one is set
	 * (D304), else the chosen page, else the first published page holding the Turing search
	 * block (it reads `q`), else the theme's search page (`s`). The overlay's Enter, the
	 * box-only search block and the theme's search forms all read it.
	 *
	 * @return array{searchPage: string, queryParam: string}
	 */
	public function search_page(): array {
		$remote = $this->remote_search_url();
		if ( '' !== $remote ) {
			parse_str( (string) ( wp_parse_url( $remote )['query'] ?? '' ), $query );

			return array(
				'searchPage' => $remote,
				// A theme's own search page (`?s=`) keeps its parameter; anything else is the block's.
				'queryParam' => array_key_exists( 's', $query ) ? 's' : 'q',
			);
		}

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
	 * The mapped fields marked "Show on result cards" in Fields (D305), in mapping order and
	 * once each, labelled as their facet label or else their name. The browser gets the same
	 * list as `cards` in the public config, so first paint and hydration draw the same lines.
	 *
	 * @return array<int, array{field: string, label: string, type: string}>
	 */
	public function card_fields(): array {
		$cards = array();
		foreach ( ( new Viglet_Dumont_Mapping( $this->settings ) )->all() as $type_mapping ) {
			if ( $type_mapping['exclude'] ) {
				continue;
			}
			foreach ( $type_mapping['fields'] as $field ) {
				if ( ! empty( $field['card'] ) && ! isset( $cards[ $field['field'] ] ) ) {
					$cards[ $field['field'] ] = array(
						'field' => $field['field'],
						'label' => '' !== $field['facet_label'] ? $field['facet_label'] : $field['field'],
						'type'  => $field['type'],
					);
				}
			}
		}

		return array_values( $cards );
	}

	/**
	 * Per post type, the mapped fields its result cards show as the date and the image in
	 * place of the publication date and the featured image (D305); a type that keeps both
	 * defaults is left out. A type another source sent under this site key (D327) has its own
	 * in Fields as well. The browser gets the same map as `cardTypes`.
	 *
	 * @return array<string, array{date: string, image: string}>
	 */
	public function card_types(): array {
		$types   = array();
		$mapping = new Viglet_Dumont_Mapping( $this->settings );
		foreach ( $mapping->foreign() as $type => $looks ) {
			if ( '' !== $looks['card_date'] || '' !== $looks['card_image'] ) {
				$types[ (string) $type ] = array(
					'date'  => $looks['card_date'],
					'image' => $looks['card_image'],
				);
			}
		}
		foreach ( $mapping->all() as $post_type => $type_mapping ) {
			if ( ! $type_mapping['exclude'] && ( '' !== $type_mapping['card_date'] || '' !== $type_mapping['card_image'] ) ) {
				$types[ $post_type ] = array(
					'date'  => $type_mapping['card_date'],
					'image' => $type_mapping['card_image'],
				);
			}
		}

		return $types;
	}

	/**
	 * A document's value for a card's date or image field, its first when a list; null when
	 * the type keeps the default or the document carries nothing there.
	 *
	 * @param array  $fields The document's fields.
	 * @param string $field  The mapped field, or '' for the default.
	 * @return mixed
	 */
	private static function card_override( array $fields, string $field ) {
		$value = '' !== $field ? ( $fields[ $field ] ?? null ) : null;
		$value = is_array( $value ) ? reset( $value ) : $value;

		return null === $value || false === $value || '' === $value ? null : $value;
	}

	/**
	 * A card's date in the site's date format (D305). A date with no time of day (a day-only
	 * meta, indexed as midnight UTC) is that day everywhere: read in the site's own zone it
	 * would show the day before west of Greenwich, as search.js's formatCardDate() knows.
	 *
	 * @param int $timestamp The date.
	 */
	public static function card_day( int $timestamp ): string {
		$zone = 0 === $timestamp % 86400 ? new DateTimeZone( 'UTC' ) : null;

		return (string) wp_date( get_option( 'date_format' ), $timestamp, $zone );
	}

	/**
	 * A server-drawn result card through `viglet_dumont_result_card` (D305).
	 *
	 * @param array $card   The card, in the keys search-block.js's card() gives the store.
	 * @param array $fields The document's fields.
	 */
	private static function filter_card( array $card, array $fields ): array {
		/**
		 * Filters a result card the search block draws on the server, before its script runs.
		 *
		 * The keys are the store's (title, url, type, date, iso, image, snippetText, fields,
		 * ...). The script redraws a card it searched itself from the mapping, not through
		 * this filter, so change only what Fields cannot say, or the card changes on hydration.
		 *
		 * @param array $card     The card.
		 * @param array $document The document's fields, as Turing answered them.
		 */
		return apply_filters( 'viglet_dumont_result_card', $card, $fields );
	}

	/**
	 * A result's card lines (D305): each card field the document carries, as search-block.js's
	 * cardValues() writes it. A date in the site's date format, a list joined, and a URL never
	 * shown raw; a field with nothing left to show is no line.
	 *
	 * @param array<int, array{field: string, label: string, type: string}> $cards  card_fields().
	 * @param array                                                         $fields The document's fields.
	 * @return array<int, array{field: string, label: string, value: string}>
	 */
	public static function card_values( array $cards, array $fields ): array {
		$lines = array();
		foreach ( $cards as $card ) {
			$raw    = $fields[ $card['field'] ] ?? array();
			$values = array();
			foreach ( is_array( $raw ) ? $raw : array( $raw ) as $value ) {
				$value = trim( html_entity_decode( wp_strip_all_tags( (string) $value ) ) );
				if ( '' === $value || preg_match( '#^https?://#i', $value ) ) {
					continue;
				}
				if ( 'DATE' === $card['type'] ) {
					$timestamp = is_numeric( $value ) ? (int) ( $value / 1000 ) : strtotime( $value );
					if ( false === $timestamp ) {
						continue;
					}
					$value = self::card_day( $timestamp );
				}
				$values[] = $value;
			}
			if ( array() !== $values ) {
				$lines[] = array(
					'field' => $card['field'],
					'label' => $card['label'],
					'value' => implode( ', ', $values ),
				);
			}
		}//end foreach

		return $lines;
	}

	/**
	 * The results page on another site this site sends its searches to (D304), or '' when
	 * they stay here: an index-only site's box lands on the shared search it indexes into.
	 */
	public function remote_search_url(): string {
		return self::sanitize_search_url( (string) ( $this->settings['search_url'] ?? '' ) );
	}

	/**
	 * A results page on another site, as the setting keeps it: an absolute https URL (http
	 * only while this site itself is served over http, a local stack), and never this site's
	 * own home, whose search the other settings already reach. Anything else is ''.
	 *
	 * @param string $url The URL as typed, or as a constant set it.
	 */
	public static function sanitize_search_url( string $url ): string {
		$url = trim( $url );
		if ( '' === $url ) {
			return '';
		}

		$parts   = wp_parse_url( $url );
		$scheme  = strtolower( (string) ( $parts['scheme'] ?? '' ) );
		$local   = strtolower( (string) ( wp_parse_url( home_url( '/' ) )['scheme'] ?? '' ) );
		$allowed = 'http' === $local ? array( 'https', 'http' ) : array( 'https' );
		if ( empty( $parts['host'] ) || ! in_array( $scheme, $allowed, true ) ) {
			return '';
		}
		$base = untrailingslashit( (string) strtok( $url, '?#' ) );
		if ( untrailingslashit( home_url( '/' ) ) === $base ) {
			return '';
		}

		return esc_url_raw( $url, $allowed );
	}

	/**
	 * Point a theme's search form at the results page on another site (D304): its action
	 * becomes that page and its field the parameter the page reads, so Enter lands there with
	 * or without the overlay. Read by `render_block_core/search` and `get_search_form`.
	 *
	 * @param string $html The form's markup.
	 * @return string The markup, sent to the other site when one is set.
	 */
	public function point_search_form( string $html ): string {
		if ( '' === $this->remote_search_url() ) {
			return $html;
		}
		$target = $this->search_page();
		$tags   = new WP_HTML_Tag_Processor( $html );
		if ( ! $tags->next_tag( array( 'tag_name' => 'form' ) ) ) {
			return $html;
		}
		$tags->set_attribute( 'action', $target['searchPage'] );
		$tags->set_attribute( 'method', 'get' );
		while ( $tags->next_tag( array( 'tag_name' => 'input' ) ) ) {
			if ( 's' === $tags->get_attribute( 'name' ) ) {
				$tags->set_attribute( 'name', $target['queryParam'] );
			}
		}

		return $tags->get_updated_html();
	}

	/**
	 * Whether the search block is being drawn for the block editor's preview (ServerSideRender
	 * asks the block renderer endpoint with `context=edit`), where its script does not run.
	 */
	public static function is_editor_preview(): bool {
		// phpcs:ignore WordPress.Security.NonceVerification.Recommended -- a read-only look at the request.
		return defined( 'REST_REQUEST' ) && REST_REQUEST && 'edit' === sanitize_key( wp_unslash( $_GET['context'] ?? '' ) );
	}

	/** How many values a facet shows before "Show more", as search-block.js's facetValuesShown. */
	public const FACET_VALUES_SHOWN = 5;

	/**
	 * The first page of a search page opened with a query (D209): the same search the block's
	 * script will make, drawn on the server in the shape search-block.js's sync() and card()
	 * give it, so the results are on the first paint and the script's own answer moves
	 * nothing. Kept out of Turing's metrics, where the script's search is the one counted.
	 * Facets are left to the script: they sit beside the results, not above them.
	 *
	 * @param array $state The block's state: query, sort, perPage and the labels.
	 * @return array State to merge over it; none when Turing did not answer.
	 */
	public function first_page( array $state ): array {
		// phpcs:disable WordPress.Security.NonceVerification.Recommended -- a read-only page, as query().
		$filters = array_map( 'sanitize_text_field', (array) wp_unslash( $_GET['fq'] ?? array() ) );
		$page    = max( 1, absint( wp_unslash( $_GET['pg'] ?? 1 ) ) );
		// phpcs:enable WordPress.Security.NonceVerification.Recommended
		$query = '' !== trim( $state['query'] ) ? trim( $state['query'] ) : '*';
		$rows  = (int) $state['perPage'];

		$response = ( new Viglet_Dumont_API_Client( $this->settings ) )->for_render()->search_unrecorded(
			array(
				'q'    => $query,
				'p'    => $page,
				'rows' => $rows,
				'sort' => $state['sort'],
				// The site's scope, the block's own (D306), then what the visitor picked.
				'fq[]' => array_merge( ( new Viglet_Dumont_Indexer( $this->settings ) )->scope_filters(), $state['fixedFilters'] ?? array(), $filters ),
			)
		);
		// Unanswered here, the script asks as it always did.
		if ( ! is_array( $response ) || ! isset( $response['queryContext'] ) ) {
			return array();
		}

		$names   = (array) ( $response['queryContext']['defaultFields'] ?? array() );
		$field   = static fn ( array $fields, string $key, string $fallback ) => $fields[ $names[ $key ] ?? $fallback ] ?? '';
		$shop    = Viglet_Dumont_WooCommerce::active();
		$offset  = ( $page - 1 ) * $rows;
		$cards   = $this->card_fields();
		$types   = $this->card_types();
		$results = array();
		foreach ( array_values( (array) ( $response['results']['document'] ?? array() ) ) as $i => $document ) {
			$fields    = (array) ( $document['fields'] ?? array() );
			$url       = (string) $field( $fields, 'url', 'url' );
			$title     = html_entity_decode( wp_strip_all_tags( (string) $field( $fields, 'title', 'title' ) ) );
			$title     = '' !== $title ? $title : $url;
			$type      = Viglet_Dumont_Mapping::document_type( $fields['type'] ?? '' );
			$looks     = $types[ $type ] ?? array(
				'date'  => '',
				'image' => '',
			);
			$date      = self::card_override( $fields, $looks['date'] ) ?? $fields['publication_date'] ?? $field( $fields, 'date', 'publication_date' );
			$timestamp = is_numeric( $date ) ? (int) ( $date / 1000 ) : ( $date ? strtotime( (string) $date ) : false );
			$parts     = wp_parse_url( $url );
			$result    = array(
				'id'          => (string) ( $fields['id'] ?? '' ),
				'position'    => $offset + $i + 1,
				'url'         => $url,
				'title'       => $title,
				'type'        => $state['typeLabels'][ $type ] ?? $type,
				'date'        => $timestamp ? self::card_day( $timestamp ) : '',
				'iso'         => $timestamp ? gmdate( 'c', $timestamp ) : '',
				'crumb'       => self::crumb( $url ),
				'image'       => (string) ( self::card_override( $fields, $looks['image'] ) ?? $field( $fields, 'image', 'image' ) ),
				'snippet'     => self::snippet( (string) $field( $fields, 'description', 'abstract' ) ),
				// The snippet as plain text, so the server draws it at its height; the script then
				// puts the highlights in.
				'snippetText' => '',
				'fields'      => self::card_values( $cards, $fields ),
				'hasFields'   => false,
				'price'       => '',
			);
			// A product's price row, as search-block.js's product() draws it (D173).
			if ( $shop && is_numeric( $fields['price'] ?? null ) ) {
				$price   = (float) $fields['price'];
				$regular = (float) ( $fields['regular_price'] ?? 0 );
				preg_match( '/:post:(\d+)$/', (string) ( $fields['id'] ?? '' ), $post );
				$result = array_merge(
					$result,
					array(
						'price'      => html_entity_decode( wp_strip_all_tags( wc_price( $price ) ) ),
						'regular'    => $regular > $price ? html_entity_decode( wp_strip_all_tags( wc_price( $regular ) ) ) : '',
						'productId'  => (int) ( $post[1] ?? 0 ),
						'cartLabel'  => str_replace( '%s', $title, $state['cartLabel'] ),
						'compareFor' => str_replace( '%s', $title, $state['compareFor'] ?? '%s' ),
					)
				);
			}
			$result['snippetText'] = implode( '', array_column( $result['snippet'], 'text' ) );
			$result['hasFields']   = array() !== $result['fields'];
			$results[]             = self::filter_card( $result, $fields );
		}//end foreach

		$total  = (int) ( $response['queryContext']['count'] ?? 0 );
		$config = $this->public_config();
		$pages  = array_values(
			array_map(
				static fn ( array $p ) => array(
					'page'    => $p['page'] ?? 0,
					'text'    => (string) ( $p['text'] ?? '' ),
					'current' => ! empty( $p['current'] ) || 'CURRENT' === ( $p['type'] ?? '' ),
				),
				array_filter( (array) ( $response['pagination'] ?? array() ), static fn ( $p ) => in_array( $p['type'] ?? '', array( 'PAGE', 'CURRENT' ), true ) )
			)
		);
		// A filtered page's facets are rebuilt by the script from an unfiltered answer, and its
		// applied filters are labelled from them: both are left to it there.
		$facets = $filters ? array() : self::facets( (array) ( $response['widget']['facet'] ?? array() ), $config );

		return array(
			'status'        => 'success',
			'total'         => $total,
			'results'       => $results,
			'facets'        => $facets,
			'pages'         => $pages,
			// What search-block.js's getters derive, which the server cannot run.
			/* translators: %d: how many results Turing found. */
			'summary'       => $total > 0 ? sprintf( _n( '%d result', '%d results', $total, 'viglet-turing-search' ), number_format_i18n( $total ) ) : $state['noneLabel'],
			'hasResults'    => $total > 0,
			'isEmpty'       => 0 === $total,
			'isLoading'     => false,
			'showSkeleton'  => false,
			'hasQuery'      => '' !== $state['query'],
			'hasFacets'     => ! empty( $facets ),
			'hasPages'      => count( $pages ) > 1,
			'hasSpotlights' => false,
			'canCompare'    => Viglet_Dumont_WooCommerce::active() && '' !== (string) Viglet_Dumont_Compare::page_url(),
			'spotlights'    => array_values(
				array_map(
					static fn ( array $spot ) => array(
						'url'     => (string) $spot['url'],
						'title'   => '' !== wp_strip_all_tags( (string) ( $spot['title'] ?? '' ) ) ? wp_strip_all_tags( (string) $spot['title'] ) : (string) $spot['url'],
						'content' => wp_strip_all_tags( (string) ( $spot['content'] ?? '' ) ),
					),
					array_filter( (array) ( $response['spotlights'] ?? array() ), static fn ( $spot ) => ! empty( $spot['url'] ) )
				)
			),
		);
	}

	/**
	 * Turing's facets as search-block.js's facets() gives them for an unfiltered answer: the
	 * site's own left out, values with no results dropped, a value facet's ties by label, and
	 * only the first values the block shows (the script adds the rest, hidden).
	 *
	 * @param array $answered The response's `widget.facet`.
	 * @param array $config   The script's config, for which facets are ranges.
	 */
	private static function facets( array $answered, array $config ): array {
		$ranges = array_column( array_filter( (array) ( $config['facets'] ?? array() ), static fn ( $f ) => ! empty( $f['range'] ) ), 'field' );
		$facets = array();
		foreach ( $answered as $facet ) {
			$name = (string) ( $facet['name'] ?? '' );
			if ( 'site_key' === $name || empty( $facet['facets'] ) ) {
				continue;
			}
			$items = array_values( array_filter( (array) $facet['facets'], static fn ( $i ) => (int) ( $i['count'] ?? 0 ) > 0 ) );
			if ( ! in_array( $name, $ranges, true ) ) {
				// Turing's order is by count; within a count, by label.
				usort(
					$items,
					static function ( $a, $b ) {
						$by_count = (int) $b['count'] <=> (int) $a['count'];
						return 0 !== $by_count ? $by_count : strcasecmp( (string) $a['label'], (string) $b['label'] );
					}
				);
			}
			if ( ! $items ) {
				continue;
			}
			$facets[] = array(
				'name'          => $name,
				'label'         => (string) ( $facet['label']['text'] ?? $name ),
				'items'         => array_map(
					static fn ( array $i ) => array(
						'label'    => (string) ( $i['label'] ?? '' ),
						'count'    => (int) $i['count'],
						'selected' => false,
						'target'   => array(
							'action'      => 'ADD',
							'field'       => $name,
							'filterQuery' => (string) ( $i['filterQuery'] ?? '' ),
						),
						'extra'    => false,
					),
					array_slice( $items, 0, self::FACET_VALUES_SHOWN )
				),
				'hasMore'       => count( $items ) > self::FACET_VALUES_SHOWN,
				'more'          => count( $items ) - self::FACET_VALUES_SHOWN,
				'selectedCount' => 0,
			);
		}//end foreach

		return $facets;
	}

	/**
	 * Where a result lives, as a reader scans it: the path's segments, after the host only when
	 * the result is on another site (D263). A shopper on this site needs no domain, and a demo's
	 * localhost would read as one.
	 *
	 * @param string $url The result's URL.
	 */
	public static function crumb( string $url ): string {
		$parts = wp_parse_url( $url );
		$home  = wp_parse_url( home_url( '/' ) );
		$here  = is_array( $home ) ? (string) ( $home['host'] ?? '' ) : '';
		$host  = (string) ( $parts['host'] ?? '' );
		if ( '' !== $host && isset( $parts['port'] ) ) {
			$host .= ':' . $parts['port'];
		}
		$own   = '' === $host || strtolower( (string) ( $parts['host'] ?? '' ) ) === strtolower( $here );
		$crumb = array_merge( $own ? array() : array( $host ), explode( '/', (string) ( $parts['path'] ?? '' ) ) );

		return implode( ' › ', array_filter( $crumb, static fn ( string $part ): bool => '' !== $part ) );
	}

	/**
	 * A description with Turing's highlights as the parts search-block.js's highlighted() makes:
	 * each a run of text, marked or not.
	 *
	 * @param string $html Turing's description, `<mark>` around the matched words.
	 * @return array<int, array{text: string, mark: bool}>
	 */
	private static function snippet( string $html ): array {
		$parts = array();
		foreach ( (array) preg_split( '#(<mark>.*?</mark>)#s', $html, -1, PREG_SPLIT_DELIM_CAPTURE | PREG_SPLIT_NO_EMPTY ) as $part ) {
			$mark = str_starts_with( (string) $part, '<mark>' );
			$text = html_entity_decode( strip_tags( (string) $part ) ); // phpcs:ignore WordPress.WP.AlternativeFunctions.strip_tags_strip_tags -- the spaces between highlights are part of the text.
			if ( '' !== $text ) {
				$parts[] = array(
					'text' => $text,
					'mark' => $mark,
				);
			}
		}

		return $parts;
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
		$response = ( new Viglet_Dumont_API_Client( $this->settings ) )->for_render()->search(
			'*',
			1,
			$rows,
			array( 'fq[]' => array_merge( ( new Viglet_Dumont_Indexer( $this->settings ) )->scope_filters(), $labels['fixedFilters'] ?? array() ) )
		);
		if ( ! is_array( $response ) ) {
			return array(
				'status'  => 'error',
				'summary' => $labels['errorLabel'],
				'isError' => true,
			);
		}

		$cards   = $this->card_fields();
		$types   = $this->card_types();
		$results = array();
		foreach ( (array) ( $response['results']['document'] ?? array() ) as $document ) {
			$fields    = (array) ( $document['fields'] ?? array() );
			$url       = (string) ( $fields['url'] ?? '' );
			$type      = Viglet_Dumont_Mapping::document_type( $fields['type'] ?? '' );
			$looks     = $types[ $type ] ?? array(
				'date'  => '',
				'image' => '',
			);
			$date      = self::card_override( $fields, $looks['date'] ) ?? $fields['publication_date'] ?? null;
			$timestamp = is_numeric( $date ) ? (int) ( $date / 1000 ) : ( $date ? strtotime( (string) $date ) : false );
			$parts     = wp_parse_url( $url );
			$values    = self::card_values( $cards, $fields );
			$card      = array(
				'url'       => $url,
				'title'     => wp_strip_all_tags( (string) ( $fields['title'] ?? $url ) ),
				'type'      => $labels['typeLabels'][ $type ] ?? $type,
				'date'      => $timestamp ? self::card_day( $timestamp ) : '',
				'iso'       => $timestamp ? gmdate( 'c', $timestamp ) : '',
				'crumb'     => self::crumb( $url ),
				'image'     => (string) ( self::card_override( $fields, $looks['image'] ) ?? $fields['image'] ?? '' ),
				'fields'    => $values,
				'hasFields' => array() !== $values,
			);
			$results[] = self::filter_card( $card, $fields );
		}//end foreach

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
			'summary'    => $total > 0 ? sprintf( _n( '%d result', '%d results', $total, 'viglet-turing-search' ), $total ) : $labels['noneLabel'],
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
		$indexer = new Viglet_Dumont_Indexer( $this->settings );
		// A page read (D328): while Turing is down WordPress searches its own database at once.
		$client   = ( new Viglet_Dumont_API_Client( $this->settings ) )->for_render();
		$response = $client->search( '' !== trim( $query ) ? $query : '*', $page, $per_page, array( 'fq[]' => $indexer->own_filter() ) );
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
					// Which way a date's periods run (D307), so a period read from a sentence
					// (D324) lands on the agenda's dates to come, or a past date's.
					if ( 'DATE' === $field['type'] ) {
						$facets[ $range ]['periods'] = (string) ( $field['periods'] ?? 'past' );
					}
				}
			}//end foreach
		}//end foreach

		$config = array(
			'url'      => untrailingslashit( '' !== (string) $this->settings['public_url'] ? $this->settings['public_url'] : $this->settings['server_url'] ),
			'site'     => (string) $this->settings['site_name'],
			'locale'   => (string) $this->settings['locale'],
			'siteKey'  => ( new Viglet_Dumont_Indexer( $this->settings ) )->site_key(),
			// Where a post of this site opens when its indexed URL is another WordPress's (D239).
			'home'     => home_url( '/' ),
			// Where a browser reports that Turing refused an AI call with 429 (D332).
			'aiPaused' => rest_url( 'viglet-dumont/v1/ai-paused' ),
			'facets'   => array_values( $facets ),
			// The mapped fields a result card shows as labelled lines (D305).
			'cards'    => $this->card_fields(),
		);
		// Each type's card date and image fields, where a type changed them (D305).
		$card_types = $this->card_types();
		if ( array() !== $card_types ) {
			$config['cardTypes'] = $card_types;
		}
		// The results page on another site (D304), so a theme's own field sends its query there.
		$remote = $this->remote_search_url();
		if ( '' !== $remote ) {
			$config['searchUrl'] = $remote;
		}
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
