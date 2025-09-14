import { TurSNPaginationType } from '../enums/TurSNPaginationType.js';

/**
 * Pagination information for search results
 */
export interface TurSNSiteSearchPagination {
  type?: TurSNPaginationType;
  text?: string;
  href?: string;
  page?: number;
}