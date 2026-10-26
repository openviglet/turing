import { afterEach, describe, expect, it, vi } from "vitest";

import { createTuringClient, postClick } from "../index";

/**
 * T1128 — a result click reaches the server even though the click starts a
 * navigation: one request, kept alive, with no CSRF prime before it.
 */
const BASE = "https://turing.example/api";

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("postClick", () => {
  it("sends one keepalive POST and no CSRF prime", async () => {
    const fetchMock = vi.fn(async () => new Response(null, { status: 202 }));
    vi.stubGlobal("fetch", fetchMock);
    const client = createTuringClient({ baseURL: BASE });

    await postClick(client, "Sample", { term: "workshop", documentId: "post-7", position: 2 });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(new URL(url).pathname).toBe("/api/sn/Sample/click");
    expect(init.method).toBe("POST");
    expect(init.keepalive).toBe(true);
  });
});
