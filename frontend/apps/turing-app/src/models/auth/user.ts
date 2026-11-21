import type { TurGroup } from "./group";

export type TurUser = {
  /** T1319 — optimistic-lock version; a save from an older one is refused with 409. */
  version?: number;
  username: string;
  firstName: string;
  lastName: string;
  admin: boolean;
  email: string;
  password?: string;
  hasAvatar?: boolean;
  avatarUrl?: string;
  realm?: string;
  turGroups?: TurGroup[];
  privileges?: string[];
};
