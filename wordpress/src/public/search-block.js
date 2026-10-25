/**
 * The Turing search block in the browser: an Interactivity API store over the SDK's search
 * and autocomplete controllers (from `viglet-dumont/search`, already configured for this
 * site). render.php prints the markup and the directives; this keeps the state they read.
 */
import { getContext, getElement, store } from '@wordpress/interactivity';
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
// The pending instant search, restarted by every keystroke.
let instant = null;
// The page's own title, which retitle() puts the query in front of.
let baseTitle = '';

// Each facet as Turing last described it unfiltered, by field (see facets()).
const facetMemory = new Map();
// The search learnFacets() last asked about, so it asks once per search.
let learning = '';
// How many values a facet shows before "Show more".
const facetValuesShown = 5;

// How long typing must pause before the results follow it.
const instantMs = 300;
// Recent searches live in this browser only and are never sent anywhere.
const recentKey = 'viglet-dumont-recent-searches';
const recentMax = 5;

const { state, actions } = store( 'viglet-dumont/search', {
	state: {
		// What the list offers: suggestions for what is typed, or recent searches for an
		// empty box. The part already typed is split off so it can be shown in bold.
		get options() {
			const typed = state.query.trim();
			const source = typed.length > 1 ? state.suggestions : state.recent;
			return source.map( ( text, i ) => {
				const prefix = text
					.toLowerCase()
					.startsWith( typed.toLowerCase() )
					? text.slice( 0, typed.length )
					: '';
				return {
					id: `${ state.listId }-${ i }`,
					text,
					prefix,
					rest: text.slice( prefix.length ),
					active: i === state.active,
				};
			} );
		},
		get isExpanded() {
			return state.open && state.options.length > 0;
		},
		get activeId() {
			return state.isExpanded && state.active >= 0
				? state.options[ state.active ]?.id
				: null;
		},
		get showsRecent() {
			return state.isExpanded && state.query.trim().length < 2;
		},
		get optionsAnnouncement() {
			return state.isExpanded
				? state.optionsLabel.replace(
						'%d',
						String( state.options.length )
					)
				: '';
		},
		get hasQuery() {
			return '' !== state.query;
		},
		get hasFacets() {
			return state.facets.length > 0;
		},
		get filtersLabel() {
			return state.applied.length
				? state.filtersLabelN.replace(
						'%d',
						String( state.applied.length )
					)
				: state.filtersLabel0;
		},
		// Read inside a facet's template: whether this one shows all its values.
		get facetExpanded() {
			return state.expanded.includes( getContext().facet.name );
		},
		get itemHidden() {
			return getContext().item.extra && ! state.facetExpanded;
		},
		get moreLabel() {
			return state.facetExpanded
				? state.fewerLabel
				: state.moreValues.replace(
						'%d',
						String( getContext().facet.more )
					);
		},
		get hasPages() {
			return state.pages.length > 1;
		},
		get hasApplied() {
			return state.applied.length > 0;
		},
		get hasResults() {
			return 'success' === state.status && state.total > 0;
		},
		get hasCorrection() {
			return '' !== state.corrected;
		},
		get isLoading() {
			return 'loading' === state.status;
		},
		// Placeholders only for a first search: a later one dims the results it replaces.
		get showSkeleton() {
			return state.isLoading && 0 === state.results.length;
		},
		get isEmpty() {
			return 'success' === state.status && 0 === state.total;
		},
		get isError() {
			return 'error' === state.status;
		},
		get wordpressSearchUrl() {
			const url = new URL( state.homeUrl );
			url.searchParams.set( 's', state.query );
			return url.toString();
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
			state.open = true;
			state.active = -1;
			window.clearTimeout( instant );
			if ( state.query.trim().length > 1 ) {
				autocomplete.fetch( state.query );
				// Results follow the typing once it pauses; the SDK drops any answer a
				// newer search has overtaken.
				instant = window.setTimeout(
					() => run( state.query, true ),
					instantMs
				);
			} else {
				autocomplete.clear();
			}
		},
		focus() {
			state.recent = loadRecent();
			state.open = true;
		},
		blur() {
			close();
		},
		// A click on an option must not blur the field first, or the list closes under it.
		keepFocus( event ) {
			event.preventDefault();
		},
		keydown( event ) {
			const count = state.options.length;
			switch ( event.key ) {
				case 'ArrowDown':
					event.preventDefault();
					state.open = true;
					state.active = count ? ( state.active + 1 ) % count : -1;
					break;
				case 'ArrowUp':
					event.preventDefault();
					state.open = true;
					state.active = count
						? ( state.active - 1 + count ) % count
						: -1;
					break;
				case 'Enter':
					if ( state.activeId ) {
						event.preventDefault();
						choose( state.options[ state.active ].text );
					}
					break;
				case 'Escape':
					// Closing the list comes first; only a closed list lets the browser
					// clear the field.
					if ( state.isExpanded ) {
						event.preventDefault();
						close();
					}
					break;
				case 'Delete':
					if (
						event.shiftKey &&
						state.showsRecent &&
						state.activeId
					) {
						event.preventDefault();
						forget( state.options[ state.active ].text );
					}
					break;
			}
		},
		submit( event ) {
			event.preventDefault();
			choose( state.query );
		},
		pick() {
			const { option } = getContext();
			choose( option.text );
		},
		clear() {
			window.clearTimeout( instant );
			state.query = '';
			autocomplete.clear();
			focusInput();
		},
		clearRecent() {
			saveRecent( [] );
			state.recent = [];
			focusInput();
		},
		applyCorrection() {
			state.query = state.corrected;
			run( state.corrected );
		},
		// The facets re-render from the answer, which would leave the focus nowhere; the
		// search box is where the next step starts. In the drawer the visitor may pick more
		// than one, so the focus stays there.
		toggleFacet() {
			const { item } = getContext();
			search.toggleFacet( item.target );
			writeUrl( true );
			if ( ! state.drawerOpen ) {
				focusInput();
			}
		},
		toggleMore() {
			const { facet } = getContext();
			state.expanded = state.facetExpanded
				? state.expanded.filter( ( name ) => name !== facet.name )
				: [ ...state.expanded, facet.name ];
		},
		openDrawer() {
			state.drawerOpen = true;
			// The first control in the drawer, once it shows.
			window.requestAnimationFrame( () =>
				document
					.querySelector(
						`#${ state.inputId }-facets summary, #${ state.inputId }-facets input`
					)
					?.focus()
			);
		},
		closeDrawer() {
			state.drawerOpen = false;
			document
				.querySelector( '.viglet-dumont-search__filters-toggle' )
				?.focus();
		},
		drawerKeydown( event ) {
			if ( 'Escape' === event.key && state.drawerOpen ) {
				event.preventDefault();
				actions.closeDrawer();
			}
		},
		removeFilter() {
			const { filter } = getContext();
			search.toggleFacet( filter.target );
			writeUrl( true );
			focusInput();
		},
		clearFilters() {
			// The SDK's clearAllFilters would drop the site filter too; keep it.
			send( { fq: [ siteFilter ], page: 1 } );
			writeUrl( true );
		},
		retry() {
			send( {} );
		},
		goToPage() {
			const { page } = getContext();
			search.goToPage( page.page );
			writeUrl( true );
			focusInput();
		},
		sortBy( event ) {
			state.sort = event.target.value;
			send( { page: 1 } );
			writeUrl( true );
		},
	},
	callbacks: {
		// The snippet keeps Turing's highlight: built from text nodes and <mark>, never HTML.
		snippet() {
			const { result } = getContext();
			const { ref } = getElement();
			ref.replaceChildren(
				...result.snippet.map( ( part ) => {
					if ( ! part.mark ) {
						return document.createTextNode( part.text );
					}
					const mark = document.createElement( 'mark' );
					mark.textContent = part.text;
					return mark;
				} )
			);
		},
		init() {
			search = createSiteSearch();
			autocomplete = createSiteAutoComplete();
			client = createClient();
			search.subscribe( sync );
			autocomplete.subscribe( ( s ) => {
				state.suggestions = s.suggestions.slice( 0, 8 );
			} );
			baseTitle = document.title;
			const params = new URL( window.location.href ).searchParams;
			if (
				state.query ||
				[ 'fq[]', 'pg', 'sort' ].some( ( key ) => params.has( key ) )
			) {
				readUrl();
			}
			window.addEventListener( 'popstate', readUrl );
		},
	},
} );

/**
 * Close the list and forget which option was active.
 */
function close() {
	state.open = false;
	state.active = -1;
}

/**
 * Search for what the visitor settled on (submitted, or picked from the list) and remember
 * it as a recent search.
 *
 * @param {string} query The query.
 */
function choose( query ) {
	window.clearTimeout( instant );
	state.query = query;
	autocomplete.clear();
	close();
	if ( query.trim() && '*' !== query.trim() ) {
		state.recent = [
			query.trim(),
			...loadRecent().filter( ( q ) => q !== query.trim() ),
		].slice( 0, recentMax );
		saveRecent( state.recent );
	}
	run( query );
}

/**
 * Drop one recent search.
 *
 * @param {string} query The search to forget.
 */
function forget( query ) {
	state.recent = loadRecent().filter( ( q ) => q !== query );
	saveRecent( state.recent );
	state.active = Math.min( state.active, state.recent.length - 1 );
}

/**
 * The recent searches this browser kept; none where storage is blocked.
 *
 * @return {string[]} Newest first.
 */
function loadRecent() {
	try {
		const kept = JSON.parse( window.localStorage.getItem( recentKey ) );
		return Array.isArray( kept ) ? kept.filter( Boolean ) : [];
	} catch {
		return [];
	}
}

/**
 * Keep the recent searches in this browser.
 *
 * @param {string[]} queries Newest first.
 */
function saveRecent( queries ) {
	try {
		window.localStorage.setItem( recentKey, JSON.stringify( queries ) );
	} catch {
		// Storage blocked: the list lasts as long as the page.
	}
}

/**
 * Put the focus back in the search box, without opening the list the focus would.
 */
function focusInput() {
	document.getElementById( state.inputId )?.focus();
	close();
}

/**
 * Search, and keep the query in the address bar so the page can be shared or reloaded.
 *
 * @param {string}  query  What to search for; empty searches everything.
 * @param {boolean} prefix Whether the last word is still being typed: Turing matches whole
 *                         words, so it is sent as a prefix ("work" finds "workshop"). The
 *                         address bar keeps what was typed. A guess at the query
 *                         syntax until Turing matches prefixes itself (T1120, D183).
 */
function run( query, prefix = false ) {
	const typed = query.trim();
	lastQuery = typed || '*';
	state.corrected = '';
	send( {
		q: prefix && /[\p{L}\p{N}]$/u.test( typed ) ? `${ typed }*` : lastQuery,
		page: 1,
	} );
	// A search the visitor settled on is a step Back returns to; typing is not.
	writeUrl( ! prefix );
}

/**
 * Search with these changes to the current request, keeping its filters, sort and page
 * size.
 *
 * @param {Object} changes Fields of the structured request.
 */
function send( changes ) {
	search.searchRequest( {
		locale: config.locale,
		...search.getState().request,
		rows: state.perPage,
		sort: state.sort,
		...changes,
	} );
}

/**
 * Put the whole search in the address bar, so it can be shared, reloaded and gone back to:
 * the query as typed, the filters (the site's own left out), the page and the sort. The page
 * is pg, since WordPress reads page as a page of the post the block sits in.
 *
 * @param {boolean} push A new history entry (a deliberate step) rather than a replacement.
 */
function writeUrl( push ) {
	const request = search.getState().request || {};
	const url = new URL( window.location.href );
	[ 'q', 's', 'fq[]', 'pg', 'sort' ].forEach( ( key ) =>
		url.searchParams.delete( key )
	);
	url.searchParams.set( 'q', state.query );
	( request.fq || [] )
		.filter( ( fq ) => fq !== siteFilter )
		.forEach( ( fq ) => url.searchParams.append( 'fq[]', fq ) );
	if ( Number( request.page ) > 1 ) {
		url.searchParams.set( 'pg', String( request.page ) );
	}
	if ( 'relevance' !== state.sort ) {
		url.searchParams.set( 'sort', state.sort );
	}
	if ( url.href !== window.location.href ) {
		window.history[ push ? 'pushState' : 'replaceState' ]( null, '', url );
	}
	retitle();
}

/**
 * Search for what the address bar says: on load, and on Back and Forward.
 */
function readUrl() {
	const params = new URL( window.location.href ).searchParams;
	state.query = params.get( 'q' ) ?? params.get( 's' ) ?? '';
	state.sort = params.get( 'sort' ) || 'relevance';
	lastQuery = state.query.trim() || '*';
	state.corrected = '';
	send( {
		q: lastQuery,
		fq: [ siteFilter, ...params.getAll( 'fq[]' ) ],
		page: Math.max( 1, Number( params.get( 'pg' ) ) || 1 ),
	} );
	retitle();
}

/**
 * The tab's title names the search: "workshop – Search – Site".
 */
function retitle() {
	const typed = state.query.trim();
	document.title = typed ? `${ typed } – ${ baseTitle }` : baseTitle;
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
 * Split a highlighted value into runs of plain and matched text. Turing marks the matched
 * words with <mark>; every other tag is dropped and only its text kept.
 *
 * @param {string} html A highlighted value.
 * @return {Array<{text: string, mark: boolean}>} The runs, in order.
 */
function highlighted( html ) {
	const body = new window.DOMParser().parseFromString(
		html || '',
		'text/html'
	).body;
	return [ ...body.childNodes ]
		.map( ( node ) => ( {
			text: node.textContent || '',
			mark: 'MARK' === node.nodeName,
		} ) )
		.filter( ( part ) => part.text );
}

/**
 * Where a result lives, as a reader scans it: the host, then the path's segments.
 *
 * @param {string} url The result's URL.
 */
function crumb( url ) {
	try {
		const { host, pathname } = new URL( url );
		return [ host, ...pathname.split( '/' ).filter( Boolean ) ].join(
			' › '
		);
	} catch {
		return url;
	}
}

const dateFormat = new Intl.DateTimeFormat(
	document.documentElement.lang || undefined,
	{ dateStyle: 'medium' }
);

/**
 * A result card from one of the SDK's resolved documents.
 *
 * @param {Object} d A ResolvedDocument.
 */
function card( d ) {
	const fields = d.raw?.fields || {};
	const at = new Date( fields.publication_date ?? d.date );
	const valid = d.date || fields.publication_date ? ! isNaN( at ) : false;
	return {
		url: d.url,
		title: plain( d.title ) || d.url,
		type: state.typeLabels[ fields.type ] || fields.type || '',
		date: valid ? dateFormat.format( at ) : '',
		iso: valid ? at.toISOString() : '',
		crumb: crumb( d.url ),
		image: d.image || '',
		snippet: highlighted( d.description ),
	};
}

/**
 * The facets to show, in the order they were first answered. Turing stops describing a
 * facet once the search filters on it (T1115), so a filtered facet is rebuilt from the last
 * answer that described it: its applied values checked (and removable), the others still
 * there to switch to, with the counts they had before the filter.
 *
 * @param {Object[]} answered The response's facets.
 * @param {string[]} applied  The request's filters, the site's own left out.
 */
function facets( answered, applied ) {
	const filtered = new Set( applied.map( ( fq ) => fq.split( ':' )[ 0 ] ) );
	for ( const facet of answered ) {
		if ( facet.items?.length && ! filtered.has( facet.name ) ) {
			facetMemory.set( facet.name, facet );
		}
	}
	// A page opened filtered (a shared link, a reload) never saw those facets unfiltered.
	if (
		[ ...filtered ].some(
			( name ) =>
				! facetMemory.has( name ) &&
				! answered.some( ( facet ) => facet.name === name )
		)
	) {
		learnFacets();
	}
	const byName = new Map(
		answered
			.filter( ( facet ) => facet.items?.length )
			.map( ( facet ) => [ facet.name, facet ] )
	);
	const names = [ ...new Set( [ ...facetMemory.keys(), ...byName.keys() ] ) ];
	return names
		.map(
			( name ) =>
				byName.get( name ) ||
				( filtered.has( name ) ? facetMemory.get( name ) : null )
		)
		.filter( Boolean )
		.map( ( facet ) => {
			const items = byLabelWithinCount( facet.items ).map( ( i ) => {
				const selected =
					i.selected || applied.includes( i.filterQuery );
				return {
					label: i.label,
					count: i.count,
					selected,
					target: {
						action: selected ? 'REMOVE' : i.action,
						field: i.field,
						filterQuery: i.filterQuery,
					},
				};
			} );
			return {
				name: facet.name,
				label: facet.label || facet.name,
				items: items.map( ( item, index ) => ( {
					...item,
					extra: index >= facetValuesShown && ! item.selected,
				} ) ),
				hasMore: items.length > facetValuesShown,
				more: items.length - facetValuesShown,
				selectedCount: items.filter( ( item ) => item.selected ).length,
			};
		} );
}

/**
 * Turing's order, with each run of values that share a count put in label order: Turing
 * leaves ties in no fixed order, so the same search would list them differently each time.
 *
 * @param {Object[]} items A facet's items, in Turing's order.
 */
function byLabelWithinCount( items ) {
	const runs = [];
	for ( const item of items ) {
		const last = runs[ runs.length - 1 ];
		if ( last && last[ 0 ].count === item.count ) {
			last.push( item );
		} else {
			runs.push( [ item ] );
		}
	}
	return runs.flatMap( ( tie ) =>
		tie.sort( ( a, b ) =>
			String( a.label ).localeCompare( String( b.label ) )
		)
	);
}

/**
 * Ask once, without the filters, for the facets a filtered page has not seen, and redraw
 * the block when they arrive. Only a page opened already filtered needs this; until T1115
 * Turing does not describe a facet it filters on.
 */
function learnFacets() {
	const key = `${ lastQuery }|${ ( search.getState().request?.fq || [] ).join( '|' ) }`;
	if ( learning === key ) {
		return;
	}
	learning = key;
	const probe = createSiteSearch();
	const unsubscribe = probe.subscribe( ( s ) => {
		if ( 'success' !== s.status && 'error' !== s.status ) {
			return;
		}
		unsubscribe?.();
		for ( const facet of s.structured?.facets || [] ) {
			if ( facet.items?.length && ! facetMemory.has( facet.name ) ) {
				facetMemory.set( facet.name, facet );
			}
		}
		sync( search.getState() );
	} );
	probe.searchRequest( {
		locale: config.locale,
		q: lastQuery,
		fq: [ siteFilter ],
		rows: 1,
	} );
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
	state.results = s.documents.map( card );
	state.facets = facets(
		response?.facets || [],
		( s.request?.fq || [] ).filter( ( fq ) => fq !== siteFilter )
	);
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
