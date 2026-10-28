/**
 * The Ask the catalog block in the editor: its heading, placeholder and whether it offers the
 * Build my kit mode (D201). The conversation runs
 * only on the site (render.php and public/copilot.js), so the editor shows a placeholder and
 * there is nothing to save.
 *
 * Written against the wp.* globals WordPress already loads, so it needs no build.
 *
 * @param {Object} wp The wp.* globals.
 */
( function ( wp ) {
	'use strict';

	const el = wp.element.createElement;
	const __ = wp.i18n.__;
	const { useBlockProps, InspectorControls } = wp.blockEditor;
	const { PanelBody, Placeholder, TextControl, ToggleControl } =
		wp.components;

	/**
	 * The block in the editor.
	 *
	 * @param {Object}                       props               The block's props.
	 * @param {Object}                       props.attributes    heading, placeholder and kit.
	 * @param {(attributes: Object) => void} props.setAttributes Updates them.
	 */
	function Edit( { attributes, setAttributes } ) {
		return el(
			'div',
			useBlockProps(),
			el(
				InspectorControls,
				null,
				el(
					PanelBody,
					{ title: __( 'Ask the catalog', 'viglet-dumont' ) },
					el( TextControl, {
						label: __( 'Heading', 'viglet-dumont' ),
						placeholder: __( 'Ask the catalog', 'viglet-dumont' ),
						value: attributes.heading,
						onChange: ( heading ) => setAttributes( { heading } ),
					} ),
					el( TextControl, {
						label: __( 'Placeholder', 'viglet-dumont' ),
						placeholder: __(
							'A waterproof jacket under 200',
							'viglet-dumont'
						),
						value: attributes.placeholder,
						onChange: ( placeholder ) =>
							setAttributes( { placeholder } ),
					} ),
					el( ToggleControl, {
						label: __( 'Offer Build my kit', 'viglet-dumont' ),
						help: __(
							'A visitor describes a plan and gets the products it needs, grouped by need, to put in the cart together.',
							'viglet-dumont'
						),
						checked: attributes.kit,
						onChange: ( kit ) => setAttributes( { kit } ),
					} )
				)
			),
			el( Placeholder, {
				icon: 'format-chat',
				label:
					attributes.heading ||
					__( 'Ask the catalog', 'viglet-dumont' ),
				instructions: __(
					'Visitors ask in plain words and Turing’s catalog copilot answers from this site, citing its items. It shows only when Turing has a language model for it.',
					'viglet-dumont'
				),
			} )
		);
	}

	wp.blocks.registerBlockType( 'viglet-dumont/copilot', {
		edit: Edit,
		save: () => null,
	} );
} )( window.wp );
