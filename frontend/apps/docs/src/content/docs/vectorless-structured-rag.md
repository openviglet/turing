---
title: "Vectorless (Structured-Data) RAG"
description: "Answer questions over a structured catalog (products, prices, specs, a model registry) grounded and cited, using only an LLM and your search index. No embeddings, no vector store."
---
**Talk to a structured catalog, grounded, cited, with no vector database.** Viglet
Turing can answer questions over your product catalog, price list, spec sheet,
or model registry using only a language model and your existing search index, no
embeddings, no vector store, and no chunking pipeline to stand up.

---

## What it is

Most RAG (Retrieval-Augmented Generation) needs a vector database: your content is
chunked, each chunk is turned into an embedding vector, stored in an [Embedding
Store](/docs/embedding-stores/), and queries are matched by cosine similarity. That
is the right tool for **unstructured prose**: articles, manuals, support tickets.
See [What is RAG?](/docs/rag/) for that path.

But when your knowledge is **structured** (rows with named fields such as `price`,
`contextWindow`, `category`, `inStock`, `rating`) you do not need any of that.
Turing parses the user's question into a search over your **declared field
schema**, runs it against the search engine (Solr / Elasticsearch / Lucene) as
ordinary field / facet / range queries, and the language model answers **strictly
from the matched rows**, citing each claim as `[1]`, `[2]`, …

This is **Vectorless (Structured-Data) RAG**, and it needs only a **default LLM**.

```
question ──▶ NL→filter over your declared schema ──▶ structured search (no embeddings)
                                                        │
              cited answer  ◀── LLM (grounded, [n]) ◀──┘
```

## When to use which mode

Each Semantic Navigation site has a **Knowledge Base Mode**, set on **Semantic
Navigation → (site) → Generative AI → Knowledge Base Mode**:

| Mode | Best for | Requires |
| ---- | -------- | -------- |
| **Vector** (default) | Prose / unstructured content: articles, docs, manuals, support tickets | Embedding model **+** vector store on the bound AI Agent |
| **Vectorless (Structured)** | A structured catalog: products, prices, specs, a model registry | **Only a default LLM** |
| **Hybrid** | A site that has both prose *and* structured attributes | The full vector setup (a superset of Vector) |

The default is **Vector**, so every existing site is unchanged. Choosing
**Vectorless (Structured)** makes the AI-Mode readiness check report the site as
ready as soon as a default LLM is configured: instead of asking you to wire up an
embedding model and a vector store you do not need.

## Vectorless disables embedding indexing entirely

This is the key guarantee to be sure of when you pick the mode. When a site is set
to **Vectorless (Structured)**, Turing **never generates embeddings for it**, on
any path:

- **No embedding at import / reindex.** Documents are indexed into the search
  engine as structured fields only; nothing is embedded into a vector store. The
  **Reindex embeddings** button disappears from the GenAI form.
- **Even if the AI Agent has RAG on.** A site with no AI Agent of its own falls
  back to the global **Default AI Agent**. In Vectorless mode this fallback no
  longer causes embedding: the site stays vectorless **even when that Default AI
  Agent (or a directly-bound agent) has RAG enabled**. The mode is the deciding
  switch, not the agent.
- **No ANN (vector) search.** The **ANN Search** launcher and the vector-based
  hybrid ranking are turned off for the site; its public search runs on the pure
  lexical path.

The GenAI form shows an explicit **"Embedding indexing is OFF for this site"**
notice while Vectorless is selected, so the configuration is never a surprise.
Switch back to **Vector** or **Hybrid** to re-enable embedding indexing.

## How to turn it on

1. **Index your structured records.** Push them to `POST /api/sn/import` (each
   record's attributes become searchable fields), or configure the scheduled
   JSON-feed ingester (`turing.genai.structured-feed`) to *pull* a remote feed on
   a schedule. A deployment can even auto-provision the whole site from env config.
2. **Declare the field schema.** Enable the fields you want the assistant to reason
   over (`price`, `contextWindow`, `kind`, …) as Semantic Navigation site fields,
   with facet/type set correctly. The NL→filter step may only reference **declared**
   fields, anything it cannot ground is dropped, and it never invents a field.
3. **Set Knowledge Base Mode = Vectorless (Structured)** on the site's Generative
   AI form.
4. **Configure a default LLM** in Global Settings.

## Rendering titles, descriptions and facet labels

A structured feed carries **its own** field names: a model catalog indexes `label`,
`vendor`, `kind`, `contextWindow`, … and has no `title` or `abstract`. Search results
and facets are rendered through the site's *default field* mapping and its facet
labels, so a feed-indexed site can return the right number of results while showing
blank titles and label-less facet headers.

**Safe defaults, out of the box.** A site that declares no mapping falls back to the
conventional field names (`title`, `abstract`, `text`, `url`, `image`,
`publication_date`), and a facet field with no label configured gets a **humanized
field name** as its group header (`pricing_inputPer1M` renders as *"Pricing Input Per
1M"*). Nothing is ever blank, but the fallback can only name conventional fields, so
declare the mapping when your feed's names differ.

**Declare it in the feed source.** When the scheduled ingester provisions the site,
it can wire the mapping for you:

```yaml
turing:
  genai:
    structured-feed:
      sources:
        - id: model-catalog
          site-name: model-catalog
          feed-url: https://example.com/catalog.ndjson
          manifest-url: https://example.com/query-manifest.json
          provision: true
          # which feed field renders as what
          default-title-field: label
          default-description-field: summary
          default-url-field: url
          # facet group headers, by field name
          facet-labels:
            kind: Model Type
            vendor: Vendor
            pricing_inputPer1M: Input price (per 1M tokens)
```

Both are applied only where the value is still **blank**: re-running provisioning is a
no-op, and anything you later change on the site's admin forms is never overwritten.
Declaring a label does not turn a field into a facet: that stays the field manifest's
decision.

**Or declare labels in the field manifest.** A field in the manifest may name its own
facet header, either as a plain string or as a per-locale map:

```json
{
  "fields": [
    { "name": "kind", "type": "STRING", "facet": true, "facetName": "Model Type" },
    {
      "name": "vendor",
      "type": "STRING",
      "facet": true,
      "facetName": { "default": "Vendor", "pt_BR": "Fornecedor" }
    }
  ]
}
```

Locale-specific labels configured on the site always win over both of the above.

## Asking questions

`POST /api/sn/{siteName}/copilot`

```json
{
  "messages": [
    { "role": "user", "content": "cheapest embedding model with at least 1M context?" }
  ],
  "locale": "en"
}
```

The response is a grounded answer plus a `citations[]` array: each citation is a
matched row (id, title, optional link, score) the answer is allowed to reference.
Multi-turn is supported: pass the running conversation and the latest user turn
drives retrieval.

`GET /api/sn/{siteName}/copilot/available` reports whether a default LLM is
configured (i.e. whether the copilot can answer).

The site's regular chat endpoints answer through the copilot too, so a chat UI
built for a vector site works unchanged on a vectorless one:
`POST /api/sn/{siteName}/chat/conversation` (and the single-turn
`GET /api/sn/{siteName}/chat`) route a vectorless site's turns to the copilot,
and `GET /api/sn/{siteName}/chat/enabled` reports it ready on exactly the same
condition: a usable default LLM, otherwise `MISSING_DEFAULT_LLM`. The chat
endpoint returns the copilot's answer as one message once it is complete,
rather than streaming it token by token.

### Ranking & superlative questions

Superlative and comparative questions are understood out of the box: "the
**cheapest** embedding model", "which chat model scores **highest** on the
intelligence index", "the model with the **biggest** context window", "chat models
**sorted by** price". Turing recognises the ranking phrase and sorts the results by
the matching **numeric** field in your declared schema (ascending for a minimum
(cheapest / lowest / smallest / fastest) descending for a maximum, highest /
largest / most / best), while still keeping any facet filter in the same question
("**chat** models sorted by intelligence index" stays scoped to chat models).

This works generically for any catalog: the sort field is resolved from your
fields' **names and descriptions**, so a clear description
(`"Overall intelligence index"`, `"Input price per 1M tokens (USD)"`,
`"Maximum context window in tokens"`) is what lets Turing map "most intelligent",
"cheapest" or "biggest context" to the right field. Give each ranking field a
descriptive `description` in its manifest and superlative questions just work.

Matching is done with a language analyzer rather than a word list, so inflections
collapse on their own: a field named `pricing_inputPer1M` is reached by "price",
"prices" or "priced" without any of them being configured.

#### Ranking languages

The ranking vocabulary follows the **request locale**. English (`en`), Portuguese
(`pt`) and Spanish (`es`) ship, so "modelo **mais barato**", "**maior** janela de
contexto", "modelos **ordenados por** preco" and "el modelo **mas barato**" resolve
the same sorts their English equivalents do. Any other locale ranks with the
English vocabulary, which is what every locale did before.

#### Teaching Turing your catalog's own ranking words

If your catalog has a quality of its own ("the **torquiest** engine", "the
**lightest** frame"), you can teach it, per site, under **Semantic Navigation →
your site → GenAI → Copilot Query Planning → Ranking vocabulary**.

The field takes a small JSON bundle. It **extends** the shipped vocabulary rather
than replacing it, so list only what is missing:

```json
{
  "ascWords": ["lightest"],
  "descWords": ["torquiest"],
  "synonyms": {
    "lightest": ["weight", "mass"],
    "torquiest": ["torque"]
  }
}
```

| Key | What it declares |
| --- | --- |
| `ascWords` | Words that mean *smallest first*: the sort is ascending |
| `descWords` | Words that mean *largest first*: the sort is descending |
| `synonyms` | Which of your fields a word is about: the word on the left, tokens from the target field's name or description on the right |
| `sortTriggers` | Extra "sorted by" phrasings, e.g. `["ranked on"]` |
| `stopwords` | Words to ignore when matching a field |

Leave it empty to rank with the shipped vocabulary alone. A bundle that does not
parse is ignored at ranking time: a typo costs you the extra words, never the
sort.

Which is exactly why the field has a **Check vocabulary** button next to it. Type
a question your users would ask, press it, and Turing answers with three things:
whether the bundle parsed, which words it added that the shipped vocabulary did
not already have, and the sort your sample question actually resolves to. That
last one is the answer you cannot get any other way: a bundle can parse, add a
real word, and still resolve nothing because the word bridges to no numeric field
in your schema. The check works on what is in the box, so you can try a bundle
before saving it.

To add a whole language, or the same vocabulary to every site at once, point
`turing.genai.copilot.ranking.lexicon-dir` at a directory of
`lexicon-<language>.json` files in the same shape. Each is merged over the shipped
bundle for that language, and a file for a language Turing does not ship (say
`lexicon-fr.json`) adds it. Name its `stemmer` after the Snowball language
(`French`, `Italian`, `German`, …) so inflections still collapse:

```yaml
turing:
  genai:
    copilot:
      ranking:
        lexicon-dir: /etc/turing/lexicons
```

After editing a file there, reload the lexicons (no restart needed):

```bash
curl -X POST https://your-turing/api/system/cache/ranking-lexicons/reload
```

Clearing all application caches (`POST /api/system/cache/clear`) does it too. The
next question rebuilds from the resources and the directory as they are then,
including for any site whose own vocabulary was merged on top.

### Query-planning strategy

Turning a question into a structured search is a step you can choose a strategy
for, per site, under **Semantic Navigation → your site → GenAI → Copilot Query
Planning**:

| Strategy | What it does | Cost per question |
| --- | --- | --- |
| **Deterministic** (default) | Ranking phrases are resolved without an LLM (as described above), and one LLM call maps the rest of the question onto your facets. Fully reproducible. Ranking phrases are understood in English, Portuguese and Spanish; any other language ranks with the English vocabulary unless you add one. | 1 LLM call |
| **LLM-assisted** | Multi-pass: parse, then an LLM pass audits the query against the question (dropped filter? missing sort? undeclared field?), then a refine pass restates it. Understands questions in **any language** with no per-language configuration, and recovers when the first parse comes back empty. | up to 3 LLM calls |
| **Hybrid** | Runs the deterministic plan first and only escalates to the audit/refine passes when it returned nothing, or produced no filter and no ordering. Common questions stay instant and free. | 1 LLM call, 3 only on a failed search |
| **Default** | Pins nothing on the site: inherits the deployment setting below. | n/a |

Pick **Hybrid** if you have seen a question return no results that clearly should
have matched, or your users ask in a language with no ranking vocabulary. Pick
**LLM-assisted** if most of your traffic is in such a language. **Deterministic**
(the default) is the cheapest and needs no LLM budget beyond the single parse, and
since it understands English, Portuguese and Spanish ranking phrases, a
multilingual audience is no longer a reason on its own to leave it.

**Analysis depth** bounds how many passes the LLM strategies may spend after the
initial parse: `0` = parse only, `1` = + the audit pass, `2` = + the refine pass.
The control is only active on a strategy that spends passes.

Deployment-wide defaults (used by every site set to *Default*):

```yaml
turing:
  genai:
    copilot:
      planning:
        strategy: DETERMINISTIC # or LLM_ASSISTED / HYBRID
        max-passes: 2           # 0 = parse only, 1 = + audit, 2 = + refine
```

Every strategy keeps the same guarantees: each field the LLM proposes is checked
against your declared schema before it reaches the index (an invented field is
dropped, never queried), and a failed pass falls back to the previous plan rather
than erroring.

#### Comparing the strategies on your own catalog

Rather than guess which strategy suits your data, measure it. An
[NL→facet eval pack](/docs/agent-eval/) (the prose questions your users ask, each
with the filters/ordering it should produce) can be run through **every** strategy
at once, scoring them side by side:

- In the admin, open **Eval Studio → NL→Facet**, import your pack as a dataset, and
  press **Compare planners**.
- Over the API:

  ```bash
  curl -X POST https://<turing>/api/sn/nl-facet-eval/planning-comparison \
    -H 'Content-Type: application/json' \
    -d '{ "pack": { … }, "strategies": ["DETERMINISTIC","LLM_ASSISTED"], "maxPasses": 2 }'
  ```

  `strategies` is optional (omit it to compare all three) and `maxPasses` overrides
  the analysis depth for the run. A saved dataset can be compared directly with
  `POST /api/sn/nl-facet-eval/dataset/{id}/planning-comparison`.

The report gives one row per strategy with the three numbers the choice turns on,
**score** (how many of your golden filters/ordering the plan reproduced), **LLM
calls** (the cost per run) and **elapsed** (the latency), plus the per-case
breakdown for the questions that missed.

:::caution[Read the caveats printed with the table]
The comparison scores the query each strategy **plans**; it does not execute it
against your index. That matters for **Hybrid**: its escalation only triggers on a
live search that came back empty, which a plan-only run cannot produce, so Hybrid
scores exactly like Deterministic here. That is its fast-path, not what it does in
production. Elapsed time is real LLM wall-clock and varies run to run; compare
orders of magnitude, not milliseconds.
:::

Because an LLM-assisted row costs up to three LLM calls **per case**, restrict
`strategies` (or lower `maxPasses`) when the pack is large.

### Building a kit from a plan

A shopper often does not know every item to ask for. Describe the plan instead, and
the copilot returns the needs it implies, each with the catalog products that fill it:

`POST /api/sn/{siteName}/copilot/kit`

```json
{
  "plan": "two days crossing the mountains in July, first time",
  "locale": "en",
  "maxGaps": 5,
  "perGap": 3,
  "availabilityField": "in_stock",
  "availability": "MARK",
  "guideTypes": ["guide", "checklist"]
}
```

Only `plan` is required. The response carries a one-sentence `summary`, the `stages`
(each with the need, why the plan needs it, the search it ran and its `products`),
the needs the catalog had nothing for in `unfilled`, and up to two `guides`.

- **Products come from the site's own search, never from the model.** One LLM call
  names the needs; each need then runs the same retrieval as a copilot question, so
  the site's planning strategy and hybrid ranking apply, and no id or price is the
  model's. A product staged under one need is not repeated under another.
- **Needs are worded in the catalog's own terms.** The call that names the needs is
  shown the most common values of the site's facet fields (category, type and the
  like), so a need searches for `fleece` when that is what the catalog calls it,
  rather than a phrase the model made up. When one of those values names the need
  outright (a `Waterproof jackets` category for a rain shell), the need searches
  inside it: its words run with that value as a filter. A value the site does not
  have is ignored, and when the filter finds nothing the words run on their own.
- **Each product says why it fits from its own facet values** (`reason`, e.g.
  `Category: Tents; Capacity: 2 people`), the ones matching the need first. Only the
  site's facet fields count, each under its facet label in the request's locale, so
  plumbing fields such as a site key or an image URL never appear. A product with no
  facet value gets `reason: null`.
- **Availability.** Name the field that says whether a product can be bought in
  `availabilityField`. With `availability: "MARK"` (the default) an unavailable
  product stays, with `available: false`; with `"DROP"` it is left out. Values such
  as `false`, `0`, `out of stock` and `sold out` read as unavailable.
- **Guides.** List the document types that count as a guide on your site in
  `guideTypes`, and the documents of those types the plan itself retrieves are cited
  beside the stages. Without `guideTypes`, no guide is searched. A document of one
  of those types is never staged as a product, even when a need's search finds it:
  the stage fills from the next products, and the document stays a guide.

Both SDKs call it as `postCopilotKit` (`@viglet/turing-sdk` and
`@viglet/turing-react-sdk`); gate it on `/copilot/available`, like the copilot.

### From React (SDK)

The `@viglet/turing-react-sdk` ships a `useTuringCopilot` hook and an embeddable
`TuringCopilot` widget bound to the endpoint above, the copilot analogue of the
vector-RAG chat, fully skinnable via a `classNames` map. See the
[JavaScript SDK](/docs/javascript-sdk/).

## Guarantees

- **Grounded**: the answer asserts nothing that is not in the matched rows; the
  parser never invents a field.
- **Cited**: every claim carries the `[n]` of the row it came from, and the
  returned citations are limited to the rows the answer actually references.
- **Objective ranking**: results are ordered by relevance / objective signals
  only; no paid placement ever enters the pipeline.
- **Fail-open**: a parse failure degrades to a free-text query, a ranking failure
  degrades to lexical order, and an LLM failure still returns the matched citations
  so the client can render results without a synthesized answer.

## Over the OpenAI-compatible gateway (zero SDK)

Any OpenAI-style client can get grounded, cited structured answers with **no
Turing SDK** through the [Governed LLM Gateway](/docs/llm-gateway/), either a
`turing-copilot:<site>` model name or an `x-turing-copilot-site: <site>` header
routes the request to the vectorless copilot. The cited rows come back as a
plain-text **Sources** footer so the answer stays traceable over the OpenAI wire
format.

```bash
curl -sX POST https://<host>/v1/chat/completions \
  -H "authorization: Bearer sk-turing-…" \
  -H "content-type: application/json" \
  -d '{"model":"turing-copilot:model-catalog",
       "messages":[{"role":"user","content":"cheapest embedding model with >= 1M context?"}]}'
```

## Stuff-all mode (small catalogs)

For a catalog small enough to fit in one prompt (a few hundred rows), the copilot
can skip retrieval entirely and ground the LLM on **every** row, the
extreme-vectorless path. This is what makes open-ended advisory questions work
("which of these would suit a small documentation site?"): the model sees the whole
catalog rather than a filtered slice. It is **self-gating by size**: enabled by
default, it only activates when the whole index fits within a token budget and
document cap, and otherwise falls back to the normal filtered retrieval. Answers
stay cited and fail-open either way.

As a catalog grows, Turing adapts before giving up on whole-catalog grounding: it
first tries a full description-labelled row per document, and if that exceeds the
budget it retries a **compact** projection (one lean line per row covering only
your sortable and facet fields, no snippet or descriptions) which is roughly ten
times smaller, so several times more rows still fit. Only when even the compact
whole catalog is over budget does it fall back to filtered retrieval. If you want a
large catalog to keep answering advisory questions, raise the token budget
(`TURING_GENAI_COPILOT_STUFFALLTOKENBUDGET`); the application log states which mode
each request used and, when it fell back, by how much it was over.

Configure under `turing.genai.copilot`:

```yaml
turing:
  genai:
    copilot:
      stuff-all-enabled: true       # default
      stuff-all-token-budget: 40000 # ~a few hundred rows
      stuff-all-max-docs: 1000
```

## Indexing large catalogs (performance)

A structured catalog can be big: hundreds or thousands of rows. Turing indexes a
catalog-scale import in a **single bulk pass** rather than one document at a time,
so a large feed lands fast. This is automatic and needs no configuration: when a
batch of records arrives, they are grouped and written to the search engine in one
bulk request with a single commit, the field schema is converged once for the whole
batch (not re-checked per row), and the embedded **Lucene** engine writes the whole
batch in one writer session with one flush. If a bulk write fails, indexing falls
back to one-document-at-a-time automatically, so robustness is unchanged.

### Chunking the scheduled feed

When you use the scheduled JSON-feed ingester (`turing.genai.structured-feed`), a
large feed is split into **bounded chunks**: each chunk is imported and committed
on its own, with per-chunk progress in the logs, and one failing chunk never aborts
the rest. Tune the chunk size for very large feeds:

```yaml
turing:
  genai:
    structured-feed:
      batch-size: 200   # records per chunk (default 200; 0 = one unbounded chunk)
```

De-index of rows that vanished from the feed is still computed over the whole run,
so chunking never changes which stale rows are reconciled.

### Direct bulk-index fast path (advanced, opt-in)

For a **very large reindex of a Vectorless site**, the asynchronous indexing queue
buys nothing: its purpose is to offload embedding work, and a vectorless site does
none. An opt-in endpoint writes the documents straight to the search engine in
bounded chunks, skipping the queue:

```yaml
turing:
  sn:
    import:
      bulk-direct:
        enabled: false   # default off, the ordered queue stays the default path
        batch-size: 500  # documents per direct chunk
```

```
POST /api/sn/import/bulk      # same TurSNJobItems body as POST /api/sn/import
```

It is **vectorless-only**: if any target site has embeddings enabled, the request
transparently falls back to the normal queued import, never rejected, never
skipping a site's embedding. Leave it **off** unless you are reindexing a large
vectorless catalog and have measured that the queue is the bottleneck.

## Live example

The public [`openviglet/model-catalog`](https://openviglet.github.io/model-catalog/)
is dogfooded as the first vectorless knowledge base on `turing-demo.viglet.org`:
ask it *"cheapest embedding model with ≥ 1M context?"* and it answers with linked,
cited model rows.

