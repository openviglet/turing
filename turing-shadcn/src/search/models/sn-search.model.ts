import type { TurSNSearchPaginationItem } from "./sn-search-pagination-item.model";
import type { TurSNSearchQueryContext } from "./sn-search-query-context.model";
import type { TurSNSearchResults } from "./sn-search-results.model";
import type { TurSNSearchWidget } from "./sn-search-widget.model";

export interface TurSNSearch {
  pagination: TurSNSearchPaginationItem[];
  queryContext: TurSNSearchQueryContext;
  results: TurSNSearchResults;
  widget: TurSNSearchWidget;
}
