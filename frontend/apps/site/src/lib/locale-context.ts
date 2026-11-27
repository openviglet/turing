/**
 * T1376: the page's language, for every component under it.
 *
 * App reads it from the path (`splitLocale()`, lib/i18n.ts) and provides it once
 * (`LocaleProvider`, lib/locale.tsx), so the chrome (nav, footer, hero, cookie banner)
 * speaks the page's language without each page passing it down, and a link it draws
 * stays in that language.
 */
import { createContext, useContext } from "react";
import type { Locale } from "./i18n";

export const LocaleContext = createContext<Locale>("en");

/** The language of the page being drawn; English outside a provider. */
export function useLocale(): Locale {
  return useContext(LocaleContext);
}
