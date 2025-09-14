import { TurSNSiteSearchPagination } from './TurSNSiteSearchPagination.js';
import { TurSNSiteSearchQueryContext } from './TurSNSiteSearchQueryContext.js';
import { TurSNSiteSearchResults } from './TurSNSiteSearchResults.js';
import { TurSNSiteSearchGroup } from './TurSNSiteSearchGroup.js';
import { TurSNSiteSearchWidget } from './TurSNSiteSearchWidget.js';

/**
 * Main search response from Turing Semantic Navigation
 */
export interface TurSNSiteSearch {
  pagination?: TurSNSiteSearchPagination[];
  queryContext?: TurSNSiteSearchQueryContext;
  results?: TurSNSiteSearchResults;
  groups?: TurSNSiteSearchGroup[];
  widget?: TurSNSiteSearchWidget;
}