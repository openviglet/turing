/**
 * `viglet-dumont/search`: the Turing JS SDK, configured for this WordPress site.
 *
 * A theme's own search blocks import this module instead of vendoring the SDK:
 *
 *     import { createSiteSearch } from 'viglet-dumont/search';
 *     const search = createSiteSearch();
 *     search.subscribe( ( state ) => render( state.documents ) );
 *     await search.searchQuery( 'workshop' );
 *
 * The configuration comes from viglet_dumont_public_config() (Turing's public URL, the SN
 * site, the locale and this site's key); it never holds the API key. Searches are filtered
 * to this site's key, so a SN site shared with other sites returns only this one's posts.
 */
import {
	TURING_ANALYTICS_EVENTS,
	createAutoComplete,
	createSearchController,
	createTuringAnalytics,
	createTuringClient,
	googleAnalyticsSink,
	postClick,
} from '@viglet/turing-sdk';
import { getConfig, store } from '@wordpress/interactivity';

export * from '@viglet/turing-sdk';

/** viglet_dumont_public_config(): { url, site, locale, siteKey, facets, analytics }. */
export const config = getConfig( 'viglet-dumont' );

/** The filter that keeps a search to this site's documents. */
export const siteFilter = `site_key:${ config.siteKey }`;

/**
 * An SDK client for this site's Turing.
 *
 * @param {Object} options Extra TuringClientConfig fields, e.g. headers.
 */
export function createClient( options = {} ) {
	return createTuringClient( { baseURL: `${ config.url }/api`, ...options } );
}

/**
 * A search controller over this site's documents, search-only by default (no AI answer).
 *
 * With `track`, the controller reports to the analytics bus (D188): the searches the visitor
 * settles on (never an as-you-type one, which runs in instant mode), and, when the site counts
 * clicks, a search left without opening a result. Only with the visitor's consent, and with
 * no session cookie.
 *
 * @param {Object}  options            Overrides.
 * @param {Object}  options.params     Initial SearchParams, merged over the site filter.
 * @param {Object}  options.controller SearchControllerOptions, e.g. { chat: true }.
 * @param {boolean} options.track      Report this controller's searches.
 */
export function createSiteSearch( {
	params = {},
	controller = {},
	track = false,
} = {} ) {
	return createSearchController(
		createClient(),
		{ site: config.site, locale: config.locale },
		{ fq: [ siteFilter ], ...params },
		{
			chat: false,
			...( track
				? {
						analytics: bus(),
						consent: trackingAllowed,
						sessionCookie: false,
						// A click disarms it through the controller, which happens only when
						// clicks are counted (trackResultClick()); otherwise every opened result
						// would read as a search left behind.
						abandonment: !! config.analytics?.clicks,
					}
				: {} ),
			...controller,
		}
	);
}

/**
 * An autocomplete controller for this site's SN site.
 *
 * @param {number} debounceMs Milliseconds to wait after the last keystroke.
 */
export function createSiteAutoComplete( debounceMs = 250 ) {
	return createAutoComplete(
		createClient(),
		{ site: config.site, locale: config.locale },
		debounceMs
	);
}

/**
 * Whether this visitor may be counted: the site turned reporting on, and the visitor allows
 * statistics. A consent plugin speaking the WP Consent API decides that whenever it is
 * present; without one, the site does (the `viglet_dumont_tracking_consent` filter). Asked
 * at every event, so a choice made on the page applies from the next one.
 */
export function trackingAllowed() {
	if ( ! config.analytics ) {
		return false;
	}
	if ( 'function' === typeof window.wp_has_consent ) {
		return !! window.wp_has_consent( 'statistics' );
	}
	return ! config.analytics.requireConsent;
}

let analytics = null;

// The last search trackSearch() reported, so the next one can be told apart as a refinement.
let reported = '';

/**
 * Report a search the visitor settled on, for a theme's own block that searches without a
 * tracked controller (createSiteSearch( { track: true } ) reports by itself, D188). Nothing
 * personal: the query and a count.
 *
 * @param {string} query   What was searched for; `*` (browse everything) is not reported.
 * @param {number} results How many results Turing found.
 */
export function trackSearch( query, results ) {
	const typed = query.trim();
	if ( ! typed || '*' === typed || ! trackingAllowed() ) {
		return;
	}
	const events = TURING_ANALYTICS_EVENTS;
	if ( reported && reported !== typed ) {
		bus().emit( events.searchRefined, { from: reported, to: typed } );
	}
	bus().emit( events.search, { query: typed, results } );
	if ( 0 === results ) {
		bus().emit( events.searchNoResults, { query: typed } );
	}
	reported = typed;
}

/**
 * The SDK's analytics bus, with Google Analytics as its sink when the site sends events
 * there: the turing_* events, and GA4's own `search` and `select_content` beside them so its
 * site-search reports fill (Turing T1130). The search controllers report through it
 * (createSiteSearch( { track: true } )), which report a search when its query changes and
 * treat a filter, a sort or a page on it as the same search (Turing T1155, D224).
 */
function bus() {
	if ( ! analytics ) {
		const { ga, measurementId } = config.analytics || {};
		analytics = createTuringAnalytics( {
			sinks: ga
				? [
						googleAnalyticsSink( {
							measurementId: measurementId || undefined,
							recommended: true,
						} ),
					]
				: [],
			context: { site: config.site },
		} );
	}
	return analytics;
}

/**
 * The WordPress post id a Turing document stands for: its id ends in it,
 * `<site key>:post:<id>`.
 *
 * @param {string} documentId Turing's id of the document.
 * @return {number} The post id, or 0 for a document the plugin did not index.
 */
export function postIdOf( documentId ) {
	const match = String( documentId || '' ).match( /:post:(\d+)$/ );
	return match ? Number( match[ 1 ] ) : 0;
}

/**
 * Put one of a WooCommerce product in the visitor's cart through the Store API (D173), on a
 * site running WooCommerce (`config.shop`). WooCommerce says why when it cannot (out of
 * stock, say); nothing throws.
 *
 * @param {number} productId The product's post id.
 * @return {Promise<{ok: boolean, message: string}>} Whether it was added, and WooCommerce's
 *                                                    reason when not.
 */
export async function addToCart( productId ) {
	if ( ! config.shop || ! productId ) {
		return { ok: false, message: '' };
	}
	try {
		const response = await window.fetch(
			`${ config.shop.storeApi }cart/add-item`,
			{
				method: 'POST',
				credentials: 'same-origin',
				headers: {
					'Content-Type': 'application/json',
					Nonce: config.shop.nonce,
				},
				body: JSON.stringify( { id: productId, quantity: 1 } ),
			}
		);
		const body = await response.json().catch( () => ( {} ) );
		return { ok: response.ok, message: String( body.message || '' ) };
	} catch {
		return { ok: false, message: '' };
	}
}

/**
 * An amount the Store API gives in minor units, formatted in the shop's currency.
 *
 * @param {number|string} minor    The amount, in minor units.
 * @param {Object}        currency The Store API's currency_code and currency_minor_unit.
 * @return {string} The amount as the page's language writes it.
 */
function shopMoney( minor, currency ) {
	return new Intl.NumberFormat( document.documentElement.lang || undefined, {
		style: 'currency',
		currency: currency.currency_code || config.shop.currency,
	} ).format(
		Number( minor ) / 10 ** Number( currency.currency_minor_unit ?? 2 )
	);
}

/**
 * The visitor's cart total as WooCommerce computes it (D201), never added up here: taxes,
 * coupons and shipping are the shop's to apply.
 *
 * @return {Promise<string>} The total, formatted; empty when there is no shop or no answer.
 */
export async function cartTotal() {
	if ( ! config.shop ) {
		return '';
	}
	try {
		const response = await window.fetch( `${ config.shop.storeApi }cart`, {
			credentials: 'same-origin',
		} );
		if ( ! response.ok ) {
			return '';
		}
		const totals = ( await response.json() ).totals || {};
		return shopMoney( totals.total_price, totals );
	} catch {
		return '';
	}
}

/**
 * A WooCommerce product as the shop sells it now, from the Store API: its price in the
 * shop's currency (and the regular one when on sale) and whether it can be bought.
 *
 * @param {number} productId The product's post id.
 * @return {Promise<{price: string, regular: string, buyable: boolean}|null>} Null when it
 *                                                                            is not a product.
 */
export async function shopProduct( productId ) {
	if ( ! config.shop || ! productId ) {
		return null;
	}
	try {
		const response = await window.fetch(
			`${ config.shop.storeApi }products/${ productId }`,
			{ credentials: 'same-origin' }
		);
		if ( ! response.ok ) {
			return null;
		}
		const product = await response.json();
		const prices = product.prices || {};
		const money = ( minor ) => shopMoney( minor, prices );
		return {
			price: money( prices.price ),
			regular:
				prices.regular_price && prices.regular_price !== prices.price
					? money( prices.regular_price )
					: '',
			buyable: !! product.is_purchasable && !! product.is_in_stock,
		};
	} catch {
		return null;
	}
}

// The products ticked for comparison (D200), for this tab only. One list for every card that
// offers Compare (search results, Related, the copilot: D212), so a choice made in one
// counts in the others; it lives in a store of its own so each block's getters follow it.
const compareKey = 'viglet-dumont-compare';
const compareMax = 4;
const compared = store( 'viglet-dumont/compare', {
	state: { ids: loadCompare() },
} ).state;

/**
 * The products this tab ticked for comparison; none where storage is blocked.
 *
 * @return {number[]} Product ids, in the order ticked.
 */
function loadCompare() {
	try {
		const kept = JSON.parse( window.sessionStorage.getItem( compareKey ) );
		return Array.isArray( kept )
			? kept.map( Number ).filter( Boolean ).slice( 0, compareMax )
			: [];
	} catch {
		return [];
	}
}

/**
 * Compare, for a card that shows a product (D200, D212): offered once a page holds the
 * compare block (`config.shop.comparePage`), at most four products at a time.
 */
export const compare = {
	/** @return {boolean} Whether cards offer Compare at all. */
	get available() {
		return !! config.shop?.comparePage;
	},
	/** @return {number[]} The ticked product ids, in the order ticked. */
	get ids() {
		return compared.ids;
	},
	/** @return {boolean} Whether two or more are ticked, so the compare link is worth showing. */
	get ready() {
		return compare.available && compared.ids.length >= 2;
	},
	/** @return {string} The compare page, asked for the ticked products. */
	get url() {
		const url = new URL(
			config.shop?.comparePage || '/',
			window.location.href
		);
		url.searchParams.set( 'compare', compared.ids.join( ',' ) );
		return url.href;
	},
	/**
	 * @param {number} productId The product's post id.
	 * @return {boolean} Whether it is ticked.
	 */
	has( productId ) {
		return compared.ids.includes( productId );
	},
	/**
	 * @param {number} productId The product's post id.
	 * @return {boolean} Whether it cannot be ticked, four others being ticked already.
	 */
	full( productId ) {
		return ! compare.has( productId ) && compared.ids.length >= compareMax;
	},
	/**
	 * Tick the product, or untick it, and keep the list for this tab.
	 *
	 * @param {number} productId The product's post id.
	 */
	toggle( productId ) {
		compared.ids = compare.has( productId )
			? compared.ids.filter( ( id ) => id !== productId )
			: [ ...compared.ids, productId ].slice( 0, compareMax );
		try {
			window.sessionStorage.setItem(
				compareKey,
				JSON.stringify( compared.ids )
			);
		} catch {
			// Storage blocked: the choice lasts as long as the page.
		}
	},
};

/**
 * Report the result a visitor opened: to Turing (`POST /api/sn/{site}/click`, which its
 * click-through rates read) and to the analytics bus. The SDK sends it with `keepalive`, so it
 * outlives the navigation the click starts (T1128). Opened from a search controller's results,
 * it goes through that controller, which also disarms the search's abandonment (D188).
 *
 * @param {Object} click            The click.
 * @param {string} click.documentId Turing's id of the document opened.
 * @param {number} click.position   Its rank in the results, from 1 across pages.
 * @param {string} click.term       The query that listed it.
 * @param {Object} controller       The search controller that listed it, if any.
 */
export function trackResultClick(
	{ documentId, position, term },
	controller = null
) {
	if ( ! documentId || ! trackingAllowed() ) {
		return;
	}
	if ( ! config.analytics.clicks ) {
		bus().emit( TURING_ANALYTICS_EVENTS.searchResultClick, {
			query: term,
			document_id: documentId,
			position,
		} );
		return;
	}
	if ( controller ) {
		controller.trackResultClick( documentId, position, term );
		return;
	}
	void postClick( createClient(), config.site, {
		term,
		documentId,
		position,
		locale: config.locale,
	} );
	bus().emit( TURING_ANALYTICS_EVENTS.searchResultClick, {
		query: term,
		document_id: documentId,
		position,
	} );
}
