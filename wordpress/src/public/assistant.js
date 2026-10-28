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
	recordAnswerSignal,
} from 'viglet-dumont/search';
/* eslint-enable import/no-unresolved */

// Turing exempts /api/sn/** from CSRF, so the token round trip would be wasted.
const client = createClient( { getCsrfToken: () => null } );
let chat = null;
// The answers already rated, by message id.
const rated = new Set();

/**
 * A page of this site, or nothing: the assistant may take the visitor to a post, never away.
 *
 * @param {string} url Where the agent asked to go.
 * @return {string} The absolute URL, or '' for another site.
 */
function onThisSite( url ) {
	try {
		const target = new URL( String( url || '' ), window.location.href );
		return target.origin === window.location.origin ? target.href : '';
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
			error: 'only pages of this site can be opened',
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
	} ) );
	return ( cited.length ? cited : message.sources || [] )
		.filter( ( s ) => s.url && ! seen.has( s.url ) && seen.add( s.url ) )
		.map( ( s ) => ( { url: s.url, title: s.title || s.url } ) );
}

/**
 * The controller's state, as the template reads it.
 *
 * @param {Object} s ChatControllerState.
 */
function sync( s ) {
	state.available = true === s.enabled;
	state.streaming = s.isStreaming;
	state.status = s.isStreaming ? state.thinking : '';
	if ( 'error' === s.status ) {
		state.status = state.failed;
	}
	state.messages = s.messages.map( ( message, i ) => {
		const last = i === s.messages.length - 1;
		const answer = 'assistant' === message.role;
		const done =
			answer && ! ( last && s.isStreaming ) && !! message.content;
		return {
			id: message.id,
			visitor: ! answer,
			text: message.content,
			sources: answer ? sourcesOf( message ) : [],
			canRate: done && ! rated.has( message.id ),
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
 * The dialog of the block an event came from.
 */
function dialogOf() {
	return getElement()
		.ref.closest( '.viglet-dumont-assistant' )
		.querySelector( 'dialog' );
}

const { state } = store( 'viglet-dumont/assistant', {
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
			void chat.send( question );
		},
		suggest() {
			void chat.send( getContext().suggestion.text );
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
		init() {
			// One conversation for every assistant on the page, kept for this tab.
			if ( ! chat ) {
				chat = createChatController( client, {
					site: config.site,
					locale: config.locale,
					persist: 'viglet-dumont-assistant',
					clientTools: { navigate },
				} );
				chat.subscribe( sync );
			}
			sync( chat.getState() );
		},
	},
} );
