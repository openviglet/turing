/**
 * Where the site is served from.
 *
 * GitHub Pages serves it under a project path (https://openviglet.github.io/turing/),
 * not at a domain root, so every root-absolute link has to carry that prefix. The
 * prefix is Vite's `base` (vite.config.ts), read back here as BASE_URL, so the
 * client, the SSR prerender and the dev server all agree on it.
 */

/** The public origin; the canonical, og:url and JSON-LD URLs hang off it. */
export const SITE_ORIGIN = "https://openviglet.github.io";

/** Vite's `base`, always with a trailing slash ("/turing/"). */
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
