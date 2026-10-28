---
title: "Synonyms"
description: "Author query-time synonyms once and Viglet Turing pushes them into Solr, Elasticsearch or Lucene with no reindex, plus a one-click import from Algolia and AI-assisted synonym suggestions mined from your search logs."
---
Synonyms make search forgiving: a shopper who types **tv** should still find products described as **television**, and **notebook** should match **laptop**. In **Viglet Turing** you author synonyms **once**, per Semantic Navigation Site and locale, and Turing pushes them into whichever search engine backs the site (Solr, Elasticsearch or the embedded Lucene store) as a **query-time** rule that takes effect **without reindexing**.

Manage synonyms from a Semantic Navigation Site: open the site and choose **Synonyms** from its settings.

## Synonym types

Turing supports the five synonym kinds you may know from other engines:

| Type | What it does | Example |
|---|---|---|
| **Regular (multi-way)** | A set of fully-equivalent terms: a query for any one matches records containing any other. | `tv`, `television`, `telly` |
| **One-way** | An input term expands to alternatives, but not the reverse. | `tablet` → `ipad`, `galaxy tab` |
| **Alternative correction (1 typo)** | A term treated as a one-typo variant of the targets. | `tshirt` ≈ `t-shirt` |
| **Alternative correction (2 typos)** | Same, allowing a two-typo distance. | `colour` ≈ `color` |
| **Placeholder** | A tokenised slot in record text that matches any value from a set. | `<streetnumber> street` |

Each rule belongs to one **locale** (synonyms rarely hold across languages) and has an **enabled** toggle, so you can stage a rule and turn it on when you're ready.

## How each engine applies them

Synonyms are a Turing-owned, engine-neutral model; the engine plugin translates each rule into that engine's query-time mechanism:

- **Solr**: written through the Managed Synonyms resource and applied by a query-time synonym graph filter; the core is reloaded so the change is live with no reindex.
- **Elasticsearch**: pushed through the Synonyms API as a synonym set that a search-analyzer synonym-graph filter reads, updatable without reindex.
- **Lucene** (embedded store): compiled into an in-memory synonym map consulted by the query analyzer. Because that map lives in memory, Turing re-applies every Lucene site's synonyms when it starts, so they keep working after a restart or deploy. After a restart the live set is every **enabled** rule, whether or not it had been pushed before.

Engines differ in what they can represent, and Turing is honest about it rather than dropping a rule silently:

- **Alternative corrections** map to the engine's typo mechanism where one exists, otherwise they **degrade to a one-way synonym** (a note tells you when).
- **Placeholders** are **not supported on Lucene** and are surfaced as unsupported.

The admin page shows a small **engine-support banner** for the site's engine so you know before you author.

## Managing synonyms

1. **Create or edit a rule**: pick a type and locale, add the equivalent terms (for one-way and correction types you also give the input term), and save.
2. **Enable** the rules you want live.
3. **Push to engine**: click **Push to engine** to apply the site's enabled synonyms to its search backend. Because this is a query-time change, results reflect it immediately with no reindex.

## Import from Algolia

Migrating from Algolia? Turing reads your index's synonyms and maps all five Algolia types onto the Turing model in one step. On the Synonyms page choose **Import from Algolia**, enter your **Application ID**, an **API key** with synonym access, the **index name** and the target **locale**, and run the import. The rules land in Turing ready to review and push, closing the last gap in the [migration path](/docs/migration/).

## AI-assisted suggestions

Because Turing already computes embeddings for your content, it can **suggest** synonyms you haven't thought of. Click **Suggest from search logs**: Turing takes your most-searched terms, groups the ones that are semantically close, and adds each group as a **disabled** candidate rule. Nothing is ever applied automatically: you review the suggestions, keep the good ones, enable them, and push. This turns synonym upkeep from a chore into a suggested-and-approved workflow.

Two kinds of logged query are left out, because they are spellings of one search rather than synonyms. A query that uses search syntax (`work*`, a quoted phrase, `field:value`) is not mined. A term that another term in its group starts with (`worksho` next to `workshop`) is taken as a word typed half-way and dropped. An as-you-type search sent in [instant mode](/docs/rest-api/#post-native-search) is never logged at all.

> AI suggestions require a **default embedding model** to be configured (see [Embedding models](/docs/embedding-models/)). Without one, the suggestion action simply returns nothing.

## API

All actions are available under a Semantic Navigation Site's synonym endpoint (`/api/sn/{siteId}/synonym`):

| Method & path | Purpose |
|---|---|
| `GET /api/sn/{siteId}/synonym` | List synonyms (optional `?locale=` / `?q=` filters) |
| `POST /api/sn/{siteId}/synonym` | Create a rule |
| `PUT /api/sn/{siteId}/synonym/{id}` | Update a rule |
| `DELETE /api/sn/{siteId}/synonym/{id}` | Delete a rule |
| `POST /api/sn/{siteId}/synonym/batch` | Bulk create/update |
| `GET /api/sn/{siteId}/synonym/support` | Whether the site's engine supports synonyms |
| `POST /api/sn/{siteId}/synonym/apply` | Push synonyms into the engine (all locales, or one via `?locale=`) |
| `POST /api/sn/{siteId}/synonym/import/algolia` | Import an Algolia index's synonyms |
| `POST /api/sn/{siteId}/synonym/mine` | Mine candidate synonym sets from the search log (never auto-applied) |

