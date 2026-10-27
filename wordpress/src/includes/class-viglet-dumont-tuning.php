<?php
/**
 * Viglet Dumont Tuning
 *
 * The Search tuning screen's half on the server (D172): synonyms and promoted results,
 * written to the SN site through Turing's console APIs with the stored key, so a WordPress
 * owner fixes a bad result without Turing's console.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Synonyms and spotlights the plugin made, beside the ones an operator made in the console.
 */
class Viglet_Dumont_Tuning {

	/**
	 * What every item the plugin makes is named with, so the screen tells its own from an
	 * operator's and never edits those.
	 */
	public const PREFIX = 'WordPress: ';

	/**
	 * The client the console calls go through.
	 *
	 * @var Viglet_Dumont_API_Client
	 */
	private Viglet_Dumont_API_Client $client;

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
		$this->client   = new Viglet_Dumont_API_Client( $this->settings );
	}

	/**
	 * The SN site's id, which the console APIs are addressed by: looked up by name.
	 *
	 * @return string|WP_Error
	 */
	public function site_id() {
		$sites = $this->client->call( 'GET', '/api/sn' );
		if ( is_wp_error( $sites ) ) {
			return $sites;
		}
		foreach ( $sites as $site ) {
			if ( 0 === strcasecmp( (string) ( $site['name'] ?? '' ), (string) $this->settings['site_name'] ) ) {
				return (string) $site['id'];
			}
		}

		/* translators: %s: the SN site's name. */
		return new WP_Error( 'viglet_dumont_no_site', sprintf( __( 'Turing has no SN site named %s.', 'viglet-dumont' ), $this->settings['site_name'] ) );
	}

	/**
	 * Every synonym and promoted result on the site, the plugin's own apart.
	 *
	 * @return array|WP_Error {synonyms: {ours, others}, spotlights: {ours, others}}
	 */
	public function all() {
		$site = $this->site_id();
		if ( is_wp_error( $site ) ) {
			return $site;
		}
		$synonyms   = $this->client->call( 'GET', "/api/sn/{$site}/synonym" );
		$spotlights = $this->client->call( 'GET', "/api/sn/{$site}/spotlight" );
		foreach ( array( $synonyms, $spotlights ) as $answer ) {
			if ( is_wp_error( $answer ) ) {
				return $answer;
			}
		}

		$synonym_rows   = array_map(
			fn ( array $row ) => array(
				'id'    => (string) $row['id'],
				'terms' => array_values( (array) ( $row['terms'] ?? array() ) ),
				'ours'  => $this->is_ours( $row ),
			),
			$synonyms
		);
		$spotlight_rows = array_map(
			fn ( array $row ) => array(
				'id'    => (string) $row['id'],
				'query' => implode( ', ', array_column( (array) ( $row['turSNSiteSpotlightTerms'] ?? array() ), 'name' ) ),
				'posts' => array_map(
					static fn ( array $document ) => array(
						'title' => (string) ( $document['title'] ?? '' ),
						'url'   => (string) ( $document['link'] ?? '' ),
					),
					$this->by_position( (array) ( $row['turSNSiteSpotlightDocuments'] ?? array() ) )
				),
				'ours'  => $this->is_ours( $row ),
			),
			$spotlights
		);

		$boosts = $this->boosts();

		return array(
			'synonyms'   => $synonym_rows,
			'spotlights' => $spotlight_rows,
			'boosts'     => is_wp_error( $boosts ) ? null : $boosts,
			'types'      => array_map(
				static fn ( string $type ) => array(
					'name'  => $type,
					'label' => (string) ( get_post_type_object( $type )->labels->name ?? $type ),
				),
				array_values( viglet_dumont_get_indexable_types() )
			),
		);
	}

	/**
	 * Make words mean the same in search, and apply the site's synonyms to the engine.
	 *
	 * @param string[] $terms Two or more words or phrases.
	 * @return true|WP_Error
	 */
	public function add_synonym( array $terms ) {
		$terms = array_values( array_unique( array_filter( array_map( 'trim', $terms ) ) ) );
		if ( count( $terms ) < 2 ) {
			return new WP_Error( 'viglet_dumont_synonym', __( 'A synonym needs two words or more.', 'viglet-dumont' ) );
		}
		$site = $this->site_id();
		if ( is_wp_error( $site ) ) {
			return $site;
		}
		$saved = $this->client->call(
			'POST',
			"/api/sn/{$site}/synonym",
			array(
				'name'     => self::PREFIX . implode( ', ', $terms ),
				'type'     => 'REGULAR',
				'language' => $this->settings['locale'],
				'terms'    => $terms,
				'enabled'  => true,
			)
		);

		return is_wp_error( $saved ) ? $saved : $this->apply_synonyms( $site );
	}

	/**
	 * Remove a synonym the plugin made.
	 *
	 * @param string $id The synonym's id.
	 * @return true|WP_Error
	 */
	public function remove_synonym( string $id ) {
		return $this->remove( 'synonym', $id );
	}

	/**
	 * Put up to three posts at the head of the results for a query, as a Turing spotlight.
	 *
	 * @param string $query    The query, as visitors type it.
	 * @param int[]  $post_ids The posts, in order.
	 * @return true|WP_Error
	 */
	public function add_spotlight( string $query, array $post_ids ) {
		$query = trim( $query );
		$posts = array_slice( array_filter( array_map( 'get_post', array_map( 'intval', $post_ids ) ) ), 0, 3 );
		if ( '' === $query || ! $posts ) {
			return new WP_Error( 'viglet_dumont_spotlight', __( 'Name a search and at least one post.', 'viglet-dumont' ) );
		}
		$site = $this->site_id();
		if ( is_wp_error( $site ) ) {
			return $site;
		}
		$indexer   = new Viglet_Dumont_Indexer( $this->settings );
		$documents = array();
		foreach ( array_values( $posts ) as $i => $post ) {
			$documents[] = array(
				'position'    => $i + 1,
				'title'       => get_the_title( $post ),
				'type'        => $post->post_type,
				'referenceId' => $indexer->build_doc_id( $post->ID ),
				'content'     => wp_strip_all_tags( get_the_excerpt( $post ) ),
				'link'        => get_permalink( $post ),
			);
		}
		$saved = $this->client->call(
			'POST',
			"/api/sn/{$site}/spotlight",
			array(
				'name'                        => self::PREFIX . $query,
				'language'                    => $this->settings['locale'],
				'turSNSite'                   => array( 'id' => $site ),
				'turSNSiteSpotlightTerms'     => array( array( 'name' => $query ) ),
				'turSNSiteSpotlightDocuments' => $documents,
			)
		);

		return is_wp_error( $saved ) ? $saved : true;
	}

	/**
	 * Remove a promoted result the plugin made.
	 *
	 * @param string $id The spotlight's id.
	 * @return true|WP_Error
	 */
	public function remove_spotlight( string $id ) {
		return $this->remove( 'spotlight', $id );
	}

	/**
	 * The boosts the plugin owns, read back from the site's ranking expressions: newer first,
	 * sticky posts first, and a post type up or down.
	 *
	 * @return array{recency: bool, sticky: bool, types: array<string, string>}|WP_Error
	 */
	public function boosts() {
		$site = $this->site_id();
		if ( is_wp_error( $site ) ) {
			return $site;
		}
		$expressions = $this->client->call( 'GET', "/api/sn/{$site}/ranking-expression" );
		if ( is_wp_error( $expressions ) ) {
			return $expressions;
		}
		$boosts = array(
			'recency' => false,
			'sticky'  => false,
			'types'   => array(),
		);
		foreach ( $expressions as $expression ) {
			if ( ! $this->is_ours( $expression ) ) {
				continue;
			}
			$condition = (array) ( ( $expression['turSNRankingConditions'] ?? array() )[0] ?? array() );
			$attribute = (string) ( $condition['attribute'] ?? '' );
			if ( 'publication_date' === $attribute ) {
				$boosts['recency'] = true;
			} elseif ( 'sticky' === $attribute ) {
				$boosts['sticky'] = true;
			} elseif ( 'type' === $attribute ) {
				$boosts['types'][ (string) $condition['value'] ] = 2 === (int) ( $condition['condition'] ?? 1 ) ? 'down' : 'up';
			}
		}

		return $boosts;
	}

	/**
	 * Make the plugin's boosts what the owner asked for: its ranking expressions are replaced
	 * whole, so a switch turned off leaves nothing behind. An operator's are not touched.
	 *
	 * @param bool                  $recency Newer posts rank higher.
	 * @param bool                  $sticky  Sticky posts rank higher.
	 * @param array<string, string> $types   Post type to 'up' or 'down'.
	 * @return true|WP_Error
	 */
	public function set_boosts( bool $recency, bool $sticky, array $types ) {
		$site = $this->site_id();
		if ( is_wp_error( $site ) ) {
			return $site;
		}
		$expressions = $this->client->call( 'GET', "/api/sn/{$site}/ranking-expression" );
		if ( is_wp_error( $expressions ) ) {
			return $expressions;
		}
		foreach ( $expressions as $expression ) {
			if ( $this->is_ours( $expression ) ) {
				$removed = $this->client->call( 'DELETE', "/api/sn/{$site}/ranking-expression/" . rawurlencode( (string) $expression['id'] ) );
				if ( is_wp_error( $removed ) ) {
					return $removed;
				}
			}
		}

		// A recency boost is Turing's date form, `asc` on a DATE field; the rest match a value.
		$wanted = array();
		if ( $recency ) {
			$wanted[] = array( __( 'newer first', 'viglet-dumont' ), 'publication_date', 1, 'asc', 5.0 );
		}
		if ( $sticky ) {
			$wanted[] = array( __( 'sticky posts first', 'viglet-dumont' ), 'sticky', 1, 'true', 3.0 );
		}
		foreach ( $types as $type => $direction ) {
			$type = sanitize_key( (string) $type );
			if ( '' === $type || ! in_array( $direction, array( 'up', 'down' ), true ) ) {
				continue;
			}
			// Down is every other type up: Turing boosts, it never sinks.
			$wanted[] = array( "{$type} {$direction}", 'type', 'down' === $direction ? 2 : 1, $type, 2.0 );
		}
		foreach ( $wanted as list( $label, $attribute, $condition, $value, $weight ) ) {
			$saved = $this->client->call(
				'POST',
				"/api/sn/{$site}/ranking-expression",
				array(
					'name'                   => self::PREFIX . $label,
					'weight'                 => $weight,
					'turSNSite'              => array( 'id' => $site ),
					'turSNRankingConditions' => array(
						array(
							'attribute' => $attribute,
							'condition' => $condition,
							'value'     => $value,
						),
					),
				)
			);
			if ( is_wp_error( $saved ) ) {
				return $saved;
			}
		}//end foreach

		return true;
	}

	/**
	 * Synonyms Turing mined from the search log, to offer one by one; none is applied here.
	 * A query and its own wildcard or half-typed prefix are not synonyms, though mining
	 * proposes them (Turing T1125), so those are left out.
	 *
	 * @return array<int, array{terms: string[]}>|WP_Error
	 */
	public function suggestions() {
		$site = $this->site_id();
		if ( is_wp_error( $site ) ) {
			return $site;
		}
		$mined = $this->client->call( 'POST', "/api/sn/{$site}/synonym/mine?locale=" . rawurlencode( (string) $this->settings['locale'] ) . '&limit=50' );
		if ( is_wp_error( $mined ) ) {
			return $mined;
		}

		$suggestions = array();
		foreach ( $mined as $candidate ) {
			$terms = array_values( array_unique( array_map( 'strval', (array) ( $candidate['terms'] ?? array() ) ) ) );
			if ( count( $terms ) < 2 || array_filter( $terms, static fn ( $term ) => str_contains( $term, '*' ) ) ) {
				continue;
			}
			$prefixed = false;
			foreach ( $terms as $a ) {
				foreach ( $terms as $b ) {
					$prefixed = $prefixed || ( $a !== $b && str_starts_with( strtolower( $b ), strtolower( $a ) ) );
				}
			}
			if ( ! $prefixed ) {
				$suggestions[] = array( 'terms' => $terms );
			}
		}

		return $suggestions;
	}

	/**
	 * What the site's search answers for a query now, as titles: the "Try it" preview.
	 *
	 * @param string $query The query.
	 * @return array<int, array{title: string, url: string, featured: bool}>|WP_Error
	 */
	public function try_query( string $query ) {
		$indexer  = new Viglet_Dumont_Indexer( $this->settings );
		$response = $this->client->search( '' === trim( $query ) ? '*' : $query, 1, 5, array( 'fq[]' => array( 'site_key:' . $indexer->site_key() ) ) );
		if ( is_wp_error( $response ) ) {
			return $response;
		}
		$rows = array();
		foreach ( (array) ( $response['widget']['spotlights'] ?? array() ) as $spot ) {
			$rows[] = array(
				'title'    => wp_strip_all_tags( (string) ( $spot['title'] ?? '' ) ),
				'url'      => (string) ( $spot['link'] ?? $spot['url'] ?? '' ),
				'featured' => true,
			);
		}
		foreach ( (array) ( $response['results']['document'] ?? array() ) as $document ) {
			$fields = (array) ( $document['fields'] ?? array() );
			$rows[] = array(
				'title'    => wp_strip_all_tags( (string) ( $fields['title'] ?? '' ) ),
				'url'      => (string) ( $fields['url'] ?? '' ),
				'featured' => false,
			);
		}

		return $rows;
	}

	/**
	 * Remove one of the plugin's own items; an operator's stays as the console made it.
	 *
	 * @param string $kind synonym or spotlight.
	 * @param string $id   The item's id.
	 * @return true|WP_Error
	 */
	private function remove( string $kind, string $id ) {
		$site = $this->site_id();
		if ( is_wp_error( $site ) ) {
			return $site;
		}
		$item = $this->client->call( 'GET', "/api/sn/{$site}/{$kind}/" . rawurlencode( $id ) );
		if ( is_wp_error( $item ) ) {
			return $item;
		}
		if ( ! $item || ! $this->is_ours( $item ) ) {
			return new WP_Error( 'viglet_dumont_not_ours', __( 'Only what this plugin added can be removed here; change the rest in Turing\'s console.', 'viglet-dumont' ) );
		}
		$removed = $this->client->call( 'DELETE', "/api/sn/{$site}/{$kind}/" . rawurlencode( $id ) );
		if ( is_wp_error( $removed ) ) {
			return $removed;
		}

		return 'synonym' === $kind ? $this->apply_synonyms( $site ) : true;
	}

	/**
	 * Push the site's synonyms to its search engine; a stored synonym does nothing until then.
	 *
	 * @param string $site The SN site's id.
	 * @return true|WP_Error
	 */
	private function apply_synonyms( string $site ) {
		$applied = $this->client->call( 'POST', "/api/sn/{$site}/synonym/apply", array() );

		return is_wp_error( $applied ) ? $applied : true;
	}

	/**
	 * Whether the plugin made an item: its name carries the prefix.
	 *
	 * @param array $item A synonym or spotlight as Turing answers it.
	 */
	private function is_ours( array $item ): bool {
		return str_starts_with( (string) ( $item['name'] ?? '' ), self::PREFIX );
	}

	/**
	 * Spotlight documents in their position order.
	 *
	 * @param array $documents The documents.
	 */
	private function by_position( array $documents ): array {
		usort( $documents, static fn ( $a, $b ) => (int) ( $a['position'] ?? 0 ) <=> (int) ( $b['position'] ?? 0 ) );

		return $documents;
	}
}
