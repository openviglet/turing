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

- 📋 **T1308** (deps: T1307 ✅) **Controllers and services of every subsystem share the api package, so genai calls into it** — Each controller moves into the module it serves and services such as TurSNSiteSearchService leave api, so web adapters depend inward and nothing depends on them. → §CLXXXIX.4
- 📋 **T1309** (deps: T1306 ✅) **genai is one 646-class package mixing providers, agents, personas, eval and transcription** — Splitting it into llm, agent, rag, persona, eval, transcription and batch modules gives each a boundary the verifier can hold, instead of one package that depends on everything. → §CLXXXIX.5
- 📋 **T1310** (deps: T1306 ✅) **Search-engine code is spread over plugins, se, solr, lucene, elasticsearch and contentai** — One searchengine module exposing TurSearchEnginePlugin as its named interface keeps sn and genai off the engine internals they reach into today. → §CLXXXIX.6
- 📋 **T1311** (deps: T1306 ✅) **The service and onstartup packages hold chat, storage, auth and seeding for unrelated modules** — Chat analytics, slots and memory form a chat module, storage and assets another, and each seeder moves into the module whose data it writes. → §CLXXXIX.7
- 📋 **T1312** (deps: T1306 ✅) **Semantic Navigation entities, repositories and DTOs live in a shared persistence package** — Moving them into the sn module as internal packages makes sn the only writer of its tables, with other modules reading through its named interface or its events. → §CLXXXIX.8
- 📋 **T1313** (deps: T1309) **LLM, agent and flow entities sit in persistence, apart from the genai modules that own them** — Moving entities, repositories, DTOs, mappers and enums into the new genai modules ends persistence importing genai types such as TurBatchKind. → §CLXXXIX.9
- 📋 **T1314** (deps: T1311) **Chat, thesaurus, tenant, system and persona entities still sit in the shared persistence package** — Moving the rest empties persistence.model, repository, dto, mapper and adapter, so the package is deleted and no module shares another's entity classes. → §CLXXXIX.10
- 📋 **T1315** (deps: T1312, T1313, T1314) **JPA associations cross module lines, so TurAIAgent and TurSNSite load each other's graph** — With compatibility waived, cross-module JPA associations become id columns resolved through the owning module, the reference style Spring Modulith expects. → §CLXXXIX.11
- 📋 **T1316** (deps: T1312) **Entity classes name sn and genai eviction listeners, so persistence depends on its callers** — Owning modules publish change events after commit and the cache owners listen, which removes the back-edge and the @EntityListeners coupling on the SN and agent entities. → §CLXXXIX.12
- 📋 **T1317** (deps: T1314) **Table names follow no convention: 87 of 105 lack the tur_ prefix the other 18 carry** — With compatibility waived, every table takes one rule naming its module (for example sn_site, agent_eval_case), so a table's owner is readable from the schema alone. → §CLXXXIX.13
- 📋 **T1318** (deps: T1314) **54 of about 106 entities have no tenant column, and the rest mix @TenantId with plain tenantId** — Each entity is ruled tenant-scoped or deliberately global, and every tenant-scoped one uses one mechanism, so Block J isolation no longer depends on which entity a query hits. → §CLXXXIX.14
- 📋 **T1319** (deps: T1314) **No entity has @Version and only one records created and modified dates through auditing** — A mapped superclass with audit dates and an optimistic-lock version stops silent concurrent overwrites and replaces 20 hand-rolled date fields. → §CLXXXIX.15
- 📋 **T1320** (deps: T1314) **29 @Lob fields leave each large-text column's type to the database dialect** — Mapping large text to the portable longtext column the persistence rules require, and binary data to storage, gives the same schema on H2, MySQL, PostgreSQL and SQL Server. → §CLXXXIX.16
- 📋 **T1321** (deps: T1315, T1317, T1318, T1319, T1320) **One Liquibase tree of 205 files replays the whole history and no module owns its tables** — With compatibility waived, a squashed baseline per module replaces the history, and a 2026.2 install upgrades by export and import instead of by migration. → §CLXXXIX.17
- 📋 **T1322** (deps: T1308, T1310, T1311, T1315) **No package declares what it exposes, so any class is reachable from any module** — A package-info with @ApplicationModule and allowedDependencies on each module, plus @NamedInterface on what others may call, turns the map in ADR 0005 into code. → §CLXXXIX.18
- 📋 **T1323** (deps: T1322, T1316) **The module report never fails the build, so a new cycle merges unnoticed** — Once the moves land, the module verification runs in the unit suite and fails on any cycle or any access to another module's internal package. → §CLXXXIX.19
- 📋 **T1324** (deps: T1316, T1321) **A module event lost to a crash between commit and listener is never delivered** — The JPA event publication registry, its table in the Liquibase baseline, tenant-aware and republished by one node under ShedLock, makes cross-module listeners at-least-once. → §CLXXXIX.20
- 📋 **T1325** (deps: T1323) **Every integration test boots the whole context, even for one module's behaviour** — Module slice tests boot one module with its declared dependencies, so module ITs start faster and fail when a hidden dependency appears. → §CLXXXIX.21
- 📋 **T1326** (deps: T1323) **No diagram shows the backend's modules or how they depend on each other** — Documenter output generated from the verified model keeps a contributor architecture page current without anyone redrawing it. → §CLXXXIX.22
- 📋 **T1327** (deps: T1323) **Traces do not show which module a request crossed or where its time went** — Module observability wraps calls into each module's named interface in Micrometer observations, so the existing tracing shows time per module. → §CLXXXIX.23
- 📋 **T1328** (deps: T1323) **agents.md, skills and hand-written lints still describe the old layered package layout** — Rewriting agents.md and the persistence, domain-layer, search and IT skills for modules, and dropping lint rules the verifier now enforces, keeps one source per rule. → §CLXXXIX.24

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
