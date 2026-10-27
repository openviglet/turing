/**
 * The Related content block in the browser. Its links are drawn on the server
 * (blocks/related/render.php); this reports which one a visitor opened, through the same
 * tracking as the search block (D176), so related links count in the click-through rates
 * (a left click, or a middle click opening it in a new tab), and ticks a product card for
 * comparison into the list every block shares (D212).
 */
import { getContext, store } from '@wordpress/interactivity';
/* eslint-disable import/no-unresolved -- a script module id, resolved by WordPress's import map at runtime. */
import { compare, trackResultClick } from 'viglet-dumont/search';
/* eslint-enable import/no-unresolved */

const { state } = store( 'viglet-dumont/related', {
	state: {
		get canCompare() {
			return compare.available;
		},
		get compared() {
			return compare.has( getContext().productId );
		},
		get compareFull() {
			return compare.full( getContext().productId );
		},
		get compareReady() {
			return compare.ready;
		},
		get compareUrl() {
			return compare.url;
		},
		get compareText() {
			return state.compareLabel.replace( '%d', compare.ids.length );
		},
	},
	actions: {
		open( event ) {
			if ( 'auxclick' === event.type && 1 !== event.button ) {
				return;
			}
			const { id, position } = getContext();
			trackResultClick( { documentId: id, position, term: '' } );
		},
		toggleCompare() {
			compare.toggle( getContext().productId );
		},
	},
} );
