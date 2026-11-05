/**
 * The instant-search overlay: Ctrl/⌘+K anywhere, or the theme's own Search block (its form
 * carries `data-viglet-dumont-overlay`, set in Viglet_Dumont_Native_Search), opens a dialog
 * that answers from Turing as the visitor types. Up and Down choose a result, Enter opens it
 * or, with none chosen, takes the query to the full search page; Escape closes and the focus
 * goes back where it came from.
 *
 * The dialog is built on first use, from text nodes only: nothing Turing or the visitor
 * typed is ever parsed as HTML.
 */
import { getConfig } from '@wordpress/interactivity';
/* eslint-disable import/no-unresolved -- a script module id, resolved by WordPress's import map at runtime. */
import {
	cardLines,
	cardWhen,
	config,
	createAnswerController,
	createClient,
	createSiteSearch,
	documentType,
	formatCardDate,
	isAiPaused,
	isQuestion,
	originOf,
	reportAiPaused,
	siteUrl,
	trackResultClick,
} from 'viglet-dumont/search';
/* eslint-enable import/no-unresolved */

const {
	labels,
	searchPage,
	queryParam,
	layout,
	typeLabels,
	popular = [],
	answers = false,
} = getConfig( 'viglet-dumont/overlay' );

// D311: results grouped by type, with images and a "See all" per type, recent searches before
// anything is typed, and the whole screen on a phone.
const immersive = 'immersive' === layout;
// How long typing must pause before the results follow it, as in the search block.
const instantMs = 250;
// How many results the overlay lists before "See all results".
const shown = immersive ? 12 : 6;
// The search block's recent searches, kept in this browser only (search-block.js).
const recentKey = 'viglet-dumont-recent-searches';
const recentMax = 5;

/**
 * This browser's recent searches, newest first; none when storage is unavailable.
 */
function loadRecent() {
	try {
		const list = JSON.parse( window.localStorage.getItem( recentKey ) );
		return Array.isArray( list ) ? list.filter( Boolean ) : [];
	} catch {
		return [];
	}
}

/**
 * Remember a search the visitor settled on, as the search block does.
 *
 * @param {string} query The query.
 */
function saveRecent( query ) {
	if ( ! query ) {
		return;
	}
	try {
		window.localStorage.setItem(
			recentKey,
			JSON.stringify(
				[ query, ...loadRecent().filter( ( q ) => q !== query ) ].slice(
					0,
					recentMax
				)
			)
		);
	} catch {}
}
// The page language's plural rule, for the results count.
const plurals = new Intl.PluralRules(
	document.documentElement.lang || undefined
);
/**
 * A result's site when it is a sibling's (D303), date and first card field (D305), as one
 * line: the same date the search block's card shows (the type's card date field, else the
 * publication date).
 *
 * @param {Object} fields The document's fields.
 */
function meta( fields ) {
	const when = cardWhen( fields );
	const at = when ? new Date( when ) : null;
	const first = cardLines( fields )[ 0 ];
	return [
		// The sibling site a result comes from (D303).
		originOf( fields.id ),
		at && ! isNaN( at ) ? formatCardDate( at ) : '',
		first ? `${ first.label }: ${ first.value }` : '',
	]
		.filter( Boolean )
		.join( ' · ' );
}

let dialog = null;
let input = null;
let list = null;
let status = null;
let all = null;
let search = null;
let opener = null;
let timer = null;
let results = [];
let active = -1;
// The immersive layout's answer strip, preview pane and answer controller (D311).
let answerBox = null;
let preview = null;
let answerer = null;
let answerTimer = null;
// How long a question must rest before it is answered: each answer is an LLM call.
const answerMs = 1200;

/**
 * An element with a class and, optionally, attributes and text.
 *
 * @param {string} tag        The element.
 * @param {string} className  Its class, after the overlay's prefix.
 * @param {Object} attributes Attributes to set.
 * @param {string} text       Its text.
 */
function make( tag, className, attributes = {}, text = '' ) {
	const element = document.createElement( tag );
	if ( className ) {
		element.className = `viglet-dumont-overlay__${ className }`;
	}
	Object.entries( attributes ).forEach( ( [ name, value ] ) =>
		element.setAttribute( name, value )
	);
	element.textContent = text;
	return element;
}

/**
 * Where the full search for a query lives.
 *
 * @param {string} query The query.
 * @param {string} type  A post type to filter the results page to; empty for none.
 */
function fullSearchUrl( query, type = '' ) {
	const url = new URL( searchPage );
	url.searchParams.set( queryParam, query );
	// A type group's "See all" opens the results page filtered to that type (D311).
	if ( type ) {
		url.searchParams.append( 'fq[]', `type:${ type }` );
	}
	return url.toString();
}

/**
 * Build the dialog once.
 */
function build() {
	dialog = make( 'dialog', '', { 'aria-label': labels.title } );
	dialog.className = immersive
		? 'viglet-dumont-overlay viglet-dumont-overlay--immersive'
		: 'viglet-dumont-overlay';

	const form = make( 'form', 'form', { role: 'search' } );
	const label = make(
		'label',
		'',
		{ for: 'viglet-dumont-overlay-input' },
		labels.search
	);
	label.className = 'screen-reader-text';
	input = make( 'input', 'input', {
		id: 'viglet-dumont-overlay-input',
		type: 'search',
		role: 'combobox',
		autocomplete: 'off',
		'aria-autocomplete': 'list',
		'aria-expanded': 'false',
		'aria-controls': 'viglet-dumont-overlay-results',
		placeholder: labels.title,
	} );
	const close = make(
		'button',
		'close',
		{ type: 'button', 'aria-label': labels.close },
		'×'
	);
	form.append( label, input, close );

	list = make( 'ul', 'results', {
		id: 'viglet-dumont-overlay-results',
		role: 'listbox',
		'aria-label': labels.results,
	} );
	status = make( 'p', 'status', { 'aria-live': 'polite' } );
	all = make( 'a', 'all', { href: fullSearchUrl( '' ) }, labels.all );
	const hint = make( 'p', 'hint', {}, labels.hint );
	if ( immersive ) {
		// D311: Turing's answer above the results, and the highlighted result beside them.
		answerBox = make( 'section', 'answer', {
			'aria-label': labels.answer,
			'aria-live': 'polite',
			hidden: '',
		} );
		preview = make( 'aside', 'preview', {
			'aria-label': labels.preview,
			hidden: '',
		} );
		const body = make( 'div', 'body' );
		body.append( list, preview );
		dialog.append( form, status, answerBox, body, all, hint );
		if ( answers ) {
			answerer = createAnswerController( createClient(), {
				site: config.site,
				locale: config.locale,
			} );
			answerer.subscribe( ( s ) => {
				// A spent AI budget says so where the answer would be, and tells the
				// server (D336); the results below go on as ever.
				const paused = 'error' === s.status && isAiPaused( s.error );
				if ( paused ) {
					reportAiPaused();
				}
				answerBox.textContent = paused ? labels.paused : s.answer || '';
				answerBox.hidden = ! paused && ! s.answer;
			} );
		}
	} else {
		dialog.append( form, status, list, all, hint );
	}
	document.body.append( dialog );

	input.addEventListener( 'input', () => {
		window.clearTimeout( timer );
		timer = window.setTimeout( update, instantMs );
		all.href = fullSearchUrl( input.value.trim() );
	} );
	input.addEventListener( 'keydown', keydown );
	form.addEventListener( 'submit', ( event ) => {
		event.preventDefault();
		go();
	} );
	close.addEventListener( 'click', () => dialog.close() );
	// A click on the backdrop lands on the dialog itself.
	dialog.addEventListener( 'click', ( event ) => {
		if ( event.target === dialog ) {
			dialog.close();
		}
	} );
	dialog.addEventListener( 'close', () => opener?.focus?.() );

	// Tracked, so an opened result is reported through it; as-you-type searches never are.
	search = createSiteSearch( { track: true } );
	search.subscribe( render );
}

/**
 * Search for what is typed, as Turing's instant mode (T1120): the last word matches as a
 * prefix ("work" finds "workshop") and the search stays out of the site's search log.
 */
function update() {
	const typed = input.value.trim();
	if ( typed.length < 2 ) {
		// The immersive overlay starts from this browser's recent searches (D311).
		results = immersive
			? [
					...loadRecent().map( ( query ) => ( {
						kind: 'recent',
						group: labels.recent,
						title: query,
						query,
					} ) ),
					...popular.map( ( query ) => ( {
						kind: 'recent',
						group: labels.popular,
						title: query,
						query,
					} ) ),
				]
			: [];
		window.clearTimeout( answerTimer );
		answerer?.reset();
		active = -1;
		draw();
		status.textContent = '';
		return;
	}
	// A question that rests gets Turing's answer; anything else clears the strip.
	window.clearTimeout( answerTimer );
	if ( answerer && isQuestion( typed ) ) {
		answerTimer = window.setTimeout(
			() => answerer.ask( typed, { locale: config.locale } ),
			answerMs
		);
	} else {
		answerer?.reset();
	}
	search.searchRequest( {
		locale: config.locale,
		...search.getState().request,
		q: typed,
		mode: 'instant',
		page: 1,
		rows: shown,
	} );
}

/**
 * Follow the search controller: the top results, and a status line read aloud.
 *
 * @param {Object} s The SearchControllerState.
 */
function render( s ) {
	if ( 'loading' === s.status ) {
		status.textContent = labels.loading;
		return;
	}
	if ( 'error' === s.status ) {
		status.textContent = labels.error;
		results = [];
	} else if ( 'success' === s.status ) {
		const total = s.structured?.queryContext?.count ?? s.documents.length;
		// The form for one or for more, as the page's language counts (D184).
		const form =
			'one' === plurals.select( total )
				? labels.found.one
				: labels.found.other;
		status.textContent = total
			? form.replace( '%d', String( total ) )
			: labels.none;
		results = s.documents.slice( 0, shown ).map( ( d ) => {
			const type = documentType( d.raw?.fields );
			return {
				kind: 'result',
				id: String( d.raw?.fields?.id ?? '' ),
				url: siteUrl( d.url, d.raw?.fields?.id ),
				title: plainText( d.title ) || d.url,
				meta: meta( d.raw?.fields || {} ),
				image: d.image || '',
				snippet: plainText( d.description ),
				lines: cardLines( d.raw?.fields || {} ),
				type,
				group: typeLabels?.[ type ] || type,
			};
		} );
		if ( immersive ) {
			results = grouped( results, input.value.trim() );
		}
	}
	active = -1;
	draw();
}

/**
 * The text of a value Turing highlighted with markup.
 *
 * @param {string} html The value.
 */
function plainText( html ) {
	return (
		new window.DOMParser().parseFromString( html || '', 'text/html' ).body
			.textContent || ''
	);
}

/**
 * The results gathered by type, in the order each type first ranked, each group closed by
 * its "See all" entry into the results page filtered to that type (D311).
 *
 * @param {Object[]} entries The results, in Turing's order.
 * @param {string}   query   What is typed.
 */
function grouped( entries, query ) {
	const groups = new Map();
	for ( const entry of entries ) {
		groups.set( entry.type, [
			...( groups.get( entry.type ) || [] ),
			entry,
		] );
	}
	return [ ...groups.values() ].flatMap( ( group ) => [
		...group,
		{
			kind: 'all',
			group: group[ 0 ].group,
			title: labels.seeAll.replace( '%s', group[ 0 ].group ),
			url: fullSearchUrl( query, group[ 0 ].type ),
		},
	] );
}

/**
 * One entry as an option of the listbox.
 *
 * @param {Object} result The entry.
 * @param {number} i      Its index in `results`.
 */
function optionFor( result, i ) {
	const option = make( 'li', 'all' === result.kind ? 'see-all' : 'result', {
		id: `viglet-dumont-overlay-result-${ i }`,
		role: 'option',
		'aria-selected': String( i === active ),
	} );
	if ( immersive && result.image ) {
		option.classList.add( 'viglet-dumont-overlay__result--with-image' );
		const image = make( 'img', 'image', { alt: '', loading: 'lazy' } );
		image.src = result.image;
		option.append( image );
	}
	option.append( make( 'span', 'title', {}, result.title ) );
	if ( result.meta ) {
		option.append( make( 'span', 'meta', {}, result.meta ) );
	}
	if ( 'result' === result.kind ) {
		option.append( make( 'span', 'url', {}, result.url ) );
	}
	// The input keeps the focus; a click on a result only opens it.
	option.addEventListener( 'mousedown', ( event ) => event.preventDefault() );
	option.addEventListener( 'click', () => openResult( i ) );
	return option;
}

/**
 * The listbox's children: the options, or in the immersive layout a group per type (or for
 * the recent searches), each named, its options inside.
 */
function children() {
	if ( ! immersive ) {
		return results.map( optionFor );
	}
	const groups = [];
	results.forEach( ( result, i ) => {
		let group = groups[ groups.length - 1 ];
		if ( ! group || group.name !== result.group ) {
			const element = make( 'li', 'group', {
				role: 'group',
				'aria-label': result.group,
			} );
			element.append(
				make(
					'span',
					'group-title',
					{ 'aria-hidden': 'true' },
					result.group
				)
			);
			const items = make( 'ul', 'group-items', { role: 'none' } );
			element.append( items );
			group = { name: result.group, element, items };
			groups.push( group );
		}
		group.items.append( optionFor( result, i ) );
	} );
	return groups.map( ( group ) => group.element );
}

/**
 * Redraw the list and the combobox's state from `results` and `active`.
 */
function draw() {
	list.replaceChildren( ...children() );
	input.setAttribute( 'aria-expanded', String( results.length > 0 ) );
	if ( active >= 0 ) {
		input.setAttribute(
			'aria-activedescendant',
			`viglet-dumont-overlay-result-${ active }`
		);
	} else {
		input.removeAttribute( 'aria-activedescendant' );
	}
	showPreview();
}

/**
 * The immersive layout's preview pane (D311): the highlighted result's image, snippet and
 * card lines, so the arrows browse without leaving. Hidden when no result is highlighted.
 */
function showPreview() {
	if ( ! preview ) {
		return;
	}
	const result = results[ active ];
	if ( ! result || 'result' !== result.kind ) {
		preview.hidden = true;
		preview.replaceChildren();
		return;
	}
	const parts = [];
	if ( result.image ) {
		const image = make( 'img', 'preview-image', { alt: '' } );
		image.src = result.image;
		parts.push( image );
	}
	parts.push( make( 'p', 'preview-title', {}, result.title ) );
	if ( result.snippet ) {
		parts.push( make( 'p', 'preview-snippet', {}, result.snippet ) );
	}
	for ( const line of result.lines || [] ) {
		parts.push(
			make( 'p', 'preview-line', {}, `${ line.label }: ${ line.value }` )
		);
	}
	preview.replaceChildren( ...parts );
	preview.hidden = false;
}

/**
 * Up and Down move through the results; Enter is handled by the form's submit, and
 * Ctrl+Enter (⌘+Enter) opens the full results page whatever is highlighted (D311).
 *
 * @param {KeyboardEvent} event The key.
 */
function keydown( event ) {
	if ( 'Enter' === event.key && ( event.ctrlKey || event.metaKey ) ) {
		event.preventDefault();
		saveRecent( input.value.trim() );
		window.location.assign( fullSearchUrl( input.value.trim() ) );
		return;
	}
	if (
		! results.length ||
		! [ 'ArrowDown', 'ArrowUp' ].includes( event.key )
	) {
		return;
	}
	event.preventDefault();
	const step = 'ArrowDown' === event.key ? 1 : -1;
	active = ( active + step + results.length ) % results.length;
	draw();
}

/**
 * Enter: the chosen result, or the full search for what is typed.
 */
function go() {
	if ( active >= 0 ) {
		openResult( active );
		return;
	}
	saveRecent( input.value.trim() );
	window.location.assign( fullSearchUrl( input.value.trim() ) );
}

/**
 * Open a result, reporting which one it was (when the site counts clicks).
 *
 * @param {number} i Its index in the list.
 */
function openResult( i ) {
	const result = results[ i ];
	if ( 'recent' === result.kind ) {
		input.value = result.query;
		update();
		return;
	}
	if ( 'all' === result.kind ) {
		saveRecent( input.value.trim() );
		window.location.assign( result.url );
		return;
	}
	trackResultClick(
		{
			documentId: results[ i ].id,
			position: i + 1,
			term: input.value.trim(),
		},
		search
	);
	window.location.assign( results[ i ].url );
}

/**
 * Open the overlay, starting from what the visitor already typed elsewhere.
 *
 * @param {string} value The query to start with.
 */
function open( value = '' ) {
	if ( ! dialog ) {
		build();
	}
	if ( dialog.open ) {
		return;
	}
	opener = dialog.ownerDocument.activeElement;
	input.value = value;
	all.href = fullSearchUrl( value.trim() );
	dialog.showModal();
	input.focus();
	update();
}

document.addEventListener( 'keydown', ( event ) => {
	if (
		( event.ctrlKey || event.metaKey ) &&
		'k' === event.key.toLowerCase()
	) {
		event.preventDefault();
		open();
	}
} );

// The theme's Search block: a click in it, typing in it or submitting it opens the
// overlay. Focus alone does not, or closing the overlay (which gives the focus back)
// would open it again.
document
	.querySelectorAll( 'form[data-viglet-dumont-overlay]' )
	.forEach( ( form ) => {
		const field = form.querySelector(
			'input[type="search"], input[name="s"]'
		);
		const take = ( event ) => {
			event.preventDefault();
			open( field?.value || '' );
		};
		form.addEventListener( 'submit', take );
		field?.addEventListener( 'click', take );
		field?.addEventListener( 'input', () => {
			open( field.value );
			field.value = '';
		} );
		form.querySelector( 'button' )?.addEventListener( 'click', take );
		field?.setAttribute( 'aria-keyshortcuts', 'Control+K Meta+K' );
		field?.setAttribute( 'title', labels.shortcut );
	} );
