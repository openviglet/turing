# Turing — Roadmap (active backlog)

> **Written by [roadkeep](../roadkeep.toml), never by hand.** Open work only, one line per
> task; the `→ §` pointer is that task's design in [IMPROVEMENTS.md](IMPROVEMENTS.md).
> Shipped work lives in [CHANGELOG.md](CHANGELOG.md) and positioning in
> [STRATEGY.md](STRATEGY.md); the markers and limits are `roadkeep.toml`'s. Blocks are
> reused, not minted — `roadkeep brief` picks the next task and states why.

## Block CD — turing.viglet.org relevance pass (2026-10)

## Block BZ — Indexing API trust for connectors: key auth, scripted login, request validation

- 📋 **T1112** (deps: T1133) **An import DELETE removes by id or type only, so a connector sharing an SN site cannot delete its documents by a field** — Each connector lists its ids through search and deletes them one by one, and an id of * is accepted and deletes nothing. → §CXXIV
- 📋 **T1124** (deps: —) **POST /api/v2/summary refuses a Key-authenticated server call with 403, as CSRF does not exempt it** — A connector cannot draft a summary with its API key, though agents.md documents that call, and Dumont's own insights use it. → §CXXXVI
- 📋 **T1126** (deps: —) **Saving an audited entity over an API key fails with 500: the auditor casts the key's TurUser to TurCustomUserDetails** — A connector cannot create ranking expressions or any entity with createdBy, though the same key writes synonyms. → §CXXXVIII
- 📋 **T1133** (deps: —) **An API key reads and deletes every document of a shared SN site, as nothing binds a key to its own partition** — Each endpoint needs its own fq to keep shared sites apart, and any endpoint without one leaks another site's documents. → §CXLV

## Block CH — Published container images track the shipped source

- 📋 **T1113** (deps: —) **ghcr turing-ce:latest is the two-month-old 2026.3.4 build, so API-key imports fail with a 500 that T1061 already fixed** — A connector that follows the published quick start fails on its first import, and the fix needs a Turing checkout to build an image. → §CXXV

## Block BW — What the link-free response and its consumers still lack (§LXX)

- 📋 **T1114** (deps: —) **The structured POST search leaves spellCheck out where the GET search returns a correction for the same query** — A UI on the SDK's default POST-native controller never offers did-you-mean, or pays one more request for it. → §CXXVI
- 📋 **T1115** (deps: —) **A structured search filtered on a facet drops that facet and lists no selectedFilterQueries** — A UI cannot show which value is applied, switch to another value or remove the filter without tracking its own request. → §CXXVII
- 📋 **T1116** (deps: —) **Chat answers through the default agent while /answer says AI is not enabled, and no manifest can enable it** — A connector that provisions its site as code cannot make the answer engine work, and the two AI endpoints disagree. → §CXXVIII
- 📋 **T1117** (deps: —) **Natural-language filters resolve last year from the model's training date, returning a 2022 range in 2026** — A visitor asking for recent content gets a filter that hides all of it, with no sign it was guessed. → §CXXIX
- 📋 **T1118** (deps: —) **related-terms answers 401 to a browser, though the SDK exports it beside the public search calls** — A site cannot offer related searches without a server proxy, and nothing checks the SDK against the filter chain. → §CXXX
- 📋 **T1119** (deps: —) **Zero-result queries and content gaps are MCP tools only, with no REST endpoint a connector can call** — An admin screen outside the console cannot show what visitors failed to find, the metric that most tells what to write next. → §CXXXI
- 📋 **T1120** (deps: —) **The search matches whole words only, so an as-you-type search for a half-typed word finds nothing** — A search box that updates while typing shows No results until the word is complete, or guesses a wildcard into the query. → §CXXXII
- 📋 **T1121** (deps: —) **The SDK search controller drops a superseded answer but never aborts the superseded request** — Every pause in a typed query leaves a full search running and downloading on the server for an answer that is thrown away. → §CXXXIII
- 📋 **T1122** (deps: —) **A custom facet provisioned through the manifest is stored but never appears in the search answer** — A connector that declares range facets as code gets a 200 and a changed facet, and visitors never see a bucket. → §CXXXIV
- 📋 **T1123** (deps: T1116) **No cheap probe says whether answer or natural-language filters are on for a site** — A client learns it only by starting an LLM answer or parse, so a status screen either spends tokens or guesses. → §CXXXV
- 📋 **T1125** (deps: T1120) **Synonym mining proposes a query and its own wildcard or half-typed prefix as synonyms** — An as-you-type search fills the log with work and work*, so every mined suggestion is noise an owner must reject. → §CXXXVII
- 📋 **T1127** (deps: —) **An SN site's chat calls the LLM but the cost ledger records nothing: cost summary stays at zero** — No operator or client can see what a site's generative features cost, so no budget can be held to. → §CXXXIX
- 📋 **T1128** (deps: —) **The SDK's result-click call can be cut off by the navigation the click starts, and /click refuses a beacon** — Clicks are lost silently, more often on slow networks, so click-through rates undercount. → §CXL
- 📋 **T1129** (deps: —) **The search controller's analytics option writes a 30-day cookie before consent and reports every as-you-type search** — A consent-respecting site with instant search cannot use the SDK's search analytics and has to re-emit the events itself. → §CXLI
- 📋 **T1130** (deps: —) **The GA sink sends only turing_* event names, and the taxonomy has no search abandonment event** — GA4's site-search reports stay empty without hand-made custom definitions, and searched-and-left is never measured. → §CXLII
- 📋 **T1131** (deps: —) **Search-result clicks are stored but nothing reads them, and /click answers 202 with metrics off** — No site can see a click-through rate, and a client cannot tell that its clicks are being dropped. → §CXLIII
- 📋 **T1132** (deps: T1133) **The similar-documents search takes no filter, so a shared SN site answers with every site's documents** — A related-content rail must over-fetch and discard, and can come up short on a busy shared site. → §CXLIV

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
