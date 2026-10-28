---
title: "Logging"
description: "Monitor server activity, indexing pipeline, and AEM connector logs from the Viglet Turing admin console."
---
The **Logging** console (`/admin/logging/instance`) provides centralized visibility into Viglet Turing operations. It is accessible from the sidebar and requires MongoDB to be enabled.

:::note[MongoDB required]
The Logging console requires `turing.mongodb.enabled: true`. Logs are stored in MongoDB collections and automatically purged after the configured retention period. See [Configuration Reference](/docs/configuration-reference/#mongodb-application-logs) for setup details.
:::

---

## Log Categories

The logging landing page displays three cards, each linking to a dedicated log viewer:

| Category | Path | Description |
|---|---|---|
| **Server** | `/admin/logging/instance/server` | General application logs: startup, runtime errors, request processing, and framework messages |
| **Indexing** | `/admin/logging/instance/indexing` | Content indexing pipeline events: tracks every document from connector receipt through queue processing to final index commit |
| **AEM** | `/admin/logging/instance/aem` | Adobe Experience Manager connector logs: authentication, content retrieval, and AEM-specific processing |

---

## Server Logging

Displays general application logs captured from the Turing runtime.

**Filters:**

| Filter | Description |
|---|---|
| Level | `INFO`, `WARN`, `ERROR`, `DEBUG`, `TRACE`: or All |
| Date From / Date To | Restrict results to a time window |
| Search | Free-text search across log messages and stack traces |

**Result columns:**

| Column | Description |
|---|---|
| Date | Full timestamp with relative time (e.g., "2 hours ago") |
| Node | Cluster node name (useful in multi-node deployments) |
| Level | Color-coded badge: blue (INFO), yellow (WARN), red (ERROR), purple (DEBUG), slate (TRACE) |
| Logger | Java class that produced the log entry |
| Message | Log message text with syntax highlighting |

**Stack trace viewer:** Clicking the alert icon on an error row opens a resizable side panel (60/40 split) displaying the full stack trace with syntax highlighting for Java packages, severity levels, and Spring components.

---

## Indexing Logging

Tracks every document through the indexing pipeline, from connector receipt to final index commit. This is the primary troubleshooting view for content indexing issues.

**Filters:**

| Filter | Description |
|---|---|
| Status | Any of the indexing statuses listed below |
| Result Status | `SUCCESS` or `ERROR` |
| Date From / Date To | Restrict results to a time window |
| Content ID | Filter by document ID (supports partial match) |
| URL | Filter by document URL (supports partial match) |

**Result columns:**

| Column | Description |
|---|---|
| Date | Timestamp with relative time |
| Source | Source system that sent the document |
| Status | Processing status with icon and color coding (see table below) |
| Result Status | `SUCCESS` (green) or `ERROR` (red) |
| URL | Document URL (clickable) |
| Environment | `AUTHOR` (amber) or `PUBLISHING` (emerald) |
| Locale | Language code badge |
| Sites | Target SN Sites displayed as colored badges |

### Indexing Status Values

| Status | Icon | Description |
|---|---|---|
| `PREPARE_INDEX` | Search | Preparing to index the document |
| `PREPARE_UNCHANGED` | Clock | No changes detected since last indexing |
| `PREPARE_REINDEX` | Refresh | Preparing a reindexation |
| `PREPARE_FORCED_REINDEX` | Zap | Forced reindexation triggered |
| `RECEIVED_AND_SENT_TO_TURING` | Send | Document received by the connector and forwarded to Turing |
| `SENT_TO_QUEUE` | Arrow | Document placed in the Artemis processing queue |
| `RECEIVED_FROM_QUEUE` | Database | Document consumed from the queue by the indexing pipeline |
| `INDEXED` | Check | Document successfully indexed |
| `FINISHED` | Check | Operation finished |
| `DEINDEXED` | Ban | Document removed from the index |
| `NOT_PROCESSED` | Clock | Document skipped |
| `IGNORED` | Ban | Document ignored due to an indexing rule or connector decision |

---

## AEM Logging

Displays logs specific to the Adobe Experience Manager integration. The filter and column layout is identical to [Server Logging](#server-logging): level, date range, and text search are available.

Use this view to diagnose AEM authentication failures, content retrieval errors, or replication issues.

---

## Common Features

All three log viewers share the following capabilities.

### Auto-Refresh

A dropdown in the page header configures automatic data refresh:

| Interval | Value |
|---|---|
| Off | No auto-refresh |
| 1 second | Real-time monitoring |
| 5 seconds | Active troubleshooting |
| 10 seconds | Default monitoring |
| 30 seconds | Background monitoring |
| 1 minute | Low-frequency check |
| 5 minutes | Periodic review |

The last sync timestamp is displayed next to the refresh indicator.

### Pagination

| Control | Description |
|---|---|
| Page navigation | First, Previous, Next, Last page buttons |
| Rows per page | Configurable: 10, 20, 30, 40, 50, or 100 rows |
| Sort order | Ascending or descending by date |

### Syntax Highlighting

Log messages are rendered with syntax highlighting for:

- **Severity keywords**: ERROR/FATAL (red), WARN (yellow), INFO (blue), DEBUG (purple)
- **Spring components**: DispatcherServlet, ContextLoaderListener, Servlet, Filter (indigo)
- **Java packages and classes**: amber italic
- **URLs and file paths**: blue with underline
- **IP addresses**: cyan
- **Quoted strings**: sky blue monospace

---

## REST API

The logging data is available via REST endpoints. Authentication required.

### Server Logs

```
GET /api/logging
```

| Parameter | Default | Description |
|---|---|---|
| `page` | `0` | Page number |
| `pageSize` | `100` | Records per page |
| `level` | | Log level filter (`INFO`, `WARN`, `ERROR`, `DEBUG`, `TRACE`) |
| `dateFrom` | | Start date (`yyyy-MM-dd`) |
| `dateTo` | | End date (`yyyy-MM-dd`) |
| `search` | | Text search in message or stack trace |
| `sort` | `asc` | Sort order (`asc` or `desc`) |

**Example:**

```bash
curl "http://localhost:2700/api/logging?level=ERROR&pageSize=50&sort=desc" \
  -H "Key: <YOUR_API_TOKEN>"
```

### Indexing Logs

```
GET /api/logging/indexing
```

| Parameter | Default | Description |
|---|---|---|
| `page` | `0` | Page number |
| `pageSize` | `100` | Records per page |
| `dateFrom` | | Start date (`yyyy-MM-dd`) |
| `dateTo` | | End date (`yyyy-MM-dd`) |
| `status` | | Indexing status filter (see status table above) |
| `contentId` | | Content ID filter (partial match) |
| `resultStatus` | | `SUCCESS` or `ERROR` |
| `url` | | URL filter (partial match) |
| `sort` | `asc` | Sort order (`asc` or `desc`) |

**Example:**

```bash
curl "http://localhost:2700/api/logging/indexing?status=INDEXED&resultStatus=ERROR&sort=desc" \
  -H "Key: <YOUR_API_TOKEN>"
```

### AEM Logs

```
GET /api/logging/aem
```

Same parameters as [Server Logs](#server-logs).

### Response Structure

All three endpoints return a paginated response:

```json
{
  "content": [ ... ],
  "page": 0,
  "pageSize": 100,
  "totalElements": 1250,
  "totalPages": 13
}
```

---

## Configuration

Logging is configured in `application.yaml`. See [Configuration Reference](/docs/configuration-reference/) for the full property list.

**MongoDB (application logs):**

```yaml
turing:
  mongodb:
    enabled: true
    uri: mongodb://localhost:27017
    logging:
      database: turingLog
      collection:
        server: server
        aem: aem
        indexing: indexing
      purge:
        days: 30
```

**Local file logging (Logback):**

```yaml
logging:
  file:
    name: store/logs/turing.log
  logback:
    rollingpolicy:
      max-file-size: 25MB
      max-history: 10
  level:
    com.viglet: INFO
    org.springframework: INFO
    dev.langchain4j: INFO
```

---

## Related Pages

| Page | Description |
|---|---|
| [Configuration Reference](/docs/configuration-reference/) | Full property reference for MongoDB and Logback settings |
| [Integration](/docs/integration/) | Integration monitoring dashboard (per-connector indexing events) |
| [Architecture Overview](/docs/architecture-overview/) | End-to-end indexing flow |

---

