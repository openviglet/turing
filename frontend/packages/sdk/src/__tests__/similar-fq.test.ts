import { describe, expect, it, vi } from "vitest";
import { fetchSimilar, fetchStoredDocuments } from "../api";
import { createSimilarController } from "../controllers/similar";
import type { TuringClient } from "../client";

/**
 * T1132 — the similar-documents call takes the search's filter queries, so a
 * related-content rail asks for exactly the neighbours it shows.
 */
describe("similar documents with filters (T1132)", () => {
  function client() {
    const get = vi.fn().mockResolvedValue([]);
    return { get, client: { get } as unknown as TuringClient };
  }

  it("sends each filter query as fq[]", async () => {
    const { get, client: c } = client();

    await fetchSimilar(c, "shared", { id: "post-7", rows: 6, fq: ["type:post", "site_key:a.example.org"] });

    const url = new URL(get.mock.calls[0][0] as string, "https://turing.example");
    expect(url.pathname).toBe("/sn/shared/search/similar");
    expect(url.searchParams.getAll("fq[]")).toEqual(["type:post", "site_key:a.example.org"]);
  });

  it("T1140 - sends the locale as _setlocale, the name the server reads", async () => {
    const { get, client: c } = client();

    await fetchSimilar(c, "shared", { id: "post-7", locale: "pt_BR" });
    await fetchStoredDocuments(c, "shared", ["post-7"], "pt_BR");

    for (const call of get.mock.calls) {
      const url = new URL(call[0] as string, "https://turing.example");
      expect(url.searchParams.get("_setlocale")).toBe("pt_BR");
      expect(url.searchParams.has("locale")).toBe(false);
    }
  });

  it("sends no filter when none is given", async () => {
    const { get, client: c } = client();

    await fetchSimilar(c, "shared", { id: "post-7" });

    expect(get.mock.calls[0][0]).not.toContain("fq");
  });

  it("carries the controller's default filters into each load", async () => {
    const { get, client: c } = client();
    const similar = createSimilarController(c, "shared", { fq: ["type:post"] });

    await similar.load("post-7");

    const url = new URL(get.mock.calls[0][0] as string, "https://turing.example");
    expect(url.searchParams.getAll("fq[]")).toEqual(["type:post"]);
  });
});
