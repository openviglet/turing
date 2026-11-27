/**
 * T1377: the page's copy, in the language the LocaleProvider gives (T1376).
 */
import { useLocale } from "@/lib/locale-context";
import { CONTENT } from "./index";
import type { SiteContent } from "./types";

/** The dictionary of the page being drawn. */
export function useContent(): SiteContent {
  return CONTENT[useLocale()];
}
