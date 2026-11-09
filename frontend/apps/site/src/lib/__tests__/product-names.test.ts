import { describe, expect, it } from "vitest";

import * as siteContent from "@/lib/site-content";
import { PRODUCTS } from "@/components/products";
import llmsTxt from "../../../public/llms.txt?raw";
import aemSearchMd from "../../../public/aem-search.md?raw";
import personaMd from "../../../public/persona.md?raw";

/**
 * T1268 — the hub's naming decision: every visitor-facing mention is the full
 * name (Viglet Turing, Viglet Dumont, Viglet Shio), repeated mentions included.
 * A bare name is one not preceded by "Viglet "; lowercase technical identifiers
 * (`turing-sdk`, `turing-agent:<id>`, URLs) never match the capitalised word.
 */
const BARE = /(?<!Viglet )\b(Turing|Dumont|Shio)\b/;

/** Every string reachable from a value, with the path that leads to it. */
function strings(value: unknown, at: string, out: Array<[string, string]>) {
  if (typeof value === "string") out.push([at, value]);
  else if (Array.isArray(value)) value.forEach((v, i) => strings(v, `${at}[${i}]`, out));
  else if (value && typeof value === "object" && Object.getPrototypeOf(value) === Object.prototype)
    for (const [k, v] of Object.entries(value)) strings(v, `${at}.${k}`, out);
  return out;
}

describe("product names (T1268)", () => {
  it("each product identity is its full Viglet name", () => {
    for (const p of Object.values(PRODUCTS)) expect(p.fullName).toMatch(/^Viglet (Turing|Dumont|Shio)$/);
  });

  it("site content never uses a bare product name", () => {
    const bare = strings(siteContent, "site-content", []).filter(([, s]) => BARE.test(s));
    expect(bare).toEqual([]);
  });

  it.each([
    ["llms.txt", llmsTxt],
    ["aem-search.md", aemSearchMd],
    ["persona.md", personaMd],
  ])("public/%s never uses a bare product name", (_file, text) => {
    const bare = text.split("\n").filter((line: string) => BARE.test(line));
    expect(bare).toEqual([]);
  });
});
