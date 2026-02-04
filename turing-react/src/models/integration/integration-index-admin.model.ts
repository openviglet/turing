export interface TurIntegrationIndexAdmin {
  attribute: "ID" | "URL";
  paths: string[];
  event: "PUBLISHING" | "UNPUBLISHING";
  recursive?: boolean;
}
