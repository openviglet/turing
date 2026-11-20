import type { TurSEInstance } from "../se/se-instance.model.ts";
import type { TurSNSiteFacetFieldSortTypes } from "./sn-site-facet-field-sort.type.ts";
import type { TurSNSiteFacetTypes } from "./sn-site-facet.type.ts";
import type { TurSNSiteGenAi } from "./sn-site-genai.model.ts";
import type { TurSNSiteLocale } from "./sn-site-locale.model.ts";

export type TurSNSite = {
  id: string;
  /** T1319 — optimistic-lock version; a save from an older one is refused with 409. */
  version?: number;
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
  /** T1176 — 1 retries a search that found nothing with the query's main words. */
  relaxNoResults?: number;
  /** T1246 — 1 adds the most searched terms (that found results) to autocomplete. */
  autocompleteTopTerms?: number;
  /** T1329 — 1 masks personal data in stored search terms, 0 keeps it, null follows the deployment. */
  metricsMaskPersonalData?: number | null;
  /** T1330 — IANA time zone that "today" and "tomorrow" in sentence filters mean; null is the server's. */
  timeZone?: string | null;
  exactMatch: number;
  turSNSiteGenAi: TurSNSiteGenAi;
  icon?: string | null;
  searchTemplate?: string | null;
  /**
   * T233 / §VII.6.h — whether the visitor-facing API (search / autocomplete /
   * query / click / chat / spell-check) is open or requires a Turing-registered
   * API key (Dev Token). Defaults to {@code "PUBLIC"} server-side.
   */
  apiAuthMode?: TurSNSiteApiAuthMode;
};

/** T233 / §VII.6.h — public vs API-key access for the visitor-facing API. */
export type TurSNSiteApiAuthMode = "PUBLIC" | "API_KEY";
