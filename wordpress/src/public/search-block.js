/**
 * The Turing search block in the browser: an Interactivity API store over the SDK's search
 * and autocomplete controllers (from `viglet-dumont/search`, already configured for this
 * site). render.php prints the markup and the directives; this keeps the state they read.
 */
import { getContext, store } from '@wordpress/interactivity';
/* eslint-disable import/no-unresolved -- a script module id, resolved by WordPress's import map at runtime. */
import {
	config,
	createClient,
	createSiteAutoComplete,
	createSiteSearch,
	fetchSpellCheck,
	siteFilter,
} from 'viglet-dumont/search';
/* eslint-enable import/no-unresolved */

let search = null;
let autocomplete = null;
let client = null;
// The query last sent, so a late spell-check answer for an older one is dropped.
let lastQuery = '';

const { state } = store( 'viglet-dumont/search', {
	state: {
		get hasSuggestions() {
			return state.suggestions.length > 0;
		},
		get hasFacets() {
			return state.facets.length > 0;
		},
		get hasPages() {
			return state.pages.length > 1;
		},
		get hasApplied() {
			return state.applied.length > 0;
		},
		get hasCorrection() {
			return '' !== state.corrected;
		},
		get summary() {
			if ( 'error' === state.status ) {
				return state.errorLabel;
			}
			if ( 'success' !== state.status ) {
				return '';
			}
			return state.total > 0
				? state.foundLabel.replace( '%d', String( state.total ) )
				: state.noneLabel;
		},
	},
	actions: {
		type( event ) {
			state.query = event.target.value;
			if ( state.query.trim().length > 1 ) {
				autocomplete.fetch( state.query );
			} else {
				autocomplete.clear();
			}
		},
		keydown( event ) {
			if ( 'Escape' === event.key ) {
				autocomplete.clear();
			}
		},
		submit( event ) {
			event.preventDefault();
			autocomplete.clear();
			run( state.query );
		},
		pick() {
			const { suggestion } = getContext();
			state.query = suggestion;
			autocomplete.clear();
			run( suggestion );
		},
		applyCorrection() {
			state.query = state.corrected;
			run( state.corrected );
		},
		toggleFacet() {
			const { item } = getContext();
			search.toggleFacet( item.target );
		},
		removeFilter() {
			const { filter } = getContext();
			search.toggleFacet( filter.target );
		},
		clearFilters() {
			// The SDK's clearAllFilters would drop the site filter too; keep it.
			search.searchRequest( {
				...search.getState().request,
				fq: [ siteFilter ],
				page: 1,
			} );
		},
		goToPage() {
			const { page } = getContext();
			search.goToPage( page.page );
		},
	},
	callbacks: {
		init() {
			search = createSiteSearch();
			autocomplete = createSiteAutoComplete();
			client = createClient();
			search.subscribe( sync );
			autocomplete.subscribe( ( s ) => {
				state.suggestions = s.suggestions.slice( 0, 8 );
			} );
			if ( state.query ) {
				run( state.query );
			}
		},
	},
} );

/**
 * Search, and keep the query in the address bar so the page can be shared or reloaded.
 *
 * @param {string} query What to search for; empty searches everything.
 */
function run( query ) {
	const url = new URL( window.location.href );
	url.searchParams.set( 'q', query );
	url.searchParams.delete( 's' );
	window.history.replaceState( null, '', url );
	lastQuery = query.trim() || '*';
	state.corrected = '';
	search.searchQuery( lastQuery );
}

/**
 * Turing's structured search leaves the spell-check out (T1114), so when a query finds
 * nothing the suggestion is asked for on its own.
 *
 * @param {string} query The query that found nothing.
 */
async function suggestCorrection( query ) {
	try {
		const spell = await fetchSpellCheck(
			client,
			config.site,
			config.locale,
			query
		);
		if ( query === lastQuery && spell?.correctedText ) {
			state.corrected = spell.corrected?.text || '';
		}
	} catch {
		// No suggestion is not an error the visitor needs to see.
	}
}

/**
 * The label the mapping gives a facet field, or the field name.
 *
 * @param {string} field A Turing field.
 */
function facetLabel( field ) {
	return (
		( config.facets || [] ).find( ( f ) => f.field === field )?.label ||
		field
	);
}

/**
 * Turing highlights the matched words with markup; the block shows plain text.
 *
 * @param {string} html A highlighted value.
 */
function plain( html ) {
	return (
		new window.DOMParser().parseFromString( html || '', 'text/html' ).body
			.textContent || ''
	);
}

/**
 * Copy what the SDK's search controller holds into the block's state.
 *
 * @param {Object} s The SearchControllerState.
 */
function sync( s ) {
	const response = s.structured;
	state.status = s.status;
	state.error = s.error || '';
	state.total = response?.queryContext?.count ?? 0;
	state.results = s.documents.map( ( d ) => ( {
		url: d.url,
		title: plain( d.title ) || d.url,
		description: plain( d.description ),
	} ) );
	state.facets = ( response?.facets || [] )
		.filter( ( f ) => f.items && f.items.length )
		.map( ( f ) => ( {
			label: f.label || f.name,
			items: f.items.map( ( i ) => ( {
				label: i.label,
				count: i.count,
				selected: i.selected,
				target: {
					action: i.action,
					field: i.field,
					filterQuery: i.filterQuery,
				},
			} ) ),
		} ) );
	// Turing's response stops describing a facet once it filters on it (T1115), so the
	// applied filters come from the request: every filter but the site's own.
	state.applied = ( s.request?.fq || [] )
		.filter( ( fq ) => fq !== siteFilter )
		.map( ( fq ) => {
			const [ field, ...rest ] = fq.split( ':' );
			const value = rest.join( ':' ).replace( /^"|"$/g, '' );
			return {
				label: `${ facetLabel( field ) }: ${ value }`,
				target: { action: 'REMOVE', field, filterQuery: fq },
			};
		} );
	state.pages = ( response?.pagination || [] )
		.filter( ( p ) => 'PAGE' === p.type || 'CURRENT' === p.type )
		.map( ( p ) => ( {
			page: p.page,
			text: p.text,
			// The structured response marks the current page by its type.
			current: !! p.current || 'CURRENT' === p.type,
		} ) );
	const spell = response?.spellCheck;
	if ( spell?.correctedText && ! spell.usingCorrectedText ) {
		state.corrected = spell.correctedTextValue || '';
	} else if (
		'success' === s.status &&
		0 === state.total &&
		'*' !== lastQuery
	) {
		void suggestCorrection( lastQuery );
	}
}
