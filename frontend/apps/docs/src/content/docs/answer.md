---
title: "Answer Engine"
description: "The /answer lens in Viglet Turing: a streaming, grounded, cited answer over a Semantic Navigation site's knowledge base, with freshness re-checks, interaction signals and shareable permalinks."
---
The **answer engine** turns a search query into a written answer, grounded in the documents of a Semantic Navigation site and carrying `[n]` citations back to them. It is a *lens* on the same index your search results come from, not a separate product: same site, same fields, same filters.

You would reach for this page when you want a search box to answer rather than only list, and you want to place that answer next to the results yourself.

It is deliberately a **separate endpoint from search**. The client fires the cheap, cacheable search and the slower answer in parallel and composes them, so the hot search path never waits for a language model.

---

## The request

```
GET /api/sn/{siteName}/answer?q=how+do+i+rotate+a+key
Accept: text/event-stream
```

| Parameter | Required | Meaning |
|---|---|---|
| `q` | yes | The question. Blank or missing returns a single `error` event rather than an empty stream. |
| `locale` | no | Which locale core to answer from, e.g. `pt_BR` or `pt-BR` (both accepted). Unparseable or absent falls back to English. |
| `fq` | no | Filter query, repeatable: `fq=type:manual&fq=product:turing`. The active facet filters reach retrieval, so the answer is synthesized **within** the narrowed set. |

The `fq` values are the same field/value pairs your facets already produce, so an answer beside a filtered result list can simply pass the filter state through and stay consistent with what the user is looking at.

### Authentication

The endpoint is anonymous on a site left in the default **PUBLIC** API mode, which is what lets a public search page consume it with no credentials.

A site switched to **API_KEY** mode requires a key on `/answer` exactly as it does on `/search` and `/chat`: send it in the `Key` header, or as `?apiKey=` for a plain `<script>` embed. Anonymous traffic to `/answer` is also rate limited per IP and per session, alongside chat and the catalog copilot.

---

## The stream

The response is `text/event-stream`. Each frame carries a named event; a client that only wants the prose can listen to `token` and ignore the rest.

| Event | Payload | When |
|---|---|---|
| `sources` | The retrieved documents, numbered | First, before any prose, so the UI can render the source list while the answer is still being written. |
| `token` | A fragment of the answer text | Repeatedly, in order. Concatenate them. |
| `citations` | The `[n]` → source mapping | After the answer completes. |
| `followups` | Grounded follow-up questions | After the answer, when follow-ups are enabled. |
| `trace` | Why this answer: knowledge-base mode, whether reranking ran and with which strategy | After the answer. |
| `trace_step` | One step of the retrieval/generation trace | During, when step tracing is on. |
| `artifact` | A rich artifact produced by a tool | During, when the agent emits one. |
| `error` | `{"message": "..."}` | Instead of an answer. |

The `[n]` markers inside the `token` text refer to the entries of the `sources` event, and `citations` states that mapping explicitly, so a client never has to parse the prose to build its footnotes.

### When you get an `error` instead

A single `error` event, and the stream ends. The message says which:

- **no query**: `q` was blank or missing;
- **site not found**: no site by that name;
- **AI is not enabled for site (REASON)**: the site's chat cannot answer either. `REASON` is the same value `GET /api/sn/{site}/chat/enabled` reports (`NO_GENAI`, `NO_AGENT`, `MISSING_EMBEDDING`, ...). A site with no Generative AI configuration of its own still answers through the [Default AI Agent](/docs/ai-agents/#default-ai-agent-global-fallback) when one is set;
- **No LLM configured for site**: the site's effective agent has no language model (see [LLM Instances](/docs/llm-instances/));
- **temporarily unavailable**: anything else failed upstream; the detail is in the server log, not the response.

This is a stream that always answers something. It never hangs and never returns a bare status.

---

## Freshness: has the answer gone stale?

An answer is grounded in the documents as they were when it ran. `POST /api/sn/{siteName}/answer/freshness` re-checks whether those cited sources have since drifted, so a client can show a freshness badge and offer a one-click re-answer.

```json
{
  "q": "how do i rotate a key",
  "answeredAt": "2026-08-19T12:00:00Z",
  "topK": 5,
  "sources": [{ "sourceId": "doc-123", "citedText": "…the grounded passage…" }]
}
```

It returns one verdict per source. It is **fail-open**: an empty list when drift detection is off or nothing re-checks, which renders as no badge rather than as an error.

---

## Signals: telling the engine an answer landed

`POST /api/sn/{siteName}/answer/signal` records an answer-level interaction (a citation opened, a follow-up taken) for the self-tuning loop:

```json
{ "conversationId": "…", "signal": "citation_click", "query": "how do i rotate a key" }
```

Anonymous like the answer itself, and fire-and-forget: it is a companion of an answer already produced.

---

## Permalinks

`POST /api/sn/{siteName}/answer/share` persists a snapshot of an answer (query, filter state, sources, answer text, timestamp) and returns an id you turn into a URL. `GET …/share/{shareId}` reads it back, and `GET …/share/{shareId}/og` renders its Open-Graph card for link previews.

:::note[Sharing is not anonymous]
Unlike `answer`, `freshness` and `signal`, the share endpoints require authentication. Minting a permalink writes a row, and an anonymous write is a storage and spam surface. Read this as deliberate rather than as an oversight.
:::

---

## Related Pages

- [Semantic Navigation](/docs/semantic-navigation/): the sites, fields and facets this answers over
- [RAG](/docs/rag/): retrieval, grounding and citation behaviour
- [Vectorless (Structured-Data) RAG](/docs/vectorless-structured-rag/): answering a structured catalog with no embeddings
- [LLM Instances](/docs/llm-instances/): the model an answer is written by

