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

- 🛠 **T1310** (deps: T1306 ✅) **Search-engine code is spread over plugins, se, solr, lucene, elasticsearch and contentai** — The plugin contract still takes TurSNSite and TurSNSiteLocale, and 37 engine files import sn 156 times, so searchengine and sn depend on each other. → §CLXXXIX.6
- 🛠 **T1315** (deps: T1312 ✅, T1313 ✅, T1314 ✅) **JPA associations cross module lines, so TurAIAgent and TurSNSite load each other's graph** — TurAIAgent (persona, LLM and MCP sets) and TurSNSite (SE instance) still hold JPA associations into other modules. → §CLXXXIX.11
- 🛠 **T1319** (deps: T1314 ✅) **No entity has @Version and only one records created and modified dates through auditing** — Aggregates other than the agent and the SN site still have no @Version, and no shared superclass replaces the 20 hand-rolled date fields. → §CLXXXIX.15
- 📋 **T1321** (deps: T1315 🛠, T1317 ✅, T1318 ✅, T1319 🛠, T1320 ✅) **One Liquibase tree of 205 files replays the whole history and no module owns its tables** — With compatibility waived, a squashed baseline per module replaces the history; a 2026.2 install upgrades by export and import. → §CLXXXIX.17
- 📋 **T1322** (deps: T1308 ✅, T1310 🛠, T1311 ✅, T1315 🛠) **No package declares what it exposes, so any class is reachable from any module** — A package-info with @ApplicationModule and allowedDependencies on each module, plus @NamedInterface on what others may call, turns the map in ADR 0005 into code. → §CLXXXIX.18
- 📋 **T1323** (deps: T1322, T1316 ✅) **The module report never fails the build, so a new cycle merges unnoticed** — Once the moves land, the module verification runs in the unit suite and fails on any cycle or any access to another module's internal package. → §CLXXXIX.19
- 📋 **T1324** (deps: T1316 ✅, T1321) **A module event lost to a crash between commit and listener is never delivered** — The JPA event publication registry, its table in the Liquibase baseline, tenant-aware and republished by one node under ShedLock, makes cross-module listeners at-least-once. → §CLXXXIX.20
- 📋 **T1325** (deps: T1323) **Every integration test boots the whole context, even for one module's behaviour** — Module slice tests boot one module with its declared dependencies, so module ITs start faster and fail when a hidden dependency appears. → §CLXXXIX.21
- 📋 **T1326** (deps: T1323) **No diagram shows the backend's modules or how they depend on each other** — Documenter output generated from the verified model keeps a contributor architecture page current without anyone redrawing it. → §CLXXXIX.22
- 📋 **T1327** (deps: T1323) **Traces do not show which module a request crossed or where its time went** — Module observability wraps calls into each module's named interface in Micrometer observations, so the existing tracing shows time per module. → §CLXXXIX.23
- 📋 **T1328** (deps: T1323) **agents.md, skills and hand-written lints still describe the old layered package layout** — Rewriting agents.md and the persistence, domain-layer, search and IT skills for modules, and dropping lint rules the verifier now enforces, keeps one source per rule. → §CLXXXIX.24
- 📋 **T1336** (deps: T1334 ✅) **With tenancy on, a conversation that left only chat memory cannot be opened in the session inspector** — Chat memory stores no owning agent, so the T1334 guard finds no tenant-scoped record for a plain RAG chat and answers 404. → §CLXXXIX.31

## Done when — T1184

- **An evaluator sees the admin console without installing** A read-only guest console
  on the demo host (browse, no save, reset on a schedule) or a recorded walk-through on
  the site, linked from the README.

## Done when — T1310

- **No module outside searchengine imports an engine's internal package**
  TurModuleReportTest reports no "non-exposed type com.viglet.turing.searchengine."
  violation: callers go through TurSearchEnginePlugin, and the DSL executors either move
  into the module with the DSL types or call the plugin.

## Done when — T1315

- **No JPA association targets an entity of another module** Every @ManyToOne,
  @OneToMany, @ManyToMany and @OneToOne field's type lives in the same top-level module
  package as its entity; checked by grepping the association fields' types against their
  packages.

## Done when — T1319

- **Every console-edited aggregate refuses a save from an older version** The SN site
  and the other aggregates the console edits carry @Version, and an update API test per
  aggregate shows a stale save answering 409.
- **Audit dates come from one mapped superclass** No entity declares its own createdAt
  or modifiedAt field; a convention test fails on one that does.

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
