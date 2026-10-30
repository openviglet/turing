/**
 * The Ask the catalog block in the browser (D173): a conversation with Turing's catalog
 * copilot (`POST /api/sn/{site}/copilot`), answered from this site's content with the items
 * it cites. On a WooCommerce shop each cited product shows its price from the Store API and
 * goes in the cart with a click; the copilot never puts anything there on its own.
 *
 * Build my kit (D201) sends a described plan to `POST /api/sn/{site}/copilot/kit` instead:
 * Turing answers the needs the plan implies, each staged with products retrieval found, and
 * the guides that cover it. Whether a product can be bought is the shop's word (the Store
 * API), not the index's, and the whole kit goes in the cart in one click, minus whatever
 * the visitor unticked. The total is the cart's, from WooCommerce.
 */
import { getContext, store } from '@wordpress/interactivity';
/* eslint-disable import/no-unresolved -- a script module id, resolved by WordPress's import map at runtime. */
import {
	addToCart,
	cartTotal,
	compare,
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
// The field a product's stock is indexed under (the WooCommerce mapping's `stock_status`).
// Turing marks what reads as unavailable; the Store API is checked as well, because the
// field holds the shop's own words and those are translated.
const availabilityField = 'stock_status';
// The document type a journal post is indexed with, cited beside the kit as a guide.
const guideTypes = [ 'post' ];

/**
 * One staged product as the block shows it.
 *
 * @param {Object} product A KitProduct from Turing.
 * @param {number} stage   Its stage's position, for a key unique across stages.
 * @return {Object} The product's view state.
 */
function kitProduct( product, stage ) {
	return {
		key: `${ stage }-${ product.id }`,
		title: product.title || product.url,
		url: product.url,
		reason: product.reason || '',
		productId: postIdOf( product.id ),
		price: '',
		out: false === product.available,
		// Ticked once the shop has said which of its stage's products it can sell (D240).
		included: false,
		includeFor: state.includeFor.replace( '%s', product.title || '' ),
	};
}

/**
 * Tick the first product of each stage the shop can sell, and only it (D240): a stage is the
 * choice for one need, not a bundle, so the shopper swaps one in rather than unticking many.
 *
 * @param {Array<{products: Object[]}>} stages The kit's stages.
 */
function tickFirstSellable( stages ) {
	for ( const stage of stages ) {
		const first = stage.products.find(
			( product ) => ! product.out && product.productId
		);
		for ( const product of stage.products ) {
			product.included = product === first;
		}
	}
}

const { state } = store( 'viglet-dumont/copilot', {
	state: {
		get asking() {
			return 'kit' !== state.mode;
		},
		get planning() {
			return 'kit' === state.mode;
		},
		get formLabel() {
			return state.planning ? state.planLabel : state.askLabel;
		},
		get formButton() {
			return state.planning ? state.planButton : state.askButton;
		},
		get kitShown() {
			return state.planning && state.kit.shown;
		},
		// Only what can be bought and is still ticked goes in the cart.
		get kitChosen() {
			return state.kit.stages
				.flatMap( ( stage ) => stage.products )
				.filter(
					( product ) =>
						product.included && ! product.out && product.productId
				);
		},
		get kitCanAdd() {
			return !! config.shop && state.kitChosen.length > 0;
		},
		get kitAddText() {
			const count = state.kitChosen.length;
			return 1 === count
				? state.kitAddOne
				: state.kitAdd.replace( '%d', count );
		},
		// Compare on a cited product (D212), over the list the search cards use.
		get canCompare() {
			return compare.available;
		},
		get compared() {
			return compare.has( getContext().item.productId );
		},
		get compareFull() {
			return compare.full( getContext().item.productId );
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
		type( event ) {
			state.question = event.target.value;
		},
		setMode( event ) {
			state.mode = event.currentTarget.dataset.mode;
			state.status = '';
		},
		async ask( event ) {
			event.preventDefault();
			if ( state.planning ) {
				await buildKit();
				return;
			}
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
				compareFor: state.compareFor.replace(
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
		toggleCompare() {
			compare.toggle( getContext().item.productId );
		},
		toggleInclude( event ) {
			getContext().product.included = event.target.checked;
		},
		async addKit() {
			const chosen = state.kitChosen;
			if ( ! chosen.length || state.kitBusy ) {
				return;
			}
			state.kitBusy = true;
			state.kitStatus = '';
			// One at a time: the Store API's cart is one session, and parallel adds race on it.
			let added = 0;
			for ( const product of chosen ) {
				if ( ( await addToCart( product.productId ) ).ok ) {
					added++;
				}
			}
			state.kitStatus =
				added === chosen.length
					? state.kitAdded
							.replace( '%1$d', added )
							.replace( '%2$s', await cartTotal() )
					: state.kitPartial
							.replace( '%1$d', added )
							.replace( '%2$d', chosen.length );
			state.kitBusy = false;
		},
	},
	callbacks: {
		// Turing is asked only after the probe said the copilot can answer, so without a
		// language model there is no kit to fake: the whole block stays hidden.
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

/**
 * Send the described plan to Turing's kit call and stage its answer.
 */
async function buildKit() {
	const plan = state.question.trim();
	if ( ! plan || state.busy ) {
		return;
	}
	state.busy = true;
	state.status = state.kitThinking;
	state.kitStatus = '';
	let kit;
	try {
		kit = await client.post(
			`/sn/${ site }/copilot/kit`,
			{
				plan,
				locale: config.locale,
				availabilityField,
				availability: 'MARK',
				guideTypes,
			},
			{ csrf: false }
		);
	} catch {
		kit = null;
	}
	state.busy = false;
	if ( ! kit || kit.error ) {
		state.status = state.failed;
		return;
	}
	const stages = ( kit.stages || [] ).map( ( stage, index ) => ( {
		key: `${ index }-${ stage.gap }`,
		gap: stage.gap,
		reason: stage.reason || '',
		products: ( stage.products || [] ).map( ( product ) =>
			kitProduct( product, index )
		),
	} ) );
	const unfilled = kit.unfilled || [];
	state.kit = {
		shown: true,
		summary: kit.summary || ( stages.length ? '' : state.kitNothing ),
		stages,
		unfilled: unfilled.length
			? state.kitUnfilled.replace( '%s', unfilled.join( ', ' ) )
			: '',
		guides: ( kit.guides || [] ).map( ( guide ) => ( {
			key: String( guide.id || guide.url ),
			title: guide.title || guide.url,
			url: guide.url,
		} ) ),
	};
	state.status = '';
	// The shop's price, and its word on stock: a product the index calls available but the
	// shop cannot sell is marked and left out of the cart.
	const notProducts = new Set();
	await Promise.all(
		state.kit.stages
			.flatMap( ( stage ) => stage.products )
			.map( async ( product ) => {
				const shop = await shopProduct( product.productId );
				if ( shop ) {
					product.price = shop.price;
					if ( ! shop.buyable ) {
						product.out = true;
					}
				} else if ( config.shop ) {
					notProducts.add( product.key );
				}
			} )
	);
	dropNotProducts( notProducts, unfilled );
	tickFirstSellable( state.kit.stages );
}

/**
 * Take out of the kit what the shop says is not a product (D241): Turing stages any indexed
 * document that is not a guide, a page among them, and the cart refuses one. A need left with
 * nothing joins the line naming what the catalog has nothing for.
 *
 * @param {Set<string>} keys     The staged products the Store API did not know.
 * @param {string[]}    unfilled The needs Turing already said the catalog has nothing for.
 */
function dropNotProducts( keys, unfilled ) {
	if ( ! keys.size ) {
		return;
	}
	const emptied = [];
	state.kit.stages = state.kit.stages.filter( ( stage ) => {
		stage.products = stage.products.filter(
			( product ) => ! keys.has( product.key )
		);
		if ( stage.products.length ) {
			return true;
		}
		emptied.push( stage.gap );
		return false;
	} );
	if ( emptied.length ) {
		state.kit.unfilled = state.kitUnfilled.replace(
			'%s',
			[ ...unfilled, ...emptied ].join( ', ' )
		);
	}
}
