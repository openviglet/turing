import { Type } from "class-transformer";
import { TurSNURLImpl } from "../impl/TurSNUrlImpl";
/**
 * Locale information for Turing Semantic Navigation Site
 */
export class TurSNSiteLocale {
  locale?: string;
  @Type(() => TurSNURLImpl)
  link?: string;
}
