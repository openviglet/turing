import { SiteNav } from "@/components/sections/site-nav";
import { Hero } from "@/components/sections/hero";
import { Playground } from "@/components/sections/playground";
import { Providers } from "@/components/sections/providers";
import { Pillars } from "@/components/sections/pillars";
import { Outcomes } from "@/components/sections/outcomes";
import { Audiences } from "@/components/sections/audiences";
import { NativeTools } from "@/components/sections/native-tools";
import { Comparison } from "@/components/sections/comparison";
import { HowItWorks } from "@/components/sections/how-it-works";
import { LiveDemo } from "@/components/sections/live-demo";
import { SelfHost } from "@/components/sections/self-host";
import { Cli } from "@/components/sections/cli";
import { Stats } from "@/components/sections/stats";
import { Marketplace } from "@/components/sections/marketplace";
import { Resources } from "@/components/sections/resources";
import { CallToAction } from "@/components/sections/cta";
import { SiteFooter } from "@/components/sections/site-footer";
import { LINKS } from "@/components/brand-links";
import { siteUrl } from "@/lib/site-url";

/**
 * Structured data for the front door: the Viglet organization, the site, the
 * product as a SoftwareApplication and its source repository. The subpages
 * (aem, compare, security, persona, feature pages) carry their own graphs; the
 * landing had none, so answer engines had to infer what Turing is from prose.
 */
function LandingJsonLd() {
  const graph = {
    "@context": "https://schema.org",
    "@graph": [
      {
        "@type": "Organization",
        "@id": `${LINKS.viglet}/#organization`,
        name: "Viglet",
        url: LINKS.viglet,
      },
      {
        "@type": "WebSite",
        name: "Viglet Turing",
        url: siteUrl("/"),
        publisher: { "@id": `${LINKS.viglet}/#organization` },
      },
      {
        "@type": "SoftwareApplication",
        "@id": `${siteUrl("/")}#software`,
        name: "Viglet Turing",
        applicationCategory: "BusinessApplication",
        applicationSubCategory: "Enterprise search, RAG and AI agents",
        operatingSystem: "Any (self-hosted, Docker, Java 21)",
        url: siteUrl("/"),
        description:
          "Open-source enterprise search platform: hybrid keyword + semantic search, cited RAG answers, AI agents, tools, skills and MCP over your own content. Self-hosted with any LLM (including local models) and Apache Solr, Elasticsearch or embedded Lucene. Indexes Adobe Experience Manager (AEM), WordPress, databases and files via the Dumont connectors.",
        featureList: [
          "Hybrid keyword + semantic search with reciprocal rank fusion",
          "Retrieval-augmented generation (RAG) chat with inline citations",
          "AI agents with tools, skills and Model Context Protocol (MCP)",
          "Faceted semantic navigation",
          "Adobe Experience Manager (AEM 6.5 and AEM as a Cloud Service) connector",
          "React SDK and zero-dependency vanilla-JS SDK",
          "Self-hosted: content and queries stay on your infrastructure",
        ],
        offers: { "@type": "Offer", price: "0", priceCurrency: "USD" },
        license: "https://www.apache.org/licenses/LICENSE-2.0",
        publisher: { "@id": `${LINKS.viglet}/#organization` },
      },
      {
        "@type": "SoftwareSourceCode",
        name: "Viglet Turing",
        codeRepository: "https://github.com/openviglet/turing",
        programmingLanguage: ["Java", "TypeScript"],
        license: "https://www.apache.org/licenses/LICENSE-2.0",
        targetProduct: { "@id": `${siteUrl("/")}#software` },
      },
    ],
  };
  return (
    <script
      type="application/ld+json"
      // Prerendered to static HTML; this is our own content, not user input.
      dangerouslySetInnerHTML={{ __html: JSON.stringify(graph).replace(/</g, "\\u003c") }}
    />
  );
}

/**
 * The single-page landing: the marketing front door.
 *
 * Section order is a deliberate conversion narrative, built around the live
 * playground as the centerpiece proof:
 * hook → SEE it work (live search+chat over one corpus) → the code behind it →
 * what it is → outcomes it drives → works with your stack → who it's for →
 * proof → how → why vs alternatives → run it yourself → advanced → dev workflow
 * → starting points → ecosystem → convert.
 */
export function LandingPage() {
  return (
    <div className="flex min-h-screen flex-col">
      <LandingJsonLd />
      <SiteNav />
      <main className="flex-1">
        <Hero />
        <Playground />
        <LiveDemo />
        <Pillars />
        <Outcomes />
        <Providers />
        <Audiences />
        <Stats />
        <HowItWorks />
        <Comparison />
        <SelfHost />
        <NativeTools teaser />
        <Cli />
        <Marketplace />
        <Resources />
        <CallToAction />
      </main>
      <SiteFooter />
    </div>
  );
}
