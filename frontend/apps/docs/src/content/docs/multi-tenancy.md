---
title: "Multi-Tenancy"
description: "Run one Viglet Turing JVM that serves many fully isolated tenants: discriminator-column data isolation, a single Keycloak realm with a tenant claim, tenant-owned infrastructure, plan quotas, and tenant lifecycle."
---
Viglet Turing can run as a **single JVM process that serves many isolated tenants** (a SaaS / shared-platform deployment), or as a classic **single-tenant install**. The same binary does both: the behavior is controlled by one flag.

:::note[One paragraph summary]
Tenant data isolation is a **discriminator column**: Hibernate's native `@TenantId` adds the partition column to every insert and a `WHERE tenantId = ?` to every read, driven by a request-scoped current-tenant holder. Identity is a **single Keycloak realm plus a `tenant` claim**; a tenant aggregate and a membership join let one user belong to many tenants. Shared infrastructure (LLM, embedding, store, search-engine, MCP, integration instances) is **tenant-owned (bring-your-own keys) with an optional shared pool**.
:::

---

## The off-means-legacy guarantee

Everything hides behind **`turing.tenancy.enabled`**, which defaults to `false`.

When the flag is **off**:

- Every request resolves the immutable `DEFAULT` tenant.
- The discriminator becomes a constant equality (`tenantId = 'DEFAULT'`).
- A single-tenant install behaves byte-for-byte as it always has.
- No existing content, search core, secret, or asset needs migration.

This means you can upgrade to a multi-tenancy-capable build with **zero changes** to an existing deployment, and flip the flag on only when you actually need it.

```yaml
turing:
  tenancy:
    enabled: false   # default: single-tenant. Set true for SaaS / shared platform.
```

---

## How isolation works

The `@TenantId` discriminator column handles the bulk of isolation automatically: once a JPA entity carries it, Hibernate stamps the current tenant on writes and filters reads: application code never writes a tenant `WHERE` clause by hand.

The harder problem is every path that **bypasses the ORM**. Turing closes each one with a dedicated, tenant-aware component:

| Surface | How it's isolated |
|---|---|
| **JPA reads/writes** | `@TenantId` discriminator column on every tenant-owned entity |
| **Distributed cache** (Hazelcast `@Cacheable`) | Cache keys are prefixed with the tenant id |
| **Search-engine cores** (Solr / Elasticsearch) | Name-keyed cores get a `t<shortId>_` prefix |
| **Object storage** (MinIO / filesystem) | Objects live under a `tenants/<id>/` prefix, with path-traversal guards |
| **Lucene vector store** | Vector collections resolve under a per-tenant base path |
| **Secret encryption** | The AES key is derived per tenant: `PBKDF2(master, tenantId)` |
| **Message queue** (Artemis JMS) | Indexing/routine messages carry a tenant header that listeners restore |
| **Scheduled jobs** | Tenant-touching sweeps fan out, running once per active tenant |
| **Reactive / async hops** | The tenant context propagates across thread and reactor boundaries |

:::note[UUID-keyed cores don't need a prefix]
RAG and intent cores are keyed by globally-unique UUIDs, so they can't collide across tenants. Only cores keyed by a human-chosen name (such as a semantic-navigation site name) are prefixed.
:::

A continuous-integration **isolation suite** (`*Tenant*` tests) is the ship gate: it proves tenant B can't read tenant A's sites, BYO-infrastructure visibility is `current ∪ shared`, and secret crypto is end-to-end tenant-scoped before any SaaS endpoint goes live.

---

## Tenants & memberships

A **tenant** is the unit of isolation: it has a unique slug, a status (`ACTIVE` / `SUSPENDED`), and a plan. A **membership** joins a user to a tenant with a role:

| Role | Capabilities |
|---|---|
| `OWNER` | Full control of the tenant; created automatically on signup |
| `ADMIN` | Manage the tenant's content and members |
| `MEMBER` | Use the tenant's content |

One user can belong to **many tenants** and switch between them. The `DEFAULT` tenant is immutable and always present: it is the home of all data in single-tenant mode.

---

## Identity & access

Multi-tenancy uses a **single Keycloak realm**. The tenant a request belongs to is resolved per request, in priority order:

1. The `tenant` claim on the JWT
2. The session attribute (set when a user switches tenant)
3. The request subdomain (e.g. `acme.turing.example.com`)
4. The `X-Turing-Tenant` request header

A request for a **suspended** tenant, or from a user with **no active membership**, is rejected with **HTTP 403**. (Platform admins bypass this, see below.)

On successful resolution, Turing grants per-membership authorities of the form `TENANT_<id>` and `TENANT_<id>_<ROLE>`, in both the OIDC and the session-login paths.

### Tenant management endpoints

| Endpoint | Purpose |
|---|---|
| `POST /api/signup` | Self-service: creates a tenant + an `OWNER` membership. Idempotent, slug-validated, reserved words rejected. |
| `GET /api/tenants/mine` | List the tenants the current user belongs to. |
| `POST /api/tenants/{slug}/switch` | Switch the current session's active tenant. |
| `GET/POST /api/platform/tenants/**` | Platform-admin only: list, suspend, activate, impersonate. |

### Platform administrators

`ROLE_PLATFORM_ADMIN` is the **only sanctioned way to cross tenant boundaries**. Cross-tenant operations go through an audited platform-admin service (`runForTenant` / `runAsSystem`); every such access is logged. Application code never reads another tenant's data directly.

---

## Tenant-owned infrastructure (bring-your-own keys)

LLM instances, embedding models, embedding stores, search-engine instances, MCP servers, and integrations are **tenant-owned**. Each of these carries an optional tenant id:

- A row scoped to a tenant id is **private** to that tenant (its own API keys, its own endpoints).
- A row with **no** tenant id is a **shared / platform-provided global** instance, visible to every tenant.

Repository queries return `current tenant ∪ shared` (`tenantId = :t OR tenantId IS NULL`). Only a platform admin may create a `null`-tenant global instance. This lets a SaaS operator offer a shared default model *and* let each tenant plug in their own provider keys.

---

## Plans & quotas

A tenant's **plan** maps to limits enforced at runtime:

- The `FREE` plan is **bounded** (capped resources / spend).
- Paid and custom plans are **unlimited**.

Enforcement points:

| Limit hit at | Response |
|---|---|
| Creating an AI Agent or a Semantic Navigation site over quota | **HTTP 402** Payment Required |
| A chat or completion over the monthly token budget | **HTTP 429** Too Many Requests |

The `FREE` plan allows **3 AI Agents, 2 Semantic Navigation sites and 100,000 tokens per calendar month**. Only the token budget resets on its own; a resource count frees up when you delete an agent or a site, or move to a bigger plan.

Embeddings are deliberately **not** metered against the token budget, so an exhausted chat credit never turns into a search outage or a half-indexed corpus.

### What you see when a limit is reached

The refusal is not a bare error. Its body names the resource, how much of the limit is used, and when the window resets, and the console turns that into a banner offering two ways forward: **continue on a paid plan**, or **use your own API key**. Bringing your own key is never hidden or presented as a downgrade, it is free and fully featured (see [Viglet AI](/docs/llm-instances/#viglet-ai-start-without-a-key-of-your-own)).

### Importing when a limit is close

The limit applies to bulk creation too. Importing an archive (or installing a marketplace app) is refused when what it would **add** crosses your plan, and it is refused **whole**: nothing is written, rather than importing up to the cap and stopping halfway.

- Entries that match something you already have are **updates, not additions**. An archive that refreshes three agents you own is not refused on a three-agent plan.
- The refusal says how much the archive would have added, so you know how much to trim.
- The first-boot seed is **exempt**: a fresh installation provisioning itself is never refused, because it runs before any tenant exists.

The paid-plan link appears only when this install can produce a signed one; an install that cannot simply gets the banner without it, rather than a link that goes nowhere.

Per-tenant **cost attribution** rolls up token usage by tenant, feeding the live AI-spend dashboard (see [Token Usage](/docs/token-usage/)).

---

## Tenant lifecycle & teardown

Tenant teardown is platform-admin only, **audited**, and **idempotent**:

- **Suspend**: flips status to `SUSPENDED`. The resolution filter then blocks the tenant's members (403) without destroying any data, so suspension is fully reversible (**Activate** flips it back).
- **Delete**: supports a `dryRun` mode that returns the deletion *plan* without mutating anything. A real delete purges the storage prefix, removes the Lucene vector directory, evicts caches, and removes the membership + tenant rows. The tenant's `@TenantId` content becomes **unreachable immediately**, because the resolver never again yields a deleted tenant id.

:::caution[Engine-side cleanup is a runbook step]
A physical database row sweep, dropping external Solr/Elasticsearch cores, and Keycloak attribute cleanup require the live engines and are deployment-specific operational steps that run alongside the in-app delete.
:::

---

## Adding a tenant-safe entity (for developers)

If you extend Turing with a new persistent entity, decide which kind it is:

1. **Tenant-owned content** (a tenant's own data, like a site or an agent): add a `@TenantId` discriminator column. Add it to the schema as nullable with a `DEFAULT` default value, backfill existing NULLs to `DEFAULT`, then flip it `NOT NULL` in a follow-up migration. Hibernate handles stamping and filtering from there.

2. **Shared infrastructure** (BYO keys with a global pool): add a **plain** nullable tenant id (not `@TenantId`) and a `findVisibleToTenant` query that returns `tenantId = :t OR tenantId IS NULL`. Gate creation of `null`-tenant globals on `ROLE_PLATFORM_ADMIN`.

3. **Out-of-band resource** (a new cache, core, storage path, or queue): prefix it with the current tenant using the matching component, and add a negative assertion to the isolation suite.

4. **Never** read across tenants except through the audited platform-admin service.

---

## Escalation seam (shared → schema → database)

Every tenant carries an `isolationMode` of `SHARED`, `SCHEMA`, or `DB` (default `SHARED`). This reserves a future migration path to move a heavy enterprise tenant onto a dedicated schema or database (via a pluggable Hibernate tenant connection provider) without changing the data model. **Only `SHARED` is implemented today**; the field exists so the escalation path requires no schema rewrite later.

---

## Related Pages

- [Configuration Reference](/docs/configuration-reference/): the `turing.tenancy.enabled` flag.
- [Security & Keycloak](/docs/security-keycloak/): the single realm and the `tenant` claim.
- [Token Usage](/docs/token-usage/): per-tenant cost attribution and the AI-spend dashboard.

