/**
 * The showcase's notice that the AI features are off (D198): Turing's chat probe, the one the
 * Site assistant reads, decides it. The notice's words are the page's own (demo/setup.php).
 */
/* eslint-disable import/no-unresolved -- a script module id, resolved by WordPress's import map at runtime. */
import { config, createClient, fetchChatEnabled } from 'viglet-dumont/search';
/* eslint-enable import/no-unresolved */

const notice = document.querySelector( '.tarnfell-ai-off' );
if ( notice ) {
	fetchChatEnabled( createClient(), config.site )
		.then( ( probe ) => ! probe?.enabled )
		.catch( () => true )
		.then( ( off ) => notice.classList.toggle( 'is-shown', off ) );
}
