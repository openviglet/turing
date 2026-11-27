import { PageHero } from "@/components/page-hero";
import { ArrowRight, Check, ShieldCheck } from "lucide-react";
import { SiteNav } from "@/components/sections/site-nav";
import { SiteFooter } from "@/components/sections/site-footer";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Container } from "@/components/brand";
import { LINKS } from "@/components/brand-links";
import { usePages } from "@/content/use-pages";
import { cn } from "@/lib/utils";
import { withBase } from "@/lib/site-url";

/** FAQPage JSON-LD for the data-residency questions, prerendered to static HTML. */
function SecurityJsonLd() {
  const { SECURITY_FAQ } = usePages();
  const graph = {
    "@context": "https://schema.org",
    "@type": "FAQPage",
    mainEntity: SECURITY_FAQ.map((item) => ({
      "@type": "Question",
      name: item.q,
      acceptedAnswer: { "@type": "Answer", text: item.a },
    })),
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
 * Security & data residency (/security).
 *
 * Turns the platform's strongest, most under-sold argument (the data never
 * leaves your infrastructure) into a dedicated sales asset. Answers the
 * enterprise buyer's SOC 2 / GDPR reflex with the honest self-hosted counter:
 * you don't have to trust our compliance because the data is stored on your side to begin
 * with. Mirrors the Compare/Migrate/AEM shell (dark hero, dependency-free
 * routing via plain <a href> full loads).
 */
export function SecurityPage() {
  const { SECURITY_FAQ, SECURITY_POINTS, SECURITY_PAGE: t } = usePages();
  return (
    <div className="flex min-h-screen flex-col">
      <SecurityJsonLd />
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
                <a href={`${LINKS.docs}deploy-operate/`}>
                  {`${t.docsCta} `}<ArrowRight className="size-4" />
                </a>
              </Button>
              <Button
                asChild
                size="lg"
                variant="outline"
              >
                <a href={withBase("/#self-host")}>{t.selfHostCta}</a>
              </Button>
            </>
          }
        />

        {/* the core argument: SaaS trust vs self-hosted */}
        <section className="py-16 lg:py-20">
          <Container>
            <Card className="mx-auto max-w-3xl gap-0 border-primary/20 bg-primary/5 p-8 text-center sm:p-10">
              <span className="mx-auto flex size-12 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                <ShieldCheck className="size-6" />
              </span>
              <h2 className="mt-5 text-2xl font-extrabold tracking-tight">
                {t.boundaryTitle}
              </h2>
              <p className="mx-auto mt-3 max-w-xl text-base leading-relaxed text-muted-foreground">
                {t.boundaryBody}
              </p>
            </Card>
          </Container>
        </section>

        {/* security points grid */}
        <section className="border-t border-border bg-muted/40 py-16 lg:py-20">
          <Container>
            <div className="mx-auto mb-10 max-w-2xl text-center">
              <h2 className="text-2xl font-extrabold tracking-tight sm:text-3xl">
                {t.pointsTitle}
              </h2>
              <p className="mt-3 text-base leading-relaxed text-muted-foreground">
                {t.pointsLead}
              </p>
            </div>
            <div className="grid gap-5 md:grid-cols-2 lg:grid-cols-3">
              {SECURITY_POINTS.map((p) => (
                <Card key={p.title} className="h-full gap-0 p-6">
                  <span
                    className={cn(
                      "flex size-11 items-center justify-center rounded-2xl bg-gradient-to-br text-white shadow-md",
                      p.iconClass
                    )}
                  >
                    <p.icon className="size-5" />
                  </span>
                  <h3 className="mt-5 text-base font-bold">{p.title}</h3>
                  <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                    {p.body}
                  </p>
                </Card>
              ))}
            </div>
          </Container>
        </section>

        {/* data-residency questions, as buyers ask them */}
        <section id="faq" className="scroll-mt-24 py-16 lg:py-20">
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
              {SECURITY_FAQ.map((item) => (
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

        {/* honest scope note + CTA */}
        <section className="py-16 lg:py-20">
          <Container>
            <div className="mx-auto max-w-3xl">
              <h2 className="text-2xl font-extrabold tracking-tight">
                {t.scopeTitle}
              </h2>
              <p className="mt-3 text-base leading-relaxed text-muted-foreground">
                {t.scopeBody}
              </p>
              <ul className="mt-6 grid gap-2.5 sm:grid-cols-2">
                {t.scopeList.map((item) => (
                  <li key={item} className="flex items-start gap-2 text-sm">
                    <Check className="mt-0.5 size-4 shrink-0 text-emerald-500" />
                    <span>{item}</span>
                  </li>
                ))}
              </ul>
              <div className="mt-8 flex flex-wrap gap-3">
                <Button asChild size="lg">
                  <a href={withBase("/aem-search/")}>
                    {`${t.aemCta} `}<ArrowRight className="size-4" />
                  </a>
                </Button>
                <Button asChild size="lg" variant="outline">
                  <a href={withBase("/compare/")}>{t.compareCta}</a>
                </Button>
              </div>
            </div>
          </Container>
        </section>
      </main>
      <SiteFooter />
    </div>
  );
}
