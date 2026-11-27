import { ArrowRight, Check } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Container } from "@/components/brand";
import { LINKS } from "@/components/brand-links";
import { CopyCommand } from "@/components/copy-command";
import { SearchDemo } from "@/components/sections/search-demo";
import { usePages } from "@/content/use-pages";

/**
 * The home hero, in the viglet.org hub's grammar (T1269): a neutral,
 * theme-aware surface closed by a border, the licence and hosting facts as a
 * plain eyebrow rather than a chip, a foreground title with no gradient text,
 * and no grid or animated orbs competing with the live search demo.
 */
export function Hero() {
  const { HERO: t } = usePages();
  return (
    <section id="top" data-page-hero className="border-b border-border bg-background">
      <Container className="grid grid-cols-1 items-center gap-12 py-14 lg:grid-cols-[1.05fr_0.95fr] lg:py-20">
        <div className="min-w-0">
          <p className="text-xs font-semibold tracking-wider text-muted-foreground uppercase">
            {t.eyebrow}
          </p>
          <h1 className="mt-4 text-4xl font-extrabold leading-[1.08] tracking-tight text-foreground sm:text-5xl lg:text-[3.4rem]">
            {t.title}
          </h1>
          <p className="mt-5 max-w-xl text-lg leading-relaxed text-muted-foreground">
            {t.lead}
          </p>
          <p className="mt-3 max-w-xl text-sm leading-relaxed text-muted-foreground">
            {t.audience}
          </p>

          <div className="mt-7">
            <CopyCommand command="npx @viglet/turing-cli init my-search" />
          </div>

          <div className="mt-6 flex flex-wrap gap-3">
            <Button asChild size="lg">
              <a href={LINKS.getStarted}>
                {`${t.getStartedCta} `}<ArrowRight className="size-4" />
              </a>
            </Button>
            <Button asChild size="lg" variant="outline">
              <a href="#playground">{t.liveCta}</a>
            </Button>
          </div>

          <ul className="mt-7 flex flex-wrap gap-x-6 gap-y-2 text-sm text-muted-foreground">
            {t.points.map((item) => (
              <li key={item} className="inline-flex items-center gap-1.5">
                <Check className="size-4 text-primary" />
                {item}
              </li>
            ))}
          </ul>
        </div>

        <div className="min-w-0">
          <SearchDemo />
          <p className="mt-3 text-center text-xs text-muted-foreground">
            {t.demoHint}
          </p>
        </div>
      </Container>
    </section>
  );
}
