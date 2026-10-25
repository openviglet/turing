/**
 * The Search tuning page (D172): synonyms and promoted results on the SN site, written through
 * `viglet-dumont/v1/tuning` to Turing's console APIs with the stored key. What an operator
 * made in Turing's console is listed but never edited here.
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
	TextControl,
} from '@wordpress/components';
import {
	createRoot,
	useCallback,
	useEffect,
	useState,
} from '@wordpress/element';
import { __, sprintf } from '@wordpress/i18n';

/**
 * A row's action: Remove for what the plugin made, its origin for what it did not.
 *
 * @param {Object}                 props          The props.
 * @param {Object}                 props.item     A synonym or promoted result.
 * @param {(item: Object) => void} props.onRemove Removes one of the plugin's own.
 * @param {string}                 props.label    What the remove button names.
 */
function RowAction( { item, onRemove, label } ) {
	if ( ! item.ours ) {
		return (
			<span className="description">
				{ __( 'Set in Turing’s console', 'viglet-dumont' ) }
			</span>
		);
	}
	return (
		<Button
			variant="link"
			isDestructive
			onClick={ () => onRemove( item ) }
			aria-label={ label }
		>
			{ __( 'Remove', 'viglet-dumont' ) }
		</Button>
	);
}

function Synonyms( { rows, act } ) {
	const [ terms, setTerms ] = useState( '' );

	return (
		<Card>
			<CardHeader>
				<h2>{ __( 'Synonyms', 'viglet-dumont' ) }</h2>
			</CardHeader>
			<CardBody>
				<p>
					{ __(
						'Words a visitor may type for the same thing. A search for any of them finds posts with the others.',
						'viglet-dumont'
					) }
				</p>
				{ rows.length > 0 && (
					<table className="widefat striped">
						<thead>
							<tr>
								<th scope="col">
									{ __( 'Words', 'viglet-dumont' ) }
								</th>
								<th scope="col">
									<span className="screen-reader-text">
										{ __( 'Actions', 'viglet-dumont' ) }
									</span>
								</th>
							</tr>
						</thead>
						<tbody>
							{ rows.map( ( row ) => (
								<tr key={ row.id }>
									<td>{ row.terms.join( ' = ' ) }</td>
									<td>
										<RowAction
											item={ row }
											label={ sprintf(
												/* translators: %s: the synonym's words. */
												__(
													'Remove %s',
													'viglet-dumont'
												),
												row.terms.join( ', ' )
											) }
											onRemove={ () =>
												act( {
													path: `/viglet-dumont/v1/tuning/synonym/${ row.id }`,
													method: 'DELETE',
												} )
											}
										/>
									</td>
								</tr>
							) ) }
						</tbody>
					</table>
				) }
				<form
					onSubmit={ ( event ) => {
						event.preventDefault();
						act( {
							path: '/viglet-dumont/v1/tuning/synonym',
							method: 'POST',
							data: { terms: terms.split( ',' ) },
						} ).then( ( ok ) => ok && setTerms( '' ) );
					} }
				>
					<TextControl
						__nextHasNoMarginBottom
						__next40pxDefaultSize
						label={ __(
							'Words that mean the same, separated by commas',
							'viglet-dumont'
						) }
						value={ terms }
						onChange={ setTerms }
					/>
					<p>
						<Button variant="secondary" type="submit">
							{ __( 'Add synonym', 'viglet-dumont' ) }
						</Button>
					</p>
				</form>
			</CardBody>
		</Card>
	);
}

function Promoted( { rows, act } ) {
	const [ query, setQuery ] = useState( '' );
	const [ find, setFind ] = useState( '' );
	const [ found, setFound ] = useState( [] );
	const [ chosen, setChosen ] = useState( [] );

	// The site's own post search, as the editor's link picker uses it.
	useEffect( () => {
		if ( find.trim().length < 2 ) {
			return undefined;
		}
		const timer = window.setTimeout( () => {
			apiFetch( {
				path: `/wp/v2/search?type=post&subtype=any&per_page=5&search=${ encodeURIComponent(
					find
				) }`,
			} )
				.then( setFound )
				.catch( () => setFound( [] ) );
		}, 250 );
		return () => window.clearTimeout( timer );
	}, [ find ] );

	return (
		<Card>
			<CardHeader>
				<h2>{ __( 'Promoted results', 'viglet-dumont' ) }</h2>
			</CardHeader>
			<CardBody>
				<p>
					{ __(
						'Up to three posts shown first, as featured, when a visitor searches for these words.',
						'viglet-dumont'
					) }
				</p>
				{ rows.length > 0 && (
					<table className="widefat striped">
						<thead>
							<tr>
								<th scope="col">
									{ __( 'Search', 'viglet-dumont' ) }
								</th>
								<th scope="col">
									{ __( 'Shown first', 'viglet-dumont' ) }
								</th>
								<th scope="col">
									<span className="screen-reader-text">
										{ __( 'Actions', 'viglet-dumont' ) }
									</span>
								</th>
							</tr>
						</thead>
						<tbody>
							{ rows.map( ( row ) => (
								<tr key={ row.id }>
									<td>{ row.query }</td>
									<td>
										{ row.posts
											.map( ( post ) => post.title )
											.join( ', ' ) }
									</td>
									<td>
										<RowAction
											item={ row }
											label={ sprintf(
												/* translators: %s: the search the posts are promoted for. */
												__(
													'Remove the promotion for %s',
													'viglet-dumont'
												),
												row.query
											) }
											onRemove={ () =>
												act( {
													path: `/viglet-dumont/v1/tuning/spotlight/${ row.id }`,
													method: 'DELETE',
												} )
											}
										/>
									</td>
								</tr>
							) ) }
						</tbody>
					</table>
				) }
				<form
					onSubmit={ ( event ) => {
						event.preventDefault();
						act( {
							path: '/viglet-dumont/v1/tuning/spotlight',
							method: 'POST',
							data: {
								query,
								posts: chosen.map( ( post ) => post.id ),
							},
						} ).then( ( ok ) => {
							if ( ok ) {
								setQuery( '' );
								setChosen( [] );
								setFind( '' );
								setFound( [] );
							}
						} );
					} }
				>
					<TextControl
						__nextHasNoMarginBottom
						__next40pxDefaultSize
						label={ __(
							'When visitors search for',
							'viglet-dumont'
						) }
						value={ query }
						onChange={ setQuery }
					/>
					<TextControl
						__nextHasNoMarginBottom
						__next40pxDefaultSize
						label={ __(
							'Find a post to show first',
							'viglet-dumont'
						) }
						value={ find }
						onChange={ setFind }
					/>
					{ find.trim().length > 1 && found.length > 0 && (
						<ul aria-label={ __( 'Posts found', 'viglet-dumont' ) }>
							{ found.map( ( post ) => (
								<li key={ post.id }>
									{ post.title }{ ' ' }
									<Button
										variant="link"
										disabled={
											chosen.length >= 3 ||
											chosen.some(
												( c ) => c.id === post.id
											)
										}
										onClick={ () =>
											setChosen( [ ...chosen, post ] )
										}
										aria-label={ sprintf(
											/* translators: %s: a post's title. */
											__(
												'Show %s first',
												'viglet-dumont'
											),
											post.title
										) }
									>
										{ __( 'Add', 'viglet-dumont' ) }
									</Button>
								</li>
							) ) }
						</ul>
					) }
					{ chosen.length > 0 && (
						<ol aria-label={ __( 'Shown first', 'viglet-dumont' ) }>
							{ chosen.map( ( post ) => (
								<li key={ post.id }>
									{ post.title }{ ' ' }
									<Button
										variant="link"
										isDestructive
										onClick={ () =>
											setChosen(
												chosen.filter(
													( c ) => c.id !== post.id
												)
											)
										}
										aria-label={ sprintf(
											/* translators: %s: a post's title. */
											__(
												'Take %s out',
												'viglet-dumont'
											),
											post.title
										) }
									>
										{ __( 'Take out', 'viglet-dumont' ) }
									</Button>
								</li>
							) ) }
						</ol>
					) }
					<p>
						<Button
							variant="secondary"
							type="submit"
							disabled={ ! query.trim() || ! chosen.length }
						>
							{ __( 'Promote', 'viglet-dumont' ) }
						</Button>
					</p>
				</form>
			</CardBody>
		</Card>
	);
}

function Tuning() {
	const [ data, setData ] = useState( null );
	const [ error, setError ] = useState( '' );

	const load = useCallback( () => {
		apiFetch( { path: '/viglet-dumont/v1/tuning' } )
			.then( setData )
			.catch( ( e ) => setError( e.message || String( e ) ) );
	}, [] );
	useEffect( load, [ load ] );

	// Every change answers with the whole list again, so the screen shows what Turing holds.
	const act = ( request ) =>
		apiFetch( request )
			.then( ( next ) => {
				setData( next );
				setError( '' );
				return true;
			} )
			.catch( ( e ) => {
				setError( e.message || String( e ) );
				return false;
			} );

	if ( ! data ) {
		return error ? (
			<Notice status="error" isDismissible={ false }>
				{ error }
			</Notice>
		) : (
			<Spinner />
		);
	}

	return (
		<Flex direction="column" gap={ 4 } style={ { maxWidth: 960 } }>
			{ error && (
				<Notice status="error" onRemove={ () => setError( '' ) }>
					{ error }
				</Notice>
			) }
			<Synonyms rows={ data.synonyms } act={ act } />
			<Promoted rows={ data.spotlights } act={ act } />
		</Flex>
	);
}

const mount = document.getElementById( 'viglet-dumont-tuning' );
if ( mount ) {
	createRoot( mount ).render( <Tuning /> );
}
