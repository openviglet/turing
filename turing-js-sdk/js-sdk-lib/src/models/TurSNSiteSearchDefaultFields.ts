import { Type } from "class-transformer";
import { TurSNURLImpl } from "../impl/TurSNUrlImpl";
/**
 * Default fields configuration for search results
 */
export class TurSNSiteSearchDefaultFields {
  title?: string;
  date?: string;
  description?: string;
  text?: string;
  image?: string;
  @Type(() => TurSNURLImpl)
  url?: string;
}
