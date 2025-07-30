import type {TurSNSiteMergeField} from "./sn-site-merge-field.model";
import type {TurSNSite} from "./sn-site.model";

export type TurSNSiteMerge = {
  id: string;
  turSNSite: TurSNSite;
  locale: string;
  providerFrom: string;
  providerTo: string;
  relationFrom: string;
  relationTo: string;
  overwrittenFields: TurSNSiteMergeField[];
  description: string;
}
