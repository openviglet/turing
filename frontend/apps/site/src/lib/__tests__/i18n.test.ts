import { describe, expect, it } from "vitest";

import { CHROME, PT_BR_PATHS, localeOf, localePath, splitLocale } from "@/lib/i18n";
import { localizedRoutes, render, routes } from "@/entry-server";
import { matchRoute, resolveRoute } from "@/route-match";

/** Every string reachable from a value, with the path that leads to it. */
function leaves(value: unknown, at = ""): Array<[string, string]> {
  if (typeof value === "string") return [[at, value]];
  if (value && typeof value === "object")
    return Object.entries(value).flatMap(([k, v]) => leaves(v, at ? `${at}.${k}` : k));
  return [];
}

describe("site languages", () => {
  it("routes /educacao/ to pt-BR, with or without the trailing slash, and the rest to English", () => {
    expect(localeOf("/educacao/")).toBe("pt-BR");
    expect(localeOf("/educacao")).toBe("pt-BR");
    expect(localeOf("/")).toBe("en");
    expect(localeOf("/wordpress/")).toBe("en");
  });

  it("reads pt-BR from the /br/ prefix and routes the path below it (T1376)", () => {
    expect(splitLocale("/br/")).toEqual({ locale: "pt-BR", path: "/" });
    expect(splitLocale("/br")).toEqual({ locale: "pt-BR", path: "/" });
    expect(splitLocale("/br/compare/")).toEqual({ locale: "pt-BR", path: "/compare/" });
    expect(splitLocale("/compare/")).toEqual({ locale: "en", path: "/compare/" });
    // A path that only starts with the same letters is English.
    expect(splitLocale("/brand/")).toEqual({ locale: "en", path: "/brand/" });
    expect(resolveRoute("/br/features/search/")).toEqual({ locale: "pt-BR", route: "/features/search/", slug: "search" });
    expect(matchRoute("/br/features/search/")).toBe(matchRoute("/features/search/"));
  });

  it("keeps a pt-BR link in Portuguese only where the target has a /br/ twin (T1376)", () => {
    const twins = ["/", "/compare/"];
    expect(localePath("/compare/", "pt-BR", twins)).toBe("/br/compare/");
    expect(localePath("/#features", "pt-BR", twins)).toBe("/br/#features");
    expect(localePath("/wordpress/", "pt-BR", twins)).toBe("/wordpress/");
    expect(localePath("/compare/", "en", twins)).toBe("/compare/");
    expect(localePath("https://example.org/", "pt-BR", twins)).toBe("https://example.org/");
    // Nothing has a twin yet, so no page links into an empty /br/ tree.
    expect(localePath("/compare/", "pt-BR")).toBe("/compare/");
  });

  it("prerenders a /br/ twin, in pt-BR, for each route that has one (T1376)", () => {
    const listed = localizedRoutes(
      [
        { path: "/", title: "Home", description: "d" },
        { path: "/compare/", title: "Compare", description: "d" },
      ],
      ["/compare/"]
    );
    expect(listed.map((r) => [r.path, r.lang])).toEqual([
      ["/", "en"],
      ["/compare/", "en"],
      ["/br/compare/", "pt-BR"],
    ]);
  });

  it("gives the chrome the page's language from the path, with no prop (T1376)", () => {
    const pt = render("/educacao/");
    expect(pt).toContain(CHROME["pt-BR"].footer.license);
    expect(pt).not.toContain(CHROME.en.footer.license);
    const en = render("/compare/");
    expect(en).toContain(CHROME.en.footer.license);
  });

  it("prerenders every pt-BR path, marked with its language", () => {
    for (const path of PT_BR_PATHS) {
      expect(routes.find((r) => r.path === path)?.lang).toBe("pt-BR");
    }
    expect(routes.find((r) => r.path === "/")?.lang).toBe("en");
  });

  it("translates every chrome string a pt-BR page shows", () => {
    // Names that read the same in both languages are the only ones allowed to match.
    const same = new Set(["nav.marketplace"]);
    const en = new Map(leaves(CHROME.en));
    const untranslated = leaves(CHROME["pt-BR"]).filter(([k, v]) => !same.has(k) && en.get(k) === v);
    expect(untranslated).toEqual([]);
  });
});
