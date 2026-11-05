# Turing — Roadmap (active backlog)

> **Written by [roadkeep](../roadkeep.toml), never by hand.** Open work only, one line per
> task; the `→ §` pointer is that task's design in [IMPROVEMENTS.md](IMPROVEMENTS.md).
> Shipped work lives in [CHANGELOG.md](CHANGELOG.md) and positioning in
> [STRATEGY.md](STRATEGY.md); the markers and limits are `roadkeep.toml`'s. Blocks are
> reused, not minted — `roadkeep brief` picks the next task and states why.

## Block CD — turing.viglet.org relevance pass (2026-10)

## Block BV — The quality gate stops at the package boundary

## Block CJ — Releases ship from the repository the README names

## Block AR — Live-demo search & chat quality (Lucene parity gaps, §XLI)

## Block AN — Security hardening (post-audit, §XXXVII)

## Block CL — SN sites for research and memory archives

## Block CF — What the SN site manifest cannot declare

## Block CN — Autocomplete and suggestions

## Block CG — Search-engine plugin parity (Solr, Lucene, Elasticsearch)

- 📋 **T1233** (deps: —) **A negative filter query (fq[]=-field:value) excludes nothing on Lucene and Elasticsearch, and misgroups on Solr** — A client cannot say all but its own documents, so it pages the whole index to find another source's records. → §CLXXV.2

## Block CO — SDK errors that carry Turing's own answer

- 📋 **T1234** (deps: —) **The JS SDK's TuringHttpError drops the response body, so a 429 quota-exhausted never shows its resetsAt or detail** — A client cannot tell visitors when AI answers come back, nor read any problem field Turing sends. → §CLXXVI.1

## Done when — T1184

- **An evaluator sees the admin console without installing** A read-only guest console
  on the demo host (browse, no save, reset on a schedule) or a recorded walk-through on
  the site, linked from the README.

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
