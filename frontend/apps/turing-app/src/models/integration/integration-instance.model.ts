export type TurIntegrationInstance = {
  id: string;
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
