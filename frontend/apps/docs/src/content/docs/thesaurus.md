---
title: "Thesaurus"
description: "Enrich indexing with a curated, hierarchical controlled vocabulary: Viglet Turing recognises the terms a document mentions and expands it with their broader concepts at index time, adding recall, a concept-facet browse, and related-concept suggestions. Includes a seed library, XML import, and AI-assisted generation."
---
A **thesaurus** is a curated, hierarchical **controlled vocabulary**, the concepts of a domain, arranged from broad to narrow, with the synonyms and spelling variants people actually type. In **Viglet Turing** a thesaurus enriches your index with structure the search engine can't infer on its own: at **index time**, every document is expanded with the *broader concepts* of each term it mentions, written to a dedicated field. A page about **pneumonia** is silently tagged with **respiratory disease** and **disease** too, so a search for the broad term still finds the specific page, and visitors can browse by concept.

You'd reach for a thesaurus when embedding search and synonyms aren't enough on their own. Embeddings find text that is *semantically near*; [synonyms](/docs/synonyms/) rewrite the *query* so equivalent words match. Neither gives you a **curated, hierarchical, domain-controlled** vocabulary, the deliberate "these concepts nest under those" structure a librarian or subject-matter expert maintains. A thesaurus complements both: it works at **index time** on the *document*, so query time stays fast and simple.

The thesaurus admin lives at **`/bento/thesaurus`**. Everything below is **opt-in per Semantic Navigation Site**, a site that selects no thesaurus indexes exactly as before.

---

## How it's organised

```
Thesaurus (a library, per tenant)
└─ Microthesaurus  (one tree = one language + one domain, e.g. "Education · pt")
   └─ Term  (a preferred concept, "Pneumonia")
      ├─ broader / narrower   the hierarchy spine (BT / NT)
      ├─ related              associative links to sibling concepts (RT)
      └─ variations           surface forms used to recognise the term in text
```

- A **Thesaurus** is a reusable library scoped to your tenant. It holds one or more microthesauri.
- A **Microthesaurus** is a single tree for one **language** + one **domain** (e.g. `EDUCATION`, `MEDICINE`). Locale matters: only microthesauri whose language matches a document's locale participate in that document's indexing.
- A **Term** is a preferred concept. Its place in the tree is set by its **broader** term (its parent); **narrower** is just the other end of the same link. **Related** terms are associative ("see also") links that are *not* broader/narrower.
- **Variations** are the surface forms Turing looks for in document text, spelling variants, singular/plural, and non-preferred synonyms. Recognising a variation still expands to the *preferred* term's concept path.

The relation vocabulary follows the classic thesaurus standard: **BT** (broader), **NT** (narrower), **RT** (related), **U** (use) and **UF** (used-for). This is exactly what the **Turing Thesaurus Exchange** authority-file format carries, so a thesaurus round-trips through import and export intact.

---

## What it does at index time

When a site has selected one or more microthesauri, Turing adds a step to indexing for every document:

1. **Recognise**: scan the document's title, description and body for any term's surface forms (preferred label, variations, and use/used-for synonyms), matching whole words with configurable case/accent sensitivity.
2. **Expand**: for each recognised term, collect the labels on the path from the tree **root down to that term**, and *only* that path. A document about "pneumonia" is expanded with `Disease → Respiratory disease → Pneumonia`; it is **not** pulled into unrelated branches like "cancer".
3. **Write**: the collected concept labels are de-duplicated and written to a multi-valued field, **`microthesaurus_terms`**, provisioned automatically on the site.

All the cost is paid at index time. Query time gains two things for free:

- **Recall**: the field joins the query with a **low boost** (below title/body), so a search for a broad concept now also reaches the specific documents beneath it, without overwhelming relevance.
- **A concept facet**: the field's faceting companion becomes a "browse by concept" facet, the Semantic-Navigation-style drill-down.

> **Defensive by design.** If anything about the thesaurus is misconfigured, expansion is skipped and the document still indexes normally: a vocabulary problem can never block content from being searchable.

---

## Building a thesaurus

Open **`/bento/thesaurus`** and create a Thesaurus, then add a microthesaurus (name, language, domain). There are four ways to fill it with terms: use whichever fits.

### 1. Curate the term tree by hand

The microthesaurus page shows the terms as an expandable tree. You can:

- **Add** a root term or a child under any term, inline.
- **Rename**, add a **scope note**, or **enable/disable** a term (disabled terms are ignored at index time but kept for reference).
- **Reparent** a term by picking a new broader term.
- Edit **variations** (surface form + case/accent flags) and **relations** (Related / Use / Used-for, pointing at another term in the same tree).

Variation and relation counts show as small chips on each row, so the shape of the vocabulary is visible at a glance.

### 2. Import from the seed library

Turing ships a ready-made **Education** vocabulary in **Portuguese, English and Italian**. On the Thesaurus page, open **Seed library & import** and click **Import** on a seed to drop a fully-formed tree into your library, a rich starting point the engine alone can't produce. Seeds are *not* loaded automatically; you choose what to import.

### 3. Upload an authority file

Already have a controlled vocabulary? Click **Upload XML** and select a **Turing Thesaurus Exchange authority file** (`.xml`). Turing maps `BT`/`NT` onto the hierarchy, `RT`/`U`/`UF` onto relations, and `variations` onto recognition surface forms. Reciprocal pairs and duplicate edges are de-duplicated, and the source term ids are preserved for round-tripping.

### 4. Generate with AI

Don't have a vocabulary yet? Let the default LLM draft one. In the **Generate with AI** section, enter a **domain**, a **language**, and optional free-text **guidance** (e.g. *"focus on higher education and distance learning"*), then click **Generate draft**.

- Turing returns a **draft tree** rendered as an indented preview with related-term chips.
- **Nothing is saved automatically.** Review the draft, then click **Create microthesaurus** to persist it, or **Discard** and try a different prompt.
- On accept, the draft is materialised through the same authority-file import path, so its hierarchy, relations and variations land exactly as a hand-built or imported tree would.

> Generation uses your **default LLM** and requires a provider that supports structured output (OpenAI or Anthropic). If the default LLM can't produce structured output, generation reports a clear error rather than saving a malformed tree.

---

## Turning it on for a site

A thesaurus does nothing until a site opts in. Open a **Semantic Navigation Site**, go to its **Thesaurus** section, and:

1. **Select microthesauri**: add one or more from your library. Each selection has its own enable toggle. Only microthesauri whose language matches a document's locale expand that document.
2. **Configure indexing:**
   - **Enable expansion**: the master switch. Off ⇒ the site indexes exactly as before.
   - **Field name**: the backing field (default `microthesaurus_terms`).
   - **Boost**: the query-time weight for recall (default `0.8`, deliberately below title/body).
   - **Include synonyms**: also recognise `use`/`used-for` non-preferred terms and expand them to the preferred concept.
   - **Hierarchical concept facet** *(optional)*, also write **level-prefixed path tokens** (`0/Disease`, `1/Disease/Respiratory disease`, `2/Disease/Respiratory disease/Pneumonia`) to a second field (default `microthesaurus_path`) for a proper **drill-down** facet. Off by default; turning it on provisions the extra field.

Re-index (or index new content) after enabling, so documents pick up the expansion.

---

## Related-concept suggestions

Beyond enriching the index, a thesaurus can power a **"related concepts"** surface at query time: the associative sibling of spell-check's "did you mean". When a query mentions a controlled-vocabulary term, Turing can suggest the terms **related** to it (via `RT` links), so a search UI can offer *"you may also be interested in…"* chips.

- **REST:** `GET /api/sn/{site}/{locale}/related-terms?q=…` returns, for each recognised term, its label and its related labels. It's read-only, never throws, and returns nothing for a site that hasn't opted in or a query that mentions no thesaurus term. Like search and spell-check it needs no login, so a browser can call it directly; a site in `API_KEY` mode needs its key, as for search.
- **SDK:** both JavaScript SDKs wrap it. In React, [`useTuringRelatedTerms`](/docs/react-sdk/) mirrors `useTuringSpellCheck`, auto-lookup on the query, plus a `relatedLabels` convenience list:

  ```tsx
  const { relatedLabels } = useTuringRelatedTerms({ query, locale: "en" });
  return relatedLabels.map((label) => (
    <button key={label} onClick={() => search(label)}>{label}</button>
  ));
  ```

  The vanilla [`@viglet/turing-sdk`](/docs/javascript-sdk/) exposes the same call as `fetchRelatedTerms(site, locale, q)`.

---

## API

The thesaurus library is managed under `/api/kb`; per-site selection lives under a Semantic Navigation Site.

| Method & path | Purpose |
|---|---|
| `GET/POST /api/kb` | List / create thesauri |
| `GET/PUT/DELETE /api/kb/{kbId}` | Show / update / delete a thesaurus |
| `GET/POST /api/kb/{kbId}/microthesaurus` | List / create microthesauri in a thesaurus |
| `PUT/DELETE /api/kb/{kbId}/microthesaurus/{id}` | Update / delete a microthesaurus |
| `POST /api/kb/{kbId}/microthesaurus/import` | Import a Turing Thesaurus Exchange authority file (XML) |
| `POST /api/kb/{kbId}/microthesaurus/generate` | Draft a hierarchy with the LLM (returns a draft, never persisted) |
| `POST /api/kb/{kbId}/microthesaurus/from-draft` | Persist a reviewed generated draft |
| `GET/POST/PUT/DELETE …/microthesaurus/{id}/term…` | Manage terms, variations and relations |
| `GET /api/kb/seeds` · `POST /api/kb/{kbId}/seeds/{seedId}` | List the bundled seed library · import a seed |
| `GET/POST /api/sn/{siteId}/microthesaurus` | List / add a site's microthesaurus selections |
| `GET/PUT /api/sn/{siteId}/microthesaurus/config` | Read / save a site's index configuration |
| `GET /api/sn/{site}/{locale}/related-terms?q=…` | Related-concept suggestions for a query |

---

## How it compares

| Mechanism | Works on | When | Gives you |
|---|---|---|---|
| **Thesaurus** (this page) | the **document**, at index time | curated, hierarchical vocabulary | broad→narrow recall + concept-facet browse + related-concept suggestions |
| [Synonyms](/docs/synonyms/) | the **query**, at query time | equivalent words | forgiving matching, no reindex |
| [Embedding search / RAG](/docs/rag/) | vectors | semantic nearness | fuzzy, meaning-based retrieval |

They stack: use a thesaurus for the concepts your domain experts curate, synonyms for the words users type, and embeddings for everything in between.

