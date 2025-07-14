import {TurIntegrationAemLocalePath} from "./integration-aem-locale-path.model";

export interface TurIntegrationAemSource {
  id: string;
  name: string;
  endpoint: string;
  username: string;
  password: string;
  rootPath: string;
  contentType: string;
  subType: string;
  oncePattern: string;
  defaultLocale: string;
  localeClass: string;
  deltaClass: string;
  author: boolean;
  publish: boolean;
  authorSNSite: string;
  publishSNSite: string;
  authorURLPrefix: string;
  publishURLPrefix: string;
  localePaths: TurIntegrationAemLocalePath[];
  attributeSpecifications: object;
  models: object;
}
