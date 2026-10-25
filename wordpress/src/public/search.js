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
	createAutoComplete,
	createSearchController,
	createTuringClient,
} from '@viglet/turing-sdk';
import { getConfig } from '@wordpress/interactivity';

export * from '@viglet/turing-sdk';

/** viglet_dumont_public_config(): { url, site, locale, siteKey, facets }. */
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
