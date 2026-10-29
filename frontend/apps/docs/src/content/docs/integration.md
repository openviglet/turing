---
title: "Integration"
description: "Manage content connectors that import and index content from external sources into Viglet Turing."
---
The **Integration** page (`/admin/integration/instance`) manages connectors that import and index content from external sources (such as AEM, web crawlers, or other content repositories) into the Viglet Turing search engine. It is accessible from the **Enterprise Search** section of the sidebar.

Each **Integration instance** represents a configured connection to a content connector application. The connector runs as a separate process (for example, Viglet Dumont) and communicates with Turing via REST. Turing acts as a proxy: the API path `/api/v2/integration/{integrationId}/**` forwards requests to the configured connector endpoint, with built-in SSRF protection.

---

## Instance Listing

The listing page shows all configured Integration instances as a grid of cards, each displaying the integration's title and description. A button at the top of the page opens the creation form.

---

## Creating or Editing an Integration

The creation and edit form is divided into two sections.

**General Information**

| Field | Description |
|---|---|
| Integration Name | Human-readable identifier for this integration (required) |
| Short Description | Optional description of the integration's purpose |

**Connection Details**

| Field | Description |
|---|---|
| Integration Type | Connector type: **AEM** or **Web Crawler** |
| Endpoint | URL of the connector application |
| Enabled | Toggle to activate or deactivate this integration |

---

## Integration Detail: Sections

After an integration is created, its detail page provides navigation to several sections. The sections below are common to all connector types. For AEM-specific configuration, see the [Turing AEM Connector](/docs/integration-aem/) overview and the full [Dumont AEM Connector](https://openviglet.github.io/dumont/docs/connectors/aem/) documentation.

---

### Settings

The Settings section contains the same form fields as the creation form (General Information and Connection Details), allowing you to edit the integration name, description, endpoint, type, and enabled state.

---

### Monitoring

The Monitoring section is a real-time dashboard for tracking the indexing pipeline. It displays indexing events as they are received and processed.

**Filters:**

| Filter | Description |
|---|---|
| Date From / Date To | Restrict results to a time window |
| Object ID | Filter by a specific document ID |
| Status | Filter by processing status (see status table below) |
| Environment | Author or Publish |
| Language | Locale code (for example, `en_US`) |
| Sites | Filter by target SN Site |

**Auto-refresh:** configurable intervals: Off, 1s, 5s, 10s, 30s, 1m, 5m.

**Result columns:** Date, Object ID, Status, Environment, Language, Sites.

Results are paginated and sortable.

#### Indexing Status Values

| Status | Meaning |
|---|---|
| `PREPARE_INDEX` | Preparing to index the document |
| `PREPARE_UNCHANGED` | No changes detected since last indexing |
| `PREPARE_REINDEX` | Preparing a reindexation |
| `PREPARE_FORCED_REINDEX` | Forced reindexation triggered |
| `RECEIVED_AND_SENT_TO_TURING` | Document received by the connector and forwarded to Turing |
| `SENT_TO_QUEUE` | Document placed in the Artemis processing queue |
| `RECEIVED_FROM_QUEUE` | Document consumed from the queue by the indexing pipeline |
| `INDEXED` | Document successfully indexed in Solr |
| `FINISHED` | Operation finished |
| `DEINDEXED` | Document removed from the index |
| `NOT_PROCESSED` / `IGNORED` | Document skipped due to an Indexing Rule or connector decision |

---

### Indexing Stats

The Indexing Stats section provides a table of completed bulk indexing operations for this integration.

| Column | Description |
|---|---|
| Start Time | Timestamp when the operation started |
| Source | Source that was operated on |
| Operation | `INDEX_ALL` or `REINDEX_ALL` |
| Documents | Number of documents processed. A **Partial** badge marks a run that stopped before reaching the whole source (a call budget, a rate limit, an interruption), with the reason listed under the count. A partial run never removes documents it did not reach. |
| Duration | Total elapsed time |
| Docs/min | Throughput (documents per minute) |

---

### Double Check

The Double Check section validates the consistency between the connector's content and the Turing search index. It detects drift between what the connector knows about and what is actually indexed.

- Select a **Source** to inspect
- Results are shown in two views:
  - **Missing**: content that exists in the connector but is not present in the index
  - **Extra**: content that is present in the index but no longer exists in the connector
- Results are grouped by Solr core in an accordion, listing the affected document paths

Use Double Check after a partial failure, a forced reindex, or when users report missing or stale search results.

---

### System Information

Displays live diagnostic information from the remote connector application.

**Status badge:** UP (green) or DOWN (red): indicates whether the connector endpoint is reachable.

**Application:**

| Item | Description |
|---|---|
| Application | Connector application name |
| Version | Connector build version |
| Java Version | JVM version running the connector |
| Vendor | JVM vendor |
| JVM | JVM identifier |
| OS | Operating system name and version |

**Memory and disk gauges** (progress bars with total, used, and free values):

| Gauge | Description |
|---|---|
| Physical Memory (RAM) | Host system memory |
| JVM Heap Memory | Java heap utilisation and limits |
| Disk Space | Available storage on the connector's host volume |

---

## Architecture

Turing acts as a transparent proxy to the connector. The API path `/api/v2/integration/{integrationId}/**` forwards all requests to the configured connector endpoint, including authentication headers. This design keeps the connector application decoupled: it does not need to be publicly accessible, only reachable from the Turing server.

Built-in SSRF protection validates the endpoint before forwarding. Requests to private IP ranges, loopback addresses, or disallowed schemes are rejected.

---

## Related Pages

| Page | Description |
|---|---|
| [AEM Connector](/docs/integration-aem/) | AEM-specific configuration: sources, content types, environments, indexing rules, and the indexing manager |
| [Semantic Navigation](/docs/semantic-navigation/) | Configure the SN Sites that receive indexed content |
| [Architecture Overview](/docs/architecture-overview/) | End-to-end indexing flow from connector to Solr |
| [REST API Reference](/docs/rest-api/) | API endpoints for programmatic indexing |
| [Dumont: Connectors](https://openviglet.github.io/dumont/docs/connectors/overview/) | Available connectors (Web Crawler, AEM, Database, FileSystem, WordPress) and how to deploy them |
| [Dumont: REST API](https://openviglet.github.io/dumont/docs/rest-api/) | Connector-side API for triggering indexing, monitoring, and source management |

---


