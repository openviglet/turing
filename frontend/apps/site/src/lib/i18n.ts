/**
 * The site's languages, and the words of the chrome every page shares (navigation,
 * footer, the hero's way back, the cookie banner) in each of them.
 *
 * English is the site's language, at the root. Brazilian Portuguese lives under
 * /br/ with the same paths below it (T1376): /br/, /br/features/search/. The
 * locale is read from the path, by `splitLocale()`, on the server and on the
 * client alike, and reaches the chrome through `LocaleProvider` (lib/locale.tsx),
 * so a visitor reads one language from the header to the cookie banner. A page's
 * own copy lives with the page. The prerender sets `<html lang>` from the route's
 * `lang`.
 */
export type Locale = "en" | "pt-BR";

/** Where the Portuguese tree starts: the owner chose /br/ over /pt-br/ (T1376). */
export const PT_BR_PREFIX = "/br";

/**
 * The routes with a Portuguese twin under /br/ (T1376), as their English path. A page
 * joins once its copy is translated (T1381, T1382), so /br/ never serves an English body.
 */
export const BR_ROUTES: readonly string[] = [];

/**
 * The routes written in Brazilian Portuguese outside /br/, from before it existed;
 * T1383 moves them under it and keeps their address answering.
 */
export const PT_BR_PATHS = ["/educacao/"] as const;

/** A path with its trailing slash, the form the route lists use. */
function slashed(path: string): string {
  return path.endsWith("/") ? path : `${path}/`;
}

/**
 * The language a path is in and the route below its prefix (T1376): "/br/compare/"
 * is pt-BR's "/compare/", "/br" is pt-BR's "/", and any other path is itself, in
 * English unless it is one of PT_BR_PATHS. The one rule the prerender and the client
 * read.
 */
export function splitLocale(path: string): { locale: Locale; path: string } {
  if (path === PT_BR_PREFIX || path.startsWith(`${PT_BR_PREFIX}/`)) {
    return { locale: "pt-BR", path: path.slice(PT_BR_PREFIX.length) || "/" };
  }
  const legacy = (PT_BR_PATHS as readonly string[]).includes(slashed(path.split("#")[0]));
  return { locale: legacy ? "pt-BR" : "en", path };
}

/** The language of a path ("/educacao/" or "/br/compare/" → "pt-BR"). */
export function localeOf(path: string): Locale {
  return splitLocale(path).locale;
}

/**
 * A site path as a page in `locale` links to it (T1376): under /br/ when the visitor
 * reads Portuguese and the route has a twin there, else as it is. An anchor
 * ("/#features") follows its page.
 */
export function localePath(path: string, locale: Locale, twins: readonly string[] = BR_ROUTES): string {
  if (locale !== "pt-BR" || !path.startsWith("/")) return path;
  const [route] = path.split("#");
  return twins.includes(slashed(route)) ? `${PT_BR_PREFIX}${path}` : path;
}
