/**
 * @vitest-environment jsdom
 */
import { afterEach, describe, expect, it, vi } from "vitest";
import { createTuringAnalytics, type TuringAnalyticsEvent } from "../analytics";
import { googleAnalyticsSink } from "../analytics-sinks";
import { createSearchController, type SearchControllerOptions } from "../controllers/search";
import { clearTurSession } from "../session";
import type { TuringClient } from "../client";
import type { TurSearchResponse } from "../types";

/**
 * T1130 — GA4 recommended events from the sink, and the search abandonment
 * event: searched, saw results, opened none and left.
 */
describe("GA4 recommended events (T1130)", () => {
  function emitThrough(recommended: boolean) {
    const gtag = vi.fn();
    const analytics = createTuringAnalytics({
      sinks: [googleAnalyticsSink({ gtag, recommended })],
      context: { site: "s1" },
    });
    analytics.emit("turing_search", { query: "boots", results: 4 });
    analytics.emit("turing_search_result_click", { query: "boots", document_id: "doc-9", position: 3 });
    return gtag;
  }

  it("sends only the canonical names by default", () => {
    const gtag = emitThrough(false);
    expect(gtag.mock.calls.map((c) => c[1])).toEqual(["turing_search", "turing_search_result_click"]);
  });

  it("also sends search and select_content when recommended is on", () => {
    const gtag = emitThrough(true);

    expect(gtag.mock.calls.map((c) => c[1])).toEqual([
      "turing_search",
      "search",
      "turing_search_result_click",
      "select_content",
    ]);
    expect(gtag.mock.calls[1][2]).toMatchObject({ search_term: "boots", site: "s1" });
    expect(gtag.mock.calls[3][2]).toMatchObject({
      content_type: "search_result",
      content_id: "doc-9",
      index: 3,
    });
  });

  it("sends the search abandonment through the beacon transport", () => {
    const gtag = vi.fn();
    const analytics = createTuringAnalytics({ sinks: [googleAnalyticsSink({ gtag })] });
    analytics.emit("turing_search_abandoned", { query: "boots", results: 4, reason: "unload" });
    expect(gtag.mock.calls[0][2]).toMatchObject({ transport_type: "beacon" });
  });
});

describe("createSearchController — search abandonment (T1130)", () => {
  afterEach(() => clearTurSession());

  function setup(options: Omit<SearchControllerOptions, "analytics"> = {}) {
    const response: TurSearchResponse = {
      queryContext: { count: 4 } as TurSearchResponse["queryContext"],
      results: { document: [] },
      widget: {} as TurSearchResponse["widget"],
    };
    const client = {
      get: vi.fn(() => Promise.resolve(null)),
      post: vi.fn(() => Promise.resolve(response)),
    } as unknown as TuringClient;
    const events: TuringAnalyticsEvent[] = [];
    const analytics = createTuringAnalytics({ sinks: [(e) => events.push(e)] });
    const search = createSearchController(client, { site: "s1", locale: "en_US" }, undefined, {
      analytics,
      chat: false,
      ...options,
    });
    const abandoned = () => events.filter((e) => e.name === "turing_search_abandoned");
    return { search, abandoned };
  }

  function leave(): void {
    globalThis.dispatchEvent(new Event("pagehide"));
  }

  it("is off by default", async () => {
    const { search, abandoned } = setup();
    await search.searchQuery("boots");
    leave();
    expect(abandoned()).toHaveLength(0);
  });

  it("fires once when the visitor leaves a settled search without a click", async () => {
    const { search, abandoned } = setup({ abandonment: true });
    await search.searchQuery("boots");
    leave();
    leave();

    expect(abandoned()).toHaveLength(1);
    expect(abandoned()[0].params).toMatchObject({ query: "boots", results: 4, reason: "unload" });
  });

  it("does not fire after a result click", async () => {
    const { search, abandoned } = setup({ abandonment: true });
    await search.searchQuery("boots");
    search.trackResultClick("doc-9", 1);
    leave();
    expect(abandoned()).toHaveLength(0);
  });

  it("reports the search the visitor left, not an as-you-type one", async () => {
    const { search, abandoned } = setup({ abandonment: true });
    await search.searchQuery("boots");
    await search.searchQuery("bootsx", { mode: "instant" });
    leave();
    expect(abandoned().map((e) => e.params.query)).toEqual(["boots"]);
  });
});

/** T1155 — a filter, sort or page change on the same query is not another search. */
describe("createSearchController — refinements (T1155)", () => {
  afterEach(() => {
    clearTurSession();
    vi.useRealTimers();
  });

  const facet = { action: "ADD", field: "type", filterQuery: "type:article" } as const;

  function setup(options: Omit<SearchControllerOptions, "analytics"> = {}, postNative = true) {
    const response: TurSearchResponse = {
      queryContext: { count: 4 } as TurSearchResponse["queryContext"],
      results: { document: [] },
      widget: {} as TurSearchResponse["widget"],
    };
    const client = {
      get: vi.fn(() => Promise.resolve(response)),
      post: vi.fn(() => Promise.resolve(response)),
    } as unknown as TuringClient;
    const events: TuringAnalyticsEvent[] = [];
    const analytics = createTuringAnalytics({ sinks: [(e) => events.push(e)] });
    const search = createSearchController(client, { site: "s1", locale: "en_US" }, undefined, {
      analytics,
      chat: false,
      postNative,
      ...options,
    });
    const named = (name: string) => events.filter((e) => e.name === name);
    return { search, named };
  }

  it("reports one search for a facet, a sort and a page after it", async () => {
    const { search, named } = setup();
    await search.searchQuery("workshop");
    await search.toggleFacet(facet);
    await search.changeSort("newest");
    await search.goToPage(2);
    await search.searchRequest({ ...search.getState().request, fq: [] });

    expect(named("turing_search")).toHaveLength(1);
  });

  it("reports one search on the legacy GET path too", async () => {
    const { search, named } = setup({}, false);
    await search.searchQuery("workshop");
    await search.changeSort("newest");
    await search.goToPage(2);

    expect(named("turing_search")).toHaveLength(1);
  });

  it("reports the same term again after a wildcard search", async () => {
    const { search, named } = setup();
    await search.searchQuery("workshop");
    await search.searchQuery("*");
    await search.searchQuery("workshop");

    expect(named("turing_search")).toHaveLength(2);
  });

  it("keeps one abandonment watcher across a refinement, with its count and idle timer", async () => {
    vi.useFakeTimers();
    const { search, named } = setup({ abandonment: { idleMs: 1000 } });
    await search.searchQuery("workshop");
    vi.advanceTimersByTime(800);
    await search.toggleFacet(facet);
    vi.advanceTimersByTime(800);
    expect(named("turing_search_abandoned")).toHaveLength(0);

    vi.advanceTimersByTime(200);
    expect(named("turing_search_abandoned")).toHaveLength(1);
    expect(named("turing_search_abandoned")[0].params).toMatchObject({ query: "workshop", reason: "idle" });
  });
});
