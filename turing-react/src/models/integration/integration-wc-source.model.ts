import type { TurIntegrationWcAttrib } from "./integration-wc-attrib.model";
import type { TurIntegrationWcExtension } from "./integration-wc-extension.model";
import type { TurIntegrationWcUrl } from "./integration-wc-url.model";

export type TurIntegrationWcSource = {
  id: string;
  title: string;
  description: string;
  locale: string;
  localeClass: string;
  url: string;
  turSNSite: string;
  username: string;
  password: string;
  allowUrls: TurIntegrationWcUrl[];
  notAllowUrls: TurIntegrationWcUrl[];
  notAllowExtensions: TurIntegrationWcExtension[];
  attributeMappings: TurIntegrationWcAttrib[];
};
