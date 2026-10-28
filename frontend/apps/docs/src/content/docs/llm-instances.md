---
title: "Configure LLM Instances in Viglet Turing"
description: "Connect Viglet Turing to OpenAI, Anthropic Claude, Gemini, Ollama and 7 other model providers: credentials per vendor, generation parameters, and which features each model supports."
sidebar:
  label: "LLM Instances"
---
An **LLM Instance** is a named, configured connection to a Large Language Model provider, a vendor, a model name, credentials, and generation parameters bundled under a title you choose. It is the unit every Generative AI feature points at: an AI Agent, a Semantic Navigation site's RAG, the Chat interface, a reranker, or an embedding pipeline each select an instance by name.

You'd reach for this page the moment you want Viglet Turing to *talk to a model*. Nothing in the GenAI surface works until at least one enabled instance exists. Because instances are independent, you can run several at once (a fast local Ollama model for low-stakes classification, Anthropic Claude for a complex reasoning agent, and a Voyage instance purely for embeddings) and wire each feature to the one that fits.

The **Language Model** page lives at `/admin/llm/instance`, under the **Generative AI** section of the sidebar.

---

## From zero: create your first instance

1. Open **Generative AI → Language Model** and click **"New language model instance"**.
2. Pick a **Vendor**. Selecting one pre-fills the **Endpoint URL** and **Model Name** with working defaults (see the table below), so a stock OpenAI or Ollama instance needs almost nothing else.
3. Paste your **API Key** (skip it for a local Ollama, or for cloud-IAM vendors like Bedrock/Vertex that authenticate differently, see [Authentication by vendor](#authentication-by-vendor)).
4. Leave the generation parameters at their defaults, make sure **Enabled** is on, and **Save**.
5. Set this instance as the **Default LLM Instance** in **Administration → Settings** so features with no explicit override use it. See [Generative AI & LLM Configuration](/docs/genai-llm/#global-settings).

That instance is now selectable in every agent, site, and the Chat tab.

---

## Zero-config: provision from `OPENAI_API_KEY` at startup

For containerized and hands-off deployments (the public demo is the canonical case), Viglet Turing can create the first LLM instance for you at boot, straight from an environment variable, no admin visit required.

**On startup, when all of these hold, Turing creates a GLOBAL OpenAI instance and sets it as the [Default LLM Instance](/docs/genai-llm/#global-settings):**

- `OPENAI_API_KEY` is present and non-blank in the environment,
- `turing.startup.default-llm.enabled` is not `false` (it defaults to **on**), and
- **no LLM instance exists yet**: the table is empty.

The created instance uses the **OpenAI** vendor, endpoint `https://api.openai.com/v1`, a cheap default model, has tools enabled, and stores the key encrypted exactly like a manually-created one. Because it is a GLOBAL instance (no owning tenant), every tenant can use it.

| Setting | Env var | Default | Purpose |
|---|---|---|---|
| Enable startup provisioning | `TURING_STARTUP_DEFAULT_LLM_ENABLED` | `true` | Set `false` to never auto-create the instance. |
| API key (the trigger) | `OPENAI_API_KEY` | *(unset)* | When unset/blank, nothing is provisioned. |
| Model | `TURING_STARTUP_DEFAULT_LLM_MODEL` | `gpt-4o-mini` | Chat model for the created instance. |

:::note[Idempotent by design: it never overrides your setup]
The empty-table guard means this runs only on a **fresh install**. The moment any instance exists (created here, imported, or added in the admin) the bootstrap is skipped on every subsequent boot and your instances are never touched. (On an ephemeral demo whose store volume is wiped, the table is empty again, so the default is recreated automatically.)
:::

You can override the model, disable auto-provisioning, or add more instances in the admin at any time; the auto-created instance behaves like any other from then on.

---

## Viglet AI: start without a key of your own

Both routes above assume you already have a provider account. **Viglet AI** is the third way in: a managed endpoint Viglet runs, which you pair with in a couple of clicks and use immediately, with no account, card or API key.

On a fresh install the bento home shows a **Turn on AI** card offering two equally weighted paths, *Activate Viglet AI* and *Use my own API key*. They are equivalent afterwards, every GenAI feature works the same either way, and bringing your own key stays free and fully featured. The card states plainly what differs: **Viglet AI runs on Viglet's endpoint, so prompts and retrieved content leave your install**, while your own key keeps that traffic between you and your provider.

### Pairing this install

1. On the bento home, click **Activate Viglet AI**.
2. The dialog shows a short **user code** and a verification URL.
3. Open the URL, sign in, and approve the code.
4. The install finishes on its own: it creates an LLM instance called **Viglet AI** and, if you had no default yet, sets it as the [Default LLM Instance](/docs/genai-llm/#global-settings).

An install that already has a default keeps it, pairing adds an instance rather than taking your choice away, and re-pairing refreshes the existing managed instance instead of stacking a second one beside it.

The managed instance serves a single opaque model id (`viglet-fast`); the endpoint and model are fixed and any URL or model name on the instance is ignored. Chat and embeddings both ride that alias. Provider-native tools and computer use are not available on it, which is a good reason to bring your own key.

### Rotating the install key

Pairing also generates an **Ed25519 signing key** that stays on this install and never leaves it. Every call to the managed endpoint is signed with it, so a credential copied out of your database is not usable elsewhere.

If that key may have leaked, rotate it: **Administration → Settings → Rotate key**, in the section beside the URL-signing secret. It appears only on a paired install.

The new key is registered with Viglet *before* it replaces the old one. If the registration is refused or the endpoint cannot be reached, nothing changes and the action reports **Unchanged**, your current key keeps working. The private half is never displayed.

| Setting | Env var | Default |
|---|---|---|
| Managed endpoint | `TURING_VIGLETAI_BASEURL` | `https://llm.viglet.cloud/v1` |
| Pairing endpoint | `TURING_VIGLETAI_PAIRINGBASEURL` | `https://llm.viglet.cloud/pair` |
| Model alias | `TURING_VIGLETAI_MODELALIAS` | `viglet-fast` |

:::note[Running out of the free credit]
The managed credit is capped. When you reach the cap, Turing shows a banner naming what ran out and when it resets, with two ways forward: continue on a paid plan, or paste your own API key. See [Plans & quotas](/docs/multi-tenancy/#plans--quotas).
:::

---

## Instance Listing

The page displays all configured instances as a grid of cards (title and description). Use the **"New language model instance"** button to create a new one. The vendor dropdown is populated from `/api/llm/vendor`: the list is database-driven, not hard-coded, so the vendors you see are exactly the ones seeded into your install.

---

## Create / Edit Form

The form is organized into **5 colour-coded sections** for quick visual orientation.

### 1. General Information (blue)

| Field | Required | Description |
|---|---|---|
| **Title** | ✅ | Display name for this instance: appears in dropdowns and agent configuration |
| **Vendor** | ✅ | Select the LLM provider. Selecting a vendor applies sensible defaults to Endpoint URL, Model Name, and generation parameters automatically. |
| **Description** | | Free-text notes about this instance's purpose |

### 2. Model Settings (purple)

| Field | Required | Description |
|---|---|---|
| **Endpoint URL** | ✅ (URL-based vendors) | Base URL for the provider API. Left empty for IAM-authenticated vendors (Bedrock, Vertex AI) and the native Gemini SDK, which are not addressed by URL. |
| **API Key** | | Provider API key: stored encrypted in the database. Leave blank when editing to keep the existing key. Optional for local Ollama and OpenAI-compatible endpoints that need no auth. |

:::note[API Key security]
The API Key field is **write-only**. It is stored encrypted and never returned in API responses. When editing an existing instance, leaving the field blank preserves the previously saved key. The encryption key is configured in `turing.ai.crypto.key` in `application.yaml`. See [Configuration Reference](/docs/configuration-reference/#turing-es-core).
:::

### 3. Models (violet)

Choose **one or more models** the instance may serve, and mark exactly one as the **default**. The default is the model used everywhere the platform points at this instance (AI Agents, Semantic Navigation RAG, Chat, rerankers, embeddings): the rest of the platform is single-model and always consumes the default.

To use the picker:

1. Click **Add a model…** to open the picker. It lists the known models for the selected vendor (live from the provider when a key is available, otherwise a suggested catalog). You can also type a model identifier that isn't in the list (useful for preview models, private fine-tunes, or a Bedrock model id / inference-profile ARN) and press **Enter** to add it.
2. Each selected model appears as a row. Click the **★ star** on a row to make that model the **default** (the default row is highlighted and badged **Default**).
3. Remove a model with the **✕** button. If you remove the current default, the first remaining model is promoted automatically.

At least one model is required to save. Selecting a vendor (or changing it) resets the list and seeds it with that vendor's default model. Instances created before this feature keep their single model as the default.

:::note[Multiple models, one default]
The extra models are recorded on the instance (as a comma-separated `modelNames` list) and surfaced where multi-model selection is useful, for example the [Governed LLM Gateway](/docs/llm-gateway/) model picker. They do **not** change which model a given feature calls: that is always the **default** (`modelName`).
:::

### 4. Generation Parameters (emerald)

Fine-tune how the model generates responses. Defaults are appropriate for most use cases.

| Field | Description | Notes |
|---|---|---|
| **Temperature** | Randomness of the output (0.0 = deterministic, ~1.0 = very creative) | Applies to all chat vendors |
| **Top P** | Nucleus sampling: restricts token selection to the top P probability mass | Applies to all chat vendors |
| **Seed** | Fixed seed for reproducible outputs | Defaulted for **OLLAMA** and **OPENAI** |

### 5. Advanced Options (amber)

| Field | Description |
|---|---|
| **Response Format** | Output format: `TEXT` (default) or `JSON` |
| **Supported Capabilities** | Comma-separated list of feature flags (e.g., `RESPONSE_FORMAT_JSON_SCHEMA`). For native server-side tools and the two-level capability gate, see [Capabilities](/docs/capabilities/). |
| **Timeout** | Maximum time to wait for a response, in ISO 8601 duration format (e.g., `PT60S` = 60 seconds). Default: `PT60S` |
| **Max Retries** | Number of retry attempts on transient failures. Default: `3` |
| **Provider Options (Visual)** | Vendor-specific fields rendered dynamically based on the selected vendor (see [Provider Options](#provider-options) below) |
| **Provider Options (JSON)** | Raw JSON override for any vendor-specific setting, the home for cloud-IAM credentials and other settings not exposed as visual fields (see [Authentication by vendor](#authentication-by-vendor)) |

### 6. Status (slate)

| Field | Description |
|---|---|
| **Enabled** | Toggle to activate or deactivate this instance. Disabled instances are not available for selection in agents or sites. |
| **Tools Enabled** | Toggle to allow this instance to use function calling (tools such as web search, code interpreter, etc.) |

---

## Catalog-driven intelligence

Turing consumes a public [**model catalog**](https://openviglet.github.io/model-catalog/), an open, vendor-neutral project ([openviglet/model-catalog](https://github.com/openviglet/model-catalog)), that describes each well-known model, pricing, context window, max output, capabilities, modalities, benchmarks, performance, knowledge cutoff, tier and deprecation status. The instance form and pickers use that data so you configure models with the facts in front of you instead of from memory. The catalog URL is configurable via `turing.model-catalog.url`: see [Configuration Reference § Model Catalog](/docs/configuration-reference/#model-catalog).

### Enriched model picker

Every row in the model picker shows the catalog facts for that model: a compact line with **price** (input/output per 1M tokens), **context window**, **max output**, **intelligence index**, **time-to-first-token**, **knowledge cutoff**, and capability icons (vision / tools / reasoning), plus a **tier badge** (Frontier / High / Mid / Light). A **Sort** control orders the list by *relevance*, *cheapest*, *largest context*, or *most intelligent*. Models discovered live from a vendor that aren't in the catalog simply show no extra metadata, never a misleading zero.

### Auto-filled limits

Selecting a model **pre-fills** the instance's **Context window** and **Max output tokens** from the catalog (and **embedding dimensions** when you pick an embedding model). The pre-fill only fills a field you left empty: your own values are never overwritten, and you can edit or clear a pre-filled value. A hint under each field shows the catalog value, and warns (in amber) when a hand-entered value **exceeds the catalog maximum** for that model.

### Capability warnings

When you enable **Tool calling** or **File upload** on an instance whose selected model the catalog does *not* list as supporting it, an amber warning appears under the toggle, so you catch "an image sent to a text-only model" or "tools on a model without tool support" before it fails at runtime. The warning only shows on positive evidence the model lacks the capability; it stays silent when the catalog doesn't describe the model.

### Deprecation & change-feed banners

The **LLM instance list** surfaces two catalog-driven banners:

- **Deprecated models in use** (amber): lists any instance whose configured model the catalog marks `deprecated` / `RETIRED`, with a suggested GA replacement (same vendor, covers the model's capabilities, similar-or-lower price).
- **Catalog changes to your models** (blue): consumes the catalog change feed to flag configured models that were **removed**, **superseded** (removed while a newer same-vendor model appeared), or **changed** since the last catalog snapshot.

You can also receive the change feed by **email**: set `turing.model-catalog.change-feed.email-enabled=true` and a daily job emails the recipient configured in Global Settings a digest whenever a change affects a model you use (deduped so an unchanged feed is never re-sent). The in-UI banner is always on regardless of the email setting.

### Model Advisor: "find me a model"

The **Find a model** launcher on the instance list opens the **Model Advisor** (`/bento/llm/advisor`). Describe what you need, required capabilities (tools / vision / reasoning), a minimum context window, a maximum input price, a minimum intelligence index, and a minimum tier, and it returns a **ranked shortlist** of catalog models that satisfy every constraint (ranked by intelligence, then price, then context). Each result has a **Create instance** button that one-click-creates a draft LLM instance for that vendor + model and drops you on its edit page to add credentials.

---

## Supported Vendors

Viglet Turing ships **11 vendor types**, resolved by plugin identifier through `TurGenAiLlmProviderFactory`. The canonical set is `LlmProviderType.KNOWN`, seeded into the `llm_vendor` table by `TurLLMVendorOnStartup`. When a vendor is selected in the form, the Endpoint URL, Model Name, and generation defaults are pre-filled as shown below.

| Vendor (ID) | Plugin | Default Endpoint | Default Model |
|---|---|---|---|
| **Ollama** (`OLLAMA`) | `ollama` | `http://localhost:11434` | `mistral` |
| **Open AI** (`OPENAI`) | `openai` | `https://api.openai.com/v1` | `gpt-4o-mini` |
| **Anthropic** (`ANTHROPIC`) | `anthropic` | `https://api.anthropic.com` | `claude-sonnet-4-20250514` |
| **Google Gemini** (`GEMINI`) | `gemini` | *(native GenAI SDK: no URL)* | `gemini-2.0-flash` |
| **Google Gemini (OpenAI Compatible)** (`GEMINI_OPENAI`) | `gemini-openai` | `https://generativelanguage.googleapis.com/v1beta/openai` | `gemini-2.0-flash` |
| **OpenAI-Compatible** (`OPENAI_COMPAT`) | `openai-compatible` | *(none: set the base URL)* | *(set per endpoint)* |
| **AWS Bedrock** (`BEDROCK`) | `bedrock` | *(IAM: no URL)* | `anthropic.claude-3-5-sonnet-20241022-v2:0` |
| **Voyage AI** (`VOYAGE`) | `voyage` | `https://api.voyageai.com/v1` | `voyage-3` |
| **Cohere** (`COHERE`) | `cohere` | `https://api.cohere.ai/compatibility/v1` | `command-r-plus` |
| **Mistral AI** (`MISTRAL`) | `mistral` | `https://api.mistral.ai/v1` | `mistral-large-latest` |
| **Google Vertex AI** (`VERTEX_AI`) | `vertex-ai` | *(IAM: no URL)* | `gemini-2.0-flash` |

:::note[Azure OpenAI was removed]
`AZURE_OPENAI` is **no longer a vendor**. Spring AI dropped the `spring-ai-azure-openai` module, and the vendor plus any instances using it are deleted on upgrade. To reach an Azure-hosted OpenAI deployment, use the **OpenAI-Compatible** vendor and point its base URL at your Azure resource.
:::

:::tip[OpenAI-Compatible covers a long tail]
One **OpenAI-Compatible** vendor reaches DeepSeek, xAI Grok, Groq, Cerebras, OpenRouter, Together, Fireworks, and any local vLLM / LM Studio server, the base URL identifies the actual provider. The API key is optional (blank → a `not-needed` placeholder for keyless local servers). Cohere and Mistral are first-class vendors of their own but ride the same OpenAI-compatible transport under the hood.
:::

---

## Authentication by vendor

Most vendors authenticate with a single API key in the **API Key** field. Three patterns differ:

| Vendor | How it authenticates |
|---|---|
| **OpenAI-Compatible** | Base URL (required) identifies the endpoint; API key optional, blank becomes a `not-needed` placeholder for keyless local servers. |
| **AWS Bedrock** | IAM, not URL-based. Set `region` (and optional `accessKeyId` / `secretAccessKey`) in **Provider Options (JSON)**; with no static keys it falls back to the standard `DefaultCredentialsProvider` chain (env vars, profile, instance role). Model name is the Bedrock model id or an inference-profile ARN. |
| **Google Vertex AI** | IAM on GCP. Set `project` (required) and `location` in **Provider Options (JSON)**, plus optional `credentialsJson` (a service-account key); with none, it uses Application Default Credentials. Supports VPC-SC and CMEK via the project's GCP configuration. |

The native **Gemini** vendor uses the Google GenAI SDK addressed by API key (no endpoint URL); **Gemini (OpenAI Compatible)** uses the OpenAI-style endpoint instead.

---

## Provider Options

Each vendor exposes additional fields in the **Provider Options (Visual)** section, rendered dynamically when that vendor is selected. Anything not surfaced as a visual field (notably the cloud-IAM credentials above) goes in **Provider Options (JSON)** and is preserved as-is on save.

| Vendor | Visual fields |
|---|---|
| **Ollama** | `embeddingModel`, `topK`, `repeatPenalty`, `numPredict`, `stop`, `maxTokens` |
| **Open AI** | `embeddingModel`, `maxTokens` |
| **Anthropic** | `topK`, `maxTokens` |
| **Gemini** | `topK`, `maxTokens` |
| **Gemini (OpenAI Compatible)** | `maxTokens` |
| **OpenAI-Compatible / Cohere / Mistral** | `maxTokens` |
| **AWS Bedrock / Vertex AI** | `maxTokens` (+ IAM credentials via Provider Options JSON) |
| **Voyage AI** | *(embedding-only: model name + API key)* |

:::note[Where "max tokens" is stored]
For **Ollama**, max-tokens is persisted as `numPredict` on the entity; every other vendor stores it as `maxTokens` inside `providerOptionsJson`. The form handles the routing transparently.
:::

---

## Capabilities by Vendor

Not all vendors do everything. Chat, embeddings, tool calling, and managed reranking are independent capabilities: pick an instance whose vendor supports what the feature needs.

| Vendor | Chat | Embedding | Tool Calling | Managed Rerank |
|---|:---:|:---:|:---:|:---:|
| **Ollama** | ✅ | ✅ *(configurable model)* | ✅ | ❌ |
| **Open AI** | ✅ | ✅ | ✅ | ❌ |
| **Anthropic** | ✅ | ❌ | ✅ | ❌ |
| **Gemini** | ✅ | ✅ *(native `embedContent`, T495)* | ✅ | ❌ |
| **Gemini (OpenAI Compatible)** | ✅ | ❌ | ✅ | ❌ |
| **OpenAI-Compatible** | ✅ | ✅ *(if backing endpoint supports it)* | ✅ | ❌ |
| **AWS Bedrock** | ✅ *(Converse)* | ✅ *(Titan / Cohere via `invokeModel`)* | ✅ | ✅ *(Bedrock Rerank)* |
| **Voyage AI** | ❌ | ✅ | ❌ | ✅ *(Voyage rerank)* |
| **Cohere** | ✅ | ✅ *(Embed v4)* | ✅ | ✅ *(Cohere rerank)* |
| **Mistral AI** | ✅ | ✅ *(`mistral-embed`)* | ✅ | ❌ *(no rerank API)* |
| **Vertex AI** | ✅ | ✅ | ✅ | ✅ *(Vertex AI Ranking)* |

:::note[Embedding vendors]
If you need embeddings (for RAG and the Knowledge Base), use **OpenAI**, **Ollama**, **Gemini**, **OpenAI-Compatible**, **Bedrock**, **Voyage**, **Cohere**, **Mistral**, or **Vertex AI**. **Anthropic** and **Gemini (OpenAI Compatible)** are chat/tool-calling only. For provider-by-provider embedding details see [Embedding Models](/docs/embedding-models/).
:::

:::note[Reranking is separate from chat/embedding]
The "Managed Rerank" column refers to a vendor's *dedicated* rerank API used by the pluggable reranker. **Mistral has no rerank API**: use the generic `LLM` rerank strategy on a Mistral instance instead. See [Reranking](/docs/reranking/).
:::

---

## Security

API Keys are handled with care at every layer:

- **Stored encrypted**: the key is encrypted before being persisted to the database.
- **Never returned**: the API Key field is write-only: it flows in on save but never comes back in API responses or GET endpoints.
- **Edit safely**: leaving the API Key field blank when editing an instance preserves the existing encrypted value without modification.
- **Encryption key**: configured via `turing.ai.crypto.key` in `application.yaml`. **Always set a strong, unique value in production**: the default is a placeholder and must be changed before handling real API keys.
- **Cloud-IAM credentials**: Bedrock secret keys and Vertex service-account JSON placed in Provider Options are treated as secret material; prefer ambient credentials (instance role / ADC) over embedding static keys where possible.

---

## Related Pages

| Page | Description |
|---|---|
| [Generative AI & LLM Configuration](/docs/genai-llm/) | Conceptual overview of RAG, embeddings, tool calling, and agents |
| [Capabilities](/docs/capabilities/) | Native server-side tools and the two-level capability gate |
| [Embedding Models](/docs/embedding-models/) | Per-vendor embedding support and model selection |
| [Reranking](/docs/reranking/) | Pluggable + managed rerankers (Cohere / Voyage / Vertex / Bedrock) |
| [Chat](/docs/chat/) | Using the chat interface with configured LLM instances |
| [Token Usage](/docs/token-usage/) | Monitor token consumption per instance |
| [Configuration Reference](/docs/configuration-reference/#turing-es-core) | `turing.ai.crypto.key` and other application settings |

