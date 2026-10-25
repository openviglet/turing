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
import { createSiteSearch } from 'viglet-dumont/search';
/* eslint-enable import/no-unresolved */

const { labels, searchPage, queryParam } = getConfig( 'viglet-dumont/overlay' );

// How long typing must pause before the results follow it, as in the search block.
const instantMs = 250;
// How many results the overlay lists before "See all results".
const shown = 6;

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
 */
function fullSearchUrl( query ) {
	const url = new URL( searchPage );
	url.searchParams.set( queryParam, query );
	return url.toString();
}

/**
 * Build the dialog once.
 */
function build() {
	dialog = make( 'dialog', '', { 'aria-label': labels.title } );
	dialog.className = 'viglet-dumont-overlay';

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
	dialog.append( form, status, list, all, hint );
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

	search = createSiteSearch();
	search.subscribe( render );
}

/**
 * Search for what is typed, the last word as a prefix (Turing matches whole words, T1120).
 */
function update() {
	const typed = input.value.trim();
	if ( typed.length < 2 ) {
		results = [];
		active = -1;
		draw();
		status.textContent = '';
		return;
	}
	search.searchQuery(
		/[\p{L}\p{N}]$/u.test( typed ) ? `${ typed }*` : typed
	);
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
		status.textContent = total
			? labels.found.replace( '%d', String( total ) )
			: labels.none;
		results = s.documents.slice( 0, shown ).map( ( d ) => ( {
			url: d.url,
			title: plainText( d.title ) || d.url,
		} ) );
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
 * Redraw the list and the combobox's state from `results` and `active`.
 */
function draw() {
	list.replaceChildren(
		...results.map( ( result, i ) => {
			const option = make( 'li', 'result', {
				id: `viglet-dumont-overlay-result-${ i }`,
				role: 'option',
				'aria-selected': String( i === active ),
			} );
			option.append(
				make( 'span', 'title', {}, result.title ),
				make( 'span', 'url', {}, result.url )
			);
			// The input keeps the focus; a click on a result only opens it.
			option.addEventListener( 'mousedown', ( event ) =>
				event.preventDefault()
			);
			option.addEventListener( 'click', () =>
				window.location.assign( result.url )
			);
			return option;
		} )
	);
	input.setAttribute( 'aria-expanded', String( results.length > 0 ) );
	if ( active >= 0 ) {
		input.setAttribute(
			'aria-activedescendant',
			`viglet-dumont-overlay-result-${ active }`
		);
	} else {
		input.removeAttribute( 'aria-activedescendant' );
	}
}

/**
 * Up and Down move through the results; Enter is handled by the form's submit.
 *
 * @param {KeyboardEvent} event The key.
 */
function keydown( event ) {
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
	window.location.assign(
		active >= 0
			? results[ active ].url
			: fullSearchUrl( input.value.trim() )
	);
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
