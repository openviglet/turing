import { describe, expect, it, vi } from "vitest";
import { fetchSimilar, fetchStoredDocuments } from "../api.js";
import { createSimilarController } from "../controllers/similar.js";
import type { TuringClient } from "../client.js";

/**
 * T1132 — the similar-documents call takes the search's filter queries, so a
 * related-content rail asks for exactly the neighbours it shows. T1239 — it sends
 * them as a JSON body to `POST /search/similar/query`, like a search.
 */
describe("similar documents with filters (T1132)", () => {
  function client() {
    const get = vi.fn().mockResolvedValue([]);
    const post = vi.fn().mockResolvedValue([]);
    return { get, post, client: { get, post } as unknown as TuringClient };
  }

  it("T1239 - posts the request as JSON to /search/similar/query without CSRF", async () => {
    const { post, client: c } = client();

    await fetchSimilar(c, "shared", { id: "post-7", rows: 6, fq: ["type:post", "site_key:a.example.org"] });

    expect(post.mock.calls[0][0]).toBe("/sn/shared/search/similar/query");
    expect(post.mock.calls[0][1]).toEqual({
      id: "post-7",
      rows: 6,
      fq: ["type:post", "site_key:a.example.org"],
    });
    expect(post.mock.calls[0][2]).toMatchObject({ csrf: false });
  });

  it("T1239 - keeps the comma of a single filter value", async () => {
    const { post, client: c } = client();

    await fetchSimilar(c, "shared", { id: "post-7", fq: ["type:a,b"] });

    expect(post.mock.calls[0][1]).toMatchObject({ fq: ["type:a,b"] });
  });

  it("sends the locale as the body's locale, and fetchStoredDocuments as _setlocale", async () => {
    const { get, post, client: c } = client();

    await fetchSimilar(c, "shared", { id: "post-7", locale: "pt_BR" });
    await fetchStoredDocuments(c, "shared", ["post-7"], "pt_BR");

    expect(post.mock.calls[0][1]).toMatchObject({ locale: "pt_BR" });
    const url = new URL(get.mock.calls[0][0] as string, "https://turing.example");
    expect(url.searchParams.get("_setlocale")).toBe("pt_BR");
  });

  it("T1235 - sends the filter operators, fqItemOperator included", async () => {
    const { post, client: c } = client();

    await fetchSimilar(c, "shared", {
      id: "post-7",
      fq: ["site_key:a", "site_key:b"],
      fqAnd: ["type:post"],
      fqOr: ["lang:en"],
      fqOperator: "AND",
      fqItemOperator: "OR",
    });

    expect(post.mock.calls[0][1]).toEqual({
      id: "post-7",
      fq: ["site_key:a", "site_key:b"],
      fqAnd: ["type:post"],
      fqOr: ["lang:en"],
      fqOperator: "AND",
      fqItemOperator: "OR",
    });
  });

  it("sends no filter when none is given", async () => {
    const { post, client: c } = client();

    await fetchSimilar(c, "shared", { id: "post-7" });

    expect(post.mock.calls[0][1]).toEqual({ id: "post-7" });
  });

  it("carries the controller's default filters into each load", async () => {
    const { post, client: c } = client();
    const similar = createSimilarController(c, "shared", { fq: ["type:post"] });

    await similar.load("post-7");

    expect(post.mock.calls[0][1]).toMatchObject({ fq: ["type:post"] });
  });
});
