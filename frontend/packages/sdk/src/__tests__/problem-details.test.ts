import { afterEach, describe, expect, it, vi } from "vitest";

import { createTuringClient, TuringHttpError } from "../index.js";

/**
 * T1234 — a non-2xx answer keeps the RFC 7807 problem details Turing explained
 * it with, so a spent AI budget (429 `quota-exhausted`) tells a page when
 * answers come back instead of only "429".
 */
const BASE = "https://turing.example/api";
const QUOTA = {
  type: "https://turing.viglet.org/problems/quota-exhausted",
  title: "AI quota exhausted",
  status: 429,
  detail: "The site's AI budget is spent until 2026-12-01.",
  used: 500,
  limit: 500,
  resetsAt: "2026-12-01T00:00:00Z",
  upgradeUrl: "https://turing.example/billing",
};

function clientAnswering(response: () => Response) {
  vi.stubGlobal("fetch", vi.fn(async () => response()) as unknown as typeof fetch);
  return createTuringClient({ baseURL: BASE });
}

afterEach(() => vi.unstubAllGlobals());

describe("TuringHttpError problem details (T1234)", () => {
  it("carries a quota-exhausted body, resetsAt included", async () => {
    const client = clientAnswering(() => new Response(JSON.stringify(QUOTA), {
      status: 429,
      headers: { "Content-Type": "application/problem+json" },
    }));

    const err = await client.post("/sn/site/copilot", { q: "x" }, { csrf: false }).catch((e: unknown) => e);

    expect(err).toBeInstanceOf(TuringHttpError);
    const problem = (err as TuringHttpError).problem;
    expect(problem?.type).toMatch(/quota-exhausted$/);
    expect(problem?.resetsAt).toBe("2026-12-01T00:00:00Z");
    expect(problem?.upgradeUrl).toBe("https://turing.example/billing");
    expect((err as TuringHttpError).message).toContain("budget is spent");
  });

  it("keeps extension members it has no type for", async () => {
    const client = clientAnswering(() => new Response(JSON.stringify({ title: "Bad", field: "q" }), {
      status: 400,
      headers: { "Content-Type": "application/json" },
    }));

    const err = (await client.get("/sn/site/search").catch((e: unknown) => e)) as TuringHttpError;

    expect(err.problem?.["field"]).toBe("q");
  });

  it("has no problem when the body is not JSON", async () => {
    const client = clientAnswering(() => new Response("<html>Bad gateway</html>", {
      status: 502,
      headers: { "Content-Type": "text/html" },
    }));

    const err = (await client.get("/sn/site/search").catch((e: unknown) => e)) as TuringHttpError;

    expect(err.status).toBe(502);
    expect(err.problem).toBeUndefined();
  });

  it("has no problem when a JSON body does not parse", async () => {
    const client = clientAnswering(() => new Response("{not json", {
      status: 500,
      headers: { "Content-Type": "application/json" },
    }));

    const err = (await client.get("/sn/site/search").catch((e: unknown) => e)) as TuringHttpError;

    expect(err.problem).toBeUndefined();
  });
});
