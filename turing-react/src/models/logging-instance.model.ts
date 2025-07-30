import type { TurLoggingVendor } from "./logging-vendor.model";

export interface TurLoggingInstance {
  id: string;
  title: string;
  description: string;
  url: string;
  turLoggingVendor: TurLoggingVendor;
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
