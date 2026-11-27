/**
 * The site's languages, and the words of the chrome every page shares (navigation,
 * footer, the hero's way back, the cookie banner) in each of them.
 *
 * English is the site's language; a page written in Brazilian Portuguese passes
 * `locale="pt-BR"` to the chrome so a visitor reads one language from the header
 * to the cookie banner. A page's own copy lives with the page. The prerender sets
 * `<html lang>` from the route's `lang`, and `localeOf()` gives the same answer
 * from a path on the client.
 */
export type Locale = "en" | "pt-BR";

/** The routes written in Brazilian Portuguese. Every other route is English. */
export const PT_BR_PATHS = ["/educacao/"] as const;

/** The language of a route ("/educacao/" → "pt-BR"). */
export function localeOf(path: string): Locale {
  const normalized = path.endsWith("/") ? path : `${path}/`;
  return (PT_BR_PATHS as readonly string[]).includes(normalized) ? "pt-BR" : "en";
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
  npm: "npm",
  cli: "CLI",
};
