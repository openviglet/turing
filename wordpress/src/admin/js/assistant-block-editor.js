/**
 * The Site assistant block in the editor: its button text and the first questions it suggests.
 * The conversation runs only on the site (render.php and public/assistant.js), so the editor
 * shows a placeholder and there is nothing to save.
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
	const { PanelBody, Placeholder, TextControl, TextareaControl } =
		wp.components;

	/**
	 * The block in the editor.
	 *
	 * @param {Object}                       props               The block's props.
	 * @param {Object}                       props.attributes    heading and suggestions.
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
					{ title: __( 'Site assistant', 'viglet-dumont' ) },
					el( TextControl, {
						label: __( 'Button and heading', 'viglet-dumont' ),
						placeholder: __( 'Ask this site', 'viglet-dumont' ),
						value: attributes.heading,
						onChange: ( heading ) => setAttributes( { heading } ),
					} ),
					el( TextareaControl, {
						label: __( 'Suggested questions', 'viglet-dumont' ),
						help: __(
							'One per line. Left empty, the assistant suggests this week’s popular searches.',
							'viglet-dumont'
						),
						value: attributes.suggestions,
						onChange: ( suggestions ) =>
							setAttributes( { suggestions } ),
					} )
				)
			),
			el( Placeholder, {
				icon: 'format-chat',
				label:
					attributes.heading ||
					__( 'Ask this site', 'viglet-dumont' ),
				instructions: __(
					'A button that opens a chat answering from this site’s posts, with the posts it used. It shows only while Turing’s chat is on for this site.',
					'viglet-dumont'
				),
			} )
		);
	}

	wp.blocks.registerBlockType( 'viglet-dumont/assistant', {
		edit: Edit,
		save: () => null,
	} );
} )( window.wp );
