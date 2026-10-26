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
} from '@viglet/turing-sdk';
import { getConfig } from '@wordpress/interactivity';

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
 * @param {Object} options            Overrides.
 * @param {Object} options.params     Initial SearchParams, merged over the site filter.
 * @param {Object} options.controller SearchControllerOptions, e.g. { chat: true }.
 */
export function createSiteSearch( { params = {}, controller = {} } = {} ) {
	return createSearchController(
		createClient(),
		{ site: config.site, locale: config.locale },
		{ fq: [ siteFilter ], ...params },
		{ chat: false, ...controller }
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

/**
 * The SDK's analytics bus, with Google Analytics as its sink when the site sends events
 * there, under the SDK's turing_* names (not yet GA4's search and select_content, Turing
 * T1130). It is never handed to a search controller: the controller would set a 30-day
 * session cookie before consent is asked, and report every as-you-type search (Turing
 * T1129), so the blocks report through trackSearch() and trackResultClick() instead.
 */
function bus() {
	if ( ! analytics ) {
		const { ga, measurementId } = config.analytics || {};
		analytics = createTuringAnalytics( {
			sinks: ga
				? [
						googleAnalyticsSink( {
							measurementId: measurementId || undefined,
						} ),
					]
				: [],
			context: { site: config.site },
		} );
	}
	return analytics;
}

// The last search reported, so the next one can be told apart as a refinement.
let reported = '';

/**
 * Report a search the visitor settled on (submitted, picked, opened from a link), never the
 * searches typing runs on the way there. Nothing personal: the query and a count.
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
		const money = ( minor ) =>
			new Intl.NumberFormat( document.documentElement.lang || undefined, {
				style: 'currency',
				currency: prices.currency_code || config.shop.currency,
			} ).format(
				Number( minor ) /
					10 ** Number( prices.currency_minor_unit ?? 2 )
			);
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

/**
 * Report the result a visitor opened: to Turing (`POST /api/sn/{site}/click`, which its
 * click-through rates read) and to the analytics bus. Sent with `keepalive`, so it outlives
 * the navigation the click starts and never delays it; the SDK's postClick would first ask
 * for a CSRF token and could be cut off by the page unloading (Turing T1128).
 *
 * @param {Object} click            The click.
 * @param {string} click.documentId Turing's id of the document opened.
 * @param {number} click.position   Its rank in the results, from 1 across pages.
 * @param {string} click.term       The query that listed it.
 */
export function trackResultClick( { documentId, position, term } ) {
	if ( ! documentId || ! trackingAllowed() ) {
		return;
	}
	if ( config.analytics.clicks ) {
		createClient()
			.fetchRaw(
				`/sn/${ encodeURIComponent( config.site ) }/click`,
				{
					method: 'POST',
					keepalive: true,
					headers: { 'Content-Type': 'application/json' },
					body: JSON.stringify( {
						term,
						documentId,
						position,
						locale: config.locale,
					} ),
				},
				false
			)
			.catch( () => {
				// A lost click is not something the visitor needs to see.
			} );
	}
	bus().emit( TURING_ANALYTICS_EVENTS.searchResultClick, {
		query: term,
		document_id: documentId,
		position,
	} );
}
