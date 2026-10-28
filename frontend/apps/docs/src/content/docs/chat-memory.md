---
title: "Chat Memory"
description: "Persist conversation history per agent, surface the most relevant older turns with BM25 retrieval, and compress long histories into a workspace-stored summary."
---
> *A good assistant remembers what you told it on turn 3, even on turn 30.*

By default a chat turn only knows what the front-end sends it: the recent message window. For long conversations that means context stated early (a preference, a constraint, an account number) scrolls out of view and the agent starts to feel forgetful.

**Chat Memory** gives an agent three escalating capabilities, each **opt-in per agent** so existing agents are unaffected until you turn them on:

1. **Persistence**: every turn is written to a durable store, keyed by `conversationId`.
2. **Relevance retrieval**: older turns relevant to the current question are pulled back into the prompt with BM25.
3. **Memory compression**: when the older history grows large, it's summarized in one cheap LLM call and the summary (not the raw turns) carries the background context.

Configure all three on the agent, under **Administration → AI Agents → Settings**.

---

## 1. Persistence

Persistence is the foundation: relevance retrieval and compression both read from it.

Turn it on with **`chatMemoryEnabled`** on the agent, and point the platform at a store with **`turing.logging.engine`**:

```yaml
turing:
  logging:
    engine: mongodb     # none | mongodb | redis
```

With the engine set to `none`, persistence is a no-op regardless of the agent toggle. With `mongodb` or `redis`, each conversation becomes one document/key (`chat:{conversationId}`) holding its role-tagged turns.

| Agent setting | Default | Description |
|---|---|---|
| `chatMemoryEnabled` | `false` | Persist this agent's turns to the configured engine |
| `chatMemoryFlushIntervalMinutes` | `5` | How often the in-memory write buffer is flushed to the store (turns are batched, not one round-trip per token) |
| `chatMemoryMaxMessages` | `100` | Hard cap on messages kept per conversation; older turns are trimmed |

:::note[Shared with the Logging engine]
Chat memory reuses the same `turing.logging.engine` backend as server/indexing logs. See [Logging](/docs/logging/) and [Configuration Reference](/docs/configuration-reference/) for engine setup.
:::

---

## 2. Relevance retrieval

Persistence alone gives the model a chronological FIFO of the last *N* turns. **Relevance retrieval** adds the most *on-topic* older turns back on top of that window.

When enabled, on each turn Turing:

1. reads the persisted history,
2. splits it into a **recent window** (the last `chatMemoryRecentN` turns, kept verbatim) and an **older pool** (everything before),
3. builds a tiny in-memory BM25 index over the older pool and scores it against the latest user message,
4. prepends the top `chatMemoryRelevanceTopK` matches (each tagged with a `[earlier turn #… at …]` marker so the model can place it on the timeline) ahead of the recent window.

The cost is a few milliseconds per turn (the index is built and discarded per request); the payoff is an agent that recalls a constraint from turn 3 because the current question is about it.

| Agent setting | Default | Description |
|---|---|---|
| `chatMemoryRelevanceEnabled` | `false` | Turn on BM25 retrieval of older turns |
| `chatMemoryRelevanceTopK` | `5` | How many older turns to retrieve (capped at 20) |
| `chatMemoryRecentN` | `10` | Size of the recent window kept verbatim; only turns **older** than this are scored |

Relevance retrieval requires `chatMemoryEnabled` (there's nothing persisted to retrieve from otherwise).

---

## Memory compression

Relevance retrieval surfaces *specific* high-signal turns. But a very long conversation also carries a lot of *low-signal* background that's expensive to keep shipping verbatim. **Memory compression** handles that remainder: it summarizes the older pool once, and the compact summary stands in for those turns.

### How it works

When `chatMemoryCompressionEnabled` is on and the older pool (turns past the `chatMemoryRecentN` window) is estimated to exceed `chatMemoryCompressionThresholdTokens`:

1. those older turns are summarized in **one dedicated LLM call** (using a deliberately cheap model, see below),
2. the summary is stored in the [Agent Workspace](/docs/agent-workspace/) at `memory/summary-1-{n}.md`,
3. a compact summary block is prepended to the prompt **ahead of** the relevance-retrieved turns and the recent window.

It **composes with relevance retrieval**: relevance still surfaces the exact older turns that match the current query (high-signal recovery), while the summary is the cheap default for everything else.

### Synchronous vs. background

By default compression runs **off the hot path** (`chatMemoryCompressionAsync: true`): the turn that triggers a refresh proceeds *uncompressed* (reusing the last summary if one exists) and a background worker regenerates the summary for the **next** turn. This means no chat turn ever blocks on the summary LLM call. The trade-off is *eventual* compression: the first turn past the threshold isn't compressed yet.

Set `chatMemoryCompressionAsync: false` to summarize **synchronously**: the summary applies on the very turn it becomes due, at the cost of that turn waiting on the LLM call.

Either way, the call is **throttled to once per `chatMemoryCompressionInterval`** per conversation, so a long chat doesn't pay for a summary every turn.

| Agent setting | Default | Description |
|---|---|---|
| `chatMemoryCompressionEnabled` | `false` | Summarize the older pool when it exceeds the threshold |
| `chatMemoryCompressionThresholdTokens` | `8192` | Older-pool token estimate above which a summary is generated |
| `chatMemoryCompressionLlmId` | *(unset)* | LLM instance used for the summary; unset → the Global Settings default LLM |
| `chatMemoryCompressionInterval` | `PT1H` | Minimum [ISO-8601 duration](https://en.wikipedia.org/wiki/ISO_8601#Durations) between summaries for one conversation |
| `chatMemoryCompressionAsync` | `true` | Run the summary call in the background (true) vs. synchronously on the turn (false) |

:::tip[Pick a cheap model for the summary]
The summary doesn't need a flagship reasoner: set `chatMemoryCompressionLlmId` to a fast, inexpensive instance (e.g. GPT-4o-mini, Gemini Flash). It runs at most once per interval and only on long conversations.
:::

Compression requires both `chatMemoryEnabled` and a storage backend (the summary is written to the workspace). With `turing.storage.type: none` the summary is kept in memory only for the current process.

---

## How the three layers stack in the prompt

For a long, opted-in conversation, the history the model receives is assembled in this order:

```
[ memory summary of turns 1–N ]        ← compression (low-signal background)
[ earlier turn #… ] (BM25 matches)     ← relevance retrieval (high-signal recovery)
… recent window (last chatMemoryRecentN turns, verbatim) …
current user message
```

Each layer is independent and opt-in: persistence with neither retrieval nor compression is a plain durable FIFO; add relevance for recall; add compression to keep the cost of long chats flat.

---

## Where to watch it

- [Chat Analytics](/docs/chat-analytics/) surfaces per-conversation turn counts and token usage, the signal that a conversation is long enough to benefit from retrieval/compression.
- [Observability](/docs/observability/) timers cover the per-turn setup phases, including memory retrieval.
- Compression summaries appear as artifacts in the [Agent Workspace](/docs/agent-workspace/) (`memory/…`), visible in the live artifact stream.

---

## Related Pages

| Page | Description |
|---|---|
| [AI Agents](/docs/ai-agents/) | Where all chat-memory settings live |
| [Agent Workspace](/docs/agent-workspace/) | Where compression summaries are stored |
| [Logging](/docs/logging/) | The `turing.logging.engine` backend chat memory persists to |
| [Chat Analytics](/docs/chat-analytics/) | Per-conversation turn + token metrics |
| [Token Usage](/docs/token-usage/) | Track the cost of the summary model |

---

