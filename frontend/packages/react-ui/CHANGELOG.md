# @viglet/turing-react-ui

## 2026.3.8

- a11y + SSR hardening pass (T304): the `TuringHtmlSandbox` fullscreen portal is now a labelled modal dialog (`role="dialog"` + `aria-modal` + `aria-label`) with a focus trap (Tab/Shift+Tab cycle inside), initial focus into the dialog, and focus restoration to the trigger on close. The code/preview toggle gained an accessible name for icon-only use. `TuringD2Diagram` exposes its loading state as an `aria-live` status, the rendered SVG as a labelled `role="img"` (new `labels.diagram`), and the source fallback caption as a `role="alert"`. `TuringCopyButton` announces its "Copied" state via `aria-live`. SSR/RSC-safety audited across all components — client-only `document`/`navigator`/portal access stays guarded and effect-scoped.

- Split `TuringSearchField` into a headless view + a data-wiring wrapper (T307).
  The presentation half — the compound `Input` / `Button` / `Dropdown` with the
  suggestions/history dropdown — now ships from `@viglet/turing-react-ui` as
  `TuringSearchFieldView`, driven entirely by props/context with **no baked-in
  styling** (the old Tailwind defaults — `w-full px-4 py-2.5 hover:bg-gray-100 …`
  — were stripped to honor the headless contract; visuals now come from
  `className` + the dropdown render props). The orchestrating Root (autocomplete,
  search-history, url-search hooks and the results→history save effect) stays in
  `@viglet/turing-react-sdk` as `TuringSearchField`, which feeds the view its
  state/callbacks and re-attaches `.Input` / `.Button` / `.Dropdown` — so consumer
  markup (`<TuringSearchField.Input />`, the marketplace apps) is unchanged. Lets
  viglet.com reuse the search field without pulling the axios SDK.

- Split `TuringWorkspacePanel` into a headless view + a thin SSE-wiring wrapper
  (T308). The presentation half — the `aside/ul/li` structure, the default
  download-link row, and the `formatBytes` helper — now ships from
  `@viglet/turing-react-ui` as `TuringWorkspacePanelView`, taking `artifacts` +
  `status` (plus `title`/`itemComponent`/`emptyComponent`) as props and carrying
  no SSE/API coupling. `TuringWorkspacePanel` stays in `@viglet/turing-react-sdk`
  as a one-call wrapper that subscribes via `useTuringWorkspace` and forwards the
  snapshot to the view — same props, so consumers are unchanged. The artifact
  shape is redeclared locally in react-ui (structurally compatible with the SDK's
  `TurWorkspaceArtifact`) to keep the package zero-runtime-dep, letting viglet.com
  render the workspace list with its own Next transport, axios-free.

- T710 — the React SDK + react-ui go POST-native.

  `useTuringSearch` and the URL-synced `TuringProvider` store now fetch over
  `POST /sn/{site}/search/query` with a JSON body and the link-free `STRUCTURED`
  response (`postNative`, default `true`), so a filter value containing
  `& = + # %` or a space can no longer be mis-read as query structure. New
  `structured` / `request` state plus `toggleFacet` / `clearFacet` /
  `clearAllFilters` / `goToPage` / `applySpellCheck` / `searchRequest`;
  `navigate(href)` is kept but `@deprecated`.

  **URL sync is unchanged.** The browser URL stays the source of truth for search
  state — deep-linkable, bookmarkable, shareable search URLs are a kept feature.
  What moved is the *API transport*, which is where the bug class lived: facet
  toggles now write filter queries into the URL from structured data instead of
  parsing a server-rendered href.

  `useTuringFacets` and `useTuringPagination` keep their exact public shape
  (`facetGroups[].facets[].toggle`, `pages[].select`, …) and switch to structured
  actions underneath, so consuming JSX needs no change.

  `applyFacetAction`, `searchParamsToRequest` and `requestToSearchParams` are
  re-exported from `@viglet/turing-sdk` rather than duplicated, so the
  ADD/REMOVE/REPLACE semantics cannot drift between the two SDKs.

  **react-ui `TuringPagination`:** `href` is now optional, `current` is honoured,
  and a new `onSelectPage(page)` navigates by page number (it wins over the now
  optional, deprecated `onNavigate`).

  **Behaviour change (opt-out available):** POST-native, `data` is `null` and
  `structured` carries the response. `documents` / `groups` / `status` / `error` /
  `params` are unchanged. A consumer reading `data` or calling `navigate(href)`
  should pass `postNative: false` (hook option, or on `urlSync`).

- T713 — the href round-trip is on notice (deprecated, not removed).

  Block AV's whole point is that a client should never parse a server-rendered
  search URL back into params: that round-trip is what let a facet value
  containing `& = + # %` or a space be re-read as query structure. T709/T710 made
  POST-native the default and T714 migrated the last consumers, so as of now
  nothing in this repository calls the round-trip.

  **What changed.** `parseHrefToParams` now logs a **one-time** console notice
  naming the structured replacements, and is marked `@deprecated` along with
  `navigate(href)` on the vanilla controller, `useTuringSearch`, the URL-synced
  store, and `onNavigate` on react-ui's `TuringPagination`. Everything still
  works exactly as before — this release removes nothing.

  The notice lives at the single parse site, so a caller sees one line per page
  rather than one per facet chip, and `navigate(href)` routes through it instead
  of warning again. react-ui emits no notice at all: it only *forwards* an href
  to the host, it never parses one (and it stays zero-runtime-dependency, so it
  could not import the notice anyway).

  **Fixed on the way through:** `useCoBrowseSearch`'s `toggle_facet` and
  `clear_facets` client tools looked their target up in `store.data`, which is
  `null` POST-native — so both had been failing in the *default* mode since T710.
  They now resolve against `store.structured` and fall back to the legacy lookup
  only when running with `postNative: false`.

  **What is NOT deprecated:** the server-rendered `link` / `href` /
  `cleanUpLink` / `cleanUpFacets` / spell-check `link` fields themselves. They
  are what make facet pages deep-linkable, bookmarkable, SEO-crawlable and usable
  without JavaScript, and they are staying — render them as real anchors. Only
  the SDK *parsing* them is going away, in the next major.

  **Size note:** contrary to what the T709 changeset anticipated, this does not
  claw back gzip budget — a deprecation notice adds bytes rather than removing
  them. Both entry budgets were raised (`turing-sdk.js` 17500 → 17700,
  `turing-sdk.umd.cjs` 15500 → 15750). The reclaim happens at the actual removal.

- T864 — the link-free response is finally link-free.

  The `STRUCTURED` search response dropped every href except one: each document's
  `metadata[]` — its facet chips — still carried `{href, text}` pointing back at
  `/api/sn/{site}/search?…&fq[]=…`. A consumer rendering those chips was still
  parsing URLs, which is the whole bug class this contract exists to remove.

  **What changed.** In the structured response, a document's `metadata` entries are
  now `{ field, value, filterQuery, text }` — the same shape as a structured facet
  item, because it is the same action: add `filterQuery` to `request.fq` and
  re-send. New types: `TurStructuredDocumentMetadata`, `TurStructuredDocument`,
  `TurStructuredResults`.

  **What you may need to do.** If you render `doc.raw.metadata` and read `href`,
  narrow first — `"href" in chip` is the legacy shape, `"filterQuery" in chip` is
  the link-free one. `ResolvedDocument.raw` is now
  `TurDocument | TurStructuredDocument`; everything the resolver produces
  (`url`/`title`/`description`/`date`/`image`/`text`) is unchanged, and
  `resolveDocuments` accepts either response exactly as before.

  react-ui's `TuringRawDocument.metadata` widened to the same union. `text` is
  common to both shapes, so a component that only renders the chip label needs no
  change.

  The legacy (`postNative: false`) response is untouched: it still carries
  `{href, text}`, byte for byte.
