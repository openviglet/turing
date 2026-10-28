---
title: "Chat"
description: "The Viglet Turing Chat interface is where AI Agents meet customers. Direct LLM, Semantic Navigation grounded chat, and agent-per-tab, each tuned for a different conversion moment."
---
> *Every conversation is a chance to convert. The Chat interface is where Viglet Turing's reasoning, search, and brand voice meet your customer's question.*

The **Chat** interface is the front door of every AI capability in Turing. It's organized into three modes, each tuned for a different shape of conversation:

| Mode | When it shines | What grounds the answer |
|---|---|---|
| **Chat (direct LLM)** | The user wants raw assistance: *"summarize this PDF"*, *"draft this email"* | The LLM's parametric knowledge + any tools the user enables |
| **Semantic Navigation** | The user is looking for something *inside your indexed content* | Strict: only your indexed sites and documents |
| **AI Agent** *(one tab per agent)* | The user is having a purpose-specific conversation (sales, onboarding, support) | The agent's tools + persona + your content |

Every mode streams responses token by token, supports rich content (Markdown, code, charts, diagrams, HTML previews), and stores history in the user's browser, *not* on your servers.

:::note[LLM required]
The Chat interface is only available when at least one LLM Instance is configured and enabled. See [LLM Instances](/docs/llm-instances/) to set one up.
:::

---

## The Big Picture

The chat surface looks the same across modes, but each mode reaches a different backend and uses a different prompt strategy.

![Chat Interface: Layout Overview](/docs/img/diagrams/turing-chat-layout.svg)

**Header controls:**

| Control | Description |
|---|---|
| **Tab navigation** | Switch between Chat, Semantic Navigation, and the dynamic AI Agent tabs |
| **LLM model selector** | Pick the LLM Instance to run this conversation against |
| **New Chat** | Start a fresh session (the current one is saved automatically) |
| **Dark mode toggle** | Switch between light and dark themes; code highlighting follows |
| **Session history** | Browse, restore, or delete previous conversations from this browser |

**Context Bar**, sits below the message area:

| Indicator | Behaviour |
|---|---|
| **Token counter** | Shows `current/max` (e.g., `2.5k/128k`); estimated client-side at ~4 chars per token |
| **Progress bar** | Visual fill: **blue** under 60%, **yellow** at 60–79%, **red** at 80%+ |
| **Compact** | Compresses the conversation via the LLM to free context space |

---

## Mode 1: Chat (Direct LLM)

A general-purpose conversation with the selected LLM. Use this when the question doesn't depend on your enterprise content, the user wants the model itself, sometimes augmented with a few opt-in tools.

**Real conversations this mode handles well:**

- *"Rewrite this paragraph in a more formal tone."*
- *"Plot the rolling 30-day average of these numbers."*
- *"What's the weather in Lisbon next Tuesday?"*
- *"Find me an image of a Saturn V launch."*

### File Attachments

Drag-and-drop, or click the paperclip. Two paths, depending on the file:

| File type | How it's handled |
|---|---|
| **Documents** (PDF, DOCX, XLSX, PPTX, HTML, TXT, …) | Text extracted via **Apache Tika** and added to the prompt as context |
| **Images** (PNG, JPEG, WebP, GIF, …) | Sent directly as media to vision-capable models (Claude Sonnet, GPT-4o, Gemini) |

Attached files appear as badges on the message they're sent with. Multiple files per message are supported.

### Streaming

Every response streams in real time over **Server-Sent Events (SSE)**. The user sees tokens arrive as the model produces them, no spinner, no wait. Perceived latency drops by roughly half compared to wait-then-show.

### Tools You Can Enable

In direct LLM mode, the user toggles tools per conversation. The LLM decides when to invoke them.

| Tool | What it does | Latency typical |
|---|---|---|
| **Code Interpreter** | Runs Python in a sandbox (Matplotlib supported). 30s timeout. Files generated (charts, CSVs) come back as download links inline. | 1–10s |
| **Web Crawler** | Fetches and extracts a public web page. Up to 12,000 chars of body text and 30 links. | 1–3s |
| **Image Search** | Searches images via DuckDuckGo / Bing. Up to 8 results. | 1–2s |
| **Weather** | 1–7 day forecast for any location ([Open-Meteo](https://open-meteo.com)). | `<1s` |
| **Finance** | Stock quotes and historical prices via Yahoo Finance. | 1–2s |
| **Date / Time** | Current date/time in any timezone. | `<100ms` |
| **RAG Search** | Searches the [Knowledge Base](/docs/assets/) by semantic similarity; lists files; reads file contents. | 100–500ms |

The model picks which tool to call based on the question. The user enables the menu of options; the LLM picks within it.

---

## Mode 2: Semantic Navigation

This is where the chat becomes a *grounded* conversation: the LLM's job is no longer to answer from its training data, but to **search your indexed sites and explain what it finds**.

Use this mode when:

- The question is about your **products**, **internal documentation**, or **published content**.
- You need answers that **always trace back to a real document**.
- Hallucinations are unacceptable: the LLM is constrained to answer from indexed content.

The system prompt for this mode is built per request and includes:

- The list of available SN Sites and their locales,
- The facets configured on each site (so the model knows which filters it can use),
- An instruction to ground every answer in search results.

**Tools available:**

| Tool | Purpose |
|---|---|
| `list_sites` | Enumerates available SN Sites and their locales |
| `get_site_fields` | Returns the indexed fields and facets for a specific site |
| `get_valid_filter_values` | Lists valid values for a given facet field: prevents the model from inventing filters |
| `search_site` | Performs a semantic search and returns results with snippets and metadata |

Any **MCP Servers** configured globally are also available here, extending the tool set with external capabilities (e.g., a CRM lookup, a translation service).

:::tip[When to use SN mode vs. an Agent]
**SN mode** is the right answer when the user is *looking for something inside your content* but doesn't need a specific persona or workflow. It's the AI version of *"search the site, but smart"*. **Agent mode** is the right answer when the conversation has a *purpose*, booking a demo, qualifying a lead, getting onboarded. Both can search the same content; the agent layers persona + workflow on top.
:::

---

## Mode 3: AI Agents

Each [AI Agent](/docs/ai-agents/) configured and enabled in **Administration → AI Agents** appears as its own tab in Chat. The visitor picks the specialist that fits their need.

| What's per-agent | Set in |
|---|---|
| **Name & Avatar** | Agent → Settings tab |
| **System prompt** | Agent → Settings tab |
| **LLM Instance** | Agent → LLM tab |
| **Native tool selection** | Agent → Tools tab |
| **MCP servers** | Agent → MCP Servers tab |
| **Skills** *(portable capabilities)* | Agent → Settings (Skills toggle): see [Skills in Chat](#skills-in-chat) |
| **Persona** *(brand voice)* | Agent → Settings (Persona dropdown) |

The **Persona** is the voice layer: see [Personas](/docs/personas/) for full coverage. With a persona attached, every conversation in this tab speaks in your brand's tone, uses your mandatory vocabulary, avoids your forbidden vocabulary, and (if configured) draws on a **few-shot store of curated Q/A pairs** plus **live brand context from an MCP server**.

Use agents when you want consistency across thousands of conversations, *"every visitor who lands on the discovery agent should hear the same voice, regardless of LLM or time of day"*.

---

## Skills in Chat

A [**Skill**](/docs/skills/) is a portable, Anthropic-compatible capability, a folder of instructions, scripts, and reference material that runs in a hardened Docker sandbox. When an agent has skills enabled, the conversation can hand off a whole task to one of them ("generate the campaign assets", "build the financial model") without you wiring a tool for it.

Skills are **per-agent**: enable them with the **Skills** toggle in the agent's Settings tab (visible only when the feature is available). The native and MCP tools you select still work exactly as before: skills are an *additional* layer.

### How a skill runs inside a chat turn

The agent doesn't load the whole skill up front. It works by **progressive disclosure** plus a delegated sub-loop:

1. The agent's turn (running on the agent's own LLM) sees only each enabled skill's **name + description**, plus a single `run_skill(skill, task)` tool.
2. When the model decides a skill fits, it calls `run_skill`. Turing spins up a **sub-conversation on the Global Settings Default LLM**, which reads the skill's full instructions (`load_skill`) and drives a `bash` session in the skill's sandbox (`skill_bash`).
3. The sub-loop's result text comes back to the agent's turn as the tool result, and the agent weaves it into its reply.

:::note[A skill always runs on the Default LLM]
A portable skill should behave identically no matter who calls it, so it never runs on the agent's own model: it runs on the **Default LLM** configured in Global Settings. The agent orchestrates; the Default LLM executes the skill. Token usage for the sub-loop is recorded separately.
:::

### Skill mode: pin one skill for the conversation

By default every enabled skill is offered and the model picks. You can also pin **one** skill for a conversation using the **Skill mode** selector beside the model/flow selectors:

| Skill mode | Behavior |
|---|---|
| **Auto** (default) | All enabled skills offered; the model decides whether and which to use |
| **A specific skill** | Only that skill is offered: useful when the conversation *is* that capability |

The selector appears only when the agent has skills enabled and the catalog has at least one enabled skill.

:::note[Requirements]
Skills need a [storage backend](/docs/configuration-reference/#storage), the [Code Interpreter](/docs/tool-calling/#code-interpreter-1-tool) in **DOCKER** mode, and a configured Default LLM. When any is missing the agent behaves exactly as if Skills didn't exist. See the [Skills](/docs/skills/) page for the full picture.
:::

---

## Picking a Mode for the Job

If you take one thing away from this page, take this:

| The user is trying to… | Use this mode |
|---|---|
| Generate, summarize, or transform content (no enterprise grounding needed) | **Chat (direct LLM)** |
| Find or understand something inside your indexed content | **Semantic Navigation** |
| Do a *purposeful* conversation (sell, support, onboard, qualify) | **AI Agent** with a persona |
| Investigate a specific past conversation and what went well or wrong | Open the [Chat Analytics](/docs/chat-analytics/) page (separate, not in Chat) |

You don't have to pick one mode and stick with it. Most deployments use all three: direct chat for power users, SN chat for content exploration, and agents for customer-facing flows.

---

## How a Message Travels End-to-End

What actually happens between *the user pressing Enter* and *the response appearing*?

1. **Front-end** captures the message + any attachments and POSTs to the appropriate endpoint:
   - Direct LLM → `/api/v2/llm/{instanceId}/chat`
   - SN chat → `/api/v2/llm/{instanceId}/semantic-chat`
   - Agent → `/api/v2/ai-agent/{agentId}/chat`
2. **Spring AI** assembles the prompt: agent system prompt → persona overlays (if any) → tool definitions → conversation history → current message + media.
3. **Tool calling**: if the LLM requests a tool, Spring AI executes it (native or MCP), returns the result, and the loop continues until the model decides to answer.
4. **Streaming**: the response flows back through `Flux<ChatResponse>`. The front-end consumes it as SSE, rendering each token as it arrives.
5. **Persona post-validation**: `TurPersonaToneValidator` redacts any forbidden terms before the response is finalized.
6. **Analytics emission**: `TurChatAnalyticsService.recordSessionStart` / `recordTurn` / `recordSessionEnd` record turn count, token usage, and outcome to the analytics store. See [Chat Analytics](/docs/chat-analytics/).
7. **Browser** stores the session in IndexedDB, attaches an auto-generated title (LLM-summarized from the first exchange), and updates the session sidebar.

If observability is enabled (Prometheus scraping `/actuator/prometheus`), every step also emits metrics: see [Observability](/docs/observability/).

---

## Rich Content Rendering

Chat responses are rendered with full media-type awareness:

| Content type | Rendering |
|---|---|
| **Markdown** | GitHub Flavored: tables, strikethrough, task lists, inline code, blockquotes |
| **Code blocks** | Syntax highlighting via highlight.js with automatic light/dark theme switching |
| **D2 diagrams** | Rendered to SVG via WASM; falls back to a dev server in development |
| **HTML** | Sandboxed preview in an iframe; toggle between rendered view and source, with fullscreen |
| **Generated files** | Files from the Code Interpreter (charts, CSVs, processed data) appear as inline download links |

Code blocks pick up the chat's dark/light theme automatically, no flash on switch.

---

## Session History

Sessions are stored in the **browser's IndexedDB**. They never leave the user's machine.

What that means in practice:

- Sessions are **per browser and per device**, clearing browser data removes them.
- **No authentication** is required to access past sessions.
- **No server cost** for session storage.

**Session sidebar features:**

| Feature | Description |
|---|---|
| **Auto-title** | A short title is generated by the LLM from the first exchange; falls back to the first message text if generation fails |
| **Model badge** | Which LLM model ran the session |
| **Message count** | Number of messages |
| **Timestamp** | Date/time of the last message |
| **Restore** | Click to resume a previous session |
| **Delete** | Remove a session from local history |

Sessions are saved automatically after each complete response.

:::note[Browser-local vs. Chat Analytics]
The IndexedDB **session history** is a *user convenience*: it's how a single user finds their own past conversations. The **[Chat Analytics](/docs/chat-analytics/)** store is *operator analytics*, anonymized session metadata + transcripts that ship to MongoDB or Redis for dashboarding. They serve different audiences and don't depend on each other.
:::

---

## Context Window Management

A context bar at the bottom of the input shows token usage in real time:

```
2.5k / 128k  ████████░░░░░░░░░░░░
```

Tokens are estimated client-side at **~4 characters per token** (`Math.ceil(text.length / 4)`), counting the full message history. The bar's fill and color reflect how close you are to the model's context window.

### Resolving the Context Window Size

When the front-end first loads a session, it figures out the model's window via a three-tier fallback:

1. **Backend API**: `GET /v2/llm/{instanceId}/chat/context-info` returns the configured limit. The front-end caches it.
2. **LLM Instance configuration**: the `contextWindow` field set on the instance.
3. **Default**: 128,000 tokens, used when neither of the above is available.

### Progress Bar Colours

| Usage | Colour | Meaning |
|---|---|---|
| Below 60% | **Blue** | Plenty of room |
| 60–79% | **Yellow** | Consider compacting |
| 80%+ | **Red** | Compact before the limit |

### Compact

The **Compact** button (lightning bolt) becomes available when the conversation has at least **4 messages** and the model isn't currently generating. Compacting:

1. Sends the full history to the same LLM with a summarisation prompt.
2. The LLM produces a concise summary preserving facts, decisions, and context.
3. The conversation in IndexedDB is replaced with a single `**[Context compacted]**` block + the summary.
4. The next user turn continues from the compacted state, with significantly reduced token usage.

Compacting works in all three modes.

:::tip[When to compact]
When the bar turns **yellow**, compact proactively. By the time it's red, the model may have already started losing context from the earliest messages. The Compact tooltip shows how much context remains.
:::

---

## Files & Attachments Reference

| Capability | Detail |
|---|---|
| **Upload method** | Drag-and-drop onto the chat window, or click the attachment button |
| **Transfer format** | Multipart form (files sent together with the message) |
| **Document processing** | Apache Tika extracts text from PDF, DOCX, XLSX, PPTX, HTML, TXT, and more |
| **Image processing** | Passed directly as media bytes to vision-capable models |
| **Display** | Shown as file badges on the sent message bubble |

---

## API Endpoints

| Method | Endpoint | Mode |
|---|---|---|
| `POST` | `/api/v2/llm/{instanceId}/chat` | Direct LLM (SSE stream) |
| `POST` | `/api/v2/llm/{instanceId}/semantic-chat` | Semantic Navigation (SSE stream, tool-calling enabled) |
| `POST` | `/api/v2/ai-agent/{agentId}/chat` | Agent (SSE stream, full agent execution loop) |
| `GET` | `/api/v2/llm/{instanceId}/chat/context-info` | Context window size resolution |

See [REST API Reference → GenAI API](/docs/rest-api/#genai-api) for request/response shapes and examples.

### Per-call options on Direct LLM chat

Besides `messages`, a Direct LLM request can override the instance's own
settings for that call. Any field left out keeps the instance's value.

```json
{
  "messages": [{ "role": "user", "content": "List three colours as JSON." }],
  "temperature": 0,
  "topP": 0.9,
  "maxTokens": 200,
  "stop": ["END"],
  "responseFormat": "json_object"
}
```

`temperature`, `topP`, `maxTokens` and `stop` apply on every provider,
best-effort: a reasoning model that rejects sampling parameters is retried
without them. `responseFormat` takes `json_object`, or `json_schema` together
with `responseSchema` (the JSON schema) and an optional `strict`. JSON output
works on providers served over the OpenAI API (OpenAI, OpenAI-compatible
endpoints, Gemini's OpenAI endpoint, Mistral, Cohere). On any other provider
the request is refused with HTTP 400 naming `responseFormat`, never answered in
prose.

---

## Related Pages

| Page | Description |
|---|---|
| [AI Agents](/docs/ai-agents/) | The specialists that power the per-tab agent conversations |
| [Personas](/docs/personas/) | The voice layer applied to every customer-facing agent |
| [LLM Instances](/docs/llm-instances/) | Configure and connect the model providers |
| [Tool Calling](/docs/tool-calling/) | The 27 native tools and how they appear in chat |
| [Skills](/docs/skills/) | Portable, sandboxed capabilities an agent can hand a whole task to |
| [MCP Servers](/docs/mcp-servers/) | Connect external tool servers to chat |
| [Chat Analytics](/docs/chat-analytics/) | Investigate, classify, and learn from past conversations |
| [Observability](/docs/observability/) | Prometheus + Grafana dashboards over chat traffic |

---

