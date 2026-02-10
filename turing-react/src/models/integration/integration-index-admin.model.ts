export interface TurIntegrationIndexAdmin {
  attribute: "ID" | "URL";
  paths: string[];
  event: "PUBLISHING" | "UNPUBLISHING" | "INDEXING" | "DEINDEXING";
  recursive?: boolean;
}
