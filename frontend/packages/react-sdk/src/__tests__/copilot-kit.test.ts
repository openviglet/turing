import axios from "axios";
import { afterEach, describe, expect, it, vi } from "vitest";

import { postCopilotKit, type TurCatalogKitResult } from "../index";

/**
 * T1159 — the React SDK posts a plan to the kit endpoint through the host's axios
 * instance and returns the vanilla SDK's typed result.
 */
const KIT: TurCatalogKitResult = {
  summary: "A weekend trip.",
  stages: [
    {
      gap: "Shelter",
      query: "tent",
      products: [{ rank: 1, id: "t1", title: "Ridge 2", reason: "weight: 1.2 kg" }],
    },
  ],
  unfilled: [],
};

afterEach(() => {
  vi.restoreAllMocks();
});

describe("postCopilotKit", () => {
  it("posts the plan to /sn/{site}/copilot/kit and returns the stages", async () => {
    const post = vi.spyOn(axios, "post").mockResolvedValue({ data: KIT });

    const result = await postCopilotKit("outdoor", { plan: "a weekend trip", maxGaps: 3 });

    expect(post).toHaveBeenCalledWith("/sn/outdoor/copilot/kit", { plan: "a weekend trip", maxGaps: 3 });
    expect(result.stages[0].products[0].reason).toBe("weight: 1.2 kg");
    expect(result.unfilled).toEqual([]);
  });
});
