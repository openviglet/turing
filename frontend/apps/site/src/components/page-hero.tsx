import type { ReactNode } from "react";
import { ArrowLeft } from "lucide-react";
import { Container } from "@/components/brand";
import { cn } from "@/lib/utils";
import { withBase } from "@/lib/site-url";

/**
 * T1269: the one hero every marketing subpage uses, in the viglet.org hub's
 * grammar: a neutral, theme-aware surface closed by a border, a plain
 * uppercase eyebrow (no chip), a foreground title and muted lede, then the
 * actions. No permanent dark surface, grid, orbs or gradient text; the
 * product accent comes from the primary button. `aside` (a video, a demo)
 * makes it two columns from lg up.
 */
export function PageHero({
  eyebrow,
  title,
  description,
  actions,
  aside,
  back = true,
}: Readonly<{
  eyebrow: ReactNode;
  title: ReactNode;
  description: ReactNode;
  actions?: ReactNode;
  aside?: ReactNode;
  back?: boolean;
}>) {
  const copy = (
    <div className="min-w-0">
      {back && (
        <a
          href={withBase("/")}
          className="inline-flex items-center gap-1.5 text-sm font-semibold text-muted-foreground transition-colors hover:text-foreground"
        >
          <ArrowLeft className="size-4" /> Back to overview
        </a>
      )}
      <p className={cn("text-xs font-semibold tracking-wider text-muted-foreground uppercase", back && "mt-6")}>
        {eyebrow}
      </p>
      <h1 className="mt-3 max-w-3xl text-3xl font-extrabold leading-[1.1] tracking-tight text-foreground sm:text-4xl lg:text-5xl">
        {title}
      </h1>
      <p className="mt-5 max-w-2xl text-lg leading-relaxed text-muted-foreground">{description}</p>
      {actions && <div className="mt-7 flex flex-wrap gap-3">{actions}</div>}
    </div>
  );
  return (
    <section data-page-hero className="border-b border-border bg-background py-14 lg:py-20">
      {aside ? (
        <Container className="grid gap-10 lg:grid-cols-2 lg:items-center">
          {copy}
          <div className="min-w-0">{aside}</div>
        </Container>
      ) : (
        <Container>{copy}</Container>
      )}
    </section>
  );
}
