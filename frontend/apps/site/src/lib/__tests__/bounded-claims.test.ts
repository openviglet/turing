import { describe, expect, it } from "vitest";

import * as siteContent from "@/lib/site-content";
import llmsTxt from "../../../public/llms.txt?raw";
import aemSearchMd from "../../../public/aem-search.md?raw";
import personaMd from "../../../public/persona.md?raw";
import ogSvg from "../../../public/og.svg?raw";

/**
 * T1272 — the hub's rule against invented or universal claims. The site names
 * the providers and engines it supports instead of "any LLM" / "any engine",
 * says where data is stored instead of promising it "never leaves" (an external
 * model provider receives the prompt), and makes no delivery-time promise.
 */
const UNIVERSAL = [
  /\bany (LLM|search engine|engine)\b/i,
  /\bnever leaves?\b/i,
  /\bin (days|minutes)\b|not quarters|week engagement/i,
  /Time to first result/i,
  /Azure OpenAI/i,
];

function strings(value: unknown, out: string[] = []): string[] {
  if (typeof value === "string") out.push(value);
  else if (Array.isArray(value)) value.forEach((v) => strings(v, out));
  else if (value && typeof value === "object" && Object.getPrototypeOf(value) === Object.prototype)
    Object.values(value).forEach((v) => strings(v, out));
  return out;
}

describe("bounded claims (T1272)", () => {
  it("site content makes no universal or delivery-time claim", () => {
    const hits = strings(siteContent).filter((s) => UNIVERSAL.some((re) => re.test(s)));
    expect(hits).toEqual([]);
  });

  it.each([
    ["llms.txt", llmsTxt],
    ["aem-search.md", aemSearchMd],
    ["persona.md", personaMd],
    ["og.svg", ogSvg],
  ])("public/%s makes no universal or delivery-time claim", (_file, text) => {
    const hits = text.split("\n").filter((line: string) => UNIVERSAL.some((re) => re.test(line)));
    expect(hits).toEqual([]);
  });

  it("names the supported search engines where it speaks of engines", () => {
    expect(llmsTxt).toMatch(/Solr, Elasticsearch or embedded Lucene/);
  });
});
