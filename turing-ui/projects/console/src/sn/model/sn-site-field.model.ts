import {TurSNSiteFieldFacet} from "./sn-site-field-facet.model";
import {TurSNSiteFacetRangeEnum} from "./sn-site-facet-range.enum";
import {TurSNSiteFacetFieldEnum} from "./sn-site-facet.field.enum";

export interface TurSNSiteField {
  id: string;
  name: string;
  description: string;
  defaultValue: string;
  enabled: number;
  externalId: string;
  facet: number;
  facetName: string;
  facetRange: TurSNSiteFacetRangeEnum;
  facetLocales: TurSNSiteFieldFacet[]
  facetType: TurSNSiteFacetFieldEnum;
  mlt: number;
  multiValued: number;
  nlp: number;
  required: number;
  snType: string;
  type: string;
  hl: number;
}
