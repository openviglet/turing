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
							help={
								'DATE' === field.type
									? __(
											'Upcoming, last 7 and 30 days, this year, before.',
											'viglet-turing-search'
										)
									: undefined
							}
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
		</Flex>
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
	const [ error, setError ] = useState( '' );
	const [ notice, setNotice ] = useState( null );
	const [ saving, setSaving ] = useState( false );

	const load = useCallback( () => {
		apiFetch( { path: '/viglet-dumont/v1/mapping' } )
			.then( ( result ) => {
				setData( result );
				setMapping( result.mapping );
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
			data: { mapping },
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
