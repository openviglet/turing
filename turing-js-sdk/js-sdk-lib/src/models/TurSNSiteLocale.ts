import { Type } from "class-transformer";
import { TurSNURL } from "./TurSNUrl";
/**
 * Locale information for Turing Semantic Navigation Site
 */
export class TurSNSiteLocale {
  locale?: string;
  @Type(() => TurSNURL)
  link?: string;
}
