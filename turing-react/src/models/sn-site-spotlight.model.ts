import type {TurSNSiteSpotlightDocument} from "./sn-site-spotlight-document.model";
import type {TurSNSiteSpotlightTerm} from "./sn-site-spotlight-term.model";
import type {TurSNSite} from "./sn-site.model";

export type TurSNSiteSpotlight = {
  id: string;
  name: string;
  description: string;
  language: string;
  modificationDate: Date;
  turSNSiteSpotlightTerms: TurSNSiteSpotlightTerm[];
  turSNSiteSpotlightDocuments: TurSNSiteSpotlightDocument[];
  turSNSite: TurSNSite;
}
