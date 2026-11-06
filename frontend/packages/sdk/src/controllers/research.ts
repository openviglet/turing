import type { TuringClient } from "../client.js";
import {
  postResearch,
  type AnswerFilters,
  type TurResearchSection,
  type TurResearchSource,
  type TurResearchStep,
  type TurResearchTimelineEntry,
} from "../api.js";
import { problemOf, type TuringProblem } from "../errors.js";
import { createStore, type Store } from "../store.js";
import type { TuringConfig } from "../types.js";

/**
 * T1220 — the vanilla deep-dive research controller. It drives the streaming
 * `POST /sn/{site}/research` endpoint into an observable {@link Store}: the plan
 * first, each step as its retrieval finishes (a step with `hits: 0` is one the
 * index has nothing on), then the cited sections, what the index does not cover
 * and the sources grouped by document type, ready for cards.
 *
 * <p>Fail-open like the answer controller: a refusal or a mid-stream `error`
 * lands in {@link ResearchControllerState.error}, and a superseded or stopped
 * request never clobbers a newer one.
 *
 * @since 2026.3.5
 */

export type ResearchStatus = "idle" | "loading" | "success" | "error";

export interface ResearchControllerState {
  status: ResearchStatus;
  /** One sentence restating what the visitor wants to understand. */
  summary: string;
  steps: TurResearchStep[];
  sections: TurResearchSection[];
  /** Sub-questions and topics the index does not cover. */
  uncovered: string[];
  /** The cited sources that carry a date, oldest first (empty under two). */
  timeline: TurResearchTimelineEntry[];
  /** The cited documents by type; each `rank` is the `[n]` the sections cite. */
  sourceGroups: Record<string, TurResearchSource[]>;
  error: string | null;
  /** T1240 — the problem details behind `error`, when the server answered with them (a 429's `resetsAt`). */
  errorProblem?: TuringProblem;
  isStreaming: boolean;
}

export interface ResearchAskOptions {
  locale?: string;
  /** Every read stays inside these filters, e.g. `{ site_key: ["main"] }`. */
  filters?: AnswerFilters;
  /** How many sub-questions to plan, 3 to 6. */
  maxSteps?: number;
}

export interface ResearchController extends Store<ResearchControllerState> {
  /** Stream a deep dive into {@code question}. */
  ask(question: string, options?: ResearchAskOptions): Promise<void>;
  /** Abort the in-flight research, keeping what has streamed so far. */
  stop(): void;
  /** Abort and clear back to the idle state. */
  reset(): void;
  /** Abort and release resources. */
  destroy(): void;
}

const INITIAL: ResearchControllerState = {
  status: "idle",
  summary: "",
  steps: [],
  sections: [],
  uncovered: [],
  timeline: [],
  sourceGroups: {},
  error: null,
  errorProblem: undefined,
  isStreaming: false,
};

function isAbortError(err: unknown): boolean {
  return (err instanceof Error || (typeof DOMException !== "undefined" && err instanceof DOMException))
    && (err as Error).name === "AbortError";
}

export function createResearchController(client: TuringClient, config: TuringConfig): ResearchController {
  const store = createStore<ResearchControllerState>({ ...INITIAL });
  let latestRequestId = 0;
  let abortController: AbortController | null = null;

  const isStale = (requestId: number) => requestId !== latestRequestId;

  async function ask(question: string, options?: ResearchAskOptions): Promise<void> {
    const trimmed = question?.trim();
    if (!trimmed) return;
    const requestId = ++latestRequestId;
    abortController?.abort();
    abortController = new AbortController();
    store.setState({ ...INITIAL, status: "loading", isStreaming: true });
    try {
      const result = await postResearch(client, config.site, trimmed, options?.locale ?? config.locale, {
        signal: abortController.signal,
        filters: options?.filters,
        maxSteps: options?.maxSteps,
        onPlan: (summary, steps) => {
          if (!isStale(requestId)) store.setState({ summary, steps });
        },
        onStep: (step) => {
          if (!isStale(requestId)) {
            store.setState((prev) => ({
              steps: prev.steps.map((s) => (s.index === step.index ? { ...s, ...step } : s)),
            }));
          }
        },
        onSection: (section) => {
          if (!isStale(requestId)) store.setState((prev) => ({ sections: [...prev.sections, section] }));
        },
        onUncovered: (uncovered) => {
          if (!isStale(requestId)) store.setState({ uncovered });
        },
        onTimeline: (timeline) => {
          if (!isStale(requestId)) store.setState({ timeline });
        },
        onSources: (sourceGroups) => {
          if (!isStale(requestId)) store.setState({ sourceGroups });
        },
        onError: (message) => {
          if (!isStale(requestId)) store.setState({ error: message });
        },
      });
      if (isStale(requestId)) return;
      store.setState({ status: result.error ? "error" : "success", isStreaming: false, error: result.error ?? null });
    } catch (err) {
      if (isStale(requestId) || isAbortError(err)) return;
      store.setState({ status: "error", isStreaming: false, error: err instanceof Error ? err.message : String(err), errorProblem: problemOf(err) });
    }
  }

  function stop(): void {
    latestRequestId++;
    abortController?.abort();
    abortController = null;
    if (store.getState().status === "loading") store.setState({ status: "idle", isStreaming: false });
  }

  function reset(): void {
    latestRequestId++;
    abortController?.abort();
    abortController = null;
    store.setState({ ...INITIAL });
  }

  return {
    getState: store.getState,
    setState: store.setState,
    subscribe: store.subscribe,
    ask,
    stop,
    reset,
    destroy: reset,
  };
}
