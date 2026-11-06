import { afterEach, describe, expect, it, vi } from "vitest";

import { createTuringClient, problemOf, TuringHttpError } from "../index.js";
import { createAnswerController } from "../controllers/answer.js";
import { createChatController } from "../controllers/chat.js";
import { createSearchController } from "../controllers/search.js";
import { createSimilarController } from "../controllers/similar.js";

/**
 * T1240 — a controller keeps the problem details of a failed call beside its
 * `error` text, so a 429 `quota-exhausted` tells a subscriber `resetsAt`.
 */
const BASE = "https://turing.example/api";
const QUOTA = {
  type: "https://turing.viglet.org/problems/quota-exhausted",
  title: "AI quota exhausted",
  status: 429,
  detail: "The site's AI budget is spent until 2026-12-01.",
  resetsAt: "2026-12-01T00:00:00Z",
  upgradeUrl: "https://turing.example/billing",
};

function quotaResponse(): Response {
  return new Response(JSON.stringify(QUOTA), {
    status: 429,
    headers: { "Content-Type": "application/problem+json" },
  });
}

/** Every call answers the quota problem; the chat probe answers enabled. */
function clientAnsweringQuota() {
  vi.stubGlobal(
    "fetch",
    vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes("/chat/enabled")) {
        return new Response(JSON.stringify({ enabled: true }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      return quotaResponse();
    }) as unknown as typeof fetch,
  );
  return createTuringClient({ baseURL: BASE, getCsrfToken: () => "token" });
}

afterEach(() => vi.unstubAllGlobals());

describe("problem details in controller state (T1240)", () => {
  it("problemOf reads a TuringHttpError's problem and nothing else", () => {
    expect(problemOf(new TuringHttpError(429, "", "/x", QUOTA))?.resetsAt).toBe(QUOTA.resetsAt);
    expect(problemOf(new Error("HTTP 429"))).toBeUndefined();
    expect(problemOf("HTTP 429")).toBeUndefined();
  });

  it("the answer controller exposes resetsAt, and a new ask or reset clears it", async () => {
    const controller = createAnswerController(clientAnsweringQuota(), { site: "site1" });

    await controller.ask("tents");

    const state = controller.getState();
    expect(state.status).toBe("error");
    expect(state.error).toContain("HTTP 429");
    expect(state.errorProblem?.resetsAt).toBe(QUOTA.resetsAt);
    expect(state.errorProblem?.upgradeUrl).toBe(QUOTA.upgradeUrl);

    controller.reset();
    expect(controller.getState().errorProblem).toBeUndefined();
  });

  it("the chat controller exposes resetsAt", async () => {
    const chat = createChatController(clientAnsweringQuota(), { site: "site1", conversationId: "c1" });

    await chat.send("hello");

    expect(chat.getState().status).toBe("error");
    expect(chat.getState().errorProblem?.resetsAt).toBe(QUOTA.resetsAt);
  });

  it("the search and similar controllers expose it too", async () => {
    const client = clientAnsweringQuota();
    const search = createSearchController(client, { site: "site1", locale: "en_US" });
    const similar = createSimilarController(client, "site1");

    await search.search({ q: "tents" });
    await similar.load("post-7");

    expect(search.getState().errorProblem?.resetsAt).toBe(QUOTA.resetsAt);
    expect(similar.getState().errorProblem?.resetsAt).toBe(QUOTA.resetsAt);
  });
});
