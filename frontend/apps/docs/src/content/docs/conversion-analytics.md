---
title: "Conversion Analytics (GA4 bridge)"
description: "Measure lead conversion, abandonment, and which persona A/B variation was preferred, in the browser, in your own Google Analytics. A zero-dependency canonical event bus in the Turing SDK with pluggable sinks (GA4/GTM first), auto-detecting an existing gtag/dataLayer for zero-config Adobe EDS setups."
---
> *Turing already measures the funnel server-side (outcome, experiment, persona). This is the other half: the **browser-owned** view, emitted at the lifecycle moments only the client can see (like a tab close) and delivered straight into your own GA4 next to acquisition source, campaign and audiences.*

Turing's [Chat Analytics](/docs/chat-analytics/) and [Experiments](/docs/experiments/) record conversion, abandonment and A/B outcomes **server-side**. But marketing teams run **GA4 in the browser**, where conversion lives next to media spend and audiences. Block Z adds a **client-side, marketing-owned** measurement layer that answers three concrete questions *in your GA*, not ours:

1. **Lead conversion** in chat & semantic navigation.
2. **Drop-off / abandonment**: and *where* the visitor gave up.
3. **Which persona A/B variation was preferred.**

## The architecture: a canonical bus + pluggable sinks

Google Analytics is **not** hard-coded into the SDK: that would break the zero-dependency, framework-agnostic, [EDS-friendly](#adobe-eds-zero-config) contract. Instead:

- a **canonical, vendor-neutral event bus** (`createTuringAnalytics`) that the chat / search controllers feed, plus
- **pluggable sinks**: GA4 is the *first adapter*, not a dependency.

The bus is **no-op when no `analytics` option is passed**, so every existing embed is byte-for-byte unchanged.

## Quick start (vanilla SDK)

```js
import {
  createTuringClient, createTuringAnalytics, googleAnalyticsSink,
  createSearchController, createChatController,
} from "@viglet/turing-sdk";

const analytics = createTuringAnalytics({
  sinks: [googleAnalyticsSink()],   // auto-detects window.gtag / dataLayer
  context: { site: "my-site" },
});

const client = createTuringClient({ baseURL: "/api" });

// One bus, shared by both surfaces → a single search → chat → lead funnel.
createSearchController(client, { site: "my-site" }, undefined, { analytics });
createChatController(client, { site: "my-site", analytics, goalSlots: ["email", "phone"] });
```

## React

```tsx
import { useTuringAnalytics, useTuringChat, useTuringSearch } from "@viglet/turing-react-sdk";

function Storefront() {
  const { analytics } = useTuringAnalytics();          // GA4 sink auto-attached
  const chat = useTuringChat({ site: "store", analytics, goalSlots: ["email"] });
  const search = useTuringSearch(undefined, { analytics });
  // search.trackResultClick(documentId, position) → server CTR + GA event
  // ...
}
```

`useTuringAnalytics()` seeds the `site` from your `<TuringProvider>` automatically and returns a **memoized** bus (stable across renders).

`useTuringSearch` takes the same options as the vanilla search controller: `consent`, `sessionCookie` and `abandonment`. It writes no `TUR_SESSION` cookie on mount, and `searchQuery(q, { mode: "instant" })` emits no search event:

```tsx
const search = useTuringSearch(undefined, {
  analytics,
  consent: () => hasStatisticsConsent(),
  abandonment: true,
});
```

## The canonical event taxonomy

Vendor-neutral names: sinks map them verbatim, so a funnel built on `turing_*` is portable across GA4, Segment, Matomo, …

| Event | Fired when | Key params |
|-------|-----------|-----------|
| `turing_chat_start` | conversation begins (first user message) | `mode` |
| `turing_chat_message_sent` | every user turn | `turn`, `length` |
| `turing_chat_step` | a flow node is reached (client mirror of the server funnel) | `node_id`, `flow_name`, `step` |
| `turing_chat_lead_captured` | a goal slot is written or a native form is submitted | `reason`, `goal_slot` |
| `turing_chat_abandoned` | a started, unconverted conversation ends (tab hide / unload / idle) | `reason`, `turns`, `last_step`, `elapsed_ms` |
| `turing_ab_variant_assigned` | the A/B arm resolves | stamps `experiment_key` / `variant_label` / `persona_id` |
| `turing_search` | a query runs | `query`, `results` |
| `turing_search_no_results` | a query returns 0 hits (content-gap signal) | `query` |
| `turing_search_result_click` | a result is clicked | `query`, `document_id`, `position` |
| `turing_search_refined` | a query is reformulated | `from`, `to` |
| `turing_search_abandoned` | a settled search is left without a result click (tab hide / unload / idle); opt-in | `query`, `results`, `reason` |

Once `turing_ab_variant_assigned` fires, `experiment_key` / `variant_label` / `persona_id` are stamped on **every subsequent event**, so a GA4 comparison or audience answers *"which persona variation converted"* natively.

## Abandonment: the signal the server can't see

The server only *infers* abandonment from a session TTL. The browser actually sees the visitor leave. A started conversation (≥1 user message) with no conversion emits a single `turing_chat_abandoned` on tab hide / page unload / idle timeout, carrying the last step reached and the turn count. On unload the GA4 sink flags the event `transport_type: 'beacon'` so `navigator.sendBeacon` flushes it before teardown.

```js
createChatController(client, {
  site: "my-site", analytics,
  abandonment: { idleMs: 60_000, onHidden: true, onUnload: true }, // or `false` to disable
});
```

Search has the same signal: a visitor who searched, saw results, opened none and left. It is off by default in the search controller. Turn it on with `abandonment`. Each settled search arms the watcher, `trackResultClick` disarms it, and as-you-type searches (`mode: "instant"`) never arm it:

```js
createSearchController(client, { site: "my-site" }, undefined, {
  analytics,
  abandonment: true, // or { idleMs: 120_000, onHidden: false }
});
```

## Cross-surface stitching (search → chat → lead)

Both the search and chat controllers key off the same `TUR_SESSION` cookie, and the bus stamps it as the session id. A chat lead therefore attributes back to the **originating search query**: GA4 shows one journey, not two disjoint funnels. The search controller mints that cookie only after the visitor consents: see [javascript-sdk § Consent](/docs/javascript-sdk/#consent).

## Sinks

- **`googleAnalyticsSink({ measurementId?, transport?, recommended? })`**: maps each event to GA4 `gtag('event', name, params)` **and/or** a GTM `dataLayer.push({ event, ... })`. Identity fields are snake_cased to GA convention; the reserved `session_id` is namespaced to `turing_session_id`. With `recommended: true` it also sends GA4's recommended events, so the built-in site-search reports fill without custom definitions: `search { search_term }` next to `turing_search`, and `select_content { content_type: "search_result", content_id, index }` next to `turing_search_result_click`.
- **`onEventSink(fn)`**: generic escape hatch for Segment / Matomo / Plausible or any custom pipeline.
- **`debugSink()`**: console tracing during integration.

### Adobe EDS zero-config

`googleAnalyticsSink()` **auto-detects** an existing `window.gtag` / `window.dataLayer` **lazily at emit time**. On a GTM-tagged or Adobe Edge Delivery Services site that already loads analytics, the bridge lights up with **no measurement id and no extra config**. See the [`turing-analytics` EDS example block](https://github.com/openviglet/turing/tree/main/frontend/packages/sdk/examples/eds).

## Non-goal

Turing does **not** ship its own conversion dashboards here: GA4 (and any sink) owns visualization; the SDK only emits clean, canonical events.

---

## Related Pages

| Page | Description |
|---|---|
| [Chat Analytics](/docs/chat-analytics/) | The server-side funnel (outcome, sentiment, tools) |
| [Experiments](/docs/experiments/) | A/B outcomes recorded server-side |
| [React SDK](/docs/react-sdk/) | Where the client-side events are emitted |
| [Token Usage](/docs/token-usage/) · [Cost Governance](/docs/cost-governance/) | The cost side of the funnel |

