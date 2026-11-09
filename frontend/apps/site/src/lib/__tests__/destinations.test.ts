import { describe, expect, it } from "vitest";

import { DESTINATIONS, destinationHref, type DestinationKey } from "@/lib/destinations";
import llmsTxt from "../../../public/llms.txt?raw";
import aemSearchMd from "../../../public/aem-search.md?raw";

/** The docs pages, by their URL path ("/docs/getting-started/quick-start/"). */
const docsPages = new Set(
  Object.keys(import.meta.glob("../../../../docs/src/content/docs/**/*.{md,mdx}")).map((file) => {
    const slug = file
      .replace(/^.*\/src\/content\/docs\//, "")
      .replace(/\.mdx?$/, "")
      .replace(/(^|\/)index$/, "");
    return slug ? `/docs/${slug}/` : "/docs/";
  })
);

const keys = Object.keys(DESTINATIONS) as DestinationKey[];

/** T1270 — one typed map for the visitor's journey, with no retired or dangling target. */
describe("destinations", () => {
  it("never points at the retired docs.viglet.org domain", () => {
    for (const key of keys) expect(destinationHref(key, (p) => p)).not.toMatch(/docs\.viglet\.org/);
    for (const text of [llmsTxt, aemSearchMd]) expect(text).not.toMatch(/docs\.viglet\.org/);
  });

  it("leaves the site only over https", () => {
    for (const d of Object.values(DESTINATIONS)) if ("url" in d) expect(d.url).toMatch(/^https:\/\//);
  });

  it("every docs path is a page the docs app builds", () => {
    for (const d of Object.values(DESTINATIONS))
      if ("path" in d && d.path.startsWith("/docs/")) expect(docsPages, d.path).toContain(d.path);
  });

  it("Get started is the actionable setup path, not the docs overview", () => {
    expect(destinationHref("getStarted", (p) => p)).toBe("/docs/getting-started/quick-start/");
    expect(destinationHref("getStarted", (p) => p)).not.toBe(destinationHref("docs", (p) => p));
  });

  it("the static machine-readable files use the map's AEM connector URL", () => {
    expect(llmsTxt).toContain(DESTINATIONS.aemConnector.url);
    expect(aemSearchMd).toContain(DESTINATIONS.aemConnector.url);
  });

  it("release notes and the development roadmap stay separate destinations", () => {
    expect(DESTINATIONS.releaseNotes.url).toMatch(/^https:\/\/www\.viglet\.org\/turing\/release-notes\/$/);
    expect(DESTINATIONS.devRoadmap.url).not.toBe(DESTINATIONS.releaseNotes.url);
  });
});
