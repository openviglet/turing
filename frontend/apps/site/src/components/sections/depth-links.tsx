import { ArrowRight } from "lucide-react";
import { Container, SectionHeader } from "@/components/brand";
import { LINKS } from "@/components/brand-links";
import { usePages } from "@/content/use-pages";
import type { PagesContent } from "@/content/pages";
import { withBase } from "@/lib/site-url";

/**
 * T1271: where the home's depth went. These sections used to be full blocks on
 * the home (the agent loop, the SDK code, native tools, the CLI, the example
 * apps); each is now one short summary that links to the page that owns the
 * detail. Each card keeps its old anchor id, so a link to /#how, /#developers,
 * /#native-tools, /#cli or /#marketplace still lands on its subject.
 */
function cards(
  t: PagesContent["DEPTH_LINKS"],
  marketplace: PagesContent["MARKETPLACE"]
): {
  id: string;
  title: string;
  body: string;
  links: { label: string; href: string }[];
}[] {
  return [
    { id: "how", ...t.cards.how, links: [{ label: t.cards.how.links[0], href: `${LINKS.docs}rag/` }] },
    {
      id: "developers",
      ...t.cards.developers,
      links: [
        { label: t.cards.developers.links[0], href: `${LINKS.docs}javascript-sdk/` },
        { label: t.cards.developers.links[1], href: LINKS.storybook },
      ],
    },
    {
      id: "native-tools",
      ...t.cards.nativeTools,
      links: [{ label: t.cards.nativeTools.links[0], href: withBase("/features/automate/") }],
    },
    { id: "cli", ...t.cards.cli, links: [{ label: t.cards.cli.links[0], href: `${LINKS.docs}cli/` }] },
    {
      id: "marketplace",
      ...t.cards.marketplace,
      links: marketplace.map((m) => ({ label: m.title, href: m.href })),
    },
  ];
}

export function DepthLinks() {
  const { DEPTH_LINKS: t, MARKETPLACE } = usePages();
  const CARDS = cards(t, MARKETPLACE);
  return (
    <section className="border-t border-border py-16 lg:py-20">
      <Container>
        <SectionHeader
          eyebrow={t.eyebrow}
          title={t.title}
          description={t.description}
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
