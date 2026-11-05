/**
 * The Fields page: per post type, which taxonomies, meta keys and post fields become which
 * Turing field, of which type, and whether each is a facet. Shows where each field stands in
 * Turing and the document a real post produces.
 *
 * Reads and writes `viglet-dumont/v1/mapping`; saving also provisions the fields
 * (`/provision`), and "Preview document" reads `/mapping/preview`.
 */
import apiFetch from '@wordpress/api-fetch';
import {
	Button,
	Card,
	CardBody,
	CardHeader,
	CheckboxControl,
	Flex,
	Notice,
	SelectControl,
	Spinner,
	TextControl,
	ToggleControl,
} from '@wordpress/components';
import {
	createRoot,
	useCallback,
	useEffect,
	useState,
} from '@wordpress/element';
import { __, sprintf } from '@wordpress/i18n';

const STATUS_LABEL = {
	provisioned: __( 'In Turing', 'viglet-turing-search' ),
	pending: __( 'Added on the next save', 'viglet-turing-search' ),
	conflict: __( 'Conflict', 'viglet-turing-search' ),
};

function emptyField( sources ) {
	const source = sources.length ? sources[ 0 ].source : 'thumbnail';
	return {
		source,
		field: source
			.replace( /^[a-z]+:/, '' )
			.replace( /[^A-Za-z0-9_]/g, '_' ),
		type: 'STRING',
		multi: false,
		facet: false,
		facet_label: '',
		card: false,
		weight: null,
		range: false,
		ranges: [],
	};
}

// The types a range facet can bucket; a date gets a fixed set of periods.
const RANGE_TYPES = [ 'INT', 'LONG', 'FLOAT', 'DOUBLE', 'DATE' ];

function bound( value ) {
	return value === '' ? null : Number( value );
}

function RangeRows( { ranges, onChange } ) {
	const setRange = ( index, key, value ) =>
		onChange(
			ranges.map( ( r, i ) =>
				i === index ? { ...r, [ key ]: value } : r
			)
		);

	return (
		<fieldset>
			<legend>{ __( 'Ranges', 'viglet-turing-search' ) }</legend>
			<p className="description">
				{ __(
					'Leave a bound empty for an open end, and a label empty to name the range after its bounds.',
					'viglet-turing-search'
				) }
			</p>
			{ ranges.map( ( range, index ) => (
				<Flex key={ index } justify="flex-start" align="flex-end">
					<TextControl
						__nextHasNoMarginBottom
						label={ __( 'Label', 'viglet-turing-search' ) }
						value={ range.label || '' }
						onChange={ ( v ) => setRange( index, 'label', v ) }
					/>
					<TextControl
						__nextHasNoMarginBottom
						label={ __( 'From', 'viglet-turing-search' ) }
						type="number"
						value={ range.from ?? '' }
						onChange={ ( v ) =>
							setRange( index, 'from', bound( v ) )
						}
					/>
					<TextControl
						__nextHasNoMarginBottom
						label={ __( 'To', 'viglet-turing-search' ) }
						type="number"
						value={ range.to ?? '' }
						onChange={ ( v ) =>
							setRange( index, 'to', bound( v ) )
						}
					/>
					<Button
						variant="tertiary"
						isDestructive
						onClick={ () =>
							onChange( ranges.filter( ( r, i ) => i !== index ) )
						}
					>
						{ __( 'Remove range', 'viglet-turing-search' ) }
					</Button>
				</Flex>
			) ) }
			<Button
				variant="secondary"
				style={ { marginTop: 8 } }
				onClick={ () =>
					onChange( [
						...ranges,
						{ label: '', from: null, to: null },
					] )
				}
			>
				{ __( 'Add range', 'viglet-turing-search' ) }
			</Button>
		</fieldset>
	);
}

/**
 * A date's range facet periods (D307): the past (publication dates), an agenda's (dates to
 * come), or its own, each bound written in Turing's date math.
 *
 * @param {Object}                                             props       The field and its setter.
 * @param {Object}                                             props.field The mapped field.
 * @param {(key: string) => (v: string|Object[]|null) => void} props.set   Sets one of its keys.
 */
function DatePeriods( { field, set } ) {
	const periods = field.periods || 'past';
	const ranges = field.ranges || [];
	const setRange = ( index, key, value ) =>
		set( 'ranges' )(
			ranges.map( ( r, i ) =>
				i === index ? { ...r, [ key ]: value } : r
			)
		);
	return (
		<fieldset>
			<SelectControl
				__nextHasNoMarginBottom
				__next40pxDefaultSize
				label={ __( 'Periods', 'viglet-turing-search' ) }
				value={ periods }
				options={ [
					{
						label: __(
							'Past: last 7 and 30 days, this year, before (and upcoming)',
							'viglet-turing-search'
						),
						value: 'past',
					},
					{
						label: __(
							'Agenda: today, next 7 days, this month, upcoming, past',
							'viglet-turing-search'
						),
						value: 'agenda',
					},
					{
						label: __( 'My own', 'viglet-turing-search' ),
						value: 'custom',
					},
				] }
				onChange={ set( 'periods' ) }
			/>
			{ 'custom' === periods && (
				<>
					<p className="description">
						{ __(
							'Each bound is NOW or a yyyy-mm-dd day, rounded with /DAY, /MONTH or /YEAR and moved with +7DAYS, -1MONTH and so on. Leave one empty for an open end. Up to 8.',
							'viglet-turing-search'
						) }
					</p>
					{ ranges.map( ( range, index ) => (
						<Flex
							key={ index }
							justify="flex-start"
							align="flex-end"
						>
							<TextControl
								__nextHasNoMarginBottom
								label={ __( 'Label', 'viglet-turing-search' ) }
								value={ range.label || '' }
								onChange={ ( v ) =>
									setRange( index, 'label', v )
								}
							/>
							<TextControl
								__nextHasNoMarginBottom
								label={ __( 'From', 'viglet-turing-search' ) }
								value={ range.from ?? '' }
								placeholder="NOW/DAY"
								onChange={ ( v ) =>
									setRange( index, 'from', v || null )
								}
							/>
							<TextControl
								__nextHasNoMarginBottom
								label={ __( 'To', 'viglet-turing-search' ) }
								value={ range.to ?? '' }
								placeholder="NOW/DAY+7DAYS"
								onChange={ ( v ) =>
									setRange( index, 'to', v || null )
								}
							/>
							<Button
								variant="tertiary"
								isDestructive
								onClick={ () =>
									set( 'ranges' )(
										ranges.filter( ( r, i ) => i !== index )
									)
								}
							>
								{ __( 'Remove range', 'viglet-turing-search' ) }
							</Button>
						</Flex>
					) ) }
					{ ranges.length < 8 && (
						<Button
							variant="secondary"
							style={ { marginTop: 8 } }
							onClick={ () =>
								set( 'ranges' )( [
									...ranges,
									{ label: '', from: null, to: null },
								] )
							}
						>
							{ __( 'Add range', 'viglet-turing-search' ) }
						</Button>
					) }
				</>
			) }
		</fieldset>
	);
}

function FieldRow( { field, sources, types, status, onChange, onRemove } ) {
	const set = ( key ) => ( value ) =>
		onChange( { ...field, [ key ]: value } );
	const rangeable = RANGE_TYPES.includes( field.type );
	const state = status ? status[ field.field ] : null;
	const known = sources.some( ( s ) => s.source === field.source );
	const options = (
		known ? [] : [ { source: field.source, label: field.source } ]
	)
		.concat( sources )
		.map( ( s ) => ( { value: s.source, label: s.label } ) );

	return (
		<>
			<tr>
				<td>
					<SelectControl
						__nextHasNoMarginBottom
						hideLabelFromVision
						label={ __( 'Source', 'viglet-turing-search' ) }
						value={ field.source }
						options={ options }
						onChange={ set( 'source' ) }
					/>
				</td>
				<td>
					<TextControl
						__nextHasNoMarginBottom
						hideLabelFromVision
						label={ __( 'Turing field', 'viglet-turing-search' ) }
						value={ field.field }
						onChange={ set( 'field' ) }
					/>
				</td>
				<td>
					<SelectControl
						__nextHasNoMarginBottom
						hideLabelFromVision
						label={ __( 'Type', 'viglet-turing-search' ) }
						value={ field.type }
						options={ types.map( ( t ) => ( {
							value: t,
							label: t,
						} ) ) }
						onChange={ set( 'type' ) }
					/>
				</td>
				<td>
					<CheckboxControl
						__nextHasNoMarginBottom
						label={ __( 'Multiple', 'viglet-turing-search' ) }
						checked={ !! field.multi }
						onChange={ set( 'multi' ) }
					/>
					<CheckboxControl
						__nextHasNoMarginBottom
						label={ __(
							'Show on result cards',
							'viglet-turing-search'
						) }
						checked={ !! field.card }
						onChange={ set( 'card' ) }
					/>
				</td>
				<td>
					<CheckboxControl
						__nextHasNoMarginBottom
						label={ __( 'Facet', 'viglet-turing-search' ) }
						checked={ !! field.facet }
						onChange={ set( 'facet' ) }
					/>
					{ rangeable && (
						<CheckboxControl
							__nextHasNoMarginBottom
							label={ __( 'Ranges', 'viglet-turing-search' ) }
							checked={ !! field.range }
							onChange={ set( 'range' ) }
						/>
					) }
					{ ( field.facet ||
						field.card ||
						( rangeable && field.range ) ) && (
						<TextControl
							__nextHasNoMarginBottom
							label={
								field.facet || ( rangeable && field.range )
									? __(
											'Facet label',
											'viglet-turing-search'
										)
									: __( 'Label', 'viglet-turing-search' )
							}
							value={ field.facet_label }
							onChange={ set( 'facet_label' ) }
						/>
					) }
				</td>
				<td>
					<TextControl
						__nextHasNoMarginBottom
						hideLabelFromVision
						label={ __( 'Search weight', 'viglet-turing-search' ) }
						type="number"
						min={ 1 }
						max={ 100 }
						value={ field.weight ?? '' }
						onChange={ ( v ) =>
							set( 'weight' )( v === '' ? null : Number( v ) )
						}
					/>
				</td>
				<td>
					{ state ? (
						<span title={ state.detail }>
							{ STATUS_LABEL[ state.status ] || state.status }
							{ state.detail && (
								<>
									<br />
									<small>{ state.detail }</small>
								</>
							) }
						</span>
					) : (
						'—'
					) }
				</td>
				<td>
					<Button
						variant="tertiary"
						isDestructive
						onClick={ onRemove }
						label={ __( 'Remove field', 'viglet-turing-search' ) }
					>
						{ __( 'Remove', 'viglet-turing-search' ) }
					</Button>
				</td>
			</tr>
			{ rangeable && field.range && 'DATE' !== field.type && (
				<tr>
					<td colSpan={ 8 }>
						<RangeRows
							ranges={ field.ranges || [] }
							onChange={ set( 'ranges' ) }
						/>
					</td>
				</tr>
			) }
			{ field.range && 'DATE' === field.type && (
				<tr>
					<td colSpan={ 8 }>
						<DatePeriods field={ field } set={ set } />
					</td>
				</tr>
			) }
		</>
	);
}

/**
 * What this type's result cards show in place of the defaults (D305): a mapped DATE field
 * for the date (an event's start, not the day it was posted) and a mapped field for the image.
 *
 * @param {Object}                 props          The type's mapping, its fields and the change handler.
 * @param {Object}                 props.value    The type's mapping.
 * @param {Object[]}               props.fields   Its fields.
 * @param {(next: Object) => void} props.onChange Receives the type's mapping changed.
 */
function CardAppearance( { value, fields, onChange } ) {
	const named = ( list ) =>
		list.map( ( f ) => ( {
			label: f.facet_label || f.field,
			value: f.field,
		} ) );
	return (
		<Flex justify="flex-start" align="flex-end" style={ { marginTop: 12 } }>
			<SelectControl
				__nextHasNoMarginBottom
				__next40pxDefaultSize
				label={ __( 'Date shown on cards', 'viglet-turing-search' ) }
				value={ value.card_date || '' }
				options={ [
					{
						label: __( 'Publication date', 'viglet-turing-search' ),
						value: '',
					},
					...named( fields.filter( ( f ) => 'DATE' === f.type ) ),
				] }
				onChange={ ( cardDate ) =>
					onChange( { ...value, card_date: cardDate } )
				}
			/>
			<SelectControl
				__nextHasNoMarginBottom
				__next40pxDefaultSize
				label={ __( 'Image on cards', 'viglet-turing-search' ) }
				value={ value.card_image || '' }
				options={ [
					{
						label: __( 'Featured image', 'viglet-turing-search' ),
						value: '',
					},
					...named(
						fields.filter( ( f ) =>
							[ 'STRING', 'TEXT' ].includes( f.type )
						)
					),
				] }
				onChange={ ( cardImage ) =>
					onChange( { ...value, card_image: cardImage } )
				}
			/>
			<SelectControl
				__nextHasNoMarginBottom
				__next40pxDefaultSize
				label={ __( 'Leaves search on', 'viglet-turing-search' ) }
				help={ __(
					'A post leaves search once this date has passed, and comes back if it moves forward.',
					'viglet-turing-search'
				) }
				value={ value.expires || '' }
				options={ [
					{
						label: __( 'Never', 'viglet-turing-search' ),
						value: '',
					},
					...named( fields.filter( ( f ) => 'DATE' === f.type ) ),
				] }
				onChange={ ( expires ) => onChange( { ...value, expires } ) }
			/>
		</Flex>
	);
}

/**
 * The types other sources sent under this site key (D327), a catalog's "Article" or "Thesis":
 * no post type has them, so they are shown, not indexed, and here they get their badge text
 * and the fields their cards show as the date and the image.
 *
 * @param {Object}                 props          The types seen and how each shows.
 * @param {Object[]}               props.seen     Each type Turing holds: type, count, dates, texts.
 * @param {Object}                 props.value    How each type shows, keyed by type.
 * @param {(next: Object) => void} props.onChange Receives the map changed.
 */
function ForeignTypes( { seen, value, onChange } ) {
	// A type configured before but not seen this time keeps its row, so it can be cleared.
	const names = [
		...new Set( [
			...seen.map( ( s ) => s.type ),
			...Object.keys( value ),
		] ),
	];
	if ( ! names.length ) {
		return null;
	}
	const byType = Object.fromEntries( seen.map( ( s ) => [ s.type, s ] ) );
	const set = ( type, key ) => ( v ) =>
		onChange( {
			...value,
			[ type ]: {
				label: '',
				card_date: '',
				card_image: '',
				...value[ type ],
				[ key ]: v,
			},
		} );
	const options = ( list, current, none ) => [
		{ label: none, value: '' },
		...[ ...new Set( [ ...list, current ].filter( Boolean ) ) ].map(
			( f ) => ( { label: f, value: f } )
		),
	];

	return (
		<Card>
			<CardHeader>
				<h2>
					{ __( 'Types from other sources', 'viglet-turing-search' ) }
				</h2>
			</CardHeader>
			<CardBody>
				<p>
					{ __(
						'Documents another source sent under this site key, such as a catalog, carry types no post type has. Name each one as its result badge reads, and choose the fields its cards show as the date and the image. This changes how they show, never what is indexed.',
						'viglet-turing-search'
					) }
				</p>
				<table className="widefat striped viglet-dumont-foreign-types">
					<thead>
						<tr>
							<th scope="col">
								{ __( 'Type', 'viglet-turing-search' ) }
							</th>
							<th scope="col">
								{ __( 'Label', 'viglet-turing-search' ) }
							</th>
							<th scope="col">
								{ __(
									'Date shown on cards',
									'viglet-turing-search'
								) }
							</th>
							<th scope="col">
								{ __(
									'Image on cards',
									'viglet-turing-search'
								) }
							</th>
						</tr>
					</thead>
					<tbody>
						{ names.map( ( type ) => {
							const looks = value[ type ] || {};
							const found = byType[ type ];
							return (
								<tr key={ type } data-type={ type }>
									<th scope="row">
										{ type }
										{ found && (
											<>
												<br />
												<small>
													{ sprintf(
														/* translators: %d: how many documents of the type were read. */
														__(
															'Documents read: %d',
															'viglet-turing-search'
														),
														found.count
													) }
												</small>
											</>
										) }
									</th>
									<td>
										<TextControl
											__nextHasNoMarginBottom
											hideLabelFromVision
											label={ sprintf(
												/* translators: %s: a document type, e.g. Article. */
												__(
													'Label for %s',
													'viglet-turing-search'
												),
												type
											) }
											placeholder={ type }
											value={ looks.label || '' }
											onChange={ set( type, 'label' ) }
										/>
									</td>
									<td>
										<SelectControl
											__nextHasNoMarginBottom
											hideLabelFromVision
											label={ sprintf(
												/* translators: %s: a document type, e.g. Article. */
												__(
													'Date shown on %s cards',
													'viglet-turing-search'
												),
												type
											) }
											value={ looks.card_date || '' }
											options={ options(
												found?.dates || [],
												looks.card_date,
												__(
													'Publication date',
													'viglet-turing-search'
												)
											) }
											onChange={ set(
												type,
												'card_date'
											) }
										/>
									</td>
									<td>
										<SelectControl
											__nextHasNoMarginBottom
											hideLabelFromVision
											label={ sprintf(
												/* translators: %s: a document type, e.g. Article. */
												__(
													'Image on %s cards',
													'viglet-turing-search'
												),
												type
											) }
											value={ looks.card_image || '' }
											options={ options(
												found?.texts || [],
												looks.card_image,
												__(
													'Its image field',
													'viglet-turing-search'
												)
											) }
											onChange={ set(
												type,
												'card_image'
											) }
										/>
									</td>
								</tr>
							);
						} ) }
					</tbody>
				</table>
			</CardBody>
		</Card>
	);
}

// The entity facets Turing fills (D310), in the order the screen lists them.
const ENTITY_KINDS = [
	{
		kind: 'person',
		field: 'entity_person',
		label: __( 'People', 'viglet-turing-search' ),
	},
	{
		kind: 'organization',
		field: 'entity_organization',
		label: __( 'Organisations', 'viglet-turing-search' ),
	},
	{
		kind: 'place',
		field: 'entity_place',
		label: __( 'Places', 'viglet-turing-search' ),
	},
];

/**
 * The people, organisations and places Turing's language model finds in the posts (D310): no
 * post type maps them, Turing fills them as it indexes, and here each gets its facet label
 * and whether the search block offers it.
 *
 * @param {Object}                 props            The facets, the settings they depend on and the change handler.
 * @param {Object}                 props.value      Each kind's `{label, offered}`.
 * @param {Object}                 props.extraction Whether Turing is asked to find them, and why not.
 * @param {Object}                 props.status     Where each field stands in Turing.
 * @param {(next: Object) => void} props.onChange   Receives the facets changed.
 */
function EntityFacets( { value, extraction, status, onChange } ) {
	const set = ( kind, key ) => ( v ) =>
		onChange( {
			...value,
			[ kind ]: {
				label: '',
				offered: true,
				...value[ kind ],
				[ key ]: v,
			},
		} );
	let off = '';
	if ( ! extraction.asked ) {
		off = __(
			'Off: turn on "Find people, organisations and places in posts" under Settings › Generative AI. Until then nothing is found and these facets are not offered.',
			'viglet-turing-search'
		);
	} else if ( ! extraction.generative_ai ) {
		off = __(
			'Off: finding names needs generative AI, which is off under Settings › Generative AI. Until it is on, nothing is found and these facets are not offered.',
			'viglet-turing-search'
		);
	}

	return (
		<Card>
			<CardHeader>
				<h2>
					{ __(
						'People, organisations and places',
						'viglet-turing-search'
					) }
				</h2>
			</CardHeader>
			<CardBody>
				<p>
					{ __(
						"Turing's language model finds the people, organisations and places each post names as it is indexed, and keeps only the names the text holds. Label each facet as the search block shows it, and choose which ones it offers.",
						'viglet-turing-search'
					) }
				</p>
				{ off && (
					<Notice status="info" isDismissible={ false }>
						{ off }{ ' ' }
						<a href={ extraction.settings_url }>
							{ __( 'Open Settings', 'viglet-turing-search' ) }
						</a>
					</Notice>
				) }
				<table className="widefat striped viglet-dumont-entity-facets">
					<thead>
						<tr>
							<th scope="col">
								{ __( 'Turing field', 'viglet-turing-search' ) }
							</th>
							<th scope="col">
								{ __( 'Facet label', 'viglet-turing-search' ) }
							</th>
							<th scope="col">
								{ __( 'Offered', 'viglet-turing-search' ) }
							</th>
							<th scope="col">
								{ __( 'In Turing', 'viglet-turing-search' ) }
							</th>
						</tr>
					</thead>
					<tbody>
						{ ENTITY_KINDS.map( ( { kind, field, label } ) => {
							const facet = value[ kind ] || {};
							const state = status ? status[ field ] : null;
							return (
								<tr key={ kind } data-kind={ kind }>
									<th scope="row">
										<code>{ field }</code>
									</th>
									<td>
										<TextControl
											__nextHasNoMarginBottom
											hideLabelFromVision
											label={ sprintf(
												/* translators: %s: the Turing field, e.g. entity_person. */
												__(
													'Facet label for %s',
													'viglet-turing-search'
												),
												field
											) }
											placeholder={ label }
											value={ facet.label || '' }
											onChange={ set( kind, 'label' ) }
										/>
									</td>
									<td>
										<CheckboxControl
											__nextHasNoMarginBottom
											label={ sprintf(
												/* translators: %s: a facet's label, e.g. People. */
												__(
													'Offer %s',
													'viglet-turing-search'
												),
												facet.label || label
											) }
											checked={ false !== facet.offered }
											onChange={ set( kind, 'offered' ) }
										/>
									</td>
									<td>
										{ state
											? STATUS_LABEL[ state.status ] ||
												state.status
											: '—' }
									</td>
								</tr>
							);
						} ) }
					</tbody>
				</table>
			</CardBody>
		</Card>
	);
}

function TypeCard( { postType, value, sources, types, status, onChange } ) {
	const [ preview, setPreview ] = useState( null );
	const [ previewError, setPreviewError ] = useState( '' );

	const fields = value.fields;
	const setFields = ( next ) => onChange( { ...value, fields: next } );

	const loadPreview = () => {
		setPreviewError( '' );
		apiFetch( {
			path: `/viglet-dumont/v1/mapping/preview?post_type=${ encodeURIComponent( postType ) }`,
		} )
			.then( setPreview )
			.catch( ( e ) => setPreviewError( e.message || String( e ) ) );
	};

	return (
		<Card>
			<CardHeader>
				<h2>{ postType }</h2>
				<ToggleControl
					__nextHasNoMarginBottom
					label={ __(
						'Leave this type out of search',
						'viglet-turing-search'
					) }
					checked={ !! value.exclude }
					onChange={ ( exclude ) =>
						onChange( { ...value, exclude } )
					}
				/>
			</CardHeader>
			<CardBody>
				{ fields.length === 0 ? (
					<p>
						{ __(
							'Only the standard fields are sent for this type.',
							'viglet-turing-search'
						) }
					</p>
				) : (
					<table className="widefat striped">
						<thead>
							<tr>
								<th scope="col">
									{ __( 'Source', 'viglet-turing-search' ) }
								</th>
								<th scope="col">
									{ __(
										'Turing field',
										'viglet-turing-search'
									) }
								</th>
								<th scope="col">
									{ __( 'Type', 'viglet-turing-search' ) }
								</th>
								<th scope="col">
									{ __( 'Multiple', 'viglet-turing-search' ) }
								</th>
								<th scope="col">
									{ __( 'Facet', 'viglet-turing-search' ) }
								</th>
								<th scope="col">
									{ __( 'Weight', 'viglet-turing-search' ) }
								</th>
								<th scope="col">
									{ __(
										'In Turing',
										'viglet-turing-search'
									) }
								</th>
								<th scope="col">
									<span className="screen-reader-text">
										{ __(
											'Actions',
											'viglet-turing-search'
										) }
									</span>
								</th>
							</tr>
						</thead>
						<tbody>
							{ fields.map( ( field, index ) => (
								<FieldRow
									key={ index }
									field={ field }
									sources={ sources }
									types={ types }
									status={ status }
									onChange={ ( next ) =>
										setFields(
											fields.map( ( f, i ) =>
												i === index ? next : f
											)
										)
									}
									onRemove={ () =>
										setFields(
											fields.filter(
												( f, i ) => i !== index
											)
										)
									}
								/>
							) ) }
						</tbody>
					</table>
				) }
				{ fields.length > 0 && (
					<CardAppearance
						value={ value }
						fields={ fields }
						onChange={ onChange }
					/>
				) }
				<Flex justify="flex-start" style={ { marginTop: 12 } }>
					<Button
						variant="secondary"
						onClick={ () =>
							setFields( [ ...fields, emptyField( sources ) ] )
						}
					>
						{ __( 'Add field', 'viglet-turing-search' ) }
					</Button>
					<Button variant="tertiary" onClick={ loadPreview }>
						{ __( 'Preview document', 'viglet-turing-search' ) }
					</Button>
				</Flex>
				{ previewError && (
					<Notice status="warning" isDismissible={ false }>
						{ previewError }
					</Notice>
				) }
				{ preview && (
					<>
						<p>
							{ sprintf(
								/* translators: %s: the post's title. */
								__(
									'What "%s" sends, as saved (unsaved changes are not in it):',
									'viglet-turing-search'
								),
								preview.post.title
							) }
						</p>
						{ /* It scrolls, so the keyboard must be able to reach it. */ }
						<pre
							tabIndex={ 0 }
							style={ {
								maxHeight: 360,
								overflow: 'auto',
								background: '#f6f7f7',
								padding: 12,
							} }
						>
							{ JSON.stringify( preview.document, null, 2 ) }
						</pre>
					</>
				) }
			</CardBody>
		</Card>
	);
}

function Mapping() {
	const [ data, setData ] = useState( null );
	const [ mapping, setMapping ] = useState( null );
	const [ foreign, setForeign ] = useState( {} );
	const [ entities, setEntities ] = useState( {} );
	const [ error, setError ] = useState( '' );
	const [ notice, setNotice ] = useState( null );
	const [ saving, setSaving ] = useState( false );

	const load = useCallback( () => {
		apiFetch( { path: '/viglet-dumont/v1/mapping' } )
			.then( ( result ) => {
				setData( result );
				setMapping( result.mapping );
				setForeign( result.foreign || {} );
				setEntities( result.entities || {} );
			} )
			.catch( ( e ) => setError( e.message || String( e ) ) );
	}, [] );

	useEffect( load, [ load ] );

	const save = () => {
		setSaving( true );
		setNotice( null );
		apiFetch( {
			path: '/viglet-dumont/v1/mapping',
			method: 'POST',
			data: { mapping, foreign, entities },
		} )
			.then( () =>
				apiFetch( {
					path: '/viglet-dumont/v1/provision',
					method: 'POST',
				} ).then(
					( r ) =>
						setNotice( { status: 'success', text: r.message } ),
					( e ) =>
						setNotice( {
							status: 'warning',
							text: e.message || String( e ),
						} )
				)
			)
			.catch( ( e ) => setError( e.message || String( e ) ) )
			.finally( () => {
				setSaving( false );
				load();
			} );
	};

	if ( error && ! data ) {
		return (
			<Notice status="error" isDismissible={ false }>
				{ error }
			</Notice>
		);
	}
	if ( ! data || ! mapping ) {
		return <Spinner />;
	}

	return (
		<Flex direction="column" gap={ 4 } style={ { maxWidth: 1200 } }>
			<p>
				{ __(
					'Each post type always sends its title, text, URL, dates, author, categories and tags. Add the taxonomies and custom fields that should become their own fields, typed, and mark the ones to filter by as facets. Saving sends the fields to Turing; run a reindex afterwards so existing posts carry them.',
					'viglet-turing-search'
				) }
			</p>
			{ error && (
				<Notice status="error" onRemove={ () => setError( '' ) }>
					{ error }
				</Notice>
			) }
			{ data.status_error && (
				<Notice status="warning" isDismissible={ false }>
					{ sprintf(
						/* translators: %s: why Turing could not say where each field stands. */
						__(
							'Turing did not say where the fields stand: %s',
							'viglet-turing-search'
						),
						data.status_error
					) }
				</Notice>
			) }
			{ data.genai_change && (
				<Notice status="info" isDismissible={ false }>
					{ __(
						"Saving also turns generative AI on for this site in Turing, answering through its Default AI Agent until an agent is chosen in Turing's console.",
						'viglet-turing-search'
					) }
				</Notice>
			) }
			{ notice && (
				<Notice
					status={ notice.status }
					onRemove={ () => setNotice( null ) }
				>
					{ notice.text }
				</Notice>
			) }
			{ Object.keys( mapping ).map( ( postType ) => (
				<TypeCard
					key={ postType }
					postType={ postType }
					value={ mapping[ postType ] }
					sources={ data.sources[ postType ] || [] }
					types={ data.types }
					status={ data.field_status }
					onChange={ ( next ) =>
						setMapping( { ...mapping, [ postType ]: next } )
					}
				/>
			) ) }
			<ForeignTypes
				seen={ data.foreign_seen || [] }
				value={ foreign }
				onChange={ setForeign }
			/>
			{ data.extraction && (
				<EntityFacets
					value={ entities }
					extraction={ data.extraction }
					status={ data.field_status }
					onChange={ setEntities }
				/>
			) }
			<Flex justify="flex-start">
				<Button
					variant="primary"
					isBusy={ saving }
					disabled={ saving }
					onClick={ save }
				>
					{ __( 'Save and send to Turing', 'viglet-turing-search' ) }
				</Button>
			</Flex>
		</Flex>
	);
}

const mount = document.getElementById( 'viglet-dumont-mapping' );
if ( mount ) {
	createRoot( mount ).render( <Mapping /> );
}
