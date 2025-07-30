import type { TurSEVendor } from "./se-vendor.model.ts";

export interface TurSEInstance {
  id: string;
  title: string;
  description: string;
  host: string;
  port: number;
  turSEVendor: TurSEVendor;
  language: string;
  enabled: number;
}
