/**
 * T1379: what tells a search engine that an English page and its /br/ twin are one page
 * in two languages: each page's hreflang alternates, and the sitemap that lists them.
 *
 * Both are derived from the prerender's route list (entry-server.tsx), so a twin added to
 * BR_ROUTES appears in the page head and the sitemap at once, and neither is kept by hand.
 * A page and its twin share a key, its path below the /br/ prefix.
 */
import { splitLocale, type Locale } from "./i18n";
import { SITE_ORIGIN } from "./site-url";

export type Alternate = Readonly<{ hreflang: Locale | "x-default"; href: string }>;

type Listed = Readonly<{ path: string; lang: Locale }>;

/** The address a route is served at on the public origin. */
export function absolute(path: string): string {
  return `${SITE_ORIGIN}${path}`;
}

/**
 * The alternates of the page at `path` among `routes`: one per language the page is in,
 * itself included, then x-default, the English one where it exists, else the page itself.
 * The same list on every member of a group, which is what makes them reciprocal.
 */
export function alternatesOf(path: string, routes: readonly Listed[]): Alternate[] {
  const key = splitLocale(path).path;
  const group = routes.filter((r) => splitLocale(r.path).path === key);
  const english = group.find((r) => r.lang === "en");
  const fallback = english ?? group.find((r) => r.path === path);
  return [
    ...group.map((r) => ({ hreflang: r.lang, href: absolute(r.path) })),
    ...(fallback ? [{ hreflang: "x-default" as const, href: absolute(fallback.path) }] : []),
  ];
}

/** The `<link rel="alternate">` tags of a page's head. */
export function alternateLinks(path: string, routes: readonly Listed[]): string {
  return alternatesOf(path, routes)
    .map((a) => `<link rel="alternate" hreflang="${a.hreflang}" href="${a.href}" />`)
    .join("\n    ");
}

/** How much a crawler should weigh a page, by its kind. */
export function priorityOf(path: string): string {
  const { path: key } = splitLocale(path);
  if (key === "/") return "1.0";
  if (key.startsWith("/features/") || key === "/persona/") return "0.9";
  return "0.8";
}

/**
 * sitemap.xml for the prerendered routes and the other pages the site serves (`extra`,
 * built by other jobs), each route with its xhtml:link alternates.
 */
export function sitemapXml(
  routes: readonly Listed[],
  extra: readonly Readonly<{ path: string; priority: string }>[] = []
): string {
  const url = (path: string, priority: string, links: Alternate[]) =>
    [
      "  <url>",
      `    <loc>${absolute(path)}</loc>`,
      ...links.map((a) => `    <xhtml:link rel="alternate" hreflang="${a.hreflang}" href="${a.href}" />`),
      `    <priority>${priority}</priority>`,
      "  </url>",
    ].join("\n");
  return [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xhtml="http://www.w3.org/1999/xhtml">',
    ...routes.map((r) => url(r.path, priorityOf(r.path), alternatesOf(r.path, routes))),
    ...extra.map((e) => url(e.path, e.priority, [])),
    "</urlset>",
    "",
  ].join("\n");
}
