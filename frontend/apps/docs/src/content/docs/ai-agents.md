---
title: "AI Agents"
description: "AI Agents are purpose-built specialists you compose from an LLM, tools, and a persona. They are the conversion engine of Viglet Turing."
---
> *Hire a salesperson. Hire a solutions engineer. Hire an onboarding coach. Hire them all in an afternoon, and have them on duty 24/7 by sundown.*

An **AI Agent** in Viglet Turing is the specialist you don't have to recruit, train, or replace. It's a small, named bundle of:

- a **brain**, an [LLM Instance](/docs/llm-instances/) you've already configured,
- a set of **hands**: [native tools](/docs/tool-calling/) and [MCP servers](/docs/mcp-servers/) that let the agent actually *do* things (search your content, fetch live data, run code),
- a **voice**, a [Persona](/docs/personas/) that aligns the answers with your brand, your funnel stage, and your compliance posture.

Each agent appears as its own tab in the [Chat](/docs/chat/) interface. Visitors pick the specialist that matches their need; agents stay in their lane.

Configure agents in **Administration → AI Agents**.

---

## What "Agent" Actually Means

The word "agent" is overloaded. In Turing, an Agent has two non-negotiable properties:

1. **It's a composition, not a model.** You're not picking a model from a marketplace and renaming it. You're combining decisions (*which* LLM, *which* tools, *which* voice) into a single, deployable unit. Change any of the three, and you've changed the agent.
2. **It can act, not just answer.** The defining feature of an agent over a plain chatbot is its ability to call tools: search a knowledge base, look up a stock quote, run a Python script, query an MCP server. The model doesn't just generate words; it *requests an action*, Turing executes it, and the model reasons over the result.

Together, these mean an agent is a **business process expressed as a conversation**. A user asks; the agent searches your documentation, fetches their account data, summarizes the answer, and books a follow-up, in one exchange.

---

## Composition: The Four Layers

An AI Agent is built from four layers. Get all four right and the agent feels like a senior team member; get any one wrong and it feels like a generic chatbot.

![AI Agent: Composition](/docs/img/diagrams/turing-agent-composition.svg)

### 1. Identity

| Field | Description |
|---|---|
| **Name** | Display name shown as the tab label in the Chat interface. *"Sales Concierge"* beats *"Agent #3"*. |
| **Avatar** | Profile image. Even a small one moves trust meters, visitors react to faces, even illustrated ones. |
| **Description** | One-line explanation of the agent's purpose. Shown in admin lists; hints visitors what to expect. |
| **Enabled** | Toggle to activate or hide the agent. Disabled agents do not appear in the Chat interface. |

### 2. Brain

The **LLM Instance** is the agent's reasoning engine. You can attach **one or more** instances; at chat time the user (or front-end) picks which one to run.

Why multiple? Because the cost/quality tradeoff varies by question:

- A trivial routing question doesn't need a frontier model. Attach a fast, cheap instance (e.g., Gemini Flash, GPT-4o-mini) and let users default to it.
- A complex multi-step deal qualification benefits from a top-tier reasoner. Attach Claude Sonnet alongside, as an opt-in upgrade.

See [LLM Instances](/docs/llm-instances/) for vendor support, default models, and provider-specific options.

### 3. Capabilities

This is the largest configuration surface. Two sets of capabilities can be attached:

**Native tools**, 27 first-party tools, grouped into 8 categories:

| Category | Tools | When to attach |
|---|---|---|
| **Semantic Navigation** | `list_sites`, `search_site`, `get_document_details`, `find_similar_documents`, `search_by_date_range`, `get_site_fields`, `get_valid_filter_values`, … (15 total) | The agent should answer using your indexed enterprise content (CMS pages, products, internal documentation) |
| **RAG / Knowledge Base** | `search_knowledge_base`, `list_knowledge_base_files`, `get_file_content`, `knowledge_base_stats` | The agent should answer using files in [Assets](/docs/assets/) (PDFs, Word docs, manuals) |
| **Web Crawler** | `fetch_webpage`, `extract_links` | The agent should be able to read a public URL on demand (e.g., reference a customer's website) |
| **Finance** | `get_stock_quote`, `search_ticker` | Investor-facing or financial-research agents |
| **Weather** | `get_weather` | Travel, logistics, field-service agents |
| **Image Search** | `search_images` | Marketing, content-creation agents |
| **Date / Time** | `get_current_time` | Any agent that books, schedules, or references "now" |
| **Code Interpreter** | `execute_python` | Data analysis, chart generation, technical agents |

**MCP Servers**, external tool servers the agent can call:

| Badge | Transport | Typical use |
|---|---|---|
| **HTTP** (blue) | SSE over HTTP | A REST API your team owns; a brand-context server; a CRM lookup |
| **COMMAND** (amber) | stdio | A local script or process: useful for filesystem or shell access on the host |

See [Tool Calling](/docs/tool-calling/) for the full native tool reference and [MCP Servers](/docs/mcp-servers/) for connection setup.

:::caution[Lean tool lists win]
A common mistake: attaching every tool because *"it might be useful"*. Every additional tool consumes prompt tokens to describe to the model, and forces the model to choose among more options. **Fewer, sharper tools produce more precise tool calls and shorter latencies.** Start with 3–5 tools per agent and add only what an actual user request demanded.
:::

### 4. Voice

The **Persona** field attaches a [Persona](/docs/personas/) to the agent. The persona overlays:

- a system instruction (brand voice directive),
- tone, verbosity, language style,
- mandatory + forbidden vocabulary (enforced both pre- and post-LLM),
- optional few-shot examples retrieved from a vector store,
- optional live brand context from an MCP.

A persona is **optional**. An agent without one uses the LLM's default voice. For internal tools that's fine; for any customer-facing agent it's almost always wrong.

---

## Default AI Agent (global fallback)

One agent can be marked as the global **Default AI Agent** in Global Settings. When a Semantic Navigation site's chat is invoked but the site has **no agent of its own**, chat resolves to this default agent (using its LLM, embedding model and vector store) instead of reporting chat as disabled. This is **fail-open and gated**: it only happens when a default agent is actually configured, with none set, a site without its own agent keeps chat disabled exactly as before.

This is what lets a search-only site (for example the zero-config public demo, whose seed is search-only) answer grounded chat without wiring a per-site agent. A site that *does* bind its own agent always uses that one; the default is only a fallback.

Indexing follows the same rule: content imported into a site with no agent of its own is embedded into the Default AI Agent's vector store when that agent has RAG on (a `VECTORLESS_STRUCTURED` site is never embedded). Before 2026.3.6, import read only the site's own agent, so a site that answers through the Default AI Agent and was indexed on an earlier release holds no vectors: **reindex it** once after upgrading, or its chat will say the content is not available. The site's **Generative AI** page shows a notice when the site is set up for RAG but its vector store holds none of its content, with **Reindex all** beside it, also for a site that runs on the Default AI Agent.

---

## How an Agent Actually Runs

When a user sends a message, this loop runs end-to-end. Understanding it is the difference between debugging "the AI gave a weird answer" in five minutes vs. five hours.

![AI Agent: Execution Flow](/docs/img/diagrams/turing-agent-flow.svg)

### Step 1: Prompt assembly

Turing builds the prompt in this exact order:

1. **Persona system instruction** (if a persona is attached)
2. **Style guidelines block**: derived from the persona's tone + verbosity + language style
3. **Mandatory / forbidden vocabulary lists** (if any)
4. **Brand context**: fetched from the persona's MCP server, if configured
5. **Agent system prompt**: the agent's own purpose-specific instruction
6. **Tool definitions**: JSON schemas for every native tool and MCP tool the agent has access to
7. **Few-shot examples**: top-K Q/A pairs retrieved by similarity from the persona's few-shot store, if configured (only on the first user turn)
8. **Conversation history**: full message log so far
9. **Current user message**

Layers 1–6 don't change per turn after the first message. Spring AI, the underlying integration, handles the assembly; you don't write any of this manually.

### Step 2: LLM inference

The assembled prompt goes to the LLM Instance the user picked. The model receives it and decides one of two things:

- **Respond directly**: when the answer doesn't need any tool. Common for chit-chat, restated questions, simple factual recall.
- **Request a tool call**: when the answer requires action. The model emits a structured request: tool name + arguments.

This decision is the agent's "thinking step". Tool-capable models (Claude, GPT-4, Gemini) do this well; small or older models struggle and may hallucinate tool calls or skip them entirely.

### Step 3: Tool execution

If the model requested a tool, Turing:

1. Validates the call (does the agent have access to that tool? are the arguments valid JSON?).
2. Routes it to the correct executor: native tool implementation or MCP server.
3. Captures the result (or the error).
4. Logs the invocation: tool name, arguments, latency, response size.

### Step 4: Multi-step reasoning

The tool result is fed back to the model, which can:

- **Call another tool**: chain steps. For example: `search_knowledge_base` → `get_file_content` → `execute_python` to chart the data.
- **Stop and answer**: synthesize the results into a final response.

This loop runs until the model decides it has enough. Spring AI's `internalToolExecutionEnabled(true)` setting handles the loop transparently: the orchestration is built in.

### Step 5: Streaming the response

The final response streams back to the chat UI **token by token** via Server-Sent Events. The user sees the answer as it's being generated, the same way they'd watch someone typing. This isn't cosmetic, perceived latency drops by half versus waiting for a complete response.

### Step 6: Post-LLM persona enforcement

Before the response leaves the chat executor, **`TurPersonaToneValidator`** scans it for forbidden vocabulary from the persona. Matches are masked. The user never sees them. Logs record the event so you know your prompt isn't holding in some scenarios.

### Step 7: Analytics emission

A **chat session event** is recorded by `TurChatAnalyticsService` (turn count, token usage, agent ID, persona ID, locale, started-at, completed-at). This is the foundation for [Chat Analytics](/docs/chat-analytics/): the dashboard that tells you, days later, *which agents are converting and which aren't*.

---

## Composing Agents Around the Funnel

The biggest leverage in agent design is **funnel mapping**: build one agent per stage, not one agent for everything. Each agent gets a tighter system prompt, a more focused tool list, and a persona that fits the moment.

### The Discovery Agent

For visitors who arrived from a marketing campaign and don't yet know what they want.

| Layer | Configuration |
|---|---|
| **Name** | Discovery Concierge |
| **LLM** | A fast, cheap instance (Gemini Flash, GPT-4o-mini), discovery is high-volume |
| **Tools** | `search_site` (your marketing pages), `find_similar_documents` |
| **MCP Servers** | Optional: a CRM-lookup MCP that personalizes if the user is logged in |
| **Persona** | `top-of-funnel-sales`: EXECUTIVE tone, PERSUASIVE, verbosity 2 |
| **System Prompt** | *"You greet first-time visitors. Ask one qualifying question per turn. Don't list features; ask about outcomes. After three turns, offer a 15-min call."* |

**Conversion target:** **discovery → demo booking.**

### The Solutions Engineer Agent

For technical evaluators in active comparison.

| Layer | Configuration |
|---|---|
| **Name** | Solutions Engineer |
| **LLM** | A top-tier reasoner (Claude Sonnet, GPT-4): evaluators ask hard questions |
| **Tools** | `search_knowledge_base`, `get_file_content`, `fetch_webpage`, `execute_python` |
| **MCP Servers** | Internal API documentation MCP |
| **Persona** | `solutions-engineer`: TECHNICAL tone, INSTRUCTIONAL, verbosity 4 |
| **System Prompt** | *"You answer integration and architecture questions. Lead with the architectural answer. Confirm assumptions before recommending. Cite docs when possible."* |

**Conversion target:** **evaluation → technical buy-in.**

### The Onboarding Coach Agent

For customers who just signed up.

| Layer | Configuration |
|---|---|
| **Name** | Onboarding Coach |
| **LLM** | Mid-tier (Sonnet, GPT-4o): needs to follow conversational state precisely |
| **Tools** | `search_knowledge_base` (product docs), `get_file_content` |
| **MCP Servers** | Optional: account-state MCP that knows where the user is in the wizard |
| **Persona** | `onboarding-coach`: CASUAL, INSTRUCTIONAL, verbosity 3 |
| **System Prompt** | *"You guide brand-new users through their first day. Show one next step. Wait for confirmation. Celebrate small wins."* |

**Conversion target:** **signup → activation milestone.**

### The Internal IT Agent

For employees, on-premises, behind your firewall.

| Layer | Configuration |
|---|---|
| **Name** | IT Helpdesk |
| **LLM** | A local LLM via Ollama: keeps queries off third-party clouds |
| **Tools** | `search_knowledge_base` (IT runbook), `execute_python`, `get_current_time` |
| **MCP Servers** | Internal ticketing system (stdio MCP) |
| **Persona** | None: this one optimizes for precision, not voice |
| **System Prompt** | *"You answer IT questions for employees. Cite the runbook section. If the issue isn't covered, open a ticket via the MCP and reply with the ticket ID."* |

**Outcome target:** **first-call resolution rate.**

---

## Configuration Form

The agent form is split into four tabs.

### Settings

| Field | Required | Description |
|---|---|---|
| **Name** | Yes | Display name shown as the tab label in the Chat interface |
| **Avatar** | | Profile image: supports upload and removal |
| **Description** | | Brief explanation of the agent's purpose |
| **System Prompt** | | Instructions sent as a system message before every conversation. Defines purpose, scope, and behavior. |
| **Persona** | | Optional [Persona](/docs/personas/): overlays voice, vocabulary, brand context, and few-shot examples |
| **Enabled** | | Toggle to activate or deactivate the agent |
| **Submission retention** | | How long completed-conversation submissions are kept, see [Compliance & retention](#compliance--retention) |
| **Monthly budget (USD)** | | Soft cap on the agent's monthly AI spend: see [Cost governance](/docs/cost-governance/). Optional `Per-turn soft cap` + `Downgrade LLM` |
| **Disclose knowledge cutoff** | | Opt-in toggle: injects *"Your knowledge cutoff is &lt;date&gt;"* into the system prompt, using the configured model's cutoff from the public model catalog, so answers self-disclose how stale their knowledge may be. Off by default; a no-op when the catalog doesn't know the model's cutoff. |

:::note[Default system prompt]
If the system prompt is left blank, the agent uses the built-in default:

*"You are an AI assistant. Answer the user's questions using the tools available to you. If you have access to MCP server tools, use them when relevant to fulfill the user's request. If the user asks in a specific language, respond in that same language."*

This is fine for prototypes. For any customer-facing agent, write a real system prompt.
:::

### Live Preview & prompt diagnostics

The **System Prompt** page has a **Live Preview** that reverses the opaque assembled system message into labeled, color-coded segments (persona, agent prompt, MCP instructions, the active chat-flow addendum) rendered as one flowing article so you can read exactly what the model receives, top to bottom. It's also a diagnostic surface for *optimizing the turn*, what the model receives, what it costs, and what is wasted:

- **Flow-aware persona.** Pick a chat flow in the preview and the persona shown is the one the flow actually resolves to, a `persona` flow node that switches the active voice is reflected (labeled *"switched by flow"*), not silently ignored. If a flow's persona node points at a persona that isn't in the agent's catalog (or is audience-only), the conflict check warns you: that's a voice that would silently fall back to the default at runtime.
- **Token accounting & cost.** A metrics strip shows the total token estimate, the **cacheable-prefix vs per-turn split** (the stable portion a provider prefix cache can reuse across turns), and each segment's token count + share of the total. When the agent's LLM instance has a configured [price](/docs/cost-governance/), an approximate per-turn cost is shown.
- **"Inert this turn".** When a flow governs the turn, its addendum tells the model to ignore the agent prompt's welcome / first-interaction / routing scaffolding. The preview **dims those regions** and totals their token cost so you can see and trim the dead weight. Mark a section precisely with an author comment: `<!-- turing:concierge-only -->` (inert when a flow runs) or `<!-- turing:flow-only -->` (inert on a plain turn).
- **Cross-segment lint.** The conflict check runs *across* segments: it flags a rule repeated in more than one place (e.g. a forbidden-vocabulary list in both the persona and the agent prompt, you pay for it twice) and a directive stated one way in one segment and contradicted in another. A deeper **LLM-powered audit** is available on demand for semantic conflicts.
- **Node picker & simulated values.** Preview any point in a conversation, not just the flow's entry: pick a flow node and supply the collected slot values, and the addendum's "Already collected" line and next-step hint reflect that real mid-conversation turn.
- **Replay a real conversation.** Instead of *simulating* a turn, paste a **conversation id** into the "Replay a real conversation" box and the preview is rebuilt from that conversation's actual persisted state, its governing flow, cursor node, collected slots, and the persona the runtime really resolved (honouring any flow persona switch). The assembled prompt shown is exactly what the model would receive for that conversation's next turn, the ground truth for debugging a specific reply, not a synthesized turn. A **tool-call trace** below the article lists what the model actually called during that conversation (name, arguments digest, ok/error, duration), so you can read "what the prompt said" against "what the model did". (The conversation id is the value the SDK stores in the session cookie; find it on the conversation's inspector/export.)
- **Whole message list.** Beyond the system message, a **Message list** section shows the full ordered set of messages the model receives: the assembled system message, the *history-prefix* turns the runtime prepends from memory (a compressed summary of older turns, plus the earlier turns most relevant to the current message), then the recent conversation window. Each prefix turn is labeled with its source, so you can see the entire input, not just the system prompt.
- **Replay any *past* turn, verbatim.** Replaying a conversation (above) rebuilds the prompt from its *current* state: what the *next* turn would send. That is enough for most debugging, but by the time you inspect it the flow may have advanced, slots may have been overwritten, and memory may have been compacted, so it is not the exact prompt sent on an earlier turn. Enable **Prompt Capture (Replay)** on the agent's **Settings** tab and Turing saves the exact system message and whole-message list of every turn to object storage as it is sent. The replay bar then shows a **turn picker**: pick "Current state (reconstructed)" for the live rebuild, or any captured turn ("Turn N · timestamp") to load it **byte-for-byte**, the precise prompt the model received on that turn. Capture is off by default (it only writes when enabled), requires object storage to be configured, and is bounded automatically (a per-conversation turn limit, a per-entry size cap, and a retention window) so it can't grow without limit.

All of these are read-only and never change the assembled prompt: the preview stays a faithful mirror of what the model receives.

### LLM

Select one or more LLM Instances. The list shows each instance's title, description, vendor, and model name. The user (or the front-end) picks which one to use at chat time, from the agent's allowed set.

### Tools

Select which of the **27 native tools** are available. Tools are grouped by category: each group has a select-all checkbox for quick configuration.

### MCP Servers

Select which external MCP servers this agent can call. The list shows each server's title, description, and connection type.

---

## Provider-native tools (OpenAI Responses)

Turing's own tool catalog (the native tools above, plus MCP and Custom tools) works on **every** provider. Separately, when an agent runs on an OpenAI instance through the **Responses API path**, it can also use OpenAI's *server-side* built-in tools, the model runs them in OpenAI's cloud, not in Turing:

| Capability | Built-in tool |
|---|---|
| `openai-web-search` | Live web search |
| `openai-file-search` | Search over OpenAI-hosted vector stores |
| `openai-code-interpreter` | OpenAI's managed code sandbox (alongside Turing's own [Code Interpreter](/docs/tool-calling/)) |
| `openai-image-generation` | Inline image generation |
| `openai-mcp` | A remote MCP connector run by OpenAI |

These are governed by a **per-LLM-instance capability matrix**: each capability is switched on per instance. An **empty matrix means the unchanged Spring AI path**: nothing about the default deployment changes until you opt a capability in. **Anthropic and Gemini have their own server-side tools too** (web search, code execution, computer use, and more), the full cross-vendor matrix and the two-level gate that governs all of them live on [Capabilities](/docs/capabilities/).

---

## Reasoning, caching & background execution

Beyond *what tools* an agent can call, three cross-vendor request options tune *how the turn runs*, each opt-in per instance/agent (a [Request Option](/docs/capabilities/#request_option-capabilities)), each fail-open to the unchanged path when off. They reach parity across OpenAI, Anthropic, and Gemini behind shared seams.

### Extended thinking / reasoning budget

Give the model a reasoning budget and surface its thought summary in the **"Why this answer"** panel, one contract across all three vendors:

| Vendor | How |
|---|---|
| **OpenAI** | `reasoning-effort` (minimal/low/medium/high) + `reasoning-summary` |
| **Anthropic** | `anthropic-extended-thinking`: a positive `thinkingBudget` enables extended thinking (forces `temperature = 1`, as Anthropic requires); `interleavedThinking` lets Claude reason *between* tool calls in the loop |
| **Gemini** | `thinkingBudget` (0 disables, -1 dynamic) + thought summaries |

All three harvest the thought summary onto the shared `reasoning` SSE event, so it lands in the same panel with no UI change.

### Explicit context caching

The RAG cost lever: cache the stable prefix (system prompt + pinned grounding) once and reuse it across turns, conversations, and users at a steep discount. A vendor-neutral `TurContextCacheProvider` seam routes by vendor:

| Vendor | Mechanism |
|---|---|
| **Gemini** | A named, TTL'd remote `cachedContents` object (`gemini-context-cache` option), keyed so one prefix maps to one shared cache (~75% off) |
| **Anthropic** | *Inline* `cache_control` breakpoints on the system prompt + last grounding block (`cacheControlEnabled`, TTL `5m`/`1h`), nothing created remotely |
| **OpenAI** | Automatic prefix caching: no opt-in needed |

The default no-op provider returns empty, so an unconfigured vendor is byte-for-byte unchanged. Fail-open: a too-small prefix, quota, or any error → uncached turn, never a failed turn.

### Background execution (OpenAI)

Long reasoning/agentic turns (deep code-interpreter or computer-use loops) no longer hold the HTTP connection open: `openai-background` submits the Responses call with `background:true` and resumes by polling until terminal, freeing the connection, not the worker. Opt-in per instance (`backgroundEnabled`, poll interval + max-wait configurable); fail-open returns the latest response so text extraction still runs.

---

## Compliance & retention

A completed conversation writes a **submission** (the collected slots + outcome, see [Chat Flow](/docs/chat-flow/)). For LGPD/GDPR, each agent chooses how long to keep those:

| Setting | Behavior |
|---|---|
| `RETAIN_FOREVER` (default) | Keep submissions indefinitely |
| `RETAIN_DAYS` | Keep for `submissionRetentionDays`, then a daily cleanup job purges older ones |
| `DELETE_AFTER_EXPORT` | Treat the conversation export as the system-of-record archive and purge Turing's copy right after export |

The default preserves today's behavior; the other two are opt-in per agent. `pii_*` slots are additionally encrypted/redacted regardless of this setting.

---

## Memory & Workspace

Beyond the four composition layers, an agent has two optional runtime capabilities that make long, multi-step conversations work. Both are off by default and configured on the agent's **Settings** tab.

- **[Chat Memory](/docs/chat-memory/)**: persist the conversation, surface the most relevant older turns with BM25 retrieval, and compress very long histories into a cheap summary. Turn it on for any agent that holds extended conversations where context stated early still matters later.
- **[Agent Workspace](/docs/agent-workspace/)**: a per-conversation file store. Tools write drafts, reports, and charts to it; large tool results are offloaded to it automatically to keep the prompt small; the user downloads finished files from signed links. Requires a [storage backend](/docs/configuration-reference/#storage).

The two interlock: memory compression stores its summaries *in* the workspace. Neither changes an agent's behaviour until you enable it.

---

## REST API

### Agent Management

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/ai-agent` | List all agents (ordered by title) |
| `GET` | `/api/ai-agent/structure` | Empty structure template for a new agent |
| `GET` | `/api/ai-agent/{id}` | Get a specific agent |
| `POST` | `/api/ai-agent` | Create a new agent |
| `PUT` | `/api/ai-agent/{id}` | Update an existing agent |
| `DELETE` | `/api/ai-agent/{id}` | Delete an agent |

### Agent Chat

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v2/ai-agent/{agentId}/chat` | Stream chat response (SSE). Body: `{ llmInstanceId, messages[] }` |
| `GET` | `/api/v2/ai-agent/{agentId}/chat/context-info` | Context window size for the chosen LLM. Query: `llmInstanceId` |

### Native Tools

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/native-tool` | List all available tool groups, names, and descriptions |

---

## Diagnosing a Misbehaving Agent

| Symptom | Likely cause | Where to look |
|---|---|---|
| Agent gives generic answers, doesn't cite your content | No tool was called; the model didn't know to use one | Add a sharper system prompt directive: *"Always start by calling `search_knowledge_base` before answering."* |
| Agent uses brand names from a competitor or wrong product | Forbidden vocabulary not configured | Add the wrong terms to the persona's forbidden list |
| Agent's tone changes mid-conversation | No persona attached, or weak system prompt | Attach a persona; the post-LLM validator and prompt-side directives stabilize voice |
| Agent calls tools but ignores the results | Model is too small or the tool result is too large | Try a stronger LLM Instance; or trim the tool's output (e.g., reduce `find_similar_documents` top-K) |
| Conversion rate dropping over weeks | Few-shot store is stale; brand-context MCP returning old prices | [Chat Analytics](/docs/chat-analytics/) drill-down → check goal-achievement rate per persona |
| Latency spikes occasionally | A tool is slow; or the LLM provider is rate-limiting | [Observability](/docs/observability/) dashboards (`turing.llm.calls` timer) |

---

## Related Pages

| Page | Description |
|---|---|
| [LLM Instances](/docs/llm-instances/) | Configure the model providers used as agent brains |
| [Tool Calling](/docs/tool-calling/) | The 27 native tools, grouped and explained |
| [MCP Servers](/docs/mcp-servers/) | Connect agents to external tool servers |
| [Personas](/docs/personas/) | Give agents a brand voice |
| [Chat Memory](/docs/chat-memory/) | Persist, retrieve, and compress conversation history |
| [Agent Workspace](/docs/agent-workspace/) | Per-conversation file store for tool artifacts and offloaded results |
| [Chat Flow](/docs/chat-flow/) | Scripted conversational journeys attached to an agent |
| [Agent Evaluation](/docs/agent-eval/) | Regression-test an agent before publishing |
| [Cost Governance](/docs/cost-governance/) | The per-agent budget fields and AI-spend dashboard |
| [Chat](/docs/chat/) | The interface where agents come to life |
| [Chat Analytics](/docs/chat-analytics/) | Measure which agents are converting |
| [Observability](/docs/observability/) | Watch latency, token usage, and tool reliability in real time |

---

