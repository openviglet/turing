/**
 * The site's languages, and the words of the chrome every page shares (navigation,
 * footer, the hero's way back, the cookie banner) in each of them.
 *
 * English is the site's language, at the root. Brazilian Portuguese lives under
 * /br/ with the same paths below it (T1376): /br/, /br/features/search/. The
 * locale is read from the path, by `splitLocale()`, on the server and on the
 * client alike, and reaches the chrome through `LocaleProvider` (lib/locale.tsx),
 * so a visitor reads one language from the header to the cookie banner. A page's
 * own copy lives with the page. The prerender sets `<html lang>` from the route's
 * `lang`.
 */
export type Locale = "en" | "pt-BR";

/** Where the Portuguese tree starts: the owner chose /br/ over /pt-br/ (T1376). */
export const PT_BR_PREFIX = "/br";

/**
 * The routes with a Portuguese twin under /br/ (T1376), as their English path. A page
 * joins once its copy is translated (T1381, T1382), so /br/ never serves an English body.
 */
export const BR_ROUTES: readonly string[] = [];

/**
 * The routes written in Brazilian Portuguese outside /br/, from before it existed;
 * T1383 moves them under it and keeps their address answering.
 */
export const PT_BR_PATHS = ["/educacao/"] as const;

/** A path with its trailing slash, the form the route lists use. */
function slashed(path: string): string {
  return path.endsWith("/") ? path : `${path}/`;
}

/**
 * The language a path is in and the route below its prefix (T1376): "/br/compare/"
 * is pt-BR's "/compare/", "/br" is pt-BR's "/", and any other path is itself, in
 * English unless it is one of PT_BR_PATHS. The one rule the prerender and the client
 * read.
 */
export function splitLocale(path: string): { locale: Locale; path: string } {
  if (path === PT_BR_PREFIX || path.startsWith(`${PT_BR_PREFIX}/`)) {
    return { locale: "pt-BR", path: path.slice(PT_BR_PREFIX.length) || "/" };
  }
  const legacy = (PT_BR_PATHS as readonly string[]).includes(slashed(path.split("#")[0]));
  return { locale: legacy ? "pt-BR" : "en", path };
}

/** The language of a path ("/educacao/" or "/br/compare/" → "pt-BR"). */
export function localeOf(path: string): Locale {
  return splitLocale(path).locale;
}

/**
 * A site path as a page in `locale` links to it (T1376): under /br/ when the visitor
 * reads Portuguese and the route has a twin there, else as it is. An anchor
 * ("/#features") follows its page.
 */
export function localePath(path: string, locale: Locale, twins: readonly string[] = BR_ROUTES): string {
  if (locale !== "pt-BR" || !path.startsWith("/")) return path;
  const [route] = path.split("#");
  return twins.includes(slashed(route)) ? `${PT_BR_PREFIX}${path}` : path;
}

export type ChromeStrings = Readonly<{
  nav: Readonly<{
    explore: string;
    docs: string;
    aem: string;
    product: string;
    capabilities: string;
    personas: string;
    why: string;
    developers: string;
    marketplace: string;
    solutions: string;
    wordpress: string;
    education: string;
    security: string;
    compare: string;
    migrate: string;
    getStarted: string;
    portfolio: string;
    home: string;
    openMenu: string;
    closeMenu: string;
  }>;
  footer: Readonly<{
    tagline: string;
    product: string;
    developers: string;
    open: string;
    howItWorks: string;
    aemSearch: string;
    selfHost: string;
    docs: string;
    cookies: string;
    license: string;
  }>;
  hero: Readonly<{ back: string }>;
  consent: Readonly<{
    region: string;
    title: string;
    body: string;
    decline: string;
    declineAndClose: string;
    accept: string;
  }>;
}>;

export const CHROME: Record<Locale, ChromeStrings> = {
  en: {
    nav: {
      explore: "Try it live",
      docs: "Docs",
      aem: "AEM AI Search",
      product: "Product",
      capabilities: "Capabilities",
      personas: "AI Personas",
      why: "Why Viglet Turing",
      developers: "Developers",
      marketplace: "Marketplace",
      solutions: "Solutions",
      wordpress: "WordPress & WooCommerce",
      education: "Education (in Portuguese)",
      security: "Security & data residency",
      compare: "Compare",
      migrate: "Migrate",
      getStarted: "Get started",
      portfolio: "part of viglet.org",
      home: "Viglet Turing home",
      openMenu: "Open menu",
      closeMenu: "Close menu",
    },
    footer: {
      tagline:
        "Open-source Enterprise Search Intelligence Platform: agents, tools, skills, MCP and RAG for your content.",
      product: "Product",
      developers: "Developers",
      open: "Open & transparent",
      howItWorks: "How it works",
      aemSearch: "AEM search",
      selfHost: "Self-host",
      docs: "Docs",
      cookies: "Cookie settings",
      license: "Apache 2.0 License",
    },
    hero: { back: "Back to overview" },
    consent: {
      region: "Cookie consent",
      title: "We respect your privacy",
      body: "We use Google Analytics cookies only to understand how the site is used, never for ads. Nothing is loaded until you choose. You can change this anytime from the footer.",
      decline: "Decline",
      declineAndClose: "Decline and close",
      accept: "Accept analytics",
    },
  },
  "pt-BR": {
    nav: {
      explore: "Experimente ao vivo",
      docs: "Documentação",
      aem: "Busca com IA para AEM",
      product: "Produto",
      capabilities: "Recursos",
      personas: "Personas de IA",
      why: "Por que o Viglet Turing",
      developers: "Desenvolvedores",
      marketplace: "Marketplace",
      solutions: "Soluções",
      wordpress: "WordPress e WooCommerce",
      education: "Educação",
      security: "Segurança e residência de dados",
      compare: "Comparativo",
      migrate: "Migração",
      getStarted: "Começar",
      portfolio: "parte do viglet.org",
      home: "Página inicial do Viglet Turing",
      openMenu: "Abrir menu",
      closeMenu: "Fechar menu",
    },
    footer: {
      tagline:
        "Plataforma open source de busca corporativa com IA: agentes, ferramentas, skills, MCP e RAG sobre o seu conteúdo.",
      product: "Produto",
      developers: "Desenvolvedores",
      open: "Aberto e transparente",
      howItWorks: "Como funciona",
      aemSearch: "Busca para AEM",
      selfHost: "Instalar no seu servidor",
      docs: "Documentação",
      cookies: "Preferências de cookies",
      license: "Licença Apache 2.0",
    },
    hero: { back: "Voltar à visão geral" },
    consent: {
      region: "Consentimento de cookies",
      title: "Respeitamos a sua privacidade",
      body: "Usamos cookies do Google Analytics só para entender como o site é usado, nunca para anúncios. Nada é carregado antes da sua escolha. Você pode mudar isso quando quiser pelo rodapé.",
      decline: "Recusar",
      declineAndClose: "Recusar e fechar",
      accept: "Aceitar analytics",
    },
  },
};

/** The pt-BR labels of the shared destinations (lib/destinations.ts), by key. */
export const DESTINATION_LABELS_PT_BR: Readonly<Record<string, string>> = {
  explore: "Experimente ao vivo",
  getStarted: "Começar",
  docs: "Documentação",
  download: "Download",
  releaseNotes: "Notas de versão",
  source: "Código no GitHub",
  devRoadmap: "Roadmap de desenvolvimento",
  portfolio: "viglet.org",
  contact: "Fale com a gente",
  npm: "npm",
  cli: "CLI",
};
