---
title: "getting-started/core-concepts"
description: "The mental model behind Viglet Turing (semantic navigation sites, faceted search, indexing, and generative AI) explained in plain terms."
---
This page explains the fundamental concepts of Viglet Turing in plain terms. No configuration files, no code, just the mental model you need before diving into the technical documentation.

:::tip[This is Step 1 of the learning path]
You're at the start of the [zero → first-agent learning path](/docs/getting-started/intro/#your-learning-path). When the model below clicks, move on to **Step 2, your first search** ([Installation](/docs/installation-guide/) → [Semantic Navigation](/docs/semantic-navigation/)).
:::

---

## Semantic Navigation Site

The **Semantic Navigation Site** (or SN Site) is the central object in Turing. Everything revolves around it.

Think of an SN Site as a configured search experience. It defines:

- **What content is indexed**: which fields each document has (title, text, URL, date, image, and custom fields)
- **How content is searched**: which fields are searchable, how results are ranked, how many results per page
- **How results are navigated**: which fields become filterable facets (e.g., category, author, date range)
- **How results are presented**: field mappings for display, highlighting of matched terms
- **Advanced behaviors**: Spotlights for curated results, Targeting Rules for personalization, Merge Providers for combining content from multiple sources
- **AI capabilities**: whether GenAI and RAG are enabled, and which language model to use

A single Turing instance can host multiple SN Sites, each independently configured. For example, you could have one site for your public website, another for your internal knowledge base, and a third for your product documentation, all on the same server.

---

## Content Ingestion

Content does not flow into Turing on its own. It needs connectors.

### Viglet Dumont

**Viglet Dumont** is a separate application that manages connectors, the components responsible for extracting content from its original source and sending it to Turing. Dumont and Turing are separate projects that work together: Dumont handles *getting* content, Turing handles *indexing and searching* it.

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'fontSize': '13px', 'primaryColor': '#fff', 'primaryBorderColor': '#c0c0c0', 'lineColor': '#888', 'textColor': '#333'}}}%%
graph TB
    SRC["📡 Content Sources\nCMS · Database · Files · Web"]
    DEP["🔌 Viglet Dumont\nConnectors"]
    TES["🌐 Turing\nREST API"]
    IDX["🔎 Index\n(Search Engine)"]

    SRC --> DEP
    DEP -->|"HTTP POST"| TES
    TES --> IDX

    classDef blue fill:#dbeafe,stroke:#4A90D9,stroke-width:2px,color:#1a1a1a
    classDef green fill:#dcfce7,stroke:#50B86C,stroke-width:2px,color:#1a1a1a
    classDef purple fill:#ede9fe,stroke:#9B6EC5,stroke-width:2px,color:#1a1a1a
    classDef amber fill:#fef3c7,stroke:#E8A838,stroke-width:2px,color:#1a1a1a

    class SRC amber
    class DEP purple
    class TES blue
    class IDX green
```

### How a connector works

Each connector in Dumont:
1. Connects to a content source (a website, a database table, a file folder, a CMS)
2. Extracts documents according to its configuration (which pages to crawl, which SQL query to run, which folder to scan)
3. Sends each document to the Turing REST API, targeting a specific SN Site

Turing receives the document, validates it against the SN Site configuration, and creates an indexing job. The job processes the document and writes it to the configured search engine (Apache Solr, Elasticsearch, or Apache Lucene), making it immediately searchable. Apache Solr is the primary and default backend, but Turing also supports Elasticsearch and Lucene as alternative search engine backends.

### What happens when two connectors index the same content

Sometimes the same real-world document exists in two systems with complementary information. For example, your CMS has structured metadata (author, tags, content type) while your web crawler has the full rendered text of the same page. Neither connector alone gives you a complete document.

**Merge Providers** solve this by instructing Turing to detect when two connectors have indexed the same document (using a shared field as a join key) and merge them into one enriched result. See [Semantic Navigation](/docs/semantic-navigation/#merge-providers) to learn how to configure this.

---

## Search

When a user searches on an SN Site, this is what happens, in simple terms:

```mermaid
%%{init: {'theme': 'base', 'themeVariables': {'fontSize': '13px', 'actorBkg': '#dbeafe', 'actorBorder': '#4A90D9', 'actorTextColor': '#1a1a1a', 'activationBkgColor': '#ede9fe', 'activationBorderColor': '#9B6EC5', 'labelBoxBkgColor': '#fef3c7', 'labelBoxBorderColor': '#E8A838', 'labelTextColor': '#1a1a1a', 'noteBkgColor': '#dcfce7', 'noteBorderColor': '#50B86C', 'noteTextColor': '#1a1a1a', 'signalColor': '#333', 'signalTextColor': '#333'}}}%%
sequenceDiagram
    participant U as User
    participant API as Turing API
    participant S as Search Engine
    participant SP as Spotlight Engine

    U->>API: Query + user profile attributes
    API->>API: Targeting Rules →<br/>build filter queries
    API->>S: Query + filters + facets
    S-->>API: Filtered results + facet counts
    API->>API: Map fields + highlighting
    API->>SP: Check spotlight term match
    SP-->>API: Inject curated results<br/>into response
    API-->>U: Results · Facets · Spotlights · Locales
```

1. The query arrives at the Turing API along with the user's profile attributes (if Targeting Rules are configured)
2. **Targeting Rules** run first: they translate the user's profile attributes into additional filter queries (e.g., Solr `fq` parameters when using the default Solr backend), so only content relevant to that user is retrieved
3. Turing executes the search engine query with those filters, facet counts, and result ranking applied together
4. Results come back from the search engine already filtered; Turing maps fields and applies highlighting
5. **Spotlights** are checked last: if the query matches a spotlight term, curated documents are injected at their configured positions in the response
6. The final response (results, facets, spotlights, locales, spell check, and similar documents) goes back to the client

### Facets

Facets are filterable dimensions shown alongside search results. A user searching for "annual report" might see facets for *Year*, *Department*, and *Document Type*, and click to narrow results to "Finance" documents from "2024".

Facets are configured per SN Site, at the field level. Any indexed field can become a facet: the configuration defines how it is labeled, sorted, and counted.

### Spotlights

A Spotlight is a curated result pinned to a specific search term. When someone searches for "benefits", you can ensure your HR benefits page always appears at the top: regardless of how it ranks organically.

Spotlights are configured per SN Site and managed through the admin console. See [Spotlights](/docs/semantic-navigation/#spotlights) for details.

### Targeting Rules

Targeting Rules let you show different results to different users from the same index. A document tagged as `audience: internal` only appears for users whose profile includes that attribute. Users without a matching profile always see untagged content.

See [Targeting Rules](/docs/semantic-navigation/#targeting-rules) for a full explanation.

---

## Generative AI

Turing integrates generative AI in two places: directly on **SN Sites** (search + summarization) and through standalone **AI Agents** (open-ended chat with tools).

### RAG on an SN Site

When GenAI is enabled for an SN Site, indexed documents are also stored as **vector embeddings**, a mathematical representation of their meaning, not just their words. When a user asks a question, Turing:

1. Finds the most semantically relevant documents (not just keyword matches) using the vector embeddings
2. Passes those documents as context to a language model
3. Returns a natural-language answer grounded in your actual content

This is called **Retrieval-Augmented Generation (RAG)**. The LLM does not make things up: it answers based on what is in your index.

### Knowledge Base

Beyond SN Sites, Turing provides a **Knowledge Base**, a file and folder interface in the admin console (backed by MinIO) where you can upload documents directly. These files are also indexed as vector embeddings and become available to AI Agents as a searchable knowledge source, independent of any connector or SN Site.

The Knowledge Base is managed through the **[Assets](/docs/assets/)** page: drag-and-drop upload, folder navigation, inline preview, and batch AI training with real-time progress. Uploaded files are indexed automatically on upload and unindexed on deletion.

### AI Agents

An **AI Agent** is a named assistant that you compose from three ingredients:

- A **language model** (Anthropic Claude, OpenAI, Gemini, Ollama, or others)
- A set of **tools** it can call (search your SN Sites, query the Knowledge Base, browse the web, run Python code, get financial data, and more)
- Optionally, one or more **MCP Servers**: external services that provide additional tools via the Model Context Protocol

Each AI Agent appears as its own tab in the **[Chat](/docs/chat/)** interface. You can have a "Research Assistant" that combines SN Site search with web browsing, a "Data Analyst" that can run Python code and query your knowledge base, and a "Support Agent" that only sees your product documentation, all on the same platform.

See [AI Agents](/docs/ai-agents/) for configuration, [Tool Calling](/docs/tool-calling/) for the full tool reference, and [MCP Servers](/docs/mcp-servers/) for connecting external tools.

---

## The Admin Console

Everything described above (SN Sites, connectors, Spotlights, Targeting Rules, Merge Providers, AI Agents, MCP Servers, and the Knowledge Base) is configured and managed through the **Turing Admin Console**: a browser-based React application served by Turing itself.

The admin console also exposes application logs (when MongoDB is configured), search metrics, and top search terms, giving you operational visibility without needing access to the server.

---

## Consuming Turing from Your Application

Turing exposes its search, chat, and indexing capabilities through four integration options. Choose the one that best fits your technology stack.

### REST API

The primary integration method. All search, indexing, and administration operations are available as HTTP endpoints. The search response is a self-describing JSON object with pre-built navigation links, no URL construction needed on the client side.

```
GET https://<your-host>/api/sn/{siteName}/search?q=your+query
```

Suitable for any language or platform that can make HTTP requests.

### GraphQL

Turing also exposes a GraphQL endpoint for clients that prefer a graph-based query model. Useful when you want to request only specific fields or combine multiple queries in a single request.

```
POST https://<your-host>/graphql
```

### Java SDK

An official Java client library is available on **Maven Central**. It provides a typed API for performing searches, submitting documents for indexing, and interacting with SN Sites, without dealing with raw HTTP or JSON.

```xml
<dependency>
    <groupId>com.viglet.turing</groupId>
    <artifactId>turing-java-sdk</artifactId>
    <version>${turing.version}</version>
</dependency>
```

### JavaScript / TypeScript SDK

An official JavaScript SDK is available on **npm**. It is TypeScript-ready and covers the same search and indexing operations as the Java SDK, suited for web applications and Node.js backends.

```bash
npm install @viglet/turing-sdk
```

---

## Ready to go deeper?

| I want to... | Go to |
|---|---|
| Understand the full system architecture | [Architecture Overview](/docs/architecture-overview/) |
| Configure Spotlights, Targeting Rules, or Merge Providers | [Semantic Navigation](/docs/semantic-navigation/) |
| Set up GenAI and RAG | [GenAI & LLM Configuration](/docs/genai-llm/) |
| Configure AI Agents and tools | [AI Agents](/docs/ai-agents/), [Tool Calling](/docs/tool-calling/) |
| Use the AI chat interface | [Chat](/docs/chat/) |
| Upload documents to the Knowledge Base | [Assets](/docs/assets/) |
| Monitor LLM token consumption | [Token Usage](/docs/token-usage/) |
| Authenticate via API Key | [Authentication](/docs/security-authentication/) |
| Secure Turing with Keycloak | [Security & Keycloak](/docs/security-keycloak/) |
| Install Turing | [Installation Guide](/docs/installation-guide/) |

---


