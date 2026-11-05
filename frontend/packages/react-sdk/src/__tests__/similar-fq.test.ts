import axios from "axios";
import { afterEach, describe, expect, it, vi } from "vitest";

import { fetchSimilar } from "../core/api.js";

/**
 * T1235 — the React SDK's similar-documents call sends the search's filters,
 * which it used to drop: `fq[]`, `fq.and[]`, `fq.or[]`, `fq.op` and `fqi.op`.
 */
describe("fetchSimilar filters (T1235)", () => {
  afterEach(() => vi.restoreAllMocks());

  function stubGet() {
    return vi.spyOn(axios, "get").mockResolvedValue({ data: [] });
  }

  it("sends every filter parameter, fqi.op included", async () => {
    const get = stubGet();

    await fetchSimilar("shared", {
      id: "post-7",
      fq: ["site_key:a", "site_key:b"],
      fqAnd: ["type:post"],
      fqOr: ["lang:en"],
      fqOperator: "AND",
      fqItemOperator: "OR",
    });

    const url = new URL(get.mock.calls[0][0] as string, "https://turing.example");
    expect(url.pathname).toBe("/sn/shared/search/similar");
    expect(url.searchParams.getAll("fq[]")).toEqual(["site_key:a", "site_key:b"]);
    expect(url.searchParams.getAll("fq.and[]")).toEqual(["type:post"]);
    expect(url.searchParams.getAll("fq.or[]")).toEqual(["lang:en"]);
    expect(url.searchParams.get("fq.op")).toBe("AND");
    expect(url.searchParams.get("fqi.op")).toBe("OR");
  });

  it("sends no filter when none is given", async () => {
    const get = stubGet();

    await fetchSimilar("shared", { id: "post-7" });

    expect(get.mock.calls[0][0]).not.toContain("fq");
  });
});
