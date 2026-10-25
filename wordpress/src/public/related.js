/**
 * The Related content block in the browser. Its links are drawn on the server
 * (blocks/related/render.php); this only reports which one a visitor opened, through the
 * same tracking as the search block (D176), so related links count in the click-through
 * rates. A left click, or a middle click opening it in a new tab.
 */
import { getContext, store } from '@wordpress/interactivity';
/* eslint-disable import/no-unresolved -- a script module id, resolved by WordPress's import map at runtime. */
import { trackResultClick } from 'viglet-dumont/search';
/* eslint-enable import/no-unresolved */

store( 'viglet-dumont/related', {
	actions: {
		open( event ) {
			if ( 'auxclick' === event.type && 1 !== event.button ) {
				return;
			}
			const { id, position } = getContext();
			trackResultClick( { documentId: id, position, term: '' } );
		},
	},
} );
