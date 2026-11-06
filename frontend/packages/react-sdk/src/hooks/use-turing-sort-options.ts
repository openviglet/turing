/* eslint-disable react-hooks/set-state-in-effect -- T1027; rationale in eslint.config.js */
import { useCallback, useEffect, useState } from "react";
import type { TuringProblem } from "@viglet/turing-sdk";
import { problemOf } from "../core/errors.js";
import { fetchSortOptions } from "../core/api.js";
import { useTuringContext } from "../core/use-turing-context.js";
import type { TurSortOption } from "../core/types.js";

export interface UseTuringSortOptionsReturn {
  sortOptions: TurSortOption[];
  isLoading: boolean;
  error: string | null;
  /** T1240 — the problem details behind `error`, when the server answered with them (a 429's `resetsAt`). */
  errorProblem?: TuringProblem;
  refresh: () => Promise<void>;
}

/**
 * Hook that fetches the available sort options for the current site.
 * Returns the built-in sorts (relevance, newest, oldest) plus any
 * custom sorts configured in the Turing admin console.
 *
 * @example
 * ```tsx
 * const { sortOptions, isLoading } = useTuringSortOptions();
 *
 * <select onChange={(e) => changeSort(e.target.value)}>
 *   {sortOptions.map((o) => (
 *     <option key={o.value} value={o.value}>{o.label}</option>
 *   ))}
 * </select>
 * ```
 *
 * @since 2026.2.0
 */
export function useTuringSortOptions(): UseTuringSortOptionsReturn {
  const { config } = useTuringContext();
  const [sortOptions, setSortOptions] = useState<TurSortOption[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  // T1240 — the problem details behind `error` (a 429's `resetsAt`).
  const [errorProblem, setErrorProblem] = useState<TuringProblem | undefined>(undefined);

  const refresh = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    setErrorProblem(undefined);
    try {
      const options = await fetchSortOptions(config.site);
      setSortOptions(options);
    } catch (err) {
      setErrorProblem(problemOf(err));
      setError(err instanceof Error ? err.message : "Failed to load sort options");
    } finally {
      setIsLoading(false);
    }
  }, [config.site]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  return { sortOptions, isLoading, error, errorProblem, refresh };
}
