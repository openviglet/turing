<?php
/**
 * The Site assistant (D175): a conversation with Turing's chat over this site's posts, as a
 * block or, with the setting on, as a launcher in a corner of every page.
 *
 * @package Viglet_Dumont
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * Puts the assistant in every page's footer when the site asks for it, and picks the first
 * questions it suggests.
 */
class Viglet_Dumont_Assistant {

	/** The block, which the launcher renders too. */
	public const BLOCK = 'viglet-dumont/assistant';

	/** The most first questions suggested. */
	public const MAX_SUGGESTIONS = 4;

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
	}

	/**
	 * With "Site assistant on every page" on, load the block's module and style on the site
	 * and draw the launcher at the end of the page.
	 */
	public function register(): void {
		if ( empty( $this->settings['assistant_launcher'] ) ) {
			return;
		}
		add_action( 'wp_enqueue_scripts', array( $this, 'enqueue' ) );
		// Before the footer prints the interactivity state the block's render adds to.
		add_action( 'wp_footer', array( $this, 'launcher' ), 5 );
	}

	/**
	 * The module and style, asked for at enqueue time: a block theme prints its modules in the
	 * head, before the footer draws the launcher.
	 */
	public function enqueue(): void {
		if ( ! $this->wanted() ) {
			return;
		}
		wp_enqueue_script_module( 'viglet-dumont/assistant-block' );
		wp_enqueue_style( 'viglet-dumont-assistant-block' );
	}

	/**
	 * The launcher: the block, floating in a corner.
	 */
	public function launcher(): void {
		if ( ! $this->wanted() ) {
			return;
		}
		// Core escapes the block's output as it renders it.
		echo render_block( // phpcs:ignore WordPress.Security.EscapeOutput.OutputNotEscaped
			array(
				'blockName'    => self::BLOCK,
				'attrs'        => array( 'floating' => true ),
				'innerBlocks'  => array(),
				'innerHTML'    => '',
				'innerContent' => array(),
			)
		);
	}

	/**
	 * Whether this page gets the launcher: a registered block, and none already in the page.
	 */
	private function wanted(): bool {
		if ( ! WP_Block_Type_Registry::get_instance()->is_registered( self::BLOCK ) ) {
			return false;
		}
		$post = is_singular() ? get_queried_object() : null;

		return ! ( $post instanceof WP_Post && has_block( self::BLOCK, $post ) );
	}

	/**
	 * The first questions offered: the block's own, one per line, else the week's popular
	 * searches (D202), which are checked to find something on the site.
	 *
	 * @param string $lines The block's suggestions, one per line.
	 * @return string[]
	 */
	public function suggestions( string $lines ): array {
		$own = Viglet_Dumont_Trending::lines( $lines );
		$top = $own ? $own : ( new Viglet_Dumont_Trending( $this->settings ) )->chips();

		return array_slice( $top, 0, self::MAX_SUGGESTIONS );
	}
}
