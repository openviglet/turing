/* eslint-disable react-hooks/set-state-in-effect -- T1027; rationale in eslint.config.js */
import { useEffect, useState } from "react";
import type { TuringProblem } from "@viglet/turing-sdk";
import { problemOf } from "../core/errors.js";
import { fetchIntents } from "../core/api.js";
import { useTuringContext } from "../core/use-turing-context.js";
import type { TurIntent } from "../core/types.js";

export interface UseTuringIntentsReturn {
  /** Enabled intents (already sorted by `sortOrder`) */
  intents: TurIntent[];
  /** Whether the request is in flight */
  loading: boolean;
  /** Error message from the last failed fetch */
  error: string | null;
  /** T1240 — the problem details behind `error`, when the server answered with them (a 429's `resetsAt`). */
  errorProblem?: TuringProblem;
  /** Re-fetch intents */
  refresh: () => void;
}

/**
 * Loads the curated list of enabled intents for the site's AI agent.
 * Use the resulting `intents[].actions` to render quick-prompt chips below
 * a chat composer or empty state.
 *
 * @example
 * ```tsx
 * const { intents } = useTuringIntents();
 * const flatActions = intents.flatMap((i) => i.actions);
 * return flatActions.map((a) => (
 *   <button key={a.id ?? a.label} onClick={() => send(a.prompt)}>
 *     {a.label}
 *   </button>
 * ));
 * ```
 *
 * @since 2026.2.12
 */
export function useTuringIntents(): UseTuringIntentsReturn {
  const { config } = useTuringContext();
  const [intents, setIntents] = useState<TurIntent[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // T1240 — the problem details behind `error` (a 429's `resetsAt`).
  const [errorProblem, setErrorProblem] = useState<TuringProblem | undefined>(undefined);
  const [tick, setTick] = useState(0);

  useEffect(() => {
    let alive = true;
    setLoading(true);
    setError(null);
    setErrorProblem(undefined);
    fetchIntents(config.site)
      .then((res) => {
        if (!alive) return;
        setIntents(Array.isArray(res) ? res : []);
        setLoading(false);
      })
      .catch((err) => {
        if (!alive) return;
        setErrorProblem(problemOf(err));
        setError(err instanceof Error ? err.message : "Failed to load intents");
        setIntents([]);
        setLoading(false);
      });
    return () => {
      alive = false;
    };
  }, [config.site, tick]);

  return {
    intents,
    loading,
    error,
    errorProblem,
    refresh: () => setTick((t) => t + 1),
  };
}
