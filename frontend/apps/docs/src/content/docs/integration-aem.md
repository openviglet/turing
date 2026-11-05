---
title: "AEM Connector"
description: "AEM integration overview in Viglet Turing: connector setup and links to detailed Viglet Dumont documentation."
---
The **Adobe Experience Manager (AEM)** connector allows Viglet Turing to index content from AEM author and publish instances. The connector runs as a separate process ([Viglet Dumont](https://dumont.viglet.org/docs/connectors/aem/)) and communicates with Turing via REST. Turing acts as a proxy, the API path `/api/v2/integration/{integrationId}/**` forwards requests to the configured connector endpoint.

For general integration management (creating instances, monitoring, statistics, and system information) see [Integration](/docs/integration/).

:::tip[Already running Adobe AEM Content AI?]
Then you have a second option and do not need this connector for search. The
[`CONTENTAI` search-engine vendor](/docs/search-engine/#content-ai) queries the Content AI
index Adobe already built over your published content, so you index it once instead of
twice. The trade-off is that relevance becomes Adobe's and the index is read-only.
Compare the two before choosing.
:::

---

## Overview

The AEM connector is a two-part system:

| Component | Where it runs | Documentation |
|---|---|---|
| **AEM Event Listener** | Inside AEM (OSGi bundle): sends indexing events to the connector | [Dumont: AEM Event Listener](https://dumont.viglet.org/docs/aem-event-listener/) |
| **AEM Connector Plugin** | Dumont process: fetches content from AEM and indexes it into Turing | [Dumont: AEM Connector](https://dumont.viglet.org/docs/connectors/aem/) |
| **Integration Instance** | Turing admin console: proxy configuration and monitoring | [Integration](/docs/integration/) |

---

## Configuration

All AEM-specific source configuration (connection details, root paths, content types, author/publish environments, locale mappings, delta tracking, indexing rules, and the Indexing Manager) is documented in the **[Dumont: AEM Connector](https://dumont.viglet.org/docs/connectors/aem/#source-configuration)** page.

The Turing admin console provides the UI for managing these settings under **Enterprise Search → Integration → [your AEM instance]**.

---

## Monitoring & Diagnostics

The following features are available in the Turing admin console for each AEM integration instance (see [Integration](/docs/integration/) for details):

| Feature | Description |
|---|---|
| **Monitoring** | Real-time dashboard for tracking indexing events with filters and auto-refresh |
| **Indexing Stats** | Table of completed bulk indexing operations with throughput metrics |
| **Double Check** | Validates consistency between the connector's content and the Turing search index |
| **System Information** | Live status, version, memory, and disk information from the connector |
| **AEM Logging** | AEM-specific logs in **Logging → AEM** (see [Logging](/docs/logging/)) |

---

## Related Pages

| Page | Description |
|---|---|
| [Integration](/docs/integration/) | General integration management: instances, monitoring, statistics, and system information |
| [Semantic Navigation](/docs/semantic-navigation/) | Configure the SN Sites that receive indexed content |
| [Architecture Overview](/docs/architecture-overview/) | End-to-end indexing flow from connector to Solr |
| [REST API Reference](/docs/rest-api/) | API endpoints for programmatic indexing |
| [Dumont: AEM Connector](https://dumont.viglet.org/docs/connectors/aem/) | How the AEM connector works: source configuration, indexing flow, event listeners, API triggering |
| [Dumont: AEM Event Listener](https://dumont.viglet.org/docs/aem-event-listener/) | Install the OSGi event listener bundle inside AEM for real-time indexing |
| [Dumont: Extending AEM](https://dumont.viglet.org/docs/extending-aem/) | Custom attribute extractors, content processors, and configuration JSON reference |

---

