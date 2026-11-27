import { PageHero } from "@/components/page-hero";
import { ArrowRight, Check, ShieldCheck } from "lucide-react";
import { SiteNav } from "@/components/sections/site-nav";
import { SiteFooter } from "@/components/sections/site-footer";
import { CodeBlock } from "@/components/code-block";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Container, TableScroll } from "@/components/brand";
import { LINKS } from "@/components/brand-links";
import { usePages } from "@/content/use-pages";
import { withBase } from "@/lib/site-url";

/**
 * "Switching from Algolia / Elasticsearch?" migration landing (§XXVII.3, T479).
 *
 * A sales page, not a runbook: the how-to lives in the docs. It leads with the
 * one thing that removes the objection ("switching is one command"), shows the
 * team keeps everything it has (concept map) and gains more (outcomes), and
 * closes on the differentiator: the data stays on their infrastructure.
 */
export function MigratePage() {
  const { MIGRATION_CLI_CODE, MIGRATION_CLI_FEATURES, MIGRATION_COLUMNS, MIGRATION_MAP, MIGRATION_OUTCOMES, MIGRATE_PAGE: t } = usePages();
  return (
    <div className="flex min-h-screen flex-col">
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
                <a href={`${LINKS.docs}migration`} target="_blank" rel="noreferrer">
                  {`${t.guideCta} `}<ArrowRight className="size-4" />
                </a>
              </Button>
              <Button
                asChild
                size="lg"
                variant="outline"
              >
                <a href={withBase("/#playground")}>{t.tryCta}</a>
              </Button>
            </>
          }
        />

        {/* the one command + trust signals */}
        <section className="py-16 lg:py-20">
          <Container className="grid gap-10 lg:grid-cols-2 lg:items-center">
            <div>
              <h2 className="text-2xl font-extrabold tracking-tight sm:text-3xl">
                {t.commandTitle}
              </h2>
              <p className="mt-4 text-base leading-relaxed text-muted-foreground">
                <code className="rounded bg-muted px-1.5 py-0.5 font-mono text-sm">
                  turing migrate
                </code>{" "}
                {t.commandBody}
              </p>
              <ul className="mt-6 space-y-4">
                {MIGRATION_CLI_FEATURES.map((f) => (
                  <li key={f.title} className="flex gap-3">
                    <Check className="mt-0.5 size-5 shrink-0 text-emerald-500" />
                    <div>
                      <p className="text-sm font-bold">{f.title}</p>
                      <p className="mt-0.5 text-sm leading-relaxed text-muted-foreground">
                        {f.body}
                      </p>
                    </div>
                  </li>
                ))}
              </ul>
            </div>
            <CodeBlock
              caption={MIGRATION_CLI_CODE.caption}
              code={MIGRATION_CLI_CODE.code}
            />
          </Container>
        </section>

        {/* concept mapping: you keep everything */}
        <section className="border-t border-border bg-muted/40 py-16 lg:py-20">
          <Container>
            <div className="mx-auto mb-10 max-w-2xl text-center">
              <h2 className="text-2xl font-extrabold tracking-tight sm:text-3xl">
                {t.mapTitle}
              </h2>
              <p className="mt-3 text-base leading-relaxed text-muted-foreground">
                {t.mapLead}
              </p>
            </div>
            <TableScroll fadeClassName="from-muted">
              <table className="w-full min-w-[720px] border-collapse text-sm">
                <thead>
                  <tr>
                    <th className="sticky left-0 z-10 w-[140px] bg-background p-3 text-left font-semibold text-muted-foreground sm:w-1/5">
                      {MIGRATION_COLUMNS.concept}
                    </th>
                    <th className="p-3 text-left font-semibold text-muted-foreground">
                      {MIGRATION_COLUMNS.algolia}
                    </th>
                    <th className="p-3 text-left font-semibold text-muted-foreground">
                      {MIGRATION_COLUMNS.elastic}
                    </th>
                    <th className="rounded-t-xl bg-primary/10 p-3 text-left font-extrabold text-primary">
                      {MIGRATION_COLUMNS.turing}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {MIGRATION_MAP.map((row, i) => (
                    <tr key={row.concept} className="border-t border-border">
                      <td className="sticky left-0 z-10 bg-background p-3 font-semibold">
                        {row.concept}
                      </td>
                      <td className="p-3 text-muted-foreground">{row.algolia}</td>
                      <td className="p-3 text-muted-foreground">{row.elastic}</td>
                      <td
                        className={
                          "bg-primary/5 p-3 font-medium" +
                          (i === MIGRATION_MAP.length - 1 ? " rounded-b-xl" : "")
                        }
                      >
                        {row.turing}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </TableScroll>
            <p className="mt-4 text-center text-xs text-muted-foreground">
              {t.mapNote}
            </p>
          </Container>
        </section>

        {/* what you gain */}
        <section className="py-16 lg:py-20">
          <Container>
            <div className="mx-auto mb-10 max-w-2xl text-center">
              <h2 className="text-2xl font-extrabold tracking-tight sm:text-3xl">
                {t.outcomesTitle}
              </h2>
            </div>
            <div className="grid gap-6 md:grid-cols-3">
              {MIGRATION_OUTCOMES.map((o) => (
                <div
                  key={o.title}
                  className="rounded-2xl border border-border bg-card p-6"
                >
                  <h3 className="text-base font-bold">{o.title}</h3>
                  <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                    {o.body}
                  </p>
                </div>
              ))}
            </div>
          </Container>
        </section>

        {/* your data stays in your infra */}
        <section className="border-t border-border py-16 lg:py-20">
          <Container>
            <Card className="mx-auto max-w-3xl gap-0 border-primary/20 bg-primary/5 p-8 text-center sm:p-10">
              <span className="mx-auto flex size-12 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                <ShieldCheck className="size-6" />
              </span>
              <h2 className="mt-5 text-2xl font-extrabold tracking-tight">
                {t.dataTitle}
              </h2>
              <p className="mx-auto mt-3 max-w-xl text-base leading-relaxed text-muted-foreground">
                {t.dataBody}
              </p>
              <ul className="mx-auto mt-6 grid max-w-xl gap-2.5 text-left sm:grid-cols-2">
                {t.keepList.map((item) => (
                  <li key={item} className="flex items-start gap-2 text-sm">
                    <Check className="mt-0.5 size-4 shrink-0 text-emerald-500" />
                    <span>{item}</span>
                  </li>
                ))}
              </ul>
              <div className="mt-8 flex flex-wrap justify-center gap-3">
                <Button asChild size="lg">
                  <a href={`${LINKS.docs}migration`} target="_blank" rel="noreferrer">
                    {`${t.guideCta} `}<ArrowRight className="size-4" />
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
