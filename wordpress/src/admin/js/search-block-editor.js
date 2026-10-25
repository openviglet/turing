/**
 * The Turing search block in the editor: a placeholder with the block's two settings.
 * The block renders on the server (render.php), so there is nothing to save.
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
	 * @param {Object}                       props.attributes    placeholder and showFacets.
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
					{ title: __( 'Search', 'viglet-dumont' ) },
					el( TextControl, {
						label: __( 'Placeholder', 'viglet-dumont' ),
						value: attributes.placeholder,
						onChange: ( placeholder ) =>
							setAttributes( { placeholder } ),
					} ),
					el( ToggleControl, {
						label: __( 'Show filters (facets)', 'viglet-dumont' ),
						checked: attributes.showFacets,
						onChange: ( showFacets ) =>
							setAttributes( { showFacets } ),
					} )
				)
			),
			el( Placeholder, {
				icon: 'search',
				label: __( 'Turing search', 'viglet-dumont' ),
				instructions: __(
					'A search box with suggestions, filters and results from Turing. It shows live on the published page.',
					'viglet-dumont'
				),
			} )
		);
	}

	wp.blocks.registerBlockType( 'viglet-dumont/search', {
		edit: Edit,
		save: () => null,
	} );
} )( window.wp );
