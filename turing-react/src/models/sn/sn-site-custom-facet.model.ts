export interface TurSNSiteCustomFacetItem {
  id?: string;
  label: string;
  position?: number;
  rangeStart?: number | null;
  rangeEnd?: number | null;
}

export interface TurSNSiteCustomFacet {
  id?: string;
  name: string;
  defaultLabel?: string;
  label: Record<string, string>;
  facetPosition?: number;
  items: TurSNSiteCustomFacetItem[];
  fieldExtId: string;
  fieldExtName?: string;
}

export interface TurSNSiteCustomFacetFieldOption {
  id: string;
  name: string;
}
