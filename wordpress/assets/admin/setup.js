/**
 * The Setup page: connect to Turing in three steps. Enter the connection, test it (each check
 * names what failed), create the SN site if it is missing, then run the first index with live
 * progress.
 *
 * Reads and writes `viglet-dumont/v1/connection`, tests with `/diagnose`, creates the site
 * with `/provision` and indexes through `/index-batch`.
 */
import apiFetch from '@wordpress/api-fetch';
import {
	Button,
	Card,
	CardBody,
	CardHeader,
	Flex,
	Notice,
	ProgressBar,
	Spinner,
	TextControl,
} from '@wordpress/components';
import { createRoot, useEffect, useState } from '@wordpress/element';
import { __, sprintf } from '@wordpress/i18n';

const STEP_LABEL = {
	url: __( 'Address', 'viglet-dumont' ),
	reach: __( 'Network', 'viglet-dumont' ),
	turing: __( 'Turing', 'viglet-dumont' ),
	key: __( 'API key', 'viglet-dumont' ),
	site: __( 'SN site', 'viglet-dumont' ),
};

function Checks( { result } ) {
	return (
		<ul>
			{ result.steps.map( ( step ) => (
				<li key={ step.step }>
					<strong>
						{ step.ok ? '✓' : '✗' }{ ' ' }
						{ STEP_LABEL[ step.step ] || step.step }:
					</strong>{ ' ' }
					{ step.message }
				</li>
			) ) }
		</ul>
	);
}

function Setup() {
	const [ form, setForm ] = useState( null );
	const [ apiKey, setApiKey ] = useState( '' );
	const [ busy, setBusy ] = useState( '' );
	const [ result, setResult ] = useState( null );
	const [ error, setError ] = useState( '' );
	const [ progress, setProgress ] = useState( null );

	useEffect( () => {
		apiFetch( { path: '/viglet-dumont/v1/connection' } )
			.then( setForm )
			.catch( ( e ) => setError( e.message || String( e ) ) );
	}, [] );

	const fail = ( e ) => setError( e.message || String( e ) );

	const test = () =>
		apiFetch( { path: '/viglet-dumont/v1/diagnose' } ).then( setResult );

	const saveAndTest = () => {
		setBusy( 'test' );
		setError( '' );
		setResult( null );
		apiFetch( {
			path: '/viglet-dumont/v1/connection',
			method: 'POST',
			data: { ...form, api_key: apiKey },
		} )
			.then( ( saved ) => {
				setForm( saved );
				setApiKey( '' );
				return test();
			} )
			.catch( fail )
			.finally( () => setBusy( '' ) );
	};

	const createSite = () => {
		setBusy( 'site' );
		apiFetch( { path: '/viglet-dumont/v1/provision', method: 'POST' } )
			.then( test )
			.catch( fail )
			.finally( () => setBusy( '' ) );
	};

	const indexAll = () => {
		setBusy( 'index' );
		setProgress( {
			percent: 0,
			message: __( 'Starting…', 'viglet-dumont' ),
		} );
		const batch = ( offset ) =>
			apiFetch( {
				path: '/viglet-dumont/v1/index-batch',
				method: 'POST',
				data: { offset, post_type: 'all' },
			} ).then( ( r ) => {
				setProgress( { percent: r.percent, message: r.message } );
				return r.done ? r : batch( r.offset );
			} );
		batch( 0 )
			.catch( fail )
			.finally( () => setBusy( '' ) );
	};

	if ( ! form ) {
		return error ? (
			<Notice status="error" isDismissible={ false }>
				{ error }
			</Notice>
		) : (
			<Spinner />
		);
	}

	const set = ( key ) => ( value ) => setForm( { ...form, [ key ]: value } );

	return (
		<Flex direction="column" gap={ 4 } style={ { maxWidth: 760 } }>
			{ error && (
				<Notice status="error" onRemove={ () => setError( '' ) }>
					{ error }
				</Notice>
			) }

			<Card>
				<CardHeader>
					<h2>{ __( '1. Connection', 'viglet-dumont' ) }</h2>
				</CardHeader>
				<CardBody>
					<TextControl
						__nextHasNoMarginBottom
						label={ __( 'Turing URL', 'viglet-dumont' ) }
						help={ __(
							'The address of your Turing, e.g. https://search.example.org.',
							'viglet-dumont'
						) }
						type="url"
						value={ form.server_url }
						onChange={ set( 'server_url' ) }
					/>
					<TextControl
						__nextHasNoMarginBottom
						label={ __( 'API key', 'viglet-dumont' ) }
						help={
							form.has_api_key
								? __(
										'A key is stored. Leave this empty to keep it.',
										'viglet-dumont'
									)
								: __(
										'Create one in Turing under API Tokens.',
										'viglet-dumont'
									)
						}
						type="password"
						autoComplete="new-password"
						value={ apiKey }
						onChange={ setApiKey }
					/>
					<TextControl
						__nextHasNoMarginBottom
						label={ __( 'SN site', 'viglet-dumont' ) }
						help={ __(
							'The Semantic Navigation site this WordPress publishes to and searches.',
							'viglet-dumont'
						) }
						value={ form.site_name }
						onChange={ set( 'site_name' ) }
					/>
					<TextControl
						__nextHasNoMarginBottom
						label={ __( 'Locale', 'viglet-dumont' ) }
						help={ __(
							'The language the content is indexed in, e.g. pt_BR or en_US.',
							'viglet-dumont'
						) }
						value={ form.locale }
						onChange={ set( 'locale' ) }
					/>
					<TextControl
						__nextHasNoMarginBottom
						label={ __( 'Site key', 'viglet-dumont' ) }
						help={ sprintf(
							/* translators: %s: the key derived from this site's address. */
							__(
								"Keeps this site's documents apart when several sites share one SN site. Empty uses %s.",
								'viglet-dumont'
							),
							form.derived_key
						) }
						value={ form.site_key }
						onChange={ set( 'site_key' ) }
					/>
					<Button
						variant="primary"
						isBusy={ busy === 'test' }
						disabled={ !! busy }
						onClick={ saveAndTest }
					>
						{ __(
							'Save and test the connection',
							'viglet-dumont'
						) }
					</Button>
				</CardBody>
			</Card>

			{ result && (
				<Card>
					<CardHeader>
						<h2>{ __( '2. Test', 'viglet-dumont' ) }</h2>
					</CardHeader>
					<CardBody>
						<Checks result={ result } />
						{ result.site_exists === false && (
							<Button
								variant="secondary"
								isBusy={ busy === 'site' }
								disabled={ !! busy }
								onClick={ createSite }
							>
								{ sprintf(
									/* translators: %s: the SN site's name. */
									__(
										'Create the SN site %s',
										'viglet-dumont'
									),
									form.site_name
								) }
							</Button>
						) }
					</CardBody>
				</Card>
			) }

			{ result && result.ok && (
				<Card>
					<CardHeader>
						<h2>{ __( '3. First index', 'viglet-dumont' ) }</h2>
					</CardHeader>
					<CardBody>
						<p>
							{ __(
								'Sends every published post of the indexed types now. New and edited posts are sent on their own after that.',
								'viglet-dumont'
							) }
						</p>
						<Button
							variant="primary"
							isBusy={ busy === 'index' }
							disabled={ !! busy }
							onClick={ indexAll }
						>
							{ __( 'Index everything now', 'viglet-dumont' ) }
						</Button>
						{ progress && (
							<div style={ { marginTop: 12 } } aria-live="polite">
								<ProgressBar value={ progress.percent } />
								<p>{ progress.message }</p>
							</div>
						) }
					</CardBody>
				</Card>
			) }
		</Flex>
	);
}

const mount = document.getElementById( 'viglet-dumont-setup' );
if ( mount ) {
	createRoot( mount ).render( <Setup /> );
}
