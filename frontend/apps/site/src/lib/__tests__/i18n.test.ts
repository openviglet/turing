import { describe, expect, it } from "vitest";

import { CHROME, PT_BR_PATHS, localeOf } from "@/lib/i18n";
import { routes } from "@/entry-server";

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
