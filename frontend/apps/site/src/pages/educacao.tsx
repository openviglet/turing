import { ArrowRight, Search, Sparkles } from "lucide-react";
import { PageHero } from "@/components/page-hero";
import { SiteNav } from "@/components/sections/site-nav";
import { SiteFooter } from "@/components/sections/site-footer";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Container } from "@/components/brand";
import { LINKS } from "@/components/brand-links";
import {
  EDUCACAO_BENEFITS,
  EDUCACAO_BEYOND,
  EDUCACAO_CONTACT,
  EDUCACAO_CONTACT_UTM,
  EDUCACAO_COST,
  EDUCACAO_DEMO,
  EDUCACAO_EXAMPLES,
  EDUCACAO_FAQ,
  EDUCACAO_HERO,
  EDUCACAO_META,
  EDUCACAO_PRIVACY,
  EDUCACAO_PROBLEM,
  EDUCACAO_STEPS,
} from "@/lib/educacao-content";
import { siteUrl } from "@/lib/site-url";

/** The viglet.org contact form, tagged with this page's campaign, opened at the form. */
const CONTACT_HREF = `${LINKS.contact}?${EDUCACAO_CONTACT_UTM}#contact-form`;

/**
 * Structured data (@graph): BreadcrumbList (Início › Educação), the WebPage in
 * pt-BR, and the FAQPage. Prerendered to static HTML.
 */
function EducacaoJsonLd() {
  const graph = {
    "@context": "https://schema.org",
    "@graph": [
      {
        "@type": "BreadcrumbList",
        itemListElement: [
          { "@type": "ListItem", position: 1, name: "Viglet Turing", item: siteUrl("/") },
          { "@type": "ListItem", position: 2, name: "Educação", item: siteUrl("/educacao/") },
        ],
      },
      {
        "@type": "WebPage",
        name: EDUCACAO_META.title,
        description: EDUCACAO_META.description,
        url: siteUrl("/educacao/"),
        inLanguage: "pt-BR",
        audience: { "@type": "EducationalAudience", educationalRole: "administrator" },
      },
      {
        "@type": "FAQPage",
        inLanguage: "pt-BR",
        mainEntity: EDUCACAO_FAQ.map((item) => ({
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
      dangerouslySetInnerHTML={{ __html: JSON.stringify(graph).replaceAll("<", String.raw`<`) }}
    />
  );
}

/** The hero's aside: what a visitor types, and what comes back. */
function ExampleSearches() {
  return (
    <Card className="gap-0 p-0">
      <ul className="divide-y divide-border">
        {EDUCACAO_EXAMPLES.map((ex) => (
          <li key={ex.query} className="p-5">
            <p className="flex items-center gap-2 font-semibold text-foreground">
              <Search className="size-4 shrink-0 text-primary" aria-hidden />
              <span>“{ex.query}”</span>
            </p>
            <p className="mt-1.5 pl-6 text-sm leading-relaxed text-muted-foreground">{ex.result}</p>
          </li>
        ))}
      </ul>
    </Card>
  );
}

function Eyebrow({ children }: Readonly<{ children: string }>) {
  return (
    <p className="text-xs font-extrabold tracking-[0.12em] text-primary uppercase">{children}</p>
  );
}

/**
 * Educação (/educacao/): Viglet Turing on the website of a school, college or
 * university, in Brazilian Portuguese. A search box on such a site can link its
 * "Powered by Viglet Turing" here, so the page speaks to the institution's
 * communication and IT teams in their language, chrome included.
 */
export function EducacaoPage() {
  return (
    <div className="flex min-h-screen flex-col" lang="pt-BR">
      <EducacaoJsonLd />
      <SiteNav />
      <main className="flex-1">
        <PageHero
          eyebrow={EDUCACAO_HERO.eyebrow}
          title={EDUCACAO_HERO.title}
          description={EDUCACAO_HERO.description}
          actions={
            <>
              <Button asChild size="lg">
                <a href={CONTACT_HREF} target="_blank" rel="noopener">
                  {EDUCACAO_CONTACT.cta} <ArrowRight className="size-4" />
                </a>
              </Button>
              <Button asChild size="lg" variant="outline">
                <a href="#recursos">O que a busca faz</a>
              </Button>
            </>
          }
          aside={<ExampleSearches />}
        />

        {/* the problem */}
        <section className="py-16 lg:py-20">
          <Container className="max-w-3xl">
            <Eyebrow>{EDUCACAO_PROBLEM.eyebrow}</Eyebrow>
            <h2 className="mt-3 text-2xl font-extrabold tracking-tight sm:text-3xl">
              {EDUCACAO_PROBLEM.title}
            </h2>
            {EDUCACAO_PROBLEM.body.map((p) => (
              <p key={p} className="mt-4 text-base leading-relaxed text-muted-foreground">
                {p}
              </p>
            ))}
          </Container>
        </section>

        {/* benefits */}
        <section id="recursos" className="scroll-mt-24 border-y border-border bg-muted/40 py-16 lg:py-20">
          <Container>
            <div className="mx-auto mb-12 max-w-2xl text-center">
              <Eyebrow>O que muda no seu site</Eyebrow>
              <h2 className="mt-3 text-2xl font-extrabold tracking-tight sm:text-3xl">
                {EDUCACAO_BENEFITS.length} coisas que a busca passa a fazer
              </h2>
              <p className="mt-3 text-base leading-relaxed text-muted-foreground">
                Os itens marcados com <Sparkles className="inline size-4 text-primary" aria-label="IA" />{" "}
                usam um modelo de linguagem configurado no Viglet Turing. Os demais funcionam sem
                nenhum.
              </p>
            </div>
            <ul className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
              {EDUCACAO_BENEFITS.map((b) => (
                <li key={b.title}>
                  <Card className="h-full gap-0 p-6">
                    <h3 className="flex items-start justify-between gap-3 text-lg font-bold">
                      {b.title}
                      {b.ai && (
                        <Sparkles
                          className="mt-1 size-4 shrink-0 text-primary"
                          aria-label="Usa um modelo de linguagem"
                        />
                      )}
                    </h3>
                    <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{b.body}</p>
                  </Card>
                </li>
              ))}
            </ul>
          </Container>
        </section>

        {/* privacy */}
        <section id="dados" className="scroll-mt-24 py-16 lg:py-20">
          <Container>
            <div className="mx-auto mb-12 max-w-2xl text-center">
              <Eyebrow>{EDUCACAO_PRIVACY.eyebrow}</Eyebrow>
              <h2 className="mt-3 text-2xl font-extrabold tracking-tight sm:text-3xl">
                {EDUCACAO_PRIVACY.title}
              </h2>
            </div>
            <div className="grid gap-8 md:grid-cols-3">
              {EDUCACAO_PRIVACY.points.map((p) => (
                <div key={p.title}>
                  <h3 className="text-lg font-bold">{p.title}</h3>
                  <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{p.body}</p>
                </div>
              ))}
            </div>
          </Container>
        </section>

        {/* cost */}
        <section className="border-y border-border bg-muted/40 py-16 lg:py-20">
          <Container className="max-w-3xl text-center">
            <Eyebrow>{EDUCACAO_COST.eyebrow}</Eyebrow>
            <h2 className="mt-3 text-2xl font-extrabold tracking-tight sm:text-3xl">
              {EDUCACAO_COST.title}
            </h2>
            <p className="mt-3 text-base leading-relaxed text-muted-foreground">{EDUCACAO_COST.body}</p>
          </Container>
        </section>

        {/* getting started */}
        <section id="comecar" className="scroll-mt-24 py-16 lg:py-20">
          <Container>
            <div className="mx-auto mb-12 max-w-2xl text-center">
              <Eyebrow>Como começar</Eyebrow>
              <h2 className="mt-3 text-2xl font-extrabold tracking-tight sm:text-3xl">
                Do servidor à caixa de busca, em três passos
              </h2>
            </div>
            <ol className="grid gap-5 md:grid-cols-3">
              {EDUCACAO_STEPS.map((s, i) => (
                <li key={s.title}>
                  <Card className="h-full gap-0 p-6">
                    <p className="font-mono text-sm font-bold text-primary">Passo {i + 1}</p>
                    <h3 className="mt-2 text-lg font-bold">{s.title}</h3>
                    <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{s.body}</p>
                  </Card>
                </li>
              ))}
            </ol>
            <Card className="mx-auto mt-10 max-w-3xl gap-0 border-primary/20 bg-primary/5 p-8 text-center">
              <h3 className="text-xl font-extrabold tracking-tight">{EDUCACAO_DEMO.title}</h3>
              <p className="mx-auto mt-3 max-w-xl text-sm leading-relaxed text-muted-foreground">
                {EDUCACAO_DEMO.body}
              </p>
              <div className="mt-6 flex flex-wrap justify-center gap-3">
                <Button asChild>
                  <a href={LINKS.wordpressDocs}>
                    Documentação do plugin (em inglês) <ArrowRight className="size-4" />
                  </a>
                </Button>
                <Button asChild variant="outline">
                  <a href={LINKS.getStarted}>Instalar o Viglet Turing (em inglês)</a>
                </Button>
              </div>
            </Card>
          </Container>
        </section>

        {/* FAQ */}
        <section id="perguntas" className="scroll-mt-24 border-t border-border py-16 lg:py-20">
          <Container className="max-w-3xl">
            <h2 className="mb-8 text-center text-2xl font-extrabold tracking-tight sm:text-3xl">
              Perguntas frequentes
            </h2>
            <dl className="divide-y divide-border">
              {EDUCACAO_FAQ.map((item) => (
                <div key={item.q} className="py-6">
                  <dt className="text-base font-bold">{item.q}</dt>
                  <dd className="mt-2 text-sm leading-relaxed text-muted-foreground">{item.a}</dd>
                </div>
              ))}
            </dl>
          </Container>
        </section>

        {/* lead capture: the viglet.org contact form */}
        <section id="contato" className="scroll-mt-24 pb-16">
          <Container>
            <Card className="mx-auto max-w-3xl gap-0 border-primary/30 bg-primary/10 p-8 text-center sm:p-10">
              <Eyebrow>{EDUCACAO_CONTACT.eyebrow}</Eyebrow>
              <h2 className="mt-3 text-2xl font-extrabold tracking-tight sm:text-3xl">
                {EDUCACAO_CONTACT.title}
              </h2>
              <p className="mx-auto mt-3 max-w-xl text-base leading-relaxed text-muted-foreground">
                {EDUCACAO_CONTACT.body}
              </p>
              <div className="mt-8 flex justify-center">
                <Button asChild size="lg">
                  <a href={CONTACT_HREF} target="_blank" rel="noopener">
                    {EDUCACAO_CONTACT.cta} <ArrowRight className="size-4" />
                  </a>
                </Button>
              </div>
              <p className="mt-4 text-xs text-muted-foreground">{EDUCACAO_CONTACT.note}</p>
            </Card>
          </Container>
        </section>

        {/* beyond WordPress */}
        <section className="pb-16">
          <Container>
            <Card className="mx-auto max-w-3xl gap-0 border-primary/20 bg-primary/5 p-8 text-center sm:p-10">
              <h2 className="text-2xl font-extrabold tracking-tight">{EDUCACAO_BEYOND.title}</h2>
              <p className="mx-auto mt-3 max-w-xl text-base leading-relaxed text-muted-foreground">
                {EDUCACAO_BEYOND.body}
              </p>
              <div className="mt-8 flex flex-wrap justify-center gap-3">
                <Button asChild size="lg">
                  <a href={LINKS.dumont} target="_blank" rel="noreferrer">
                    Viglet Dumont <ArrowRight className="size-4" />
                  </a>
                </Button>
                <Button asChild size="lg" variant="outline">
                  <a href={LINKS.source} target="_blank" rel="noreferrer">
                    Código no GitHub
                  </a>
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
