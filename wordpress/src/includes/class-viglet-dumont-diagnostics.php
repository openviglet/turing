<?php
/**
 * Viglet Dumont Diagnostics
 *
 * The connection test the Setup page runs: each step checks one thing and, when it fails,
 * says which thing in words an editor can act on.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Runs the steps in order and stops at the first failure: a later step cannot be judged
 * when an earlier one failed (no key can be checked on a server that does not answer).
 */
class Viglet_Dumont_Diagnostics {

	/**
	 * The plugin settings under test.
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
	 * Every step's outcome, in order: `step` (url, reach, turing, key, site), `ok` and a
	 * `message`. `site_exists` says whether the SN site is there, for the "create it" offer.
	 *
	 * @return array{ok: bool, site_exists: bool|null, steps: array<int, array{step: string, ok: bool, message: string}>}
	 */
	public function run(): array {
		$steps = array();
		$url   = untrailingslashit( (string) $this->settings['server_url'] );

		$parts = wp_parse_url( $url );
		if ( empty( $parts['scheme'] ) || empty( $parts['host'] ) || ! in_array( $parts['scheme'], array( 'http', 'https' ), true ) ) {
			return $this->stop( $steps, 'url', __( 'The Turing URL must be a full http:// or https:// address, e.g. https://search.example.org.', 'viglet-dumont' ) );
		}
		$steps[] = $this->pass( 'url', __( 'The URL is well formed.', 'viglet-dumont' ) );

		$response = wp_remote_get( $url . '/api/discovery', array( 'timeout' => 10 ) );
		if ( is_wp_error( $response ) ) {
			return $this->stop( $steps, 'reach', $this->explain_transport( $response, (string) $parts['host'] ) );
		}
		$steps[] = $this->pass( 'reach', __( 'The server answers.', 'viglet-dumont' ) );

		$code = (int) wp_remote_retrieve_response_code( $response );
		$body = json_decode( (string) wp_remote_retrieve_body( $response ), true );
		if ( $code < 200 || $code >= 300 || ! is_array( $body ) ) {
			return $this->stop(
				$steps,
				'turing',
				/* translators: %d: the HTTP status the server answered with. */
				sprintf( __( 'The server answered HTTP %d where Turing answers its discovery document: this URL is not a Turing, or a proxy is in the way.', 'viglet-dumont' ), $code )
			);
		}
		$steps[] = $this->pass( 'turing', __( 'It is a Turing.', 'viglet-dumont' ) );

		if ( '' === (string) ( $this->settings['api_key'] ?? '' ) ) {
			return $this->stop( $steps, 'key', __( 'No API key is set. Create one in Turing under API Tokens and paste it here.', 'viglet-dumont' ) );
		}

		$client  = new Viglet_Dumont_API_Client( $this->settings );
		$mapping = new Viglet_Dumont_Mapping( $this->settings );
		$plan    = $client->plan( $mapping->manifest() );
		if ( is_wp_error( $plan ) ) {
			$message = $plan->get_error_message();
			$refused = preg_match( '/HTTP (401|403|422)\b/', $message );
			return $this->stop(
				$steps,
				'key',
				$refused
					? __( 'Turing refused the API key: it is wrong, expired, or its user cannot manage SN sites.', 'viglet-dumont' )
					: $message
			);
		}
		$steps[] = $this->pass( 'key', __( 'Turing accepts the API key.', 'viglet-dumont' ) );

		$exists  = ! empty( $plan['siteExists'] );
		$steps[] = $exists
			? $this->pass(
				'site',
				/* translators: %s: the SN site's name. */
				sprintf( __( 'The SN site %s exists.', 'viglet-dumont' ), $this->settings['site_name'] )
			)
			: $this->fail(
				'site',
				/* translators: %s: the SN site's name. */
				sprintf( __( 'There is no SN site named %s in Turing yet. Create it from here, or check the name.', 'viglet-dumont' ), $this->settings['site_name'] )
			);

		return array(
			'ok'          => $exists,
			'site_exists' => $exists,
			'steps'       => $steps,
		);
	}

	/**
	 * Name a transport failure: the host does not resolve, TLS failed, or nothing answers.
	 *
	 * @param WP_Error $error The failure.
	 * @param string   $host  The host tried.
	 */
	private function explain_transport( WP_Error $error, string $host ): string {
		$message = $error->get_error_message();

		if ( preg_match( '/resolve host|Could not resolve|getaddrinfo|Name or service not known/i', $message ) ) {
			/* translators: %s: the host name. */
			return sprintf( __( '%s does not resolve (DNS): check the address for typos, or whether this server can see that network.', 'viglet-dumont' ), $host );
		}
		if ( preg_match( '/SSL|TLS|certificate/i', $message ) ) {
			/* translators: %s: the transport's own message. */
			return sprintf( __( 'The secure connection failed (TLS): the certificate is invalid, expired or self-signed. Details: %s', 'viglet-dumont' ), $message );
		}
		if ( preg_match( '/timed out|timeout/i', $message ) ) {
			/* translators: %s: the host name. */
			return sprintf( __( '%s did not answer in time: a firewall may be dropping the connection, or Turing is down.', 'viglet-dumont' ), $host );
		}
		if ( preg_match( '/refused|Failed to connect/i', $message ) ) {
			/* translators: %s: the host name. */
			return sprintf( __( 'Nothing is listening at %s on that port: Turing is down, or the port is wrong.', 'viglet-dumont' ), $host );
		}

		return $message;
	}

	/**
	 * A passed step.
	 *
	 * @param string $step    The step.
	 * @param string $message What was found.
	 */
	private function pass( string $step, string $message ): array {
		return array(
			'step'    => $step,
			'ok'      => true,
			'message' => $message,
		);
	}

	/**
	 * A failed step.
	 *
	 * @param string $step    The step.
	 * @param string $message What failed, in words to act on.
	 */
	private function fail( string $step, string $message ): array {
		return array(
			'step'    => $step,
			'ok'      => false,
			'message' => $message,
		);
	}

	/**
	 * The outcome when a step fails and the rest cannot be judged.
	 *
	 * @param array  $steps   The steps passed so far.
	 * @param string $step    The failing step.
	 * @param string $message What failed.
	 * @return array{ok: bool, site_exists: bool|null, steps: array}
	 */
	private function stop( array $steps, string $step, string $message ): array {
		$steps[] = $this->fail( $step, $message );

		return array(
			'ok'          => false,
			'site_exists' => null,
			'steps'       => $steps,
		);
	}
}
