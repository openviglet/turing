import type { TurIntegrationIndexing } from "./integration-indexing.model";

export type TurIntegrationMonitoring = {
  sources: string[];
  indexing: TurIntegrationIndexing[];
};
