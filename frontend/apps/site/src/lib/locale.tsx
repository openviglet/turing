/**
 * T1376: gives every component below it the page's language (lib/locale-context.ts).
 */
import type { ReactNode } from "react";
import type { Locale } from "./i18n";
import { LocaleContext } from "./locale-context";

/** Gives every component below it the page's language. */
export function LocaleProvider({ locale, children }: Readonly<{ locale: Locale; children: ReactNode }>) {
  return <LocaleContext.Provider value={locale}>{children}</LocaleContext.Provider>;
}
