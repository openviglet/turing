/**
 * `@viglet/turing-sdk` — vanilla JS SDK for Viglet Turing ES.
 *
 * Framework-agnostic, zero-dependency. Provides an explicit HTTP client, the
 * full Turing REST/SSE API surface, and observable controllers (search, chat,
 * autocomplete, slots) for the common front-end flows. Works in Adobe Edge
 * Delivery Services blocks, plain `<script>`, or any bundler.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.1
 */

// ── Errors ──
//
// T1005 — exported so a consumer can `instanceof` them instead of parsing a
// message. `status` is a field on TuringHttpError; TuringTransportError means the
// request produced no usable response (refused, offline, CORS, empty body).
// T1079 — TuringTimeoutError (a TuringTransportError) when the client's timeoutMs elapses.
export { TuringHttpError, TuringTimeoutError, TuringTransportError, problemOf } from "./errors.js";
export type { TuringProblem } from "./errors.js";

// ── Client ──
export { createTuringClient } from "./client.js";
export type {
  TuringClient,
  TuringCallOptions,
  TuringClientConfig,
  TuringRequestOptions,
} from "./client.js";

// ── API (functions + bound helper) ──
export {
  fetchSearch,
  postSearch,
  postStructuredSearch,
  applyFacetAction,
  postClick,
  fetchChat,
  fetchChatEnabled,
  postChatConversation,
  postAgentChat,
  postPersonaChat,
  postClientToolResult,
  postLlmChat,
  postSemanticChat,
  postAnswer,
  postAnswerFreshness,
  createAnswerShare,
  fetchAnswerShare,
  recordAnswerSignal,
  consumeAnswerStream,
  postResearch,
  deleteSiteConversationState,
  deleteAgentFlowState,
  fetchIntents,
  fetchSiteChatSlots,
  fetchAgentChatSlots,
  postSiteChatSlot,
  postSiteSlotExtract,
  postSiteSlotExtractMulti,
  fetchSiteChatState,
  postSiteHandoff,
  postSiteFlowSelect,
  fetchPersonaContentFit,
  fetchSitePersonas,
  postSiteFormSubmit,
  fetchLlmInstances,
  fetchLlmContextInfo,
  fetchAgentContextInfo,
  fetchAutoComplete,
  fetchSortOptions,
  fetchFailedSearches,
  fetchContentGaps,
  fetchClickThrough,
  fetchAiCapabilities,
  fetchSimilar,
  similarRequestBody,
  fetchStoredDocuments,
  postNlFilters,
  postCopilotKit,
  postCopilot,
  chatConversationBody,
  parseFiltersEvent,
  postSiteSlotUpload,
  postSiteChatResume,
  fetchSpellCheck,
  fetchRelatedTerms,
  dslSearch,
  dslQuery,
  fetchDiscovery,
  fetchFeatures,
  fetchSystemLocales,
  fetchLlmVendors,
  postSummary,
  parseHrefToParams,
  createTuringApi,
  CHAT_DISABLED_HINTS,
  LOW_CONFIDENCE_THRESHOLD,
} from "./api.js";
export type {
  SearchParams,
  ClickTrackParams,
  ChatEnabledResponse,
  ChatDisabledReason,
  PostChatConversationOptions,
  PostAgentChatOptions,
  PostPersonaChatOptions,
  PostClientToolResultOptions,
  ClientToolResultBody,
  PostLlmChatOptions,
  PostSemanticChatOptions,
  PostAnswerOptions,
  AnswerStreamHandlers,
  AnswerResult,
  AnswerFilters,
  AnswerShareSnapshot,
  TurAnswerSignalKind,
  PostResearchOptions,
  ResearchStreamHandlers,
  ResearchResult,
  TurResearchStep,
  TurResearchSection,
  TurResearchSource,
  TurResearchTimelineEntry,
  TurChatSessionSlots,
  TurChatSessionSlotsDelta,
  TurWorkspaceArtifact,
  TurWorkspaceEvent,
  TurWorkspaceArtifacts,
  TurChatSlotWriteResponse,
  TurChatSlotExtractResponse,
  TurChatConversationState,
  TurChatHandoffChannel,
  TurChatHandoffResponse,
  TurChatFlowSelectResponse,
  TurSimilarMode,
  TurSimilarResult,
  FetchSimilarParams,
  TurNLFilter,
  TurNLDroppedFilter,
  TurNLFilters,
  TurNLFiltersOptions,
  TurCatalogKitAvailability,
  TurCatalogKitRequest,
  TurCatalogKitProduct,
  TurCatalogKitStage,
  TurCatalogKitGuide,
  TurCatalogKitResult,
  TurCatalogCitation,
  TurCatalogCopilotRequest,
  TurCatalogCopilotResult,
  TurChatConversationRequest,
  TurChatSlotUploadResponse,
  PostSlotUploadOptions,
  SlotExtractOptions,
  TurChatResumeResponse,
  TurDslSearchRequest,
  TurDslSearchHit,
  TurDslSearchResponse,
  TurDiscoveryInfo,
  TurFeaturesInfo,
  TurSystemLocale,
  TurSummaryResult,
  TuringApi,
} from "./api.js";

// ── Session ──
export {
  getOrCreateTurSession,
  readTurSession,
  clearTurSession,
  TUR_SESSION_DEFAULT_COOKIE_NAME,
  TUR_SESSION_DEFAULT_TTL_SECONDS,
} from "./session.js";
export type { TurSessionOptions } from "./session.js";

// ── Resolve utilities ──
export { resolveDocuments, resolveGroups, resolveStructuredGroups } from "./resolve.js";
export type { ResolvableSearchResponse } from "./resolve.js";

// T881 — the rest of the response, projected once for both shapes.
export { toSearchView } from "./search-view.js";
export type {
  ProjectableSearchStore,
  SearchView,
  SearchLocaleView,
  SearchSpellCheckView,
  AppliedFilterView,
} from "./search-view.js";

// ── Sandbox artifact URLs (code-interpreter / workspace) ──
export { resolveSandboxUrl, isSandboxUrl, absolutizeArtifactUrls } from "./sandbox.js";

// ── Rich-content segmentation (```html live preview / games / charts) ──
export { splitRichContent, hasHtmlBlock } from "./rich-content.js";
export type { TurRichSegment } from "./rich-content.js";

// ── Slots SSE multiplexer (advanced) ──
export { subscribeSlotsSse } from "./slots-sse.js";
export type {
  SlotsSseSubscriber,
  SlotsSseSubscription,
  SlotsSseOptions,
} from "./slots-sse.js";

// ── Workspace SSE multiplexer (T113) ──
export { subscribeWorkspaceSse, _workspaceSseOpenChannelCount } from "./workspace-sse.js";
export type {
  WorkspaceSseSubscriber,
  WorkspaceSseSubscription,
  WorkspaceSseScope,
} from "./workspace-sse.js";

// ── Store ──
export { createStore } from "./store.js";
export type { Store, Listener } from "./store.js";

// ── Analytics event bus (T458, Block Z) ──
export { createTuringAnalytics, TURING_ANALYTICS_EVENTS } from "./analytics.js";
export type {
  TuringAnalytics,
  TuringAnalyticsOptions,
  TuringAnalyticsContext,
  TuringAnalyticsEvent,
  TuringAnalyticsEventName,
  TuringAnalyticsSink,
  TuringAnalyticsValue,
} from "./analytics.js";
// ── Analytics sinks: GA4/GTM, generic, debug (T459) ──
export { googleAnalyticsSink, onEventSink, debugSink } from "./analytics-sinks.js";
export type {
  GoogleAnalyticsSinkOptions,
  DebugSinkOptions,
} from "./analytics-sinks.js";
// ── Analytics lifecycle: abandonment watcher (T460) ──
export { createAbandonmentWatcher } from "./analytics-lifecycle.js";
export type {
  AbandonmentOptions,
  AbandonmentReason,
  AbandonmentWatcher,
} from "./analytics-lifecycle.js";

// ── Controllers ──
export {
  createSearchController,
  // T710 — the SearchParams <-> TurSearchRequest bridges, shared with the React
  // SDK so the two cannot drift on how a legacy param maps onto the POST body.
  searchParamsToRequest,
  requestToSearchParams,
  // T1249 — what clearing every filter keeps: the secondary facets' selections.
  keepSecondaryFilterQueries,
} from "./controllers/search.js";
export type {
  SearchController,
  SearchControllerState,
  SearchControllerOptions,
} from "./controllers/search.js";
export { createChatController } from "./controllers/chat.js";
export type {
  ChatController,
  ChatControllerState,
  ChatControllerOptions,
  ChatControllerAgent,
  ChatControllerPersona,
  ChatMessage,
  ChatStatus,
  SendOverrides,
  ClientToolHandler,
  ClientToolRegistration,
} from "./controllers/chat.js";
// T1220 — observable deep-dive research controller.
export { createResearchController } from "./controllers/research.js";
export type {
  ResearchController,
  ResearchControllerState,
  ResearchAskOptions,
  ResearchStatus,
} from "./controllers/research.js";

// T837 / §LXII.6 — observable answer controller (the vanilla useTuringAnswer).
export { createAnswerController } from "./controllers/answer.js";
export type {
  AnswerController,
  AnswerControllerState,
  AnswerAskOptions,
  AnswerStatus,
} from "./controllers/answer.js";
// T450 — embeddable action widget: default host-action client tools.
export { createHostActions } from "./host-actions.js";
export type { HostActions, HostActionsOptions } from "./host-actions.js";
export { createAutoComplete } from "./controllers/autocomplete.js";
export type {
  AutoCompleteController,
  AutoCompleteState,
} from "./controllers/autocomplete.js";
export { createSlotsController } from "./controllers/slots.js";
export type {
  SlotsController,
  SlotsControllerState,
  SlotsControllerOptions,
  SlotsControllerScope,
  SlotsStatus,
} from "./controllers/slots.js";
export { createSimilarController } from "./controllers/similar.js";
export type {
  SimilarController,
  SimilarControllerState,
  SimilarControllerOptions,
  SimilarStatus,
} from "./controllers/similar.js";

// ── Types ──
export type {
  TuringConfig,
  TurSearchResponse,
  TurQueryContext,
  TurDefaultFields,
  TurQueryMeta,
  TurSearchResults,
  TurDocument,
  TurDocumentMetadata,
  TurDuplicateCluster,
  TurDuplicateClusterMember,
  TurPaginationItem,
  TurWidget,
  TurFacetGroup,
  TurFacetItem,
  TurSpellCheck,
  // T709 / §XLV — POST-native search: request + link-free response.
  TurSearchRequest,
  TurSearchMode,
  TurResponseFormat,
  TurFacetItemAction,
  TurStructuredSearchResponse,
  // T864 — link-free results: a document's facet chips are data, not hrefs.
  TurStructuredDocument,
  TurStructuredDocumentMetadata,
  TurStructuredResults,
  TurStructuredFacet,
  TurStructuredFacetItem,
  // T869 — the toggleable shape both a rail item and a result's chip satisfy.
  TurFacetToggleTarget,
  TurStructuredPage,
  TurStructuredLocale,
  TurStructuredSpellCheck,
  TurStructuredGroup,
  // T880 — on the wire since T709, typed since T880.
  TurSimilarDocument,
  TurSpotlightDocument,
  TurRelatedTermSuggestion,
  TurLocaleItem,
  TurSortOption,
  TurSearchInsightWindow,
  TurSearchInsightTerm,
  TurSearchInsightReport,
  TurClickThroughWindow,
  TurClickThroughTerm,
  TurClickThroughReport,
  TurAiCapability,
  TurAiCapabilities,
  TurChatResponse,
  TurChatConversationMessage,
  TurChatConversationResponse,
  TurChatStreamEvent,
  TurChatSource,
  TurAnswerCitation,
  TurAnswerArtifact,
  TurAnswerTrace,
  TurAnswerFreshnessSource,
  TurAnswerFreshnessVerdict,
  TurAnswerFreshnessStatus,
  TurAnswerShareView,
  TurChatCitation,
  TurChatGrounding,
  TurChatSecondOpinion,
  TurPersonaOption,
  TurPersonaPersonality,
  TurContentFit,
  TurContentFitMisfit,
  TurSearchSuggestions,
  TurChatToolCall,
  TurClientToolCall,
  TurChatForm,
  TurChatFormField,
  TurChatFormSubmitResponse,
  TurIntent,
  TurIntentAction,
  TurLlmInstance,
  TurLlmVendor,
  TurLlmContextInfo,
  TurGroupBean,
  SearchStatus,
  SearchState,
  ResolvedDocument,
  ResolvedGroup,
} from "./types.js";
