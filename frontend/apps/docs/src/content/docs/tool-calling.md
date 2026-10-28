---
title: "Tool Calling"
description: "Native tool calling capabilities available to AI Agents in Viglet Turing, DSL-based search tools, RAG, web crawling, and more."
---
A **Tool Calling** is a function that the LLM can invoke autonomously during a conversation to retrieve information or perform an action. Instead of relying purely on training data, the LLM calls tools to fetch live data, search indexed content, execute code, and more, then incorporates the results into its reasoning.

Viglet Turing includes native tools organized into categories, plus support for external tools via [MCP Servers](/docs/mcp-servers/). Tools are enabled per [AI Agent](/docs/ai-agents/), each agent selects only the tools it needs.

---

## DSL Search: 6 tools

These tools provide **Elasticsearch-compatible Query DSL** access to any Semantic Navigation Site. Inspired by the [Elasticsearch MCP Server](https://github.com/elastic/mcp-server-elasticsearch), they allow the LLM to build and execute full DSL queries, aggregations, and analytics, automatically translated to the site's configured search engine (Elasticsearch, Solr, or Lucene).

| Tool | Description |
|---|---|
| `dsl_list_indices` | Lists all available SN Sites with engine type, locales, and status |
| `dsl_get_mappings` | Returns field mappings (types, facets, multi-valued) in Elasticsearch format |
| `dsl_search` | Executes a full Elasticsearch Query DSL search (40 query types, 35 aggregations) |
| `dsl_get_document` | Retrieves a document by ID with all stored fields |
| `dsl_suggest` | Autocomplete suggestions and spell-check corrections |
| `dsl_get_shards` | Shard/core information: document counts, engine type, endpoints, locales |

### How the LLM uses DSL tools

The LLM follows a pattern similar to how a developer uses the Elasticsearch API:

1. **Discover** → `dsl_list_indices("*")` to find available sites
2. **Understand schema** → `dsl_get_mappings("mySite")` to see fields and types
3. **Search** → `dsl_search("mySite", "en", queryBody)` with full DSL
4. **Deep dive** → `dsl_get_document("mySite", "en", "doc-123")` for full content
5. **Correct typos** → `dsl_suggest("mySite", "en", "enterprse")` for corrections

### The `dsl_search` tool

This is the most powerful tool. The `queryBody` parameter accepts the complete Elasticsearch `_search` request body as a JSON string. The LLM constructs the query based on the user's intent:

**Simple search:**
```json
{"query":{"match":{"title":"machine learning"}},"size":5}
```

**Filtered search with aggregations:**
```json
{
  "query":{"bool":{"must":[{"match":{"title":"security"}}],"filter":[{"term":{"type":"article"}}]}},
  "size":10,
  "aggs":{"by_author":{"terms":{"field":"author","size":5}}},
  "highlight":{"fields":{"title":{},"body":{}}}
}
```

**Date range with sorting:**
```json
{
  "query":{"range":{"date":{"gte":"2025-01-01"}}},
  "sort":[{"date":"desc"}],
  "size":20
}
```

**Similar documents (More Like This):**
```json
{"query":{"more_like_this":{"fields":["title","body"],"like":"enterprise search platform","min_term_freq":1}}}
```

**Facet value discovery (terms aggregation):**
```json
{"query":{"match_all":{}},"size":0,"aggs":{"categories":{"terms":{"field":"category","size":50}}}}
```

For the complete DSL reference, see [DSL Query API](/docs/dsl-query/) and [DSL Compatibility Matrix](/docs/dsl-compatibility/).

### Prompt examples

- *"Search for documents about authentication in the Sample site"* → `dsl_search` with `match` query
- *"What fields can I filter by on the Sample site?"* → `dsl_get_mappings`
- *"Show me the full content of document ID abc-123"* → `dsl_get_document`
- *"How many documents do we have per content type?"* → `dsl_search` with `terms` aggregation
- *"Find documents similar to the article about Solr configuration"* → `dsl_search` with `more_like_this`
- *"What articles were updated in the last 7 days about security?"* → `dsl_search` with `range` + `sort`
- *"Compare the features of Product A and Product B"* → `dsl_get_document` called for each product
- *"What categories are available?"* → `dsl_search` with `size:0` and `terms` aggregation
- *"How many documents per locale does the site have?"* → `dsl_get_shards`

---

## RAG / Knowledge Base: 4 tools

These tools enable the LLM to query the **Knowledge Base**, files indexed from MinIO via the [Assets](/docs/assets/) section, using semantic similarity search.

| Tool | Description |
|---|---|
| `search_knowledge_base` | Searches for relevant documents by semantic similarity (top 10, threshold 0.7) |
| `knowledge_base_stats` | Returns statistics: total files, chunks, and storage size |
| `list_knowledge_base_files` | Lists all indexed files, with optional keyword filter |
| `get_file_from_knowledge_base` | Retrieves the full indexed content of a specific file |

**Prompt examples:**

- *"What does our internal documentation say about deployment procedures?"* → triggers `search_knowledge_base`
- *"How many files are indexed in the knowledge base?"* → triggers `knowledge_base_stats`
- *"Show me the full content of the file onboarding-guide.pdf"* → triggers `list_knowledge_base_files` then `get_file_from_knowledge_base`

---

## Web Crawler: 2 tools

| Tool | Description |
|---|---|
| `fetch_webpage` | Fetches a web page by URL and returns its content as plain text |
| `extract_links` | Extracts all links from a web page, with optional keyword filter |

**Prompt examples:**

- *"What does the Apache Solr documentation say about faceted search?"* → triggers `fetch_webpage`
- *"List all links on our company blog homepage"* → triggers `extract_links`

---

## Finance: 2 tools

| Tool | Description |
|---|---|
| `get_stock_quote` | Current price and market data for a stock ticker symbol |
| `search_ticker` | Looks up a ticker symbol by company name or keyword |

**Prompt examples:**

- *"What is the current stock price of Apple?"* → triggers `search_ticker` then `get_stock_quote`
- *"Show me the market data for MSFT"* → triggers `get_stock_quote`

---

## Weather: 1 tool

| Tool | Description |
|---|---|
| `get_weather` | Current weather and forecast for a city (1–7 day range) |

**Prompt examples:**

- *"What's the weather forecast for São Paulo this week?"* → triggers `get_weather`

---

## Image Search: 1 tool

| Tool | Description |
|---|---|
| `search_images` | Searches the web for images and returns URLs and descriptions |

**Prompt examples:**

- *"Find images of enterprise search architecture diagrams"* → triggers `search_images`

---

## DateTime: 1 tool

| Tool | Description |
|---|---|
| `get_current_time` | Returns the current date and time for a given IANA timezone |

**Prompt examples:**

- *"What time is it right now in Tokyo?"* → triggers `get_current_time`

---

## Code Interpreter: 1 tool

| Tool | Description |
|---|---|
| `execute_python` | Executes Python code in a sandboxed environment and returns stdout/stderr |

The Code Interpreter runs Python in an isolated sandbox directory with a **30-second execution timeout**. It supports:

- Standard output capture
- Matplotlib chart generation (with `Agg` backend, no display required)
- Automatic `print()` wrapping for bare expressions

The Python executable path is configured in **Administration → Settings → Python Path**.

**Prompt examples:**

- *"Calculate the compound interest on $10,000 at 5% for 10 years"* → triggers `execute_python`
- *"Generate a bar chart showing monthly sales: Jan=100, Feb=150, Mar=120"* → triggers `execute_python` with matplotlib

### Execution modes: NATIVE vs DOCKER

You choose how Python actually runs in **Console → Global Settings → Code Interpreter**:

| Mode | What it is | When to use |
|---|---|---|
| **NATIVE** (default) | A host subprocess running the configured Python | Trusted content, simplest setup |
| **DOCKER** | Each execution runs in a fresh, hardened container | Untrusted input, multi-tenant, defense-in-depth |

**DOCKER** is the recommended posture when code might be influenced by untrusted input. Every execution gets a throwaway container with baseline hardening always applied (`--cap-drop ALL`, `--no-new-privileges`, `--read-only`, and Docker's built-in seccomp profile) plus configurable limits under `turing.code-interpreter.docker.*`:

| Key | Default | Purpose |
|---|---|---|
| `network` | `none` | `none` = **no egress**; or a named docker network |
| `memory` | `512m` | hard RAM cap per execution |
| `cpus` | `1.0` | CPU quota per execution |
| `pids-limit` | `128` | max PIDs (fork-bomb containment) |
| `runtime` | (host default) | set `runsc` for **gVisor** extra isolation (opt-in) |
| `seccomp-profile` | (Docker default) | path to a tighter seccomp profile (opt-in) |

A **"Check Docker"** probe in Global Settings verifies the daemon is reachable before you switch.

### Resource limits for the NATIVE path

DOCKER caps via the container runtime; the NATIVE path can stop a runaway script (e.g. `[x*x for x in range(10**9)]`) from exhausting host RAM before the timeout fires. Opt in under `turing.code-interpreter.native.limits` (Linux only):

```yaml
turing:
  code-interpreter:
    native:
      limits:
        enabled: true       # default false (timeout is the only guard)
        memory-max: 1g       # hard RAM cap
        cpu-seconds: 35      # CPU-time cap
        limiter: auto        # auto | prlimit | systemd-run | none
```

### Warm pool (NATIVE only)

A frequently-invoked tool pays a ~200ms cold-boot each call. Enable a pre-warmed pool of single-use interpreters to skip it:

```yaml
turing:
  code-interpreter:
    warm-pool:
      enabled: true   # default false
      size: 2          # interpreters kept booted and ready
```

Workers are **single-use** (one execution then exit, no cross-tenant state leak) and refilled asynchronously. The pool is automatically bypassed in DOCKER mode and whenever the NATIVE limits above are enabled (a pooled worker can't be wrapped by `prlimit`/`systemd-run`).

### Structured output & `sandbox:` URLs

Beyond stdout/stderr, the interpreter can return a **structured result**, used by [Custom Tools](/docs/custom-tools/) via the Groovy `code.executePythonStructured(...)` / `code.executePythonJson(...)` bindings. It carries `stdout`, `stderr`, an `exitCode`, `timedOut`, `durationMs`, a rendered `markdown`, and a list of generated **files** (`name`, `url`, `image`, `sizeBytes`). Files (a chart PNG, a generated PDF) are addressed with Turing's **`sandbox:` URL scheme** (the OpenAI convention): clients resolve them to a real download URL, and the underlying `/api/v2/code-interpreter/{session}/{file}` URLs are **HMAC-signed and cookie-bound** so they can't be replayed across browsers. This retires the old "regex-parse the markdown for file links" approach.

Agents can also declare extra Python dependencies (e.g. `reportlab matplotlib qrcode`) that Turing ensures are installed before running.

:::tip[Tool output feeding a slot]
A Code Interpreter file can land in a **multi-modal slot**. Slot types `IMAGE` / `AUDIO` / `FILE` hold an artifact's storage URL, so a generated chart or document becomes a first-class value in a [Chat Flow](/docs/chat-flow/#slots-the-conversations-memory).
:::

---

## External Tools via MCP Servers

Beyond the native tools, AI Agents can access tools from any external server implementing the **Model Context Protocol (MCP)**. This covers company-internal systems, proprietary data APIs, and the growing ecosystem of public MCP servers.

See [MCP Servers](/docs/mcp-servers/) for configuration details.

---

## Provider-native server tools

The tools above are **Turing's own**, Turing runs them and feeds results back to the model. Separately, some vendors expose **server-side built-in tools** that run inside the provider's infrastructure (the model searches the web, executes code, or fetches a URL without Turing doing the work). These are selected through the [capability](/docs/capabilities/) gate, not the agent's tool list, and they coexist with Turing/MCP tools in one tool loop.

| Function | OpenAI | Anthropic | Gemini |
|---|---|---|---|
| Web search | ✅ | ✅ | ✅ *(Google Search grounding)* |
| Web fetch / URL context | n/a | ✅ | ✅ |
| Code execution | ✅ | ✅ | ✅ |
| Image generation | ✅ | n/a | ✅ |
| Computer use | ✅ | ✅ | ✅ |
| Remote MCP | ✅ | ✅ | n/a |

Gemini's native primitives (Google Search grounding, URL Context, code execution, image generation, Computer Use, plus thinking budget, context caching, Batch, Files, full-context answering, and native video understanding) are documented in depth in [Generative AI → Gemini native primitives](/docs/genai-llm/#gemini-native-primitives-f16). For how server-native and Turing tools share one loop, and the two-level gate that controls them, see [Capabilities](/docs/capabilities/).

### Long and reasoning-heavy tool loops

Two cross-vendor request options shape the tool loop itself:

- **Interleaved thinking** (Anthropic) lets Claude reason *between* tool calls, not just before the first one: useful for multi-step agentic loops.
- **Background mode** (OpenAI Responses) runs a long tool loop asynchronously and resumes by polling, so a deep code-interpreter or computer-use loop doesn't hold the HTTP connection open.

Both are opt-in and fail-open. See [AI Agents → Reasoning, caching & background execution](/docs/ai-agents/#reasoning-caching--background-execution).

---

## Large results are offloaded automatically

Some tools return a lot: a big search dump, a long catalog, a wide JSON array. Pasting all of that into the prompt every turn is wasteful. When a tool result exceeds `turing.genai.tool-result-offload.inline-max-chars` (default **4096**), Turing writes the full payload to the [Agent Workspace](/docs/agent-workspace/) and replaces the inline result with a short `workspace://…` reference. The model pulls the data back **only when it needs it**, via an always-present `workspace_read` tool.

This applies uniformly to native, MCP, and [Custom](/docs/custom-tools/) tools, and is active only when a [storage backend](/docs/configuration-reference/#storage) is configured, so the default deployment behaves exactly as before. See [Agent Workspace → Auto-offload](/docs/agent-workspace/#2-auto-offload-of-large-tool-results) for details.

---

## Related Pages

| Page | Description |
|---|---|
| [AI Agents](/docs/ai-agents/) | How to compose agents with the tools they need |
| [Custom Tools](/docs/custom-tools/) | Author Groovy tools; the `code.executePython*` bindings |
| [Chat Flow](/docs/chat-flow/) | `functionCall` nodes and multi-modal slots fed by tool output |
| [MCP Servers](/docs/mcp-servers/) | Extend agents with external tools via MCP |
| [Agent Workspace](/docs/agent-workspace/) | Where large tool results are offloaded |
| [DSL Query API](/docs/dsl-query/) | Full reference for the Elasticsearch-compatible DSL |
| [DSL Compatibility Matrix](/docs/dsl-compatibility/) | Engine compatibility for all DSL features |
| [Assets](/docs/assets/) | Knowledge Base files queried by RAG tools |
| [Semantic Navigation](/docs/semantic-navigation/) | The search experience powering DSL tools |

---

