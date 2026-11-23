# ADR 0005: A modular monolith verified by Spring Modulith, compatibility waived

- **Status:** Accepted (2026-11-19)
- **Deciders:** Alexandre Oliveira
- **Task:** T1305 (Block CZ): the gate that unblocks T1306–T1328. Deliverable is this
  ADR only; no code moves here.
- **Amends:** ADR 0001 (domain records and ports move into the module that owns them).

## Context

`turing-app` holds about 1,880 classes under top-level packages that mix layers with
features: `api` (228), `persistence` (431), `service` (105) are layers; `genai` (646),
`sn` (157), `kb`, `tenant`, `mcp` are features. Import counts show cycles nothing stops
from growing:

| Cycle | Imports one way / the other |
|---|---|
| `genai` ↔ `api` | 74 / 13 (genai uses API exceptions and services that live in controllers' packages) |
| `genai` ↔ `sn` | 28 / 8 |
| `persistence` → `sn`, `genai` | 21 / 7, through entity listeners and enums the entities import |
| `api` ↔ `sn`, `service` ↔ `genai` | smaller, both ways |

A change in one subsystem therefore compiles against, and can break, any other. The
entity graph is shared: `TurAIAgent` and `TurSNSite` load each other's associations, and
any module can write any table.

## Decision

### 1. One module per subsystem, verified by Spring Modulith

The backend becomes a modular monolith: each module a direct sub-package of
`com.viglet.turing`, declared with `@ApplicationModule`, exposing what other modules may
call through `@NamedInterface` packages, and verified in the unit suite (T1323: `TurModuleReportTest`
runs `ApplicationModules.verify()` on every `mvn test`, over a model that leaves out the
library jars sharing the base package: turing-commons' `commons` and `se`,
turing-java-sdk's `client`, turing-spring's `spring`). Modules
own their entities, repositories, DTOs, mappers and controllers; nothing is shared by
layer.

| Module | What it owns (today's packages) |
|---|---|
| `shared` (kernel) | API exceptions (`api.exception`), `utils`, `bean`, `jpa`, cross-module events; no business logic |
| `sn` | `sn.*`, `api.sn`, `domain.sn`, `exchange.sn`, `se.facet`/`se.result` beans, the SN entities in `persistence` |
| `searchengine` | `plugins.se`, `se`, `solr`, `lucene`, `elasticsearch`, `contentai`, `api.se` |
| `llm` | `genai.provider`, `genai.nativeapi`, `genai.gateway`, `genai.servicetier`, `genai.vigletai`, `genai.structuredoutputs`, `service.llm`, `resilience.llm`, `api.llm`, `api.embedding`, `api.gateway`, `domain.llm`, `onstartup.llm` |
| `agent` | `genai.flow`, `genai.tool`, `genai.clienttool`, `genai.skill`, `genai.workspace`, `genai.handoff`, `genai.prompt`, `genai.capture`, `genai.safety`, `genai.secondopinion`, `genai.verify`, `service.chatflow`, `api.agent`, `api.customtool`, `api.skill`, `api.workspace`, `api.intent`, `domain.agent`, `domain.customtool`, `exchange.agent` |
| `rag` | `genai.rag`, `genai.answer`, `genai.citation`, `genai.catalog`, `genai.feed`, `genai.research`, `genai.deepdive`, `genai.urlfetch`, `genai.connector`, `genai.multimodal`, `genai.ocr`, the SN chat (`genai.TurSNGenAi` and its context builders), `api.rag`, `api.research`, `api.ocr`, `api.ann`, `api.store` |
| `persona` | `genai.persona`, `genai.authoring`, `api.persona` |
| `eval` | `genai.eval`, `genai.distillation`, `genai.selftuning`, `genai.spectator` |
| `transcription` | `genai.transcription` |
| `batch` | `genai.batch` |
| `kb` | `kb`, `api.kb` |
| `chat` | `service.chatanalytics`, `service.chatmemory`, `service.chatslots`, `service.usermemory`, `genai.callcenter`, `api.chat`, `api.chatanalytics` |
| `storage` | `service.storage`, `service.asset`, `service.git`, `api.asset`, `api.git` |
| `tenant` | `tenant`, `api.tenant` |
| `auth` | `spring.security`, `service.auth`, `service.keycloak`, `api.auth`, `onstartup.auth` |
| `system` | `system`, `logging`, `email`, `artemis`, `tomcat`, `swagger`, `properties`, `bento`, `api.system`, `api.discovery`, `api.dev`, `api.queue`, `api.logging`, `api.integration`, `api.marketplace`, `api.page`, `api.bento`, `onstartup.system` |
| `exchange` | `exchange` (the import/export orchestration; each module's exchange beans stay with it) |
| `mcp` | `mcp`, `api.mcp`, `domain.mcp` |
| `observability` | `observability` |

Rules the verification enforces:

- **Direction.** `shared` depends on nothing. `system`, `tenant` and `auth` are
  infrastructure and depend only on `shared`. `searchengine` and `llm` depend on
  infrastructure only. `sn`, `kb`, `storage` depend on infrastructure and
  `searchengine`. `rag`, `agent`, `persona`, `eval`, `transcription`, `batch`, `chat`,
  `mcp` may depend on `llm`, `sn` and `searchengine`; none of those may depend back on
  them. `exchange` and `observability` sit on top.
- **Through named interfaces.** A module reaches another only through its
  `@NamedInterface` packages: a service facade, a read-only port (ADR 0001) or an event.
- **No shared entities.** A JPA association never crosses a module line; the other
  module's row is an id column resolved through that module (T1315). A module learns
  about another's changes from events published after commit (T1316), made
  at-least-once by the event publication registry (T1324: Spring Modulith's JPA
  registry, its `EVENT_PUBLICATION` table created by `system/v2026.3.8.0.yaml`; an
  asynchronous module listener is an `@ApplicationModuleListener`, its event carries the
  tenant it was published under, and `TurEventPublicationJob` republishes what is
  incomplete and purges what is old, one node at a time under ShedLock. The synchronous
  cache-eviction listeners stay outside the registry).
- **Seeders and controllers live with their module.** `onstartup` and `api` are emptied
  into the modules whose data and endpoints they are.

### 2. Compatibility is waived for internals

To reach that map without carrying two layouts, the following **may break** in this
block, with no shim kept:

- package and class names under `com.viglet.turing`;
- JPA mappings, entity class names and association shapes;
- table and column names (one naming rule per module, T1317);
- the Liquibase history: a squashed baseline per module replaces the 200-file chain
  (T1321).

An existing installation upgrades from 2026.2 (and from a 2026.3 build that predates the
squash) by **exporting its sites, agents and settings and importing them** into the new
version, not by migrating the database in place.

Done by T1321 (2026-11-23): `db/changelog/<module>/baseline.yaml` per module, generated
from a fresh replay of the old 220-file tree and checked equal to it on H2 (columns,
keys, constraints, indexes, seed rows), applied on MySQL 8 and PostgreSQL 16 by
`TurBaselineChangelogIT`. The master includes the modules in layer order; a foreign key
from a lower module's table to a higher one's lives in the higher module's file. Before
any baseline runs, `baseline-guard.yaml` halts on a `DATABASECHANGELOG` that still lists
an old `db/changelog/v*.yaml` file, with a message naming the export and import path.

**Not waived**, and held by tests through the whole block:

- REST paths, request and response shapes (`/api/sn/**`, `/api/v2/**`, the console API);
- the SDK contracts (`@viglet/turing-sdk`, `@viglet/turing-react-sdk`, the Java SDK, the
  WordPress plugin's calls);
- the exchange (import/export) format, which is the upgrade path.

### 3. ADR 0001 is amended

The `*Domain` records and `*RepositoryPort` interfaces stay bounded as ADR 0001 rules,
but each moves into the module that owns the data (`domain.sn` into `sn`, `domain.llm`
into `llm`, ...). The top-level `domain` package disappears; the ports become the
modules' named interfaces for reads.

### 4. Cache eviction crosses modules only as events

An entity names only listeners of its own module (T1316). When another module caches
something derived from it, the owning module publishes an event from its own entity
listener (`TurAIAgentChangedEvent`), and the caching module evicts on it, still after
commit. `TurEntityListenerModuleTest` fails on a new crossing.

Listeners inside one module stay JPA entity listeners rather than events published by
services. A listener fires on every write, whichever service made it; a published event
fires only where someone remembered to publish it, which is the stale-cache defect T896
fixed.

### 5. Tenancy rulings

Every entity has one of four rulings (T1318); `TurTenancyRulingConventionTest` holds the
lists and fails on an entity without one.

- **Tenant-owned**: Hibernate `@TenantId`. This is the one mechanism for a row a tenant
  owns; it is stamped and filtered without any code asking.
- **Plain `tenantId`, on purpose**: a column the code stamps by hand and may read across
  tenants. Only three reasons qualify: the BYO-infra pool, where a null tenant is the
  GLOBAL pool every tenant sees (`TurSEInstance`, `TurLLMInstance`, `TurEmbeddingModel`,
  `TurStoreInstance`, `TurIntegrationInstance`, `TurMcpServer`); billing that rolls usage
  up across tenants (`TurLLMTokenUsage`, `TurAiUsageDaily`); and the gateway key, which
  is looked up before any tenant is bound (`TurGatewayKey`).
- **Global**: platform catalogs and identity, with no tenant column. The vendors
  (`TurLLMVendor`, `TurSEVendor`, `TurStoreVendor`), `TurLLMPrice`, `TurLocale`,
  `TurConfigVar`, `TurSNSourceType`; the identity tables (`TurUser`, `TurGroup`,
  `TurRole`, `TurPrivilege`), since one user spans tenants; and the tenancy tables
  themselves (`TurTenant`, `TurTenantMembership`).
- **Through an owner**: a child row with no column of its own, isolated because it is
  reached through a tenant-scoped owner (an SN site's fields, rules and spotlights, an
  agent's slots and conversation records, a knowledge base's terms). The test names each
  child's owner and checks the owner is tenant-scoped.

### 6. Rulings on the measured crossings (2026-11-21)

Measured after T1315, the code needed dependencies the direction rules forbid (T1322).
Each one was ruled: **move** the code so the crossing disappears, or **amend** the map.
None is allowed as an exception; a module that cannot meet its layer moves its class.

The layers, bottom to top, replacing the direction paragraph of §1 where they differ:

1. `shared`: also absorbs `bean`, `jpa`, `utils`.
2. Infrastructure: `system` (absorbs `artemis`, `logging`, `email`, `properties`,
   `swagger`, `tomcat`, `bento`, the generic part of `resilience`, `urlfetch`), `tenant`,
   `auth` (absorbs `spring`), and **`observability`**, moved down from the top: every
   module records meters and observations, so it is infrastructure. A class in these
   packages that imports a feature module (an SN mail template, an LLM retry policy, a
   per-module property class) moves into that feature module. **Inside the layer the
   order is `system`, then `tenant`, then `auth`** (T1322l): `system` imports only
   `shared`, `tenant` only `system` and `shared`, `auth` only those three, which keeps the
   dominant direction (auth and tenant read system's configuration). What a lower one
   needs from a higher one is an SPI the lower one declares. **`storage`** (object
   storage, assets and the Git server) also sits here, above `tenant` whose
   `TurTenantObjectStore` it implements (moved down from layer 4 by T1322: the Lucene
   engine syncs its index through it and SN serves stored files, so it is infrastructure
   for layer 3 and up; asset training, which embeds into stores, moved to `rag`).
   `observability` reads only `system` and `shared`.
3. `searchengine`, `llm`. **`llm` also owns the vector stores** (store instances, vendors
   and providers), which `rag`, `agent` and `persona` all read as infrastructure.
4. `sn`, **`transcription`** (moved down from layer 8 by T1322m: it is a
   speech-to-text provider layer that `persona`'s audio derivation needs; it reads `llm`'s
   instances, so it sits above `llm` rather than beside it).
5. `kb` (**moved above `sn`**: thesaurus configuration is per SN site), `rag` retrieval
   (context building, sources, rerank, citations, ocr, multimodal, feed, connectors).
6. `persona`, **`batch`** (moved down from layer 8 by T1322: the batch tier reads `llm`,
   `sn` and `rag`, and the gateway in `agent` and the eval runner submit to it).
7. `agent` (**absorbs `chat`**: sessions, slots, memory, webhooks and call-center assist
   are the agent's conversation state and their entities already live in `agent`).
8. `answer` (**new**, split from `rag`: the SN chat `TurSNGenAi`, answers, research,
   deep dive and the catalog copilot, which run agents), `eval`, `mcp`. **Inside the layer
   the order is `answer`, then `eval`, then `mcp`** (T1322): eval promotes research
   studies into datasets, and the MCP write tools reindex a site through answer's
   content exchange.
9. `exchange`.

A module depends only on lower layers, never sideways inside its own layer.

| Crossing (imports) | Ruling |
|---|---|
| `llm` → `agent` 91, `persona` 7, `rag` 30 | **Move.** Native chat execution (`TurNativeChatExecutor`, provider services that run an agent's turn with its tools, memory, workspace or computer use), the gateway's agent routing and `llm.web.chat` move to `agent`; `TurDefaultRagInfraOnStartup` to `rag`. `llm` keeps the provider clients and the capability registry. |
| `sn` → `agent` 42, `rag` 29, `chat` 12, `persona` 11 | **Move.** SN's GenAI surface (`sn.web.genai`, the LLM entity extractor, the manifest GenAI converger, `sn.dsl.eval`, `sn.contentfit`, the snapshot agent listeners, hybrid ranking) moves to `answer`, `rag`, `eval` or `persona`. Where the search path itself must call up, `sn` declares an SPI the upper module implements. Done by T1322h: `sn` imports none of `agent`, `rag`, `answer`, `persona`, `eval`. Its SPIs live in `sn.api` (`TurSNSiteAgents` for anything about a site's agent, extended before a new agent-facing SPI is added; `TurSNSiteEmbeddings`, `TurSNAudienceFit`, `TurSNDefaultEmbeddingModel`), plus `sn.ranking.TurSNHybridRankingService`, which `rag` implements. The visitor chat controller moved to `answer.web` with its REST paths unchanged. |
| `sn` → `kb` 20, `kb` → `sn` 10 | **Amend + move.** `kb` sits above `sn`; `sn.kb` moves to `kb`, and query expansion reaches it through an `sn` SPI. |
| `searchengine` → `sn` 158 | **Move**, option 1 of T1310: a site search configuration SPI in `searchengine` that `sn` implements. Done by T1310: `searchengine` imports nothing from `sn`. The SPI lives in `searchengine.spi` (`TurSESiteSearchConfig` and its records; `TurSESite` and `TurSESiteLocale` are what the plugin contract takes), and `sn.api.TurSNSiteRefs` converts an SN site, locale or field map into them at the call site. |
| `agent` ↔ `chat` 26 / 79 | **Amend.** `chat` merges into `agent`. |
| `agent` ↔ `eval` 19 / 138 | **Move.** The eval controllers in `agent.web` and the eval entities move to `eval`; the dispatcher's spectator and online-eval hooks become events `agent` publishes and `eval` listens to. |
| `agent` ↔ `persona` 34 / 14 | **Move.** `persona` sits below `agent`; the persona chat API, dialogue runner and agent-persona resolver move to `agent`. |
| `agent` ↔ `rag` 22 / 38 | **Amend + move.** Retrieval stays below `agent`; what runs agents moves to `answer`. |
| `agent` ↔ `mcp` 11 / 12 | **Move.** The MCP client (`TurMcpServer`, its repository, `TurMcpToolCallbackService`) moves to `agent`; `mcp` keeps the server and sits above `agent`. |
| `system` → `agent` 8, `chat` 4 | **Move.** Parked-conversation and chat-analytics controllers move to `agent`; global settings read the code interpreter through a `system` SPI. |
| `agent`, `sn` → `observability` | **Amend**: `observability` is infrastructure (layer 2). |
| `system` ↔ `tenant` ↔ `auth`, and layer 2 → feature modules | **Amend + move** (T1322l): ordered `system` < `tenant` < `auth`. System reads the tenant context through viglet-core's tenancy types, and storage and Git status through `TurStorageStatus`/`TurGitServerStatus`. Tenant counts plan resources through `TurTenantResourceCount`, purges through `TurTenantObjectStore` and finds upgrade URLs through `TurTenantUpgradeTarget`, each implemented by the owning module. Single-feature controllers moved to their feature with REST paths unchanged. |
| Residual crossings after T1322a–l, measured 2026-11-23 | **Move + amend** (T1322): the tool-execution loop and its park exception moved to `llm.tool`, the grounding-mode enum to `rag`, asset training to `rag.asset`, the SN content exchange to `answer.exchange`, the research eval bridge and the agent suggestion API to `eval`. Endpoints that call up were split into a controller in the upper module with the same paths: SN export and reindex and the agent export (`exchange.web`), asset training (`rag.web.asset`), promote-to-dataset (`eval.web`), store batch re-embedding (`batch.web`). Then each module was declared: its base package's `package-info` names, as `allowedDependencies`, exactly the modules below it in the layers above (plus the library jars), and every package another module used on 2026-11-23 is a `@NamedInterface`, so the rest is internal. `TurModuleDeclarationTest` fails on a top-level package without a declaration. |
| `persona` → `transcription`, `transcription` → `kb` 1 | **Amend + move** (T1322m): `transcription` sits in layer 4; its Thesaurus keyword hint is `TurTranscriptionKeywordSource`, an SPI `transcription` declares and `kb` implements. |

### 7. Audit dates (T1319)

`TurAuditable` is extended by the **console-edited aggregates only**; append-only event
rows keep their single creation timestamp. Responses keep today's JSON names through
`@JsonProperty` on the subclass (an overriding getter or a Jackson mix-in), not a DTO in
front of each entity.

## Consequences

- Every module is independently verifiable; a new cycle fails the build once T1323
  lands, and module slice tests (T1325) start only what a module declares.
- The moves are large and mechanical; they run in dependency order (T1306 baseline
  report, then the kernel, then controllers, genai, search engines, services, entities,
  associations, schema), each one green before the next.
- Installations upgrade through export/import once the Liquibase baseline lands; the
  release notes for that version must say so first.
- Contributor docs (agents.md, the skills, the hand-written lint rules) are rewritten
  for modules at the end (T1328), so for a while they describe the old layout.
