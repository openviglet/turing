import { Type } from "class-transformer";
import { TurSNURL } from "./TurSNUrl";
/**
 * Default fields configuration for search results
 */
export class TurSNSiteSearchDefaultFields {
  title?: string;
  date?: string;
  description?: string;
  text?: string;
  image?: string;
  @Type(() => TurSNURL)
  url?: string;
}
