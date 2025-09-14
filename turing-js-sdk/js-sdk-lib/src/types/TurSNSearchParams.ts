import { TurSNFilterQueryOperator } from '../enums/TurSNFilterQueryOperator.js';

/**
 * Search parameters for GET requests
 */
export interface TurSNSearchParams {
  q?: string;
  currentPage?: number;
  filterQueriesDefault?: string[];
  filterQueriesAnd?: string[];
  filterQueriesOr?: string[];
  fqOperator?: TurSNFilterQueryOperator;
  fqItemOperator?: TurSNFilterQueryOperator;
  sort?: string;
  rows?: number;
  group?: string;
  autoCorrectionDisabled?: number;
  localeRequest?: string;
}