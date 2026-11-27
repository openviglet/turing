import { PageHero } from "@/components/page-hero";
import { ArrowRight, Check, Minus } from "lucide-react";
import { SiteNav } from "@/components/sections/site-nav";
import { SiteFooter } from "@/components/sections/site-footer";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Container, TableScroll } from "@/components/brand";
import { LINKS } from "@/components/brand-links";
import { usePages } from "@/content/use-pages";
import { cn } from "@/lib/utils";
import { withBase } from "@/lib/site-url";

/** Matrix cell: yes/no render as icons, anything else as short freeform text. */
function Cell({ value, highlight }: { value: string; highlight?: boolean }) {
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
  return (
    <span className="text-xs font-medium text-muted-foreground">{value}</span>
  );
}

/** FAQPage structured data so the objection answers can win rich results. */
function FaqJsonLd() {
  const { FAQ } = usePages();
  const json = {
    "@context": "https://schema.org",
    "@type": "FAQPage",
    mainEntity: FAQ.map((item) => ({
      "@type": "Question",
      name: item.q,
      acceptedAnswer: { "@type": "Answer", text: item.a },
    })),
  };
  return (
    <script
      type="application/ld+json"
      // Prerendered to static HTML; this is our own content, not user input.
      dangerouslySetInnerHTML={{ __html: JSON.stringify(json) }}
    />
  );
}

/**
 * Deep comparison / FAQ page (§XXVII.3, T482).
 *
 * Answers the objections a team evaluating Turing against Algolia, Coveo,
 * Elasticsearch or a DIY LangChain stack will raise: a fair capability
 * matrix, honest "where they win / where Turing fits" cards, and an
 * objection-answering FAQ (with FAQPage JSON-LD). Mirrors the MigratePage
 * shell: dark hero, dependency-free routing via plain <a href> full loads.
 */
export function ComparePage() {
  const { COMPARE_COLUMNS, COMPARE_MATRIX, COMPARE_VENDORS, COVEO_LUCIDWORKS_MATRIX, COVEO_LUCIDWORKS_POST, FAQ, COMPARE_PAGE: t } = usePages();
  return (
    <div className="flex min-h-screen flex-col">
      <FaqJsonLd />
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
                <a href={withBase("/migrate/")}>{t.migrateCta}</a>
              </Button>
            </>
          }
        />

        {/* deep capability matrix */}
        <section className="py-16 lg:py-20">
          <Container>
            <div className="mx-auto mb-10 max-w-2xl text-center">
              <h2 className="text-2xl font-extrabold tracking-tight sm:text-3xl">
                {t.matrixTitle}
              </h2>
              <p className="mt-3 text-base leading-relaxed text-muted-foreground">
                {t.matrixLead}
              </p>
            </div>
            <TableScroll>
              <table className="w-full min-w-[880px] border-collapse text-sm">
                <thead>
                  <tr>
                    <th className="sticky left-0 z-10 w-[150px] bg-background p-3 text-left font-semibold sm:w-2/5"></th>
                    <th className="rounded-t-xl bg-primary/10 p-3 text-center font-extrabold text-primary">
                      {COMPARE_COLUMNS.turing}
                    </th>
                    <th className="p-3 text-center font-semibold text-muted-foreground">
                      {COMPARE_COLUMNS.algolia}
                    </th>
                    <th className="p-3 text-center font-semibold text-muted-foreground">
                      {COMPARE_COLUMNS.coveo}
                    </th>
                    <th className="p-3 text-center font-semibold text-muted-foreground">
                      {COMPARE_COLUMNS.elastic}
                    </th>
                    <th className="p-3 text-center font-semibold text-muted-foreground">
                      {COMPARE_COLUMNS.diy}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {COMPARE_MATRIX.map((row, i) => (
                    <tr key={row.capability} className="border-t border-border">
                      <td className="sticky left-0 z-10 bg-background p-3 font-medium">
                        {row.capability}
                      </td>
                      <td
                        className={cn(
                          "p-3 text-center bg-primary/5",
                          i === COMPARE_MATRIX.length - 1 && "rounded-b-xl"
                        )}
                      >
                        <Cell value={row.turing} highlight />
                      </td>
                      <td className="p-3 text-center">
                        <Cell value={row.algolia} />
                      </td>
                      <td className="p-3 text-center">
                        <Cell value={row.coveo} />
                      </td>
                      <td className="p-3 text-center">
                        <Cell value={row.elastic} />
                      </td>
                      <td className="p-3 text-center">
                        <Cell value={row.diy} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </TableScroll>
            <p className="mt-4 text-center text-xs text-muted-foreground">
              {t.matrixNote}
            </p>
          </Container>
        </section>

        {/* T1096: the enterprise-portal comparison people search for */}
        <section id="coveo-vs-lucidworks" className="scroll-mt-24 border-t border-border py-16 lg:py-20">
          <Container className="max-w-4xl">
            <div className="mx-auto mb-10 max-w-2xl text-center">
              <h2 className="text-2xl font-extrabold tracking-tight sm:text-3xl">
                {t.portalTitle}
              </h2>
              <p className="mt-3 text-base leading-relaxed text-muted-foreground">
                {t.portalLead}
              </p>
            </div>
            <TableScroll>
              <table className="w-full min-w-[640px] border-collapse text-sm">
                <thead>
                  <tr>
                    <th className="sticky left-0 z-10 bg-background p-3 text-left font-semibold"></th>
                    <th className="p-3 text-left font-semibold text-muted-foreground">Coveo</th>
                    <th className="p-3 text-left font-semibold text-muted-foreground">Lucidworks Fusion</th>
                    <th className="rounded-t-xl bg-primary/10 p-3 text-left font-extrabold text-primary">
                      {COMPARE_COLUMNS.turing}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {COVEO_LUCIDWORKS_MATRIX.map((row) => (
                    <tr key={row.capability} className="border-t border-border">
                      <td className="sticky left-0 z-10 bg-background p-3 font-medium">{row.capability}</td>
                      <td className="p-3 text-muted-foreground">{row.coveo}</td>
                      <td className="p-3 text-muted-foreground">{row.lucidworks}</td>
                      <td className="bg-primary/5 p-3 font-medium">{row.turing}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </TableScroll>
            <p className="mt-4 text-center text-sm text-muted-foreground">
              {t.postLead}{" "}
              <a href={COVEO_LUCIDWORKS_POST} className="font-semibold text-primary hover:underline">
                {t.postLink}
              </a>
            </p>
          </Container>
        </section>

        {/* honest per-competitor cards */}
        <section className="border-t border-border bg-muted/40 py-16 lg:py-20">
          <Container>
            <div className="mx-auto mb-10 max-w-2xl text-center">
              <h2 className="text-2xl font-extrabold tracking-tight sm:text-3xl">
                {t.vendorsTitle}
              </h2>
              <p className="mt-3 text-base leading-relaxed text-muted-foreground">
                {t.vendorsLead}
              </p>
            </div>
            <div className="grid gap-5 md:grid-cols-2">
              {COMPARE_VENDORS.map((v) => (
                <Card key={v.name} className="h-full gap-0 p-6">
                  <h3 className="text-lg font-extrabold">{v.name}</h3>
                  <p className="mt-1 text-sm text-muted-foreground">{v.tagline}</p>
                  <p className="mt-5 text-xs font-bold tracking-[0.08em] text-muted-foreground uppercase">
                    {t.shines}
                  </p>
                  <p className="mt-1.5 text-sm leading-relaxed">{v.shines}</p>
                  <p className="mt-5 text-xs font-bold tracking-[0.08em] text-primary uppercase">
                    {t.fits}
                  </p>
                  <p className="mt-1.5 text-sm leading-relaxed">{v.turingFit}</p>
                </Card>
              ))}
            </div>
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
              {FAQ.map((item) => (
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
        <section className="border-t border-border bg-muted/40 py-16">
          <Container>
            <Card className="mx-auto max-w-3xl gap-0 border-primary/20 bg-primary/5 p-8 text-center sm:p-10">
              <h2 className="text-2xl font-extrabold tracking-tight">
                {t.ctaTitle}
              </h2>
              <p className="mx-auto mt-3 max-w-xl text-base leading-relaxed text-muted-foreground">
                {t.ctaBody}
              </p>
              <div className="mt-8 flex flex-wrap justify-center gap-3">
                <Button asChild size="lg">
                  <a href={LINKS.docs}>
                    {`${t.getStartedCta} `}<ArrowRight className="size-4" />
                  </a>
                </Button>
                <Button asChild size="lg" variant="outline">
                  <a href={withBase("/#playground")}>{t.tryCta}</a>
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
