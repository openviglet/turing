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
call through `@NamedInterface` packages, and verified in the unit suite (T1323). Modules
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
  at-least-once by the event publication registry (T1324).
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
