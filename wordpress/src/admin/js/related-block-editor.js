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
					{ title: __( 'Related content', 'viglet-dumont' ) },
					el( TextControl, {
						label: __( 'Heading', 'viglet-dumont' ),
						placeholder: __( 'Related posts', 'viglet-dumont' ),
						value: attributes.heading,
						onChange: ( heading ) => setAttributes( { heading } ),
					} ),
					el( RangeControl, {
						label: __( 'How many', 'viglet-dumont' ),
						min: 1,
						max: 12,
						value: attributes.count,
						onChange: ( count ) => setAttributes( { count } ),
					} ),
					el( SelectControl, {
						label: __( 'Alike by', 'viglet-dumont' ),
						help: __(
							'Meaning needs a site with embeddings in Turing; without them, shared words are used.',
							'viglet-dumont'
						),
						value: attributes.mode,
						options: [
							{
								label: __( 'Turing’s choice', 'viglet-dumont' ),
								value: '',
							},
							{
								label: __( 'Shared words', 'viglet-dumont' ),
								value: 'MLT',
							},
							{
								label: __( 'Meaning', 'viglet-dumont' ),
								value: 'VECTOR',
							},
						],
						onChange: ( mode ) => setAttributes( { mode } ),
					} ),
					el( ToggleControl, {
						label: __( 'Only the same post type', 'viglet-dumont' ),
						checked: attributes.sameType,
						onChange: ( sameType ) => setAttributes( { sameType } ),
					} ),
					el( SelectControl, {
						label: __( 'Show as', 'viglet-dumont' ),
						value: attributes.layout,
						options: [
							{
								label: __( 'A grid', 'viglet-dumont' ),
								value: 'grid',
							},
							{
								label: __( 'A list', 'viglet-dumont' ),
								value: 'list',
							},
						],
						onChange: ( layout ) => setAttributes( { layout } ),
					} ),
					...[
						[
							'showImage',
							__( 'Show the image', 'viglet-dumont' ),
						],
						[ 'showDate', __( 'Show the date', 'viglet-dumont' ) ],
						[
							'showExcerpt',
							__( 'Show the excerpt', 'viglet-dumont' ),
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
							label: __( 'Related content', 'viglet-dumont' ),
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
