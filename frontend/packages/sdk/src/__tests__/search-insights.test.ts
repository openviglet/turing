import { describe, expect, it, vi } from "vitest";
import { fetchContentGaps, fetchFailedSearches } from "../api";
import type { TuringClient } from "../client";
import type { TurSearchInsightReport } from "../types";

/**
 * T1119 — failed searches and content gaps are REST metrics a connector reads
 * with its key, by site id, in one window shape.
 */
describe("search insights (T1119)", () => {
  const report: TurSearchInsightReport = {
    from: null,
    to: "2026-10-24T00:00:00Z",
    terms: [{ term: "php 9", searches: 4, avgResults: 0, lastSeen: "2026-10-20T10:00:00Z" }],
  };

  function client() {
    const get = vi.fn().mockResolvedValue(report);
    return { get, client: { get } as unknown as TuringClient };
  }

  it("reads the failed searches by site id with no window by default", async () => {
    const { get, client: c } = client();

    await expect(fetchFailedSearches(c, "site-1")).resolves.toEqual(report);

    expect(get.mock.calls[0][0]).toBe("/sn/site-1/metrics/failed-searches");
  });

  it("sends the window as query parameters", async () => {
    const { get, client: c } = client();

    await fetchContentGaps(c, "site 1", {
      period: "this-week",
      from: "2026-10-01T00:00:00Z",
      to: "2026-10-24T00:00:00Z",
      rows: 5,
    });

    expect(get.mock.calls[0][0]).toBe(
      "/sn/site%201/metrics/content-gaps?period=this-week&from=2026-10-01T00%3A00%3A00Z"
        + "&to=2026-10-24T00%3A00%3A00Z&rows=5",
    );
  });
});
