/**
 * `@viglet/turing-react-ui` — headless React UI primitives for Viglet Turing ES.
 *
 * Design-agnostic chat / rich-content components shared across the admin
 * console and viglet.com. The package carries NO API or axios dependency (that
 * lives in `@viglet/turing-react-sdk`) and NO baked-in styling — every
 * component is skinned by the host app via `classNames` / `labels` / `icons`,
 * so each surface keeps its own look while sharing one implementation. As an
 * alternative to per-slot `classNames`, apps can import the optional token
 * stylesheet (`@viglet/turing-react-ui/styles.css`) and theme via `--turing-ui-*`
 * CSS custom properties on a `.turing-ui-theme` wrapper (T305).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.1
 */
export { TuringHtmlSandbox } from "./TuringHtmlSandbox.js";
export type {
  TuringHtmlSandboxProps,
  TuringHtmlSandboxClassNames,
  TuringHtmlSandboxLabels,
  TuringHtmlSandboxIcons,
} from "./TuringHtmlSandbox.js";

export { TuringRichContent, splitRichContent } from "./TuringRichContent.js";
export type {
  TuringRichContentProps,
  TuringRichContentClassNames,
  TuringRichSegment,
  TuringMarkdownSegmentProps,
  TuringD2Props,
  TuringHtmlSegmentProps,
} from "./TuringRichContent.js";

export { TuringMarkdown, resolveSandboxUrl } from "./TuringMarkdown.js";
export type { TuringMarkdownProps } from "./TuringMarkdown.js";

export { TuringD2Diagram } from "./TuringD2Diagram.js";
export type {
  TuringD2DiagramProps,
  TuringD2DiagramClassNames,
  TuringD2DiagramLabels,
  TuringD2Renderer,
} from "./TuringD2Diagram.js";

export { TuringCopyButton } from "./TuringCopyButton.js";
export type {
  TuringCopyButtonProps,
  TuringCopyButtonClassNames,
  TuringCopyButtonLabels,
  TuringCopyButtonIcons,
} from "./TuringCopyButton.js";

export { TuringThinkingDots } from "./TuringThinkingDots.js";
export type {
  TuringThinkingDotsProps,
  TuringThinkingDotsClassNames,
} from "./TuringThinkingDots.js";

export { TuringCodeBlock } from "./TuringCodeBlock.js";
export type {
  TuringCodeBlockProps,
  TuringCodeBlockClassNames,
  TuringCodeBlockLabels,
} from "./TuringCodeBlock.js";

export { TuringChatMessage } from "./TuringChatMessage.js";
export type {
  TuringChatMessageProps,
  TuringChatMessageRole,
  TuringChatMessageClassNames,
} from "./TuringChatMessage.js";

export { TuringSourceChips } from "./TuringSourceChips.js";
export type {
  TuringSourceChipsProps,
  TuringSourceChipsClassNames,
  TuringSourceChipsLabels,
  TuringSourceChipsIcons,
  TuringRagSource,
  TuringSourceConfidence,
} from "./TuringSourceChips.js";

export { TuringToolActivity } from "./TuringToolActivity.js";
export type {
  TuringToolActivityProps,
  TuringToolActivityClassNames,
  TuringToolActivityLabels,
  TuringToolActivityIcons,
  TuringToolCall,
} from "./TuringToolActivity.js";

export { TuringGenerativeContent } from "./TuringGenerativeContent.js";
export type {
  TuringGenerativeContentProps,
  TuringGenerativeContentClassNames,
  TuringGenerativeItem,
  TuringGenerativeComponentProps,
  TuringGenerativeRegistry,
} from "./TuringGenerativeContent.js";

// T636 / §XXVII.4 — persona-adaptive & content-validation primitives
// ("same question, different eyes" + "validate content as persona X").
export { TuringPersonaPicker } from "./TuringPersonaPicker.js";
export type {
  TuringPersonaPickerProps,
  TuringPersonaPickerClassNames,
  TuringPersonaPickerLabels,
  TuringPersonaPickerIcons,
  TuringPersonaOption,
} from "./TuringPersonaPicker.js";

export { TuringContentFit } from "./TuringContentFit.js";
export type {
  TuringContentFitProps,
  TuringContentFitClassNames,
  TuringContentFitLabels,
  TuringContentFitIcons,
  TuringContentFitResult,
  TuringContentFitMisfit,
} from "./TuringContentFit.js";

// T442 / §XXIII.1 — answer-as-an-app generative components (comparison table,
// spec card, configurator) keyed by the built-in client-tool names. Headless,
// zero-dep; register against `useGenerativeUI` + `TuringGenerativeContent`.
export {
  TuringComparisonTable,
  TuringSpecCard,
  TuringConfigurator,
  ANSWER_AS_APP_COMPONENTS,
  formatTypedValue,
} from "./TuringAnswerAsApp.js";
export type {
  TuringAppFieldType,
  TuringAppControlType,
  TuringAppColumn,
  TuringAppField,
  TuringAppAction,
  TuringAppControl,
  TuringComparisonTableProps,
  TuringComparisonTableClassNames,
  TuringSpecCardProps,
  TuringSpecCardClassNames,
  TuringConfiguratorProps,
  TuringConfiguratorClassNames,
} from "./TuringAnswerAsApp.js";

export { TuringCitedAnswer, segmentCitedAnswer } from "./TuringCitedAnswer.js";
export type {
  TuringCitedAnswerProps,
  TuringCitedAnswerClassNames,
  TuringCitation,
  TuringCitedMark,
} from "./TuringCitedAnswer.js";

// T836 / §LXII.5 — inline `[n]` citation anchors for markdown answers: a rehype
// plugin turning each `[n]` into a clickable superscript that scroll-links +
// highlights the matching source card, plus the `components` map that renders it.
export { rehypeCitationAnchors } from "./rehypeCitationAnchors.js";
export type { RehypeCitationAnchorsOptions } from "./rehypeCitationAnchors.js";
export { TuringCitationAnchor, citationMarkdownComponents } from "./TuringCitationAnchor.js";

// T839 / §LXII.8 — the "why this answer" auditable provenance panel.
export { TuringAnswerProvenance } from "./TuringAnswerProvenance.js";
export type {
  TuringAnswerProvenanceProps,
  TuringAnswerProvenanceClassNames,
  TuringAnswerProvenanceLabels,
  TuringAnswerProvenanceTrace,
  TuringAnswerProvenanceCitation,
} from "./TuringAnswerProvenance.js";
export type {
  TuringCitationAnchorProps,
  TuringCitationAnchorClassNames,
  TuringCitationAnchorData,
  CitationMarkdownComponentsOptions,
} from "./TuringCitationAnchor.js";

// Design-less search primitives migrated from `@viglet/turing-react-sdk` (T306).
// Pure renderers (props + render-props, no hook/API coupling); their structural
// data types are redeclared locally to keep this package zero-runtime-dep.
export { TuringSearchBar } from "./TuringSearchBar.js";
export type {
  TuringSearchBarProps,
  TuringSearchBarInputProps,
} from "./TuringSearchBar.js";

export { TuringResultList } from "./TuringResultList.js";
export type {
  TuringResultListProps,
  TuringResolvedDocument,
  TuringRawDocument,
} from "./TuringResultList.js";

// T882 — a result's facet chips as actions, not as dead badges.
export { TuringDocumentChips } from "./TuringDocumentChips.js";
export type {
  TuringDocumentChipsProps,
  TuringDocumentChip,
  TuringFacetToggleTarget,
  TuringFacetAction,
} from "./TuringDocumentChips.js";

export { TuringPagination } from "./TuringPagination.js";
export type {
  TuringPaginationProps,
  TuringPaginationItemData,
} from "./TuringPagination.js";

// Headless view half of the legacy `TuringSearchField`, split out in T307. The
// orchestrating Root (autocomplete + history + url-search hooks) stays in
// `@viglet/turing-react-sdk` and feeds this view its state/callbacks via props;
// the baked Tailwind defaults were stripped here to honor the headless contract.
export { TuringSearchFieldView } from "./TuringSearchFieldView.js";
export type {
  TuringSearchFieldViewProps,
  TuringSearchFieldViewContextValue,
  TuringSearchFieldViewInputProps,
  TuringSearchFieldViewButtonProps,
  TuringSearchFieldViewDropdownProps,
} from "./TuringSearchFieldView.js";

// Headless view half of the legacy `TuringWorkspacePanel`, split out in T308.
// The SSE subscription (`useTuringWorkspace`) stays in `@viglet/turing-react-sdk`
// and forwards `artifacts` + `status` to this view; `formatBytes` ships here too.
export {
  TuringWorkspacePanelView,
  formatBytes,
} from "./TuringWorkspacePanelView.js";
export type {
  TuringWorkspacePanelViewProps,
  TuringWorkspaceArtifact,
  TuringWorkspaceStatus,
} from "./TuringWorkspacePanelView.js";

// Optional token theming (T305) — an alternative to `classNames`. Import the
// stylesheet (`@viglet/turing-react-ui/styles.css`) and drive the look via
// `--turing-ui-*` custom properties on a `.turing-ui-theme` wrapper.
export {
  TURING_UI_THEME_CLASS,
  TURING_UI_TOKENS,
} from "./tokens.js";
export type { TuringUiTheme, TuringUiTokenName } from "./tokens.js";
