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
	 * The body of the HTTP 400 WordPress Playground answers with itself when the browser it
	 * runs in could not make a request (D237).
	 */
	private const PLAYGROUND_REFUSAL = 'Playground could not serve the request.';

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
				? __( 'Connection successful', 'viglet-turing-search' )
				/* translators: %d: the HTTP status code Turing answered with. */
				: sprintf( __( 'Server returned HTTP %d', 'viglet-turing-search' ), $code ),
			'code'    => $code,
		);
	}

	/**
	 * Import job items into Turing (index documents).
	 *
	 * Turing embeds each document for RAG while it imports (about half a second each), so the
	 * wait follows the batch: two seconds a document, never under 30, which the
	 * `viglet_dumont_http_timeout` filter can change (D256). A request that outlives it is
	 * `timed_out`: sent, with an outcome nobody knows, since Turing goes on importing it.
	 *
	 * @param array $job_items Array of TurSNJobItem-compatible arrays.
	 * @return array{success: bool, message: string, note: string|array, code: int, timed_out: bool}
	 *         `code` is 0 when no response arrived; `note` is the message for the event log (D180).
	 */
	public function import( array $job_items ): array {
		if ( Viglet_Dumont_Settings::demo() ) {
			return $this->outcome( false, self::demo_refusal()->get_error_message(), 0 );
		}
		$payload = array( 'snJobItems' => $job_items );

		/**
		 * Seconds to wait for Turing to answer one import.
		 *
		 * @param int $timeout   Two seconds a document, at least 30.
		 * @param int $documents How many documents the import carries.
		 */
		$timeout  = (int) apply_filters( 'viglet_dumont_http_timeout', max( 30, 2 * count( $job_items ) ), count( $job_items ) );
		$response = $this->request( 'POST', '/api/sn/import', null, $payload, array( 'timeout' => max( 1, $timeout ) ) );

		if ( is_wp_error( $response ) ) {
			$outcome              = $this->outcome( false, $response->get_error_message(), 0 );
			$outcome['timed_out'] = self::timed_out( $response );
			return $outcome;
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
	 * @return array{success: bool, message: string, note: string|array, code: int, timed_out: bool}
	 */
	private function outcome( bool $ok, $note, int $code ): array {
		return array(
			'success'   => $ok,
			'message'   => Viglet_Dumont_Events::text( $note ),
			'note'      => $note,
			'code'      => $code,
			'timed_out' => false,
		);
	}

	/**
	 * Whether a transport error is the wait running out (cURL error 28), not a refusal or a
	 * connection that never opened.
	 *
	 * @param WP_Error $error The error wp_remote_request() returned.
	 */
	private static function timed_out( WP_Error $error ): bool {
		return 1 === preg_match( '/cURL error 28\b|timed out/i', $error->get_error_message() );
	}

	/**
	 * Converge the SN site's field schema to a manifest (`POST /api/sn/manifest`).
	 *
	 * @param array $manifest `{name, locales, fields}` as Turing's manifest binds it.
	 * @return array{success: bool, message: string, note: string|array, code: int} `code` is 0 when no response arrived.
	 */
	public function provision( array $manifest ): array {
		if ( Viglet_Dumont_Settings::demo() ) {
			return $this->outcome( false, self::demo_refusal()->get_error_message(), 0 );
		}
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
		if ( Viglet_Dumont_Settings::demo() ) {
			return self::demo_refusal();
		}
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
				sprintf( __( 'Turing could not plan the fields (HTTP %d).', 'viglet-turing-search' ), $code )
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
		if ( Viglet_Dumont_Settings::demo() ) {
			return $this->outcome( false, self::demo_refusal()->get_error_message(), 0 );
		}
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
					__( 'Search failed with HTTP %d', 'viglet-turing-search' ),
					$code
				)
			);
		}

		$data = json_decode( $body, true );
		if ( json_last_error() !== JSON_ERROR_NONE ) {
			return new WP_Error( 'invalid_response', __( 'Invalid JSON response from server', 'viglet-turing-search' ) );
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
		return $this->build_delete_by_fields( array( 'id' => $doc_id ) );
	}

	/**
	 * Build a TurSNJobItem that deletes every document holding all these field values
	 * (Turing T1112), e.g. `site_key` for every post of one site of a shared SN site.
	 *
	 * @param array<string, string> $fields Field to the one value it must hold.
	 * @return array TurSNJobItem-compatible array.
	 */
	public function build_delete_by_fields( array $fields ): array {
		return array(
			'locale'         => $this->locale,
			'turSNJobAction' => 'DELETE',
			'siteNames'      => array( $this->site_name ),
			// For a delete by fields, source_apps is one more: only this plugin's documents go.
			'attributes'     => array_merge( $fields, array( 'source_apps' => $this->source_app ) ),
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

		$response = wp_remote_request( $url, array_replace_recursive( $args, $extra ) );

		return self::playground_refusal( $response, $this->server_url ) ?? $response;
	}

	/**
	 * What every write answers in demo mode (D238): the site holds no key and its index is
	 * built elsewhere, so nothing is sent.
	 */
	public static function demo_refusal(): WP_Error {
		return new WP_Error(
			'viglet_dumont_demo',
			sprintf(
				/* translators: %s: the constant or environment variable that turns demo mode on. */
				__( 'Demo mode (%s) is on: this site holds no Turing key and never writes to Turing, so nothing is indexed, removed or provisioned from here.', 'viglet-turing-search' ),
				Viglet_Dumont_Settings::DEMO
			)
		);
	}

	/**
	 * Inside WordPress Playground (D237), PHP's requests go out through the browser, and one the
	 * browser cannot make comes back as Playground's own HTTP 400, which would read as Turing
	 * refusing it. Measured on playground.wordpress.net (2026-10-30): a Turing on a public
	 * address is reached, directly when it allows the Playground origin and through
	 * Playground's proxy when it does not; one on a private address such as localhost never
	 * is, even with the origin allowed.
	 *
	 * Public for the connection check, which asks Turing's discovery document directly.
	 *
	 * @param array|WP_Error $response   What wp_remote_request returned.
	 * @param string         $server_url The Turing address that was asked.
	 * @return WP_Error|null The cause, named, or null when this is not that refusal.
	 */
	public static function playground_refusal( $response, string $server_url ): ?WP_Error {
		if ( is_wp_error( $response )
			|| 400 !== (int) wp_remote_retrieve_response_code( $response )
			|| self::PLAYGROUND_REFUSAL !== trim( (string) wp_remote_retrieve_body( $response ) ) ) {
			return null;
		}
		$software = sanitize_text_field( wp_unslash( $_SERVER['SERVER_SOFTWARE'] ?? '' ) );
		if ( ! defined( 'PLAYGROUND_PARENT_ORIGIN' ) && 'PHP.wasm' !== $software ) {
			return null;
		}

		return new WP_Error(
			'viglet_dumont_playground',
			sprintf(
				/* translators: %s: the Turing address the plugin is set to. */
				__( 'WordPress Playground could not reach Turing at %s. From Playground, Turing has to be on a public address: one on a private address, such as localhost, cannot be reached from the browser Playground runs in.', 'viglet-turing-search' ),
				$server_url
			)
		);
	}

	/**
	 * The documents most like one already in the index (`/search/similar`): semantic on a
	 * site with vectors, lexical otherwise.
	 *
	 * @param string $doc_id The indexed document's id.
	 * @param int    $rows   How many.
	 * @param string $mode   `MLT` (lexical) or `VECTOR` (semantic); empty lets Turing choose.
	 * @param array  $fq     Filter queries, as the search takes them (Turing T1132).
	 * @return array<int, array{id: string, title: string, url: string}>|WP_Error An empty list when the id is not indexed.
	 */
	public function similar( string $doc_id, int $rows = 5, string $mode = '', array $fq = array() ) {
		$params = array(
			'id'         => $doc_id,
			'rows'       => $rows,
			'_setlocale' => $this->locale,
			'fq[]'       => $fq,
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
	 * (`POST /api/v2/summary`), cached by Turing under the key until asked to regenerate. It
	 * names the SN site, so Turing counts the call in that site's cost and budget (D216).
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
					'site'         => $this->site_name,
				),
				array( 'timeout' => 60 )
			)
		);
	}

	/**
	 * The same search as search(), kept out of Turing's search metrics (`populateMetrics:
	 * false`): for a page drawn on the server whose script then asks again (D209), so one
	 * visitor's search counts once.
	 *
	 * @param array $params The query string, as search() builds it: q, p, rows, sort, fq[].
	 * @return array|WP_Error The decoded answer.
	 */
	public function search_unrecorded( array $params ) {
		return $this->decoded(
			$this->request(
				'POST',
				'/api/sn/' . rawurlencode( $this->site_name ) . '/search',
				array_merge( array( '_setlocale' => $this->locale ), $params ),
				array( 'populateMetrics' => false )
			)
		);
	}

	/**
	 * The audience personas the SN site's agent accepts for content-fit (Turing T1143): id,
	 * name and description, ordered by name, none when the site has no agent.
	 *
	 * @return array|WP_Error Turing's list, as it answered.
	 */
	public function personas() {
		return $this->call( 'GET', '/api/sn/' . rawurlencode( $this->site_name ) . '/personas' );
	}

	/**
	 * How well a text suits one of the site's audience personas (Turing T634): a score from 0
	 * to 100, a summary, what fits and the spans that do not, each with its reason.
	 *
	 * @param string $persona_id One of personas()'s ids.
	 * @param string $content    The text to judge.
	 * @param string $name       What the text is, for the model.
	 * @return array|WP_Error
	 */
	public function content_fit( string $persona_id, string $content, string $name ) {
		return $this->call(
			'POST',
			'/api/sn/' . rawurlencode( $this->site_name ) . '/persona/' . rawurlencode( $persona_id ) . '/content-fit',
			array(
				'content'    => $content,
				'sourceName' => $name,
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
	 * What a non-2xx answer says. A 403 with Turing's own reason says it (D190): a key bound
	 * to this site's partition is refused where Turing cannot confine it, and the reason names
	 * why, such as a set every site sharing the SN site shares.
	 *
	 * @param array $response The response.
	 */
	private static function refusal( array $response ): string {
		$code = (int) wp_remote_retrieve_response_code( $response );
		$body = json_decode( (string) wp_remote_retrieve_body( $response ), true );
		if ( 403 === $code && is_array( $body ) && is_string( $body['error'] ?? null ) && '' !== $body['error'] ) {
			/* translators: %s: Turing's reason for refusing the request. */
			return sprintf( __( 'Turing refused this site’s API key here: %s', 'viglet-turing-search' ), rtrim( $body['error'], '.' ) . '.' );
		}

		/* translators: %d: the HTTP status Turing answered with. */
		return sprintf( __( 'Turing answered HTTP %d.', 'viglet-turing-search' ), $code );
	}

	/**
	 * Whether an error is Turing refusing a key bound to this site's partition (D190): the
	 * feature behind it is off for this site, not broken.
	 *
	 * @param mixed $error What a call returned.
	 */
	public static function refused_by_partition( $error ): bool {
		if ( ! is_wp_error( $error ) ) {
			return false;
		}
		$data = $error->get_error_data();

		return is_array( $data ) && 403 === (int) ( $data['status'] ?? 0 )
			&& str_contains( $error->get_error_message(), 'partition' );
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
			return new WP_Error( 'viglet_dumont_http', self::refusal( $response ), array( 'status' => $code ) );
		}
		$decoded = json_decode( (string) wp_remote_retrieve_body( $response ), true );

		return is_array( $decoded ) ? $decoded : array();
	}

	/**
	 * Which of Turing's AI features this SN site can use (D169), each with the reason when it
	 * is off. Turing's capabilities probe answers all four from configuration, without calling
	 * a model, and a key bound to the site's partition may read it (Turing T1209, D190). A
	 * Turing without it falls back to the probes below. Call it on demand, never per page.
	 *
	 * @return array<int, array{feature: string, on: bool, reason: string}>
	 */
	public function ai_features(): array {
		$site         = '/api/sn/' . rawurlencode( $this->site_name );
		$capabilities = $this->request( 'GET', $site . '/ai/capabilities' );
		if ( ! is_wp_error( $capabilities ) && 200 === (int) wp_remote_retrieve_response_code( $capabilities ) ) {
			$decoded = json_decode( (string) wp_remote_retrieve_body( $capabilities ), true );
			if ( is_array( $decoded ) ) {
				return self::capabilities( $decoded );
			}
		}

		return $this->probed_ai_features();
	}

	/**
	 * The four rows from Turing's capabilities answer: `{chat, answer, nlFilters, copilot}`,
	 * each `{enabled, reason}` with the reason as Turing's enum name.
	 *
	 * @param array $capabilities The decoded answer.
	 * @return array<int, array{feature: string, on: bool, reason: string}>
	 */
	private static function capabilities( array $capabilities ): array {
		$rows = array();
		foreach ( array(
			'answer'     => 'answer',
			'chat'       => 'chat',
			'nl_filters' => 'nlFilters',
			'copilot'    => 'copilot',
		) as $feature => $key ) {
			$capability = (array) ( $capabilities[ $key ] ?? array() );
			$on         = ! empty( $capability['enabled'] );
			$rows[]     = array(
				'feature' => $feature,
				'on'      => $on,
				'reason'  => $on ? '' : self::ai_reason( $feature, (string) ( $capability['reason'] ?? '' ) ),
			);
		}

		return $rows;
	}

	/**
	 * Why an AI feature is off, in words, from the reason Turing names it by.
	 *
	 * @param string $feature The feature.
	 * @param string $reason  Turing's reason, e.g. `NO_GENAI`.
	 */
	private static function ai_reason( string $feature, string $reason ): string {
		$reasons = array(
			'NO_GENAI'          => __( 'Generative AI is off for this SN site in Turing.', 'viglet-turing-search' ),
			'NO_AGENT'          => __( 'The SN site has no AI agent in Turing.', 'viglet-turing-search' ),
			'AGENT_DISABLED'    => __( 'The SN site’s AI agent is turned off in Turing.', 'viglet-turing-search' ),
			'RAG_DISABLED'      => __( 'The SN site’s AI agent does not answer from the site’s content.', 'viglet-turing-search' ),
			'MISSING_LLM'       => __( 'The SN site’s AI agent has no language model.', 'viglet-turing-search' ),
			'MISSING_EMBEDDING' => __( 'The SN site’s AI agent has no embedding model.', 'viglet-turing-search' ),
			'MISSING_STORE'     => __( 'The SN site’s AI agent has no vector store.', 'viglet-turing-search' ),
		);
		if ( 'MISSING_DEFAULT_LLM' === $reason ) {
			return 'copilot' === $feature
				? __( 'Turing has no catalog copilot set up.', 'viglet-turing-search' )
				: __( 'No language model is set up for it in Turing.', 'viglet-turing-search' );
		}

		return $reasons[ $reason ] ?? $reason;
	}

	/**
	 * The AI features from one probe each, for a Turing without the capabilities probe. Answer
	 * and natural-language filters have none (Turing T1123), so this reads the start of an
	 * answer stream and parses a short phrase, which spends LLM calls when they are on.
	 *
	 * @return array<int, array{feature: string, on: bool, reason: string}>
	 */
	private function probed_ai_features(): array {
		$site        = '/api/sn/' . rawurlencode( $this->site_name );
		$json        = static function ( $response ): ?array {
			if ( is_wp_error( $response ) || 200 !== (int) wp_remote_retrieve_response_code( $response ) ) {
				return null;
			}
			$decoded = json_decode( (string) wp_remote_retrieve_body( $response ), true );
			return is_array( $decoded ) ? $decoded : null;
		};
		$unreachable = __( 'Turing did not answer.', 'viglet-turing-search' );

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
		} elseif ( 200 !== (int) wp_remote_retrieve_response_code( $answer ) ) {
			// A refusal is not a stream: without this, a 403's body read as "on".
			$answer_reason = self::refusal( $answer );
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
				'reason'  => null === $nl ? $unreachable : ( empty( $nl['available'] ) ? __( 'No language model is set up for it in Turing.', 'viglet-turing-search' ) : '' ),
			),
			array(
				'feature' => 'copilot',
				'on'      => ! empty( $copilot['available'] ),
				'reason'  => null === $copilot ? $unreachable : ( empty( $copilot['available'] ) ? __( 'Turing has no catalog copilot set up.', 'viglet-turing-search' ) : '' ),
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
