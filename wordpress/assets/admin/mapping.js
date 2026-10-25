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
	provisioned: __( 'In Turing', 'viglet-dumont' ),
	pending: __( 'Added on the next save', 'viglet-dumont' ),
	conflict: __( 'Conflict', 'viglet-dumont' ),
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
		weight: null,
	};
}

function FieldRow( { field, sources, types, status, onChange, onRemove } ) {
	const set = ( key ) => ( value ) =>
		onChange( { ...field, [ key ]: value } );
	const state = status ? status[ field.field ] : null;
	const known = sources.some( ( s ) => s.source === field.source );
	const options = (
		known ? [] : [ { source: field.source, label: field.source } ]
	)
		.concat( sources )
		.map( ( s ) => ( { value: s.source, label: s.label } ) );

	return (
		<tr>
			<td>
				<SelectControl
					__nextHasNoMarginBottom
					hideLabelFromVision
					label={ __( 'Source', 'viglet-dumont' ) }
					value={ field.source }
					options={ options }
					onChange={ set( 'source' ) }
				/>
			</td>
			<td>
				<TextControl
					__nextHasNoMarginBottom
					hideLabelFromVision
					label={ __( 'Turing field', 'viglet-dumont' ) }
					value={ field.field }
					onChange={ set( 'field' ) }
				/>
			</td>
			<td>
				<SelectControl
					__nextHasNoMarginBottom
					hideLabelFromVision
					label={ __( 'Type', 'viglet-dumont' ) }
					value={ field.type }
					options={ types.map( ( t ) => ( { value: t, label: t } ) ) }
					onChange={ set( 'type' ) }
				/>
			</td>
			<td>
				<CheckboxControl
					__nextHasNoMarginBottom
					label={ __( 'Multiple', 'viglet-dumont' ) }
					checked={ !! field.multi }
					onChange={ set( 'multi' ) }
				/>
			</td>
			<td>
				<CheckboxControl
					__nextHasNoMarginBottom
					label={ __( 'Facet', 'viglet-dumont' ) }
					checked={ !! field.facet }
					onChange={ set( 'facet' ) }
				/>
				{ field.facet && (
					<TextControl
						__nextHasNoMarginBottom
						label={ __( 'Facet label', 'viglet-dumont' ) }
						value={ field.facet_label }
						onChange={ set( 'facet_label' ) }
					/>
				) }
			</td>
			<td>
				<TextControl
					__nextHasNoMarginBottom
					hideLabelFromVision
					label={ __( 'Search weight', 'viglet-dumont' ) }
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
					label={ __( 'Remove field', 'viglet-dumont' ) }
				>
					{ __( 'Remove', 'viglet-dumont' ) }
				</Button>
			</td>
		</tr>
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
						'viglet-dumont'
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
							'viglet-dumont'
						) }
					</p>
				) : (
					<table className="widefat striped">
						<thead>
							<tr>
								<th scope="col">
									{ __( 'Source', 'viglet-dumont' ) }
								</th>
								<th scope="col">
									{ __( 'Turing field', 'viglet-dumont' ) }
								</th>
								<th scope="col">
									{ __( 'Type', 'viglet-dumont' ) }
								</th>
								<th scope="col">
									{ __( 'Multiple', 'viglet-dumont' ) }
								</th>
								<th scope="col">
									{ __( 'Facet', 'viglet-dumont' ) }
								</th>
								<th scope="col">
									{ __( 'Weight', 'viglet-dumont' ) }
								</th>
								<th scope="col">
									{ __( 'In Turing', 'viglet-dumont' ) }
								</th>
								<th scope="col">
									<span className="screen-reader-text">
										{ __( 'Actions', 'viglet-dumont' ) }
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
				<Flex justify="flex-start" style={ { marginTop: 12 } }>
					<Button
						variant="secondary"
						onClick={ () =>
							setFields( [ ...fields, emptyField( sources ) ] )
						}
					>
						{ __( 'Add field', 'viglet-dumont' ) }
					</Button>
					<Button variant="tertiary" onClick={ loadPreview }>
						{ __( 'Preview document', 'viglet-dumont' ) }
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
									'viglet-dumont'
								),
								preview.post.title
							) }
						</p>
						<pre
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
					'viglet-dumont'
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
							'viglet-dumont'
						),
						data.status_error
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
					{ __( 'Save and send to Turing', 'viglet-dumont' ) }
				</Button>
			</Flex>
		</Flex>
	);
}

const mount = document.getElementById( 'viglet-dumont-mapping' );
if ( mount ) {
	createRoot( mount ).render( <Mapping /> );
}
