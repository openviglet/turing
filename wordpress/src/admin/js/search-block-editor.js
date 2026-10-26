/**
 * The Turing search block in the editor: a live preview (rendered on the server) with the block's settings (the box, the
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
	const {
		useBlockProps,
		useInnerBlocksProps,
		InnerBlocks,
		InspectorControls,
		store: blockEditorStore,
	} = wp.blockEditor;
	const { useDispatch, useSelect } = wp.data;
	const ServerSideRender = wp.serverSideRender;
	const {
		Button,
		Disabled,
		PanelBody,
		Placeholder,
		SelectControl,
		TextControl,
		ToggleControl,
	} = wp.components;

	// The block's parts as inner blocks (D185), in the order the block draws them by default.
	const parts = [
		[ 'viglet-dumont/search-box', 'search' ],
		[ 'viglet-dumont/search-filters', 'filter' ],
		[ 'viglet-dumont/search-results', 'list-view' ],
		[ 'viglet-dumont/search-pages', 'ellipsis' ],
	];

	/**
	 * The block in the editor: a live preview of its default order, or, once its parts are
	 * arranged, the parts themselves to reorder, with any other block between them.
	 *
	 * @param {Object}                       props               The block's props.
	 * @param {string}                       props.clientId      The block's id in the editor.
	 * @param {Object}                       props.attributes    placeholder, showFacets, showSort, perPage.
	 * @param {(attributes: Object) => void} props.setAttributes Updates them.
	 */
	function Edit( { clientId, attributes, setAttributes } ) {
		const arranged = useSelect(
			( select ) =>
				select( blockEditorStore ).getBlockCount( clientId ) > 0,
			[ clientId ]
		);
		const { replaceInnerBlocks } = useDispatch( blockEditorStore );
		const blockProps = useBlockProps();
		const innerBlocksProps = useInnerBlocksProps( blockProps, {
			renderAppender: false,
		} );
		const arrange = () =>
			replaceInnerBlocks(
				clientId,
				parts.map( ( [ name ] ) => wp.blocks.createBlock( name ) )
			);

		return el(
			'div',
			arranged ? innerBlocksProps : blockProps,
			el(
				InspectorControls,
				null,
				el(
					PanelBody,
					{
						title: __( 'Layout', 'viglet-dumont' ),
						initialOpen: false,
					},
					el(
						'p',
						null,
						arranged
							? __(
									'Its parts are blocks: move them, or put other blocks between them.',
									'viglet-dumont'
								)
							: __(
									'Make its parts (box, filters, results, pages) blocks you can reorder.',
									'viglet-dumont'
								)
					),
					el(
						Button,
						{
							variant: 'secondary',
							onClick: arranged
								? () => replaceInnerBlocks( clientId, [] )
								: arrange,
						},
						arranged
							? __( 'Back to the default order', 'viglet-dumont' )
							: __( 'Arrange its parts', 'viglet-dumont' )
					)
				),
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
					el( ToggleControl, {
						label: __(
							'Answer questions from this site’s content',
							'viglet-dumont'
						),
						help: __(
							'A search that reads as a question gets Turing’s answer, with its sources, above the results. Each one is an LLM call; it shows only when Turing cites sources.',
							'viglet-dumont'
						),
						checked: attributes.showAnswer,
						onChange: ( showAnswer ) =>
							setAttributes( { showAnswer } ),
					} ),
					el( ToggleControl, {
						label: __(
							'Suggest the filters a sentence names',
							'viglet-dumont'
						),
						help: __(
							'“Posts about php from last year” offers Type and Published filters to apply with one click. Each sentence is an LLM call; nothing is applied without a click.',
							'viglet-dumont'
						),
						checked: attributes.suggestFilters,
						onChange: ( suggestFilters ) =>
							setAttributes( { suggestFilters } ),
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
			// What visitors get: the block rendered on the server with Turing's answer to "*",
			// in the chosen layout and style. Disabled, so a click selects the block.
			arranged
				? innerBlocksProps.children
				: el(
						Disabled,
						null,
						el( ServerSideRender, {
							block: 'viglet-dumont/search',
							attributes,
							LoadingResponsePlaceholder: () =>
								el( Placeholder, {
									icon: 'search',
									label: __(
										'Turing search',
										'viglet-dumont'
									),
								} ),
						} )
					)
		);
	}

	wp.blocks.registerBlockType( 'viglet-dumont/search', {
		edit: Edit,
		// The parts, when arranged; a block without them saves nothing, as before.
		save: () => el( InnerBlocks.Content ),
	} );

	// Each part in the editor: a labelled box to move. It shows Turing's answer only on the
	// page, where the whole block's store runs.
	parts.forEach( ( [ name, icon ] ) =>
		wp.blocks.registerBlockType( name, {
			edit: function PartEdit() {
				return el(
					'div',
					useBlockProps(),
					el( Placeholder, {
						icon,
						label: wp.blocks.getBlockType( name )?.title || name,
						instructions:
							wp.blocks.getBlockType( name )?.description,
					} )
				);
			},
			save: () => null,
		} )
	);
} )( window.wp );
