/**
 * T1377: the site's copy in Brazilian Portuguese. Declared `SiteContent`, so a key the
 * English dictionary has and this one lacks is a compile error, not an English
 * sentence on a /br/ page. Links into the documentation say "(em inglês)": the docs
 * stay English.
 */
import type { SiteContent } from "../types";

export const ptBR: SiteContent = {
  chrome: {
    nav: {
      explore: "Experimente ao vivo",
      docs: "Documentação (em inglês)",
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
      docs: "Documentação (em inglês)",
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
  destinations: {
    explore: "Experimente ao vivo",
    getStarted: "Começar (em inglês)",
    docs: "Documentação (em inglês)",
    download: "Download",
    releaseNotes: "Notas de versão",
    source: "Código no GitHub",
    devRoadmap: "Roadmap de desenvolvimento",
    portfolio: "viglet.org",
    contact: "Fale com a gente",
    dumont: "Viglet Dumont",
    aemConnector: "Documentação do conector AEM (em inglês)",
    wordpressDocs: "Documentação do plugin WordPress (em inglês)",
    wordpressDemo: "Experimente a loja demo",
    storybook: "React SDK",
    npm: "npm",
    cli: "CLI",
  },
};
