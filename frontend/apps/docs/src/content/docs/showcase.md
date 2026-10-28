---
title: "Reference showcase (Atlas Store)"
description: "Atlas Store: the installable reference app that exercises the maximum surface of Viglet Turing, walked end-to-end (typed catalog, faceted + hybrid search, RAG chat, agent power, creative compositions, voice/memory/ops)."
---
**Atlas Store** is the canonical reference application for Viglet Turing, the "see it all wired together" artifact. It's a commerce-catalog copilot whose single job is to **exercise the maximum surface of the platform**: a living demo, an SDK-vs-real-backend integration test, and the answer to *"what can Turing actually do?"*

If you've read the feature pages one at a time, this is where they connect. Atlas Store is the through-line: a typed catalog feeds faceted + hybrid **search**, the same index grounds a RAG **chat**, the chat gains **agent power** (client tools, code interpreter, skills), and a handful of **creative compositions** only possible because Turing owns search *and* agents. This page walks that surface in the order a visitor experiences it, linking the deep page for each capability, making it a good map for the [learning path](/docs/getting-started/intro/).

It lives in the monorepo at [`frontend/apps/showcase`](https://github.com/openviglet/turing/tree/main/frontend/apps/showcase) and **seeds its own data**: a deterministic ~200-product typed catalog (price as `CURRENCY`, rating as `FLOAT`, reviews as `INT`, on-sale as `BOOL`, release date as `DATE`, multi-valued color/material/tags) that stresses the [manifest](/docs/manifest/), hybrid ranking, and facets.

## Run it

```bash
# From the monorepo root
pnpm install
pnpm dev:showcase        # vite dev server (proxy VITE_API_URL → a running Turing)

# Package as an import bundle
pnpm build:showcase
cd frontend/apps/showcase && npm run zip   # → atlas-store.zip
```

Import `atlas-store.zip` from the Turing admin (Marketplace / Pages import) to provision the SN site, index the catalog, and serve the SPA as a search template.

:::note[Two fidelities]
The offline **Lucene seed** is search-only (no embeddings): chat and agent surfaces show honest *"configure GenAI"* notices rather than breaking. Import the bundle into a backend with an **embedding LLM + AI agent** configured and the chat, hybrid ranking, and agent features light up. Every agent-side feature is gated server-side; the client wiring is always present.
:::

---

## The walkthrough

### 1. Search: the typed catalog

The storefront is a faceted search built on the [`@viglet/turing-react-sdk`](/docs/react-sdk/) search hooks: URL-synced facets, autocomplete, a "did you mean?" spell-check banner, category tabs, a sort dropdown wired to the site's custom sorts, a "you may also like" similar rail, and click-tracking on every result. Because the catalog is **typed** (a [field manifest](/docs/manifest/)), `CURRENCY` sorts as money, `DATE` filters as a year range, and multi-valued `tags` facet with OR semantics. With an embedding model configured, [hybrid RRF ranking + a reranker](/docs/reranking/) are a server-side site-config toggle: the UI is rank-mode agnostic. → [Semantic Navigation](/docs/semantic-navigation/), [Search Engine](/docs/search-engine/)

### 2. Ask Atlas: the RAG chat

A floating "Ask Atlas" copilot exercises the [RAG chat](/docs/rag/) half via `useTuringChat`: token **streaming**, **cited sources** as chips, **option chips** for the next turn, **native multi-field forms** (text/email/date/select widgets), a **slots** "Collected" strip, **multimodal image upload** with vision, a **workspace panel** of agent artifacts, and **rich content** dispatch: markdown, sandboxed `html`, and `d2` diagrams (the diagram engine lazy-loaded so the main bundle stays small). → [Chat](/docs/chat/), [RAG](/docs/rag/)

### 3. Agent power

The copilot is a real [AI Agent](/docs/ai-agents/), so it can *act*, not just answer:

- **Client tools**: `add_to_cart` / `get_cart` / `clear_cart` run against a real cart context shared with the header drawer, so the agent acts on the shopper's live basket. → [Client Tools](/docs/client-tools/)
- **Code interpreter**: "total my cart" / "chart price history" runs Python over real data. → [Tool Calling](/docs/tool-calling/#code-interpreter-1-tool)
- **Live tool-call activity**: each tool invocation streams inline as it runs. → [Client Tools → Live tool activity](/docs/client-tools/#live-tool-activity-read-only)
- **A bundled returns/RMA skill**: a self-contained Anthropic skill folder (`SKILL.md` + a reference doc + a pure-stdlib `rma.py`) you drop into the backend `skillsPath`. → [Skills](/docs/skills/)

### 4. Creative compositions

These pay off precisely because Turing owns **both** search and agents. They are **shipped, opt-in features (default off)**, each enabled per agent, demonstrated here wired together:

| Composition | What it does |
|---|---|
| **Answer-as-an-app** | The agent renders a live `comparison_table` / `spec_card` / `configurator` from a real typed search, via [generative UI](/docs/client-tools/#generative-ui) |
| **Co-browse** | The agent drives the actual storefront the shopper sees (set query, toggle facets, sort, page) |
| **Glass-box "Why #1?"** | The agent explains the top result's ranking with the real signals (BM25 / vector / RRF / rerank), via the `explain_ranking` tool |
| **Ambient proactive copilot** | A toast offers help when an interaction signal crosses the agent's threshold (opt-in, throttled) |
| **Agent→agent handoff** | A router agent delegates to a specialist via `delegate_to_agent`, visible for free in the tool-call activity |
| **Skills that ship UI** | A skill folder can embed its own components, surfaced through the same generative-UI tool path |
| **Embeddable action widget** | A mock third-party page embeds the agent via the zero-dep vanilla [`@viglet/turing-sdk`](/docs/react-sdk/), so the agent acts on *that* site's cart, Turing as an action layer over any website |

:::note[Opt-in and default-off]
Each composition is gated by its own per-agent flag (`answerAsAppEnabled`, `coBrowseEnabled`, `proactiveEnabled`, `agentHandoffEnabled`, `userMemoryEnabled`, `actionWidgetEnabled`, …). They are real, tested capabilities, but **off by default**, so a stock deployment never changes until you opt one in. The showcase turns them on to demonstrate the full surface.
:::

### 5. Voice, memory & ops

- **Voice**: a mic button dictates the question (Web Speech STT) and auto-sends on a final utterance.
- **Search by image**: a hero affordance hands off to the copilot's multimodal vision upload.
- **Cross-conversation memory**: a "brain" panel shows what Atlas remembers about the visitor (keyed on a stable visitor id), with a "Forget me" GDPR delete. → [Chat Memory](/docs/chat-memory/)
- **Ops tour** (`/ops`): a guided map of [Chat Analytics](/docs/chat-analytics/) (sentiment trajectory, tool-latency p95, SSE channels), [agent eval](/docs/agent-eval/) + golden sets, self-tuning suggestions, and [cost governance](/docs/cost-governance/).
- **MCP exposure**: `mcp/claude-desktop.json` bridges Claude Desktop to Turing's [MCP server](/docs/mcp-servers/), so the Atlas catalog is drivable from Claude Desktop.

An in-app **Guided Tour** narrates each capability with "try it" deep-links into the copilot.

---

## What it demonstrates (at a glance)

| Surface | Capabilities |
|---|---|
| **Search** | faceted + URL-synced search, autocomplete, spell-check, category tabs, sort, similar/"more like this", click tracking; hybrid RRF + reranker as a server toggle |
| **RAG chat** | streaming, cited sources, option chips, native forms, slots, multimodal image upload, workspace panel, rich markdown/`html`/`d2` |
| **Agent power** | cart **client tools**, code interpreter, live tool-call activity, answer-as-app widgets, a bundled returns/RMA **skill** |
| **Creative compositions** | co-browse, glass-box "Why #1?", proactive copilot, agent→agent handoff, skill UIs, an embeddable action widget |
| **Voice / memory / ops** | voice dictation, search by image, cross-conversation memory (GDPR delete), an `/ops` tour, MCP exposure for Claude Desktop |

A full feature-by-feature reconciliation against the design matrix (✅ live / 🔌 backend-gated / 📄 documented) lives in [`COVERAGE.md`](https://github.com/openviglet/turing/blob/main/frontend/apps/showcase/COVERAGE.md).

## Architecture notes

- Built with **`@viglet/turing-react-sdk`** (search/chat hooks), **`@viglet/turing-react-ui`** (headless components, token-themed to the Atlas brand), and the zero-dep vanilla **`@viglet/turing-sdk`** (the embeddable action widget).
- Agent-side features are **gated server-side**; the offline Lucene seed is search-only and the UI shows honest "configure GenAI" notices, so the demo never looks broken.
- An end-to-end smoke test ([`TurShowcaseSmokeIT`](https://github.com/openviglet/turing/blob/main/turing-app/src/test/java/com/viglet/turing/showcase/TurShowcaseSmokeIT.java)) boots the backend, imports the showcase ZIP into an embedded Lucene index, and asserts the REST contract the storefront depends on.

---

## Related Pages

| Page | Description |
|---|---|
| [Getting Started](/docs/getting-started/intro/) | The zero → first-agent learning path this showcase makes concrete |
| [Semantic Navigation](/docs/semantic-navigation/) | The faceted search powering the storefront |
| [RAG](/docs/rag/) | The retrieval that grounds "Ask Atlas" |
| [AI Agents](/docs/ai-agents/) | The agent behind the copilot |
| [Client Tools](/docs/client-tools/) | Cart tools, generative UI, the embeddable widget |
| [React SDK](/docs/react-sdk/) | The hooks and components Atlas Store is built on |

