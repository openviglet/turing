# Turing — Roadmap (active backlog)

> **Written by [roadkeep](../roadkeep.toml), never by hand.** Open work only, one line per
> task; the `→ §` pointer is that task's design in [IMPROVEMENTS.md](IMPROVEMENTS.md).
> Shipped work lives in [CHANGELOG.md](CHANGELOG.md) and positioning in
> [STRATEGY.md](STRATEGY.md); the markers and limits are `roadkeep.toml`'s. Blocks are
> reused, not minted — `roadkeep brief` picks the next task and states why.

## Block CD — turing.viglet.org relevance pass (2026-10)

## Block BZ — Indexing API trust for connectors: key auth, scripted login, request validation

- 📋 **T1112** (deps: —) **An import DELETE removes by id or type only, so a connector sharing an SN site cannot delete its documents by a field** — Each connector lists its ids through search and deletes them one by one, and an id of * is accepted and deletes nothing. → §CXXIV

## Block CH — Published container images track the shipped source

- 📋 **T1113** (deps: —) **ghcr turing-ce:latest is the two-month-old 2026.3.4 build, so API-key imports fail with a 500 that T1061 already fixed** — A connector that follows the published quick start fails on its first import, and the fix needs a Turing checkout to build an image. → §CXXV

## Block BW — What the link-free response and its consumers still lack (§LXX)

- 📋 **T1114** (deps: —) **The structured POST search leaves spellCheck out where the GET search returns a correction for the same query** — A UI on the SDK's default POST-native controller never offers did-you-mean, or pays one more request for it. → §CXXVI
- 📋 **T1115** (deps: —) **A structured search filtered on a facet drops that facet and lists no selectedFilterQueries** — A UI cannot show which value is applied, switch to another value or remove the filter without tracking its own request. → §CXXVII
- 📋 **T1116** (deps: —) **Chat answers through the default agent while /answer says AI is not enabled, and no manifest can enable it** — A connector that provisions its site as code cannot make the answer engine work, and the two AI endpoints disagree. → §CXXVIII
- 📋 **T1117** (deps: —) **Natural-language filters resolve last year from the model's training date, returning a 2022 range in 2026** — A visitor asking for recent content gets a filter that hides all of it, with no sign it was guessed. → §CXXIX
- 📋 **T1118** (deps: —) **related-terms answers 401 to a browser, though the SDK exports it beside the public search calls** — A site cannot offer related searches without a server proxy, and nothing checks the SDK against the filter chain. → §CXXX
- 📋 **T1119** (deps: —) **Zero-result queries and content gaps are MCP tools only, with no REST endpoint a connector can call** — An admin screen outside the console cannot show what visitors failed to find, the metric that most tells what to write next. → §CXXXI

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
