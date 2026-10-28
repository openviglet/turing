---
title: "Field Manifest & Schema-as-Code"
description: "Provision and evolve a Semantic Navigation site's field schema declaratively in Viglet Turing: idempotent manifests, planner/migrations, LLM-assisted derivation, hybrid ranking, and field-coverage observability."
---
A **field manifest** is a single declarative document that describes a Semantic Navigation site and its field schema, the site name, its locales, and the ordered list of fields (each with a type, and whether it's a facet, mandatory, or multi-valued). You `POST` it once and Viglet Turing **converges** the live site to match it: it creates the site if missing, adds any new fields, and reports what changed. Re-posting the same manifest is a no-op.

You'd reach for this when you want your search schema to live in version control next to your code instead of being clicked together by hand in the admin: "Liquibase for the SN field schema." It's the backbone of repeatable onboarding: a new environment, a new tenant, or a CI pipeline can stand up an identical, reviewed schema from one file. Around that core, Turing adds four things: a **planner** that diffs a manifest against the live site (so breaking changes can't slip through silently), **LLM-assisted derivation** that drafts a manifest from sample documents, **hybrid RRF ranking** for the public search, and **field-coverage** observability so you can see which fields are actually populated.

> This page is about the *structured-source indexing* line (Block R). For how documents get into the index in the first place, see [Search Engine](/docs/search-engine/); for how fields are searched and faceted, see [Semantic Navigation](/docs/semantic-navigation/).

---

## From zero: provision a site from a manifest

`POST /api/sn/manifest` with a manifest body. This creates-or-converges the `products` site and its fields:

```json
POST /api/sn/manifest
{
  "name": "products",
  "description": "Product catalog",
  "seInstanceId": "<search-engine-instance-id>",
  "schemaVersion": "1",
  "locales": ["en_US", "pt_BR"],
  "fields": [
    { "name": "title",  "type": "TEXT",     "mandatory": true,  "multiValued": false, "facet": false, "searchWeight": 3 },
    { "name": "brand",  "type": "STRING",   "mandatory": false, "multiValued": false, "facet": true  },
    { "name": "price",  "type": "CURRENCY", "mandatory": false, "multiValued": false, "facet": true  },
    { "name": "tags",   "type": "STRING",   "mandatory": false, "multiValued": true,  "facet": true  }
  ]
}
```

The response tells you exactly what converged:

```json
{
  "siteId": "...",
  "siteName": "products",
  "siteCreated": true,
  "fieldsCreated": ["title", "brand", "price", "tags"],
  "fieldsSkipped": [],
  "fieldsMigrated": [],
  "localesCreated": ["en_US", "pt_BR"],
  "schemaVersion": "1"
}
```

Run it a second time and everything moves to `fieldsSkipped`: the operation is **idempotent**. Add a field or a locale and re-post: that's purely additive, applied with no reindex.

:::note[Field types]
`type` is a `TurSEFieldType` enum name: `INT`, `LONG`, `STRING`, `TEXT`, `ARRAY`, `DATE`, `BOOL`, `FLOAT`, `DOUBLE`, `CURRENCY`. The same field spec is what the indexer carries at index time, the manifest path and the index-time auto-create path share one provisioner, so a manifested field and an auto-created field are identical.
:::

:::note[Search weight]
`searchWeight` is optional. It sets how much a query match in the field counts, from 1 to 100, the same as the **Search weight** attribute in the [field detail form](/docs/semantic-navigation/#field-detail). Above, a match in `title` counts three times a match in a field with no weight. Each provision applies the declared weight to the field, so a weight lives in the manifest instead of being re-set by hand in the console. Leave it out and the field keeps whatever weight it already has. A value outside 1 to 100 is refused with HTTP 400.
:::

### Custom facets

A manifest can also declare **custom facets**: named buckets over one field, counted in the site's structured facets like any other facet. A job site, for example, can offer a posting-date filter with *last 24 hours*, *last 7 days* and *last 30 days*:

```json
POST /api/sn/manifest
{
  "name": "jobs",
  "fields": [
    { "name": "publishedAt", "type": "DATE", "mandatory": false, "multiValued": false, "facet": false }
  ],
  "customFacets": [
    {
      "field": "publishedAt",
      "name": "postedSince",
      "defaultLabel": "Posted",
      "label": { "pt": "Publicada" },
      "items": [
        { "label": "Last 24 hours", "labels": { "pt": "Últimas 24 horas" }, "rangeStartDate": "NOW-1DAY" },
        { "label": "Last 7 days",   "labels": { "pt": "Últimos 7 dias" },   "rangeStartDate": "NOW-7DAYS" },
        { "label": "Last 30 days",  "labels": { "pt": "Últimos 30 dias" },  "rangeStartDate": "NOW-30DAYS" }
      ]
    }
  ]
}
```

Each item takes the same values as a custom facet item in the console:

| Item key | Meaning |
|---|---|
| `label`, `labels` | The bucket's default label (required) and its per-locale labels |
| `rangeStartDate`, `rangeEndDate` | Bounds for a `DATE` field: an ISO date (`2026-01-01`) or relative date math (`NOW-7DAYS`, `NOW/DAY`). Leave one out for an open range |
| `rangeStart`, `rangeEnd` | Bounds for a numeric field |
| `operator` | How the bounds apply, `BETWEEN_INCLUSIVE` when left out |
| `position` | The bucket's order; by default, its place in the list |

The facet's `field` must be declared in the same manifest or already exist on the site. Turing matches each declared facet to the live one by `field` and `name`: it creates a facet that is missing, replaces the labels and buckets of one that differs, and leaves an equal one alone, so re-posting the manifest changes nothing. A custom facet the manifest does not declare stays as the console left it. The response lists what changed in `customFacetsChanged`. An invalid bound, such as malformed date math, is refused with HTTP 400 before anything is written.

From Java, the `TurSNManifest` builder takes the same declaration:

```java
TurSNManifest manifest = TurSNManifest.builder()
        .name("jobs")
        .field(publishedAtSpec)
        .customFacet(TurSNManifestCustomFacet.of("publishedAt", "postedSince", "Posted", List.of(
                TurSNManifestCustomFacetItem.since("Last 24 hours", "NOW-1DAY"),
                TurSNManifestCustomFacetItem.since("Last 7 days", "NOW-7DAYS"),
                TurSNManifestCustomFacetItem.since("Last 30 days", "NOW-30DAYS"))))
        .build();
```

### Custom sorts

A manifest can declare **custom sorts** the same way: named, multi-level sort options a search selects by passing the name as `sort`. A job site that wants "closing soonest" sorts by the expiry date ascending and puts jobs with no expiry date last:

```json
POST /api/sn/manifest
{
  "name": "jobs",
  "fields": [
    { "name": "expiresAt", "type": "DATE", "mandatory": false, "multiValued": false, "facet": false }
  ],
  "customSorts": [
    {
      "name": "closingSoonest",
      "description": "Closing soonest",
      "items": [
        { "field": "expiresAt", "order": "ASC", "missing": "LAST" },
        { "field": "publishedAt", "order": "DESC" }
      ]
    }
  ]
}
```

| Level key | Meaning |
|---|---|
| `field` | The field to sort by (required). Declared in the same manifest or already on the site |
| `order` | `ASC` (default) or `DESC` |
| `missing` | Where documents with no value in the field go: `FIRST` or `LAST`. Leave it out to keep the search engine's own placement |

Levels apply in the order listed. The name must not be `relevance`, `newest` or `oldest`, which a search answers before looking for a custom sort. Turing matches each declared sort to the live one by `name`: it creates a sort that is missing, replaces the description and levels of one that differs, and leaves an equal one alone. A custom sort the manifest does not declare stays as the console left it. The response lists what changed in `customSortsChanged`, and the [plan](#dry-run-the-diff) in `customSortsToChange`.

From Java:

```java
TurSNManifest manifest = TurSNManifest.builder()
        .name("jobs")
        .field(expiresAtSpec)
        .customSort(new TurSNManifestCustomSort("closingSoonest", "Closing soonest", List.of(
                TurSNManifestCustomSortItem.asc("expiresAt").missingLast(),
                TurSNManifestCustomSortItem.desc("publishedAt"))))
        .build();
```

:::note[Engines]
Custom sorts, `missing` included, apply on Solr, Lucene and Elasticsearch sites.
:::

### Generative AI

A `genAi` block turns the site's generative AI on: chat, the [answer engine](/docs/answer/) and RAG retrieval. Turing creates the site's GenAI configuration if it has none.

```json
POST /api/sn/manifest
{
  "name": "jobs",
  "genAi": { "agentId": "default", "knowledgeBaseMode": "VECTOR" }
}
```

| Key | Meaning |
|---|---|
| `agentId` | The [AI agent](/docs/ai-agents/) the site answers through. `default` switches the site to the [Default AI Agent](/docs/ai-agents/#default-ai-agent-global-fallback). Leave it out (`"genAi": {}`) to keep whichever agent the site already uses, for example one an admin picked in the console; a site with no GenAI configuration then starts on the Default AI Agent. An id that names no agent is refused |
| `knowledgeBaseMode` | `VECTOR`, `VECTORLESS_STRUCTURED` or `HYBRID`. Leave it out to keep the site's current mode (`VECTOR` on a new configuration) |

Only these two settings are compared and written. Ranking, semantic recall and the RAG flags stay as the console left them, and a manifest without `genAi` does not touch the site's configuration. The response says `genAiChanged: true` when the configuration was created or changed, and the [plan](#dry-run-the-diff) says `genAiToChange`. From Java, `.genAi(TurSNManifestGenAi.keepAgent())`, `.genAi(TurSNManifestGenAi.defaultAgent())` or `.genAi(TurSNManifestGenAi.agent("my-agent"))`.

:::note[Changed in 2026.3.6]
Before this release, leaving `agentId` out meant the same as `default`, so re-posting a manifest rebound a site to the Default AI Agent. A connector that relied on that now sends `"agentId": "default"`.
:::

<a id="facet-behaviour"></a>

### Facet behaviour

A `facets` block sets how the site's facets behave: drill-down, where each selection narrows the results, or multi-select, where a visitor picks several values of one facet. In drill-down mode the link-bearing response drops a facet once it is applied, and the item counts narrow with each selection. The structured response keeps applied facets in either mode.

```json
POST /api/sn/manifest
{
  "name": "jobs",
  "facets": { "enabled": true, "type": "AND", "itemType": "OR", "itemsPerFacet": 20 }
}
```

| Key | Meaning |
|---|---|
| `enabled` | Whether the site returns facets at all |
| `type` | How facets of different fields combine: `AND` narrows, `OR` broadens |
| `itemType` | How the values of one facet combine: `OR` is multi-select, `AND` narrows, `DEFAULT` follows `type` |
| `itemsPerFacet` | How many values each facet lists (at least 1) |

Each key is optional, and one you leave out keeps the site's current setting. A manifest without `facets` does not touch them. The response says `facetSettingsChanged: true` when a setting changed, and the [plan](#dry-run-the-diff) says `facetSettingsToChange`. From Java, `.facets(TurSNManifestFacets.multiSelect())` or `.facets(TurSNManifestFacets.drillDown().withItemsPerFacet(20))`.

---

## Schema-as-code: planner & migrations

Additive changes (new field, new locale) are safe and silent. But changing an **existing** field's `type` or its `multiValued` flag is **breaking**: the search-engine field must be dropped and rebuilt, which means the source has to reindex. Turing refuses to do that implicitly.

### Dry-run the diff

`POST /api/sn/manifest/plan` returns the diff without mutating anything, ideal for review in a pull request:

| Diff bucket | Meaning |
|---|---|
| `fieldsToAdd` | New fields: additive, applied silently on provision |
| `localesToAdd` | New locales: additive |
| `fieldsUnchanged` | Already match the manifest |
| `breakingChanges` | A `type` or `multiValued` change on an existing field, requires a declared migration |
| `customFacetsToChange` | [Custom facets](#custom-facets) that provisioning would create or rewrite |

Facet / mandatory / description tweaks are **not** breaking: they don't require a reindex, so the planner applies them without ceremony. Neither is a `searchWeight` change: it changes ranking at query time, not the schema.

### Declaring a migration

If `plan` reports a breaking change and you provision anyway, `POST /api/sn/manifest` returns **HTTP 409** and mutates nothing. To apply the change, declare it explicitly in the manifest's `migrations` list as a `RECREATE` action for that field. On provision, Turing then drops the field across every locale core, deletes its rows, and recreates it from the new spec.

:::caution[RECREATE is destructive]
A `RECREATE` migration drops the field's indexed data. The source **must reindex** to repopulate it. This is the deliberate safety trade: breaking schema changes are explicit, reviewable, and never accidental.
:::

The `schemaVersion` field is an informational tag you bump as your schema evolves: it travels through to the provision result so you can audit which version a site is on.

---

## LLM-assisted derivation

Hand-declaring fields for an unfamiliar source is tedious. **Derivation** flips it around: feed Turing a sample of the source's documents and it returns a **draft** manifest for you to review.

`POST /api/sn/manifest/derive` with the site identity plus a `documents` array (parsed JSON objects):

```json
POST /api/sn/manifest/derive
{
  "name": "products",
  "seInstanceId": "<search-engine-instance-id>",
  "locales": ["en_US"],
  "documents": [ { "title": "...", "brand": "...", "price": 19.9 }, ... ]
}
```

How the draft is built (it works **with or without** an LLM):

1. **Analyze**: a pure analyzer computes per-field observations: coverage (% of docs that populate it), observed types, cardinality, multi-valued-ness, max length, and samples. A `null` value counts as **absent** (lowering coverage), never as present.
2. **Heuristics**: a deterministic baseline: numeric/date/boolean and low-cardinality string fields become facets; `TEXT` never facets; near-100%-coverage fields become mandatory; mixed numerics widen to `DOUBLE`.
3. **LLM refine (optional)**: when a default LLM is configured, it *refines* the baseline, grounded on the observations. It is conservative: it only refines **observed** fields, invented field names are dropped and omitted fields keep their heuristic spec; any LLM error falls back to the heuristic draft.

The draft is **never auto-provisioned**: you review it and `POST` it back to `/api/sn/manifest`. Check whether an LLM is available with `GET /api/sn/manifest/derive/available` → `{ "llmAvailable": true|false }`.

### The admin wizard

In the console, the SN site **Fields** page has a **"Generate from sample"** action that runs this end-to-end: paste or upload sample JSON/NDJSON → review the drafted field table → apply. Apply converges through `POST /api/sn/manifest` (not raw field creation), so existing fields are pre-excluded and re-applying is a no-op.

---

## Hybrid ranking for public search

Field manifests define *what* is searchable; **hybrid ranking** improves *how well* the public faceted search ranks results. It's an opt-in per-site mode that fuses keyword (BM25) and vector relevance using Reciprocal Rank Fusion.

| Mode | Behaviour |
|---|---|
| `LEGACY` *(default)* | Pure lexical (BM25) ranking: unchanged |
| `HYBRID_RRF` | Reorders the BM25 result page by fusing it with a vector pass (RRF, `k=60`) |

Because public SN documents aren't embedded by the lexical engine, Turing maintains a parallel per-site vector collection (`sn_<siteId>`) using the default embedding model + store, side-writing embeddings as documents are indexed/de-indexed. At query time it reorders **only** the current BM25 page, and **only** when sorting by relevance on a non-wildcard, ungrouped query, so **facet counts and pagination stay BM25-derived** (the ranking changes, the result set doesn't). It is fully **fail-open**: no default embedding model/store, an embedding error, or a store outage silently degrades to legacy lexical order. Configure it on the SN site's **Generative AI** page ("Hybrid Ranking (Public Search)").

Two settings on the same card tune the blend for the site:

| Setting | Behaviour |
|---|---|
| **Vector weight** | How much the vector rank counts against the keyword rank, from `0` (keyword order) to `1` (vector order). Blank is `0.5`, an even blend, which is the order hybrid sites always had. Raise it on a site whose titles name roles or concepts that visitors describe in other words |
| **Embedded fields** | The fields whose text is embedded, comma separated, in order (`title, description, skills`). Blank embeds `title`, `abstract` and `text`. Only the named fields are embedded. Documents indexed before a change keep their old embedding, so reindex the site after changing it |

### Semantic recall

Hybrid ranking reorders what the keyword search found, so a document that shares no word with the query never appears. A visitor searching *"trabalho com crianças"* on a job site never sees a job titled *"auxiliar de creche"*, however close the meaning. **Semantic recall** closes that gap: with it on, the vector pass may also *add* documents to the page, not just reorder it.

| Setting | Behaviour |
|---|---|
| **Semantic recall** *(off by default)* | On a hybrid ranking mode, adds up to 10 documents the vector search ranks above the similarity floor that the keyword page missed |
| **Recall similarity floor** | The similarity (0 to 1) a document needs before it is added. Blank uses `0.6`. Raise it if unrelated documents appear |

Recall runs only on the **first page** of a **relevance-sorted** query; later pages stay keyword-only. Active facet filters (`fq`, `fqAnd`, `fqOr`) still apply: a recalled document outside the filter is never added. On an Adobe AEM Content AI site, which cannot restrict a search to ids, filtered queries get no recall. Recalled documents are added to the result count and, on Solr, Lucene and Elasticsearch sites, to their facet buckets. Each recalled result carries `semanticRecall: true` in its ranking explanation. Turn it on in the same **Hybrid Ranking (Public Search)** card; it is disabled while the mode is `LEGACY`, and with it off a hybrid site ranks exactly as before.

> This is distinct from the RAG hybrid retrieval that powers the chat `search_knowledge_base` tool: different corpus, different surface. See [Reranking](/docs/reranking/) for that path.

---

## Field-coverage observability

Once a schema exists, the question becomes *"is it actually being populated?"* `GET /api/sn/{snSiteId}/field-coverage` answers it: for every **enabled** field, the percentage of indexed documents that populate it, summed across all locales, **worst-coverage first**.

```json
{
  "siteId": "...",
  "totalDocuments": 1200,
  "supported": true,
  "fields": [
    { "name": "warranty", "coverage": 0.12, "supported": true },
    { "name": "brand",    "coverage": 0.94, "supported": true }
  ]
}
```

The admin surfaces this as its own nav item with red / amber / green bars (`< 50%` / `< 80%` / else). A field whose search engine can't report a count comes back as **unsupported** (`-1` → shown as "unknown", never a misleading 0%): Solr, Lucene, and Elasticsearch all implement the count; engines that don't degrade gracefully.

A near-empty field is a signal: either the source isn't sending it (a connector mapping gap) or the field was declared optimistically. It's the data-quality counterpart to the manifest's *absent ≠ empty* contract.

---

## Endpoint reference

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/sn/manifest` | Provision/converge a site + fields from a manifest (idempotent) |
| `POST` | `/api/sn/manifest/plan` | Dry-run: return the diff without mutating |
| `POST` | `/api/sn/manifest/derive` | Draft a manifest from sample documents (never auto-applied) |
| `GET` | `/api/sn/manifest/derive/available` | `{ llmAvailable }`: is an LLM configured for refinement? |
| `GET` | `/api/sn/{snSiteId}/field-coverage` | Per-field population % (worst first) |

---

## Related Pages

| Page | Description |
|---|---|
| [Search Engine](/docs/search-engine/) | How content is indexed; field types and cores |
| [Semantic Navigation](/docs/semantic-navigation/) | Sites, fields, facets, and the public search |
| [Reranking](/docs/reranking/) | RAG hybrid retrieval for the chat tool (distinct corpus) |
| [Embedding Models](/docs/embedding-models/) | The model behind the `sn_<siteId>` hybrid vectors |
| [Import / Export](/docs/import-export/) | Other ways to move site config between environments |

