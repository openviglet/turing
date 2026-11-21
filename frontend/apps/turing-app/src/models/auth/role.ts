export type TurPrivilege = {
  id: string;
  name: string;
  description: string;
  category: string;
};

export type TurRole = {
  /** T1319 — optimistic-lock version; a save from an older one is refused with 409. */
  version?: number;
  id: string;
  name: string;
  description: string;
  turPrivileges: TurPrivilege[];
};
