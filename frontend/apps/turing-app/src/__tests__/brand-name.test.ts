import { describe, expect, it } from "vitest";

/**
 * T1186 — the product is "Viglet Turing", then "Turing": the acronym was dropped
 * on 2026-10-17, and a renamed download that logs into "Viglet Turing ES" reads
 * as another product. This keeps the acronym out of what the console renders:
 * the locale files whole, and the source lines that are not comments.
 */
const sources = import.meta.glob(["../**/*.{ts,tsx}", "!../**/__tests__/**"], {
  query: "?raw",
  import: "default",
  eager: true,
}) as Record<string, string>;

const locales = import.meta.glob("../i18n/locales/**/*.json", {
  query: "?raw",
  import: "default",
  eager: true,
}) as Record<string, string>;

const ACRONYM = /Turing ES\b/;

function isComment(line: string): boolean {
  const t = line.trimStart();
  return t.startsWith("*") || t.startsWith("/*") || t.startsWith("//");
}

function offenders(files: Record<string, string>, skipComments: boolean): string[] {
  const found: string[] = [];
  for (const [path, text] of Object.entries(files)) {
    text.split("\n").forEach((line, i) => {
      if (ACRONYM.test(line) && !(skipComments && isComment(line))) {
        found.push(`${path}:${i + 1}: ${line.trim()}`);
      }
    });
  }
  return found;
}

describe("brand name (T1186)", () => {
  it("scans the console's sources and locale files", () => {
    expect(Object.keys(sources).length).toBeGreaterThan(100);
    expect(Object.keys(locales).length).toBeGreaterThan(10);
  });

  it("never renders the dropped acronym", () => {
    expect(offenders(sources, true)).toEqual([]);
    expect(offenders(locales, false)).toEqual([]);
  });

  it("catches the acronym in a rendered line and leaves comments alone", () => {
    expect(offenders({ "a.tsx": "<Login.Title>Viglet Turing ES</Login.Title>" }, true)).toHaveLength(1);
    expect(offenders({ "a.tsx": " * Built for Viglet Turing ES." }, true)).toEqual([]);
    expect(offenders({ "a.tsx": "<Title>Viglet Turing</Title>" }, true)).toEqual([]);
  });
});
