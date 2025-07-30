import type { TurTokenVendor } from "./token-vendor.model.ts";

export interface TurTokenInstance {
  id: string;
  title: string;
  description: string;
  url: string;
  turTokenVendor: TurTokenVendor;
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
