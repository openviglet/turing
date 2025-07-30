import type { TurSEInstance } from "./se-instance.model";
import type { TurSNSiteFacetFieldSortTypes } from "./sn-site-facet--field-sort.type";
import type { TurSNSiteFacetTypes } from "./sn-site-facet.type";
import type { TurSNSiteGenAi } from "./sn-site-genai.model";
import type { TurSNSiteLocale } from "./sn-site-locale.model";

export type TurSNSite = {
 id: string;
  name: string;
  description: string;
  exactMatchField: string;
  defaultField: string;
  defaultTitleField: string;
  defaultDescriptionField: string;
  defaultTextField: string;
  defaultDateField: string;
  defaultImageField: string;
  defaultURLField: string;
  facet: number;
  itemsPerFacet: number;
  hl: number;
  hlPre: string;
  hlPost: string;
  mlt: number;
  thesaurus: number;
  turSEInstance: TurSEInstance;
  turSNSiteLocales: TurSNSiteLocale[];
  rowsPerPage: number;
  spellCheck: number;
  spellCheckFixes: number;
  spotlightWithResults: number;
  facetType: TurSNSiteFacetTypes;
  facetItemType: TurSNSiteFacetTypes;
  facetSort: TurSNSiteFacetFieldSortTypes;
  wildcardNoResults: number;
  wildcardAlways: number;
  exactMatch: number;
  turSNSiteGenAi: TurSNSiteGenAi;
}
