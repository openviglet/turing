---
title: "Capabilities"
description: "The capability registry and two-level gate in Viglet Turing: how provider-native server tools, request options, and Turing's own tools surface in one taxonomy and run in one tool loop."
---
A **capability** is anything a Large Language Model can *do beyond plain text generation*: search the web, run code, generate an image, return citations, or reason with a larger compute budget. Different vendors expose these as server-side primitives with different names and shapes; Viglet Turing collapses them into **one taxonomy** so the admin sees "web search" once, not "OpenAI `web_search` vs Anthropic `web_search_20250305` vs Gemini `google_search`."

You'd reach for this page when you want an agent to use a **provider-native** feature (Claude's web search, OpenAI's hosted code interpreter, Gemini's Google Search grounding) rather than (or alongside) Turing's own [native tools](/docs/tool-calling/). The key idea to internalize: a native capability only fires when it is switched on in **two** places. Miss either gate and the request silently falls back to the standard Spring AI path, as if the capability didn't exist.

---

## Why two gates?

A capability is gated **per LLM instance** *and* **per agent** on purpose:

1. **The instance gate (admin/ops)** decides *whether this connection is even allowed* to use the feature: an operator question about cost, data residency, and contractual access. It lives in the `llm_instance_capability` matrix.
2. **The agent gate (author)** decides *whether this particular assistant should* use it, a design question about what the agent is for. It lives in the agent's `nativeCapabilities` selection.

At runtime Turing intersects the two sets: **what the agent asked for ∩ what the instance is allowed to do**. Only capabilities in that intersection are wired into the provider request.

```mermaid
flowchart LR
    A[Agent picker<br/>nativeCapabilities] -->|selected| I{∩ intersect}
    B[Instance matrix<br/>llm_instance_capability] -->|enabled| I
    I -->|in both| W[Wired into the native request]
    I -.->|missing either gate.-> F[Silent Spring AI fallback]
```

:::caution[The most common gotcha]
The picker checks the bound instance's **vendor**, not its capability matrix, so you can select a capability that no instance actually has enabled and it will simply never fire. If a native capability "isn't working," confirm it is enabled in **both** the instance matrix and the agent picker.
:::

---

## From zero: turn on Claude web search for an agent

1. **Enable it on the instance.** Open your Anthropic LLM instance (`/admin/llm/instance`). In the **Capabilities** section, toggle on **Web Search**. (Behind the scenes this `PUT`s `/api/llm/{instanceId}/capability/anthropic-web-search`.)
2. **Select it on the agent.** Open the agent's **Tools & Capabilities** picker (`/capabilities`). Under the **Web** group, pick the Anthropic web-search option. (This writes the key to the agent's `nativeCapabilities`.)
3. **Chat.** Ask a question that needs fresh information. Claude now runs web search server-side and the answer streams back with citation events.

If nothing changed, re-check step 1: the instance gate is the one most often forgotten.

---

## The capability taxonomy

Every capability carries a **kind** that decides which UI surface it appears on. This keeps ~40 capabilities from collapsing into one undifferentiated list.

| Kind | What it is | Where it surfaces |
|---|---|---|
| **TOOL** | The model *invokes* it mid-turn (web search, code interpreter, image gen, plus Turing's own native tool groups). | The agent's **Tools & Capabilities** picker |
| **REQUEST_OPTION** | Changes *how* the call is made; never invocable (reasoning effort, citations, service tier, context caching…). | Agent **Request Options** settings toggles |
| **PLATFORM** | Not per-agent at all (usage import, evals, distillation, nightly batch). | Their own admin screens |

Each `TOOL`/`REQUEST_OPTION` descriptor also carries:

| Field | Meaning |
|---|---|
| `key` | Stable identifier stored in the matrix / picker (e.g. `anthropic-web-search`) |
| `function` | Abstract function the capability fulfils (e.g. `web_search`). Capabilities sharing a function are **mutually exclusive** in the picker, you pick *one* provider to fulfil a function. |
| `provider` | The vendor that executes it (`OPENAI` / `ANTHROPIC` / `GEMINI` / `ANY`) |
| `category` | Coarse grouping for visual layout (Web, Code, Media, Reasoning…) |
| `ownsTurn` | When `true`, the capability takes the *whole* turn (e.g. Computer Use, Realtime Voice) |
| `valueType` / `options` | For `REQUEST_OPTION` rows: `BOOLEAN` toggle, or `SELECT` over `options` |

The merged list is served by `GET /api/capability/registry`; both UIs render from it.

---

## TOOL capabilities (provider-native)

These are the server-side tools a model invokes during a turn, grouped by abstract function. Within a function you choose **one** provider.

| Function | OpenAI | Anthropic | Gemini |
|---|---|---|---|
| **Web search** | `openai-web-search` | `anthropic-web-search` | `gemini-google-search` *(grounding)* |
| **Web fetch / URL context** | n/a | `anthropic-web-fetch` | `gemini-url-context` |
| **Code execution** | `openai-code-interpreter` | `anthropic-code-execution` | `gemini-code-execution` |
| **Image generation** | `openai-image-generation` | n/a | `gemini-image-generation` |
| **Computer use** *(owns turn)* | `openai-computer-use` | `anthropic-computer-use` | `gemini-computer-use` |
| **Remote MCP** | `openai-mcp` | `anthropic-mcp` | n/a |
| **Text editor** | n/a | `anthropic-text-editor` | n/a |
| **Bash** | n/a | `anthropic-bash` | n/a |
| **Memory** | n/a | `anthropic-memory` | n/a |
| **Realtime voice** *(owns turn)* | `openai-realtime-voice` | n/a | n/a |

:::note[Owns-turn capabilities]
**Computer Use** and **Realtime Voice** take the entire turn rather than coexisting with other tools. Computer Use across all three vendors runs through one vendor-neutral `TurComputerUseDriver` seam and ships against a **no-op driver**: it is matrix-selectable without bundling a browser, and only acts when a real driver bean is configured. See [Tool Calling](/docs/tool-calling/) and [AI Agents](/docs/ai-agents/).
:::

---

## REQUEST_OPTION capabilities

Request options never appear in the tool picker: they tune *how* the call is made and render as toggles/dropdowns on the agent's **Request Options** page. They are stored per-agent as a JSON map (`requestOptionsJson`) keyed by capability key.

| Key | Label | Provider | Type |
|---|---|---|---|
| `reasoning-effort` | Reasoning Effort | OpenAI | SELECT: minimal / low / medium / high |
| `reasoning-summary` | Reasoning Summary | OpenAI | SELECT: auto / concise / detailed |
| `logprobs` | Log Probabilities | OpenAI | boolean |
| `structured-outputs` | Structured Outputs | Any | boolean |
| `predicted-outputs` | Predicted Outputs | OpenAI | boolean |
| `service-tier` | Service Tier | OpenAI | SELECT: auto / default / flex / priority |
| `token-counting` | Token Counting | Anthropic | boolean |
| `stored-completions` | Stored Completions | OpenAI | boolean |
| `citations` | Citations | Anthropic | boolean |
| `native-pdf` | Native PDF Grounding | Anthropic | boolean |
| `context-editing` | Context Editing | Anthropic | boolean |
| `compaction` | Compaction | Anthropic | boolean |
| `moderation` | Moderation Pre-filter | OpenAI | boolean |
| `safety-identifier` | Safety Identifier | OpenAI | boolean |
| `gemini-context-cache` | Context Caching | Gemini | boolean |
| `openai-background` | Background Mode | OpenAI | boolean |
| `anthropic-extended-thinking` | Extended Thinking | Anthropic | boolean |

:::note[A toggle is a home, not a wire]
Switching a request option on stores your choice; the *feature* engages when the corresponding provider request-builder reads its key from `requestOptionsJson`. Several of these (citations, native PDF, context caching, extended thinking, background mode) are documented in depth on [RAG](/docs/rag/), [Tool Calling](/docs/tool-calling/), and [Generative AI & LLM Configuration](/docs/genai-llm/).
:::

---

## The union tool loop

When an agent mixes provider-native tools with Turing's own (DSL search, RAG, web crawler…) and MCP tools, they all run in **one** tool loop rather than competing. The resolver:

- intersects the agent's selected capabilities with what the instance allows;
- adds the agent's coexisting Turing/MCP/custom tools, **dropping** any Turing tool whose abstract `function` is already claimed by a selected provider-native pick (so you don't get both `search_images` and native `image_generation` fighting over the same function);
- hands the combined set to the vendor's native chat path (OpenAI Responses `function` tools, Anthropic `tool_use`/`tool_result`).

:::note[Legacy agents are untouched]
An agent whose `nativeCapabilities` is **null** (never opened the picker) keeps the pre-capability behaviour exactly, all of the instance's enabled capabilities, zero Turing tools. The two-gate intersection only takes over once the picker writes a selection. Existing agents change nothing until you choose to.
:::

---

## Admin & API reference

### Instance capability matrix

The admin gate, on the LLM instance edit form (existing instances only; rows are filtered to the vendor's plugin type).

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/llm/{instanceId}/capability` | List capabilities and their enabled state for an instance |
| `PUT` | `/api/llm/{instanceId}/capability/{key}` | Enable/disable a capability (body: `enabled`, optional `configJson`) |
| `DELETE` | `/api/llm/{instanceId}/capability/{key}` | Remove a capability row |

`configJson` carries per-capability settings, e.g. the Anthropic MCP connector's `serverUrl` / `serverLabel` / `authToken` / `allowedTools`.

### Registry

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/capability/registry` | The full tagged descriptor list both UIs render from |
| `GET` | `/api/capability/matrix` | Per-instance capability rows (instance id, title, plugin type, enabled keys) |

:::caution[Don't confuse with "Supported Capabilities"]
The instance form's free-text **Supported Capabilities** field (e.g. `RESPONSE_FORMAT_JSON_SCHEMA`) is a different, unrelated thing: it is *not* the native-capability matrix. See [LLM Instances → Advanced Options](/docs/llm-instances/#5-advanced-options-amber).
:::

---

## Related Pages

| Page | Description |
|---|---|
| [LLM Instances](/docs/llm-instances/) | Where the instance capability matrix lives, plus the 11 vendors |
| [Tool Calling](/docs/tool-calling/) | Turing's own native tools and how they coexist with provider tools |
| [AI Agents](/docs/ai-agents/) | Composing an agent from an LLM, tools, and capabilities |
| [MCP Servers](/docs/mcp-servers/) | Remote tool servers (the `mcp` capability) |
| [Generative AI & LLM Configuration](/docs/genai-llm/) | How capabilities fit the broader GenAI surface |

