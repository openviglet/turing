/**
 * T1377: the site's copy in English, the type every other locale fills.
 */
import { DESTINATIONS, type DestinationKey } from "@/lib/destinations";
import type { SiteContent } from "../types";

/** The destinations' own labels: lib/destinations.ts stays the English source, as the docs app reads it. */
const destinations = Object.fromEntries(
  Object.entries(DESTINATIONS).map(([key, d]) => [key, d.label])
) as Record<DestinationKey, string>;

export const en: SiteContent = {
  chrome: {
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
  destinations,
};
