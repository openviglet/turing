import { ArrowRight, Check, Minus } from "lucide-react";
import { Container, SectionHeader, TableScroll } from "@/components/brand";
import { usePages } from "@/content/use-pages";
import { cn } from "@/lib/utils";
import { withBase } from "@/lib/site-url";

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

export function Comparison() {
  const { COMPARISON, COMPARISON_COLUMNS, COMPARISON_SECTION: t } = usePages();
  return (
    <section id="why" className="bg-muted/40 py-20">
      <Container>
        <SectionHeader
          eyebrow={t.eyebrow}
          title={t.title}
          description={t.description}
        />
        <TableScroll fadeClassName="from-muted">
          <table className="w-full min-w-[640px] border-collapse text-sm">
            <thead>
              <tr>
                <th className="sticky left-0 z-10 w-[150px] bg-background p-3 text-left font-semibold sm:w-2/5"></th>
                <th className="rounded-t-xl bg-primary/10 p-3 text-center font-extrabold text-primary">
                  {COMPARISON_COLUMNS.turing}
                </th>
                <th className="p-3 text-center font-semibold text-muted-foreground">
                  {COMPARISON_COLUMNS.saas}
                </th>
                <th className="p-3 text-center font-semibold text-muted-foreground">
                  {COMPARISON_COLUMNS.diy}
                </th>
              </tr>
            </thead>
            <tbody>
              {COMPARISON.map((row, i) => (
                <tr key={row.feature} className="border-t border-border">
                  <td className="sticky left-0 z-10 bg-background p-3 font-medium">
                    {row.feature}
                  </td>
                  <td
                    className={cn(
                      "p-3 text-center",
                      "bg-primary/5",
                      i === COMPARISON.length - 1 && "rounded-b-xl"
                    )}
                  >
                    <Cell value={row.turing} highlight />
                  </td>
                  <td className="p-3 text-center">
                    <Cell value={row.saas} />
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
          {t.note}
        </p>
        <div className="mt-8 flex flex-wrap justify-center gap-x-8 gap-y-3 text-center">
          <a
            href={withBase("/compare/")}
            className="inline-flex items-center gap-1.5 text-sm font-semibold text-primary transition-colors hover:text-primary/80"
          >
            {t.compareCta}
            <ArrowRight className="size-4" />
          </a>
          <a
            href={withBase("/migrate/")}
            className="inline-flex items-center gap-1.5 text-sm font-semibold text-primary transition-colors hover:text-primary/80"
          >
            {t.migrateCta}
            <ArrowRight className="size-4" />
          </a>
        </div>
      </Container>
    </section>
  );
}
