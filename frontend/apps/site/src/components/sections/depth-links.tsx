import { ArrowRight } from "lucide-react";
import { Container, SectionHeader } from "@/components/brand";
import { LINKS } from "@/components/brand-links";
import { MARKETPLACE } from "@/lib/site-content";
import { withBase } from "@/lib/site-url";

/**
 * T1271: where the home's depth went. These sections used to be full blocks on
 * the home (the agent loop, the SDK code, native tools, the CLI, the example
 * apps); each is now one short summary that links to the page that owns the
 * detail. Each card keeps its old anchor id, so a link to /#how, /#developers,
 * /#native-tools, /#cli or /#marketplace still lands on its subject.
 */
const CARDS: {
  id: string;
  title: string;
  body: string;
  links: { label: string; href: string }[];
}[] = [
  {
    id: "how",
    title: "How it answers a question",
    body: "Retrieve from your sites, rerank, answer with inline citations, and refuse when the sources do not support an answer.",
    links: [{ label: "RAG, step by step", href: `${LINKS.docs}rag/` }],
  },
  {
    id: "developers",
    title: "Build it into your front end",
    body: "The playground above runs on @viglet/turing-sdk, the zero-dependency JavaScript SDK. React hooks and components are in @viglet/turing-react-sdk.",
    links: [
      { label: "JavaScript SDK", href: `${LINKS.docs}javascript-sdk/` },
      { label: "React SDK", href: LINKS.storybook },
    ],
  },
  {
    id: "native-tools",
    title: "Agents, tools, skills and MCP",
    body: "Agents call your tools, run skills in a sandbox, use the tools your model provider ships, and speak the Model Context Protocol.",
    links: [{ label: "Automate it", href: withBase("/features/automate/") }],
  },
  {
    id: "cli",
    title: "Ship agents from your terminal",
    body: "The turing CLI scaffolds a project, runs a local stack, deploys agents, flows and tools, and runs eval suites in CI.",
    links: [{ label: "CLI reference", href: `${LINKS.docs}cli/` }],
  },
  {
    id: "marketplace",
    title: "Start from a working example",
    body: "Three example search sites, each with its own data and front end.",
    links: MARKETPLACE.map((m) => ({ label: m.title, href: m.href })),
  },
];

export function DepthLinks() {
  return (
    <section className="border-t border-border py-16 lg:py-20">
      <Container>
        <SectionHeader
          eyebrow="Go deeper"
          title="The detail lives on its own page"
          description="Each of these has a page of its own. Pick the one that answers your next question."
        />
        <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {CARDS.map((card) => (
            <li
              key={card.id}
              id={card.id}
              className="scroll-mt-24 rounded-[var(--radius)] border border-border bg-card p-5"
            >
              <h3 className="text-base font-bold text-foreground">{card.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{card.body}</p>
              <ul className="mt-3 flex flex-wrap gap-x-4 gap-y-1.5">
                {card.links.map((link) => (
                  <li key={link.href}>
                    <a
                      href={link.href}
                      className="inline-flex items-center gap-1 text-sm font-semibold text-primary hover:underline"
                    >
                      {link.label} <ArrowRight className="size-3.5" />
                    </a>
                  </li>
                ))}
              </ul>
            </li>
          ))}
        </ul>
      </Container>
    </section>
  );
}
