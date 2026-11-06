// ── Core ──
export { TuringProvider } from "./core/provider.js";
export { useTuringContext } from "./core/use-turing-context.js";
export { useSearchStore } from "./core/use-search-store.js";
export type { TuringContextValue, SearchStoreValue } from "./core/context.js";

// ── API ──
export {
  fetchSearch,
  postSearch,
  postStructuredSearch,
  applyFacetAction,
  searchParamsToRequest,
  requestToSearchParams,
  fetchChat,
  fetchChatEnabled,
  fetchLlmInstances,
  fetchLlmContextInfo,
  fetchAgentContextInfo,
  postChatConversation,
  postAgentChat,
  postPersonaChat,
  postLlmChat,
  postSemanticChat,
  fetchAnswer,
  fetchAnswerFreshness,
  createAnswerShare,
  fetchAnswerShare,
  recordAnswerSignal,
  deleteSiteConversationState,
  deleteAgentFlowState,
  fetchAutoComplete,
  fetchIntents,
  fetchSiteChatSlots,
  fetchAgentChatSlots,
  postSiteChatSlot,
  postSiteSlotExtract,
  postSiteSlotExtractMulti,
  postSiteHandoff,
  postSiteFlowSelect,
  fetchPersonaContentFit,
  fetchSitePersonas,
  postSiteFormSubmit,
  fetchSiteChatState,
  fetchSortOptions,
  fetchSimilar,
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
  postClick,
  fetchVoiceAvailability,
  postVoiceSession,
  postVoiceTranslation,
  CHAT_DISABLED_HINTS,
  LOW_CONFIDENCE_THRESHOLD,
} from "./core/api.js";
export type {
  SearchParams,
  PostChatConversationOptions,
  PostAgentChatOptions,
  PostPersonaChatOptions,
  PostLlmChatOptions,
  PostSemanticChatOptions,
  FetchAnswerOptions,
  AnswerStreamHandlers,
  AnswerResult,
  AnswerFilters,
  AnswerShareSnapshot,
  TurAnswerSignalKind,
  ChatEnabledResponse,
  ChatDisabledReason,
  ClickTrackParams,
  TurChatSessionSlots,
  TurWorkspaceArtifact,
  TurWorkspaceArtifacts,
  TurWorkspaceEvent,
  TurChatSlotWriteResponse,
  TurChatSlotExtractResponse,
  TurChatHandoffChannel,
  TurChatHandoffResponse,
  TurChatFlowSelectResponse,
  TurPersonaOption,
  TurPersonaPersonality,
  TurContentFit,
  TurContentFitMisfit,
  TurChatConversationState,
  TurSimilarMode,
  TurSimilarResult,
  FetchSimilarParams,
  TurNLFilter,
  TurNLDroppedFilter,
  TurNLFilters,
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
  TurVoiceAvailability,
  TurVoiceSession,
  PostVoiceSessionOptions,
} from "./core/api.js";

// ── Session ──
export {
  getOrCreateTurSession,
  readTurSession,
  clearTurSession,
  TUR_SESSION_DEFAULT_COOKIE_NAME,
  TUR_SESSION_DEFAULT_TTL_SECONDS,
} from "./core/session.js";
export type { TurSessionOptions } from "./core/session.js";

// ── Errors ──
//
// T1012 — re-exported from `@viglet/turing-sdk`, not re-declared. `instanceof`
// compares constructor identity, so a second pair defined here would not match
// what the vanilla SDK throws and a consumer of both would be back to reading
// messages. A non-2xx throws TuringHttpError with a numeric `status`; a request
// that produced no usable response throws TuringTransportError.
export { TuringHttpError, TuringTransportError, problemOf } from "@viglet/turing-sdk";

// ── Resolve utilities ──
export { resolveDocuments, resolveGroups, resolveStructuredGroups } from "./core/resolve.js";
// T881 — the rest of the response, projected once for both shapes. Owned by the
// vanilla SDK so an EDS block gets the same answer this app's page does.
export { toSearchView } from "@viglet/turing-sdk";
export type {
  ProjectableSearchStore,
  SearchView,
  SearchLocaleView,
  SearchSpellCheckView,
  AppliedFilterView,
} from "@viglet/turing-sdk";

// ── Hooks ──
export { useTuringSearch } from "./hooks/use-turing-search.js";
export type { UseTuringSearchReturn, UseTuringSearchOptions } from "./hooks/use-turing-search.js";
export { useTuringAutoComplete } from "./hooks/use-turing-autocomplete.js";
export type { UseTuringAutoCompleteReturn } from "./hooks/use-turing-autocomplete.js";
export { useTuringSortOptions } from "./hooks/use-turing-sort-options.js";
export type { UseTuringSortOptionsReturn } from "./hooks/use-turing-sort-options.js";
export { useTuringUrlSearch } from "./hooks/use-turing-url-search.js";
export type { UseTuringUrlSearchReturn } from "./hooks/use-turing-url-search.js";
export { useTuringSearchHistory } from "./hooks/use-turing-search-history.js";
export type { UseTuringSearchHistoryReturn, SearchHistoryEntry } from "./hooks/use-turing-search-history.js";

// ── New composable hooks (v2026.3) ──
export { useTuringDocument } from "./hooks/use-turing-document.js";
export type { TuringDocumentHelper } from "./hooks/use-turing-document.js";
export { useTuringFacets } from "./hooks/use-turing-facets.js";
export type { UseTuringFacetsReturn, EnrichedFacetGroup, EnrichedFacetItem } from "./hooks/use-turing-facets.js";
export { useTuringPagination } from "./hooks/use-turing-pagination.js";
export type { UseTuringPaginationReturn, PaginationPage } from "./hooks/use-turing-pagination.js";
export { useTuringTabs } from "./hooks/use-turing-tabs.js";
export type { UseTuringTabsReturn, UseTuringTabsOptions, TabDefinition, EnrichedTab } from "./hooks/use-turing-tabs.js";
export { useTuringChat } from "./hooks/use-turing-chat.js";
export type { UseTuringChatReturn, UseTuringChatOptions, UseTuringChatAgent, ChatMessage, ChatStatus, SendOverrides } from "./hooks/use-turing-chat.js";
export { useGenerativeUI } from "./hooks/use-turing-generative-ui.js";
export type { UseGenerativeUIReturn } from "./hooks/use-turing-generative-ui.js";
export { useAnswerAsApp } from "./hooks/use-turing-answer-as-app.js";
export type { UseAnswerAsAppReturn } from "./hooks/use-turing-answer-as-app.js";
export { useCoBrowseSearch } from "./hooks/use-turing-co-browse.js";
export type { UseCoBrowseReturn } from "./hooks/use-turing-co-browse.js";
export { useTuringProactiveCopilot } from "./hooks/use-turing-proactive-copilot.js";
export type {
  TuringProactiveSuggestion,
  UseTuringProactiveCopilotOptions,
  UseTuringProactiveCopilotReturn,
} from "./hooks/use-turing-proactive-copilot.js";
// T792 / §LIV.3 (Block BF) — Vectorless (Structured-Data) RAG copilot.
// T837 / §LXII.6 — the answer-first surface: headless hook + fused component.
export { useTuringAnswer } from "./hooks/use-turing-answer.js";
export type {
  UseTuringAnswerOptions,
  UseTuringAnswerReturn,
  TuringAnswerAskOptions,
  TuringAnswerStatus,
} from "./hooks/use-turing-answer.js";
export { useTuringCopilot } from "./hooks/use-turing-copilot.js";
export type {
  UseTuringCopilotOptions,
  UseTuringCopilotReturn,
  CopilotMessage,
  CopilotStatus,
} from "./hooks/use-turing-copilot.js";
export type { TurCatalogCitation, TurCatalogCopilotResult } from "./core/api.js";
export type {
  TurCatalogKitAvailability,
  TurCatalogKitRequest,
  TurCatalogKitProduct,
  TurCatalogKitStage,
  TurCatalogKitGuide,
  TurCatalogKitResult,
} from "./core/api.js";
export { useTuringUserMemory } from "./hooks/use-turing-user-memory.js";
export type {
  TuringUserMemory,
  UseTuringUserMemoryOptions,
  UseTuringUserMemoryReturn,
} from "./hooks/use-turing-user-memory.js";
export { useTuringLlmChat } from "./hooks/use-turing-llm-chat.js";
export type {
  UseTuringLlmChatReturn,
  UseTuringLlmChatOptions,
  LlmSendOverrides,
} from "./hooks/use-turing-llm-chat.js";

// ── Analytics (Block Z, T463) ──
export { useTuringAnalytics } from "./hooks/use-turing-analytics.js";
export type {
  UseTuringAnalyticsOptions,
  UseTuringAnalyticsReturn,
} from "./hooks/use-turing-analytics.js";
export {
  createTuringAnalytics,
  TURING_ANALYTICS_EVENTS,
} from "./core/analytics.js";
export type {
  TuringAnalytics,
  TuringAnalyticsOptions,
  TuringAnalyticsContext,
  TuringAnalyticsEvent,
  TuringAnalyticsEventName,
  TuringAnalyticsSink,
  TuringAnalyticsValue,
} from "./core/analytics.js";
export { googleAnalyticsSink, onEventSink, debugSink } from "./core/analytics-sinks.js";
export type {
  GoogleAnalyticsSinkOptions,
  DebugSinkOptions,
} from "./core/analytics-sinks.js";
export { createAbandonmentWatcher } from "./core/analytics-lifecycle.js";
export type {
  AbandonmentOptions,
  AbandonmentReason,
  AbandonmentWatcher,
} from "./core/analytics-lifecycle.js";
export { useTuringSemanticChat } from "./hooks/use-turing-semantic-chat.js";
export type {
  UseTuringSemanticChatReturn,
  UseTuringSemanticChatOptions,
} from "./hooks/use-turing-semantic-chat.js";
export { useTuringIntents } from "./hooks/use-turing-intents.js";
export type { UseTuringIntentsReturn } from "./hooks/use-turing-intents.js";
export { useTuringSlots } from "./hooks/use-turing-slots.js";
export type {
  UseTuringSlotsReturn,
  UseTuringSlotsOptions,
  UseTuringSlotsAgent,
  SlotsStatus,
} from "./hooks/use-turing-slots.js";
export { useTuringSlot } from "./hooks/use-turing-slot.js";
export type {
  UseTuringSlotReturn,
  UseTuringSlotOptions,
  UseTuringSlotParseMode,
} from "./hooks/use-turing-slot.js";
export { useTuringWorkspace } from "./hooks/use-turing-workspace.js";
export type {
  UseTuringWorkspaceReturn,
  UseTuringWorkspaceOptions,
  UseTuringWorkspaceAgent,
  WorkspaceStatus,
} from "./hooks/use-turing-workspace.js";
export { useTuringRoutineWaiting } from "./hooks/use-turing-routine-waiting.js";
export type {
  UseTuringRoutineWaitingReturn,
  UseTuringRoutineWaitingOptions,
} from "./hooks/use-turing-routine-waiting.js";
export { useTuringScheduleAgentWaiting } from "./hooks/use-turing-schedule-agent-waiting.js";
export type {
  UseTuringScheduleAgentWaitingReturn,
  UseTuringScheduleAgentWaitingOptions,
} from "./hooks/use-turing-schedule-agent-waiting.js";
export { useTuringLlmInstances } from "./hooks/use-turing-llm-instances.js";
export type {
  UseTuringLlmInstancesReturn,
  UseTuringLlmInstancesOptions,
} from "./hooks/use-turing-llm-instances.js";
export { useTuringSlotWriter } from "./hooks/use-turing-slot-writer.js";
export type {
  UseTuringSlotWriterReturn,
  UseTuringSlotWriterOptions,
  SlotWriteStatus,
} from "./hooks/use-turing-slot-writer.js";
export { useTuringSlotExtract } from "./hooks/use-turing-slot-extract.js";
export type {
  UseTuringSlotExtractReturn,
  UseTuringSlotExtractOptions,
  SlotExtractStatus,
} from "./hooks/use-turing-slot-extract.js";
export { useTuringHandoff } from "./hooks/use-turing-handoff.js";
export type {
  UseTuringHandoffReturn,
  UseTuringHandoffOptions,
  HandoffStatus,
} from "./hooks/use-turing-handoff.js";
export { useTuringVoice } from "./hooks/use-turing-voice.js";
export type {
  UseTuringVoiceReturn,
  UseTuringVoiceOptions,
} from "./hooks/use-turing-voice.js";
export { useTuringRealtimeVoice } from "./hooks/use-turing-realtime-voice.js";
export type {
  UseTuringRealtimeVoiceReturn,
  UseTuringRealtimeVoiceOptions,
  RealtimeVoiceStatus,
  RealtimeToolCall,
} from "./hooks/use-turing-realtime-voice.js";
export { useTuringFlowState } from "./hooks/use-turing-flow-state.js";
export type {
  UseTuringFlowStateReturn,
  UseTuringFlowStateOptions,
} from "./hooks/use-turing-flow-state.js";
export { useTuringExperiment } from "./hooks/use-turing-experiment.js";
export type { UseTuringExperimentReturn } from "./hooks/use-turing-experiment.js";
export { useTuringFlowChooser } from "./hooks/use-turing-flow-chooser.js";
export type {
  UseTuringFlowChooserReturn,
  UseTuringFlowChooserOptions,
  FlowChooserStatus,
} from "./hooks/use-turing-flow-chooser.js";
export { useTuringClickTracking } from "./hooks/use-turing-click-tracking.js";
export type { UseTuringClickTrackingReturn } from "./hooks/use-turing-click-tracking.js";

// ── Block S — SDK parity hooks (v2026.3.4) ──
export { useTuringSimilar } from "./hooks/use-turing-similar.js";
export type {
  UseTuringSimilarReturn,
  UseTuringSimilarOptions,
} from "./hooks/use-turing-similar.js";
export { useTuringSpellCheck } from "./hooks/use-turing-spell-check.js";
export { useTuringRelatedTerms } from "./hooks/use-turing-related-terms.js";
export type {
  UseTuringRelatedTermsReturn,
  UseTuringRelatedTermsOptions,
} from "./hooks/use-turing-related-terms.js";
export type {
  UseTuringSpellCheckReturn,
  UseTuringSpellCheckOptions,
} from "./hooks/use-turing-spell-check.js";
export { useTuringSlotUpload } from "./hooks/use-turing-slot-upload.js";
export type {
  UseTuringSlotUploadReturn,
  UseTuringSlotUploadOptions,
  SlotUploadStatus,
} from "./hooks/use-turing-slot-upload.js";
export { useTuringResume } from "./hooks/use-turing-resume.js";
export type {
  UseTuringResumeReturn,
  UseTuringResumeOptions,
  ResumeStatus,
} from "./hooks/use-turing-resume.js";
export { useTuringDslSearch } from "./hooks/use-turing-dsl-search.js";
export type {
  UseTuringDslSearchReturn,
  UseTuringDslSearchOptions,
  DslSearchStatus,
} from "./hooks/use-turing-dsl-search.js";
export { useTuringPlatformInfo } from "./hooks/use-turing-platform-info.js";
export type {
  UseTuringPlatformInfoReturn,
  UseTuringPlatformInfoOptions,
} from "./hooks/use-turing-platform-info.js";

// ── UI Components ──
// TuringSearchBar / TuringResultList / TuringPagination moved to
// @viglet/turing-react-ui (T306) and are re-exported below alongside the other
// headless components, so SDK consumers' imports are unchanged.
export { TuringSearchField } from "./ui/TuringSearchField.js";
export { TuringWorkspacePanel } from "./ui/TuringWorkspacePanel.js";
export type { TuringWorkspacePanelProps } from "./ui/TuringWorkspacePanel.js";
export { TuringCopilot } from "./ui/TuringCopilot.js";
export type { TuringCopilotProps, TuringCopilotClassNames } from "./ui/TuringCopilot.js";
// T1216 — `TuringAnswer` renders markdown, which loads the optional
// react-markdown peer, so it is imported from `@viglet/turing-react-sdk/TuringAnswer`.
// Its types stay here: they load nothing at runtime.
export type {
  TuringAnswerProps,
  TuringAnswerClassNames,
  TuringAnswerLabels,
} from "./ui/TuringAnswer.js";

// ── Headless UI (re-exported from @viglet/turing-react-ui) ──
// Single import surface for the admin console: it can pull the design-agnostic
// chat / rich-content components from this SDK alongside the API hooks above.
// react-ui itself stays axios-free so viglet.com keeps consuming it directly
// (core SDK + react-ui + its own Next transport) without ever seeing axios.
// react-markdown / @terrastruct/d2 are optional peers here, mirroring react-ui:
// D2 is lazy-loaded, and the two components that import react-markdown
// statically are on subpaths (T1216), `@viglet/turing-react-sdk/TuringMarkdown`
// and `@viglet/turing-react-sdk/TuringAnswer`, so this root loads without either.
export {
  TuringHtmlSandbox,
  TuringRichContent,
  splitRichContent,
  resolveSandboxUrl,
  TuringD2Diagram,
  TuringCopyButton,
  TuringThinkingDots,
  TuringCodeBlock,
  TuringChatMessage,
  TuringSourceChips,
  TuringToolActivity,
  TuringGenerativeContent,
  TuringCitedAnswer,
  segmentCitedAnswer,
  TuringSearchBar,
  TuringResultList,
  // T882 — a result's facet chips as actions in both response shapes.
  TuringDocumentChips,
  TuringPagination,
  TuringSearchFieldView,
  TuringWorkspacePanelView,
  formatBytes,
  TURING_UI_THEME_CLASS,
  TURING_UI_TOKENS,
} from "@viglet/turing-react-ui";
export type {
  TuringHtmlSandboxProps,
  TuringHtmlSandboxClassNames,
  TuringHtmlSandboxLabels,
  TuringHtmlSandboxIcons,
  TuringRichContentProps,
  TuringRichContentClassNames,
  TuringRichSegment,
  TuringMarkdownSegmentProps,
  TuringD2Props,
  TuringHtmlSegmentProps,
  TuringMarkdownProps,
  TuringD2DiagramProps,
  TuringD2DiagramClassNames,
  TuringD2DiagramLabels,
  TuringD2Renderer,
  TuringCopyButtonProps,
  TuringCopyButtonClassNames,
  TuringCopyButtonLabels,
  TuringCopyButtonIcons,
  TuringThinkingDotsProps,
  TuringThinkingDotsClassNames,
  TuringCodeBlockProps,
  TuringCodeBlockClassNames,
  TuringCodeBlockLabels,
  TuringChatMessageProps,
  TuringChatMessageRole,
  TuringChatMessageClassNames,
  TuringSourceChipsProps,
  TuringSourceChipsClassNames,
  TuringSourceChipsLabels,
  TuringSourceChipsIcons,
  TuringRagSource,
  TuringSourceConfidence,
  TuringToolActivityProps,
  TuringToolActivityClassNames,
  TuringToolActivityLabels,
  TuringToolActivityIcons,
  TuringToolCall,
  TuringGenerativeContentProps,
  TuringGenerativeContentClassNames,
  TuringGenerativeItem,
  TuringGenerativeComponentProps,
  TuringGenerativeRegistry,
  TuringCitedAnswerProps,
  TuringCitedAnswerClassNames,
  TuringCitation,
  TuringCitedMark,
  TuringSearchBarProps,
  TuringSearchBarInputProps,
  TuringResultListProps,
  TuringResolvedDocument,
  TuringRawDocument,
  TuringDocumentChipsProps,
  TuringDocumentChip,
  TuringFacetToggleTarget,
  TuringFacetAction,
  TuringPaginationProps,
  TuringPaginationItemData,
  TuringSearchFieldViewProps,
  TuringSearchFieldViewContextValue,
  TuringSearchFieldViewInputProps,
  TuringSearchFieldViewButtonProps,
  TuringSearchFieldViewDropdownProps,
  TuringWorkspacePanelViewProps,
  TuringWorkspaceArtifact,
  TuringWorkspaceStatus,
  TuringUiTheme,
  TuringUiTokenName,
} from "@viglet/turing-react-ui";

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
  // T710 / §XLV — POST-native search: request + link-free response.
  TurSearchRequest,
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
  TurSpellCheck,
  // T880 — on the wire since T709, typed since T880.
  TurSimilarDocument,
  TurSpotlightDocument,
  TurRelatedTermSuggestion,
  TurLocaleItem,
  TurSortOption,
  TurChatResponse,
  TurChatConversationMessage,
  TurChatConversationResponse,
  TurChatStreamEvent,
  TurChatSource,
  TurChatCitation,
  TurAnswerCitation,
  TurAnswerArtifact,
  TurAnswerTrace,
  TurAnswerFreshnessSource,
  TurAnswerFreshnessVerdict,
  TurAnswerFreshnessStatus,
  TurAnswerShareView,
  TurChatGrounding,
  TurChatSecondOpinion,
  TurSearchSuggestions,
  TurChatToolCall,
  TurChatForm,
  TurChatFormField,
  TurChatFormSubmitResponse,
  TurIntent,
  TurIntentAction,
  TurLlmInstance,
  TurLlmVendor,
  TurLlmContextInfo,
  SearchStatus,
  SearchState,
  ResolvedDocument,
  ResolvedGroup,
  TurGroupBean,
  ResultItemProps,
  FacetGroupProps,
  PaginationItemProps,
} from "./core/types.js";
