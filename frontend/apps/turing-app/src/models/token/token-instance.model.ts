export type TurTokenInstance = {
  id: string;
  token: string;
  title: string;
  description: string;
  /** T1133 — the field that partitions a shared SN site; with `partitionValue`, binds the key to it. */
  partitionField?: string | null;
  /** T1133 — the value this key's documents carry in `partitionField`. */
  partitionValue?: string | null;
};
