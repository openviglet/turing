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

## Block CL — SN sites for research and memory archives

## Block CF — What the SN site manifest cannot declare

## Block CT — The WordPress plugin (Viglet Turing Search)

- 📋 **T1260** (deps: T1253 ✅, T1255 ✅, WordPress.org: first review approved and SVN access granted) **A release tag publishes the zip on GitHub only, so the plugin cannot be found or updated from wp-admin** — Sites update from the directory; a manual SVN commit per release gets skipped (ex-Dumont D227). → §CLXXXII.6

## Block CZ — Backend modules with verified boundaries

- 📋 **T1323** (deps: T1322 📋, T1316 ✅) **The module report never fails the build, so a new cycle merges unnoticed** — Once the moves land, the module verification runs in the unit suite and fails on any cycle or any access to another module's internal package. → §CLXXXIX.19
- 📋 **T1324** (deps: T1316 ✅, T1321 📋) **A module event lost to a crash between commit and listener is never delivered** — The JPA event publication registry, its table in the Liquibase baseline, tenant-aware and republished by one node under ShedLock, makes cross-module listeners at-least-once. → §CLXXXIX.20
- 📋 **T1325** (deps: T1323) **Every integration test boots the whole context, even for one module's behaviour** — Module slice tests boot one module with its declared dependencies, so module ITs start faster and fail when a hidden dependency appears. → §CLXXXIX.21
- 📋 **T1326** (deps: T1323) **No diagram shows the backend's modules or how they depend on each other** — Documenter output generated from the verified model keeps a contributor architecture page current without anyone redrawing it. → §CLXXXIX.22
- 📋 **T1327** (deps: T1323) **Traces do not show which module a request crossed or where its time went** — Module observability wraps calls into each module's named interface in Micrometer observations, so the existing tracing shows time per module. → §CLXXXIX.23
- 📋 **T1328** (deps: T1323) **agents.md, skills and hand-written lints still describe the old layered package layout** — Rewriting agents.md and the persistence, domain-layer, search and IT skills for modules, and dropping lint rules the verifier now enforces, keeps one source per rule. → §CLXXXIX.24
- 🛠 **T1310** (deps: T1306 ✅) **Search-engine code is spread over plugins, se, solr, lucene, elasticsearch and contentai** — The engines still import sn 46 times, mostly for site, locale, field and facet entities. → §CLXXXIX.6
- 📋 **T1321** (deps: T1315 ✅, T1317 ✅, T1318 ✅, T1319 ✅, T1320 ✅, T1322) **One Liquibase tree of 205 files replays the whole history and no module owns its tables** — A squashed baseline per module replaces the history once T1322 settles which module owns each table. → §CLXXXIX.17
- 📋 **T1322** (deps: T1308 ✅, T1310 🛠, T1311 ✅, T1315 ✅, T1322a ✅, T1322b ✅, T1322c ✅, T1322d ✅, T1322e ✅, T1322f ✅, T1322g ✅, T1322h 🛠, T1322i ✅, T1322j ✅, T1322k ✅, T1322l 🛠) **No package declares what it exposes, so any class is reachable from any module** — Package-info files declare the modules. → §CLXXXIX.18
- 🛠 **T1322h** (deps: T1322d ✅, T1322f ✅, T1322g ✅) **sn imports agent, rag, chat and persona about 90 times, though they sit above it** — 7 sn classes of the GenAI surface still import agent, rag or persona 45 times: genai controllers, entity indexing, content fit, hybrid ranking, synonym mining. → §CLXXXIX.40
- 🛠 **T1322l** (deps: T1322a ✅) **system, tenant and auth import each other and reach into storage, sn, llm and other feature modules** — system now imports only shared; tenant and auth still import feature modules, and tenant's counters and teardown need SPIs. → §CLXXXIX.45

## Block DA — One bad document never costs the rest of an import

- 📋 **T1343** (deps: —) **A value the engine drops at index time is logged on the server only; the client and the indexing log never see it** — A client sending malformed dates or numbers gets 200 and INDEXED and learns of the loss only by reading Turing's server log. → §CXC.2

## Block CB — LLM gateway parity with the OpenAI wire

- 📋 **T1341** (deps: —) **The LLM gateway has no /v1/files and /v1/batches, so a bulk job pays full price one synchronous call at a time** — Turing already batches its own jobs at half price (T156), but a gateway client generating thousands of texts waits an hour and pays double. → §CXIII.6

## Done when — T1184

- **An evaluator sees the admin console without installing** A read-only guest console
  on the demo host (browse, no save, reset on a schedule) or a recorded walk-through on
  the site, linked from the README.

## Done when — T1310

- **No module outside searchengine imports an engine's internal package**
  TurModuleReportTest reports no "non-exposed type com.viglet.turing.searchengine."
  violation: callers go through TurSearchEnginePlugin, and the DSL executors either move
  into the module with the DSL types or call the plugin.

## Done when — T1322h

- **sn imports no agent, rag, persona, answer or eval type** Counting imports between
  top-level packages finds none from sn to agent, rag, persona, answer or eval; after
  the entity-graph slice, 22 sn classes still import one of them directly.

## Done when — T1322l

- **system imports only shared, tenant only system and shared** A grep of the system and
  tenant packages finds no import of another module beyond those named, and auth imports
  only system, tenant and shared.

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
