# Turing — Roadmap (active backlog)

> **Written by [roadkeep](../roadkeep.toml), never by hand.** Open work only, one line per
> task; the `→ §` pointer is that task's design in [IMPROVEMENTS.md](IMPROVEMENTS.md).
> Shipped work lives in [CHANGELOG.md](CHANGELOG.md) and positioning in
> [STRATEGY.md](STRATEGY.md); the markers and limits are `roadkeep.toml`'s. Blocks are
> reused, not minted — `roadkeep brief` picks the next task and states why.

## Block CD — turing.viglet.org relevance pass (2026-10)

## Block BZ — Indexing API trust for connectors: key auth, scripted login, request validation

## Block BW — What the link-free response and its consumers still lack (§LXX)

- 📋 **T1144** (deps: —) **The LLM cost ledger books a model with no price row at zero dollars, so a dollar budget never trips** — The model Turing creates at boot has no price, so cost/summary reports a paid run as free and nothing tells free from unknown. → §LXX.10
- 📋 **T1145** (deps: —) **A search sent through the SDK cannot opt out of metrics, so typed prefixes fill top terms and failed searches** — A client that searches while typing records every half-word as a search, and the insights show those instead of what visitors settled on. → §LXX.11

## Block CF — What the SN site manifest cannot declare

## Block BV — The quality gate stops at the package boundary

- 📋 **T1141** (deps: —) **turing-app does not build at HEAD: a bento test fixture fails tsc -b, so the jar and the image fail too** — A test's type drift blocks every production build and image, and nothing caught it at the commit that changed the type. → §CLIII.1
- 📋 **T1142** (deps: —) **The npm @viglet/turing-sdk lags its source: 2026.3.9 has no postNlFilters, so consumers hand-write the request** — Every SDK function shipped since the last release is unusable from npm, and nothing shows the gap. → §CLIII.2

## Block AI — Persona as a first-class actionable surface (§XXXII)

- 📋 **T1143** (deps: —) **A site's client cannot list the audience personas its content-fit endpoint accepts, so it cannot offer one** — Every consumer of content-fit would copy Turing's agent and persona resolution to find valid ids, or hardcode them. → §CLIV.1

## Done when — T1133

- **Every SN endpoint takes a partitioned key and keeps it in its partition**
  TurSdkContractIT's two-partition test covers every endpoint, and the guard's ENFORCED
  list matches every SN route.

## Done when — T1132

- **fq[] on /search/duplicates keeps clusters to the documents that match**
  TurSdkContractIT's two-value test also runs on the duplicates endpoint of a hybrid
  site.

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
