---
title: "Agent Workspace"
description: "A per-conversation blob store that lets an agent build, keep, and hand back files (drafts, reports, charts, offloaded tool results) without bloating the prompt."
---
> *Give the agent a desk. Let it keep its notes, drafts, and the report it just built, and hand the finished file back to the user when it's done.*

A plain chatbot has nowhere to put anything. Everything it "knows" mid-conversation lives in the prompt, and the moment the context window fills up, the early work scrolls off and is gone.

The **Agent Workspace** is the fix: a **per-conversation, key-value blob store** the agent can write to and read from across turns. A tool builds a CSV on turn 3; turn 9 reads it back. A large search result is parked there instead of being pasted into the prompt. A long conversation is summarized and the summary is filed away. The user downloads the finished PDF from a signed link.

Crucially, **workspace contents are not dumped into the prompt.** The agent works with *references* (`workspace://…`) and pulls the bytes back only when it actually needs them, the "offload" pattern that keeps long, file-heavy conversations cheap.

---

## Why this is more than a scratchpad

Other agent frameworks expose a "virtual filesystem" as an in-memory implementation detail. Turing's workspace is a thin façade over the same pluggable [Storage](/docs/configuration-reference/#storage) layer that powers [Assets](/docs/assets/), which buys it properties an in-memory scratchpad can't have:

| Capability | In-memory scratchpad | Turing Agent Workspace |
|---|---|---|
| Storage backend | Process memory | **Pluggable**: None / Filesystem / MinIO / S3, per deployment |
| Multi-tenant isolation | Implicit | **Path-isolated** by agent **and** conversation |
| External access | None | **HMAC-signed, time-limited URLs** the user can download |
| Content types | Strings | **Binary blobs** with a MIME type: PDFs, images, CSVs |
| Survives a restart | No | Yes (Filesystem / MinIO backends) |

---

## Prerequisite: enable storage

The workspace requires a storage backend. With the default `turing.storage.type: none` there is nowhere to put blobs, so the workspace is inert (and the dependent features below stay dormant, existing deployments are unchanged).

```yaml
turing:
  storage:
    type: filesystem        # none | filesystem | minio
    filesystem:
      path: ./store/assets
```

See [Configuration Reference → Storage](/docs/configuration-reference/#storage) for the MinIO and filesystem options. No other configuration is needed, the workspace reuses whatever storage Assets already use.

---

## How isolation works

Every blob is written under a tenant-isolated path:

```
{storage-root}/tenants/{agentId}/{conversationId}/workspace/{key}
```

- Two conversations of the **same agent** never see each other's files.
- Two **agents** in the same Turing instance never see each other's files.
- Keys may contain `/` to express folders (`reports/lead-export.csv`); path-traversal sequences (`..`, absolute paths) are rejected before any storage call.
- A blob is served only through an **HMAC-signed URL** that binds the agent + conversation + key + expiry, so a leaked link can't be replayed for a different file or after it expires.

---

## What lives in the workspace

The workspace is written and read by four features. The first is for tool authors; the rest are automatic.

### 1. Custom Tools: the `workspace` binding

A [Custom Tool](/docs/custom-tools/) Groovy script receives a pre-scoped `workspace` object bound to the active agent + conversation. Use it to persist an artifact and hand the user a download link:

```groovy
// Build a CSV with the code helper, then file it in the workspace:
def r = code.executePythonStructured('''
    import csv
    with open('report.csv', 'w') as f:
        csv.writer(f).writerow(['lead', 'score'])
''')
def bytes = http.getBytes(r.files()[0].url())
workspace.put("reports/lead-export-${slots.get('export_date')}.csv", bytes, "text/csv")

// Later turn: read it back, or hand the LLM a signed download URL:
def previous = workspace.getText("reports/lead-export-2026-06-03.csv")
return "Your export is ready: " + workspace.url("reports/lead-export-2026-06-03.csv")
```

| Method | Returns | Purpose |
|---|---|---|
| `workspace.put(key, value)` | n/a | Store a UTF-8 string (`text/plain`) |
| `workspace.put(key, bytes, contentType)` | n/a | Store a byte blob with an explicit MIME type |
| `workspace.get(key)` | `byte[]` / `null` | Read raw bytes (null if absent) |
| `workspace.getText(key)` | `String` / `null` | Read as a UTF-8 string |
| `workspace.list(prefix)` | `List` | List artifacts under a prefix (recursive); no-arg lists all |
| `workspace.delete(key)` | n/a | Remove a blob |
| `workspace.url(key)` | `String` / `null` | Signed, time-limited download URL |

:::note[No-op outside a conversation]
When a tool is exercised from the admin preview or a unit test (no live conversation), `workspace` degrades to a safe no-op: writes log and return, reads return `null`, `list` returns empty. A script that mixes `workspace` and `slots` still previews cleanly.
:::

### 2. Auto-offload of large tool results

When **any** tool (native, MCP, or Custom) returns a result larger than `turing.genai.tool-result-offload.inline-max-chars` (default **4096**), Turing automatically:

1. writes the full payload to `tool-results/{toolName}-{timestamp}.json` in the workspace, and
2. replaces the inline result the model sees with a short reference:

   > `Stored at workspace://tool-results/search_site-1717430400000-7.json (40 KB). Call workspace_read with key="…" to read the full result when you need it.`

The model gets the data back on demand through an always-present platform tool, **`workspace_read`**: it calls it only when it actually needs the content. The effect: a tool that returns a 40 KB catalog costs ~30 characters of prompt per turn instead of 40 KB, with the full payload still one tool call away (and on the audit trail).

```yaml
turing:
  genai:
    tool-result-offload:
      enabled: true          # default true
      inline-max-chars: 4096 # results larger than this are offloaded
```

Offloading is active only when storage is enabled. With `turing.storage.type: none` results are always left inline and `workspace_read` is not added, so the default deployment is byte-for-byte unchanged. See [Tool Calling](/docs/tool-calling/) for the native tool catalog.

### 3. Memory compression summaries

When an agent has [Chat Memory](/docs/chat-memory/) compression enabled, the summary of the older conversation turns is stored as `memory/summary-1-{n}.md` in the workspace, both as the artifact the prompt references and for audit. See [Chat Memory → Memory compression](/docs/chat-memory/#memory-compression) for the full flow.

### 4. Live artifact stream ("files this agent built for you")

As the agent writes and deletes blobs, those changes are published on a per-conversation **Server-Sent Events** stream: **metadata only** (key, content type, size, signed URL), never the bytes. A front-end can subscribe and render a live sidebar of the files the agent has produced this conversation.

The [React SDK](/docs/react-sdk/) ships this ready to use:

- `subscribeWorkspaceSse(...)` / the `useTuringWorkspace<T>` hook consume the stream,
- `TuringWorkspacePanel` renders the artifact list with download links.

```
GET /api/v2/chat/workspace/stream?conversationId={id}&agentId={id}
```

---

## REST API

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v2/workspace/file` | Serve a blob. Requires the signed query string (`agentId`, `conversationId`, `key`, `exp`, `sig`) produced by `workspace.url(...)`. |
| `GET` | `/api/v2/chat/workspace/stream` | SSE stream of artifact metadata for a conversation. Query: `conversationId`, `agentId`. |

Signed URLs are the only way to read a blob from outside the chat runtime; an unsigned or expired request is rejected.

---

## Configuration

| Property | Default | Description |
|---|---|---|
| `turing.storage.type` | `none` | Must be `filesystem` or `minio` for the workspace to be active |
| `turing.genai.tool-result-offload.enabled` | `true` | Auto-offload large tool results to the workspace |
| `turing.genai.tool-result-offload.inline-max-chars` | `4096` | Result size (chars) above which a tool result is offloaded |

---

## Related Pages

| Page | Description |
|---|---|
| [AI Agents](/docs/ai-agents/) | The agent the workspace is scoped to |
| [Custom Tools](/docs/custom-tools/) | Author tools that read and write the workspace via the `workspace` binding |
| [Tool Calling](/docs/tool-calling/) | Native tools whose large results are auto-offloaded |
| [Chat Memory](/docs/chat-memory/) | Memory compression stores its summaries in the workspace |
| [React SDK](/docs/react-sdk/) | `useTuringWorkspace` hook + `TuringWorkspacePanel` |
| [Configuration Reference](/docs/configuration-reference/#storage) | Storage backend setup |

---

