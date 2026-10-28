/* eslint-disable react-hooks/refs -- T1027; rationale in eslint.config.js */
import { useCallback, useEffect, useRef, useState } from "react";
import {
  applyFacetAction,
  fetchChat,
  fetchSearch,
  parseHrefToParams,
  postClick,
  postStructuredSearch,
  requestToSearchParams,
  searchParamsToRequest,
  type SearchParams,
} from "../core/api";
import { useTuringContext } from "../core/use-turing-context";
import { resolveDocuments, resolveGroups, resolveStructuredGroups } from "../core/resolve";
import { TURING_ANALYTICS_EVENTS, type TuringAnalytics } from "../core/analytics";
import {
  createAbandonmentWatcher,
  type AbandonmentOptions,
  type AbandonmentReason,
  type AbandonmentWatcher,
} from "../core/analytics-lifecycle";
import { getOrCreateTurSession } from "../core/session";
import type {
  ResolvedDocument,
  ResolvedGroup,
  SearchStatus,
  TurChatResponse,
  TurSearchMode,
  TurSearchRequest,
  TurSearchResponse,
  TurStructuredFacet,
  TurStructuredFacetItem,
  TurStructuredSearchResponse,
} from "../core/types";

export interface UseTuringSearchReturn {
  /** Current status */
  status: SearchStatus;
  /**
   * The legacy link-bearing response. Populated only in GET mode
   * (`postNative: false`); `null` POST-native, where {@link structured} is
   * authoritative.
   */
  data: TurSearchResponse | null;
  /**
   * T710 — the link-free response, populated when the hook runs POST-native
   * (the default). Facets, pagination, locales and spell-check are data here,
   * never hrefs.
   */
  structured: TurStructuredSearchResponse | null;
  /**
   * T710 — the request that will be sent next. POST-native this is the single
   * source of truth: every toggle below is a change to this object.
   */
  request: TurSearchRequest;
  /** T710 — run a search from an explicit structured request. */
  searchRequest: (request: TurSearchRequest) => Promise<void>;
  /**
   * T710 — toggle a facet value from the structured response. Applies the
   * item's own `action` (ADD / REMOVE / REPLACE), so single-select AND fields
   * behave correctly, and resets to page 1.
   */
  toggleFacet: (item: TurStructuredFacetItem) => Promise<void>;
  /** T710 — clear every selection of one facet (replaces `cleanUpLink`). */
  clearFacet: (facet: TurStructuredFacet) => Promise<void>;
  /** T710 — clear every filter (replaces `cleanUpFacets`). */
  clearAllFilters: () => Promise<void>;
  /**
   * T710 — re-run using the spell-checker's suggestion, or force the original
   * term (`useOriginal: true`, the structured form of `nfpr=1`).
   */
  applySpellCheck: (useOriginal?: boolean) => Promise<void>;
  /** AI chat response (null for wildcard queries) */
  chat: TurChatResponse | null;
  /** Resolved documents with mapped default fields */
  documents: ResolvedDocument[];
  /** Resolved groups (when `group` param is used) */
  groups: ResolvedGroup[];
  /** Error message */
  error: string | null;
  /** Current search params (synced with URL) */
  params: SearchParams;
  /** Execute a search with given params */
  search: (params: SearchParams) => Promise<void>;
  /**
   * Execute a new text query (resets page to 1). Pass `{ mode: "instant" }` from
   * a box that searches while the visitor types: the server matches the last
   * term as a prefix, and (T1137) no search event is emitted for it.
   */
  searchQuery: (query: string, options?: { mode?: TurSearchMode }) => Promise<void>;
  /**
   * Navigate via a Turing href link (facet, pagination, spell check).
   *
   * @deprecated T709 / T713 — parsing a server-rendered URL back into params is
   *   the query-string round-trip Block AV removes. Use {@link toggleFacet},
   *   {@link clearFacet}, {@link clearAllFilters}, {@link goToPage} or
   *   {@link applySpellCheck} instead. Calling this logs a one-time console
   *   notice and it is **scheduled for removal in the next major** — as of T714
   *   nothing in this repository calls it. The links themselves stay: render
   *   them as real anchors for deep-linking / SEO / no-JS.
   */
  navigate: (href: string) => Promise<void>;
  /** Change locale */
  changeLocale: (locale: string) => Promise<void>;
  /** Change sort */
  changeSort: (sort: string) => Promise<void>;
  /** Go to specific page */
  goToPage: (page: number) => Promise<void>;
  /**
   * T462/T463 — record a result click: mirrors `postClick` (server CTR) **and**
   * emits `turing_search_result_click` when an `analytics` bus is supplied.
   */
  trackResultClick: (documentId: string, position: number, term?: string) => void;
}

export interface UseTuringSearchOptions {
  /**
   * T463 (Block Z) — canonical analytics bus (from {@link useTuringAnalytics}).
   * When set, the hook emits `turing_search` / `turing_search_no_results` /
   * `turing_search_refined` and stamps the `TUR_SESSION` id for cross-surface
   * stitching with chat. No-op when absent.
   */
  readonly analytics?: TuringAnalytics;
  /**
   * T1137 — asked before every analytics event and before the `TUR_SESSION`
   * cookie is written. While it returns `false` nothing is emitted and no cookie
   * is set; the session id is stamped on the first consented event. Omitted,
   * consent is assumed.
   */
  readonly consent?: () => boolean;
  /**
   * T1137 — `false` stamps no session id and never writes the `TUR_SESSION`
   * cookie. Defaults to `true`.
   */
  readonly sessionCookie?: boolean;
  /**
   * T1137 — search abandonment (opt-in). Each settled search arms a watcher; a
   * visitor who leaves without opening a result emits one
   * `turing_search_abandoned { query, results, reason }`. `trackResultClick`
   * disarms it.
   */
  readonly abandonment?: AbandonmentOptions | boolean;
  /**
   * T710 / §XLV — drive search over `POST /sn/{site}/search/query` with a JSON
   * body and the link-free `STRUCTURED` response, instead of `GET …/search`
   * with a query string. **Defaults to `true`.**
   *
   * <p>This is what makes a filter value containing `& = + # %` or a space
   * impossible to mis-parse: it never touches a query string. Facet,
   * pagination, locale and spell-check navigation all become changes to
   * {@link UseTuringSearchReturn.request}.
   *
   * <p>Set `false` for the legacy GET behaviour, where
   * {@link UseTuringSearchReturn.data} carries the link-bearing response. The
   * only reason to do so is a consumer that still reads `data`; `documents` /
   * `groups` / `status` / `error` / `params` are identical in both modes.
   * T713: in that mode render the response's hrefs as real anchors;
   * `navigate(href)` still works but is deprecated and logs a notice.
   */
  readonly postNative?: boolean;
}

/**
 * Core search hook for manual (non-URL-synced) mode.
 * Manages the full search lifecycle: query, results,
 * pagination, facet navigation, locale/sort changes, and AI chat.
 *
 * For URL-synced mode, use `<TuringProvider urlSync={...}>` and
 * the derived hooks (useTuringFacets, useTuringPagination, etc.) instead.
 *
 * @example
 * ```tsx
 * const { data, documents, search, navigate, status } = useTuringSearch();
 * ```
 *
 * @since 2026.2.0
 */
export function useTuringSearch(
  initialParams?: Partial<SearchParams>,
  options: UseTuringSearchOptions = {},
): UseTuringSearchReturn {
  const { config } = useTuringContext();
  const { analytics, postNative = true, sessionCookie = true, abandonment = false } = options;
  const abortRef = useRef(0);
  // Last real (non-wildcard) query, for refinement detection.
  const lastQueryRef = useRef<string | null>(null);
  // T1137 — read at emit time, so a consent granted after mount applies.
  const consentRef = useRef(options.consent);
  consentRef.current = options.consent;
  const sessionStampedRef = useRef(!sessionCookie);
  const abandonmentRef = useRef<AbandonmentWatcher | null>(null);
  // T1155 — the query last reported as a search. A filter, sort, page or locale
  // change on the same `q` is a refinement and reports nothing; a wildcard
  // search clears it, so typing the same term again is a new search.
  const lastReportedQueryRef = useRef<string | null>(null);
  // Read when the watcher fires, so a refinement can update the count it reports.
  const abandonmentResultsRef = useRef(0);

  // T463 — the site is known up front. T1137: the `TUR_SESSION` id that
  // stitches a later chat lead to this search is stamped on the first
  // consented event, so nothing is stored before the visitor agrees.
  useEffect(() => {
    analytics?.setContext({ site: config.site });
  }, [analytics, config.site]);

  /** The bus, when consent allows an event now; stamps the session first. */
  const consentedAnalytics = useCallback((): TuringAnalytics | null => {
    if (!analytics || (consentRef.current && !consentRef.current())) return null;
    if (!sessionStampedRef.current) {
      sessionStampedRef.current = true;
      analytics.setContext({ sessionId: getOrCreateTurSession() ?? undefined });
    }
    return analytics;
  }, [analytics]);

  const disarmAbandonment = useCallback(() => {
    abandonmentRef.current?.stop();
    abandonmentRef.current = null;
  }, []);

  useEffect(() => disarmAbandonment, [disarmAbandonment]);

  /** T463 — the search funnel events for a settled query; T1137 arms abandonment. */
  const emitSearchEvents = useCallback(
    (q: string, resultCount: number) => {
      if (!q || q === "*") {
        lastReportedQueryRef.current = null;
        return;
      }
      if (q === lastReportedQueryRef.current) {
        // A refinement keeps the watcher, with the new count and a fresh idle timer.
        if (abandonmentRef.current) {
          abandonmentResultsRef.current = resultCount;
          abandonmentRef.current.ping();
        }
        return;
      }
      const bus = consentedAnalytics();
      if (!bus) return;
      lastReportedQueryRef.current = q;
      const prev = lastQueryRef.current;
      if (prev && prev !== q) {
        bus.emit(TURING_ANALYTICS_EVENTS.searchRefined, { from: prev, to: q });
      }
      bus.emit(TURING_ANALYTICS_EVENTS.search, { query: q, results: resultCount });
      if (resultCount === 0) {
        bus.emit(TURING_ANALYTICS_EVENTS.searchNoResults, { query: q });
      }
      lastQueryRef.current = q;
      disarmAbandonment();
      if (abandonment) {
        abandonmentResultsRef.current = resultCount;
        abandonmentRef.current = createAbandonmentWatcher((reason: AbandonmentReason) => {
          disarmAbandonment();
          consentedAnalytics()?.emit(TURING_ANALYTICS_EVENTS.searchAbandoned, {
            query: q,
            results: abandonmentResultsRef.current,
            reason,
          });
        }, abandonment === true ? {} : abandonment);
      }
    },
    [consentedAnalytics, disarmAbandonment, abandonment],
  );

  const defaultParams: SearchParams = {
    q: "*",
    p: "1",
    _setlocale: config.locale,
    sort: config.sort ?? "relevance",
    ...initialParams,
  };

  const [status, setStatus] = useState<SearchStatus>("idle");
  const [data, setData] = useState<TurSearchResponse | null>(null);
  const [chatData, setChatData] = useState<TurChatResponse | null>(null);
  const [documents, setDocuments] = useState<ResolvedDocument[]>([]);
  const [groups, setGroups] = useState<ResolvedGroup[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [params, setParams] = useState<SearchParams>(defaultParams);
  const [structured, setStructured] = useState<TurStructuredSearchResponse | null>(null);
  const [request, setRequest] = useState<TurSearchRequest>(() =>
    searchParamsToRequest(defaultParams),
  );
  // The request is read inside callbacks that must not re-create on every
  // response (they are dependencies of the derived facet/pagination hooks), so
  // it is mirrored in a ref — React queues state updates, it does not apply
  // them synchronously.
  const requestRef = useRef(request);
  requestRef.current = request;
  const structuredRef = useRef(structured);
  structuredRef.current = structured;

  const executeSearch = useCallback(
    async (searchParams: SearchParams) => {
      const requestId = ++abortRef.current;
      setStatus("loading");
      setError(null);
      setParams(searchParams);

      try {
        const q = searchParams.q || "*";
        const shouldChat = q !== "*";

        const [searchResult, chatResult] = await Promise.all([
          fetchSearch(config.site, searchParams),
          shouldChat
            ? fetchChat(config.site, { q, _setlocale: searchParams._setlocale })
            : Promise.resolve(null),
        ]);

        if (requestId !== abortRef.current) return;

        const resolved = resolveDocuments(searchResult);
        setData(searchResult);
        setChatData(chatResult);
        setDocuments(resolved);
        setGroups(resolveGroups(searchResult));
        setStatus("success");

        // T463 — search funnel events. `count` is the total hit count.
        emitSearchEvents(q, searchResult?.queryContext?.count ?? resolved.length);
      } catch (err) {
        if (requestId !== abortRef.current) return;
        setError(err instanceof Error ? err.message : "Search failed");
        setStatus("error");
      }
    },
    [config.site, emitSearchEvents],
  );

  /**
   * T710 — the POST-native path. Identical lifecycle to {@link executeSearch}
   * (stale-response guard, parallel chat call, analytics), but the query travels
   * as a JSON body and the response is the link-free shape.
   */
  const executeRequest = useCallback(
    async (searchRequest: TurSearchRequest) => {
      const requestId = ++abortRef.current;
      setStatus("loading");
      setError(null);
      setRequest(searchRequest);
      requestRef.current = searchRequest;
      setParams(requestToSearchParams(searchRequest));

      try {
        const q = searchRequest.q || "*";
        const shouldChat = q !== "*";

        const [searchResult, chatResult] = await Promise.all([
          postStructuredSearch(config.site, searchRequest),
          shouldChat
            ? fetchChat(config.site, { q, _setlocale: searchRequest.locale })
            : Promise.resolve(null),
        ]);

        if (requestId !== abortRef.current) return;

        const resolved = resolveDocuments(searchResult);
        setData(null);
        setStructured(searchResult);
        structuredRef.current = searchResult;
        setChatData(chatResult);
        setDocuments(resolved);
        setGroups(resolveStructuredGroups(searchResult));
        setStatus("success");
        // The server echoes the request with every default resolved — adopt it
        // so the next toggle starts from what actually ran, not what we guessed.
        const effective = searchResult.request ?? searchRequest;
        setRequest(effective);
        requestRef.current = effective;
        setParams(requestToSearchParams(effective));

        // T1137 — an as-you-type request is not a search the visitor settled on.
        if (searchRequest.mode !== "instant") {
          emitSearchEvents(q, searchResult?.queryContext?.count ?? resolved.length);
        }
      } catch (err) {
        if (requestId !== abortRef.current) return;
        setError(err instanceof Error ? err.message : "Search failed");
        setStatus("error");
      }
    },
    [config.site, emitSearchEvents],
  );

  /** Re-runs the current request with `changes` applied, back on page 1. */
  const patchRequest = useCallback(
    (changes: Partial<TurSearchRequest>, resetPage = true) =>
      executeRequest({
        ...requestRef.current,
        ...changes,
        ...(resetPage ? { page: 1 } : {}),
      }),
    [executeRequest],
  );

  const search = useCallback(
    (p: SearchParams) => (postNative ? executeRequest(searchParamsToRequest(p)) : executeSearch(p)),
    [executeRequest, executeSearch, postNative],
  );

  const searchQuery = useCallback(
    (query: string, queryOptions?: { mode?: TurSearchMode }) =>
      postNative
        ? patchRequest({ q: query, mode: queryOptions?.mode ?? "standard" })
        : executeSearch({ ...params, q: query, p: "1" }),
    [executeSearch, params, patchRequest, postNative],
  );

  const toggleFacet = useCallback(
    (item: TurStructuredFacetItem) =>
      patchRequest({ fq: applyFacetAction(requestRef.current.fq ?? [], item) }),
    [patchRequest],
  );

  const clearFacet = useCallback(
    (facet: TurStructuredFacet) => {
      const clear = new Set(facet.clearFilterQueries ?? facet.selectedFilterQueries ?? []);
      return patchRequest({
        fq: (requestRef.current.fq ?? []).filter((fq) => !clear.has(fq)),
      });
    },
    [patchRequest],
  );

  const clearAllFilters = useCallback(() => patchRequest({ fq: [] }), [patchRequest]);

  const applySpellCheck = useCallback(
    (useOriginal = false) => {
      const spellCheck = structuredRef.current?.spellCheck;
      const text = useOriginal ? spellCheck?.originalText : spellCheck?.correctedTextValue;
      if (!text) return Promise.resolve();
      return patchRequest({ q: text, autoCorrectionDisabled: useOriginal });
    },
    [patchRequest],
  );

  // T713 — deprecated calling deprecated, on purpose: `parseHrefToParams` owns
  // the one-time console notice, so routing through it keeps the warning exact
  // (one per page) instead of duplicating it here.
  const navigate = useCallback(
    (href: string) => executeSearch(parseHrefToParams(href)),
    [executeSearch],
  );

  const changeLocale = useCallback(
    (locale: string) =>
      postNative
        ? patchRequest({ locale })
        : executeSearch({ ...params, _setlocale: locale }),
    [executeSearch, params, patchRequest, postNative],
  );

  const changeSort = useCallback(
    (sort: string) =>
      postNative ? patchRequest({ sort }) : executeSearch({ ...params, sort }),
    [executeSearch, params, patchRequest, postNative],
  );

  const goToPage = useCallback(
    (page: number) =>
      postNative
        ? patchRequest({ page }, false)
        : executeSearch({ ...params, p: String(page) }),
    [executeSearch, params, patchRequest, postNative],
  );

  const trackResultClick = useCallback(
    (documentId: string, position: number, term?: string) => {
      const query = term ?? params.q ?? "";
      disarmAbandonment();
      void postClick(config.site, {
        term: query,
        documentId,
        position,
        locale: params._setlocale,
      });
      consentedAnalytics()?.emit(TURING_ANALYTICS_EVENTS.searchResultClick, {
        query,
        document_id: documentId,
        position,
      });
    },
    [config.site, params, consentedAnalytics, disarmAbandonment],
  );

  return {
    status,
    data,
    structured,
    request,
    chat: chatData,
    documents,
    groups,
    error,
    params,
    search,
    searchRequest: executeRequest,
    searchQuery,
    navigate,
    toggleFacet,
    clearFacet,
    clearAllFilters,
    applySpellCheck,
    changeLocale,
    changeSort,
    goToPage,
    trackResultClick,
  };
}
