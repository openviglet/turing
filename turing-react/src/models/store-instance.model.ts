import type { TurStoreVendor } from "./store-vendor.model";

export interface TurStoreInstance {
  id: string;
  title: string;
  description: string;
  url: string;
  turStoreVendor: TurStoreVendor;
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
