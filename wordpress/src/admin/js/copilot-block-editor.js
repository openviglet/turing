/**
 * The Ask this site block in the editor: its heading, placeholder, whether it offers a plan
 * (D201), and what the plan stages, cites and reads availability from (D323). The conversation
 * runs only on the site (render.php and public/copilot.js), so the editor shows a placeholder
 * and there is nothing to save.
 *
 * It speaks as the site does: on a WooCommerce shop the defaults name the catalog and the
 * kit, which is on unless switched off; anywhere else they name this site and a plan, which is
 * off unless switched on.
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
	const shop = !! ( window.vigletDumontCopilot || {} ).shop;

	/**
	 * The block in the editor.
	 *
	 * @param {Object}                       props               The block's props.
	 * @param {Object}                       props.attributes    heading, placeholder, kit and the plan's types.
	 * @param {(attributes: Object) => void} props.setAttributes Updates them.
	 */
	function Edit( { attributes, setAttributes } ) {
		const title = shop
			? __( 'Ask the catalog', 'viglet-turing-search' )
			: __( 'Ask this site', 'viglet-turing-search' );
		const kit = undefined === attributes.kit ? shop : attributes.kit;
		return el(
			'div',
			useBlockProps(),
			el(
				InspectorControls,
				null,
				el(
					PanelBody,
					{ title },
					el( TextControl, {
						label: __( 'Heading', 'viglet-turing-search' ),
						placeholder: title,
						value: attributes.heading,
						onChange: ( heading ) => setAttributes( { heading } ),
					} ),
					el( TextControl, {
						label: __( 'Placeholder', 'viglet-turing-search' ),
						placeholder: shop
							? __(
									'A waterproof jacket under 200',
									'viglet-turing-search'
								)
							: __(
									'Which courses start next month?',
									'viglet-turing-search'
								),
						value: attributes.placeholder,
						onChange: ( placeholder ) =>
							setAttributes( { placeholder } ),
					} ),
					el( ToggleControl, {
						label: shop
							? __( 'Offer Build my kit', 'viglet-turing-search' )
							: __( 'Offer a plan', 'viglet-turing-search' ),
						help: shop
							? __(
									'A visitor describes a plan and gets the products it needs, grouped by need, to put in the cart together.',
									'viglet-turing-search'
								)
							: __(
									'A visitor describes a plan and gets the items it needs from this site, grouped by need.',
									'viglet-turing-search'
								),
						checked: kit,
						onChange: ( value ) => setAttributes( { kit: value } ),
					} )
				),
				kit &&
					el(
						PanelBody,
						{
							title: __(
								'What the plan uses',
								'viglet-turing-search'
							),
						},
						el( TextControl, {
							label: __(
								'Types it proposes',
								'viglet-turing-search'
							),
							help: __(
								'Indexed types, comma-separated, e.g. course, event. Blank: products on a shop, any type elsewhere.',
								'viglet-turing-search'
							),
							value: attributes.stageTypes,
							onChange: ( stageTypes ) =>
								setAttributes( { stageTypes } ),
						} ),
						el( TextControl, {
							label: __(
								'Types it cites as guides',
								'viglet-turing-search'
							),
							help: __(
								'Indexed types, comma-separated, e.g. page. Blank: posts on a shop, none elsewhere.',
								'viglet-turing-search'
							),
							value: attributes.guideTypes,
							onChange: ( guideTypes ) =>
								setAttributes( { guideTypes } ),
						} ),
						el( TextControl, {
							label: __(
								'Availability field',
								'viglet-turing-search'
							),
							help: __(
								'The indexed field that says whether an item can be taken, e.g. seats_left. Blank: stock_status on a shop, none elsewhere.',
								'viglet-turing-search'
							),
							value: attributes.availabilityField,
							onChange: ( availabilityField ) =>
								setAttributes( { availabilityField } ),
						} )
					)
			),
			el( Placeholder, {
				icon: 'format-chat',
				label: attributes.heading || title,
				instructions: __(
					'Visitors ask in plain words and Turing’s copilot answers from this site, citing its items. It shows only when Turing has a language model for it.',
					'viglet-turing-search'
				),
			} )
		);
	}

	wp.blocks.registerBlockType( 'viglet-dumont/copilot', {
		edit: Edit,
		save: () => null,
	} );
} )( window.wp );
