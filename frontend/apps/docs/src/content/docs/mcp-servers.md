---
title: "MCP Servers"
description: "MCP works both ways in Viglet Turing: connect AI Agents to external tool servers (client), and expose Turing's own search/RAG/agent/analytics tools to Claude Desktop, Claude Code, or Cursor (server)."
---
The **Model Context Protocol (MCP)** is an open standard that lets AI models call tools hosted on external servers, turning any API, database, or internal system into a capability that an AI Agent can use autonomously. If you are an administrator looking to extend your agents beyond the 27 built-in tools, MCP Servers let you connect to virtually any external service without writing custom code inside Viglet Turing.

An **MCP Server** extends the capabilities of an AI Agent by connecting it to any external server that implements the **Model Context Protocol (MCP)**. This allows Turing agents to use tools defined completely outside the platform, a company-internal knowledge system, a proprietary data API, a database query interface, or any of the growing ecosystem of public MCP servers.

MCP Servers are configured in **Administration → MCP Servers** and then selected per [AI Agent](/docs/ai-agents/).

:::tip[MCP works both ways]
This first part of the page is about Turing as an MCP **client**, your agents calling *out* to external tool servers. Turing can also be an MCP **server**, exposing its own search/RAG/agent/analytics tools *in* to clients like Claude Desktop, Claude Code, or Cursor. Jump to [Turing as an MCP Server](#turing-as-an-mcp-server).
:::

---

## Configuration Form

| Field | Description |
|---|---|
| **Name** | Display name for the MCP server: shown when selecting it in an AI Agent |
| **Transport type** | `HTTP` or `stdio`: how Turing communicates with the server |
| **Command** | For `stdio`: the executable to launch the MCP server process (e.g., `npx`) |
| **Arguments** | For `stdio`: command-line arguments passed to the process (e.g., `@modelcontextprotocol/server-filesystem /data`) |
| **URL** | For `HTTP`: the MCP server endpoint URL |
| **Execution mode** | `Synchronous` or `Asynchronous` |

---

## Transport Types

### stdio

Launches the MCP server as a **local subprocess**. The MCP client communicates with it via standard input/output streams. This is the standard mode for locally installed MCP servers.

```bash
# Example: filesystem MCP server
npx @modelcontextprotocol/server-filesystem /path/to/allowed/directory
```

Use stdio when the MCP server is installed locally (via npm, pip, or a system binary) and does not expose an HTTP endpoint.

### HTTP

Connects to a **remote MCP server** over HTTP. This mode supports both public MCP services and internally hosted servers accessible over the network.

```
https://mcp.example.com/api/tools
```

Use HTTP when the MCP server is a hosted service or when it runs on a different machine or container.

:::note[Client hardening (`turing.mcp-client.*`)]
Because an MCP client server is admin-configured, two optional guardrails let you lock the client path down (both default to the permissive/legacy behavior so internal-network MCP keeps working):

- `turing.mcp-client.allowed-stdio-commands`: an allowlist of permitted stdio command base-names (e.g. `[npx, uvx]`). When set, any other `Command` is refused, blocking arbitrary local-process execution by configuration.
- `turing.mcp-client.block-private-urls`: when `true`, an HTTP MCP-client URL that resolves to a private/loopback address is refused via the SSRF egress guard.

See [Security Hardening § MCP client guards](/docs/security-hardening/#8-mcp-client-guards).
:::

---

## Execution Modes

### Synchronous

Waits for the MCP tool result before continuing the agent's reasoning chain. The LLM receives the tool result and can incorporate it into its next reasoning step before deciding whether to call another tool or generate a final response.

Use synchronous mode when the tool result is needed as context for subsequent steps.

### Asynchronous

Dispatches the tool call without blocking. Useful for long-running operations or when the result does not need to be incorporated into the immediate reasoning chain.

---

## MCP Server Examples

**Internal ticketing system (stdio):**
```
Name:      JIRA Internal
Transport: stdio
Command:   node
Arguments: /opt/mcp-jira/index.js --token <TOKEN>
Mode:      Synchronous
```

**Company knowledge API (HTTP):**
```
Name:      Internal Knowledge Base API
Transport: HTTP
URL:       https://internal-api.example.com/mcp
Mode:      Synchronous
```

**Public MCP service (HTTP):**
```
Name:      Brave Search
Transport: HTTP
URL:       https://api.search.brave.com/mcp
Mode:      Asynchronous
```

---

## Assigning MCP Servers to Agents

Once configured, MCP servers appear in the **MCP Servers** multi-select field of the [AI Agent](/docs/ai-agents/) configuration form. Each agent independently selects which MCP servers it can use. The tools exposed by each selected MCP server become available to the LLM during that agent's conversations.

---

## Turing as an MCP Server

Everything above is Turing reaching *out*. The reverse is just as useful: Turing can **publish its own tools as an MCP server**, so an external AI client (**Claude Desktop**, **Claude Code**, **Cursor**, or OpenAI's Responses API) can search your indexes, ask grounded RAG questions, invoke your agents, and read your analytics, all over the standard `/mcp` Streamable HTTP endpoint.

This is **off by default**. Enable it with:

```yaml
spring:
  ai:
    mcp:
      server:
        enabled: true   # default false: when off, none of the MCP-server beans exist
```

When disabled, the deployment is byte-for-byte unchanged.

### Published tools

The MCP server exposes the same tool implementations the internal Semantic-Navigation agent uses (one search implementation, not a fork), grouped by capability:

| Group | Tools | Scope |
|---|---|---|
| **Search** | `list_sites`, `get_site_fields`, `search_site`, `get_document`, `similar_documents`, `facet_search`, `autocomplete` | read |
| **RAG** | `rag_answer` (grounded answer with citations) | read |
| **Agents** | `list_agents`, `invoke_agent` | read |
| **Analytics** | `search_metrics`, `content_gaps`, `top_failed_searches` | read |
| **Write / ingestion** | `index_document`, `deindex_document`, `reindex_site` | **write (opt-in)** |

Read tools are always published. The write/ingestion tools appear **only** when you opt in (`turing.mcp-server.write-enabled=true`) **and** the caller holds the write scope per call: see [scoping](#tool-scoping) below.

### Security: loopback-first, then OAuth

Two layers protect the `/mcp` surface:

1. **Loopback gate (default on).** `turing.mcp-server.loopback-only=true` rejects any non-loopback request with HTTP 403 *before* the security chain even runs, the safe default for a Claude Desktop/Code client on the same machine. Set it to `false` to allow remote access (then rely on OAuth below).
2. **OAuth 2.1 resource server.** `turing.mcp-server.require-auth=true` turns `/mcp` into a JWT-bearer resource server (configure `spring.security.oauth2.resourceserver.jwt.*`). If you ask for auth but don't configure a decoder, the server **fails closed**: it denies all `/mcp` requests rather than running open.

### Tool scoping

Beyond the endpoint gate, each call is checked by a per-tool **scope policy** (defense in depth, independent of what the endpoint advertised). Read tools are allowed for any caller that cleared the gate; **write tools additionally require the write scope** (`SCOPE_mcp:write` by default, configurable). So even with write tools enabled, a read-only token can't mutate anything.

### One-click client config

To point a client at your server without hand-writing JSON, fetch a ready-made config:

```
GET /api/v2/mcp/client-config
```

It returns the connection block to drop into Claude Desktop / Claude Code / Cursor (and the equivalent for OpenAI Responses), pre-filled with your endpoint.

### Resources & prompts

The server also publishes MCP **resources** and **prompts** (not just tools), so a client can discover canned prompts and readable resources alongside the callable tools.

---

## Related Pages

| Page | Description |
|---|---|
| [AI Agents](/docs/ai-agents/) | Compose agents with native tools + MCP servers; `invoke_agent` exposes them over MCP |
| [Tool Calling](/docs/tool-calling/) | The native tools built into Turing |
| [Semantic Navigation](/docs/semantic-navigation/) | The search surface the MCP read tools delegate to |
| [Chat Analytics](/docs/chat-analytics/) | The data behind `search_metrics` / `content_gaps` |
| [Chat](/docs/chat/) | Front-end chat interface where agents are used |

---


