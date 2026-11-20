import type { TurStoreVendor } from "./store-vendor.model.ts";

export interface TurStoreInstance {
  id: string;
  /** T1319 — optimistic-lock version; a save from an older one is refused with 409. */
  version?: number;
  title: string;
  description: string;
  url: string;
  turStoreVendor: TurStoreVendor;
  enabled: number;
  collectionName?: string;
  credential?: string;
  providerOptionsJson?: string;
  icon?: string | null;
  /** T372 — null for the shared GLOBAL BYO-infra pool (read-only when tenancy is on). */
  tenantId?: string | null;
}
