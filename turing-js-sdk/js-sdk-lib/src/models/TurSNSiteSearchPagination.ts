import { Type } from "class-transformer";
import { TurSNPaginationType } from "../enums/TurSNPaginationType.js";
import { TurSNURL } from "./TurSNUrl.js";
/**
 * Pagination information for search results
 */
export class TurSNSiteSearchPagination {
  type?: TurSNPaginationType;
  text?: string;
  @Type(() => TurSNURL)
  href?: string;
  page?: number;
}
