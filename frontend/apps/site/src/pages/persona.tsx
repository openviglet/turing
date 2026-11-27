import { PageHero } from "@/components/page-hero";
import { ArrowRight, Check, Minus } from "lucide-react";
import { SiteNav } from "@/components/sections/site-nav";
import { SiteFooter } from "@/components/sections/site-footer";
import { ContentFitDemo } from "@/components/sections/content-fit-demo";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Container } from "@/components/brand";
import { LINKS } from "@/components/brand-links";
import { usePages } from "@/content/use-pages";
import { cn } from "@/lib/utils";
import { siteUrl, withBase } from "@/lib/site-url";

const PERSONA_BOOK_URL = withBase("/docs/persona-book/");

/** Matrix cell: yes/no render as icons, anything else as short freeform text. */
function Cell({ value, highlight }: Readonly<{ value: string; highlight?: boolean }>) {
  if (value === "yes") {
    return (
      <Check
        className={cn("mx-auto size-5", highlight ? "text-primary" : "text-emerald-500")}
      />
    );
  }
  if (value === "no") {
    return <Minus className="mx-auto size-5 text-muted-foreground/40" />;
  }
  return <span className="text-xs font-medium text-muted-foreground">{value}</span>;
}

/**
 * Structured data (@graph): a BreadcrumbList (Home › Personas), a
 * SoftwareApplication describing the feature, and the FAQPage so the objection
 * answers can win rich results. All prerendered to static HTML.
 */
function PersonaJsonLd() {
  const { PERSONA_FAQ } = usePages();
  const graph = {
    "@context": "https://schema.org",
    "@graph": [
      {
        "@type": "BreadcrumbList",
        itemListElement: [
          {
            "@type": "ListItem",
            position: 1,
            name: "Viglet Turing",
            item: siteUrl("/"),
          },
          {
            "@type": "ListItem",
            position: 2,
            name: "AI Personas",
            item: siteUrl("/persona/"),
          },
        ],
      },
      {
        "@type": "SoftwareApplication",
        name: "Viglet Turing · AI Personas (brand voice)",
        applicationCategory: "BusinessApplication",
        operatingSystem: "Any (self-hosted, Docker)",
        url: siteUrl("/persona/"),
        description:
          "Give your AI agents a governed brand voice. Required and forbidden vocabulary enforced in two layers, live brand facts via MCP, audience content-fit scoring, Persona Match and synthetic user research. Self-hosted, open source (Apache 2.0).",
        offers: { "@type": "Offer", price: "0", priceCurrency: "USD" },
        license: "https://www.apache.org/licenses/LICENSE-2.0",
      },
      {
        "@type": "FAQPage",
        mainEntity: PERSONA_FAQ.map((item) => ({
          "@type": "Question",
          name: item.q,
          acceptedAnswer: { "@type": "Answer", text: item.a },
        })),
      },
    ],
  };
  return (
    <script
      type="application/ld+json"
      // Prerendered to static HTML; this is our own content, not user input.
      dangerouslySetInnerHTML={{ __html: JSON.stringify(graph) }}
    />
  );
}

/**
 * Persona solution landing (/persona): "Give your AI a voice that sounds like
 * your company".
 *
 * Reframes prompt-engineering-per-agent as a reusable, governable brand voice:
 * define once, attach to any agent, enforce vocabulary in two layers, and
 * measure whether content fits its audience. Mirrors the AEM/Compare shell:
 * dark hero, dependency-free routing via plain <a href> full loads, FAQPage
 * JSON-LD, and a live content-fit demo as the centerpiece proof. The long-form
 * companion is "The Persona Book" in the docs.
 */
export function PersonaPage() {
  const { PERSONA_COLUMNS, PERSONA_FAQ, PERSONA_MATRIX, PERSONA_STEPS, PERSONA_SURFACES, PERSONA_VOICES, PERSONA_PAGE: t } = usePages();
  return (
    <div className="flex min-h-screen flex-col">
      <PersonaJsonLd />
      <SiteNav />
      <main className="flex-1">
        {/* hero */}
        <PageHero
          eyebrow={<>{t.eyebrow}</>}
          title={<>{t.title}</>}
          description={<>{t.description}</>}
          actions={
            <>
              <Button asChild size="lg">
                <a href={withBase("/#playground")}>
                  {`${t.tryCta} `}<ArrowRight className="size-4" />
                </a>
              </Button>
              <Button
                asChild
                size="lg"
                variant="outline"
              >
                <a href={PERSONA_BOOK_URL} target="_blank" rel="noreferrer">
                  {t.bookCta}
                </a>
              </Button>
            </>
          }
        />

        {/* live content-fit demo: the centerpiece proof */}
        <section className="py-16 lg:py-20">
          <Container>
            <div className="mx-auto mb-10 max-w-2xl text-center">
              <p className="text-xs font-extrabold tracking-[0.12em] text-primary uppercase">
                {t.matchEyebrow}
              </p>
              <h2 className="mt-3 text-2xl font-extrabold tracking-tight sm:text-3xl">
                {t.matchTitle}
              </h2>
              <p className="mt-3 text-base leading-relaxed text-muted-foreground">
                {t.matchLead}
              </p>
            </div>
            <div className="mx-auto max-w-2xl">
              <ContentFitDemo />
            </div>
          </Container>
        </section>

        {/* same question, three voices */}
        <section className="border-t border-border bg-muted/40 py-16 lg:py-20">
          <Container>
            <div className="mx-auto mb-10 max-w-2xl text-center">
              <p className="text-xs font-extrabold tracking-[0.12em] text-primary uppercase">
                {t.voicesTitle}
              </p>
              <h2 className="mt-3 text-2xl font-extrabold tracking-tight sm:text-3xl">
                {t.voicesQuestion}
              </h2>
              <p className="mt-3 text-base leading-relaxed text-muted-foreground">
                {t.voicesLead}
              </p>
            </div>
            <div className="grid gap-5 md:grid-cols-3">
              {PERSONA_VOICES.map((v) => (
                <Card key={v.style} className="flex h-full flex-col gap-0 p-6">
                  <h3 className="text-base font-bold">{v.style}</h3>
                  <p className="mt-1 text-xs font-semibold tracking-wide text-primary">
                    {v.config}
                  </p>
                  <p className="mt-4 flex-1 text-sm leading-relaxed text-muted-foreground">
                    {v.answer}
                  </p>
                  <p className="mt-4 border-t border-border pt-3 text-xs text-muted-foreground/80">
                    {v.use}
                  </p>
                </Card>
              ))}
            </div>
          </Container>
        </section>

        {/* how a persona reaches production */}
        <section className="py-16 lg:py-20">
          <Container>
            <div className="mx-auto mb-12 max-w-2xl text-center">
              <p className="text-xs font-extrabold tracking-[0.12em] text-primary uppercase">
                {t.howEyebrow}
              </p>
              <h2 className="mt-3 text-2xl font-extrabold tracking-tight sm:text-3xl">
                {t.howTitle}
              </h2>
              <p className="mt-3 text-base leading-relaxed text-muted-foreground">
                {t.howLead}
              </p>
            </div>
            <ol className="mx-auto grid max-w-5xl gap-8 sm:grid-cols-2">
              {PERSONA_STEPS.map((s) => (
                <li key={s.step} className="flex gap-3.5">
                  <span className="mt-0.5 flex size-7 shrink-0 items-center justify-center rounded-full bg-primary/10 text-sm font-extrabold text-primary">
                    {s.step}
                  </span>
                  <div className="min-w-0">
                    <h3 className="text-base font-bold">{s.title}</h3>
                    <p className="mt-1 text-sm leading-relaxed text-muted-foreground">
                      {s.body}
                    </p>
                  </div>
                </li>
              ))}
            </ol>
          </Container>
        </section>

        {/* persona surfaces: more than a system prompt */}
        <section className="border-t border-border py-16 lg:py-20">
          <Container>
            <div className="mx-auto mb-10 max-w-2xl text-center">
              <h2 className="text-2xl font-extrabold tracking-tight sm:text-3xl">
                {t.surfacesTitle}
              </h2>
              <p className="mt-3 text-base leading-relaxed text-muted-foreground">
                {t.surfacesLead}
              </p>
            </div>
            <div className="grid gap-5 md:grid-cols-2 lg:grid-cols-3">
              {PERSONA_SURFACES.map((d) => (
                <Card key={d.title} className="h-full gap-0 p-6">
                  <span
                    className={cn(
                      "flex size-11 items-center justify-center rounded-2xl bg-gradient-to-br text-white shadow-md",
                      d.iconClass
                    )}
                  >
                    <d.icon className="size-5" />
                  </span>
                  <h3 className="mt-5 text-base font-bold">{d.title}</h3>
                  <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                    {d.body}
                  </p>
                </Card>
              ))}
            </div>
          </Container>
        </section>

        {/* capability matrix */}
        <section className="border-t border-border bg-muted/40 py-16 lg:py-20">
          <Container>
            <div className="mx-auto mb-10 max-w-2xl text-center">
              <h2 className="text-2xl font-extrabold tracking-tight sm:text-3xl">
                {t.matrixTitle}
              </h2>
              <p className="mt-3 text-base leading-relaxed text-muted-foreground">
                {t.matrixLead}
              </p>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[680px] border-collapse text-sm">
                <thead>
                  <tr>
                    <th className="w-2/5 p-3 text-left font-semibold"></th>
                    <th className="rounded-t-xl bg-primary/10 p-3 text-center font-extrabold text-primary">
                      {PERSONA_COLUMNS.turing}
                    </th>
                    <th className="p-3 text-center font-semibold text-muted-foreground">
                      {PERSONA_COLUMNS.prompt}
                    </th>
                    <th className="p-3 text-center font-semibold text-muted-foreground">
                      {PERSONA_COLUMNS.chatbot}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {PERSONA_MATRIX.map((row, i) => (
                    <tr key={row.capability} className="border-t border-border">
                      <td className="p-3 font-medium">{row.capability}</td>
                      <td
                        className={cn(
                          "p-3 text-center bg-primary/5",
                          i === PERSONA_MATRIX.length - 1 && "rounded-b-xl"
                        )}
                      >
                        <Cell value={row.turing} highlight />
                      </td>
                      <td className="p-3 text-center">
                        <Cell value={row.prompt} />
                      </td>
                      <td className="p-3 text-center">
                        <Cell value={row.chatbot} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <p className="mt-4 text-center text-xs text-muted-foreground">
              {t.matrixNote}
            </p>
          </Container>
        </section>

        {/* objection-answering FAQ */}
        <section className="py-16 lg:py-20">
          <Container className="max-w-3xl">
            <div className="mb-10 text-center">
              <h2 className="text-2xl font-extrabold tracking-tight sm:text-3xl">
                {t.faqTitle}
              </h2>
              <p className="mt-3 text-base leading-relaxed text-muted-foreground">
                {t.faqLead}
              </p>
            </div>
            <dl className="divide-y divide-border">
              {PERSONA_FAQ.map((item) => (
                <div key={item.q} className="py-6">
                  <dt className="text-base font-bold">{item.q}</dt>
                  <dd className="mt-2 text-sm leading-relaxed text-muted-foreground">
                    {item.a}
                  </dd>
                </div>
              ))}
            </dl>
          </Container>
        </section>

        {/* CTA */}
        <section className="border-t border-border pb-16">
          <Container>
            <Card className="mx-auto mt-16 max-w-3xl gap-0 border-primary/20 bg-primary/5 p-8 text-center sm:p-10">
              <h2 className="text-2xl font-extrabold tracking-tight">
                {t.ctaTitle}
              </h2>
              <p className="mx-auto mt-3 max-w-xl text-base leading-relaxed text-muted-foreground">
                {t.ctaBody}
              </p>
              <div className="mt-8 flex flex-wrap justify-center gap-3">
                <Button asChild size="lg">
                  <a href={PERSONA_BOOK_URL} target="_blank" rel="noreferrer">
                    {`${t.bookCta} `}<ArrowRight className="size-4" />
                  </a>
                </Button>
                <Button asChild size="lg" variant="outline">
                  <a href={LINKS.docs}>{t.getStartedCta}</a>
                </Button>
              </div>
            </Card>
          </Container>
        </section>
      </main>
      <SiteFooter />
    </div>
  );
}
