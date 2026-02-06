export interface TurIntegrationIndexAdmin {
  attribute: "ID" | "URL";
  paths: string[];
  event: "PUBLISHING" | "UNPUBLISHING" | "DEFAULT" | "CREATE" | "DELETE";
  recursive?: boolean;
}
