/**
 * The Compare products block in the editor: its heading. The table is drawn on the site from
 * the products a shopper ticked (render.php), so the editor shows a placeholder and there is
 * nothing to save.
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
	const { PanelBody, Placeholder, TextControl } = wp.components;

	/**
	 * The block in the editor.
	 *
	 * @param {Object}                       props               The block's props.
	 * @param {Object}                       props.attributes    heading.
	 * @param {(attributes: Object) => void} props.setAttributes Updates it.
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
					{ title: __( 'Compare products', 'viglet-turing-search' ) },
					el( TextControl, {
						label: __( 'Heading', 'viglet-turing-search' ),
						placeholder: __(
							'Compare products',
							'viglet-turing-search'
						),
						value: attributes.heading,
						onChange: ( heading ) => setAttributes( { heading } ),
					} )
				)
			),
			el( Placeholder, {
				icon: 'columns',
				label:
					attributes.heading ||
					__( 'Compare products', 'viglet-turing-search' ),
				instructions: __(
					'Shows the products a shopper ticked Compare on in the search results, side by side. The search results link here once this page is published.',
					'viglet-turing-search'
				),
			} )
		);
	}

	wp.blocks.registerBlockType( 'viglet-turing/compare', {
		edit: Edit,
		save: () => null,
	} );
} )( window.wp );
