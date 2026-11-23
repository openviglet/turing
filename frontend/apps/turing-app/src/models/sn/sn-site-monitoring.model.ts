export type TurSNSiteStatus = {
  queue: number;
  documents: number;
  /** False when the search engine was unreachable (e.g. Solr down or circuit breaker open). `documents` is then best-effort. */
  searchEngineAvailable: boolean;
  /** T1343 — field values the engine dropped from this site's documents in the last 24 hours. */
  droppedValues?: number;
  /** T1343 — documents of this site the engine refused in the last 24 hours. */
  refusedDocuments?: number;
};
