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

- 📋 **T1224** (deps: —) **A custom facet buckets one date field: an event spanning a start and an end date cannot fall in the periods it overlaps** — An exhibition running all month sits under Past once it has started, so an agenda's Next 7 days misses what is on. → §CLXIX.2

## Block CN — Autocomplete and suggestions

- 📋 **T1226** (deps: —) **Scoped autocomplete (fq or a partitioned key) answers document titles, not terms, and Lucene marks them up** — A client cannot scope its suggestions without changing what a suggestion is, and raw <mark> tags reach plain-text lists. → §CLXXI.1

## Block AQ — Knowledge Base: microthesauri & index-time term expansion (§XL)

- 📋 **T1228** (deps: —) **Nothing fills the turing_entity facets: people and places a document names outside any thesaurus are no facet** — No source can offer browsing by person or place it has no vocabulary for, though Turing already facets the fields. → §CLXXIII.1

## Block BF — Vectorless (Structured-Data) RAG: name it, feed it, integrate it (§LIV)

- 📋 **T1229** (deps: —) **A structured feed is re-ingested only on its hourly timer: no endpoint lets a publisher trigger a pull after a deploy** — The vectorless copilot answers from the previous feed for up to an hour after every catalog release, and operators cannot re-sync on demand. → §CLXXIV.1

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
