<?php
/**
 * Viglet Dumont API Client
 *
 * Communicates with the Turing REST API for content indexing and search.
 * Uses the modern /api/sn/import JSON endpoint instead of the legacy Solr XML API.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * The HTTP client for Turing's import and search endpoints.
 */
class Viglet_Dumont_API_Client {

	/**
	 * Turing's base URL, without a trailing slash.
	 *
	 * @var string
	 */
	private string $server_url;

	/**
	 * The SN site documents are published to and searched in.
	 *
	 * @var string
	 */
	private string $site_name;

	/**
	 * Sent as the `Key` header when set.
	 *
	 * @var string
	 */
	private string $api_key;

	/**
	 * The locale every job item is published in.
	 *
	 * @var string
	 */
	private string $locale;

	/**
	 * The `source_apps` value that marks this plugin's documents.
	 *
	 * @var string
	 */
	private string $source_app;

	/**
	 * Read the connection from the given settings, or from the stored ones.
	 *
	 * @param array|null $settings The plugin settings; null reads them from the options table.
	 */
	public function __construct( ?array $settings = null ) {
		if ( null === $settings ) {
			$settings = viglet_dumont_get_settings();
		}

		$this->server_url = untrailingslashit( $settings['server_url'] );
		$this->site_name  = $settings['site_name'];
		$this->api_key    = $settings['api_key'] ?? '';
		$this->locale     = $settings['locale'] ?? 'en_US';
		// phpcs:ignore WordPress.WP.CapitalPDangit.MisspelledInText -- a source_apps value Turing matches on, not prose.
		$this->source_app = $settings['source_app'] ?? 'wordpress';
	}

	/**
	 * Test connectivity with the Turing server.
	 */
	public function ping(): array {
		$response = $this->request(
			'GET',
			'/api/sn/' . rawurlencode( $this->site_name ) . '/search',
			array(
				'q'    => '*',
				'rows' => 0,
			)
		);

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
				/* translators: %d: the HTTP status code Turing answered with. */
				: sprintf( __( 'Server returned HTTP %d', 'viglet-dumont' ), $code ),
			'code'    => $code,
		);
	}

	/**
	 * Import job items into Turing (index documents).
	 *
	 * @param array $job_items Array of TurSNJobItem-compatible arrays.
	 * @return array{success: bool, message: string, note: string|array, code: int} `code` is 0
	 *         when no response arrived; `note` is the message for the event log (D180).
	 */
	public function import( array $job_items ): array {
		$payload = array( 'snJobItems' => $job_items );

		$response = $this->request( 'POST', '/api/sn/import', null, $payload );

		if ( is_wp_error( $response ) ) {
			return $this->outcome( false, $response->get_error_message(), 0 );
		}

		$code = (int) wp_remote_retrieve_response_code( $response );
		$ok   = $code >= 200 && $code < 300;

		return $this->outcome(
			$ok,
			$ok ? Viglet_Dumont_Events::note( 'imported' ) : Viglet_Dumont_Events::note( 'import_failed', array( $code, wp_remote_retrieve_body( $response ) ) ),
			$code
		);
	}

	/**
	 * A write's result: whether it went through, its message now and as a note.
	 *
	 * @param bool         $ok   Whether it went through.
	 * @param string|array $note A note from Viglet_Dumont_Events::note(), or text as it came.
	 * @param int          $code The HTTP status, 0 when no response arrived.
	 * @return array{success: bool, message: string, note: string|array, code: int}
	 */
	private function outcome( bool $ok, $note, int $code ): array {
		return array(
			'success' => $ok,
			'message' => Viglet_Dumont_Events::text( $note ),
			'note'    => $note,
			'code'    => $code,
		);
	}

	/**
	 * Converge the SN site's field schema to a manifest (`POST /api/sn/manifest`).
	 *
	 * @param array $manifest `{name, locales, fields}` as Turing's manifest binds it.
	 * @return array{success: bool, message: string, note: string|array, code: int} `code` is 0 when no response arrived.
	 */
	public function provision( array $manifest ): array {
		$response = $this->request( 'POST', '/api/sn/manifest', null, $manifest );

		if ( is_wp_error( $response ) ) {
			return $this->outcome( false, $response->get_error_message(), 0 );
		}

		$code = (int) wp_remote_retrieve_response_code( $response );
		$body = json_decode( (string) wp_remote_retrieve_body( $response ), true );

		if ( $code >= 200 && $code < 300 ) {
			$note = Viglet_Dumont_Events::note( 'provisioned' );
		} elseif ( 409 === $code ) {
			$note = Viglet_Dumont_Events::note( 'provision_breaking', array( wp_json_encode( $body['breakingChanges'] ?? array() ) ) );
		} else {
			$note = Viglet_Dumont_Events::note( 'provision_failed', array( $code, is_array( $body ) && isset( $body['error'] ) ? $body['error'] : '' ) );
		}

		return $this->outcome( $code >= 200 && $code < 300, $note, $code );
	}

	/**
	 * What provisioning a manifest would change, without changing it (`POST /api/sn/manifest/plan`).
	 *
	 * @param array $manifest `{name, locales, fields}` as Turing's manifest binds it.
	 * @return array|WP_Error The plan: `fieldsToAdd`, `fieldsUnchanged`, `breakingChanges`, ...
	 */
	public function plan( array $manifest ) {
		$response = $this->request( 'POST', '/api/sn/manifest/plan', null, $manifest );
		if ( is_wp_error( $response ) ) {
			return $response;
		}

		$code = (int) wp_remote_retrieve_response_code( $response );
		$body = json_decode( (string) wp_remote_retrieve_body( $response ), true );
		if ( $code < 200 || $code >= 300 || ! is_array( $body ) ) {
			return new WP_Error(
				'plan_failed',
				/* translators: %d: the HTTP status code Turing answered with. */
				sprintf( __( 'Turing could not plan the fields (HTTP %d).', 'viglet-dumont' ), $code )
			);
		}

		return $body;
	}

	/**
	 * Delete documents from Turing index.
	 *
	 * @param array $job_items Array of TurSNJobItem-compatible arrays with DELETE action.
	 * @return array{success: bool, message: string, note: string|array, code: int}
	 */
	public function deindex( array $job_items ): array {
		$payload = array( 'snJobItems' => $job_items );

		$response = $this->request( 'POST', '/api/sn/import', null, $payload );

		if ( is_wp_error( $response ) ) {
			return $this->outcome( false, $response->get_error_message(), 0 );
		}

		$code = (int) wp_remote_retrieve_response_code( $response );
		$ok   = $code >= 200 && $code < 300;

		return $this->outcome( $ok, $ok ? Viglet_Dumont_Events::note( 'removed' ) : Viglet_Dumont_Events::note( 'deindex_failed', array( $code ) ), $code );
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
		$query_params = array_merge(
			array(
				'q'          => $query,
				'p'          => $page,
				'rows'       => $rows,
				'_setlocale' => $this->locale,
			),
			$params
		);

		$response = $this->request(
			'GET',
			'/api/sn/' . rawurlencode( $this->site_name ) . '/search',
			$query_params
		);

		if ( is_wp_error( $response ) ) {
			return $response;
		}

		$code = wp_remote_retrieve_response_code( $response );
		$body = wp_remote_retrieve_body( $response );

		if ( $code < 200 || $code >= 300 ) {
			return new WP_Error(
				'search_failed',
				sprintf(
					/* translators: %d: the HTTP status code Turing answered with. */
					__( 'Search failed with HTTP %d', 'viglet-dumont' ),
					$code
				)
			);
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
			'locale'         => $this->locale,
			'turSNJobAction' => 'CREATE',
			'siteNames'      => array( $this->site_name ),
			'attributes'     => array_merge(
				$attributes,
				array(
					'source_apps' => $this->source_app,
				)
			),
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
			'locale'         => $this->locale,
			'turSNJobAction' => 'DELETE',
			'siteNames'      => array( $this->site_name ),
			'attributes'     => array(
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
	 * @param array      $extra  More wp_remote_request arguments, merged over the defaults.
	 * @return array|WP_Error Response or error.
	 */
	private function request( string $method, string $path, ?array $query = null, ?array $body = null, array $extra = array() ) {
		$url = $this->server_url . $path;

		// Turing reads filters as a repeated `fq[]`, which add_query_arg cannot spell: it
		// writes an array as `fq[][0]=`. So they are appended by hand.
		$filters = (array) ( $query['fq[]'] ?? array() );
		unset( $query['fq[]'] );

		if ( $query ) {
			$url = add_query_arg( $query, $url );
		}
		foreach ( $filters as $filter ) {
			$url .= ( str_contains( $url, '?' ) ? '&' : '?' ) . 'fq%5B%5D=' . rawurlencode( (string) $filter );
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

		return wp_remote_request( $url, array_replace_recursive( $args, $extra ) );
	}

	/**
	 * The documents most like one already in the index (`/search/similar`): semantic on a
	 * site with vectors, lexical otherwise.
	 *
	 * @param string $doc_id The indexed document's id.
	 * @param int    $rows   How many.
	 * @param string $mode   `MLT` (lexical) or `VECTOR` (semantic); empty lets Turing choose.
	 * @return array<int, array{id: string, title: string, url: string}>|WP_Error An empty list when the id is not indexed.
	 */
	public function similar( string $doc_id, int $rows = 5, string $mode = '' ) {
		$params = array(
			'id'         => $doc_id,
			'rows'       => $rows,
			'_setlocale' => $this->locale,
		);
		if ( in_array( $mode, array( 'MLT', 'VECTOR' ), true ) ) {
			$params['mode'] = $mode;
		}
		$response = $this->request(
			'GET',
			'/api/sn/' . rawurlencode( $this->site_name ) . '/search/similar',
			$params
		);

		return $this->decoded( $response );
	}

	/**
	 * The documents near-identical to one already in the index (`/search/duplicates`), as a
	 * cluster; on a site without vectors the cluster is the document alone.
	 *
	 * @param string $doc_id The indexed document's id.
	 * @return array{duplicate?: bool, members?: array<int, array>}|WP_Error
	 */
	public function duplicates( string $doc_id ) {
		$response = $this->request(
			'GET',
			'/api/sn/' . rawurlencode( $this->site_name ) . '/search/duplicates',
			array(
				'id'         => $doc_id,
				'_setlocale' => $this->locale,
			)
		);

		return $this->decoded( $response );
	}

	/**
	 * Text Turing's default language model writes from some data and an instruction
	 * (`POST /api/v2/summary`), cached by Turing under the key until asked to regenerate.
	 *
	 * @param string $cache_key     What the answer is cached under.
	 * @param string $data          The text the model reads.
	 * @param string $system_prompt What to write from it.
	 * @param bool   $regenerate    Write it again rather than answer from the cache.
	 * @return array{success?: bool, error?: ?string, content?: ?string}|WP_Error
	 */
	public function summary( string $cache_key, string $data, string $system_prompt, bool $regenerate = false ) {
		return $this->decoded(
			$this->request(
				'POST',
				'/api/v2/summary',
				$regenerate ? array( 'regenerate' => 'true' ) : null,
				array(
					'cacheKey'     => $cache_key,
					'data'         => $data,
					'systemPrompt' => $system_prompt,
				),
				array( 'timeout' => 60 )
			)
		);
	}

	/**
	 * A call to one of Turing's console APIs (`/api/sn/<site id>/...`), JSON both ways, for the
	 * Search tuning screen (D172).
	 *
	 * @param string     $method HTTP method.
	 * @param string     $path   API path.
	 * @param array|null $body   Request body.
	 * @return array|WP_Error The decoded answer.
	 */
	public function call( string $method, string $path, ?array $body = null ) {
		return $this->decoded( $this->request( $method, $path, null, $body ) );
	}

	/**
	 * Whether Turing keeps search metrics (`turing.search.metrics.enabled`). Turing has no
	 * read for it (Turing T1149), so this posts a click with no document: off answers 204 with
	 * `X-Turing-Metrics: disabled` before reading it, on refuses it with 400 and keeps nothing.
	 *
	 * @return bool|null Null when Turing could not be asked.
	 */
	public function metrics_enabled(): ?bool {
		$response = $this->request( 'POST', '/api/sn/' . rawurlencode( $this->site_name ) . '/click', null, array( 'documentId' => '' ) );
		if ( is_wp_error( $response ) ) {
			return null;
		}
		if ( 'disabled' === strtolower( (string) wp_remote_retrieve_header( $response, 'x-turing-metrics' ) ) ) {
			return false;
		}

		return 400 === (int) wp_remote_retrieve_response_code( $response ) ? true : null;
	}

	/**
	 * A JSON answer as an array: an error for a failed request, an empty array for a 404
	 * (Turing's "not in the index").
	 *
	 * @param array|WP_Error $response The response.
	 * @return array|WP_Error
	 */
	private function decoded( $response ) {
		if ( is_wp_error( $response ) ) {
			return $response;
		}
		$code = (int) wp_remote_retrieve_response_code( $response );
		if ( 404 === $code ) {
			return array();
		}
		if ( $code < 200 || $code >= 300 ) {
			/* translators: %d: the HTTP status Turing answered with. */
			return new WP_Error( 'viglet_dumont_http', sprintf( __( 'Turing answered HTTP %d.', 'viglet-dumont' ), $code ) );
		}
		$decoded = json_decode( (string) wp_remote_retrieve_body( $response ), true );

		return is_array( $decoded ) ? $decoded : array();
	}

	/**
	 * Which of Turing's AI features this SN site can use (D169), each with the reason when it
	 * is off. Chat and the catalog copilot have a probe; answer and natural-language filters
	 * do not (Turing T1123), so this reads the start of an answer stream and parses a short
	 * phrase, which spends LLM calls when they are on. Call it on demand, never per page.
	 *
	 * @return array<int, array{feature: string, on: bool, reason: string}>
	 */
	public function ai_features(): array {
		$site        = '/api/sn/' . rawurlencode( $this->site_name );
		$json        = static function ( $response ): ?array {
			if ( is_wp_error( $response ) || 200 !== (int) wp_remote_retrieve_response_code( $response ) ) {
				return null;
			}
			$decoded = json_decode( (string) wp_remote_retrieve_body( $response ), true );
			return is_array( $decoded ) ? $decoded : null;
		};
		$unreachable = __( 'Turing did not answer.', 'viglet-dumont' );

		$chat = $json( $this->request( 'GET', $site . '/chat/enabled' ) );

		// The first event of the stream says whether the site may answer at all.
		$answer = $this->request(
			'GET',
			$site . '/answer',
			array(
				'q'          => 'test',
				'_setlocale' => $this->locale,
			),
			null,
			array(
				'timeout'             => 15,
				'limit_response_size' => 512,
				'headers'             => array( 'Accept' => 'text/event-stream' ),
			)
		);
		$stream = is_wp_error( $answer ) ? '' : (string) wp_remote_retrieve_body( $answer );
		preg_match( '/event:\s*error\s*\ndata:\s*(\{.*\})/', $stream, $error );
		$answer_reason = '';
		if ( is_wp_error( $answer ) ) {
			$answer_reason = $answer->get_error_message();
		} elseif ( $error ) {
			$answer_reason = (string) ( json_decode( $error[1], true )['message'] ?? '' );
		} elseif ( '' === $stream ) {
			$answer_reason = $unreachable;
		}

		$nl      = $json(
			$this->request(
				'POST',
				$site . '/nl-filters',
				null,
				array(
					'text'   => 'last week',
					'locale' => $this->locale,
				)
			)
		);
		$copilot = $json( $this->request( 'GET', $site . '/copilot/available' ) );

		return array(
			array(
				'feature' => 'answer',
				'on'      => '' === $answer_reason,
				'reason'  => $answer_reason,
			),
			array(
				'feature' => 'chat',
				'on'      => ! empty( $chat['enabled'] ),
				'reason'  => null === $chat ? $unreachable : ( empty( $chat['enabled'] ) ? (string) ( $chat['reason'] ?? '' ) : '' ),
			),
			array(
				'feature' => 'nl_filters',
				'on'      => ! empty( $nl['available'] ),
				'reason'  => null === $nl ? $unreachable : ( empty( $nl['available'] ) ? __( 'No language model is set up for it in Turing.', 'viglet-dumont' ) : '' ),
			),
			array(
				'feature' => 'copilot',
				'on'      => ! empty( $copilot['available'] ),
				'reason'  => null === $copilot ? $unreachable : ( empty( $copilot['available'] ) ? __( 'Turing has no catalog copilot set up.', 'viglet-dumont' ) : '' ),
			),
		);
	}

	/**
	 * The SN site this client publishes to.
	 */
	public function get_site_name(): string {
		return $this->site_name;
	}

	/**
	 * Turing's base URL, without a trailing slash.
	 */
	public function get_server_url(): string {
		return $this->server_url;
	}
}
