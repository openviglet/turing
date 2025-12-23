import type { TurSNSearchFacet } from "./sn-search-facet.model";
import type { TurSNSearchLocale } from "./sn-search-locale.model";
import type { TurSNSearchSpellCheck } from "./sn-search-spell-check.model";

export interface TurSNSearchWidget {
  facet: TurSNSearchFacet[];
  facetToRemove: TurSNSearchFacet;
  similar: string;
  spellCheck: TurSNSearchSpellCheck;
  locales: TurSNSearchLocale[];
  cleanUpFacets: string;
}
