export type TurIntegrationInstance = {
  id: string;
  /** T1319 — optimistic-lock version; a save from an older one is refused with 409. */
  version?: number;
  title: string;
  description: string;
  endpoint: string;
  enabled: number;
  icon?: string | null;
  /** T1315 — the dev token sent as the `Key` header, by id. */
  apiTokenId?: string | null;
  /** T372 — null for the shared GLOBAL BYO-infra pool (read-only when tenancy is on). */
  tenantId?: string | null;
};
