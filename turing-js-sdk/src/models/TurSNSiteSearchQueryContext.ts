import { TurSNSiteSearchQueryContextQuery } from './TurSNSiteSearchQueryContextQuery.js';
import { TurSNSiteSearchDefaultFields } from './TurSNSiteSearchDefaultFields.js';

/**
 * Query context information for search results
 */
export interface TurSNSiteSearchQueryContext {
  count?: number;
  index?: string;
  limit?: number;
  offset?: number;
  page?: number;
  pageCount?: number;
  pageEnd?: number;
  pageStart?: number;
  responseTime?: number;
  query?: TurSNSiteSearchQueryContextQuery;
  defaultFields?: TurSNSiteSearchDefaultFields;
  facetType?: string;
  facetItemType?: string;
}