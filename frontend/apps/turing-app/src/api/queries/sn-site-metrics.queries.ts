import { useQuery } from '@tanstack/react-query';
import type { TurSNSiteMetricsTerm } from '@/models/sn/sn-site-metrics-term.model';
import {
  TurSNSiteMetricsService,
  type TurSNClickThroughReport,
  type TurSNTopTermsPeriod,
} from '@/services/sn/sn.site.metrics.service';
import { queryKeys } from './keys';

const service = new TurSNSiteMetricsService();

/**
 * T1018 — a site's most-searched terms for one period.
 *
 * Disabled until a site is chosen, so the dashboard widget's first render (which
 * has no selection yet) asks for nothing rather than guarding by hand.
 */
export function useSnTopTerms(
  siteId: string | undefined,
  period: TurSNTopTermsPeriod,
  rows: number,
) {
  return useQuery<TurSNSiteMetricsTerm>({
    queryKey: siteId
      ? queryKeys.snSiteMetrics.topTerms(siteId, period, rows)
      : ['sn-site-metrics', 'pending'],
    queryFn: () => service.topTermsByPeriod(siteId as string, period, rows),
    enabled: Boolean(siteId),
  });
}

/** T1131 — a site's click-through rate per term for one period. */
export function useSnClickThrough(
  siteId: string | undefined,
  period: TurSNTopTermsPeriod,
  rows: number,
) {
  return useQuery<TurSNClickThroughReport>({
    queryKey: siteId
      ? queryKeys.snSiteMetrics.clicks(siteId, period, rows)
      : ['sn-site-metrics', 'pending'],
    queryFn: () => service.clickThrough(siteId as string, period, rows),
    enabled: Boolean(siteId),
  });
}
