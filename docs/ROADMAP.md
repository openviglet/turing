# Turing — Roadmap (active backlog)

> **Written by [roadkeep](../roadkeep.toml), never by hand.** Open work only, one line per
> task; the `→ §` pointer is that task's design in [IMPROVEMENTS.md](IMPROVEMENTS.md).
> Shipped work lives in [CHANGELOG.md](CHANGELOG.md) and positioning in
> [STRATEGY.md](STRATEGY.md); the markers and limits are `roadkeep.toml`'s. Blocks are
> reused, not minted — `roadkeep brief` picks the next task and states why.

## Block CD — turing.viglet.org relevance pass (2026-10)

## Block BZ — Indexing API trust for connectors: key auth, scripted login, request validation

## Block BW — What the link-free response and its consumers still lack (§LXX)

- 📋 **T1144** (deps: —) **Catalog prices are keyed by plugin slug (openai) but looked up by vendor id (OPENAI), so every call costs $0** — T777 already writes gpt-4o-mini's catalog price; the key mismatch hides it from every instance, and an unpriced model still reads $0 rather than unknown. → §LXX.10

## Block CF — What the SN site manifest cannot declare

- 📋 **T1146** (deps: —) **A manifest genAi block cannot turn GenAI on and keep the site's agent; it always rebinds one** — A connector that only wants answers on overwrites the agent an admin chose in the console. → §CLV.1

## Block BV — The quality gate stops at the package boundary

- 📋 **T1142** (deps: —) **The npm @viglet/turing-sdk lags its source: 2026.3.9 has no postNlFilters, so consumers hand-write the request** — 16 sdk/src commits since the 2026-08-24 release (react-sdk: 5) reach no consumer; release is a hand bump plus a manual dispatch, and nothing counts what waits. → §CLIII.2

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
