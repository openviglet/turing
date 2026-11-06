import { act, renderHook } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { useTuringAnswer } from "../hooks/use-turing-answer.js";

/**
 * T1240 — the React answer hook keeps the problem details of a failed call
 * beside its `error` text, so a 429 `quota-exhausted` exposes `resetsAt`.
 */
const QUOTA = {
  type: "https://turing.viglet.org/problems/quota-exhausted",
  status: 429,
  detail: "The site's AI budget is spent until 2026-12-01.",
  resetsAt: "2026-12-01T00:00:00Z",
};

afterEach(() => vi.unstubAllGlobals());

describe("useTuringAnswer problem details (T1240)", () => {
  it("exposes resetsAt on a 429, and reset clears it", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () =>
        new Response(JSON.stringify(QUOTA), {
          status: 429,
          headers: { "Content-Type": "application/problem+json" },
        }),
      ) as unknown as typeof fetch,
    );
    const { result } = renderHook(() => useTuringAnswer({ siteName: "site1" }));

    await act(async () => {
      await result.current.ask("tents");
    });

    expect(result.current.status).toBe("error");
    expect(result.current.errorProblem?.resetsAt).toBe(QUOTA.resetsAt);

    act(() => result.current.reset());
    expect(result.current.errorProblem).toBeUndefined();
  });
});
