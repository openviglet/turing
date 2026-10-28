---
title: "JavaScript SDK (vanilla)"
description: "@viglet/turing-sdk, a zero-dependency, framework-agnostic JavaScript client for Viglet Turing. Observable controllers for search, chat, autocomplete, slots and analytics; built for Adobe EDS, plain <script>, Astro, WordPress or any bundler."
---
**`@viglet/turing-sdk`** is a **zero-dependency**, framework-agnostic JavaScript client for the Viglet Turing API. No React, no axios, just native `fetch`, `TextDecoder`, `EventSource` and `crypto.randomUUID`. It ships as a self-contained ESM bundle (`dist/turing-sdk.js`) plus a UMD build and TypeScript types.

**When you'd reach for it.** Use this SDK when you're building a search box or chat widget **outside React**: an [Adobe Edge Delivery Services (EDS)](#adobe-eds) block, a plain `<script>` tag, an Astro island, a WordPress theme, or any bundler-based app. If you're in a React app, use **[`@viglet/turing-react-sdk`](/docs/react-sdk/)** instead: it's the same backend and the same `Tur*` types, exposed as hooks. This vanilla SDK was extracted from the React SDK's framework-agnostic `core/` layer (axios → `fetch`), so the two stay in lockstep.

> **The mental model.** The SDK has three layers, from lowest to highest:
>
> 1. **The client** (`createTuringClient`): owns the base URL, credentials and CSRF priming. This is the explicit replacement for `axios.defaults`.
> 2. **API functions** (`fetchSearch`, `postChatConversation`, …): pure `(client, …args)` functions, one per REST/SSE endpoint. Use these when you want full control.
> 3. **Controllers** (`createSearchController`, `createChatController`, …): small observable state machines that wrap the API functions with a `subscribe()` / `getState()` pub-sub `Store`. **This is the layer you'll use 90% of the time**: it's the vanilla equivalent of the React hooks.

---

## Installation

```bash
npm install @viglet/turing-sdk
```

Or **vendor the bundle** directly (no build step): copy `dist/turing-sdk.js` into your project and import it as a module. This is the recommended path for EDS (see [below](#adobe-eds)).

```js
import { createTuringClient } from "https://cdn.jsdelivr.net/npm/@viglet/turing-sdk/+esm";
```

---

## Your first search (2 minutes)

Every program starts the same way: **create a client, then drive a controller.**

```js
import { createTuringClient, createSearchController } from "@viglet/turing-sdk";

// 1. Configure the client once. baseURL points at the Turing REST root (note the /api).
const client = createTuringClient({ baseURL: "http://localhost:2700/api" });

// 2. A controller is an observable state machine for one search surface.
const search = createSearchController(client, { site: "my-site", locale: "en_US" });

// 3. Subscribe: your callback fires on every state change (loading → success → …).
search.subscribe((state) => {
  if (state.status === "success") {
    for (const doc of state.documents) {
      console.log(doc.title, doc.url);
    }
  } else if (state.status === "error") {
    console.error(state.error);
  }
});

// 4. Trigger work. The controller updates its state; your subscriber repaints.
await search.searchQuery("artificial intelligence");
```

That's the whole pattern. `subscribe()` returns an **unsubscribe** function: call it when your component/block tears down.

---

## The client

`createTuringClient(config)` is the single source of connection truth. Every API function and controller takes the client as its first argument.

```ts
interface TuringClientConfig {
  baseURL: string;                     // REQUIRED, e.g. "http://localhost:2700/api"
  credentials?: RequestCredentials;    // default "include" (sends cookies / CSRF cross-origin)
  headers?: Record<string, string>;    // merged into every request
  apiKey?: string;                     // a Turing Developer Token → sent as the "Key" header
  getCsrfToken?: () => string | undefined | Promise<string | undefined>; // override CSRF resolution
}
```

The returned client exposes low-level helpers (`get`, `post`, `del`, `request`, `fetchRaw`, `ensureCsrfToken`) if you ever need to call an endpoint the SDK doesn't wrap yet.

### CSRF & cross-origin

The Turing backend uses Spring Security's `CookieCsrfTokenRepository`, which writes the `XSRF-TOKEN` cookie **HttpOnly** and surfaces the token in the `X-XSRF-TOKEN` response header. JavaScript can't read an HttpOnly cookie, so before any mutating request (`POST`/`PUT`/`DELETE`) the client **primes** the token with a best-effort `GET {baseURL}/csrf` and attaches it as `X-XSRF-TOKEN`.

- **Anonymous / public deployments** with CSRF disabled work **tokenless**: the priming call fails softly and requests go through.
- **Different origins?** Enable CORS with credentials on the backend (`Access-Control-Allow-Credentials: true` + an explicit allowed origin). `credentials: "include"` (the default) sends the cookie.

---

## Controllers

Each controller wraps a tiny pub-sub `Store` with `getState()`, `setState()` and `subscribe(listener)`. `createChatController` and `createSlotsController` (and `createSimilarController`) additionally expose **`destroy()`** to abort in-flight requests, timers and SSE subscriptions: always call it on teardown.

| Controller | Factory | What it does |
|---|---|---|
| **Search** | `createSearchController(client, config, initialParams?, options?)` | Full search surface: query, facets, pagination, sort, locale, click tracking. |
| **Chat** | `createChatController(client, options?)` | Streaming RAG/agent chat: token-by-token replies, suggestion chips, native forms, sources, citations, client tools. |
| **Autocomplete** | `createAutoComplete(client, config, debounceMs?)` | Debounced type-ahead suggestions (default 300 ms). |
| **Slots** | `createSlotsController(client, scope, options?)` | Live view of conversation slots (captured data) via polling or SSE. |
| **Similar** | `createSimilarController(client, site, options?)` | "Related / you may also like": similar documents for a seed id. `fq: ["type:post"]` keeps only the neighbours that match, with the search's filter semantics. |

### Search controller

```js
const search = createSearchController(
  client,
  { site: "my-site", locale: "en_US" },   // TuringConfig: { site, locale?, sort? }
  { rows: 20 },                            // optional initial params
  { analytics }                            // optional: see Analytics below
);

await search.searchQuery("laptops");       // run a query
await search.searchQuery("lap", { mode: "instant" }); // as the visitor types: "lap" finds laptops
await search.toggleFacet(facetItem);       // click a facet value (applies its own action)
await search.toggleFacet(doc.metadata[0]); // ...or a result's own facet chip
await search.clearFacet(facet);            // clear one facet
await search.clearAllFilters();            // clear every filter
await search.goToPage(2);
await search.applySpellCheck();            // accept "did you mean"
await search.changeSort("relevance");
await search.changeLocale("pt_BR");
search.trackResultClick(doc.id, position); // CTR signal
```

State: `{ status, structured, data, chat, documents, groups, error, params, request }`. `documents` and `groups` are pre-resolved from the raw response so you can render them directly.

Each call cancels the request it supersedes. When a visitor types on, the previous search is aborted rather than left to finish, and only the newest answer reaches the state.

A result click usually starts a navigation. `trackResultClick` and `postClick` send the click as a `keepalive` request with no CSRF prime, so the page can unload without cancelling it. `POST /api/sn/{site}/click` also accepts a JSON body sent as `text/plain`, so you can send a click with `navigator.sendBeacon` yourself:

```js
navigator.sendBeacon(
  "https://turing.example.com/api/sn/Sample/click",
  JSON.stringify({ term: "laptops", documentId: doc.id, position: 2 })
);
```

#### POST-native by default

The controller talks to the [POST-native search endpoint](/docs/rest-api/#post-native-search)
and asks for the **link-free** response, so `state.structured` is authoritative:
facets, pagination, locales and spell-check arrive as data, and every navigation
above is a change to `state.request` rather than a URL to parse. That is what
makes a filter value containing `&`, `=`, `+`, `#`, `%` or a space safe: there is
no query string for it to break.

Render facets straight from that state:

```js
search.subscribe((state) => {
  for (const facet of state.structured?.facets ?? []) {
    for (const item of facet.items) {
      // item.selected drives the checkbox; the click just hands the item back.
      renderChip(item.label, item.count, item.selected, () => search.toggleFacet(item));
    }
  }
  for (const page of state.structured?.pagination ?? []) {
    renderPage(page.text, page.current, () => search.goToPage(page.page));
  }
});
```

A result's own facet chips (`document.metadata[]`) carry the same three fields the
action needs (`field`, `filterQuery` and `action`) plus `selected`, so a chip can
render as already-applied and be toggled *off* rather than only added. Hand it
straight to the same call:

```js
for (const chip of doc.metadata ?? []) {
  renderChip(chip.text, chip.selected, () => search.toggleFacet(chip));
}
```

`state.params` is still kept in sync (handy for building a shareable GET URL), and
`state.data` (the legacy link-bearing body) is `null` in this mode. Pass
`{ postNative: false }` as a controller option to go back to the GET contract and
`state.data`.

In that mode, render the response's hrefs as **real anchors**: that is what they
are good for (deep-linking, SEO, no-JS). `navigate(href)` and `parseHrefToParams`
still work, but they are deprecated, log a one-time console notice, and are
**scheduled for removal in the next major**: parsing a server URL back into
params is the round-trip that made reserved characters fragile in the first
place. The server-rendered links themselves are not going anywhere.

<a id="tosearchview"></a>

#### `toSearchView`: reading either shape

Supporting both transports means every component has to ask which one arrived.
`toSearchView(store)` answers it once, for the parts of the response the facet and
pagination examples above do not cover, and it resolves the **actions** with the
state, so the component that renders an applied-filter chip never learns whether
removing it subtracts a filter query or follows an href:

```js
import { toSearchView } from "@viglet/turing-sdk";

search.subscribe((state) => {
  const view = toSearchView({ ...state, ...search });
  if (!view.ready) return;

  renderBrand(view.siteIcon);            // the site's Iconify name, or null
  renderLocales(view.locales);           // [{ locale }]
  view.appliedFilters.forEach((filter) =>
    renderAppliedChip(filter.label, filter.remove),   // one call, either shape
  );
  if (view.spellCheck?.correctedText) {
    offerCorrection(view.spellCheck.correctedTextValue, view.spellCheck.applyCorrection);
  }
  // `facetItemType: OR`: draw checkboxes rather than radio buttons.
  setMultiSelect(view.multiSelectFacetItems);
});
```

`view.source` tells you which shape backed the projection (`"structured"`,
`"legacy"` or `"none"` before the first response), and `view.ready` is the render
gate: gating on `state.data` alone renders nothing in the default mode.

The React SDK re-exports the same function, and `useSearchView()` wraps it.

### Chat controller

The chat controller streams the assistant's reply **token by token**, every token pushes a new state, so your subscriber repaints a growing bubble. It works in two modes:

- **Site mode** (default): pass `site`. Talks to the Semantic Navigation chat (`/sn/{site}/chat/conversation`), including any [chat flow](/docs/chat-flow/) on that site.
- **Agent mode**: pass `agent: { id, llmInstanceId, allowOverride? }`. Talks to a specific [AI Agent](/docs/ai-agents/) (`/v2/ai-agent/{id}/chat`).

```js
const chat = createChatController(client, {
  site: "my-site",
  persist: true,          // save the transcript to sessionStorage
  locale: "en_US",
  goalSlots: ["email"],   // writing this slot counts as a lead conversion (analytics)
});

chat.subscribe((s) => {
  renderBubbles(s.messages);           // re-renders on every token
  if (s.isStreaming) showTypingDots();
  if (s.activeForm) renderForm(s.activeForm); // a native multi-field form is pending
});

await chat.send("How do I reset my password?");
// If a chat flow raised a form:
await chat.submitForm({ name: "Ada", email: "ada@example.com" });

chat.stop();     // abort the in-flight turn
chat.reset();    // clear messages + error (client-side)
await chat.resetFlow(); // drop the server-side flow cursor
chat.destroy();  // release everything on teardown
```

A `ChatMessage` carries far more than text, as the stream progresses it accumulates `options` (suggestion chips), `form`, `sources` (RAG provenance), `citations` (per-sentence grounds), `toolCalls` (live tool activity), `grounding` (answer-grounding verdict) and `secondOpinion` (cross-vendor critic). Render whichever your UI cares about.

#### Scoping the chat to the current search

An assistant next to a results page can answer within the visitor's current search. Pass `filters` (field → values, the same shape [`postNlFilters`](/docs/rest-api/#natural-language-filters) returns) at creation, and replace them for one turn with `send`'s second argument. Pass `onFilters` to receive the filters the server read from a list-style question ("quais vagas em São Paulo?"), so the page can offer "see these results":

```js
const chat = createChatController(client, {
  site: "jobs",
  filters: { uf: ["SP"] },                       // every turn, until replaced
  onFilters: (parsed) => showSeeResults(parsed.fq), // the question's own filters
});

await chat.send("Which of these are remote?");
await chat.send("And in Rio?", { filters: { uf: ["RJ"] } }); // this turn only
```

`postChatConversation` takes the same `filters` and `onFilters` options.

#### Frontend "client tools" (generative UI)

An agent can call a tool that runs **in the browser** (open a dialog, call your own API, mutate the page) and the SDK feeds the result back so the turn resumes. Register handlers up front or imperatively:

```js
const chat = createChatController(client, {
  site: "my-site",
  clientTools: {
    // shorthand: just a handler
    log_to_crm: (args) => { crm.track(args); return { ok: true }; },
    // full form: handler + optional JSON Schema advertised to the agent
    send_email: {
      handler: async ({ to, subject, body }) => {
        const id = await mailer.send(to, subject, body);
        return { success: true, messageId: id };
      },
      schema: { type: "object", properties: { to: { type: "string" } } },
    },
  },
});

// or later:
const off = chat.registerClientTool("open_dialog", (args) => {
  openDialog(args); return { result: "opened" };
});
```

When the agent invokes a client tool, the SDK calls your handler with the parsed args, `POST`s the result to `/v2/chat/client-tool-result`, and resumes the SSE stream. See **[Client tools & agent↔UI transparency](/docs/client-tools/)**.

### Autocomplete & slots

```js
const ac = createAutoComplete(client, { site: "my-site" }, 250); // 250ms debounce
ac.subscribe((s) => renderSuggestions(s.suggestions));
input.addEventListener("input", (e) => ac.fetch(e.target.value));
```

```js
const slots = createSlotsController(
  client,
  { site: "my-site" },                 // or { agentId }
  { transport: "sse", sseDelta: true } // live updates; sseDelta = smaller delta stream (2026.3.1+)
);
slots.subscribe((s) => {
  if (s.slots.email) prefill("email", s.slots.email);
});
```

---

## Low-level API functions

When a controller doesn't fit, call the endpoint directly. Every function takes the `client` first. The streaming chat function is the most important one, it exposes a callback per SSE event type:

```js
import { createTuringClient, postChatConversation } from "@viglet/turing-sdk";

const client = createTuringClient({ baseURL: "http://localhost:2700/api" });

await postChatConversation(
  client,
  "my-site",
  [{ role: "user", content: "What is AI?" }],
  "en_US",
  {
    onToken:        (t)     => process.stdout.write(t),         // each text token
    onOptions:      (opts)  => showChips(opts),                 // suggestion chips
    onForm:         (form)  => renderForm(form),                // native form (T107)
    onSources:      (srcs)  => showSources(srcs),               // RAG provenance
    onCitations:    (cites) => underline(cites),                // per-sentence citations
    onToolCall:     (c, all) => showToolActivity(all),          // live tool lifecycle
    onGrounding:    (g)     => badge(g.grounded ? "ok" : "flagged"),
    onSecondOpinion:(o)     => badge(o.agree ? "agree" : "disagree"),
    onReasoning:    (r)     => showWhy(r),                       // reasoning summary
    conversationId: "session-123",
  }
);
```

To turn a visitor's description into your own search, `postNlFilters` returns the `q` and `fq` the search takes ([Natural-language filters](/docs/rest-api/#natural-language-filters)):

```js
import { postNlFilters, postSearch } from "@viglet/turing-sdk";

const parsed = await postNlFilters(client, "jobs", "meio periodo em sao paulo", "pt_BR");
const results = await postSearch(client, "jobs", { q: parsed.q || "*", fq: [...parsed.fq], locale: "pt_BR" });
```

Other functions include `fetchSearch`, `postClick`, `fetchAutoComplete`, `fetchSpellCheck`, `fetchSimilar` (with `fq` to filter the neighbours: `GET /api/sn/{site}/search/similar?id=&fq[]=type:post`, applied inside the similarity query so `rows` counts what passes, and answered by the lexical query when filtered), `postSiteSlotUpload` (multimodal), `postSiteChatResume`, `dslSearch` / `dslQuery` ([DSL Query](/docs/dsl-query/)), `postSemanticChat`, `postAgentChat`, and discovery helpers (`fetchDiscovery`, `fetchFeatures`, `fetchSystemLocales`, `fetchLlmVendors`). `createTuringApi(client)` returns an object with all of them pre-bound to the client.

---

## Analytics & the Google Analytics bridge

Turing measures the funnel **server-side** ([Chat Analytics](/docs/chat-analytics/), [Experiments](/docs/experiments/)). This SDK adds the **browser-owned** half (conversion, abandonment and "which persona A/B variant won") delivered into **your own GA4**, next to acquisition source and audiences. It's a zero-dependency canonical event bus with **pluggable sinks**; GA4 is the first adapter, not a dependency.

```js
import {
  createTuringClient, createTuringAnalytics, googleAnalyticsSink,
  createSearchController, createChatController,
} from "@viglet/turing-sdk";

const analytics = createTuringAnalytics({
  sinks: [googleAnalyticsSink()],       // auto-detects window.gtag / dataLayer
  context: { site: "my-site" },
});

const client = createTuringClient({ baseURL: "/api" });
createSearchController(client, { site: "my-site" }, undefined, { analytics });
createChatController(client, { site: "my-site", analytics, goalSlots: ["email"] });
```

The bus is **no-op when no `analytics` option is passed**, so existing embeds are byte-for-byte unchanged. Search → chat → lead is stitched into one funnel via the shared `TUR_SESSION` cookie. Full event taxonomy and the GA4 setup live in **[Conversion Analytics](/docs/conversion-analytics/)**. In React, use `useTuringAnalytics()`.

#### Consent

The search controller writes nothing when it is created. It asks `consent` before every event and before it writes the `TUR_SESSION` cookie, so you can pass the bus before the visitor has answered your consent banner:

```js
createSearchController(client, { site: "my-site" }, undefined, {
  analytics,
  consent: () => hasStatisticsConsent(), // e.g. the WP Consent API
  sessionCookie: true,                   // false: never write TUR_SESSION
});
```

While `consent` returns `false`, no event is emitted and no cookie is set. The session id is stamped on the first event after it returns `true`. With `sessionCookie: false` the controller writes no cookie, and search events are not stitched to a later chat lead. A search sent with `mode: "instant"` (as the visitor types) emits no search event; only the one the visitor submits does.

---

<a id="adobe-eds"></a>

## Adobe EDS

The SDK was designed for [Adobe Edge Delivery Services](/docs/integration/) blocks. Vendor `dist/turing-sdk.js` into your EDS project's `scripts/` folder and import it from a block:

```js
// blocks/turing-search/turing-search.js
import { createTuringClient, createSearchController } from "../../scripts/turing-sdk.js";

export default function decorate(block) {
  // Authors configure the block with a two-cell row: | API base URL | site |
  const [baseURL, site] = [...block.querySelectorAll("div > div")].map((c) => c.textContent.trim());
  const client = createTuringClient({ baseURL });
  const search = createSearchController(client, { site });
  search.subscribe((state) => repaint(block, state));
  // …wire an input to search.searchQuery(value)
}
```

The `examples/eds/` folder in the repository ships ready-to-copy `turing-search`, `turing-chat` and `turing-analytics` blocks. The `turing-search` block is the reference POST-native surface: it renders facets and pagination from `state.structured` and navigates with `toggleFacet` / `goToPage`, so no href is ever parsed.

---

## Related pages

- **[React SDK](/docs/react-sdk/)**: the hook-based equivalent for React apps.
- **[Turing CLI](/docs/cli/)**: scaffold, deploy and eval agent projects from the terminal.
- **[Flow DSL](/docs/flow-dsl/)**: author chat flows as typed TypeScript.
- **[Client tools & agent↔UI](/docs/client-tools/)**: the frontend-tool protocol used by `registerClientTool`.
- **[Conversion Analytics](/docs/conversion-analytics/)**: the GA4 bridge and event taxonomy.
- **[REST API](/docs/rest-api/)**: the endpoints these functions wrap.

