import { ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Container } from "@/components/brand";
import { LINKS } from "@/components/brand-links";
import { withBase } from "@/lib/site-url";

export function CallToAction() {
  return (
    <section className="py-20">
      <Container>
        {/* T1269: a neutral band in the hub's grammar, not a permanent dark panel. */}
        <div className="rounded-3xl border border-border bg-muted px-6 py-16 text-center">
          <h2 className="text-3xl font-extrabold tracking-tight text-foreground sm:text-4xl">
            Own your AI search stack
          </h2>
          <p className="mx-auto mt-3 max-w-lg text-base text-muted-foreground sm:text-lg">
            Open source, self-hosted, and works with any LLM. Stand up the full
            platform in one command and ship cited AI answers over your own
            content, without your data leaving your infrastructure.
          </p>
          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <Button asChild size="lg">
              <a href={LINKS.docs}>
                Read the docs <ArrowRight className="size-4" />
              </a>
            </Button>
            <Button asChild size="lg" variant="outline">
              <a href={withBase("/#playground")}>Try it live</a>
            </Button>
          </div>
        </div>
      </Container>
    </section>
  );
}
