/**
 * The Health page: is Turing reachable, how many documents each post type has in WordPress
 * and in Turing, how the queue stands, and buttons to retry what failed or send the queue now.
 *
 * Reads `viglet-dumont/v1/status`; the buttons call `queue/retry` and `queue/run`.
 */
import apiFetch from '@wordpress/api-fetch';
import {
	Button,
	Card,
	CardBody,
	CardHeader,
	Flex,
	Notice,
	Spinner,
} from '@wordpress/components';
import {
	createRoot,
	useCallback,
	useEffect,
	useState,
} from '@wordpress/element';
import { __, sprintf } from '@wordpress/i18n';

const EVENT_LABEL = {
	index: __( 'Indexed', 'viglet-dumont' ),
	delete: __( 'Removed', 'viglet-dumont' ),
	provision: __( 'Fields sent', 'viglet-dumont' ),
	sweep: __( 'Sweep', 'viglet-dumont' ),
};

/**
 * A moment as the site writes it: the server formats it in the site's date and time formats
 * and time zone, in the reader's language (D189), never the browser's.
 *
 * @param {{when?: string}} item An event or the last run.
 */
function formatTime( item ) {
	return item?.when || __( 'never', 'viglet-dumont' );
}

/**
 * Which of Turing's AI features this site can use. Checked on demand: Turing has no cheap
 * probe for answers and natural-language filters (T1123), so a check spends a short LLM
 * call on each one that is on.
 */
function AiFeatures() {
	const [ rows, setRows ] = useState( null );
	const [ busy, setBusy ] = useState( false );
	const [ error, setError ] = useState( '' );

	const check = () => {
		setBusy( true );
		setError( '' );
		apiFetch( { path: '/viglet-dumont/v1/ai' } )
			.then( setRows )
			.catch( ( e ) => setError( e.message || String( e ) ) )
			.finally( () => setBusy( false ) );
	};

	return (
		<Card>
			<CardHeader>
				<h2>{ __( 'AI features', 'viglet-dumont' ) }</h2>
			</CardHeader>
			<CardBody>
				<p>
					{ __(
						'Which of Turing’s generative features this site can use. Blocks that need one stay hidden while it is off.',
						'viglet-dumont'
					) }
				</p>
				{ error && (
					<Notice status="error" isDismissible={ false }>
						{ error }
					</Notice>
				) }
				{ rows && (
					<table className="widefat striped">
						<thead>
							<tr>
								<th scope="col">
									{ __( 'Feature', 'viglet-dumont' ) }
								</th>
								<th scope="col">
									{ __( 'State', 'viglet-dumont' ) }
								</th>
								<th scope="col">
									{ __( 'Why', 'viglet-dumont' ) }
								</th>
							</tr>
						</thead>
						<tbody>
							{ rows.map( ( row ) => (
								<tr key={ row.feature }>
									<th scope="row">{ row.label }</th>
									<td>
										{ row.on
											? __( 'On', 'viglet-dumont' )
											: __( 'Off', 'viglet-dumont' ) }
									</td>
									<td>
										{ [ row.reason, row.fix ]
											.filter( Boolean )
											.join( ' ' ) || '—' }
									</td>
								</tr>
							) ) }
						</tbody>
					</table>
				) }
				<p>
					<Button
						variant="secondary"
						isBusy={ busy }
						disabled={ busy }
						onClick={ check }
					>
						{ rows
							? __( 'Check again', 'viglet-dumont' )
							: __( 'Check AI features', 'viglet-dumont' ) }
					</Button>
				</p>
			</CardBody>
		</Card>
	);
}

function Health() {
	const [ status, setStatus ] = useState( null );
	const [ error, setError ] = useState( '' );
	const [ busy, setBusy ] = useState( '' );
	const [ notice, setNotice ] = useState( '' );

	const load = useCallback( () => {
		setError( '' );
		apiFetch( { path: '/viglet-dumont/v1/status' } )
			.then( setStatus )
			.catch( ( e ) => setError( e.message || String( e ) ) );
	}, [] );

	useEffect( load, [ load ] );

	const act = ( path, describe ) => {
		setBusy( path );
		setNotice( '' );
		apiFetch( { path: `/viglet-dumont/v1/${ path }`, method: 'POST' } )
			.then( ( result ) => setNotice( describe( result ) ) )
			.catch( ( e ) => setError( e.message || String( e ) ) )
			.finally( () => {
				setBusy( '' );
				load();
			} );
	};

	if ( error && ! status ) {
		return (
			<Notice status="error" isDismissible={ false }>
				{ error }
			</Notice>
		);
	}
	if ( ! status ) {
		return <Spinner />;
	}

	const connected = status.connection && status.connection.success;
	const provisionError = status.provisioned && status.provisioned.error;

	return (
		<Flex direction="column" gap={ 4 } style={ { maxWidth: 960 } }>
			{ error && (
				<Notice status="error" onRemove={ () => setError( '' ) }>
					{ error }
				</Notice>
			) }
			{ notice && (
				<Notice status="success" onRemove={ () => setNotice( '' ) }>
					{ notice }
				</Notice>
			) }

			<Card>
				<CardHeader>
					<h2>{ __( 'Connection', 'viglet-dumont' ) }</h2>
				</CardHeader>
				<CardBody>
					<Notice
						status={ connected ? 'success' : 'error' }
						isDismissible={ false }
					>
						{ status.connection.message }
					</Notice>
					<p>
						{ sprintf(
							/* translators: 1: the SN site, 2: the site key. */
							__(
								'SN site %1$s, site key %2$s.',
								'viglet-dumont'
							),
							status.site_name,
							status.site_key
						) }
					</p>
					{ provisionError && (
						<Notice status="warning" isDismissible={ false }>
							{ sprintf(
								/* translators: %s: why Turing refused the fields. */
								__(
									'The mapped fields are not provisioned: %s',
									'viglet-dumont'
								),
								provisionError
							) }
						</Notice>
					) }
				</CardBody>
			</Card>

			<Card>
				<CardHeader>
					<h2>{ __( 'Documents', 'viglet-dumont' ) }</h2>
				</CardHeader>
				<CardBody>
					<table className="widefat striped">
						<thead>
							<tr>
								<th scope="col">
									{ __( 'Post type', 'viglet-dumont' ) }
								</th>
								<th scope="col">
									{ __(
										'Published in WordPress',
										'viglet-dumont'
									) }
								</th>
								<th scope="col">
									{ __(
										'Indexed in Turing',
										'viglet-dumont'
									) }
								</th>
							</tr>
						</thead>
						<tbody>
							{ Object.entries( status.types ).map(
								( [ type, counts ] ) => (
									<tr key={ type }>
										<th scope="row">{ type }</th>
										<td>{ counts.published }</td>
										<td>
											{ counts.indexed === null
												? __(
														'unknown',
														'viglet-dumont'
													)
												: counts.indexed }
											{ counts.indexed !== null &&
												counts.indexed !==
													counts.published && (
													<strong>
														{ ' ' +
															__(
																'(differs: run a reindex)',
																'viglet-dumont'
															) }
													</strong>
												) }
										</td>
									</tr>
								)
							) }
						</tbody>
					</table>
				</CardBody>
			</Card>

			<Card>
				<CardHeader>
					<h2>{ __( 'Queue', 'viglet-dumont' ) }</h2>
				</CardHeader>
				<CardBody>
					<p>
						{ sprintf(
							/* translators: 1: jobs waiting, 2: jobs that stopped retrying. */
							__(
								'%1$d waiting, %2$d stopped retrying.',
								'viglet-dumont'
							),
							status.queue.pending,
							status.queue.failed
						) }
					</p>
					<p>
						{ status.last_run
							? sprintf(
									/* translators: 1: when, 2: documents sent, 3: documents that failed. */
									__(
										'Last run %1$s: %2$d sent, %3$d failed.',
										'viglet-dumont'
									),
									formatTime( status.last_run ),
									status.last_run.sent,
									status.last_run.failed
								)
							: __(
									'The queue has not sent anything yet.',
									'viglet-dumont'
								) }
					</p>
					<Flex justify="flex-start">
						<Button
							variant="secondary"
							isBusy={ busy === 'queue/retry' }
							disabled={ !! busy || ! status.queue.failed }
							onClick={ () =>
								act( 'queue/retry', ( r ) =>
									sprintf(
										/* translators: %d: failed jobs put back in line. */
										__(
											'%d failed jobs put back in line.',
											'viglet-dumont'
										),
										r.replayed
									)
								)
							}
						>
							{ __( 'Retry failed', 'viglet-dumont' ) }
						</Button>
						<Button
							variant="primary"
							isBusy={ busy === 'queue/run' }
							disabled={
								!! busy ||
								! ( status.queue.pending + status.queue.failed )
							}
							onClick={ () =>
								act( 'queue/run', ( r ) =>
									sprintf(
										/* translators: 1: sent, 2: failed. */
										__(
											'Sent %1$d, %2$d failed.',
											'viglet-dumont'
										),
										r.sent,
										r.failed
									)
								)
							}
						>
							{ __( 'Send queue now', 'viglet-dumont' ) }
						</Button>
					</Flex>
				</CardBody>
			</Card>

			<AiFeatures />

			<Card>
				<CardHeader>
					<h2>{ __( 'Last events', 'viglet-dumont' ) }</h2>
				</CardHeader>
				<CardBody>
					{ status.events && status.events.length ? (
						<table className="widefat striped">
							<thead>
								<tr>
									<th scope="col">
										{ __( 'When', 'viglet-dumont' ) }
									</th>
									<th scope="col">
										{ __( 'What', 'viglet-dumont' ) }
									</th>
									<th scope="col">
										{ __( 'Post', 'viglet-dumont' ) }
									</th>
									<th scope="col">
										{ __( 'Result', 'viglet-dumont' ) }
									</th>
								</tr>
							</thead>
							<tbody>
								{ status.events.map( ( event, index ) => (
									<tr key={ index }>
										<td>{ formatTime( event ) }</td>
										<td>
											{ EVENT_LABEL[ event.type ] ||
												event.type }
										</td>
										<td>{ event.post_id || '—' }</td>
										<td>
											{ event.ok
												? __( 'OK', 'viglet-dumont' )
												: __(
														'Failed',
														'viglet-dumont'
													) }
											{ event.message
												? ': ' + event.message
												: '' }
										</td>
									</tr>
								) ) }
							</tbody>
						</table>
					) : (
						<p>
							{ __(
								'Nothing has been sent yet.',
								'viglet-dumont'
							) }
						</p>
					) }
				</CardBody>
			</Card>
		</Flex>
	);
}

const mount = document.getElementById( 'viglet-dumont-health' );
if ( mount ) {
	createRoot( mount ).render( <Health /> );
}
