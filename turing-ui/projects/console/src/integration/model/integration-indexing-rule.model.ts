import {TurIntegrationIndexingCondition} from "./integration-indexing-condition.model";

export interface TurIntegrationIndexingRule {
  id: string;
  name: string;
  weight: number;
  conditions: TurIntegrationIndexingCondition[];
  lastModifiedDate: Date;
  description: string;
}
