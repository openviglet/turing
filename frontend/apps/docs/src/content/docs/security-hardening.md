---
title: "Security Hardening & Upgrade Notes"
description: "Post-audit security hardening in Viglet Turing, the new production defaults, required environment variables, abuse controls, and breaking changes operators and integrators need to know."
---
A full-surface security review of Viglet Turing produced a round of
hardening changes to the production defaults, the anonymous public surface, and
the credential/secret handling. This page is the operator- and
integrator-facing summary: **what changed, what you must now configure, and
which changes can break an existing deployment if you don't act.**

:::caution[Breaking changes: read before upgrading]
Four changes can break an existing deployment or integration if you don't act:

1. **`TURING_AI_CRYPTO_KEY` is now required in production**: the app fails to
   start on the `production` profile with a blank or known-sample key.
2. **`turing.permissions` now defaults to `true`**: an authenticated principal
   is no longer automatically an admin.
3. **`/api/system/chat-analytics/**` now requires authentication**, a Grafana /
   dashboard datasource pointed at it must send credentials.
4. **The `?apiKey=` query-parameter for API keys was removed**: clients must
   send the key in the `Key` HTTP header.

Each is detailed below.
:::

---

## 1. AI crypto master key must be set in production

Turing encrypts every stored provider API key and object-store credential with
an AES/GCM master key read from `turing.ai.crypto.key`
(env var **`TURING_AI_CRYPTO_KEY`**).

- Under the **`production`** profile the app **fails fast at startup** if the key
  is blank **or** set to a known sample/dev value (e.g. the old
  `sample-key-for-crypto`). This prevents shipping with a guessable key that
  would let anyone with repo + database access decrypt all your provider keys.
- Outside production, a warned-once insecure development fallback is used, so
  local development needs no key.

**What to do:** set a strong, secret value in the environment:

```bash
export TURING_AI_CRYPTO_KEY="$(openssl rand -base64 48)"
```

**Rotation.** If you previously ran with the committed sample key, treat every
secret encrypted under it as compromised: reissue the provider credentials at
the source, set a fresh `TURING_AI_CRYPTO_KEY`, and re-enter the secrets through
the admin UI. All instances that share a database must share the same master
key.

---

## 2. `turing.permissions` defaults to `true` (authn ≠ admin)

Previously the shipped default was `turing.permissions: false`, which granted
**`ROLE_ADMIN` and every privilege to any authenticated principal**, making all
method-level authorization checks inert. The default is now **`true`**: users
get only their real, group-derived authorities.

- The seeded `admin` user keeps full rights (it belongs to the `Administrator`
  group, which carries `ROLE_ADMIN`).
- A plain authenticated user is **no longer** an admin.

**What to do:** if your deployment relied on the old "everyone who logs in is an
admin" behavior (e.g. a trusted single-user install), set it back explicitly:

```yaml
turing:
  permissions: false
```

When you do, a `SECURITY` warning is logged at startup so the choice is visible.
Otherwise, assign users to groups/roles so they have the privileges they need.

---

## 3. Chat-analytics API requires authentication

`GET /api/system/chat-analytics/**` (session enumeration, transcripts, router
decisions, tool-call traces) is **no longer anonymous**: it exposed end-user
chat content. It now requires an authenticated admin (`ROLE_ADMIN` /
`AI_AGENT_VIEW`).

**What to do:** if you scrape it from Grafana via the Infinity datasource,
configure HTTP Basic auth (a Turing admin account) on that datasource. The
shipped Grafana provisioning (`containers/grafana/.../prometheus.yml`) has been
updated to use Basic auth with placeholder credentials: replace them with a
real admin account. See [Chat Analytics](/docs/chat-analytics/).

---

## 4. Developer tokens: header-only, with expiry & revocation

[Developer / API-key tokens](/docs/security-authentication/#rest-api-api-key)
were hardened:

- **The `?apiKey=` query-parameter is gone.** A secret in the URL leaks into
  access logs, `Referer` headers and browser history. Send the key in the
  **`Key`** HTTP header instead:

  ```bash
  curl -H "Key: <your-token>" https://your-host/api/sn/mysite/search?q=hello
  ```

  This affects the public SN endpoints (search / autocomplete / chat /
  spell-check) on API-key-mode sites and the MCP server key path. The SDKs
  already use the header.

- **Tokens now support expiry and revocation.** A token carries an `enabled`
  flag (soft revoke without deleting the row) and an optional `expiresAt`; a
  disabled or expired token is rejected. Existing tokens default to
  enabled / never-expire.

- The admin token API is now admin-only, and token values are shown **once on
  creation**: they are never returned again by list/get.

Additionally, the admin user list no longer returns password hashes
(`password` is write-only).

---

## 5. Abuse controls on the anonymous chat/search surface

The public SN chat/search path is anonymous by design. New in-process controls
(prefix `turing.abuse.chat.*`, all **on by default** except the hard cost cap)
protect it without relying solely on a reverse proxy. See the
[Configuration Reference](/docs/configuration-reference/#abuse-controls-turingabuse).

| Property | Default | Effect |
|---|---|---|
| `rate-limit-enabled` | `true` | Per-IP + per-session fixed-window rate limit on the LLM-cost chat surface (`/chat`, persona content-fit). Over the limit → HTTP 429. |
| `requests-per-minute-per-ip` | `60` | IP-dimension limit. |
| `requests-per-minute-per-session` | `30` | Session/conversation-dimension limit. |
| `hard-monthly-cap-usd` | `0` (off) | When `> 0`, a hard month-to-date LLM-spend ceiling: once reached, new anonymous conversations are refused with a friendly message and **no** upstream call fires. |
| `anonymous-tools-enabled` | `true` | When `false`, strips **all** tool callbacks for anonymous SN chat, so an anonymous visitor (or an injection payload in retrieved content) cannot trigger tool execution. |
| `max-upload-bytes` | `10 MB` | Per-file cap on anonymous slot uploads, enforced before text extraction / vision. |
| `max-messages-per-turn` | `100` | Max messages accepted in one anonymous chat turn. |
| `max-message-chars` | `24000` | Max characters per anonymous message. |
| `max-zip-entries` / `max-zip-entry-bytes` / `max-zip-total-bytes` | `5000` / `50 MB` / `250 MB` | Zip-bomb guards on page/skill ZIP import. |

Autocomplete and plain search are intentionally **not** app-rate-limited (they
are cheap and fire per keystroke); shed them at the reverse proxy instead. The
shipped `containers/nginx/conf.d/app.conf` now includes `limit_req` / `limit_conn`
zones (a tight chat zone + a looser search zone) as edge defense-in-depth.

The global multipart limit was also lowered from `1024MB` to `64MB`
(`spring.servlet.multipart.max-*-size`); raise it if you import very large
page/skill ZIPs.

---

## 6. Retrieved content is treated as untrusted (prompt-injection defense)

In RAG chat, retrieved website content is now wrapped in an explicit
`<untrusted_website_content>` block with a standing instruction that everything
inside is **data, not instructions**, so a crawled page or uploaded asset
carrying "ignore previous instructions / call tool X" can no longer hijack the
system prompt on the next query. No configuration is needed; this is always on.

Pair it with `turing.abuse.chat.anonymous-tools-enabled=false` (section 5) if
your public agent exposes write / MCP-client / custom tools and you want a hard
boundary between an anonymous visitor and tool execution.

---

## 7. Outbound-fetch SSRF guard

A single hardened egress validator now vets outbound URLs (blocking loopback /
link-local / private-IPv4 / **IPv6 ULA** / cloud-metadata targets). It protects
the connector **test-connection** probe and the integration **federation proxy**
(which also no longer follows redirects server-side). Admin-configured LLM /
MCP-client endpoints on an internal network are allowed by default; see the MCP
client guard below to lock them down.

---

## 8. MCP client guards

For the agent-as-MCP-**client** path (`turing.mcp-client.*`), see
[MCP Servers](/docs/mcp-servers/):

| Property | Default | Effect |
|---|---|---|
| `allowed-stdio-commands` | *(empty = any)* | When set, a stdio MCP server whose command base-name isn't listed is refused, blocks arbitrary local-process execution by configuration. |
| `block-private-urls` | `false` | When `true`, an HTTP MCP-client URL that resolves to a private/loopback address is refused (via the SSRF guard). Leave `false` if your MCP servers live on an internal network. |

The inbound `/mcp` server is off by default and, when enabled, restricted to the
loopback interface unless you enable OAuth (`require-auth`). Behind a same-host
reverse proxy, pair `loopback-only` with `require-auth`.

---

## 9. Actuator endpoints restricted

`management.endpoints.web.exposure.include` is now `health,info,prometheus`
(was `*`). `/actuator/env` and `/actuator/heapdump` (which could leak the
crypto key, datasource credentials and provider keys) are **no longer exposed
over HTTP**. Re-add a specific endpoint only if you need it (it will require
authentication).

---

## 10. Object-store credentials & H2 console

- **MinIO credentials are externalized** to `TURING_STORAGE_MINIO_ACCESS_KEY` /
  `TURING_STORAGE_MINIO_SECRET_KEY` (dev defaults are MinIO's own placeholders).
  Set them for any real MinIO deployment. Only relevant when
  `turing.storage.type=MINIO` (default is `FILESYSTEM`). See [Assets & Storage](/docs/assets/).
- **The H2 console stays off by default** and, if enabled for local debugging,
  now answers the loopback interface only (`web-allow-others: false`) and goes
  through the normal security chain. Never enable it in production.

---

## 11. Dependency-vulnerability CI gate

A `pnpm audit --audit-level high` gate runs on frontend dependency changes and
weekly, so a new high-severity advisory fails the build. Backend dependencies
are covered by weekly Dependabot updates.

---

## Configuration quick-reference

All new/changed properties in one place (see the full
[Configuration Reference](/docs/configuration-reference/)):

```yaml
turing:
  permissions: true                 # authn is NOT admin (was false)
  ai:
    crypto:
      key: ${TURING_AI_CRYPTO_KEY:}  # required in production
  abuse:
    chat:
      rate-limit-enabled: true
      requests-per-minute-per-ip: 60
      requests-per-minute-per-session: 30
      hard-monthly-cap-usd: 0        # >0 to enforce a hard anonymous spend ceiling
      anonymous-tools-enabled: true  # false = no tools for anonymous chat
      max-upload-bytes: 10485760
      max-messages-per-turn: 100
      max-message-chars: 24000
      max-zip-entries: 5000
      max-zip-entry-bytes: 52428800
      max-zip-total-bytes: 262144000
  mcp-client:
    block-private-urls: false
    allowed-stdio-commands: []       # e.g. [npx, uvx] to lock down stdio MCP
  storage:
    minio:
      access-key: ${TURING_STORAGE_MINIO_ACCESS_KEY:minioadmin}
      secret-key: ${TURING_STORAGE_MINIO_SECRET_KEY:minioadmin}

management:
  endpoints:
    web:
      exposure:
        include: "health,info,prometheus"
```

