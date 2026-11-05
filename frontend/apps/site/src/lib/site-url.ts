/**
 * Where the site is served from.
 *
 * At the root of its custom domain (https://turing.viglet.org/). Root-absolute
 * links still go through withBase() so the site keeps working if it is ever
 * served under a project path again: the prefix is Vite's `base`
 * (vite.config.ts), read back here as BASE_URL, so the client, the SSR prerender
 * and the dev server all agree on it.
 */

/** The public origin; the canonical, og:url and JSON-LD URLs hang off it. */
export const SITE_ORIGIN = "https://turing.viglet.org";

/** Vite's `base`, always with a trailing slash ("/" today). */
export const BASE_PATH = import.meta.env.BASE_URL;

/** Prefix a site path ("/compare/", "/#playground") with the base path. */
export function withBase(path: string): string {
  return BASE_PATH + path.replace(/^\//, "");
}

/** The absolute public URL of a site path, for canonical and structured data. */
export function siteUrl(path: string): string {
  return SITE_ORIGIN + withBase(path);
}

/** Strip the base path off a location pathname, giving the app's route ("/compare/"). */
export function stripBase(pathname: string): string {
  const base = BASE_PATH.replace(/\/$/, "");
  return pathname.startsWith(base) ? pathname.slice(base.length) || "/" : pathname;
}
