# @viglet/turing-react-sdk

## 2026.3.11

- `fetchSimilar` sends its request as a JSON body to `POST /sn/{site}/search/similar/query` (T1239) instead of a query string, in both SDKs, so a scope of several sites and filters never has to survive a URL. `similarRequestBody` builds that body and is exported. Needs a Turing server with the POST endpoint (2026.3.8 or later).

- The controllers keep a failed call's problem details on `errorProblem`, beside the `error` text (T1240): the answer, chat, search, research, similar and slots controllers, and the React `useTuringAnswer` and `useTuringChat` hooks. A 429 `quota-exhausted` now tells a subscriber `errorProblem.resetsAt`. `problemOf(err)` returns the problem details of any caught error, and is exported from both packages.

- Every hook that returns an `error` now returns `errorProblem` beside it, and so does the search provider's store (T1240): `useTuringSearch`, `useTuringSimilar`, `useTuringCopilot`, `useTuringLlmChat`, `useTuringSemanticChat`, `useTuringSlots` and the rest. The React SDK's `problemOf(err)` also reads the problem body of a call made through axios, so a 429 `quota-exhausted` on a JSON call carries `resetsAt` too.

## 2026.3.10

- The catalog copilot, its kit and nl-filters take `filters` (field to accepted values) that every read of the call stays inside (T1213), for an SN site several sites share: pass it in the request of `postCopilot` and `postCopilotKit`, in the options of the vanilla `postNlFilters`, or as the last argument of the React SDK's `postCopilot` and `postNlFilters`.

- Load in plain Node, not only through a bundler (T1214): relative imports in the emitted JavaScript and type declarations now carry their `.js` extension, and `@viglet/turing-react-sdk` declares `"type": "module"`. A Node script, a test runner in plain Node mode or server rendering that keeps `node_modules` external can now `import` these packages, and a `moduleResolution: "NodeNext"` consumer gets the real types of `@viglet/turing-sdk` instead of `any`.

- The `@viglet/turing-react-ui` root no longer needs the optional `react-markdown` peer (T1215): a project with only `react` and `react-dom` can import it. `TuringMarkdown` is no longer exported from the root; import it from `@viglet/turing-react-ui/TuringMarkdown` (or from `@viglet/turing-react-sdk`, which still re-exports it). `resolveSandboxUrl` and the `TuringMarkdownProps` type stay on the root.

- The `@viglet/turing-react-sdk` root no longer needs the optional `react-markdown` peer (T1216), so a project that uses only the hooks need not install it. `TuringMarkdown` and `TuringAnswer` moved off the root: import them from `@viglet/turing-react-sdk/TuringMarkdown` and `@viglet/turing-react-sdk/TuringAnswer`. Their prop types stay on the root. The package now declares an `exports` map with those two subpaths.

- `fetchSearch` sends the `fqAnd`, `fqOr`, `fqOperator` and `fqItemOperator` its search params take, as `fq.and[]`, `fq.or[]`, `fq.op` and `fqi.op` (T1223). Before, the GET search dropped them, so `fqItemOperator: "OR"` still intersected two values of one field.

- `TuringHttpError` carries the problem-details body of an error answer on `problem` (T1234), typed with the RFC 7807 members and the quota fields: a 429 `quota-exhausted` now tells a page `problem.resetsAt`, when AI answers come back. The `TuringProblem` type is exported. `problem` is `undefined` when the error body is not JSON.

## 2026.3.9

- T1012 — the React SDK throws the same error types as the vanilla SDK.

  `TuringHttpError` and `TuringTransportError` are now re-exported from the root
  and thrown by `core/api.ts`, where a non-2xx previously threw a plain
  `Error("HTTP 404: Not Found")` at nine call sites. So the two facts a caller
  needs — was this a transport failure or an answer, and what status was it — are
  fields rather than substrings of a message:

  ```ts
  import { TuringHttpError, TuringTransportError } from "@viglet/turing-react-sdk";

  try {
    await someHook.refetch();
  } catch (err) {
    if (err instanceof TuringHttpError && err.status === 404) renderEmpty();
    else if (err instanceof TuringTransportError) scheduleRetry();
    else throw err;
  }
  ```

  **The classes are re-exported, not re-declared.** `instanceof` compares
  constructor identity, so this package throws the very constructors
  `@viglet/turing-sdk` throws — an app using both SDKs matches one type per
  failure, not two. T1005 typed the vanilla SDK and left this one behind, which
  briefly meant the same HTTP status surfaced in two different shapes depending on
  which SDK made the call.

  **Not a behaviour change for a caller that only catches.** Both extend `Error`
  and keep a readable `message`. What changed shape: a caller matching on the old
  message text should read `status` instead.

  A request that arrives with nothing to read — a streaming endpoint with a null
  body, or a 2xx with an empty JSON body — now throws `TuringTransportError` too,
  because from a caller's side that is the same decision as a refused connection.
  `AbortError` from your own `AbortSignal` is rethrown untouched.

- T1077: read a document by id, whole.

  `fetchStoredDocuments(site, ids, locale?)` (the vanilla SDK takes the client
  first and an optional `{ signal }` last) calls the new
  `GET /api/sn/{site}/search/document?id=...` endpoint. It answers every stored
  field of each document, not highlighted or shortened, in the order the ids were
  given; ids the site does not hold are dropped. Passing several ids fetches
  several candidates in one request, so a detail page whose URL could hold more
  than one id no longer has to try each with its own search. When none of the ids
  is on the site the server answers 404.

  `createTuringApi(client)` in the vanilla SDK binds it too.

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

- Add an optional skill-mode pin to the agent chat surface (T325). A new
  `selectedSkillId` on `PostAgentChatOptions` (vanilla SDK) and
  `UseTuringChatOptions` (`useTuringChat`, React SDK) selects a single skill —
  by id or case-insensitive name — for the conversation, so the agent operates
  that one skill as a distinct "mode/flow" instead of offering the whole enabled
  skill set for progressive disclosure. The value rides the agent chat request
  body alongside `flowId`/`forcedVariant`; leaving it unset keeps the existing
  all-skills behaviour, so consumers are unchanged.

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

- T860 — `EnrichedFacetItem.link` stops lying POST-native.

  `useTuringFacets` built each item with `link: ""` in the link-free mode, because
  the structured response carries no href while the type declared `link: string`.
  That is a silent, plausible-looking bug on the consumer side: `<a
  href={facet.link}>` compiles, renders, and links to the current page.

  **What changed.** `link` is now optional (`link?: string`) and is *omitted*
  rather than emptied in POST-native mode. It is still populated in legacy
  (`postNative: false`) mode, where the server really does render an href.

  **What you may need to do.** Nothing if you use `facet.toggle` (the documented
  path). If you read `facet.link` — including in a React `key` like
  `` key={facet.label + facet.link} `` — TypeScript will now tell you it can be
  absent. Keys can simply use `facet.label`, which is unique within a group;
  anchors need an explicit fallback, because there is genuinely no URL to link to
  when the response is link-free.

  The type now says what was already true of the data, which is the point: this
  is the last place in the SDK where the link-free contract was papered over
  instead of expressed.

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
