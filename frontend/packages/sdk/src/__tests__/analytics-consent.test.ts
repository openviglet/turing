/**
 * @vitest-environment jsdom
 */
import { afterEach, describe, expect, it, vi } from "vitest";
import { createTuringAnalytics, type TuringAnalyticsEvent } from "../analytics";
import { createSearchController, type SearchControllerOptions } from "../controllers/search";
import { clearTurSession, readTurSession } from "../session";
import type { TuringClient } from "../client";
import type { TurSearchResponse } from "../types";

/**
 * T1129 — the search controller's analytics respect consent: no event and no
 * `TUR_SESSION` cookie before the visitor agrees, and an as-you-type search is
 * not reported as a search.
 */
describe("createSearchController — consent-aware analytics (T1129)", () => {
  afterEach(() => clearTurSession());

  function client(): TuringClient {
    const response: TurSearchResponse = {
      queryContext: { count: 3 } as TurSearchResponse["queryContext"],
      results: { document: [] },
      widget: {} as TurSearchResponse["widget"],
    };
    return {
      get: vi.fn(() => Promise.resolve(null)),
      post: vi.fn(() => Promise.resolve(response)),
    } as unknown as TuringClient;
  }

  function setup(options: Omit<SearchControllerOptions, "analytics"> = {}) {
    const events: TuringAnalyticsEvent[] = [];
    const analytics = createTuringAnalytics({ sinks: [(e) => events.push(e)] });
    const search = createSearchController(client(), { site: "s1", locale: "en_US" }, undefined, {
      analytics,
      chat: false,
      ...options,
    });
    return { events, search };
  }

  it("writes no cookie at construction", () => {
    setup();
    expect(readTurSession()).toBeNull();
  });

  it("emits nothing and writes no cookie until consent is given", async () => {
    let granted = false;
    const { events, search } = setup({ consent: () => granted });

    await search.searchQuery("boots");
    search.trackResultClick("doc-1", 1);
    expect(events).toHaveLength(0);
    expect(readTurSession()).toBeNull();

    granted = true;
    await search.searchQuery("boots");
    const ev = events.find((e) => e.name === "turing_search");
    expect(ev?.context.sessionId).toBe(readTurSession());
    expect(readTurSession()).not.toBeNull();
  });

  it("stamps no session id with sessionCookie: false", async () => {
    const { events, search } = setup({ sessionCookie: false });

    await search.searchQuery("boots");

    expect(events.find((e) => e.name === "turing_search")?.context.sessionId).toBeUndefined();
    expect(readTurSession()).toBeNull();
  });

  it("reports no search for an as-you-type request", async () => {
    const { events, search } = setup();

    await search.searchQuery("work", { mode: "instant" });
    await search.searchQuery("works", { mode: "instant" });
    await search.searchQuery("workshop");

    expect(events.filter((e) => e.name === "turing_search")).toHaveLength(1);
    expect(events.some((e) => e.name === "turing_search_refined")).toBe(false);
  });
});
