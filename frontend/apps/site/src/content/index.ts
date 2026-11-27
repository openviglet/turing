/**
 * T1377: every locale's dictionary, by locale. A locale missing here, or a dictionary
 * missing a key, does not compile.
 */
import type { Locale } from "@/lib/i18n";
import type { SiteContent } from "./types";
import { en } from "./en";
import { ptBR } from "./pt-BR";

export type { ChromeStrings, SiteContent } from "./types";

export const CONTENT: Readonly<Record<Locale, SiteContent>> = { en, "pt-BR": ptBR };
