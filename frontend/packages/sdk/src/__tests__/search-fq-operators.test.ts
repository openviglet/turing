import { describe, expect, it, vi } from "vitest";
import { fetchSearch } from "../api.js";
import type { TuringClient } from "../client.js";

/**
 * T1223 — the GET search sends the filter operators `SearchParams` types, so a
 * client can ask for one field's values as alternatives without the POST form.
 */
describe("GET search filter operators (T1223)", () => {
  function client() {
    const get = vi.fn().mockResolvedValue({});
    return { get, client: { get } as unknown as TuringClient };
  }

  it("sends fqItemOperator as fqi.op beside the fq[] values", async () => {
    const { get, client: c } = client();

    await fetchSearch(c, "shared", {
      q: "*",
      fq: ["site_key:a.example.org", "site_key:b.example.org"],
      fqItemOperator: "OR",
    });

    const url = new URL(get.mock.calls[0][0] as string, "https://turing.example");
    expect(url.pathname).toBe("/sn/shared/search");
    expect(url.searchParams.getAll("fq[]")).toEqual(["site_key:a.example.org", "site_key:b.example.org"]);
    expect(url.searchParams.get("fqi.op")).toBe("OR");
  });

  it("sends fqAnd, fqOr and fqOperator under the names the endpoint reads", async () => {
    const { get, client: c } = client();

    await fetchSearch(c, "shared", {
      q: "news",
      fqAnd: ["type:post"],
      fqOr: ["lang:en", "lang:pt"],
      fqOperator: "AND",
    });

    const url = new URL(get.mock.calls[0][0] as string, "https://turing.example");
    expect(url.searchParams.getAll("fq.and[]")).toEqual(["type:post"]);
    expect(url.searchParams.getAll("fq.or[]")).toEqual(["lang:en", "lang:pt"]);
    expect(url.searchParams.get("fq.op")).toBe("AND");
  });

  it("sends no operator when none is set", async () => {
    const { get, client: c } = client();

    await fetchSearch(c, "shared", { q: "*", fq: ["type:post"] });

    const url = new URL(get.mock.calls[0][0] as string, "https://turing.example");
    expect(url.searchParams.has("fqi.op")).toBe(false);
    expect(url.searchParams.has("fq.op")).toBe(false);
    expect(url.searchParams.has("fq.and[]")).toBe(false);
    expect(url.searchParams.has("fq.or[]")).toBe(false);
  });
});
