import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { queryKeys } from './keys';

/** One AI feature of a site: on, or the reason it is off (`NONE` when on). */
export interface TurSNAiCapability {
  enabled: boolean;
  reason: string;
}

/** `GET /api/sn/{site}/ai/capabilities`. */
export interface TurSNAiCapabilities {
  site: string;
  chat: TurSNAiCapability;
  answer: TurSNAiCapability;
  nlFilters: TurSNAiCapability;
  copilot: TurSNAiCapability;
  similarByVector: TurSNAiCapability;
  summaries: TurSNAiCapability;
}

/** `similarByVector`'s reason for a RAG-ready site whose store holds none of its content. */
export const NOT_EMBEDDED = 'NOT_EMBEDDED';

/**
 * T1151 — the site's AI capabilities. The console reads `similarByVector` to tell
 * an admin that a site configured for RAG has no vectors yet and needs a reindex.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
export function useSnAiCapabilities(siteName: string | undefined) {
  return useQuery<TurSNAiCapabilities>({
    queryKey: siteName
      ? queryKeys.snAiCapabilities.bySite(siteName)
      : ['sn-ai-capabilities', 'pending'],
    queryFn: async () =>
      (await axios.get<TurSNAiCapabilities>(`/sn/${encodeURIComponent(siteName as string)}/ai/capabilities`)).data,
    enabled: Boolean(siteName),
    staleTime: 60_000,
  });
}
