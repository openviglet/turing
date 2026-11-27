/**
 * T1377: the copy of the page being drawn, in the language the LocaleProvider gives.
 */
import { useLocale } from "@/lib/locale-context";
import { pagesOf, type PagesContent } from "./pages";

/** The pages' copy, in the page's language (lib/locale-context.ts). */
export function usePages(): PagesContent {
  return pagesOf(useLocale());
}
