import type { TurSNSiteFacetFieldTypes } from "./sn-site-facet.field.type";

export type TurSNSiteCustomFacetOperator =
  | "EQUAL"
  | "GREATER_THAN"
  | "GREATER_THAN_OR_EQUAL"
  | "LESS_THAN"
  | "LESS_THAN_OR_EQUAL"
  | "BETWEEN_INCLUSIVE"
  | "BETWEEN_EXCLUSIVE";

export interface TurSNSiteCustomFacetItem {
  id?: string;
  label: string;
  labels?: Record<string, string>;
  position?: number;
  operator?: TurSNSiteCustomFacetOperator;
  rangeStart?: number | null;
  rangeEnd?: number | null;
  rangeStartDate?: string | null;
  rangeEndDate?: string | null;
}

export interface TurSNSiteCustomFacet {
  id?: string;
  name: string;
  defaultLabel?: string;
  label: Record<string, string>;
  facetPosition?: number;
  facetType?: TurSNSiteFacetFieldTypes;
  facetItemType?: TurSNSiteFacetFieldTypes;
  /**
   * T1224 — the site field where a span ends (an event's end date). Set, a
   * bucket lists the documents whose span overlaps it; "" clears it on save.
   */
  endField?: string | null;
  items: TurSNSiteCustomFacetItem[];
  fieldExtId: string;
  fieldExtName?: string;
  fieldExtType?: string;
}

export interface TurSNSiteCustomFacetFieldOption {
  id: string;
  name: string;
  type?: string;
}
