---
title: "Configuration Reference"
description: "Complete reference for the Viglet Turing application.yaml configuration file."
---
This page documents every significant property in the Viglet Turing `application.yaml` file. The file uses standard Spring Boot configuration: any property can be overridden via environment variables, JVM system properties (`-Dkey=value`), or a separate `application-production.yaml` file in the working directory.

:::tip[Override pattern]
To override a property at runtime without editing the file, use the environment variable convention: replace `.` and `-` with `_` and uppercase everything. For example, `turing.storage.type` becomes `TURING_STORAGE_TYPE=minio`.
:::

---

## Full Default Configuration

```yaml
spring:
  datasource.hikari:
    maximum-pool-size: 20
    minimum-idle: 5
    idle-timeout: 600000
    pool-name: TurConnectionPool
  jmx.enabled: true
  profiles:
    active: production
  h2:
    console:
      enabled: false          # keep off in production
      path: /h2
      settings:
        web-allow-others: false   # loopback-only if ever enabled
  datasource:
    url: jdbc:h2:file:./store/db/turingDB;DATABASE_TO_UPPER=false;CASE_INSENSITIVE_IDENTIFIERS=true
    username: sa
    password: ""
    driver-class-name: org.h2.Driver
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml
    enabled: true
    default-schema: PUBLIC
  jpa:
    properties:
      hibernate:
        "[globally_quoted_identifiers]": true
        "[format_sql]": false
        "[enable_lazy_load_no_trans]": true
        "[generate_statistics]": false
      "[jakarta.persistence.sharedCache.mode]": ALL
    hibernate:
      ddl-auto: none
    show-sql: false
  jackson:
    mapper:
      "[DEFAULT_VIEW_INCLUSION]": true
  thymeleaf:
    mode: HTML
    check-template: true
    check-template-location: true
    prefix: classpath:public/
    suffix: .html
  artemis:
    mode: embedded
    broker-url: localhost:61616
    embedded:
      enabled: true
      persistent: true
      data-directory: store/queue
      queues: indexing.queue
    pool:
      max-connections: 10
  jms:
    template:
      default-destination: indexing.queue
  servlet:
    multipart:
      max-file-size: 64MB
      max-request-size: 64MB
  mvc:
    async:
      request-timeout: 3600000
  output:
    ansi:
      enabled: always
  graphql:
    graphiql:
      enabled: true
      path: /graphiql
    http.path: /graphql
    cors:
      allowed-origins: ${turing.allowedOrigins}
      allowed-methods: GET,POST
      allowed-headers: "*"

server:
  port: ${PORT:2700}
  compression:
    enabled: true
    mime-types: application/json,text/css,application/javascript
    min-response-size: 2048
  tomcat:
    accesslog:
      enabled: false
      suffix: .log
      prefix: access_log
      file-date-format: .yyyy-MM-dd
      directory: logs
    basedir: store
    threads:
      max: 600
    max-swallow-size: 200MB
    max-http-form-post-size: 200MB
    maxHttpResponseHeaderSize: 800KB

turing:
  ai.crypto.key: ${TURING_AI_CRYPTO_KEY:}   # required in production (fail-fast on blank/sample)
  allowedOrigins: http://localhost:4200,http://localhost:5173
  permissions: true                          # authn is NOT admin; false = trusted single-user mode
  multi-tenant: false
  keycloak: false
  url: http://localhost:2700
  open-browser: true
  jms.concurrency: 1-1
  code-interpreter:
    python-executable: ""
  model-catalog:                               # public LLM catalog powering the model picker
    url: ${TURING_MODEL_CATALOG_URL:https://openviglet.github.io/model-catalog/catalog.json}
    remote-enabled: true                       # remote-only; false = live vendor listing only
    price-sync:
      enabled: true                            # seed tur_llm_price from indicative pricing
    change-feed:
      email-enabled: false                     # opt-in daily change-feed digest email
  solr:
    timeout: 30000
    cloud: false
    commit:
      within: 10000
      enabled: false
  elasticsearch:
    timeout: 30000
  search:
    metrics.enabled: true
    cache:
      ttl:
        seconds: 86400000
      enabled: false
  storage:
    type: none
    filesystem:
      path: ./store/assets
    minio:
      endpoint: ${TURING_STORAGE_MINIO_ENDPOINT:http://localhost:9000}
      access-key: ${TURING_STORAGE_MINIO_ACCESS_KEY:minioadmin}   # set via env for real deployments
      secret-key: ${TURING_STORAGE_MINIO_SECRET_KEY:minioadmin}
      bucket: ${TURING_STORAGE_MINIO_BUCKET:turing-assets}
  mongodb:
    enabled: false
    uri: mongodb://localhost:27017
    logging:
      database: turingLog
      collection:
        server: server
        aem: aem
        indexing: indexing
      purge:
        days: 30

springdoc:
  pathsToMatch: /api/**
  swagger-ui:
    path: /swagger-ui.html

management:
  endpoints:
    web:
      exposure:
        include: "health,info,prometheus"   # env/heapdump/etc. not exposed over HTTP

logging:
  config: classpath:logback-spring.xml
  level:
    org:
      springframework: INFO
      apache: INFO
      "[apache.activemq]": ERROR
    com:
      viglet: INFO
    dev:
      langchain4j: INFO
      ai4j:
        openai4j: INFO
  file:
    name: store/logs/turing.log
  logback:
    rollingpolicy:
      max-file-size: 25MB
      max-history: 10
```

---

## Property Reference

### Server

| Property | Default | Description |
|---|---|---|
| `server.port` | `${PORT:2700}` | HTTP port. Override with the `PORT` environment variable. |
| `server.compression.enabled` | `true` | Enables gzip compression for JSON, CSS, and JavaScript responses |
| `server.compression.min-response-size` | `2048` | Minimum response size in bytes before compression is applied |
| `server.tomcat.threads.max` | `600` | Maximum number of Tomcat worker threads |
| `server.tomcat.basedir` | `store` | Base directory for Tomcat internal files |
| `server.tomcat.max-swallow-size` | `200MB` | Maximum size of request body Tomcat will absorb |
| `server.tomcat.max-http-form-post-size` | `200MB` | Maximum size of HTML form POST bodies |
| `server.tomcat.maxHttpResponseHeaderSize` | `800KB` | Maximum size of HTTP response headers |
| `server.tomcat.accesslog.enabled` | `false` | Enable Tomcat access log file |

---

### Database (Spring Datasource)

The default database is **H2** (embedded, file-based): suitable for development and simple deployments. For production, replace with MariaDB, MySQL, or PostgreSQL.

| Property | Default | Description |
|---|---|---|
| `spring.datasource.url` | `jdbc:h2:file:./store/db/turingDB` | JDBC URL. Change to your production DB URL. |
| `spring.datasource.username` | `sa` | Database username |
| `spring.datasource.password` | `""` | Database password |
| `spring.datasource.driver-class-name` | `org.h2.Driver` | JDBC driver class |
| `spring.datasource.hikari.maximum-pool-size` | `20` | Maximum connections in the HikariCP pool |
| `spring.datasource.hikari.minimum-idle` | `5` | Minimum idle connections kept alive |
| `spring.datasource.hikari.idle-timeout` | `600000` | Time (ms) a connection can remain idle before being removed |
| `spring.h2.console.enabled` | `false` | Enable H2 web console: **do not enable in production** |

**Switching to MariaDB / MySQL:**

```yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/turingdb
    username: turing
    password: your_password
    driver-class-name: org.mariadb.jdbc.Driver
```

---

### Message Queue (Apache Artemis)

Apache Artemis runs **embedded inside Turing** by default: no separate installation is required. For high-availability or multi-node deployments, you can switch to an external Artemis broker.

| Property | Default | Description |
|---|---|---|
| `spring.artemis.mode` | `embedded` | `embedded` (default) or `native` (external broker) |
| `spring.artemis.broker-url` | `localhost:61616` | Used when `mode: native` to point to an external broker |
| `spring.artemis.embedded.enabled` | `true` | Enables the embedded broker |
| `spring.artemis.embedded.persistent` | `true` | Persist queue messages to disk (survives restarts) |
| `spring.artemis.embedded.data-directory` | `store/queue` | Directory for persisted queue data |
| `spring.artemis.embedded.queues` | `indexing.queue` | Queue names to create on startup |
| `spring.artemis.pool.max-connections` | `10` | Maximum JMS connection pool size |
| `spring.jms.template.default-destination` | `indexing.queue` | Default JMS destination for indexing jobs |
| `turing.jms.concurrency` | `1-1` | JMS listener concurrency range (min-max consumers). Increase for higher indexing throughput. |

:::tip[External Artemis for scalability]
To use an external Artemis broker (e.g., for multi-node deployments), set `spring.artemis.mode: native` and point `broker-url` to your broker. The embedded broker should be disabled in that case (`spring.artemis.embedded.enabled: false`).
:::

---

### File Uploads

| Property | Default | Description |
|---|---|---|
| `spring.servlet.multipart.max-file-size` | `64MB` | Maximum size of a single uploaded file (was 1024MB; anonymous slot uploads are additionally capped by `turing.abuse.chat.max-upload-bytes`) |
| `spring.servlet.multipart.max-request-size` | `64MB` | Maximum total size of a multipart request |

---

### GraphQL

| Property | Default | Description |
|---|---|---|
| `spring.graphql.http.path` | `/graphql` | GraphQL endpoint path |
| `spring.graphql.graphiql.enabled` | `true` | Enable GraphiQL browser IDE |
| `spring.graphql.graphiql.path` | `/graphiql` | GraphiQL IDE path |
| `spring.graphql.cors.allowed-origins` | `${turing.allowedOrigins}` | CORS allowed origins for GraphQL |

---

### Async / Timeout

| Property | Default | Description |
|---|---|---|
| `spring.mvc.async.request-timeout` | `3600000` | Maximum time (ms) for async requests: 1 hour. Covers streaming LLM responses. |

---

<a id="turing-es-core"></a>

### Turing Core

| Property | Default | Description |
|---|---|---|
| `turing.url` | `http://localhost:2700` | Public base URL of this Turing instance |
| `turing.allowedOrigins` | `http://localhost:4200, http://localhost:5173` | CORS allowed origins for the REST API and GraphQL. Add your frontend URL here. |
| `turing.keycloak` | `false` | Set `true` to enable Keycloak OAuth2/OIDC. See [Security & Keycloak](/docs/security-keycloak/). |
| `turing.tenancy.enabled` | `false` | Enable multi-tenant mode (one JVM, many isolated tenants). Default `false` = single-tenant, byte-for-byte legacy behavior. See [Multi-Tenancy](/docs/multi-tenancy/). |
| `turing.open-browser` | `true` | Automatically open the admin console in the browser on startup |
| `turing.permissions` | `true` | When `true`, users get only their real group/role authorities. When `false`, **any** authenticated principal is granted `ROLE_ADMIN` + all privileges (trusted single-user mode only, logs a `SECURITY` warning at startup). See [Security Hardening](/docs/security-hardening/#2-turingpermissions-defaults-to-true-authn--admin). |
| `turing.admin.password` | *(blank → env `TURING_ADMIN_PASSWORD`)* | Seeds the local `admin` password at startup (minimum 6 characters) so a container/CI deploy skips the first-access setup screen. Applied only while the password is still unset: it never overwrites an existing one. See [Login](/docs/administration-guide/#login). |
| `turing.ai.crypto.key` | *(blank → env `TURING_AI_CRYPTO_KEY`)* | AES/GCM key encrypting all stored provider credentials. **Required in production**, the app fails to start on the `production` profile with a blank or known-sample value. |
| `turing.code-interpreter.python-executable` | *(auto-detected)* | Absolute path to the Python 3 binary used by the Code Interpreter GenAI tool. When blank, Turing searches standard OS locations automatically. |

:::caution[Set the crypto key in production]
`turing.ai.crypto.key` encrypts every LLM provider API key + store credential in the database. Supply it via the `TURING_AI_CRYPTO_KEY` environment variable (`openssl rand -base64 48`). Under the `production` profile a blank or known-sample key makes startup **fail fast**. See [Security Hardening § crypto key](/docs/security-hardening/#1-ai-crypto-master-key-must-be-set-in-production).
:::

<a id="model-catalog"></a>

### Model Catalog (`turing.model-catalog`)

Turing enriches its LLM-instance model picker from a public, vendor-neutral [**model catalog**](https://openviglet.github.io/model-catalog/) ([openviglet/model-catalog](https://github.com/openviglet/model-catalog)), pricing, context windows, capabilities, tiers and deprecations. The fetch is **remote-only** (no bundled copy), best-effort, runs off the boot path on a TTL, and never blocks startup; until the first successful fetch the picker falls back to live vendor listing only. See [LLM Instances § Catalog-driven intelligence](/docs/llm-instances/#catalog-driven-intelligence).

| Property | Default | Description |
|---|---|---|
| `turing.model-catalog.url` | `https://openviglet.github.io/model-catalog/catalog.json` | Catalog endpoint the model picker reads. Override with `TURING_MODEL_CATALOG_URL` to pin a self-hosted or air-gapped copy. |
| `turing.model-catalog.remote-enabled` | `true` | Set `false` to disable the remote fetch entirely, the picker then uses only models listed live by each vendor's API. |
| `turing.model-catalog.refresh-interval-ms` | `21600000` | Refresh cadence for the catalog (6h). A short initial delay (`refresh-initial-delay-ms`, default `8000`) keeps the fetch off the boot path. |
| `turing.model-catalog.price-sync.enabled` | `true` | Auto-seed/refresh the per-model price table (`tur_llm_price`) from the catalog's indicative pricing on a TTL. Only CATALOG-sourced rows are written, an operator's MANUAL rate is never overwritten. `false` keeps the price table manual-only. See [Cost Governance](/docs/cost-governance/). |
| `turing.model-catalog.change-feed.email-enabled` | `false` | Opt-in: a daily job emails the Global Settings recipient a digest when the catalog change feed affects a model you use (removed / superseded / changed), deduped. The in-UI change-feed banner is always on regardless. |

<a id="abuse-controls-turingabuse"></a>

### Abuse Controls (`turing.abuse`)

In-process protection for the anonymous public chat/search surface (all on by default except the hard cost cap). See [Security Hardening § abuse controls](/docs/security-hardening/#5-abuse-controls-on-the-anonymous-chatsearch-surface).

| Property | Default | Description |
|---|---|---|
| `turing.abuse.chat.rate-limit-enabled` | `true` | Per-IP + per-session fixed-window rate limit on the LLM-cost chat surface. Over the limit → HTTP 429 + `Retry-After`. |
| `turing.abuse.chat.requests-per-minute-per-ip` | `60` | IP-dimension request limit. `≤0` disables it. |
| `turing.abuse.chat.requests-per-minute-per-session` | `30` | Session/conversation-dimension limit. `≤0` disables it. |
| `turing.abuse.chat.hard-monthly-cap-usd` | `0` | `>0` enforces a hard month-to-date LLM-spend ceiling on anonymous chat (refuses new conversations, no upstream call). |
| `turing.abuse.chat.anonymous-tools-enabled` | `true` | `false` strips all tool callbacks for anonymous SN chat (visitors can't trigger tool execution). |
| `turing.abuse.chat.max-upload-bytes` | `10485760` | Per-file cap on anonymous slot uploads, enforced before extraction/vision. |
| `turing.abuse.chat.max-messages-per-turn` | `100` | Max messages per anonymous chat turn. |
| `turing.abuse.chat.max-message-chars` | `24000` | Max characters per anonymous message. |
| `turing.abuse.chat.max-zip-entries` | `5000` | Max entries in a page/skill import ZIP. |
| `turing.abuse.chat.max-zip-entry-bytes` | `52428800` | Max uncompressed size of one ZIP entry (zip-bomb guard). |
| `turing.abuse.chat.max-zip-total-bytes` | `262144000` | Max total uncompressed ZIP size. |

<a id="mcp-client-guards"></a>

### MCP Client Guards (`turing.mcp-client`)

Guardrails for the agent-as-MCP-client path. See [MCP Servers](/docs/mcp-servers/) and [Security Hardening § MCP](/docs/security-hardening/#8-mcp-client-guards).

| Property | Default | Description |
|---|---|---|
| `turing.mcp-client.allowed-stdio-commands` | *(empty = any)* | Allowlist of permitted stdio command base-names (e.g. `[npx, uvx]`). When set, other commands are refused, blocks arbitrary local-process execution by config. |
| `turing.mcp-client.block-private-urls` | `false` | When `true`, an HTTP MCP-client URL resolving to a private/loopback address is refused via the SSRF guard. Leave `false` for internal-network MCP servers. |

---

### Solr

| Property | Default | Description |
|---|---|---|
| `turing.solr.timeout` | `30000` | Connection and read timeout (ms) for Solr requests |
| `turing.solr.cloud` | `false` | Set `true` for SolrCloud mode with Zookeeper |
| `turing.solr.commit.within` | `10000` | Soft commit time (ms): Solr commits changes within this window |
| `turing.solr.commit.enabled` | `false` | Enable explicit commits after indexing. Leave `false` to use Solr's auto-commit. |

---

### Elasticsearch

| Property | Default | Description |
|---|---|---|
| `turing.elasticsearch.timeout` | `30000` | Connection and read timeout (ms) for Elasticsearch requests |

---

<a id="content-ai"></a>

### Adobe AEM Content AI (`turing.content-ai`)

Settings for a Search Engine instance whose vendor is `CONTENTAI`, a **federated,
read-only** backend that queries an Adobe AEM Content AI index instead of one Turing filled. See [Search Engine → Adobe AEM Content AI](/docs/search-engine/#content-ai)
for what the backend can and cannot do.

The **endpoint URL is not here**: it is the Search Engine instance's own Endpoint
URL, so each instance can point at a different Adobe bucket.

| Property | Default | Description |
|---|---|---|
| `turing.content-ai.enabled` | `false` | Master switch. With `false` the backend refuses every query and reports `DOWN` in System Information |
| `turing.content-ai.token` | *(none)* | Bearer token from the Adobe Developer Console ("AEM Content AI" card) |
| `turing.content-ai.api-key` | *(none)* | `x-api-key` credential, the alternative the search endpoints accept. Used only when `token` is blank |
| `turing.content-ai.source-type` | `ACQUISITION` | Type of the Adobe content source: `ACQUISITION`, `AEM_AUTHOR`, `AEM_PUBLISH` or `CUSTOM` |
| `turing.content-ai.sources.<site>` | *(none)* | Adobe content source name for one SN Site. Unmapped sites fall back to the site's core name |
| `turing.content-ai.hybrid` | `true` | Query semantically **and** lexically (Adobe's own hybrid shape). `false` sends the keyword query alone |
| `turing.content-ai.vector-boost` | `1.0` | Relevance weight of the semantic half |
| `turing.content-ai.fulltext-boost` | `1.0` | Relevance weight of the keyword half |
| `turing.content-ai.vector-space` | *(none)* | Vector space name from the Adobe source's IndexConfig. Blank uses Adobe's default |
| `turing.content-ai.lexical-space` | *(none)* | Lexical space name. Blank uses Adobe's default |
| `turing.content-ai.facet-size` | `10` | Facet values requested per field |
| `turing.content-ai.max-page-walk` | `10` | How deep a result page may be reached. Adobe pages by cursor with no offset, so page *N* costs *N* requests; past this the page comes back empty with the total intact |
| `turing.content-ai.timeout-seconds` | `10` | Per-request timeout |
| `turing.content-ai.source-cache-seconds` | `300` | How long the list of your Adobe content sources is reused. It is read to check your configuration (see below), not to serve searches |
| `turing.content-ai.include-chunks` | `false` | Ask Adobe for the matched passages alongside each document, and expose the best one to an agent as `hit.content` (see [Custom Tools → `turingSearch`](/docs/custom-tools/#turingsearch-what-a-hit-gives-you)). Off by default: it increases response size, and a script already reading `hit.content` will start seeing a passage |

Two limits come from Adobe's API rather than from Turing, and you will see them:
a page can hold **at most 50 results** (a larger row count is reduced to 50), and
there is no way to jump to a page, hence `max-page-walk`.

:::caution
Never commit the token to `application.yaml`. Pass it as the environment variable
`TURING_CONTENTAI_TOKEN` (or `TURING_CONTENTAI_APIKEY`).
:::

---

### Search Cache

| Property | Default | Description |
|---|---|---|
| `turing.search.metrics.enabled` | `true` | Record search metrics (query terms, result counts, timestamps) |
| `turing.search.cache.enabled` | `false` | Enable server-side search result caching |
| `turing.search.cache.ttl` | `24h` | Interval of the **backstop** sweep that clears the cache. Unit-bearing: `24h`, `PT1H`, `30m` (a bare number is milliseconds) |
| `turing.search.cache.ttl.seconds` | *(none)* | **Deprecated**: despite its name the value was *milliseconds*, so `3600` meant a sweep every 3.6 seconds. Still honoured (as milliseconds) with a startup warning; use `turing.search.cache.ttl` |

A cached answer does **not** wait for the sweep to become correct: indexing,
deindexing and commit each evict the cache of the site they wrote, so a reindexed
document shows up on the next search. `turing.search.cache.ttl` only bounds how
long an entry may survive for a site that is never written again.

---

<a id="storage"></a>

### Storage (Assets, Knowledge Base & Pages)

Storage powers the [Assets](/docs/assets/) file manager, the RAG Knowledge Base, and [SPA Pages](/docs/spa-pages/). Disabled by default (`none`). Turing supports three pluggable storage backends:

| Type | Description |
|------|-------------|
| `none` | Disabled: Assets and Pages are hidden from the sidebar |
| `minio` | MinIO object storage (recommended for production) |
| `filesystem` | Local filesystem directory (simple deployments, development) |

#### Common

| Property | Default | Description |
|---|---|---|
| `turing.storage.type` | `none` | Storage backend: `none`, `minio`, or `filesystem` |

#### MinIO Backend

| Property | Default | Description |
|---|---|---|
| `turing.storage.minio.endpoint` | `http://localhost:9000` | MinIO server URL |
| `turing.storage.minio.access-key` | *(env `TURING_STORAGE_MINIO_ACCESS_KEY`)* | MinIO access key. Set via env for any real deployment. |
| `turing.storage.minio.secret-key` | *(env `TURING_STORAGE_MINIO_SECRET_KEY`)* | MinIO secret key. Set via env for any real deployment. |
| `turing.storage.minio.bucket` | `turing-assets` | Bucket name: created automatically on startup if it does not exist |

#### Filesystem Backend

| Property | Default | Description |
|---|---|---|
| `turing.storage.filesystem.path` | `./store/assets` | Base directory for stored files. Created automatically if it does not exist. |

The filesystem backend includes **path traversal protection**: all paths are resolved against the base directory and any attempt to escape it is blocked.

:::note
The **Assets** and **Pages** sections only appear in the sidebar when `turing.storage.type` is set to `minio` or `filesystem`.
:::

#### Agent Workspace & tool-result offload

The same storage backend also powers the per-conversation [Agent Workspace](/docs/agent-workspace/). When storage is `none` the workspace is inert and the offload feature below stays off: the default deployment is unchanged.

| Property | Default | Description |
|---|---|---|
| `turing.genai.tool-result-offload.enabled` | `true` | Offload a tool result larger than the limit below to the workspace, replacing it inline with a `workspace://` reference the model resolves via the `workspace_read` tool. Only active when storage is enabled. |
| `turing.genai.tool-result-offload.inline-max-chars` | `4096` | Tool-result size (characters) above which the result is offloaded. |

---

### MongoDB (Application Logs)

MongoDB is used to persist application logs, making them accessible from the admin console. Disabled by default.

| Property | Default | Description |
|---|---|---|
| `turing.mongodb.enabled` | `false` | Set `true` to enable MongoDB log persistence |
| `turing.mongodb.uri` | `mongodb://localhost:27017` | MongoDB connection URI |
| `turing.mongodb.logging.database` | `turingLog` | Database name for logs |
| `turing.mongodb.logging.collection.server` | `server` | Collection for general server logs |
| `turing.mongodb.logging.collection.aem` | `aem` | Collection for AEM-specific logs |
| `turing.mongodb.logging.collection.indexing` | `indexing` | Collection for indexing pipeline logs |
| `turing.mongodb.logging.purge.days` | `30` | Automatically delete log entries older than this many days |

---

### API Documentation (SpringDoc)

| Property | Default | Description |
|---|---|---|
| `springdoc.pathsToMatch` | `/api/**` | API paths included in the OpenAPI spec |
| `springdoc.swagger-ui.path` | `/swagger-ui.html` | Swagger UI path |

---

### Logging

| Property | Default | Description |
|---|---|---|
| `logging.file.name` | `store/logs/turing.log` | Log file path |
| `logging.logback.rollingpolicy.max-file-size` | `25MB` | Maximum size of each log file before rotation |
| `logging.logback.rollingpolicy.max-history` | `10` | Number of rotated log files to keep |
| `logging.level.com.viglet` | `INFO` | Log level for Turing application code |
| `logging.level.org.springframework` | `INFO` | Log level for Spring framework |
| `logging.level.dev.langchain4j` | `INFO` | Log level for LangChain4j (LLM library) |

---

### Keycloak (OAuth2 / OIDC)

When `turing.keycloak: true`, uncomment and configure the following block:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          keycloak:
            client-id: demo-app
            client-secret: your-client-secret
            scope: openid
            authorization-grant-type: authorization_code
            redirect-uri: http://localhost:2700/login/oauth2/code/demo-app
        provider:
          keycloak:
            issuer-uri: http://localhost:8080/realms/demo
            jwk-set-uri: http://localhost:8080/realms/demo/protocol/openid-connect/certs
            user-info-uri: http://localhost:8080/realms/demo/protocol/openid-connect/userinfo
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/demo
```

For full Keycloak setup instructions, see [Security & Keycloak](/docs/security-keycloak/).

---

### GenAI / LLM (Database Settings)

LLM providers, models, and GenAI behavior are managed through the **Administration Console** and stored in the database, not in `application.yaml`. The following settings are available via **Settings > Global Settings** in the admin UI:

| Setting | Default | Description |
|---|---|---|
| Default LLM | *(none)* | Global default LLM instance used when a Semantic Navigation site does not specify one |
| LLM Cache Enabled | `false` | Cache LLM responses to reduce API calls for repeated queries |
| LLM Cache TTL (ms) | `3600000` (1 hour) | How long cached LLM responses remain valid |
| LLM Cache Regenerate | `false` | When `true`, regenerate cached responses in the background |
| RAG Enabled | `false` | Enable Retrieval-Augmented Generation globally |
| Default Embedding Model | *(none)* | Global default embedding model for vector operations |
| Default Embedding Store | *(none)* | Global default embedding store instance |
| Python Executable | *(auto-detected)* | Path to Python 3 for the Code Interpreter tool (also overridable via `turing.code-interpreter.python-executable` in YAML) |

**Supported LLM providers:** OpenAI, Ollama, Anthropic (Claude), Google Gemini, Google Gemini (OpenAI-compatible), and Azure OpenAI.

LLM instances are created under **GenAI > LLM** in the admin console, where you configure the provider, API key, base URL, and model name.

---

<a id="rag-reranker-database-settings"></a>

### RAG Reranker (Database Settings)

The Semantic Navigation RAG path can run an optional **reranking** pass that re-orders retrieved chunks by relevance before they reach the model (see [Reranking](/docs/rag/#reranking-sharper-context-before-generation)). It is off by default; the reranker stage is a **pluggable strategy** and **fails open** (any error or missing configuration falls back to the original retrieval order). Configure it under **Settings > Global Settings** in the admin UI:

| Setting | Default | Description |
|---|---|---|
| Enable Reranker | `false` | Run the reranking pass over retrieved candidates before generation |
| Reranker Strategy | `LLM` | Backend: `LLM` (uses the chat model), `CROSS_ENCODER` (self-hosted `/rerank`), or `COHERE` (managed Cohere Rerank) |
| Reranker Top-K kept | `20` | How many of the highest-ranked chunks survive into the prompt (clamped to 1–100) |
| Reranker Endpoint | *(none)* | For `CROSS_ENCODER`: the self-hosted `/rerank` endpoint URL (Text Embeddings Inference / Infinity / Jina-compatible) |
| Reranker Model | *(backend default)* | Rerank model name; for `COHERE` defaults to `rerank-v3.5` |
| Reranker API Key | *(none)* | For `COHERE`: the Cohere API key. Stored **encrypted**; write-only (never returned by the API) |

**Cross-encoder** is the recommended production choice: purpose-built reranker models (e.g. `BAAI/bge-reranker-v2-m3`) are cheaper, faster, and more precise than borrowing a generative model. **Cohere** is the zero-ops managed alternative. **LLM** needs no extra infrastructure and is the safe default.

---

### Embedding Store (Database Settings)

Embedding store instances are configured through the **Administration Console** under **GenAI > Embedding Store**. Each instance specifies a vendor, connection URL, credentials, and optional provider-specific options via a JSON field.

**Supported vendors:**

| Vendor | Plugin ID | Key Provider Options |
|---|---|---|
| **ChromaDB** | `chroma` | `baseUrl` (default `http://localhost:8000`), `collectionName`, `tenantName`, `databaseName`, `initializeSchema`, `keyToken`, `basicUsername`, `basicPassword` |
| **Milvus** | `milvus` | `baseUrl`, `collectionName`, `databaseName`, `token`, `embeddingDimension`, `metricType`, `indexType`, `indexParameters`, `initializeSchema` |
| **PGVector** | `pgvector` | JDBC URL (set as instance URL), `tableName` (default `vector_store`), `schemaName` (default `public`), `dimensions`, `distanceType`, `indexType`, `initializeSchema` |

Provider options are passed as a JSON object in the **Provider Options** field when creating or editing an embedding store instance.

---

### Code Interpreter (Execution Mode)

The [Code Interpreter](/docs/tool-calling/) execution mode (`NATIVE` default / `DOCKER`) is chosen in **Console → Global Settings → Code Interpreter**. The YAML below tunes each mode (all under `turing.code-interpreter`):

| Property | Default | Purpose |
|---|---|---|
| `turing.code-interpreter.python-executable` | *(auto-detected)* | Python 3 binary (NATIVE) |
| `turing.code-interpreter.docker.network` | `none` | `none` = no egress; or a named docker network |
| `turing.code-interpreter.docker.memory` | `512m` | RAM cap per execution (DOCKER) |
| `turing.code-interpreter.docker.cpus` | `1.0` | CPU quota per execution (DOCKER) |
| `turing.code-interpreter.docker.pids-limit` | `128` | max PIDs (fork-bomb containment) |
| `turing.code-interpreter.docker.runtime` | *(host default)* | `runsc` for gVisor (opt-in) |
| `turing.code-interpreter.native.limits.enabled` | `false` | Opt-in CPU/RAM caps for NATIVE (Linux) |
| `turing.code-interpreter.native.limits.memory-max` | `1g` | NATIVE RAM cap |
| `turing.code-interpreter.native.limits.cpu-seconds` | `35` | NATIVE CPU-time cap |
| `turing.code-interpreter.warm-pool.enabled` | `false` | Pre-warmed interpreter pool (NATIVE only) |
| `turing.code-interpreter.warm-pool.size` | `2` | interpreters kept ready |

### Routines (Async Queue)

| Property | Default | Purpose |
|---|---|---|
| `turing.jms.routine.concurrency` | `1-2` | JMS listener concurrency for the [routine](/docs/routines/) worker (`scheduleAgent` jobs) |
| `turing.jms.concurrency` | `1-1` | Default JMS listener concurrency (indexing queue) |

### Turing as an MCP Server

The [MCP server](/docs/mcp-servers/#turing-as-an-mcp-server) is off by default. Enable and govern it with:

| Property | Default | Purpose |
|---|---|---|
| `spring.ai.mcp.server.enabled` | `false` | Master switch: when off, no MCP-server beans exist |
| `turing.mcp-server.loopback-only` | `true` | Reject non-loopback `/mcp` requests (the T245 trust boundary) |
| `turing.mcp-server.require-auth` | `false` | Turn `/mcp` into an OAuth 2.1 JWT resource server (set with `spring.security.oauth2.resourceserver.jwt.*`); fails closed if no decoder |
| `turing.mcp-server.write-enabled` | `false` | Register the write/ingestion tools (still require the `mcp:write` scope per call) |

### Chat Analytics

The [Chat Analytics](/docs/chat-analytics/) enricher, retention, and in-flight settings are documented in full on that page. One additional flag worth noting here:

| Property | Default | Purpose |
|---|---|---|
| `turing.chat.analytics.node-visit-log.enabled` | `false` | Record the per-turn node path on each conversation, enabling the **path-aware funnel** (T237). Off = V1 funnel behavior |

### Cost Governance

[Cost Governance](/docs/cost-governance/) is configured through data, not YAML: the **price table** (`tur_llm_price`, seeded for OpenAI/Anthropic/Gemini, editable in the console) and the **per-agent budget fields** (`monthlyBudgetUsd`, `perTurnSoftCapUsd`, `budgetDowngradeLlmId`) on the agent Settings form.

---

## Common Production Overrides

A minimal production override file (`application-production.yaml`) covering the most important changes:

```yaml
spring:
  profiles:
    active: production
  datasource:
    url: jdbc:mariadb://db-host:3306/turingdb
    username: turing
    password: strong_db_password
    driver-class-name: org.mariadb.jdbc.Driver
  h2:
    console:
      enabled: false

turing:
  url: https://search.yourcompany.com
  allowedOrigins: https://search.yourcompany.com
  ai.crypto.key: your-strong-random-key-here
  storage:
    type: minio
    minio:
      endpoint: http://minio:9000
      access-key: your-minio-user
      secret-key: your-minio-password
  mongodb:
    enabled: true
    uri: mongodb://mongo-host:27017

server:
  tomcat:
    accesslog:
      enabled: true
```

---

## Related Pages

| Page | Description |
|---|---|
| [Installation Guide](/docs/installation-guide/) | Where these properties are set during setup |
| [Search Engine](/docs/search-engine/) | Solr / Elasticsearch / Lucene connection settings |
| [LLM Instances](/docs/llm-instances/) | `turing.ai.crypto.key` and GenAI provider config |
| [Reranking](/docs/reranking/) | The `GLOBAL_RAG_SN_RERANK_*` settings |
| [Security & Keycloak](/docs/security-keycloak/) | OAuth2 / OIDC properties |


