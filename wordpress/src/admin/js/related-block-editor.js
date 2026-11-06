/**
 * The Related content block in the editor: a live preview (rendered on the server, for the
 * post being edited) with the block's settings. The block renders on the server
 * (render.php), so there is nothing to save.
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
	const ServerSideRender = wp.serverSideRender;
	const {
		Disabled,
		PanelBody,
		Placeholder,
		RangeControl,
		SelectControl,
		TextControl,
		ToggleControl,
	} = wp.components;

	/**
	 * The block in the editor.
	 *
	 * @param {Object}                       props               The block's props.
	 * @param {Object}                       props.attributes    heading, count, mode, sameType, layout and the card parts.
	 * @param {(attributes: Object) => void} props.setAttributes Updates them.
	 */
	function Edit( { attributes, setAttributes } ) {
		// The post being edited; a template in the site editor has none (its id is a name).
		const postId = wp.data.select( 'core/editor' )?.getCurrentPostId();
		return el(
			'div',
			useBlockProps(),
			el(
				InspectorControls,
				null,
				el(
					PanelBody,
					{ title: __( 'Related content', 'viglet-turing-search' ) },
					el( TextControl, {
						label: __( 'Heading', 'viglet-turing-search' ),
						placeholder: __(
							'Related posts',
							'viglet-turing-search'
						),
						value: attributes.heading,
						onChange: ( heading ) => setAttributes( { heading } ),
					} ),
					el( RangeControl, {
						label: __( 'How many', 'viglet-turing-search' ),
						min: 1,
						max: 12,
						value: attributes.count,
						onChange: ( count ) => setAttributes( { count } ),
					} ),
					el( SelectControl, {
						label: __( 'Alike by', 'viglet-turing-search' ),
						help: __(
							'Meaning needs a site with embeddings in Turing; without them, shared words are used.',
							'viglet-turing-search'
						),
						value: attributes.mode,
						options: [
							{
								label: __(
									'Turing’s choice',
									'viglet-turing-search'
								),
								value: '',
							},
							{
								label: __(
									'Shared words',
									'viglet-turing-search'
								),
								value: 'MLT',
							},
							{
								label: __( 'Meaning', 'viglet-turing-search' ),
								value: 'VECTOR',
							},
						],
						onChange: ( mode ) => setAttributes( { mode } ),
					} ),
					el( ToggleControl, {
						label: __(
							'Only the same post type',
							'viglet-turing-search'
						),
						checked: attributes.sameType,
						onChange: ( sameType ) => setAttributes( { sameType } ),
					} ),
					// The sites listed in "Also search these sites" (D303).
					el( ToggleControl, {
						label: __(
							'Include the sites this one also searches',
							'viglet-turing-search'
						),
						checked: 'all' === attributes.sites,
						onChange: ( all ) =>
							setAttributes( { sites: all ? 'all' : 'own' } ),
					} ),
					el( SelectControl, {
						label: __( 'Show as', 'viglet-turing-search' ),
						value: attributes.layout,
						options: [
							{
								label: __( 'A grid', 'viglet-turing-search' ),
								value: 'grid',
							},
							{
								label: __( 'A list', 'viglet-turing-search' ),
								value: 'list',
							},
						],
						onChange: ( layout ) => setAttributes( { layout } ),
					} ),
					...[
						[
							'showImage',
							__( 'Show the image', 'viglet-turing-search' ),
						],
						[
							'showDate',
							__( 'Show the date', 'viglet-turing-search' ),
						],
						[
							'showExcerpt',
							__( 'Show the excerpt', 'viglet-turing-search' ),
						],
					].map( ( [ name, label ] ) =>
						el( ToggleControl, {
							key: name,
							label,
							checked: attributes[ name ],
							onChange: ( value ) =>
								setAttributes( { [ name ]: value } ),
						} )
					)
				)
			),
			// What visitors get, for this post. Disabled, so a click selects the block.
			el(
				Disabled,
				null,
				el( ServerSideRender, {
					block: 'viglet-dumont/related',
					attributes,
					urlQueryArgs: Number.isInteger( postId )
						? { post_id: postId }
						: {},
					LoadingResponsePlaceholder: () =>
						el( Placeholder, {
							icon: 'networking',
							label: __(
								'Related content',
								'viglet-turing-search'
							),
						} ),
				} )
			)
		);
	}

	wp.blocks.registerBlockType( 'viglet-dumont/related', {
		edit: Edit,
		save: () => null,
	} );
} )( window.wp );
