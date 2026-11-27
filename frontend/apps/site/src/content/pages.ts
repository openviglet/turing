/**
 * T1377: the copy of the site's pages, by locale.
 *
 * English (content/en/pages.ts) is complete and is the type: a locale's pages are
 * `Partial<PagesContent>`, one key per piece of copy, each typed as the English one, so
 * a translated piece cannot drop a field. A locale gains its pieces with its pages
 * (T1381, T1382); until a piece is translated the English one stands, and the build
 * gate of T1386 refuses a /br/ route whose pieces are not all there.
 *
 * Routing and the prerender's English meta read content/en/pages.ts directly: a slug
 * or a route is the same in every language.
 */
import type { Locale } from "@/lib/i18n";
import * as en from "./en/pages";

export type PagesContent = typeof en;

export const PAGES: Readonly<{ en: PagesContent } & Partial<Record<Exclude<Locale, "en">, Partial<PagesContent>>>> = {
  en,
};

/** The pages' copy in `locale`: its own pieces, and English for any it has not yet. */
export function pagesOf(locale: Locale): PagesContent {
  return locale === "en" ? PAGES.en : { ...PAGES.en, ...PAGES[locale] };
}
