/**
 * The Setup page: connect to Turing in three steps. Enter the connection, test it (each check
 * names what failed), create the SN site if it is missing, then start the first index and follow
 * its progress.
 *
 * Reads and writes `viglet-turing/v1/connection`, tests with `/diagnose`, creates the site
 * with `/provision` and starts the background reindex with `/reindex` (D331).
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
	url: __( 'Address', 'viglet-turing-search' ),
	reach: __( 'Network', 'viglet-turing-search' ),
	turing: __( 'Viglet Turing', 'viglet-turing-search' ),
	key: __( 'API key', 'viglet-turing-search' ),
	site: __( 'SN site', 'viglet-turing-search' ),
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
		apiFetch( { path: '/viglet-turing/v1/connection' } )
			.then( setForm )
			.catch( ( e ) => setError( e.message || String( e ) ) );
	}, [] );

	const fail = ( e ) => setError( e.message || String( e ) );

	const test = () =>
		apiFetch( { path: '/viglet-turing/v1/diagnose' } ).then( setResult );

	const saveAndTest = () => {
		setBusy( 'test' );
		setError( '' );
		setResult( null );
		apiFetch( {
			path: '/viglet-turing/v1/connection',
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
		apiFetch( { path: '/viglet-turing/v1/provision', method: 'POST' } )
			.then( test )
			.catch( fail )
			.finally( () => setBusy( '' ) );
	};

	// The first index runs in the background (D331): this page only follows it, and can close.
	const indexAll = () => {
		setBusy( 'index' );
		setProgress( {
			percent: 0,
			message: __( 'Starting…', 'viglet-turing-search' ),
		} );
		let started = '';
		const follow = ( report ) => {
			const run = report.running;
			if ( run ) {
				started = run.id;
				setProgress( {
					percent: run.total
						? Math.round( ( run.offset / run.total ) * 100 )
						: 100,
					message: sprintf(
						/* translators: 1: posts done, 2: posts to do in all. */
						__(
							'Indexing in the background, so you can leave this page: %1$d of %2$d posts done.',
							'viglet-turing-search'
						),
						run.offset,
						run.total
					),
				} );
				return new Promise( ( resolve ) => setTimeout( resolve, 2000 ) )
					.then( () =>
						apiFetch( { path: '/viglet-turing/v1/reindex' } )
					)
					.then( follow );
			}
			const last = report.last;
			if ( last && last.id === started ) {
				setProgress( {
					percent: 100,
					message:
						'done' === last.state
							? sprintf(
									/* translators: %d: how many documents were indexed. */
									__(
										'Indexing complete. %d documents indexed.',
										'viglet-turing-search'
									),
									last.sent
								)
							: sprintf(
									/* translators: 1: posts sent, 2: posts that failed. */
									__(
										'The first index ended with failures: %1$d sent, %2$d failed. Search health shows the errors.',
										'viglet-turing-search'
									),
									last.sent,
									last.failed
								),
				} );
			}
			return report;
		};
		apiFetch( {
			path: '/viglet-turing/v1/reindex',
			method: 'POST',
			data: { type: 'all' },
		} )
			// One running already, started elsewhere: follow that one.
			.catch( ( e ) => {
				if ( e && e.running ) {
					return e;
				}
				throw e;
			} )
			.then( follow )
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
	// A value set by wp-config.php or the environment is read-only here.
	const locked = ( key ) =>
		form.sources &&
		form.sources[ key ] &&
		form.sources[ key ].source !== 'option';
	const lockedNames = Object.keys( form.sources || {} )
		.filter( locked )
		.map( ( key ) => form.sources[ key ].name );

	return (
		<Flex direction="column" gap={ 4 } style={ { maxWidth: 760 } }>
			{ error && (
				<Notice status="error" onRemove={ () => setError( '' ) }>
					{ error }
				</Notice>
			) }

			<Card>
				<CardHeader>
					<h2>{ __( '1. Connection', 'viglet-turing-search' ) }</h2>
				</CardHeader>
				<CardBody>
					{ lockedNames.length > 0 && (
						<Notice status="info" isDismissible={ false }>
							{ sprintf(
								/* translators: %s: the constants or environment variables, comma-separated. */
								__(
									'Set outside WordPress, read-only here: %s.',
									'viglet-turing-search'
								),
								lockedNames.join( ', ' )
							) }
						</Notice>
					) }
					<TextControl
						__nextHasNoMarginBottom
						label={ __(
							'Viglet Turing URL',
							'viglet-turing-search'
						) }
						help={ __(
							'The address of your Viglet Turing, e.g. https://search.example.org.',
							'viglet-turing-search'
						) }
						type="url"
						value={ form.server_url }
						disabled={ locked( 'server_url' ) }
						onChange={ set( 'server_url' ) }
					/>
					<TextControl
						__nextHasNoMarginBottom
						label={ __( 'API key', 'viglet-turing-search' ) }
						help={
							form.has_api_key
								? __(
										'A key is stored. Leave this empty to keep it.',
										'viglet-turing-search'
									)
								: __(
										'Create one in Viglet Turing under API Tokens.',
										'viglet-turing-search'
									)
						}
						type="password"
						autoComplete="new-password"
						value={ apiKey }
						disabled={ locked( 'api_key' ) }
						onChange={ setApiKey }
					/>
					<TextControl
						__nextHasNoMarginBottom
						label={ __( 'SN site', 'viglet-turing-search' ) }
						help={ __(
							'The Semantic Navigation site this WordPress publishes to and searches.',
							'viglet-turing-search'
						) }
						value={ form.site_name }
						disabled={ locked( 'site_name' ) }
						onChange={ set( 'site_name' ) }
					/>
					<TextControl
						__nextHasNoMarginBottom
						label={ __( 'Locale', 'viglet-turing-search' ) }
						help={ __(
							'The language the content is indexed in, e.g. pt_BR or en_US.',
							'viglet-turing-search'
						) }
						value={ form.locale }
						disabled={ locked( 'locale' ) }
						onChange={ set( 'locale' ) }
					/>
					<TextControl
						__nextHasNoMarginBottom
						label={ __( 'Site key', 'viglet-turing-search' ) }
						help={ sprintf(
							/* translators: %s: the key derived from this site's address. */
							__(
								"Keeps this site's documents apart when several sites share one SN site. Empty uses %s.",
								'viglet-turing-search'
							),
							form.derived_key
						) }
						value={ form.site_key }
						disabled={ locked( 'site_key' ) }
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
							'viglet-turing-search'
						) }
					</Button>
				</CardBody>
			</Card>

			{ result && (
				<Card>
					<CardHeader>
						<h2>{ __( '2. Test', 'viglet-turing-search' ) }</h2>
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
										'viglet-turing-search'
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
						<h2>
							{ __( '3. First index', 'viglet-turing-search' ) }
						</h2>
					</CardHeader>
					<CardBody>
						<p>
							{ __(
								'Sends every published post of the indexed types now. New and edited posts are sent on their own after that.',
								'viglet-turing-search'
							) }
						</p>
						<Button
							variant="primary"
							isBusy={ busy === 'index' }
							disabled={ !! busy }
							onClick={ indexAll }
						>
							{ __(
								'Index everything now',
								'viglet-turing-search'
							) }
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

const mount = document.getElementById( 'viglet-turing-setup' );
if ( mount ) {
	createRoot( mount ).render( <Setup /> );
}
