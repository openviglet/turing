export type TurIntegrationIndexingRule = {
  id: string;
  name: string;
  description: string;
  ruleType: string;
  source: string;
  attribute: string;
  values: string[];
  lastModifiedDate: Date;
};
