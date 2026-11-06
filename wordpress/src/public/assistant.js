/**
 * The Site assistant in the browser (D175): a conversation with Turing's chat over this site's
 * posts (`POST /api/sn/{site}/chat/conversation`, through the SDK's chat controller). Answers
 * stream in as they are written, with the posts they used. The conversation is the visitor's
 * own (Turing's TUR_SESSION cookie) and lasts across the pages of this tab, so a cited post
 * can be opened without losing it; "New conversation" starts again.
 */
import { getContext, getElement, store } from '@wordpress/interactivity';
/* eslint-disable import/no-unresolved -- a script module id, resolved by WordPress's import map at runtime. */
import {
	clearTurSession,
	config,
	createChatController,
	createClient,
	isAiPaused,
	recordAnswerSignal,
	renderMarkdown,
	reportAiPaused,
	siblingSites,
	siteFilters,
	siteUrl,
} from 'viglet-turing/search';
/* eslint-enable import/no-unresolved */

// Turing exempts /api/sn/** from CSRF, so the token round trip would be wasted.
const client = createClient( { getCsrfToken: () => null } );
let chat = null;
let unsubscribe = null;
// The answers already rated, by message id.
const rated = new Set();
// The answers the search block handed over (T1258): rated there, so not asked about again.
const seeded = new Set();
// Whether chat is on, as last told to the search blocks on the page (T1258).
let announced = null;

/**
 * What a turn asked on this page carries (D242): the indexed post it shows, which Turing reads
 * first (T1178). Per turn, since the conversation follows the visitor from page to page.
 *
 * @return {Object} The chat controller's send overrides.
 */
function turn() {
	return state.documentId ? { documentId: state.documentId } : {};
}

/**
 * The origins the assistant may open a page on: this site's, and those of the sibling sites
 * it also searches whose address the settings give (D303), since it cites their documents.
 */
const origins = new Set( [
	window.location.origin,
	...siblingSites
		.map( ( site ) => {
			try {
				return site.url ? new URL( site.url ).origin : '';
			} catch {
				return '';
			}
		} )
		.filter( Boolean ),
] );

/**
 * A page of this site or of a sibling site it searches, or nothing: the assistant may take
 * the visitor to a post it can cite, never anywhere else.
 *
 * @param {string} url Where the agent asked to go.
 * @return {string} The absolute URL, or '' for any other site.
 */
function onThisSite( url ) {
	try {
		const target = new URL( String( url || '' ), window.location.href );
		return origins.has( target.origin ) ? target.href : '';
	} catch {
		return '';
	}
}

/**
 * The one host action a content site allows (the SDK's `navigate`): open a page of this site.
 * Written here rather than through createHostActions() so a refused URL is reported to the
 * agent as refused, not as done.
 *
 * @param {{url: string}} args The agent's arguments.
 */
function navigate( args ) {
	const url = onThisSite( args?.url );
	if ( ! url ) {
		return {
			success: false,
			error: 'only pages of this site, or of the sites it searches, can be opened',
		};
	}
	window.location.assign( url );
	return { success: true, url };
}

/**
 * The posts an answer used, each once: the cited ones, else what retrieval found.
 *
 * @param {Object} message A ChatMessage.
 */
function sourcesOf( message ) {
	const seen = new Set();
	const cited = ( message.citations || [] ).map( ( c ) => ( {
		url: c.url,
		title: c.documentTitle,
		sourceId: c.sourceId,
	} ) );
	return ( cited.length ? cited : message.sources || [] )
		.filter( ( s ) => s.url && ! seen.has( s.url ) && seen.add( s.url ) )
		.map( ( s ) => ( {
			url: siteUrl( s.url, s.sourceId ),
			title: s.title || s.url,
		} ) );
}

/**
 * The results page for a question (T1258), for an answer that cites more posts than it can
 * list: where the visitor sees everything the site has on it.
 *
 * @param {string} question What the visitor asked.
 * @return {string} The URL, or '' without a question or a results page.
 */
function resultsFor( question ) {
	if ( ! question || ! state.searchPage ) {
		return '';
	}
	try {
		const url = new URL( state.searchPage, window.location.href );
		url.searchParams.set( state.queryParam || 's', question );
		return url.href;
	} catch {
		return '';
	}
}

/**
 * Tell the search blocks on the page whether the assistant can take a conversation over
 * (T1258), now and to any that starts later.
 *
 * @param {boolean} available Whether chat is on.
 */
function announce( available ) {
	if ( available === announced ) {
		return;
	}
	announced = available;
	window.vigletTuringAssistant = { available };
	window.dispatchEvent(
		new window.CustomEvent( 'viglet-turing:assistant', {
			detail: { available },
		} )
	);
}

/**
 * The controller's state, as the template reads it.
 *
 * @param {Object} s ChatControllerState.
 */
function sync( s ) {
	state.available = true === s.enabled;
	announce( state.available );
	state.streaming = s.isStreaming;
	state.status = s.isStreaming ? state.thinking : '';
	if ( 'error' === s.status ) {
		// A spent AI budget says so, with its reset when the site knows it, and tells the
		// server (D336); anything else is the assistant failing.
		const paused = isAiPaused( s.error );
		if ( paused ) {
			reportAiPaused();
		}
		state.status = paused ? state.paused : state.failed;
	}
	state.messages = s.messages.map( ( message, i ) => {
		const last = i === s.messages.length - 1;
		const answer = 'assistant' === message.role;
		const done =
			answer && ! ( last && s.isStreaming ) && !! message.content;
		const sources = answer ? sourcesOf( message ) : [];
		const asked = answer
			? s.messages
					.slice( 0, i )
					.reverse()
					.find( ( m ) => 'user' === m.role )
			: null;
		return {
			id: message.id,
			visitor: ! answer,
			text: message.content,
			sources,
			// More posts than it lists (T1258): the results page has them all.
			more:
				done && sources.length > 3 ? resultsFor( asked?.content ) : '',
			canRate:
				done && ! rated.has( message.id ) && ! seeded.has( message.id ),
			rated: rated.has( message.id ),
		};
	} );
}

/**
 * Report whether an answer helped, to Turing's self-tuning, once per answer.
 *
 * @param {string} signal THUMBS_UP or THUMBS_DOWN.
 */
function rate( signal ) {
	const { message } = getContext();
	const messages = chat.getState().messages;
	const at = messages.findIndex( ( m ) => m.id === message.id );
	const asked = messages
		.slice( 0, at )
		.reverse()
		.find( ( m ) => 'user' === m.role );
	rated.add( message.id );
	sync( chat.getState() );
	void recordAnswerSignal( client, config.site, {
		signal,
		conversationId: chat.getState().conversationId,
		query: asked?.content,
	} );
}

/**
 * The chat controller, created with `initialMessages` to start from turns already had.
 *
 * @param {Array} initialMessages ChatMessages to start a new conversation with.
 */
function start( initialMessages ) {
	chat = createChatController( client, {
		site: config.site,
		locale: config.locale,
		// This site's documents only, where several sites share one SN site
		// (D287): the search block's own filter, sent as each turn's filters.
		filters: siteFilters,
		persist: 'viglet-turing-assistant',
		clientTools: { navigate },
		initialMessages,
	} );
	unsubscribe = chat.subscribe( sync );
}

/**
 * Continue a search answer here (T1258): a new conversation whose first exchange is the
 * question the visitor searched and the answer they read, with its sources, so the next
 * question follows on from it. The open assistant is the page's first one shown.
 *
 * @param {{question: string, answer: string, sources: Array}} turns What the search block had.
 */
function openWith( { question, answer, sources } ) {
	if ( ! chat || ! question || ! answer ) {
		return;
	}
	// The stored conversation is emptied, so the new controller starts from these turns.
	chat.reset();
	unsubscribe?.();
	chat.destroy();
	clearTurSession();
	rated.clear();
	const now = Date.now();
	const reply = `seed-${ now }-a`;
	seeded.add( reply );
	start( [
		{
			id: `seed-${ now }-q`,
			role: 'user',
			content: question,
			timestamp: now,
		},
		{
			id: reply,
			role: 'assistant',
			content: answer,
			sources: Array.isArray( sources ) ? sources : [],
			timestamp: now,
		},
	] );
	sync( chat.getState() );
	const dialog = document.querySelector(
		'.viglet-turing-assistant:not([hidden]) dialog'
	);
	if ( dialog && ! dialog.open ) {
		dialog.showModal();
	}
	dialog?.querySelector( 'input' )?.focus();
}
window.addEventListener( 'viglet-turing:assistant-open', ( event ) =>
	openWith( event.detail || {} )
);

/**
 * The dialog of the block an event came from.
 */
function dialogOf() {
	return getElement()
		.ref.closest( '.viglet-turing-assistant' )
		.querySelector( 'dialog' );
}

const { state } = store( 'viglet-turing/assistant', {
	state: {
		get suggesting() {
			return (
				! state.messages.length && !! getContext().suggestions.length
			);
		},
	},
	actions: {
		open() {
			const dialog = dialogOf();
			dialog.showModal();
			dialog.querySelector( 'input' ).focus();
		},
		close() {
			dialogOf().close();
		},
		// A click on the backdrop, outside the panel, closes it as Escape does.
		backdrop( event ) {
			if ( event.target === event.currentTarget ) {
				event.currentTarget.close();
			}
		},
		type( event ) {
			state.question = event.target.value;
		},
		ask( event ) {
			event.preventDefault();
			const question = state.question.trim();
			if ( ! question || state.streaming ) {
				return;
			}
			state.question = '';
			void chat.send( question, turn() );
		},
		suggest() {
			void chat.send( getContext().suggestion.text, turn() );
		},
		restart() {
			chat.reset();
			// The next question opens a conversation of its own on Turing's side too.
			clearTurSession();
			rated.clear();
			dialogOf().querySelector( 'input' ).focus();
		},
		helpful() {
			rate( 'THUMBS_UP' );
		},
		notHelpful() {
			rate( 'THUMBS_DOWN' );
		},
	},
	callbacks: {
		// A message's Markdown as lists and emphasis (D247), from text nodes, never HTML.
		renderMessage() {
			renderMarkdown( getElement().ref, getContext().message.text );
		},
		init() {
			// One conversation for every assistant on the page, kept for this tab.
			if ( ! chat ) {
				start();
			}
			sync( chat.getState() );
		},
	},
} );
