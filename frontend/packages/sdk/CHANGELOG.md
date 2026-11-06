# @viglet/turing-sdk

## 2026.3.18

- `fetchSimilar` sends its request as a JSON body to `POST /sn/{site}/search/similar/query` (T1239) instead of a query string, in both SDKs, so a scope of several sites and filters never has to survive a URL. `similarRequestBody` builds that body and is exported. Needs a Turing server with the POST endpoint (2026.3.8 or later).

- The controllers keep a failed call's problem details on `errorProblem`, beside the `error` text (T1240): the answer, chat, search, research, similar and slots controllers, and the React `useTuringAnswer` and `useTuringChat` hooks. A 429 `quota-exhausted` now tells a subscriber `errorProblem.resetsAt`. `problemOf(err)` returns the problem details of any caught error, and is exported from both packages.

## 2026.3.17

- The catalog copilot, its kit and nl-filters take `filters` (field to accepted values) that every read of the call stays inside (T1213), for an SN site several sites share: pass it in the request of `postCopilot` and `postCopilotKit`, in the options of the vanilla `postNlFilters`, or as the last argument of the React SDK's `postCopilot` and `postNlFilters`.

- Load in plain Node, not only through a bundler (T1214): relative imports in the emitted JavaScript and type declarations now carry their `.js` extension, and `@viglet/turing-react-sdk` declares `"type": "module"`. A Node script, a test runner in plain Node mode or server rendering that keeps `node_modules` external can now `import` these packages, and a `moduleResolution: "NodeNext"` consumer gets the real types of `@viglet/turing-sdk` instead of `any`.

- `fetchSearch` sends the `fqAnd`, `fqOr`, `fqOperator` and `fqItemOperator` its search params take, as `fq.and[]`, `fq.or[]`, `fq.op` and `fqi.op` (T1223). Before, the GET search dropped them, so `fqItemOperator: "OR"` still intersected two values of one field.

- `TuringHttpError` carries the problem-details body of an error answer on `problem` (T1234), typed with the RFC 7807 members and the quota fields: a 429 `quota-exhausted` now tells a page `problem.resetsAt`, when AI answers come back. The `TuringProblem` type is exported. `problem` is `undefined` when the error body is not JSON.

## 2026.3.15

- T1005 — the status is a field, not a substring.

  Two new exports, `TuringHttpError` and `TuringTransportError`. A non-2xx throws
  the first, carrying `status` as a number alongside `statusText` and `url`. A
  request that produced no usable response — refused connection, offline, CORS, or
  a response whose body had nothing to read — throws the second, with the
  underlying failure on `cause`.

  Previously every failure was `new Error(\`HTTP ${status}: ${statusText}\`)` and a
  `fetch` rejection was let through untouched, so telling "the tenant is
  unreachable" from "the tenant said 404" from "the body was not readable" took a
  regex over `error.message` plus an `instanceof TypeError` check. A message is the
  one part of an error free to be reworded, so that coupling broke consumers
  silently. Match with `instanceof` instead:

  ```ts
  try {
    await fetchSearch(client, "site", params);
  } catch (err) {
    if (err instanceof TuringHttpError && err.status === 404) renderEmpty();
    else if (err instanceof TuringTransportError) scheduleRetry();
    else throw err;
  }
  ```

  **Not a behaviour change for a caller that only catches.** Both extend `Error`
  and keep a human-readable `message`. The one thing that changed shape: a caller
  matching on the old message text should match on `status` now.

  `AbortError` from the caller's own `AbortSignal` is rethrown untouched —
  cancellation is not a transport failure, and a search-as-you-type controller
  aborts on every keystroke.

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

- T1079: a call can be bounded.

  `createTuringClient({ baseURL, timeoutMs })` puts an upper bound on every request
  the client makes. When the limit elapses the request is aborted and the call
  rejects with the new `TuringTimeoutError`, which extends `TuringTransportError`
  and carries `timeoutMs` and `url`. A JSON call is bounded until its body is read;
  a raw or streaming call (`client.fetchRaw`, the SSE helpers) only until the
  response headers arrive, so a stream that has started is never cut off.

  Every API function takes an optional last argument, `{ signal }`, which is passed
  down to the request. The SSE helpers and the slot upload/extract functions read
  `signal` from the options object they already take. `createTuringApi(client)`
  now forwards every argument, so the bound functions take it too. An abort from
  that signal still rejects with its own `AbortError`, never with a timeout or
  transport error.

  ```ts
  const client = createTuringClient({ baseURL, timeoutMs: 8000 });
  try {
    await postStructuredSearch(client, "site", request);
  } catch (err) {
    if (err instanceof TuringTimeoutError) renderUnavailable();
    else throw err;
  }
  ```

  Without `timeoutMs` nothing changes: there is no limit and no extra signal.

- Add an optional skill-mode pin to the agent chat surface (T325). A new
  `selectedSkillId` on `PostAgentChatOptions` (vanilla SDK) and
  `UseTuringChatOptions` (`useTuringChat`, React SDK) selects a single skill —
  by id or case-insensitive name — for the conversation, so the agent operates
  that one skill as a distinct "mode/flow" instead of offering the whole enabled
  skill set for progressive disclosure. The value rides the agent chat request
  body alongside `flowId`/`forcedVariant`; leaving it unset keeps the existing
  all-skills behaviour, so consumers are unchanged.

- T709 — POST-native search. `createSearchController` now drives search over
  `POST /sn/{site}/search/query` with a JSON body and the link-free `STRUCTURED`
  response, so a filter value containing `& = + # %` or a space can no longer be
  mis-read as query structure. New `toggleFacet` / `clearFacet` /
  `clearAllFilters` / `applySpellCheck` / `searchRequest` navigate from structured
  state instead of parsing an href; `state.request` is the request the controller
  mutates and re-sends. New exports: `postSearch`, `postStructuredSearch`,
  `applyFacetAction`, `resolveStructuredGroups` and the `TurSearchRequest` /
  `TurStructured*` types.

  **Behaviour change (opt-out available):** POST-native is the default, so
  `state.data` (the legacy link-bearing response) is `null` and `state.structured`
  carries the response. `state.documents` / `groups` / `status` / `error` /
  `params` are unchanged. A consumer that reads `state.data` or calls
  `navigate(href)` should pass `{ postNative: false }` to keep the previous GET
  behaviour, which is fully intact.

  No CSRF priming round-trip: `/api/sn/**` is CSRF-exempt server-side, so the
  search POST costs exactly one request, same as the GET it replaces.

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
