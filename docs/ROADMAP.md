# Turing — Roadmap (active backlog)

> **Written by [roadkeep](../roadkeep.toml), never by hand.** Open work only, one line per
> task; the `→ §` pointer is that task's design in [IMPROVEMENTS.md](IMPROVEMENTS.md).
> Shipped work lives in [CHANGELOG.md](CHANGELOG.md) and positioning in
> [STRATEGY.md](STRATEGY.md); the markers and limits are `roadkeep.toml`'s. Blocks are
> reused, not minted — `roadkeep brief` picks the next task and states why.

## Block CD — turing.viglet.org relevance pass (2026-10)

## Block BZ — Indexing API trust for connectors: key auth, scripted login, request validation

- 📋 **T1112** (deps: T1133) **An import DELETE removes by id or type only, so a connector sharing an SN site cannot delete its documents by a field** — Each connector lists its ids through search and deletes them one by one, and an id of * is accepted and deletes nothing. → §CXXIV
- 📋 **T1133** (deps: —) **An API key reads and deletes every document of a shared SN site, as nothing binds a key to its own partition** — Each endpoint needs its own fq to keep shared sites apart, and any endpoint without one leaks another site's documents. → §CXLV

## Block BW — What the link-free response and its consumers still lack (§LXX)

- ⏳ **T1127** (deps: —) **An SN site's chat calls the LLM but the cost ledger records nothing: cost summary stays at zero** — The ledger has no site column, so cost/summary cannot filter by site, and the catalog copilot still records nothing. → §CXXXIX
- 📋 **T1128** (deps: —) **The SDK's result-click call can be cut off by the navigation the click starts, and /click refuses a beacon** — Clicks are lost silently, more often on slow networks, so click-through rates undercount. → §CXL
- 📋 **T1129** (deps: —) **The search controller's analytics option writes a 30-day cookie before consent and reports every as-you-type search** — A consent-respecting site with instant search cannot use the SDK's search analytics and has to re-emit the events itself. → §CXLI
- 📋 **T1130** (deps: —) **The GA sink sends only turing_* event names, and the taxonomy has no search abandonment event** — GA4's site-search reports stay empty without hand-made custom definitions, and searched-and-left is never measured. → §CXLII
- 📋 **T1131** (deps: —) **Search-result clicks are stored but nothing reads them, and /click answers 202 with metrics off** — No site can see a click-through rate, and a client cannot tell that its clicks are being dropped. → §CXLIII
- 📋 **T1132** (deps: T1133) **The similar-documents search takes no filter, so a shared SN site answers with every site's documents** — A related-content rail must over-fetch and discard, and can come up short on a busy shared site. → §CXLIV

## Block CF — What the SN site manifest cannot declare

- 📋 **T1135** (deps: —) **The SN manifest cannot set a site's facet behaviour, so provisioning as code cannot choose drill-down or multi-select** — facetType, facetItemType and itemsPerFacet are console-only, so a connector must click through the console to get facets that stay visible. → §CXLVII

## Done when — T1127

- **cost/summary answers one site's spend, copilot included** Each SN call records its
  site, GET /api/v2/llm/cost/summary?site=<name> returns only that site's rows, and a
  catalog copilot answer appears in them.

## Non-goals (do NOT add as tasks)

Binding constraints — see [IMPROVEMENTS.md](IMPROVEMENTS.md) §I.6 (What NOT to
refactor) and §I.7 (Restrictions) for the full text. Summary:

- Don't refactor the auto-trigger router, continuation logic, A/B assignment,
  sub-flow descent/ascent, SSE bus, defense-in-depth checks, silent telemetry,
  anti-injection / CPF-CNPJ / yes-no normalization, `TurToolCallbackPipeline.decorate`,
  or `summarizeVariables` cap — these are product, not patches.
- Structured output (LLM → JSON) is **not** a path — use provider-side tool-calling schemas.
- Don't rewrite the chat-flow DSL (`ChatFlowGraph` / `ChatFlowNode` shape) — only execution.
- Keep Spring AI `ChatModel.call()` + `internalToolExecutionEnabled(true)` (no framework swap).
- Heuristic and LlmJudge strategies coexist.
- No "auto-detect" middleware guessing node intent (toolFree, captureMode) — that's authorship.
- No in-process ONNX/DJL Whisper transcription runtime (the retired `LOCAL_ONNX` backend, ex-T694). The supported fully-local path is `OPENAI_COMPATIBLE` pointed at a self-hosted server (e.g. faster-whisper-server) — the platform won't bundle a fragile in-process Whisper dependency (T193 Jlama discipline).
