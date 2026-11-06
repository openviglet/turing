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
		FormTokenField,
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
						title: __( 'Layout', 'viglet-turing-search' ),
						initialOpen: false,
					},
					el(
						'p',
						null,
						arranged
							? __(
									'Its parts are blocks: move them, or put other blocks between them.',
									'viglet-turing-search'
								)
							: __(
									'Make its parts (box, filters, results, pages) blocks you can reorder.',
									'viglet-turing-search'
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
							? __(
									'Back to the default order',
									'viglet-turing-search'
								)
							: __( 'Arrange its parts', 'viglet-turing-search' )
					)
				),
				el(
					PanelBody,
					{ title: __( 'Search', 'viglet-turing-search' ) },
					el( TextControl, {
						label: __( 'Placeholder', 'viglet-turing-search' ),
						value: attributes.placeholder,
						onChange: ( placeholder ) =>
							setAttributes( { placeholder } ),
					} ),
					// D306: a box scoped to one section of the site, not a filter to clear.
					el( FormTokenField, {
						label: __( 'Always filter by', 'viglet-turing-search' ),
						value: attributes.filters || [],
						onChange: ( tokens ) =>
							setAttributes( {
								filters: tokens
									.map( ( t ) =>
										String( t.value ?? t ).trim()
									)
									.filter( ( t ) => /^\w+:.+/.test( t ) ),
							} ),
						__next40pxDefaultSize: true,
						__nextHasNoMarginBottom: true,
					} ),
					el(
						'p',
						{ className: 'components-base-control__help' },
						__(
							'Each as field:value (type:event, or a mapped facet). Every search this block makes stays inside them, and visitors cannot remove them.',
							'viglet-turing-search'
						)
					),
					el( ToggleControl, {
						label: __(
							'Show filters (facets)',
							'viglet-turing-search'
						),
						checked: attributes.showFacets,
						onChange: ( showFacets ) =>
							setAttributes( { showFacets } ),
					} ),
					el( ToggleControl, {
						label: __(
							'Let visitors sort (relevance, newest, oldest)',
							'viglet-turing-search'
						),
						checked: attributes.showSort,
						onChange: ( showSort ) => setAttributes( { showSort } ),
					} ),
					el( ToggleControl, {
						label: __(
							'Answer questions from this site’s content',
							'viglet-turing-search'
						),
						help: __(
							'A search that reads as a question gets Turing’s answer, with its sources, above the results. Each one is an LLM call; it shows only when Turing cites sources.',
							'viglet-turing-search'
						),
						checked: attributes.showAnswer,
						onChange: ( showAnswer ) =>
							setAttributes( { showAnswer } ),
					} ),
					el( ToggleControl, {
						label: __(
							'Suggest the filters a sentence names',
							'viglet-turing-search'
						),
						help: __(
							'“Posts about php from last year” offers Type and Published filters to apply with one click. Each sentence is an LLM call; nothing is applied without a click.',
							'viglet-turing-search'
						),
						checked: attributes.suggestFilters,
						onChange: ( suggestFilters ) =>
							setAttributes( { suggestFilters } ),
					} ),
					attributes.suggestFilters &&
						el( ToggleControl, {
							label: __(
								'Apply them instead of suggesting',
								'viglet-turing-search'
							),
							help: __(
								'“Rain jacket under $400, blue” filters by colour and price at once, and a line says what was understood, with Undo. A price is read even without a language model.',
								'viglet-turing-search'
							),
							checked: attributes.applyFilters,
							onChange: ( applyFilters ) =>
								setAttributes( { applyFilters } ),
						} ),
					el( ToggleControl, {
						label: __(
							'Show popular searches under the box',
							'viglet-turing-search'
						),
						help: __(
							'Up to eight of this week’s most searched words, each one checked to find something, before a visitor types.',
							'viglet-turing-search'
						),
						checked: attributes.showTrending,
						onChange: ( showTrending ) =>
							setAttributes( { showTrending } ),
					} ),
					el( SelectControl, {
						label: __(
							'Where the filters sit',
							'viglet-turing-search'
						),
						value: attributes.facetsPosition,
						options: [
							{
								label: __( 'Left', 'viglet-turing-search' ),
								value: 'left',
							},
							{
								label: __( 'Right', 'viglet-turing-search' ),
								value: 'right',
							},
							{
								label: __(
									'Above the results',
									'viglet-turing-search'
								),
								value: 'top',
							},
						],
						onChange: ( facetsPosition ) =>
							setAttributes( { facetsPosition } ),
					} ),
					el( SelectControl, {
						label: __( 'Results as', 'viglet-turing-search' ),
						value: attributes.resultsLayout,
						options: [
							{
								label: __( 'A list', 'viglet-turing-search' ),
								value: 'list',
							},
							{
								label: __( 'A grid', 'viglet-turing-search' ),
								value: 'grid',
							},
						],
						onChange: ( resultsLayout ) =>
							setAttributes( { resultsLayout } ),
					} ),
					...[
						[
							'showImage',
							__( 'Show the image', 'viglet-turing-search' ),
						],
						[
							'showType',
							__( 'Show the type', 'viglet-turing-search' ),
						],
						[
							'showDate',
							__( 'Show the date', 'viglet-turing-search' ),
						],
						[
							'showSnippet',
							__( 'Show the snippet', 'viglet-turing-search' ),
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
						label: __( 'Results per page', 'viglet-turing-search' ),
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
										'viglet-turing-search'
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
