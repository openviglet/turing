---
title: "Governed LLM Gateway"
description: "Point any OpenAI-compatible client at Viglet Turing: virtual keys, per-key budgets and rate limits, semantic cache, guardrails, model-name routing into agents and RAG, and native tools."
---
The **Governed LLM Gateway** exposes an **OpenAI-compatible inbound API** so any
existing app can point its `base_url` at **Viglet Turing** with *zero code
change*, and, in doing so, transparently gain the governance and intelligence
Turing already provides: virtual keys, per-key budgets and rate limits, spend
tracking, a semantic cache, guardrails, model-name routing into your agents and
Semantic Navigation sites, and provider-native tools.

The gateway is **opt-in and off by default**. Enable it with:

```properties
turing.gateway.enabled=true
```

## Endpoints

Base URL: `https://<your-turing-host>/v1`

| Method & path | Purpose |
| --- | --- |
| `POST /v1/chat/completions` | Chat completions: blocking, or streamed when `"stream": true` (OpenAI `data:` chunks, terminated by `data: [DONE]`) |
| `POST /v1/embeddings` | Embeddings |
| `GET /v1/models` | Lists the models the gateway advertises (every enabled LLM instance) |

Because the wire format is OpenAI's, the official SDKs work unchanged:

```python
from openai import OpenAI

client = OpenAI(
    base_url="https://your-turing-host/v1",
    api_key="sk-turing-...",          # a Turing virtual key
)
resp = client.chat.completions.create(
    model="gpt-4o",
    messages=[{"role": "user", "content": "Hello"}],
)
```

```bash
curl https://your-turing-host/v1/chat/completions \
  -H "Authorization: Bearer sk-turing-..." \
  -H "Content-Type: application/json" \
  -d '{"model": "gpt-4o", "messages": [{"role": "user", "content": "Hello"}]}'
```

## Virtual keys

Every caller authenticates with a **virtual key** (`sk-turing-...`), created and
managed by an admin under **`/bento/gateway`**. A key is shown **once** at
creation: copy it then; only a masked prefix is stored for display.

Each key can be scoped and governed:

| Setting | Effect |
| --- | --- |
| **Allowed models** | Comma-separated allow-list; blank = any model. A request for an out-of-scope model is rejected. |
| **Monthly budget (USD)** | Soft cap: once month-to-date spend reaches it, the key **auto-downgrades** to a cheaper model (if one is configured) instead of the requested one. |
| **Hard cap (USD)** | Hard kill-switch: once reached, requests are refused with HTTP 429 before any upstream LLM call. |
| **Rate limit (req/min)** | Per-key request rate limit; excess requests get HTTP 429. |
| **Expiry** | A key past its expiry no longer authenticates. |

Rotate a key to issue a fresh secret (the old one stops working); revoke to
disable it immediately. All inbound spend is metered **per key** and shown in the
`/bento/gateway` spend dashboard.

## Model-name routing

The `model` string routes the request into the Turing stack:

| `model` value | Behaviour |
| --- | --- |
| `gpt-4o`, `claude-…`, any instance id/name | **Passthrough** to that provider (parity) |
| `turing-agent:<id>` | Runs the full **AI Agent**: its system prompt, tools, MCP servers and grounding mode |
| `turing-sn:<site>` | A **RAG-grounded** answer over a Semantic Navigation site (retrieval + citations + reranker) |
| `turing-local:*` | Forces the embedded, local ($0) model |
| `turing-router:<strategy>/<id1,id2,…>` | **Load-balances** across several deployments with a fallback chain. Strategy: `rr` (round-robin, default), `latency` (least observed latency), `failover` (declared order), or the catalog-driven `cost` (cheapest), `quality` (highest intelligence index) or `ttft` (lowest time-to-first-token). Example: `turing-router:cost/inst-a,inst-b` picks the cheapest first, the rest as fallback |

The `cost` / `quality` / `ttft` strategies order the declared deployments by the public model catalog's pricing / intelligence index / time-to-first-token for each deployment's configured model. A deployment whose model has no catalog value for the chosen metric sorts **last**, so a missing figure never wins a "cheapest" or "fastest" pick.

## Sampling parameters

`temperature`, `top_p`, `max_tokens` and `stop` on a request override the model
instance's own settings for that call, on every provider; anything the request
leaves out keeps the instance's value. They are applied best-effort, as in the
OpenAI API: a reasoning model that rejects sampling parameters is retried
without them. Turing-routed models (`turing-agent:`, `turing-sn:`, copilot)
answer with their own settings.

## JSON mode

Send OpenAI's `response_format` to get JSON back instead of prose:

```json
{"model": "gpt-4o-mini",
 "response_format": {"type": "json_object"},
 "messages": [{"role": "user", "content": "List three colours as a JSON object."}]}
```

`json_object` and `json_schema` (with the schema under `json_schema.schema`, and
`strict` honoured) are passed to the provider when the model is served over the
OpenAI wire: OpenAI, OpenAI-compatible endpoints, Gemini (OpenAI endpoint),
Mistral and Cohere. This works for blocking and streamed calls.

Where JSON mode cannot be honoured, the request is refused with HTTP 400 and
`"param": "response_format"`, never answered in prose. That covers other
providers (Anthropic, Gemini native, Ollama, Bedrock, …), the `turing-agent:`,
`turing-sn:` and copilot routes, and `x-turing-tools` turns. With
`x-turing-cache: semantic`, a JSON-mode answer is cached apart from the plain-text
answer to the same prompt.

## Cross-cutting headers

Stack these on **any** `model` to add behaviour without changing your payload:

| Header | Effect |
| --- | --- |
| `x-turing-rag-site: <site>` | **RAG-as-a-header**: retrieves the site's top passages and grounds the answer, on any base model |
| `x-turing-cache: semantic` | Serves an identical prompt from an in-memory response cache (no upstream call, no spend) |
| `x-turing-guardrails: strict` | Moderates the request input (and, on the blocking endpoint, the output); flagged content is refused |
| `x-turing-tools: web_search,web_fetch,code_execution` | Enables the provider's **native tools** on a plain chat call, the capabilities must first be enabled on the model instance by an admin |

Example, a legacy OpenAI app gains RAG grounding by changing its base URL and
adding one header:

```bash
curl https://your-turing-host/v1/chat/completions \
  -H "Authorization: Bearer sk-turing-..." \
  -H "x-turing-rag-site: wknd" \
  -H "Content-Type: application/json" \
  -d '{"model": "gpt-4o", "messages": [{"role": "user", "content": "What is WKND?"}]}'
```

## Admin

The **`/bento/gateway`** console page (admin only) provides virtual-key CRUD
(create / rotate / revoke with scope + budget), a per-key spend dashboard, and a
copy-paste playground snippet. Inbound spend also flows into the standard
[Cost Governance](/docs/cost-governance/) view, keyed by virtual key.

## Learning from traffic (optional)

With `turing.gateway.capture-traffic=true`, the gateway records inbound
base-model and router turns as rows of a reusable **evaluation dataset** (prompts
as the replay turns; the answer kept as reference *material*, asserting nothing
until a human curates it). That dataset runs through the standard
[evaluation](/docs/agent-eval/) graders to measure quality on real traffic, and an
admin can request a **propose-only** distilled/cheaper-model suggestion for an
agent (`POST /api/gateway/traffic/distill?agentId=…`, gated by
`turing.distillation.enabled`): the recommendation is never applied
automatically. Capturing prompts/answers is off by default and admin-managed.

## Errors

Errors use OpenAI's envelope (`{"error": {"message", "type", "code"}}`), so an
OpenAI SDK raises the exception it would raise against OpenAI:

| Situation | Status | `type` |
| --- | --- | --- |
| The provider is out of credits, or the tenant's monthly token budget is spent | 429 | `insufficient_quota` |
| The provider rate-limited the call | 429 | `rate_limit_exceeded` (or the provider's own type) |
| A virtual key's rate limit or hard cap | 429 | `rate_limit_exceeded` / `insufficient_quota` |
| A parameter the model cannot honour (e.g. `response_format`) | 400 | `invalid_request_error`, with `param` set |
| The provider did not answer | 502 | `api_error` |

A provider's `Retry-After` is passed through on the 429. Streamed calls answer
the same statuses when the provider refuses before the first token, which is
where quota and rate limits land. A failure after tokens have started arrives as
a final `data: {"error": {...}}` event, the shape OpenAI clients raise as an
error.

## Notes & limits

- The gateway targets the subset of the OpenAI wire contract that most clients
  use (chat, embeddings, streaming and tools) not full wire fidelity.
- It is additive and default-off: nothing changes for non-gateway paths.
- Native-tool turns (`x-turing-tools`) are not token-metered, since the
  provider-native services surface no usage.

