<?php
/**
 * The HTML a block of this plugin may print.
 *
 * @package VigletTuring
 */

if ( ! defined( 'ABSPATH' ) ) {
	exit;
}

/**
 * The allowlist the plugin's block output passes through when it is echoed: what a post may
 * hold, plus the form controls, templates and dialog the search and assistant blocks draw,
 * and the ARIA states their scripts bind. Interactivity API directives (`data-wp-on--click`)
 * are `data-*` attributes, which kses keeps on every tag.
 *
 * @since 1.0.0
 */
class Viglet_Turing_Kses {

	/**
	 * Attributes allowed on every tag: kses's own global ones (which the tags below, outside
	 * the post allowlist, do not get by themselves) and the ARIA states the blocks bind.
	 */
	private const GLOBAL_ATTRIBUTES = array(
		'aria-activedescendant',
		'aria-autocomplete',
		'aria-busy',
		'aria-controls',
		'aria-current',
		'aria-describedby',
		'aria-details',
		'aria-expanded',
		'aria-haspopup',
		'aria-hidden',
		'aria-label',
		'aria-labelledby',
		'aria-live',
		'aria-modal',
		'aria-pressed',
		'aria-selected',
		'class',
		'data-*',
		'dir',
		'hidden',
		'id',
		'lang',
		'role',
		'style',
		'tabindex',
		'title',
	);

	/**
	 * Tags a post may not hold that the blocks draw, with their own attributes.
	 */
	private const EXTRA = array(
		'input'    => array(
			'type'         => true,
			'name'         => true,
			'value'        => true,
			'placeholder'  => true,
			'autocomplete' => true,
			'checked'      => true,
			'disabled'     => true,
			'readonly'     => true,
			'required'     => true,
			'min'          => true,
			'max'          => true,
			'step'         => true,
			'inputmode'    => true,
			'enterkeyhint' => true,
			'spellcheck'   => true,
		),
		'form'     => array(
			'action' => true,
			'method' => true,
			'name'   => true,
		),
		'button'   => array(
			'type'     => true,
			'name'     => true,
			'value'    => true,
			'disabled' => true,
		),
		'textarea' => array(
			'name'        => true,
			'rows'        => true,
			'cols'        => true,
			'placeholder' => true,
			'disabled'    => true,
			'readonly'    => true,
			'required'    => true,
		),
		'select'   => array(
			'name'     => true,
			'value'    => true,
			'multiple' => true,
			'disabled' => true,
			'required' => true,
			'size'     => true,
		),
		'option'   => array(
			'value'    => true,
			'selected' => true,
			'disabled' => true,
			'label'    => true,
		),
		'optgroup' => array(
			'label'    => true,
			'disabled' => true,
		),
		'template' => array(),
		'dialog'   => array( 'open' => true ),
	);

	/**
	 * The allowed tags and attributes, for `wp_kses()`.
	 *
	 * @return array<string, array<string, bool>>
	 */
	public static function block_html(): array {
		$allowed = wp_kses_allowed_html( 'post' );
		foreach ( self::EXTRA as $tag => $attributes ) {
			$allowed[ $tag ] = array_merge( $allowed[ $tag ] ?? array(), $attributes );
		}
		$global = array_fill_keys( self::GLOBAL_ATTRIBUTES, true );
		foreach ( $allowed as $tag => $attributes ) {
			$allowed[ $tag ] = array_merge( is_array( $attributes ) ? $attributes : array(), $global );
		}

		return $allowed;
	}
}
