/**
 * The Search insights page (D178): what visitors searched for in a period, what found nothing
 * and what found results nobody opened, read through `viglet-dumont/v1/insights` from the SN
 * site's metrics in Turing with the stored key. Nothing here changes the site; "Write about
 * this" opens a new draft titled with the search.
 */
import apiFetch from '@wordpress/api-fetch';
import {
	Card,
	CardBody,
	CardHeader,
	Flex,
	Notice,
	SelectControl,
	Spinner,
} from '@wordpress/components';
import { createRoot, useEffect, useState } from '@wordpress/element';
import { __, sprintf } from '@wordpress/i18n';

const PERIODS = [
	{ value: 'today', label: __( 'Last 24 hours', 'viglet-dumont' ) },
	{ value: 'this-week', label: __( 'Last 7 days', 'viglet-dumont' ) },
	{ value: 'this-month', label: __( 'Last 30 days', 'viglet-dumont' ) },
	{ value: 'all-time', label: __( 'All time', 'viglet-dumont' ) },
];

const percent = new Intl.NumberFormat(
	document.documentElement.lang || undefined,
	{
		style: 'percent',
		maximumFractionDigits: 1,
	}
);
const count = new Intl.NumberFormat(
	document.documentElement.lang || undefined
);
const when = new Intl.DateTimeFormat(
	document.documentElement.lang || undefined,
	{
		dateStyle: 'medium',
		timeStyle: 'short',
	}
);

/**
 * A new draft titled with a search nobody found an answer to.
 *
 * @param {string} term The search.
 */
function draftUrl( term ) {
	return `post-new.php?post_title=${ encodeURIComponent( term ) }`;
}

/**
 * Search tuning (D172) with a search that found nothing typed into its synonym field, so a
 * word the site does use can be added beside it.
 *
 * @param {string} term The search.
 */
function synonymUrl( term ) {
	return `admin.php?page=viglet-dumont-tuning&synonym=${ encodeURIComponent(
		term
	) }`;
}

/**
 * One table of terms, or the sentence that says there are none.
 *
 * @param {Object}   props         The props.
 * @param {string}   props.title   The card's heading.
 * @param {string}   props.empty   What an empty period says.
 * @param {Object[]} props.terms   The rows.
 * @param {Object[]} props.columns `{label, cell(term)}` per column after the term.
 * @param {boolean}  props.write   Whether each row offers "Write about this".
 * @param {boolean}  props.synonym Whether each row offers "Add a synonym" too.
 */
function Terms( { title, empty, terms, columns, write, synonym } ) {
	return (
		<Card>
			<CardHeader>
				<h2>{ title }</h2>
			</CardHeader>
			<CardBody>
				{ terms.length === 0 ? (
					<p>{ empty }</p>
				) : (
					<table className="widefat striped">
						<thead>
							<tr>
								<th scope="col">
									{ __( 'Search', 'viglet-dumont' ) }
								</th>
								{ columns.map( ( column ) => (
									<th scope="col" key={ column.label }>
										{ column.label }
									</th>
								) ) }
								{ write && (
									<th scope="col">
										<span className="screen-reader-text">
											{ __( 'Actions', 'viglet-dumont' ) }
										</span>
									</th>
								) }
							</tr>
						</thead>
						<tbody>
							{ terms.map( ( term ) => (
								<tr key={ term.term }>
									<th scope="row">{ term.term }</th>
									{ columns.map( ( column ) => (
										<td key={ column.label }>
											{ column.cell( term ) }
										</td>
									) ) }
									{ write && (
										<td>
											<a
												href={ draftUrl( term.term ) }
												aria-label={ sprintf(
													/* translators: %s: a search visitors made. */
													__(
														'Write about this: “%s”',
														'viglet-dumont'
													),
													term.term
												) }
											>
												{ __(
													'Write about this',
													'viglet-dumont'
												) }
											</a>
											{ synonym && (
												<>
													{ ' · ' }
													<a
														href={ synonymUrl(
															term.term
														) }
														aria-label={ sprintf(
															/* translators: %s: a search that found nothing. */
															__(
																'Add a synonym: “%s”',
																'viglet-dumont'
															),
															term.term
														) }
													>
														{ __(
															'Add a synonym',
															'viglet-dumont'
														) }
													</a>
												</>
											) }
										</td>
									) }
								</tr>
							) ) }
						</tbody>
					</table>
				) }
			</CardBody>
		</Card>
	);
}

const searchesColumn = {
	label: __( 'Searches', 'viglet-dumont' ),
	cell: ( term ) => count.format( term.searches ),
};

function Insights() {
	const [ period, setPeriod ] = useState( 'this-week' );
	// The answer, kept with the period it answers, so a slower one for another is not shown.
	const [ answer, setAnswer ] = useState( null );

	useEffect( () => {
		let current = true;
		apiFetch( { path: `/viglet-dumont/v1/insights?period=${ period }` } )
			.then( ( report ) => current && setAnswer( { period, report } ) )
			.catch(
				( error ) =>
					current &&
					setAnswer( {
						period,
						error: error.message || String( error ),
					} )
			);
		return () => {
			current = false;
		};
	}, [ period ] );

	const shown = answer && answer.period === period ? answer : null;
	const report = shown?.report;

	return (
		<Flex direction="column" gap={ 4 } style={ { maxWidth: 1000 } }>
			<p>
				{ __(
					'What visitors searched for on this site, what found nothing and what they never opened, from Turing’s search metrics. Searches made while typing are left out.',
					'viglet-dumont'
				) }
			</p>
			<SelectControl
				__nextHasNoMarginBottom
				label={ __( 'Period', 'viglet-dumont' ) }
				value={ period }
				options={ PERIODS }
				onChange={ setPeriod }
			/>
			{ ! shown && <Spinner /> }
			{ shown?.error && (
				<Notice status="error" isDismissible={ false }>
					{ shown.error }
				</Notice>
			) }
			{ report && 0 === report.searches && 0 === report.failed.length && (
				<Notice status="info" isDismissible={ false }>
					{ __(
						'No searches in this period yet. They appear here once visitors use the site’s search.',
						'viglet-dumont'
					) }
				</Notice>
			) }
			{ report && ( report.searches > 0 || report.failed.length > 0 ) && (
				<>
					<p className="viglet-dumont-insights__totals">
						{ sprintf(
							/* translators: 1: how many searches, 2: how many clicks, 3: the share of searches that led to a click. */
							__(
								'%1$s searches, %2$s clicks, %3$s click-through.',
								'viglet-dumont'
							),
							count.format( report.searches ),
							count.format( report.clicks ),
							percent.format( report.ctr )
						) }
					</p>
					<Terms
						title={ __( 'Top searches', 'viglet-dumont' ) }
						empty={ __(
							'No searches in this period.',
							'viglet-dumont'
						) }
						terms={ report.top }
						columns={ [
							searchesColumn,
							{
								label: __( 'Clicks', 'viglet-dumont' ),
								cell: ( term ) => count.format( term.clicks ),
							},
							{
								label: __( 'Click-through', 'viglet-dumont' ),
								cell: ( term ) => percent.format( term.ctr ),
							},
						] }
					/>
					<Terms
						title={ __(
							'Searches that found nothing',
							'viglet-dumont'
						) }
						empty={ __(
							'Every search found something.',
							'viglet-dumont'
						) }
						terms={ report.failed }
						columns={ [
							searchesColumn,
							{
								label: __( 'Last searched', 'viglet-dumont' ),
								cell: ( term ) =>
									term.lastSeen
										? when.format(
												new Date( term.lastSeen )
											)
										: '',
							},
						] }
						write
						synonym
					/>
					<Terms
						title={ __(
							'Found, but never opened',
							'viglet-dumont'
						) }
						empty={ __(
							'Every search that found results led to a click.',
							'viglet-dumont'
						) }
						terms={ report.unopened }
						columns={ [ searchesColumn ] }
						write
					/>
				</>
			) }
		</Flex>
	);
}

const mount = document.getElementById( 'viglet-dumont-insights' );
if ( mount ) {
	createRoot( mount ).render( <Insights /> );
}
