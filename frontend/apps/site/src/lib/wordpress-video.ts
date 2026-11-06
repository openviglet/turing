import { WORDPRESS_VIDEOS, type WordPressVideoLang } from "@/lib/site-content";

/**
 * T1251 — which cut of the WordPress plugin video a visitor gets.
 *
 * The cuts that are published, in the order the toggle lists them.
 */
export const PUBLISHED_VIDEO_LANGS = (Object.keys(WORDPRESS_VIDEOS) as WordPressVideoLang[]).filter(
  (lang) => WORDPRESS_VIDEOS[lang].id,
);

/** The cut for a browser language: pt-BR for Portuguese, English otherwise, while it exists. */
export function videoLangFor(language: string | undefined): WordPressVideoLang {
  const wanted: WordPressVideoLang = language?.toLowerCase().startsWith("pt") ? "pt-BR" : "en";
  return PUBLISHED_VIDEO_LANGS.includes(wanted) ? wanted : PUBLISHED_VIDEO_LANGS[0];
}
