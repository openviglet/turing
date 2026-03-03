export interface TurSNSiteCustomFacetItem {
  id?: string;
  label: string;
  position?: number;
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
