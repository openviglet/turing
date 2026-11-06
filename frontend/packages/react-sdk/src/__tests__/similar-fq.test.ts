import axios from "axios";
import { afterEach, describe, expect, it, vi } from "vitest";

import { fetchSimilar } from "../core/api.js";

/**
 * T1235 — the React SDK's similar-documents call sends the search's filters.
 * T1239 — as the JSON body of `POST /search/similar/query`, built by the vanilla
 * SDK's `similarRequestBody`.
 */
describe("fetchSimilar filters (T1235)", () => {
  afterEach(() => vi.restoreAllMocks());

  function stubPost() {
    return vi.spyOn(axios, "post").mockResolvedValue({ data: [] });
  }

  it("posts every filter field, fqItemOperator included", async () => {
    const post = stubPost();

    await fetchSimilar("shared", {
      id: "post-7",
      fq: ["site_key:a", "site_key:b"],
      fqAnd: ["type:post"],
      fqOr: ["lang:en"],
      fqOperator: "AND",
      fqItemOperator: "OR",
    });

    expect(post.mock.calls[0][0]).toBe("/sn/shared/search/similar/query");
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
    const post = stubPost();

    await fetchSimilar("shared", { id: "post-7" });

    expect(post.mock.calls[0][1]).toEqual({ id: "post-7" });
  });
});
