import { useState } from "react";
import { Check, Copy } from "lucide-react";
import { cn } from "@/lib/utils";

/** A copy-paste command line with a copy button. */
export function CopyCommand({
  command,
  className,
}: {
  command: string;
  className?: string;
}) {
  const [copied, setCopied] = useState(false);

  const onCopy = () => {
    navigator.clipboard
      ?.writeText(command)
      .then(() => {
        setCopied(true);
        window.setTimeout(() => setCopied(false), 1500);
      })
      .catch(() => {
        /* clipboard blocked: no-op */
      });
  };

  return (
    <div
      className={cn(
        // inline-flex keeps it a compact pill on desktop; max-w-full caps it to
        // the parent so a long command can't push the page wider on mobile; the
        // command itself scrolls horizontally instead (min-w-0 lets it shrink).
        // T1269: theme tokens, not a dark-only pill, so it reads on the
        // neutral hero in both themes.
        "inline-flex max-w-full items-center gap-3 rounded-lg border border-border bg-muted px-4 py-2.5 font-mono text-sm text-foreground",
        className
      )}
    >
      <span className="shrink-0 select-none text-primary">$</span>
      <code className="min-w-0 overflow-x-auto whitespace-nowrap">{command}</code>
      <button
        type="button"
        onClick={onCopy}
        aria-label="Copy command"
        className="ml-1 shrink-0 rounded-md p-1 text-muted-foreground transition-colors hover:bg-background hover:text-foreground"
      >
        {copied ? (
          <Check className="size-4 text-emerald-600 dark:text-emerald-400" />
        ) : (
          <Copy className="size-4" />
        )}
      </button>
    </div>
  );
}
