/**
 * The Turing search block in the editor: a placeholder with the block's settings (the box, the
 * filters and where they sit, the results' layout and parts, the sort, the page size).
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
	const {
		PanelBody,
		Placeholder,
		SelectControl,
		TextControl,
		ToggleControl,
	} = wp.components;

	/**
	 * The block in the editor.
	 *
	 * @param {Object}                       props               The block's props.
	 * @param {Object}                       props.attributes    placeholder, showFacets, showSort, perPage.
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
					} ),
					el( ToggleControl, {
						label: __(
							'Let visitors sort (relevance, newest, oldest)',
							'viglet-dumont'
						),
						checked: attributes.showSort,
						onChange: ( showSort ) => setAttributes( { showSort } ),
					} ),
					el( SelectControl, {
						label: __( 'Where the filters sit', 'viglet-dumont' ),
						value: attributes.facetsPosition,
						options: [
							{
								label: __( 'Left', 'viglet-dumont' ),
								value: 'left',
							},
							{
								label: __( 'Right', 'viglet-dumont' ),
								value: 'right',
							},
							{
								label: __(
									'Above the results',
									'viglet-dumont'
								),
								value: 'top',
							},
						],
						onChange: ( facetsPosition ) =>
							setAttributes( { facetsPosition } ),
					} ),
					el( SelectControl, {
						label: __( 'Results as', 'viglet-dumont' ),
						value: attributes.resultsLayout,
						options: [
							{
								label: __( 'A list', 'viglet-dumont' ),
								value: 'list',
							},
							{
								label: __( 'A grid', 'viglet-dumont' ),
								value: 'grid',
							},
						],
						onChange: ( resultsLayout ) =>
							setAttributes( { resultsLayout } ),
					} ),
					...[
						[
							'showImage',
							__( 'Show the image', 'viglet-dumont' ),
						],
						[ 'showType', __( 'Show the type', 'viglet-dumont' ) ],
						[ 'showDate', __( 'Show the date', 'viglet-dumont' ) ],
						[
							'showSnippet',
							__( 'Show the snippet', 'viglet-dumont' ),
						],
					].map( ( [ name, label ] ) =>
						el( ToggleControl, {
							key: name,
							label,
							checked: attributes[ name ],
							onChange: ( value ) =>
								setAttributes( { [ name ]: value } ),
						} )
					),
					el( SelectControl, {
						label: __( 'Results per page', 'viglet-dumont' ),
						value: String( attributes.perPage ),
						options: [ 5, 10, 20, 50 ].map( ( n ) => ( {
							label: String( n ),
							value: String( n ),
						} ) ),
						onChange: ( perPage ) =>
							setAttributes( { perPage: Number( perPage ) } ),
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
