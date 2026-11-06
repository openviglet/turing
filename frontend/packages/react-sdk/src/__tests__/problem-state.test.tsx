import { act, renderHook, waitFor } from "@testing-library/react";
import axios, { AxiosError, type AxiosResponse } from "axios";
import { createElement, type ReactNode } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { TuringContext } from "../core/context.js";
import { problemOf } from "../index.js";
import { useTuringAnswer } from "../hooks/use-turing-answer.js";
import { useTuringSimilar } from "../hooks/use-turing-similar.js";

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

function axios429(): AxiosError {
  const response = { status: 429, statusText: "", data: QUOTA, headers: {}, config: {} } as AxiosResponse;
  return new AxiosError("Request failed with status code 429", "ERR_BAD_REQUEST", undefined, undefined, response);
}

describe("axios-based hooks (T1240)", () => {
  it("problemOf reads an axios rejection's problem body, and nothing from other errors", () => {
    expect(problemOf(axios429())?.resetsAt).toBe(QUOTA.resetsAt);
    expect(problemOf(new Error("boom"))).toBeUndefined();
    const htmlBody = axios429();
    htmlBody.response!.data = "<html>Too many</html>";
    expect(problemOf(htmlBody)).toBeUndefined();
  });

  it("useTuringSimilar exposes resetsAt on a 429", async () => {
    vi.spyOn(axios, "post").mockRejectedValue(axios429());
    const wrapper = ({ children }: { children: ReactNode }) =>
      createElement(TuringContext.Provider, { value: { config: { site: "site1" } } }, children);

    const { result } = renderHook(() => useTuringSimilar({ documentId: "post-7" }), { wrapper });

    await waitFor(() => expect(result.current.error).not.toBeNull());
    expect(result.current.errorProblem?.resetsAt).toBe(QUOTA.resetsAt);
  });
});
