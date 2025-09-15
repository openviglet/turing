import { Type } from "class-transformer";
import { TurSNPaginationType } from "../enums/TurSNPaginationType.js";
import { TurSNURLImpl } from "../impl/TurSNUrlImpl";
/**
 * Pagination information for search results
 */
export class TurSNSiteSearchPagination {
  type?: TurSNPaginationType;
  text?: string;
  @Type(() => TurSNURLImpl)
  href?: string;
  page?: number;
}
