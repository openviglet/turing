---
title: "Search Engine"
description: "Configure and manage Search Engine instances in Viglet Turing."
---
The **Search Engine** page (`/admin/se/instance`) manages the search backends that power Semantic Navigation Sites. It is accessible from the **Enterprise Search** section of the sidebar.

Each **Search Engine instance** is a configured connection to a search backend. Semantic Navigation Sites bind to a specific instance when their cores (collections) are created. Viglet Turing supports four backends via a plugin architecture: **Apache Solr** (recommended), **Apache Lucene** (embedded), **Elasticsearch**, and **Adobe AEM Content AI**.

The first three are indexes Turing owns and writes to. Content AI is different: it queries an index **Adobe** owns and fills, so it is read-only (see [Adobe AEM Content AI](#content-ai) below before choosing it).

---

## Instance Listing

The page displays all configured instances as a grid of cards (title and description). Use the **"New"** button to add an instance. When no instances exist, a blank state guides you to create the first one.

---

## Create / Edit Form

### General Information

| Field | Required | Description |
|---|---|---|
| **Title** | ✅ | Display name for this instance: shown in SN Site dropdowns |
| **Description** | | Free-text notes about this instance |

### Connection Settings

| Field | Required | Description |
|---|---|---|
| **Vendor** | ✅ | Backend type: `SOLR`, `LUCENE`, `ES`, or `CONTENTAI` |
| **Endpoint URL** | ✅ | Connection URL for the backend service |

Default endpoint URLs per vendor:

| Vendor | Default Endpoint URL |
|---|---|
| **SOLR** (Apache Solr) | `http://localhost:8983/solr` |
| **LUCENE** (embedded Apache Lucene) | `/data/turing/lucene` |
| **ES** (Elasticsearch) | `http://localhost:9200` |
| **CONTENTAI** (Adobe AEM Content AI) | none: paste the Content AI base URL Adobe gave you, e.g. `https://author-p1234-e5678.adobeaemcloud.com/adobe/experimental/aemcontentai-expires-20261231/contentAI` |

:::note
The Content AI base URL carries both a version and an expiry date, so paste it
exactly as Adobe's developer portal shows it rather than assembling it by hand.
:::

---

## Instance Detail Pages

After creating an instance, its detail page exposes three navigation sections.

### Settings

Editing form for all fields listed above, plus a **Delete instance** button. An instance cannot be deleted if any SN Site cores are still using it.

---

### Cores (Collections)

Manages the search indices (called **cores** in Solr terminology) within this search engine instance. Each Semantic Navigation Site locale maps to one core.

#### Core Listing

The list shows all cores found in the connected backend:

| Column | Description |
|---|---|
| **Name** | Core identifier (e.g., `my-site_en_US`) |
| **Documents** | Number of indexed documents (`numDocs`) reported by the engine |
| **Sites** | Badges showing each SN Site and locale using this core, with a country flag for quick recognition |

Cores are grouped by locale pattern: `{base}_{lang}_{COUNTRY}` (e.g., `product-docs_pt_BR`). A **search/filter** field at the top narrows the list by core name.

#### Core Actions

| Action | Description |
|---|---|
| **Delete core** | Permanently removes the core and all its indexed data. **Blocked** if the core is in use by any SN Site, the UI shows which sites and locales are preventing deletion. |
| **Clear documents** | Erases all indexed documents from the core without removing the core itself. Useful for a clean re-index without reconfiguring site associations. |

#### Create a New Core

| Field | Required | Description |
|---|---|---|
| **Name / Name Prefix** | ✅ | Core name or prefix |
| **Locale** | ✅ | Language and country (e.g., `en_US`, `pt_BR`) |
| **Append locale to name** | | Toggle: when enabled, the locale is automatically appended to the prefix, generating the canonical name (e.g., prefix `my-site` + locale `en_US` → `my-site_en_US`) |

A **name preview** shows the final core name as you type, before saving.

:::tip[Naming convention]
Use the `{site-name}_{lang}_{COUNTRY}` pattern consistently. This makes it easy to identify which site and locale each core belongs to when browsing multiple instances.
:::

---

### System Information

Live monitoring panel for the connected backend:

| Item | Description |
|---|---|
| **Status badge** | `UP` (green) or `DOWN` (red): real-time connectivity check |
| **Engine version** | Solr or Elasticsearch version string |
| **Lucene version** | Underlying Lucene library version |
| **Operating system** | OS name and version of the engine host |
| **Java version** | JVM version running the search engine |
| **JVM memory** | Heap usage reported by the engine |
| **Other properties** | Additional dynamic properties returned by the engine's status API |

---

<a id="content-ai"></a>

## Adobe AEM Content AI

If you already run **Adobe AEM Content AI**, you are already paying for a vector index
over your published AEM content. The `CONTENTAI` vendor lets Turing query that index
directly, so you do not embed the same content a second time. You keep the index you
already pay for, and Turing adds the layer above it: multi-turn chat, AI agents,
skills, facets, its own analytics, and every non-AEM source Content AI does not reach.

### What it can and cannot do

Turing does not own this index: Adobe fills it, using Adobe's embedding model and
the schema declared in Adobe's IndexConfig. So the backend is **read-only**, and that
is deliberate rather than unfinished:

| Works | Does not |
|---|---|
| Search, including semantic + keyword together | Indexing content from Turing or a connector |
| Facets (from the source's filterable fields) | Creating, clearing or deleting a core |
| Sorting and paging | Adding or changing fields in the schema |
| Document count and live status | Deleting documents |

A write attempt fails with a message saying so rather than silently doing nothing, so
you cannot end up believing a document was published when it was not. **Do not point a
connector at an SN Site backed by Content AI**. Publish that content in Adobe (AEM
publish, or Content AI's own acquisition service) and let Turing query it.

### Setting one up

1. In the **Adobe Developer Console**, add the "AEM Content AI" card to your project
   and note the base URL and the credential it issues.
2. Create a Search Engine instance here with **Vendor** `CONTENTAI` and that base URL.
3. Set the credential and switch the backend on: `turing.content-ai.enabled` plus
   `turing.content-ai.token` (or `api-key`). See
   [Configuration Reference → Adobe AEM Content AI](/docs/configuration-reference/#content-ai).
4. Tell Turing which Adobe content source each site queries:
   `turing.content-ai.sources.<site-name>`. Without a mapping, the site's core name is
   used as the source name.
5. Bind an SN Site to the instance and search.

### What to expect

- **Relevance is Adobe's.** The embedding model and the ranking are theirs, so Turing's own hybrid ranking and reranker settings do not apply. `hybrid`,
  `vector-boost` and `fulltext-boost` are the whole of the tuning available.
- **Pages are shallower.** Adobe returns at most 50 results per page and offers no way
  to jump to a page, so deep pagination is bounded by `max-page-walk`; beyond it a page
  comes back empty (the total is still correct).
- **Facets need filterable fields.** A facet field the Adobe source does not carry as
  filterable simply returns no values.
- **It is a hard dependency of the answer.** Search on that site stops working if the
  Content AI subscription lapses or the credential is revoked. **System Information**
  reports `DOWN` when no credential is configured.

### "My Content AI site returns nothing"

Adobe answers a query naming a source it does not have with **zero results**, the same
answer as a source that exists and matched nothing. Turing therefore checks your
configuration against Adobe rather than letting you guess:

- **Instance detail → System Information** lists the content sources your Adobe tenant
  actually has, next to the mapping you configured. A source name that does not resolve
  is marked `(NOT FOUND)`. Start here.
- **The first empty result** on a site logs a warning naming the source you asked for
  and the ones available, so the answer is in the log even if nobody opens the console.
- **Cores (Collections)** lists the real source names, so you can copy one instead of
  typing it. Document counts read `-1` there: Turing does not own these indexes, and
  a `0` would look like "this source is empty".

Three more settings fail the same quiet way, and are reported in the same warning:

| Setting | If it is wrong |
|---|---|
| `source-type` | The search names the type, so it resolves against the wrong index |
| `vector-space` / `lexical-space` | Adobe silently falls back to its default space, so relevance is not what you configured |
| A facet field | If the Adobe source does not mark that field filterable, the facet renders with no values |

:::tip
If **System Information** says `sources: unavailable`, the credential or the endpoint URL
is the problem, not the source name.
:::

---

### Grounding an AI answer on a Content AI site

Adobe holds the embeddings, so Turing's own vector store is empty on such a site and
the **RAG chat** reports itself unavailable (correctly) rather than answering from
nothing.

What does work is a [Custom Tool](/docs/custom-tools/) calling `turingSearch.sn(...)`: it
queries the Content AI index and, with `turing.content-ai.include-chunks` enabled, each
hit's `content` is the **passage Adobe matched** rather than the whole AEM page. That is
better grounding than a truncated page body, and it is the only sub-document grounding
available on a federated site, since Turing cannot re-chunk an index it does not own.

:::tip[The vectorless copilot works here]
The [vectorless copilot](/docs/rag/) needs no embeddings: it turns prose into a grounded
query over the site's declared fields and answers strictly from the matched rows, which
makes it the grounded-answer path that suits a federated index. It runs through the [DSL
API](/docs/dsl-compatibility/#adobe-aem-content-ai-a-listed-subset), which serves a defined
subset of the query syntax on Content AI.
:::

---

## Plugin Architecture

Turing uses a plugin architecture to support multiple search backends behind a unified interface. The active plugin is resolved at runtime based on the vendor configured per instance. If a vendor is unrecognised, the factory falls back to Solr.

Solr, Lucene and Elasticsearch read a Semantic Navigation search the same way: facet filters (`fq`, `fqAnd`, `fqOr`, `fqOperator`, `fqItemOperator`, custom facet selections) narrow the results, and `sort` takes `relevance`, `newest`, `oldest`, `field:asc|desc` or a [custom sort](/docs/semantic-navigation/#custom-sort) name. A site moved between them keeps its filters and sorts; how the free text is matched is each engine's own.

On **Lucene**, a text field matches regardless of accents (`saude` finds `Saúde`) and of plural or singular, in the language of the site's locale: on an `en_US` site `jackets` finds a product titled *Waterproof jacket*, and on a `pt_BR` site `jaquetas` finds *Jaqueta de chuva*. English, Portuguese, Spanish, French, German, Italian and Dutch are stemmed; other languages match the exact word. The same rules apply to Semantic Navigation search and to the [DSL API](/docs/dsl-compatibility/). Facet, id and autocomplete fields are never stemmed. A plural search finds singular-spelled documents right away; reindex the site so a singular search also finds documents spelled in the plural.

For the full interface reference (all methods across search, index management, schema management, and document operations) and instructions on implementing a new backend, see [Developer Guide → Search Engine Plugin Architecture](/docs/developer-guide/#search-engine-plugin-architecture).

---

## Protections

| Scenario | Behaviour |
|---|---|
| **Delete core in use** | Blocked: the UI displays a list of SN Sites and locales currently using the core |
| **Delete instance in use** | Should be avoided: removing an instance with active sites will break indexing and search for those sites |

Repository-level **caching** is enabled for search engine instances to avoid repeated database reads during high-frequency searches.

---

## Related Pages

| Page | Description |
|---|---|
| [Administration Guide](/docs/administration-guide/) | Full console reference |
| [Field Manifest & Schema-as-Code](/docs/manifest/) | Declaratively provision/evolve a site's field schema; field-coverage observability |
| [Semantic Navigation](/docs/semantic-navigation/) | How SN Sites use cores and search engines |
| [Architecture Overview](/docs/architecture-overview/) | Solr, Elasticsearch, and Lucene in the system architecture |
| [Configuration Reference](/docs/configuration-reference/#solr) | Solr and Elasticsearch timeout settings in `application.yaml` |
| [Configuration Reference → Adobe AEM Content AI](/docs/configuration-reference/#content-ai) | Credential, source mapping and query settings for the `CONTENTAI` vendor |
| [AEM Connector](/docs/integration-aem/) | Indexing AEM content **into** Turing, the alternative to federating to Content AI |

---


