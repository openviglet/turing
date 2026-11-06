import { useCallback, useState } from "react";
import type { TuringProblem } from "@viglet/turing-sdk";
import { problemOf } from "../core/errors.js";
import {
  dslSearch,
  type TurDslSearchRequest,
  type TurDslSearchResponse,
} from "../core/api.js";
import { useTuringContext } from "../core/use-turing-context.js";

export type DslSearchStatus = "idle" | "loading" | "success" | "error";

export interface UseTuringDslSearchOptions {
  /** Locale for the query; falls back to the provider locale, then {@code "en"}. */
  readonly locale?: string;
}

export interface UseTuringDslSearchReturn {
  /** Runs a structured Elasticsearch-compatible query and stores the response. */
  readonly search: (request: TurDslSearchRequest) => Promise<TurDslSearchResponse | null>;
  readonly status: DslSearchStatus;
  readonly data: TurDslSearchResponse | null;
  readonly error: string | null;
  /** T1240 — the problem details behind `error`, when the server answered with them (a 429's `resetsAt`). */
  readonly errorProblem?: TuringProblem;
}

/**
 * T403 — runs a structured Elasticsearch-compatible query against the site
 * ({@code POST /sn/{site}/_search}). For dashboards, relevance experiments, or a
 * catalog copilot's tool layer that needs full query control from JS.
 *
 * @example
 * ```tsx
 * const { search, data } = useTuringDslSearch();
 * await search({ query: { match: { title: "java" } }, size: 5 });
 * ```
 *
 * @since 2026.3.4
 */
export function useTuringDslSearch(
  options: UseTuringDslSearchOptions = {},
): UseTuringDslSearchReturn {
  const { config } = useTuringContext();
  const locale = options.locale ?? config.locale ?? "en";

  const [status, setStatus] = useState<DslSearchStatus>("idle");
  const [data, setData] = useState<TurDslSearchResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  // T1240 — the problem details behind `error` (a 429's `resetsAt`).
  const [errorProblem, setErrorProblem] = useState<TuringProblem | undefined>(undefined);

  const search = useCallback(
    async (request: TurDslSearchRequest): Promise<TurDslSearchResponse | null> => {
      setStatus("loading");
      setError(null);
      setErrorProblem(undefined);
      try {
        const res = await dslSearch(config.site, request, locale);
        setData(res);
        setStatus("success");
        return res;
      } catch (err) {
        setErrorProblem(problemOf(err));
        setError(err instanceof Error ? err.message : "DSL search failed");
        setStatus("error");
        return null;
      }
    },
    [config.site, locale],
  );

  return { search, status, data, error, errorProblem };
}
