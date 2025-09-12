import { TurSNFilterQueryOperator } from '../enums/TurSNFilterQueryOperator.js';

/**
 * Parameters for POST search requests
 */
export interface TurSNSitePostParams {
  userId?: string;
  populateMetrics?: boolean;
  sort?: string;
  query?: string;
  fq?: string[];
  fqAnd?: string[];
  fqOr?: string[];
  fqOperator?: TurSNFilterQueryOperator;
  fqItemOperator?: TurSNFilterQueryOperator;
  page?: number;
  rows?: number;
  group?: string;
  locale?: string;
  disableAutoComplete?: boolean;
  targetingRules?: string[];
  targetingRulesWithCondition?: { [key: string]: string[] };
  targetingRulesWithConditionAND?: { [key: string]: string[] };
  targetingRulesWithConditionOR?: { [key: string]: string[] };
}