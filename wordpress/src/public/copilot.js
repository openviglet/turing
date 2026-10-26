/**
 * The Ask the catalog block in the browser (D173): a conversation with Turing's catalog
 * copilot (`POST /api/sn/{site}/copilot`), answered from this site's content with the items
 * it cites. On a WooCommerce shop each cited product shows its price from the Store API and
 * goes in the cart with a click; the copilot never puts anything there on its own.
 */
import { getContext, store } from '@wordpress/interactivity';
/* eslint-disable import/no-unresolved -- a script module id, resolved by WordPress's import map at runtime. */
import {
	addToCart,
	config,
	createClient,
	postIdOf,
	shopProduct,
} from 'viglet-dumont/search';
/* eslint-enable import/no-unresolved */

const client = createClient();
const site = encodeURIComponent( config.site );
// The conversation so far, as the copilot reads it: follow-ups refer back to it.
const messages = [];

const { state } = store( 'viglet-dumont/copilot', {
	actions: {
		type( event ) {
			state.question = event.target.value;
		},
		async ask( event ) {
			event.preventDefault();
			const question = state.question.trim();
			if ( ! question || state.busy ) {
				return;
			}
			state.busy = true;
			state.status = state.thinking;
			messages.push( { role: 'user', content: question } );
			let answer;
			try {
				answer = await client.post(
					`/sn/${ site }/copilot`,
					{ messages, locale: config.locale },
					{ csrf: false }
				);
			} catch {
				answer = null;
			}
			state.busy = false;
			if ( ! answer?.available ) {
				messages.pop();
				state.status = state.failed;
				return;
			}
			messages.push( {
				role: 'assistant',
				content: answer.answer || '',
			} );
			const items = ( answer.citations || [] ).map( ( citation ) => ( {
				key: `${ citation.rank }-${ citation.id }`,
				rank: citation.rank,
				title: citation.title || citation.url,
				url: citation.url,
				productId: postIdOf( citation.id ),
				price: '',
				regular: '',
				buyable: false,
				cartLabel: state.cartLabel.replace(
					'%s',
					citation.title || ''
				),
				cartStatus: '',
				adding: false,
			} ) );
			state.turns = [
				...state.turns,
				{
					key: String( state.turns.length ),
					question,
					answer:
						answer.answer || ( items.length ? '' : state.nothing ),
					items,
				},
			];
			state.question = '';
			state.status = '';
			// The shop's own price and stock, for each cited product; the rest stay links.
			const turn = state.turns[ state.turns.length - 1 ];
			await Promise.all(
				turn.items.map( async ( item ) => {
					const product = await shopProduct( item.productId );
					if ( product ) {
						Object.assign( item, product );
					}
				} )
			);
		},
		async addToCart() {
			const { item } = getContext();
			item.adding = true;
			item.cartStatus = '';
			const added = await addToCart( item.productId );
			item.cartStatus = added.ok
				? state.cartAdded
				: added.message || state.cartFailed;
			item.adding = false;
		},
	},
	callbacks: {
		async init() {
			try {
				const probe = await client.get(
					`/sn/${ site }/copilot/available`
				);
				state.available = !! probe?.available;
			} catch {
				state.available = false;
			}
		},
	},
} );
