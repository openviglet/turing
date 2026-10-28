---
title: "Reranking"
description: "The reranking stage in Viglet Turing: a selectable, fail-open second pass (LLM, cross-encoder, Cohere, Voyage, Bedrock, Vertex AI) that sharpens retrieved context before generation."
---
**Reranking** is a second, more discerning relevance pass that runs *after* retrieval and *before* the answer is generated. Vector similarity is fast but coarse: it's a good first guess at relevance, not a final verdict. The top-K chunks it returns often include near-duplicates, tangential matches, or passages that *mention* the query terms without *answering* the question. A reranker re-scores those candidates against the question and keeps only the highest-precision few, so the model sees **fewer, better** chunks instead of more, noisier ones.

You'd reach for this page when RAG answers feel "almost right but padded with irrelevant context," when you want to shrink the prompt (and its token cost) without losing the chunk that actually answers the question, or when you've standardized on a cloud stack and want a managed reranker rather than a self-hosted one.

The single most important property to know up front: **reranking is fail-open.** Turning it on can only help or be neutral: it can never make retrieval worse.

---

## Where reranking sits in the pipeline

```
Query ──► Hybrid retrieval (BM25 + vector, fused by RRF) ──► top-N candidates
                                                               │
                                                               ▼
                                          Reranker re-scores against the query
                                                               │
                                                               ▼
                                                  keep top-K ──► LLM prompt
```

Reranking operates on whatever the retrieval stage hands it. In Viglet Turing that's **hybrid retrieval**: a vector pass and a BM25 keyword pass run in parallel and are fused by **Reciprocal Rank Fusion (RRF, `k=60`)** before the reranker ever sees them. The reranker then narrows that fused pool to the best few. See [What is RAG?](/docs/rag/) for the full retrieval flow and [Search Engine](/docs/search-engine/) for the BM25 backings.

:::note[Hybrid retrieval has two BM25 backings]
The BM25 half of hybrid retrieval can live in one of two places, chosen per SN site:

| Mode | BM25 lives in | Locale handling | Setup | Best for |
|---|---|---|---|---|
| **`EMBEDDED`** *(default)* | A single in-process Lucene index | One analyzer for every locale | Zero-config | Single-language, dev/test, PoCs |
| **`SE_INSTANCE`** | Per-locale Solr / Elasticsearch cores (`rag_<store>_<locale>`) | Per-locale analyzer (PT/EN/ES…) | Provision cores + reindex | Production, multi-language, large corpora |

`EMBEDDED` is always the safe fallback: if an `SE_INSTANCE` core is missing or unreachable, retrieval degrades to vector-only rather than failing. The vector half always uses the configured [embedding store](/docs/embedding-stores/). You can switch modes per site at any time.
:::

---

## From zero: turn on reranking

1. Open **Administration → Settings → Global Settings**, **RAG Reranker** section.
2. Toggle **Enable Reranker** on.
3. Pick a **Strategy** (start with `LLM`, no extra infrastructure).
4. Set **Top-K kept**: how many of the highest-ranked chunks survive into the prompt (default 20).
5. Save and ask a RAG question. The reranker now narrows the candidate pool before the model answers.

If anything goes wrong with the chosen strategy, you won't see an error: you'll see the original retrieval order. That's the fail-open guarantee at work.

---

## Strategies

Turing treats reranking as a **selectable strategy** so you can match cost, latency, and precision to your deployment. All strategies share the same fail-open facade.

| Strategy | What it is | Auth | When to use |
|---|---|---|---|
| **`LLM`** *(default)* | Asks the configured chat model to rank the candidate snippets. | (uses the chat model) | Quick start; small candidate pools; you already pay for a chat model. |
| **`LLM_LOGPROBS`** | OpenAI-only confidence reranker: a per-candidate yes/no relevance classification with `top_logprobs`, whose calibrated P(relevant) is blended with the retrieval rank. | OpenAI key | You want calibrated confidence, not just an ordering, on an OpenAI model. |
| **`CROSS_ENCODER`** | A self-hosted cross-encoder over an HTTP `/rerank` endpoint (Text Embeddings Inference, Infinity, or a Jina-compatible server, e.g. `BAAI/bge-reranker-v2-m3`). | endpoint URL | Best precision-per-cost. Local, free, fast (sub-100 ms). Recommended for production. |
| **`COHERE`** | The managed [Cohere Rerank](https://docs.cohere.com/docs/rerank) API (`https://api.cohere.com/v2/rerank`, default `rerank-v3.5`). | API key | Zero-ops managed reranker. |
| **`VOYAGE`** | The managed Voyage AI Rerank API (`https://api.voyageai.com/v1/rerank`, `rerank-2.5`), the retrieval specialist Anthropic recommends. | API key | Zero-ops; pairs naturally with Voyage embeddings. |
| **`BEDROCK`** | The managed AWS Bedrock `Rerank` API. Model is the ARN or a bare id (e.g. `amazon.rerank-v1:0`) expanded for the region. | AWS IAM | Cloud-native AWS deployments; no self-hosted model. |
| **`VERTEX_AI`** | The managed Google Vertex AI Ranking API (Discovery Engine `rankingConfigs:rank`, default `semantic-ranker-default-004`). | GCP service account / ADC | Cloud-native GCP deployments. |

:::tip[Mistral has no rerank API]
There is intentionally no `MISTRAL` strategy: Mistral exposes no managed rerank endpoint. To rerank with a Mistral model, use the `LLM` strategy pointed at a Mistral [LLM instance](/docs/llm-instances/).
:::

---

## Fail-open by design

Reranking is wrapped in a single fail-open facade (`TurRagReranker`). If the chosen strategy **errors, times out, isn't configured, or returns nothing usable**, Turing silently falls back to the original retrieval order. A corrupt strategy value parses leniently back to `LLM` rather than throwing. The net effect: enabling reranking can only improve or be neutral: it can never degrade results below the no-rerank baseline.

```mermaid
flowchart TD
    R[Retrieved candidates] --> S{Reranker strategy}
    S -->|success| K[Keep top-K by new score]
    S -.->|error / timeout / empty / not configured.-> O[Original retrieval order]
    K --> P[LLM prompt]
    O --> P
```

---

## Score caching (opt-in)

For repeated queries, the rerank scores can be cached. When `GLOBAL_RAG_SN_RERANK_CACHE_ENABLED` is on, the **ordered doc-id list** is cached keyed by `(strategy, model, query, hash of candidate ids+text, topK)` and re-mapped to live documents on read (serialization-safe across a Hazelcast cluster). The rerank-config setters evict the cache, and content edits self-invalidate via the candidate hash. Default off → byte-for-byte the uncached path.

---

## Configuration reference

All reranker settings live in Global Settings (`/api/system/global-settings`) and persist as config vars:

| Setting key | Default | Applies to | Description |
|---|---|---|---|
| `GLOBAL_RAG_SN_RERANK_ENABLED` | `false` | all | Master on/off for the rerank pass |
| `GLOBAL_RAG_SN_RERANK_STRATEGY` | `LLM` | all | Which strategy runs |
| `GLOBAL_RAG_SN_RERANK_TOP_N` | `20` | all | How many top-ranked chunks survive into the prompt |
| `GLOBAL_RAG_SN_RERANK_ENDPOINT` | *(blank)* | cross-encoder | The self-hosted `/rerank` server URL |
| `GLOBAL_RAG_SN_RERANK_MODEL` | *(blank)* | cross-encoder / Cohere / Voyage / Bedrock | Model name or ARN |
| `GLOBAL_RAG_SN_RERANK_API_KEY` | *(blank, encrypted)* | Cohere / Voyage / Vertex SA | Write-only; blank means "not configured"; a saved key is preserved on edit |
| `GLOBAL_RAG_SN_RERANK_REGION` | `us-east-1` | Bedrock | AWS region for the Bedrock Rerank API |
| `GLOBAL_RAG_SN_RERANK_VERTEX_PROJECT` | *(blank)* | Vertex AI | GCP project id |
| `GLOBAL_RAG_SN_RERANK_VERTEX_LOCATION` | `global` | Vertex AI | Vertex ranking location |
| `GLOBAL_RAG_SN_RERANK_CACHE_ENABLED` | `false` | all | Cache ordered rerank results |

:::note[The API key is write-only]
Like every secret in Turing, the rerank API key is encrypted, never returned by GET (which reports a `…ApiKeySet` boolean instead), and preserved if you leave it blank on edit. For Vertex AI, the encrypted key field holds the service-account JSON; with none set it falls back to Application Default Credentials. For Bedrock, IAM credentials follow the same chain as the [Bedrock LLM vendor](/docs/llm-instances/#authentication-by-vendor).
:::

---

## Related Pages

| Page | Description |
|---|---|
| [What is RAG?](/docs/rag/) | The full retrieval → rerank → generate pipeline |
| [Search Engine](/docs/search-engine/) | BM25 backings (`EMBEDDED` vs `SE_INSTANCE`) for hybrid retrieval |
| [Embedding Models](/docs/embedding-models/) | Voyage/Cohere models that pair with their rerankers |
| [Embedding Stores](/docs/embedding-stores/) | Where the vector half of hybrid retrieval lives |
| [LLM Instances](/docs/llm-instances/) | Vendor auth (Cohere / Voyage / Bedrock IAM / Vertex GCP) |
| [Configuration Reference](/docs/configuration-reference/#rag-reranker-database-settings) | Setting keys and defaults |

