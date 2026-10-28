---
title: "Embedding Models"
description: "Configure embedding models for vector generation, supported providers, local transformers, model selection and the REST API."
---
An **Embedding Model** converts text into a high-dimensional numerical vector that captures its semantic meaning. These vectors enable similarity search, finding documents conceptually related to a query even when they share no common keywords.

Viglet Turing uses embedding models in two phases:

- **Indexing**: document chunks are vectorized and stored in the [Embedding Store](/docs/embedding-stores/)
- **Querying**: the user's question is vectorized with the same model and compared against stored vectors

:::caution[Same model for indexing and querying]
The embedding model must remain consistent across both phases. Changing the model after documents have been indexed causes dimension mismatches and incorrect similarity results. A **full re-indexing** of all content is required whenever the embedding model changes.
:::

---

## Supported Providers

Embedding model support depends on the LLM vendor configured in the [LLM Instance](/docs/llm-instances/). Not all vendors provide an embedding API.

| Provider | Embedding Support | Example Models | Default Model |
|---|:---:|---|---|
| **OpenAI** | Yes | `text-embedding-3-small`, `text-embedding-3-large`, `text-embedding-ada-002` | `text-embedding-3-small` |
| **Ollama** | Yes | `nomic-embed-text`, `mxbai-embed-large`, `all-minilm` | *(configurable)* |
| **Gemini** | Yes | `gemini-embedding-001` *(asymmetric task types, see below)* | `gemini-embedding-001` |
| **OpenAI-Compatible** | Yes* | whatever the backing endpoint exposes at `/embeddings` | *(per endpoint)* |
| **Bedrock** | Yes | `amazon.titan-embed-*`, `cohere.embed-*` | n/a |
| **Voyage AI** | Yes | `voyage-3`, `voyage-3-large`, `voyage-context-3`, `voyage-multimodal-3` | `voyage-3` |
| **Cohere** | Yes | `embed-v4.0` (multilingual / multimodal / Matryoshka) | `embed-v4.0` |
| **Mistral AI** | Yes | `mistral-embed` | `mistral-embed` |
| **Vertex AI** | Yes | Gemini embeddings on GCP (reuses the native Gemini stack) | n/a |
| **Anthropic** | No | n/a | n/a |
| **Gemini (OpenAI-compatible)** | No | n/a | n/a |

> \* OpenAI-Compatible embeds only when the backing endpoint actually exposes `/embeddings`. **Azure OpenAI is no longer a vendor**: reach an Azure embedding deployment via OpenAI-Compatible. See [LLM Instances](/docs/llm-instances/#capabilities-by-vendor).

### OpenAI

Connects to the OpenAI API (default: `https://api.openai.com/v1`) using your API key. OpenAI offers three embedding model families:

| Model | Dimensions | Notes |
|---|---|---|
| `text-embedding-3-small` | 1,536 | Best cost-performance balance for most deployments |
| `text-embedding-3-large` | 3,072 | Higher quality, larger storage footprint |
| `text-embedding-ada-002` | 1,536 | Legacy model: use `3-small` for new deployments |

### Ollama (Local)

Runs embedding models locally via [Ollama](https://ollama.com). No API key required for local deployments: ideal for air-gapped environments or development.

| Model | Dimensions | Notes |
|---|---|---|
| `nomic-embed-text` | 768 | Good general-purpose model, lightweight |
| `mxbai-embed-large` | 1,024 | Higher quality, more resource-intensive |
| `all-minilm` | 384 | Very lightweight, fast inference |

Pull a model before using it:

```bash
ollama pull nomic-embed-text
```

### Local Transformers (ONNX)

Turing also supports running embedding models **locally, in-process** via ONNX Runtime, without an external LLM provider, API or key. This is useful for air-gapped deployments, custom/fine-tuned models, and zero-cost embeddings.

| Setting | Description |
|---|---|
| **Model Path** | Location of the `.onnx` model file. Accepts a `classpath:`, `file:` or `https:` URI. Remote (`https:`) resources are **downloaded and cached** on first use. |
| **Tokenizer Path** | Location of `tokenizer.json` (same URI schemes as above). |
| **Enable GPU** | Toggle GPU acceleration via ONNX Runtime |
| **Batch Size** | Number of texts to embed per batch |

The default recommended model is `all-MiniLM-L6-v2` (384-dimensional, ~80 MB), a small, fast sentence-transformers model. For example, point **Model Path** at `https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx` and **Tokenizer Path** at the matching `.../tokenizer.json`; the first embedding call downloads and caches both.

### HuggingFace.co

The **HuggingFace.co** provider runs the *same* in-process ONNX runtime as Local Transformers, but instead of typing the `.onnx` and `tokenizer.json` URLs by hand, you **pick a model from a searchable list**. Choose it from the provider dropdown and search HuggingFace directly in the form: Turing lists sentence-transformers repos ranked by popularity and only offers ones it has **verified expose a loadable ONNX model + tokenizer** (an *ONNX verified* check appears next to each), showing the embedding **dimensions** and **download** count. Selecting a model stores just its **repo id** (e.g. `sentence-transformers/all-MiniLM-L6-v2`); Turing resolves the artifact URLs at load time, handling both repo layouts (ONNX at the root or under an `onnx/` subfolder).

When huggingface.co is unreachable (offline, rate-limited), the picker falls back to a **curated catalog** of known-good ONNX models, the source (*Live from huggingface.co* vs *From the curated catalog*) is badged at the bottom of the list. The picker stays **editable**, so a repo id not in the list can still be typed (private or brand-new models). This is the recommended way to use a local model: same zero-cost, key-free, in-process embeddings, without hunting for URLs.

When you pick a model, Turing reads its **embedding dimension** from the repo's `config.json` and shows it inline. If that dimension differs from your current default embedding model's, a warning appears: changing the dimension means every already-embedded vector is invalid until you **re-index**, against a vector store whose field matches the new dimension. This surfaces the cost *before* you save.

If the repo ships more than one ONNX artifact (a full-precision `model.onnx` plus quantized variants like `model_quantized.onnx`), an **ONNX Variant** selector appears. Quantized variants are smaller and faster to run at a small accuracy cost; leave it on *Default (recommended)* unless you specifically want to trade accuracy for size/latency. The chosen variant is remembered with the model.

:::note[HuggingFace Inference API is not this provider]
This provider downloads and runs the model **locally**. It does *not* call HuggingFace's hosted Inference API (that would need an API key and a remote call, configure it as an [LLM Instance](/docs/llm-instances/) instead).
:::

#### Zero-config local RAG at startup

A fresh install (notably the public demo) can provision a complete local retrieval backend automatically, with no admin step. When a global [Default AI Agent](/docs/ai-agents/) is configured and lacks an embedding model or a vector store, Turing fills in each missing piece, wires it onto the Default AI Agent (enabling RAG), then downloads the model and reindexes existing content into the store, so grounded chat works out of the box:

- **Embedding model:** the global default embedding model when one is set; otherwise, when no embedding model exists at all, a new local ONNX model (`all-MiniLM-L6-v2`), registered as the global default.
- **Vector store:** the global default store when one is set; otherwise an existing enabled [Lucene vector store](/docs/embedding-stores/) (such as the "Default Vector Store" a seed import brings); otherwise, when no store exists at all, a new embedded Lucene store, registered as the global default.

It is controlled by `turing.startup.default-rag.*`:

| Property | Default | Description |
|---|---|---|
| `turing.startup.default-rag.enabled` | `true` | Master switch for the startup provisioning. |
| `turing.startup.default-rag.model-name` | `all-MiniLM-L6-v2` | Display name of the provisioned embedding model. |
| `turing.startup.default-rag.model-repo-id` | `sentence-transformers/all-MiniLM-L6-v2` | HuggingFace repo id to provision as the default (a `HuggingFace.co` model, same shape as one you'd pick in the form). **Blank it** to fall back to the raw-URL local path below. |
| `turing.startup.default-rag.model-path` | HuggingFace `all-MiniLM-L6-v2` ONNX URL | Fallback ONNX model URI, used only when `model-repo-id` is blank. |
| `turing.startup.default-rag.tokenizer-path` | HuggingFace `tokenizer.json` URL | Fallback tokenizer URI, used only when `model-repo-id` is blank. |
| `turing.startup.default-rag.store-path` | `./store/lucene-vector` | Filesystem root for the Lucene index. |
| `turing.startup.default-rag.reindex` | `true` | Reindex existing site content into the new store after provisioning. |
| `turing.genai.embedding.local.cache-dir` | *(temp dir)* | Where downloaded ONNX/tokenizer resources are cached; set a persistent path to keep them across restarts. |

The step is **idempotent** and never overrides an existing setup: a Default AI Agent that already has an embedding model and a store is left alone, and when embedding models or stores exist but none is the global default (and no Lucene store is there to adopt), it does not pick among them. Each step logs what it did, or why it stopped, at startup.

---

## Advanced embedding capabilities

Beyond plain text→vector, Turing exploits provider-specific embedding features. Each is **opt-in by model choice**: pick the right model on the right vendor and the capability engages; everything else keeps the classic per-chunk text path byte-for-byte.

### Asymmetric task types (Gemini)

`gemini-embedding-001` embeds a *document* and a *query* differently for a measured retrieval-quality win the OpenAI/Anthropic models can't match. Turing applies it automatically through the standard VectorStore convention (index-time calls use `RETRIEVAL_DOCUMENT`, query-time calls use `RETRIEVAL_QUERY`) with **zero caller changes**. An optional Matryoshka `outputDimensionality` provider option trades a smaller vector for lower storage/IO. Voyage and Bedrock embedding models apply the same asymmetric `document`/`query` distinction.

### Contextualized chunk embeddings (Voyage `voyage-context-3`)

Attacks the classic RAG failure where a chunk is meaningless without its surrounding section. Instead of embedding each chunk in isolation, `voyage-context-3` embeds every chunk **together with its siblings** so each vector carries document context. Select a `*context*` Voyage model (or set the `contextual` provider option) and the asset indexer embeds a document's chunks as a group and upserts the precomputed vectors. It is **fail-soft**, a non-contextual model, a vector-count mismatch, or any error falls back to per-chunk embedding. Query time is unchanged.

### Multimodal embeddings: search text → match an image (Voyage `voyage-multimodal-3`)

Embeds images *and* text into one shared vector space, so a plain-text query can match a PDF page-image, diagram, or screenshot the text-only path can't reach. Select a `*multimodal*` Voyage model (or set the `multimodal` option); images are indexed tagged `_modality=image`, and a text query is embedded in the same space and KNN-matched against image vectors. Exposed at `/api/v2/multimodal` (`available` / `image` upload / `search`); text-only models stay the default. Cohere `embed-v4.0` is likewise multimodal.

### Domain-specialized model per site

Each Semantic Navigation site can pin its **own** embedding model (`embeddingModelId` override on its GenAI config) (`voyage-law` for legal docs, `voyage-code` for code search, `voyage-finance` for finance, or a Cohere domain model) while every other site stays on the platform default. The site's hybrid-ranking path resolves the override for **both** index and query halves, so its per-site `sn_<siteId>` collection never mismatches. Null/blank = platform default.

:::caution[Switching a site's model = re-index that site]
A different model is a different vector space. Changing a site's embedding model requires clearing and re-indexing **that site's** collection.
:::

### Quantized / Matryoshka vectors

For the embedded Lucene store, scalar quantization (`INT8` / `INT4` / `SEVEN_BIT` / `BINARY`) shrinks the index and speeds search at a measured recall cost. It pairs with Matryoshka `outputDimensionality` truncation (on the Voyage/Gemini models) to compound the savings. Configured on the **store**, not the model: see [Embedding Stores → Lucene (embedded)](/docs/embedding-stores/#lucene-embedded).

### Mistral OCR extraction bridge

Not an embedding model itself, but it feeds the embedding pipeline: for scanned PDFs and image-only documents that Apache Tika reads poorly, Turing can call Mistral's OCR API (`mistral-ocr-latest`) to produce layout-aware Markdown before chunking and embedding. It runs **only** when a file is OCR-eligible *and* Tika produced no usable text, so ordinary documents spend no OCR credits. Opt-in via `turing.ocr.*`; disabled or keyless → inert (returns the Tika text), and any OCR error fails soft back to Tika.

---

## Create / Edit Form

Navigate to **Generative AI → Embedding Models** to manage embedding model configurations.

### General Information

| Field | Required | Description |
|---|---|---|
| **Model Name** | Yes | Display name for this embedding model |
| **Description** | | Free-text notes about the model's purpose |

### Provider

The **Provider Type** dropdown selects one of three modes, and the model field adapts to match:

| Provider mode | Model field | What is stored |
|---|---|---|
| An **LLM Instance** (remote embedding API) | A **model picker** listing the vendor's *embedding-capable* models (the same combobox the LLM form uses, filtered to embeddings), live from the vendor when a key is available, otherwise the bundled catalog. Stays editable. | `Model Reference` = the vendor model id (e.g. `text-embedding-3-large`) |
| **HuggingFace.co** | A **searchable HuggingFace picker** (ONNX-verified repos, dimensions, downloads, live/catalog badge). Stays editable. | `Model Reference` = the HuggingFace repo id (e.g. `sentence-transformers/all-MiniLM-L6-v2`) |
| **Transformers (Local)** | Manual **Model Path** + **Tokenizer Path** inputs (below) | The `.onnx` / `tokenizer.json` URIs |

The model field is no longer free-text for the LLM and HuggingFace paths, fat-fingering `text-embedding-3-smal` is caught by the picker, while an unlisted id can still be typed when needed.

### Local Transformers Options

These fields appear only when **Transformers (Local)** is selected as the provider type:

| Field | Description |
|---|---|
| **Model Path** | Path to the ONNX model file (`.onnx` extension) |
| **Tokenizer Path** | Path to the tokenizer file (`tokenizer.json`) |
| **Batch Size** | Number of texts processed per inference batch |
| **Enable GPU** | Toggle hardware acceleration |

### Status

| Field | Description |
|---|---|
| **Enabled** | Toggle to activate or deactivate this model. Disabled models are not available for selection. |

---

## Choosing a Model

The embedding model determines two things:

- **Dimensionality**: the number of dimensions in the vector (e.g., 384, 768, 1,536, 3,072). Higher dimensions capture more nuance but require more storage.
- **Semantic quality**: how well the model captures meaning. Larger models generally produce better similarity results at the cost of slower indexing.

### Recommendations

| Scenario | Recommended Model | Provider |
|---|---|---|
| **General production** | `text-embedding-3-small` | OpenAI |
| **Maximum quality** | `text-embedding-3-large` | OpenAI |
| **Local / air-gapped** | `nomic-embed-text` | Ollama |
| **Resource-constrained** | `all-minilm` | Ollama |
| **Best retrieval quality** | `gemini-embedding-001` *(asymmetric task types)* | Gemini |
| **Retrieval specialist / domain** | `voyage-3-large`, `voyage-law`, `voyage-code` | Voyage AI |
| **Long-chunk context** | `voyage-context-3` | Voyage AI |
| **Search text → image** | `voyage-multimodal-3`, `embed-v4.0` | Voyage AI / Cohere |
| **Custom fine-tuned** | Your `.onnx` model | Local Transformers |

For most deployments, a mid-sized model such as `text-embedding-3-small` (OpenAI) or `nomic-embed-text` (Ollama) provides a good balance between quality and performance.

---

## Global Configuration

Set the default embedding model in **Administration → Settings**:

| Setting | Description |
|---|---|
| **Default Embedding Model** | The embedding model used to generate vectors at indexing and query time |

Individual Semantic Navigation Sites can override this setting in their **Generative AI** tab.

The Knowledge Base always uses the global default.

---

## REST API

Embedding models are managed via the REST API at `/api/embedding-model`.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/embedding-model` | List all embedding models |
| `GET` | `/api/embedding-model/structure` | Get the structure template for a new model |
| `GET` | `/api/embedding-model/{id}` | Get a specific embedding model |
| `POST` | `/api/embedding-model` | Create a new embedding model |
| `PUT` | `/api/embedding-model/{id}` | Update an existing embedding model |
| `DELETE` | `/api/embedding-model/{id}` | Delete an embedding model |

---

## Per-Site Override

Each Semantic Navigation Site can override the global embedding model in its **Generative AI** tab. This allows different sites to use different models, for example, a multilingual site might use a model optimized for cross-language embeddings while a technical site uses a domain-specific model.

The site-level configuration includes:

| Setting | Description |
|---|---|
| **Embedding Model** | Overrides the global default for this site |
| **Embedding Store** | Overrides the global store backend for this site |
| **LLM Instance** | The chat/reasoning model for this site's GenAI features |

---

## Related Pages

| Page | Description |
|---|---|
| [Embedding Stores](/docs/embedding-stores/) | Vector backends (ChromaDB, PgVector, Milvus, embedded Lucene) + quantization |
| [What is RAG?](/docs/rag/) | How embedding models fit into the RAG pipeline |
| [Reranking](/docs/reranking/) | Re-ordering retrieved chunks (Cohere / Voyage / managed) |
| [LLM Instances](/docs/llm-instances/) | Configure the LLM providers that supply embedding APIs |
| [Assets](/docs/assets/) | Knowledge Base files indexed using the embedding model |
| [Semantic Navigation](/docs/semantic-navigation/) | Per-site GenAI and embedding overrides |
| [GenAI & LLM Configuration](/docs/genai-llm/) | Global settings and architecture overview |

---

