import type { TurIntegrationVendor } from "./integration-vendor.model";

export interface TurIntegrationInstance {
  id: string;
  title: string;
  description: string;
  url: string;
  turIntegrationVendor: TurIntegrationVendor;
  language: string;
  enabled: number;
  modelName: string;
  temperature: number;
  topK: number;
  topP: number;
  repeatPenalty: number;
  seed: number;
  numPredict: number;
  stop: string;
  responseFormat: string;
  supportedCapabilities: string;
  timeout: string;
  maxRetries: number;
}
