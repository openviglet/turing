import { act, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { createTuringAnalytics, type TuringAnalyticsEvent } from "../../core/analytics";
import { googleAnalyticsSink } from "../../core/analytics-sinks";
import { clearTurSession, readTurSession } from "../../core/session";
import { useTuringSearch, type UseTuringSearchOptions } from "../use-turing-search";

/**
 * T1137 — the React search hook's analytics respect consent, skip as-you-type
 * searches, report search abandonment, and the GA sink sends GA4's
 * recommended events: the vanilla controller's behaviour (T1129, T1130).
 */

vi.mock("../../core/use-turing-context", () => ({
  useTuringContext: () => ({ config: { site: "s1", locale: "en_US" } }),
}));

vi.mock("../../core/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../core/api")>();
  return {
    ...actual,
    postStructuredSearch: vi.fn(async () => ({ queryContext: { count: 4 }, results: { document: [] } })),
    fetchChat: vi.fn(async () => null),
    postClick: vi.fn(async () => undefined),
  };
});

describe("useTuringSearch — analytics (T1137)", () => {
  afterEach(() => clearTurSession());

  let events: TuringAnalyticsEvent[];

  beforeEach(() => {
    events = [];
  });

  function setup(options: Omit<UseTuringSearchOptions, "analytics"> = {}) {
    const analytics = createTuringAnalytics({ sinks: [(e) => events.push(e)] });
    return renderHook(() => useTuringSearch(undefined, { analytics, ...options }));
  }

  const named = (name: string) => events.filter((e) => e.name === name);

  it("writes no cookie on mount", () => {
    setup();
    expect(readTurSession()).toBeNull();
  });

  it("emits nothing and writes no cookie until consent is given", async () => {
    let granted = false;
    const { result } = setup({ consent: () => granted });

    await act(() => result.current.searchQuery("boots"));
    expect(events).toHaveLength(0);
    expect(readTurSession()).toBeNull();

    granted = true;
    await act(() => result.current.searchQuery("boots"));
    expect(named("turing_search")[0]?.context.sessionId).toBe(readTurSession());
    expect(readTurSession()).not.toBeNull();
  });

  it("stamps no session id with sessionCookie: false", async () => {
    const { result } = setup({ sessionCookie: false });

    await act(() => result.current.searchQuery("boots"));

    expect(named("turing_search")[0]?.context.sessionId).toBeUndefined();
    expect(readTurSession()).toBeNull();
  });

  it("reports no search for an as-you-type request", async () => {
    const { result } = setup();

    await act(() => result.current.searchQuery("work", { mode: "instant" }));
    await act(() => result.current.searchQuery("workshop"));

    expect(named("turing_search")).toHaveLength(1);
    expect(named("turing_search_refined")).toHaveLength(0);
  });

  it("reports a settled search left without a click, once", async () => {
    const { result } = setup({ abandonment: true });

    await act(() => result.current.searchQuery("boots"));
    globalThis.dispatchEvent(new Event("pagehide"));
    globalThis.dispatchEvent(new Event("pagehide"));

    expect(named("turing_search_abandoned")).toHaveLength(1);
    expect(named("turing_search_abandoned")[0].params).toMatchObject({ query: "boots", results: 4 });
  });

  it("does not report abandonment after a result click", async () => {
    const { result } = setup({ abandonment: true });

    await act(() => result.current.searchQuery("boots"));
    act(() => result.current.trackResultClick("doc-1", 1));
    globalThis.dispatchEvent(new Event("pagehide"));

    expect(named("turing_search_abandoned")).toHaveLength(0);
  });

  it("sends GA4's recommended events when the sink asks for them", () => {
    const gtag = vi.fn();
    const analytics = createTuringAnalytics({ sinks: [googleAnalyticsSink({ gtag, recommended: true })] });

    analytics.emit("turing_search", { query: "boots", results: 4 });

    expect(gtag.mock.calls.map((c) => c[1])).toEqual(["turing_search", "search"]);
    expect(gtag.mock.calls[1][2]).toMatchObject({ search_term: "boots" });
  });
});
