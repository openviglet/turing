import { describe, expect, it } from "vitest";

import { alternatesOf, sitemapXml } from "@/lib/alternates";
import { localizedRoutes, routes, sitemap } from "@/entry-server";

const ORIGIN = "https://turing.viglet.org";

describe("hreflang alternates and the sitemap (T1379)", () => {
  const listed = localizedRoutes(
    [
      { path: "/", title: "Home", description: "d" },
      { path: "/compare/", title: "Compare", description: "d" },
      { path: "/educacao/", title: "Educação", description: "d" },
    ],
    ["/compare/"]
  );

  it("gives a page and its /br/ twin the same alternates, x-default being the English one", () => {
    const english = alternatesOf("/compare/", listed);
    expect(english).toEqual([
      { hreflang: "en", href: `${ORIGIN}/compare/` },
      { hreflang: "pt-BR", href: `${ORIGIN}/br/compare/` },
      { hreflang: "x-default", href: `${ORIGIN}/compare/` },
    ]);
    expect(alternatesOf("/br/compare/", listed)).toEqual(english);
  });

  it("lists a page with no twin as itself and x-default, in its own language", () => {
    expect(alternatesOf("/", listed)).toEqual([
      { hreflang: "en", href: `${ORIGIN}/` },
      { hreflang: "x-default", href: `${ORIGIN}/` },
    ]);
    expect(alternatesOf("/educacao/", listed)).toEqual([
      { hreflang: "pt-BR", href: `${ORIGIN}/educacao/` },
      { hreflang: "x-default", href: `${ORIGIN}/educacao/` },
    ]);
  });

  it("writes every route to the sitemap with its alternates, and the other jobs' pages", () => {
    const xml = sitemapXml(listed, [{ path: "/marketplace/vinyl-records/", priority: "0.6" }]);
    expect(xml).toContain('xmlns:xhtml="http://www.w3.org/1999/xhtml"');
    expect(xml).toContain(`<loc>${ORIGIN}/br/compare/</loc>`);
    expect(xml).toContain(`<xhtml:link rel="alternate" hreflang="pt-BR" href="${ORIGIN}/br/compare/" />`);
    expect(xml).toContain(`<loc>${ORIGIN}/marketplace/vinyl-records/</loc>`);
  });

  it("lists every prerendered route in the site's own sitemap", () => {
    const xml = sitemap();
    for (const r of routes) expect(xml).toContain(`<loc>${ORIGIN}${r.path}</loc>`);
  });
});
