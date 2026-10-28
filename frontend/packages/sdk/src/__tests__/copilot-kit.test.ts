import { afterEach, describe, expect, it, vi } from "vitest";

import { createTuringApi, createTuringClient, postCopilotKit, type TurCatalogKitResult } from "../index";

/**
 * T1159 — a described plan comes back as staged products by need with one POST.
 */
const BASE = "https://turing.example/api";

const KIT: TurCatalogKitResult = {
  summary: "A two-day mountain crossing in July for a beginner.",
  stages: [
    {
      gap: "Shelter",
      reason: "nights on the trail",
      query: "lightweight tent",
      products: [
        { rank: 1, id: "t1", title: "Ridge 2", url: "/p/t1", score: 1, reason: "weight: 1.2 kg", available: true },
      ],
    },
  ],
  unfilled: ["Navigation"],
  error: null,
};

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("postCopilotKit", () => {
  it("posts the plan and options, and reads the stages and unfilled needs", async () => {
    const fetchMock = vi.fn(async () => new Response(JSON.stringify(KIT), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    const client = createTuringClient({ baseURL: BASE });

    const result = await postCopilotKit(client, "outdoor", {
      plan: "two days crossing the mountains in July, first time",
      locale: "en",
      perGap: 2,
      availabilityField: "in_stock",
      availability: "DROP",
    });

    expect(result.stages[0].products[0].id).toBe("t1");
    expect(result.unfilled).toEqual(["Navigation"]);
    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(new URL(url).pathname).toBe("/api/sn/outdoor/copilot/kit");
    expect(init.method).toBe("POST");
    expect(JSON.parse(init.body as string)).toEqual({
      plan: "two days crossing the mountains in July, first time",
      locale: "en",
      perGap: 2,
      availabilityField: "in_stock",
      availability: "DROP",
    });
  });

  it("is bound on the client api", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => new Response(JSON.stringify(KIT), { status: 200 })));
    const api = createTuringApi(createTuringClient({ baseURL: BASE }));

    const result = await api.postCopilotKit("outdoor", { plan: "a trip" });

    expect(result.summary).toBe(KIT.summary);
  });
});
