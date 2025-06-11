import {TurIntegrationIndexing} from "./integration-indexing.model";

export interface TurIntegrationMonitoring {
  sources: string[];
  indexing: TurIntegrationIndexing[];
}
