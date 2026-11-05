/**
 * The Health page: is Turing reachable, how many documents each post type has in WordPress
 * and in Turing, how the queue stands, and buttons to retry what failed or send the queue now.
 *
 * Reads `viglet-dumont/v1/status`; the buttons call `queue/retry`, `queue/run` and
 * `reindex/cancel`. While a reindex runs, `reindex` is read again every few seconds.
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
import { __, _n, sprintf } from '@wordpress/i18n';

const EVENT_LABEL = {
	index: __( 'Indexed', 'viglet-turing-search' ),
	delete: __( 'Removed', 'viglet-turing-search' ),
	provision: __( 'Fields sent', 'viglet-turing-search' ),
	sweep: __( 'Sweep', 'viglet-turing-search' ),
	replay: __( 'Replayed', 'viglet-turing-search' ),
	reindex: __( 'Reindex', 'viglet-turing-search' ),
};

const ENDED = {
	done: __( 'Done', 'viglet-turing-search' ),
	failed: __( 'Ended with failures', 'viglet-turing-search' ),
	cancelled: __( 'Cancelled', 'viglet-turing-search' ),
};

const PHASE = {
	posts: __( 'Sending the posts.', 'viglet-turing-search' ),
	pdfs: __( 'Sending the PDFs.', 'viglet-turing-search' ),
	sweep: __(
		'Removing what WordPress no longer publishes.',
		'viglet-turing-search'
	),
};

/**
 * Index All, run by WP-Cron (D331): the run in progress with a Cancel button, and the last one
 * that ended. Started from Settings › Actions or Setup.
 *
 * @param {Object}     props          The props.
 * @param {Object}     props.report   `{running, last}` from `/reindex`.
 * @param {boolean}    props.busy     Whether a cancel is on its way.
 * @param {() => void} props.onCancel Cancel the running reindex.
 */
function Reindex( { report, busy, onCancel } ) {
	const { running, last } = report || {};

	return (
		<Card>
			<CardHeader>
				<h2>{ __( 'Reindex', 'viglet-turing-search' ) }</h2>
			</CardHeader>
			<CardBody>
				{ running && (
					<div aria-live="polite" className="viglet-dumont-reindex">
						<progress
							max={ 100 }
							value={
								running.total
									? ( running.offset / running.total ) * 100
									: 100
							}
							aria-label={ __(
								'Reindex progress',
								'viglet-turing-search'
							) }
							style={ { width: '100%' } }
						/>
						<p>
							{ sprintf(
								/* translators: 1: when it started, 2: posts done, 3: posts to do in all, 4: posts that failed. */
								__(
									'Running since %1$s: %2$d of %3$d posts done, %4$d failed.',
									'viglet-turing-search'
								),
								running.started,
								running.offset,
								running.total,
								running.failed
							) }{ ' ' }
							{ PHASE[ running.phase ] || '' }
						</p>
						{ running.retry_at && (
							<Notice status="warning" isDismissible={ false }>
								{ sprintf(
									/* translators: 1: why the batch failed, 2: when it is tried again. */
									__(
										'A batch failed (%1$s). It is tried again at %2$s.',
										'viglet-turing-search'
									),
									running.error,
									running.retry_at
								) }
							</Notice>
						) }
						{ running.stalled && (
							<Notice status="warning" isDismissible={ false }>
								{ __(
									'Nothing has moved for a while: WP-Cron is not running. It runs when someone visits the site; with DISABLE_WP_CRON, the server’s own cron has to call it.',
									'viglet-turing-search'
								) }
							</Notice>
						) }
						<Button
							variant="secondary"
							isDestructive
							isBusy={ busy }
							disabled={ busy }
							onClick={ onCancel }
						>
							{ __( 'Cancel reindex', 'viglet-turing-search' ) }
						</Button>
					</div>
				) }
				{ last ? (
					<p className="viglet-dumont-reindex-last">
						{ sprintf(
							/* translators: 1: how it ended (Done, Cancelled...), 2: when, 3: posts sent, 4: posts that failed, 5: posts it had to do. */
							__(
								'Last reindex: %1$s, %2$s. %3$d sent, %4$d failed, of %5$d posts.',
								'viglet-turing-search'
							),
							ENDED[ last.state ] || last.state,
							last.finished,
							last.sent,
							last.failed,
							last.total
						) }
						{ last.swept > 0 &&
							' ' +
								sprintf(
									/* translators: %d: documents removed from Turing. */
									__(
										'%d documents WordPress no longer publishes removed.',
										'viglet-turing-search'
									),
									last.swept
								) }
						{ last.sweep_error && ' ' + last.sweep_error }
						{ last.state === 'failed' &&
							last.error &&
							' ' + last.error }
					</p>
				) : (
					! running && (
						<p>
							{ __(
								'No reindex has run from wp-admin yet. Index All under Settings › Actions starts one.',
								'viglet-turing-search'
							) }
						</p>
					)
				) }
			</CardBody>
		</Card>
	);
}

/**
 * A moment as the site writes it: the server formats it in the site's date and time formats
 * and time zone, in the reader's language (D189), never the browser's.
 *
 * @param {{when?: string}} item An event or the last run.
 */
function formatTime( item ) {
	return item?.when || __( 'never', 'viglet-turing-search' );
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
				<h2>{ __( 'AI features', 'viglet-turing-search' ) }</h2>
			</CardHeader>
			<CardBody>
				<p>
					{ __(
						'Which of Turing’s generative features this site can use. Blocks that need one stay hidden while it is off.',
						'viglet-turing-search'
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
									{ __( 'Feature', 'viglet-turing-search' ) }
								</th>
								<th scope="col">
									{ __( 'State', 'viglet-turing-search' ) }
								</th>
								<th scope="col">
									{ __( 'Shown by', 'viglet-turing-search' ) }
								</th>
								<th scope="col">
									{ __( 'Why', 'viglet-turing-search' ) }
								</th>
							</tr>
						</thead>
						<tbody>
							{ rows.map( ( row ) => (
								<tr key={ row.feature }>
									<th scope="row">{ row.label }</th>
									<td>
										{ row.on
											? __( 'On', 'viglet-turing-search' )
											: __(
													'Off',
													'viglet-turing-search'
												) }
									</td>
									<td>{ row.block || '—' }</td>
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
							? __( 'Check again', 'viglet-turing-search' )
							: __(
									'Check AI features',
									'viglet-turing-search'
								) }
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

	// While a reindex runs, its progress is read again every few seconds; the rest of the page,
	// which asks Turing for counts, waits for the run to end.
	const running = status && status.reindex && status.reindex.running;
	useEffect( () => {
		if ( ! running ) {
			return undefined;
		}
		const timer = setTimeout( () => {
			apiFetch( { path: '/viglet-dumont/v1/reindex' } )
				.then( ( reindex ) =>
					reindex.running
						? setStatus( ( now ) => ( { ...now, reindex } ) )
						: load()
				)
				.catch( ( e ) => setError( e.message || String( e ) ) );
		}, 3000 );
		return () => clearTimeout( timer );
	}, [ running, load ] );

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
					<h2>{ __( 'Connection', 'viglet-turing-search' ) }</h2>
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
								'viglet-turing-search'
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
									'viglet-turing-search'
								),
								provisionError
							) }
						</Notice>
					) }
					{ status.foreign > 0 && (
						<Notice status="info" isDismissible={ false }>
							{ sprintf(
								/* translators: %d: how many documents another source sent under this site key. */
								_n(
									'%d document under this site key came from another source. Index All, Delete All and the sweep leave it alone.',
									'%d documents under this site key came from another source. Index All, Delete All and the sweep leave them alone.',
									status.foreign,
									'viglet-turing-search'
								),
								status.foreign
							) }
						</Notice>
					) }
				</CardBody>
			</Card>

			<Card>
				<CardHeader>
					<h2>{ __( 'Documents', 'viglet-turing-search' ) }</h2>
				</CardHeader>
				<CardBody>
					<table className="widefat striped">
						<thead>
							<tr>
								<th scope="col">
									{ __(
										'Post type',
										'viglet-turing-search'
									) }
								</th>
								<th scope="col">
									{ __(
										'Published in WordPress',
										'viglet-turing-search'
									) }
								</th>
								<th scope="col">
									{ __(
										'Indexed in Turing',
										'viglet-turing-search'
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
														'viglet-turing-search'
													)
												: counts.indexed }
											{ counts.indexed !== null &&
												counts.indexed !==
													counts.published && (
													<strong>
														{ ' ' +
															__(
																'(differs: run a reindex)',
																'viglet-turing-search'
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

			<Reindex
				report={ status.reindex }
				busy={ busy === 'reindex/cancel' }
				onCancel={ () =>
					act( 'reindex/cancel', () =>
						__( 'Reindex cancelled.', 'viglet-turing-search' )
					)
				}
			/>

			<Card>
				<CardHeader>
					<h2>{ __( 'Queue', 'viglet-turing-search' ) }</h2>
				</CardHeader>
				<CardBody>
					<p>
						{ sprintf(
							/* translators: 1: jobs waiting, 2: jobs that stopped retrying. */
							__(
								'%1$d waiting, %2$d stopped retrying.',
								'viglet-turing-search'
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
										'viglet-turing-search'
									),
									formatTime( status.last_run ),
									status.last_run.sent,
									status.last_run.failed
								)
							: __(
									'The queue has not sent anything yet.',
									'viglet-turing-search'
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
											'viglet-turing-search'
										),
										r.replayed
									)
								)
							}
						>
							{ __( 'Retry failed', 'viglet-turing-search' ) }
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
											'viglet-turing-search'
										),
										r.sent,
										r.failed
									)
								)
							}
						>
							{ __( 'Send queue now', 'viglet-turing-search' ) }
						</Button>
					</Flex>
				</CardBody>
			</Card>

			<AiFeatures />

			<Card>
				<CardHeader>
					<h2>{ __( 'Last events', 'viglet-turing-search' ) }</h2>
				</CardHeader>
				<CardBody>
					{ status.events && status.events.length ? (
						<table className="widefat striped">
							<thead>
								<tr>
									<th scope="col">
										{ __( 'When', 'viglet-turing-search' ) }
									</th>
									<th scope="col">
										{ __( 'What', 'viglet-turing-search' ) }
									</th>
									<th scope="col">
										{ __( 'Post', 'viglet-turing-search' ) }
									</th>
									<th scope="col">
										{ __(
											'Result',
											'viglet-turing-search'
										) }
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
												? __(
														'OK',
														'viglet-turing-search'
													)
												: __(
														'Failed',
														'viglet-turing-search'
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
								'viglet-turing-search'
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
