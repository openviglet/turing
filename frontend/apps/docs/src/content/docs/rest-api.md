---
title: "REST API Reference"
description: "REST API endpoints for Semantic Navigation search, autocomplete, spell check, and more."
---
Viglet Turing exposes a REST API for integrating search and AI capabilities into any application. All endpoints use **JSON**. Public endpoints (search, chat, autocomplete) require no authentication. Administrative endpoints require a `Key` header.

For authentication details, see [Authentication](/docs/security-authentication/). For the interactive API explorer, visit `http://localhost:2700/swagger-ui.html`.

---

## Authentication

Protected endpoints require an **API Token** in the `Key` request header:

```
Key: <YOUR_API_TOKEN>
```

Create tokens in **Administration → API Tokens**. See [Authentication](/docs/security-authentication/) for step-by-step instructions.

A `POST`, `PUT` or `DELETE` that carries a valid `Key` needs no CSRF token: the key is what authenticates it, so a server-to-server call has no `XSRF-TOKEN` cookie to send back. Requests authenticated by a browser session still need one.

**Example, authenticated API call:**

```bash
curl "http://localhost:2700/api/sn/Sample/search?q=cloud&_setlocale=en_US" \
  -H "Key: <YOUR_API_TOKEN>" \
  -H "Accept: application/json"
```

<a id="partitioned-keys"></a>

### Partitioned keys for a shared site

Several clients can share one SN site, each writing its own documents with a field that tells them apart, for example `site_key`. Bind each client's token to its partition: in **Administration → API Tokens**, set **Partition of a shared site** to the field and the value (`site_key` and `a.example.org`), or send `partitionField` and `partitionValue` to `/api/dev/token`.

The server then confines that key, whatever the request asks for:

| Endpoint | With a partitioned key |
|---|---|
| Search: `GET`/`POST /api/sn/{site}/search`, `/search/query`, `/search/list`, `/search/list/query` | Only the partition's documents are returned, and facets count only them. The similar-documents widget and duplicate clusters are left out, and the search cache is not used. |
| `/search/locales`, `/search/sort-options`, `POST /click` | As for any key |
| `GET /api/sn/{site}/search/similar` | Only the partition's documents are returned, for a seed document in the partition (another seed answers `[]`). The lexical (MLT) path answers, even on a site whose vector mode would otherwise answer. |
| `GET /api/sn/{site}/search/document` | Only the partition's documents are returned, and `404` when none of the ids is in it |
| `GET /api/sn/{site}/ac`, `POST /ac/query` | Suggestions are the titles of the partition's documents that match the prefix, instead of the engine's site-wide term suggester |
| `GET /api/sn/{site}/{locale}/spell-check` | No correction: the dictionary is built from the whole site |
| `GET /api/sn/{site}/{locale}/related-terms` | As for any key: the thesaurus is site configuration |
| `GET /api/sn/{siteId}/metrics/failed-searches`, `/content-gaps`, `/clicks` | Read from the partition's searches and clicks only. The search and click logs record the partition from this release on, so earlier searches are not counted |
| `POST /api/sn/import`, `/import/bulk` | Each document gets the partition value stamped on. A `CREATE` or `DELETE` whose id already belongs to another partition, or to a document stored without one, is refused with `403`, and so is a document that carries a different value. A [delete by field](/docs/import-export/#deleting-documents) gets the partition added, so it stays inside the partition; a delete by type is refused. |
| Every other endpoint, including chat, answer, duplicates, the other metrics, the zip import and MCP | Refused with `403` (`401` on MCP) until it enforces partitions |

A key without a partition, and a browser session, are not affected. The partition field must be a stored field, because the import reads it back from the stored document before it replaces or deletes one.

### Public Endpoints (no authentication required)

Certain endpoints are publicly accessible, allowing client applications to perform searches and chat without managing sessions:

| Endpoint | Purpose |
|---|---|
| `GET /api/sn/*/search` | Semantic Navigation search |
| `POST /api/sn/*/search/query` | Semantic Navigation search, JSON body ([POST-native search](#post-native-search)) |
| `POST /api/sn/*/search/list/query` | Select-list projection, JSON body |
| `GET /api/sn/*/chat` | GenAI chat on an SN Site |
| `GET /api/sn/*/ac` | Autocomplete |
| `POST /api/sn/*/ac/query` | Autocomplete, JSON body |
| `POST /api/sn/*/nl-filters` | A visitor's description as search filters ([Natural-language filters](#natural-language-filters)) |
| `POST /api/genai/chat` | Direct GenAI chat |
| `POST /api/ocr/**` | OCR text extraction |
| `POST /api/v2/integration/**` | External integration endpoints |
| `GET /api/v2/guest/**` | Guest access endpoints |
| `POST /graphql` | GraphQL queries |
| `GET /api/login` | Login endpoint |

All other endpoints require authentication, including the full administration API, user management, site configuration, and AI Agent management.

---

## OpenAPI & Swagger

Explore and test every endpoint interactively:

| Interface | URL |
|---|---|
| **Swagger UI** | `http://localhost:2700/swagger-ui.html` |
| **OpenAPI 3.0 spec** | `http://localhost:2700/v3/api-docs` |

---

## Semantic Navigation API

### Search

The core search endpoint. Returns results, facets, spotlights, spell-check suggestions, and pagination.

```
GET  http://localhost:2700/api/sn/{siteName}/search
POST http://localhost:2700/api/sn/{siteName}/search
```

**Query Parameters (GET):**

| Parameter | Required | Description |
|---|---|---|
| `q` | ✅ | Search query |
| `_setlocale` | ✅ | Locale code (e.g., `en_US`, `pt_BR`) |
| `p` | | Page number (default: `1`) |
| `rows` | | Results per page (overrides site default) |
| `sort` | | Sort field and direction (e.g., `date desc`) |
| `fq[]` | | Filter query: apply a facet filter (e.g., `fq[]=type:news`) |
| `group` | | Group results by a field value |

**Example (GET):**

```bash
curl "http://localhost:2700/api/sn/Sample/search?q=enterprise+search&p=1&_setlocale=en_US&rows=10" \
  -H "Accept: application/json"
```

#### Search Response Structure

The search response is a **self-describing navigational JSON**: every link a user might follow (apply a filter, remove a filter, paginate, filter by metadata) is pre-built and ready to use. The front-end does not need to construct query strings or manage filter state.

```json
{
  "pagination": [
    { "type": "CURRENT", "text": "1", "href": "...", "page": 1 },
    { "type": "NEXT",    "text": "Next", "href": "...", "page": 2 }
  ],
  "queryContext": {
    "count": 42,
    "index": "Sample",
    "limit": 10,
    "offset": 0,
    "page": 1,
    "pageCount": 5,
    "pageEnd": 10,
    "pageStart": 1,
    "responseTime": 23,
    "query": { ... },
    "defaultFields": { ... },
    "facetType": "AND",
    "facetItemType": "AND"
  },
  "results": {
    "document": [
      {
        "source": "TURING",
        "elevate": false,
        "fields": { "title": "...", "url": "...", ... },
        "metadata": [
          { "name": "type", "values": [ { "label": "article", "link": "..." } ] }
        ]
      }
    ]
  },
  "widget": {
    "facet": [ ... ],
    "secondaryFacet": [ ... ],
    "facetToRemove": { ... },
    "similar": [ ... ],
    "spellCheck": { ... },
    "spotlights": [ ... ],
    "locales": [ ... ],
    "cleanUpFacets": "...",
    "selectedFilterQueries": [ ... ]
  },
  "groups": [ ... ]
}
```

**Top-level sections:**

| Section | Description |
|---|---|
| **pagination** | Pre-built page links (`FIRST`, `PREVIOUS`, `CURRENT`, `NEXT`, `LAST`), the front-end follows them directly |
| **queryContext** | Result count, page info, response time, active facet operators, and default field mappings |
| **results.document** | Array of matched documents. Each contains `fields` (all indexed values), `metadata` (facet values with pre-built filter links), `source`, and `elevate` flag |
| **widget.facet** | Primary facet groups with counts and pre-built filter/clear links per value. Enabled by `Facet = true` |
| **widget.secondaryFacet** | Separate facet groups for fields promoted to Secondary Facet, independent from `facet`, for different UI treatment (e.g., tabs) |
| **widget.similar** | More Like This results. Enabled by `MLT = true` |
| **widget.spellCheck** | Spelling correction with `original`, `corrected` text, and `usingCorrectedText` flag. Enabled by `Spell Check = true` |
| **widget.spotlights** | Curated documents injected at configured positions when the query matches a spotlight term |
| **widget.locales** | All configured locales with pre-built links: use as valid values for `_setlocale` |
| **widget.cleanUpFacets** | Pre-built link to clear all active facet filters |
| **groups** | Grouped results when the `group` parameter is used |

**Self-describing navigation** operates at three levels:

1. **Facet navigation**: Each facet value carries pre-built links for applying, removing, or clearing filters
2. **Document metadata navigation**: Each result's metadata values carry filter links (e.g., click a `type:article` tag to filter all articles)
3. **Pagination**: Next/previous/specific page links are included; no offset calculation needed

A Turing search UI can be built as a **pure rendering layer**: render results, facets, tags, pagination, and locale switchers directly from the response. Adding a new facet to the SN Site configuration propagates automatically, with no client code changes.

**Locale selection:**

```
GET /api/sn/{siteName}/search?q=annual+report&_setlocale=pt_BR
```

#### POST Body Parameters

The POST endpoint accepts the same query parameters plus additional fields in the JSON body:

| Field | Type | Description |
|---|---|---|
| `userId` | `string` | User identifier (for metrics and latest searches) |
| `query` | `string` | Search query (alternative to `q` query param) |
| `locale` | `string` | Locale code (alternative to `_setlocale` query param) |
| `page` | `integer` | Page number |
| `rows` | `integer` | Results per page |
| `sort` | `string` | Sort field and direction |
| `group` | `string` | Group results by field |
| `fq` | `string[]` | Filter queries |
| `fqAnd` | `string[]` | AND filter queries |
| `fqOr` | `string[]` | OR filter queries |
| `fqOperator` | `string` | Filter query operator between facets |
| `fqItemOperator` | `string` | Filter query operator within facet values |
| `fieldList` | `string[]` | Restrict which fields are returned |
| `disableAutoComplete` | `boolean` | Disable autocomplete suggestions (default: `false`) |
| `populateMetrics` | `boolean` | Enable search metrics recording (default: `true`) |
| `targetingRules` | `string[]` | Targeting rules: see [Targeting Rules](/docs/semantic-navigation/#targeting-rules) |
| `targetingRulesWithCondition` | `map` | Targeting rules with conditions |
| `targetingRulesWithConditionAND` | `map` | Targeting rules with AND conditions |
| `targetingRulesWithConditionOR` | `map` | Targeting rules with OR conditions |

**Example (POST with targeting rules):**

```bash
curl -X POST "http://localhost:2700/api/sn/Sample/search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "benefits",
    "locale": "en_US",
    "rows": 10,
    "targetingRules": ["department:HR", "department:Finance"]
  }'
```

For the full Targeting Rules reference (rule types, Solr query generation, fallback clause, and practical examples) see [Semantic Navigation → Targeting Rules](/docs/semantic-navigation/#targeting-rules).

---

<a id="post-native-search"></a>

### POST-native search

`POST /api/sn/{siteName}/search` above is the **authenticated** endpoint: it accepts
the impersonation fields (`userId`, `populateMetrics`, the conditional targeting
maps) and returns `401` without a principal.

For public, client-side search there is a second, **anonymous** POST endpoint that
takes a clean read-only JSON body:

```
POST http://localhost:2700/api/sn/{siteName}/search/query
```

Use it when a filter value can contain a character that means something in a query
string: `&`, `=`, `+`, `#`, `%` or a space. A JSON body has no query-string
grammar, so a value like `section:RAG & Chat` cannot be mis-read as query
structure. Nothing is deprecated: `GET /search` and its links stay, and are still
the right choice for deep-linkable, bookmarkable, SEO-crawlable and no-JS search
pages.

| Field | Type | Description |
|---|---|---|
| `q` (alias `query`) | `string` | Search query (default `*`) |
| `locale` | `string` | Locale code, e.g. `en_US` |
| `page` (alias `p`) | `integer` | Page number (default `1`) |
| `rows` | `integer` | Results per page |
| `sort` | `string` | `relevance`, `newest`, `oldest` or a custom sort name |
| `group` | `string` | Group results by field |
| `fq` / `fqAnd` / `fqOr` | `string[]` | Filter queries, each a raw `field:value` |
| `fqOperator` / `fqItemOperator` | `string` | Operators between / within facet groups |
| `fieldList` (alias `fl`) | `string[]` | Restrict which fields are returned |
| `targetingRules` | `string[]` | Targeting rules (no conditional maps, read-only) |
| `autoCorrectionDisabled` | `boolean` | Skip spelling auto-correction (default `true`) |
| `responseFormat` | `string` | `LINKS` (default) or `STRUCTURED` (see below) |
| `includeSortOptions` | `boolean` | `false`. When `true`, a `STRUCTURED` response also carries `sortOptions` |
| `mode` | `string` | `standard` (default) or `instant`. Send `instant` from a search box that updates while the visitor types: the last term of `q` matches as a prefix (`work` finds "Workshop"), and the query is not recorded in the search metrics. A query that already uses syntax (a phrase, `field:value`, a wildcard, `AND`/`OR`/`NOT`) and a last term shorter than 2 characters run as typed. `request.q` echoes what was typed |

The endpoint is anonymous and **CSRF-exempt**, so a browser client needs no
`/csrf` priming round-trip.

```bash
curl -X POST "http://localhost:2700/api/sn/Sample/search/query"   -H "Content-Type: application/json"   -d '{
    "q": "chat",
    "locale": "en_US",
    "fq": ["section:RAG & Chat"],
    "responseFormat": "STRUCTURED"
  }'
```

#### The `STRUCTURED` response

`responseFormat: "LINKS"` (the default) returns the same link-bearing body as the
GET endpoint, so a client can move to POST without changing how it reads the
response.

`responseFormat: "STRUCTURED"` returns a **link-free** body instead. The contract
is: *take the echoed `request`, apply one structured change, send it back.*

```json
{
  "request": { "q": "chat", "page": 1, "locale": "en_US", "fq": ["section:RAG & Chat"] },
  "icon": "tabler:building-store",
  "queryContext": { "count": 42, "facetItemType": "OR" },
  "results": {
    "document": [
      {
        "elevate": false,
        "fields": { "id": "doc-1", "title": "Grounding a chat on your own content" },
        "metadata": [
          {
            "field": "author",
            "value": "Ada",
            "filterQuery": "author:Ada",
            "text": "Ada",
            "selected": false,
            "action": "ADD"
          }
        ]
      }
    ]
  },
  "pagination": [
    { "type": "CURRENT", "text": "1", "page": 1, "current": true },
    { "type": "NEXT",    "text": "Next", "page": 2, "current": false }
  ],
  "facets": [
    {
      "name": "section",
      "label": "Section",
      "multiValued": true,
      "clearFilterQueries": ["section:RAG & Chat"],
      "items": [
        {
          "field": "section",
          "value": "RAG & Chat",
          "filterQuery": "section:RAG & Chat",
          "label": "RAG & Chat",
          "count": 12,
          "selected": true,
          "action": "REMOVE"
        }
      ]
    }
  ],
  "selectedFilterQueries": ["section:RAG & Chat"],
  "spellCheck": { "correctedText": false, "usingCorrectedText": false },
  "locales": [{ "locale": "en_US", "selected": true }],
  "similar": [{ "id": "doc-2", "title": "Tuning your retrieval", "url": "/tuning" }],
  "spotlights": [{ "id": "sp-1", "position": 1, "title": "Start here", "link": "/start" }]
}
```

A few fields carry state rather than navigation, and are present only when the
site or the request produced them:

| Field | When it appears |
|---|---|
| `icon` | the site configured an icon. A plain [Iconify](https://icon-sets.iconify.design/) name (`tabler:building-store`). Render it as the search header's brand mark, falling back to your own logo when absent |
| `similar` | the request named a seed document: the "more like this" neighbours, each `{ id, title, type, url }` |
| `spotlights` | the site pinned documents for this query: each `{ id, position, title, type, referenceId, content, link }`, `position` being the 1-based slot to inject it at |
| `sortOptions` | the request asked for `includeSortOptions` (see below) |

`url` and `link` above are the **documents' own destinations**, the same kind of
value `results[].fields.url` carries, not search actions. Every *navigation*
that used to be a URL is a value to put on the next request:

| Instead of following… | Do this |
|---|---|
| a facet item's `link` | apply `item.action` (`ADD` / `REMOVE` / `REPLACE`) to `request.fq` using `item.filterQuery` |
| a document's `metadata[].href` | apply the chip's own `action` to `request.fq` using its `filterQuery`, exactly as for a facet item, because it is the same action (chips are `{field, value, filterQuery, text, selected, action}` here) |
| a facet's `cleanUpLink` | remove `facet.clearFilterQueries` from `request.fq` |
| the widget's `cleanUpFacets` | clear `request.fq` (see `selectedFilterQueries`) |
| a pagination `href` | set `request.page = page.page` |
| a locale `link` | set `request.locale` |
| a spell-check `link` | set `request.q = spellCheck.correctedTextValue` |

An applied facet stays in `facets` whatever the site's facet item type. Its
selected value has `selected: true` and the facet's `clearFilterQueries` names
what to remove. The `LINKS` response keeps the older behaviour, where a site
whose item type is not `OR` drops an applied facet and lists it under
`widget.facetToRemove`.

Both JavaScript SDKs do this for you: see
[`@viglet/turing-sdk`](/docs/javascript-sdk/) (`toggleFacet`, `clearFacet`,
`clearAllFilters`, `goToPage`, `applySpellCheck`) and
[`@viglet/turing-react-sdk`](/docs/react-sdk/).

#### Sort options in one round-trip

The sort option list has its own endpoint
(`GET /api/sn/{siteName}/search/sort-options`) and is **not** part of the
response by default: the control is already rendered on every request after the
first. To paint it on the *first* frame without a second call, ask for it:

```json
{ "q": "chat", "responseFormat": "STRUCTURED", "includeSortOptions": true }
```

The response then carries a `sortOptions` array of `{ "value", "label" }`,
exactly what the dedicated endpoint serves. Put the chosen `value` on
`request.sort`.

The flag is **not** echoed back on `request`, so re-sending the echoed object
asks for results without re-fetching the list. It has no effect on a `LINKS`
response.

#### `responseFormat` on GET

`responseFormat` is a plain request field, not an `Accept` header, so the GET
search endpoint honours it too, handy for comparing the two shapes against one
query, or for a server-rendered page that wants facet *data* without parsing
links:

```
GET http://localhost:2700/api/sn/{siteName}/search?q=chat&responseFormat=STRUCTURED
```

The value is case-insensitive (`structured` works). Omitting it returns the
regular link-bearing response, unchanged; an unrecognised value returns
`400 Bad Request` rather than silently falling back.

#### Autocomplete and select-list over POST

The same body works on the other two read endpoints, so a whole search surface can
be POST-only:

```
POST http://localhost:2700/api/sn/{siteName}/ac/query
POST http://localhost:2700/api/sn/{siteName}/search/list/query
```

Both return exactly what their GET counterparts return (a list of suggestions and
a select-list projection). Neither carries links, so `responseFormat` does not
apply to them.

```bash
curl -X POST "http://localhost:2700/api/sn/Sample/ac/query"   -H "Content-Type: application/json"   -d '{ "q": "enter", "locale": "en_US", "rows": 10 }'
```

---

### Auto Complete

Returns suggestions for the given prefix. Ideal for search-as-you-type UIs.

```
GET http://localhost:2700/api/sn/{siteName}/ac
```

| Parameter | Required | Description |
|---|---|---|
| `q` | ✅ | Prefix to complete |
| `_setlocale` | ✅ | Locale |
| `rows` | | Maximum number of suggestions |

**Example:**

```bash
curl "http://localhost:2700/api/sn/Sample/ac?q=enter&_setlocale=en_US"
```

**Response:**

```json
["enterprise", "enterprise search", "enterprise AI", "entries"]
```

A `POST /api/sn/{siteName}/ac/query` variant takes the same JSON body as
[POST-native search](#post-native-search): use it when a filter narrowing the
suggestions contains a query-string character.

---

### Latest Searches

Returns the most recent search terms for a given user: useful for personalised search history UIs.

```
POST http://localhost:2700/api/sn/{siteName}/search/latest
```

| Parameter | Location | Description |
|---|---|---|
| `q` | Query | Current query |
| `rows` | Query | Max results (default: `5`) |
| `_setlocale` | Query | Locale |
| `userId` | Body (JSON) | User identifier |

**Example:**

```bash
curl -X POST "http://localhost:2700/api/sn/Sample/search/latest?q=cloud&rows=5&_setlocale=en_US" \
  -H "Key: <API_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{ "userId": "user123" }'
```

**Response:**

```json
["cloud computing", "cloud storage", "cloud security"]
```

---

### Search Locales

Lists all configured locales for a Semantic Navigation Site.

```
GET http://localhost:2700/api/sn/{siteName}/search/locales
```

**Response:**

```json
[
  { "locale": "en_US", "link": "/api/sn/Sample/search?_setlocale=en_US" },
  { "locale": "pt_BR", "link": "/api/sn/Sample/search?_setlocale=pt_BR" }
]
```

---

### Spell Check

Corrects a query against the site's indexed vocabulary.

```
GET http://localhost:2700/api/sn/{siteName}/{locale}/spell-check
```

| Parameter | Required | Description |
|---|---|---|
| `q` | ✅ | Text to check |

**Example:**

```bash
curl "http://localhost:2700/api/sn/Sample/en_US/spell-check?q=entirprise"
```

---

<a id="natural-language-filters"></a>

### Natural-language filters

Turns a visitor's description into the `q` and `fq` of the site's own search, so a page can show the ranked, faceted results for "part-time job near the subway in Sao Paulo" instead of a chat answer.

```
POST http://localhost:2700/api/sn/{siteName}/nl-filters
```

| Body field | Required | Description |
|---|---|---|
| `text` | ✅ | The visitor's request |
| `locale` | | Search locale; when left out, the site's first locale |

**Example:**

```bash
curl -X POST "http://localhost:2700/api/sn/jobs/nl-filters" \
  -H "Content-Type: application/json" \
  -d '{"text": "meio periodo em sao paulo, sem experiencia", "locale": "pt_BR"}'
```

```json
{
  "available": true,
  "text": "meio periodo em sao paulo, sem experiencia",
  "q": "sem experiencia",
  "filters": [
    { "field": "city", "values": ["São Paulo"], "rangeStart": null, "rangeEnd": null,
      "sourceWords": ["sao paulo"], "confidence": 1.0, "grounded": true },
    { "field": "workload", "values": ["Part-time"], "rangeStart": null, "rangeEnd": null,
      "sourceWords": [], "confidence": 0.6, "grounded": true }
  ],
  "fq": ["city:São Paulo", "workload:Part-time"],
  "dropped": []
}
```

Pass `q` and `fq` to the search ([`POST /api/sn/{site}/search/query`](#post-native-search)) as they are. Turing checks every value against the values the site actually holds and spells it the way the site indexes it (`sao paulo` becomes `São Paulo`). A value the site does not hold is left out of `fq` and listed in `dropped` with the reason (`NOT_IN_SITE`, or `UNKNOWN_FIELD` for a field the site does not have).

Relative dates are resolved against the server's date, in English and Portuguese: "last year", "this month", "last month", "the last 7 days", "since March", "since 2024" ("ano passado", "neste mês", "desde março", ...). "Last year" asked on 2026-10-24 becomes `[2025-01-01 TO 2025-12-31]`. A date range the text neither phrases that way nor dates with a year is a guess, so it is left out of `fq` and listed in `dropped` as `UNGROUNDED_DATE`.

| Response field | Meaning |
|---|---|
| `q` | What is left to match as free text |
| `fq` | `field:value` per value and `field:[start TO end]` per range, the form the search takes |
| `filters[].sourceWords` | The words of the request each value was read from |
| `filters[].confidence` | `1` when the request spells the value, `0.6` when it was inferred (a synonym, a translation), `0.3` when the engine could not list the field's values to check it |
| `available` | `false` when no LLM is configured: nothing is filtered and `q` is the whole request |

The call spends one LLM completion, so it is gated like the site chat: a site in API-key mode requires its key, and anonymous calls share the chat's rate limit. From the SDKs, call `postNlFilters` ([JavaScript SDK](/docs/javascript-sdk/#low-level-api-functions)).

The site chat and the catalog copilot read list-style questions ("quais vagas em São Paulo?") through the same extraction, so their answers are narrowed by the same checked values. To receive those filters in the chat stream, send `"includeFilters": true` in the body of `POST /api/sn/{siteName}/chat/conversation`: a list-style question then streams one `filters` event, carrying the same JSON as above, before the answer. It is off by default, because a client that predates the event would show it as answer text.

---

## GenAI API

:::note[Streaming responses]
Both chat endpoints return **Server-Sent Events (SSE)** streams: content is delivered progressively as the model generates it. Clients should consume the response as an event stream rather than waiting for a single JSON payload.
:::

### RAG Chat (SN Site)

Sends a question to the GenAI engine of a Semantic Navigation Site. Returns a streaming SSE response grounded in the site's indexed content.

```
GET http://localhost:2700/api/sn/{siteName}/chat?q={question}
```

**Example:**

```bash
curl "http://localhost:2700/api/sn/Sample/chat?q=What+are+the+main+features?" \
  -H "Key: <YOUR_API_TOKEN>"
```

### AI Agent Chat

Sends a message to a specific AI Agent. Returns a streaming SSE response.

```
POST http://localhost:2700/api/v2/llm/agent/{agentId}/chat
```

**Example:**

```bash
curl -X POST "http://localhost:2700/api/v2/llm/agent/my-agent/chat" \
  -H "Key: <YOUR_API_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{ "message": "Summarise the latest quarterly report" }'
```

See [Chat](/docs/chat/) for the full chat interface documentation.

<a id="ai-capabilities"></a>

### AI capabilities of a site

Which AI features a site can use, read from its configuration. No model is called, so a page can ask before it shows an AI block. It needs no login; a site in `API_KEY` mode needs its key, as for search.

```
GET http://localhost:2700/api/sn/{siteName}/ai/capabilities
```

```json
{
  "site": "Sample",
  "chat":            { "enabled": true,  "reason": "NONE" },
  "answer":          { "enabled": true,  "reason": "NONE" },
  "nlFilters":       { "enabled": true,  "reason": "NONE" },
  "copilot":         { "enabled": true,  "reason": "NONE" },
  "similarByVector": { "enabled": false, "reason": "VECTORLESS" },
  "summaries":       { "enabled": true,  "reason": "NONE" }
}
```

`chat` and `answer` always agree, and carry the same `reason` as `GET /api/sn/{siteName}/chat/enabled` (`NO_GENAI`, `NO_AGENT`, `RAG_DISABLED`, `MISSING_EMBEDDING`, ...). The features that only need a default LLM report `MISSING_DEFAULT_LLM` when none is set.

`similarByVector` is the one answer that looks past configuration: when the site is set up for RAG but its vector store holds none of its content, it reports `NOT_EMBEDDED`. That happens when the content was indexed before generative AI was turned on; **reindex the site** to fix it. Chat stays enabled meanwhile, but it will not find the content. The check lists one stored chunk per locale, never calls a model, and is cached for a minute; a store that cannot list chunks reports the site as ready. From [`@viglet/turing-sdk`](/docs/javascript-sdk/): `fetchAiCapabilities(client, site)`.

---

## Token Usage API

Returns token consumption statistics for a given month.

```
GET http://localhost:2700/api/v2/llm/token-usage?month=YYYY-MM
```

**Example:**

```bash
curl "http://localhost:2700/api/v2/llm/token-usage?month=2025-01" \
  -H "Key: <YOUR_API_TOKEN>"
```

Authentication required. See [Token Usage](/docs/token-usage/) for details.

---

## Search Insights API

What visitors searched for and did not find. Both endpoints take the site's **id** and the same window, and need authentication (a `Key` header or a session).

```
GET /api/sn/{siteId}/metrics/failed-searches?from=&to=&period=&rows=
GET /api/sn/{siteId}/metrics/content-gaps?from=&to=&period=&rows=
```

| Parameter | Meaning |
|---|---|
| `from`, `to` | ISO-8601 instants. `to` defaults to now; without `from` the report covers all time |
| `period` | Shorthand for `from`: `today`, `this-week`, `this-month` or `all-time` |
| `rows` | How many terms, 1 to 50 (default 10) |

`failed-searches` lists the queries that returned nothing every time they ran. `content-gaps` lists the queries searched often whose average result count is under 3: what to write next. Both answer `{ from, to, terms: [{ term, searches, avgResults, lastSeen }], metricsEnabled }`, most searched first; terms searched equally often come back most recently searched first, so a new gap is not cut off by older ones. These are the same reports the MCP tools `top_failed_searches` and `content_gaps` read.

`metricsEnabled` is `false` when the server does not record searches (`turing.search.metrics.enabled` is off). An empty report then says nothing about the site's traffic, so show the user that metrics are off rather than an empty list.

```bash
curl "http://localhost:2700/api/sn/<SITE_ID>/metrics/failed-searches?period=this-month&rows=20" \
  -H "Key: <YOUR_API_TOKEN>"
```

From [`@viglet/turing-sdk`](/docs/javascript-sdk/): `fetchFailedSearches(client, siteId, { period: "this-month" })` and `fetchContentGaps(...)`, with a client created with an `apiKey`.

<a id="click-through-rates"></a>

### Click-through rates

The clicks your search page reports to `POST /api/sn/{site}/click` are read back here. The window and authentication are the same as above, plus an optional `locale` (`pt_BR` or `pt-BR`) that narrows both the searches and the clicks to one locale.

```
GET /api/sn/{siteId}/metrics/clicks?from=&to=&period=&locale=&rows=
```

```json
{
  "from": "2026-09-26T00:00:00Z", "to": "2026-10-26T00:00:00Z", "locale": null,
  "searches": 1240, "clicks": 410, "ctr": 0.3306,
  "terms": [
    { "term": "workshop", "searches": 96, "clicks": 41, "ctr": 0.4271, "avgPosition": 1.8, "avgResults": 12.0 }
  ],
  "neverClicked": [
    { "term": "pricing", "searches": 30, "clicks": 0, "ctr": 0.0, "avgPosition": null, "avgResults": 4.5 }
  ],
  "metricsEnabled": true
}
```

`ctr` is clicks per search, so it can pass 1 when visitors open several results. `terms` are the most searched terms first. `neverClicked` lists terms that found results nobody opened: a relevance problem, unlike `failed-searches`, which is a content problem. The console's **Top search terms** page shows the same numbers as a click-through column.

Clicks are recorded only when `turing.search.metrics.enabled` is on (env `TURING_SEARCH_METRICS_ENABLED=true`). When it is off, `POST /click` drops the click and answers `204` with the header `X-Turing-Metrics: disabled`, so a client can tell why nothing is counted. When it is on, the answer is `202`. The reports say the same in their body: `metricsEnabled` on `clicks`, `failed-searches` and `content-gaps`.

From the vanilla SDK: `fetchClickThrough(client, siteId, { period: "this-month", locale: "pt-BR" })`.

---

## Integration API

The Integration API provides a **reverse-proxy** endpoint that forwards requests to a configured external integration instance (e.g., an AEM connector or Web Crawler). All HTTP methods are supported. The proxy validates the target host and path to prevent SSRF attacks.

### Proxy Endpoint

```
GET|POST|PUT|DELETE  http://localhost:2700/api/v2/integration/{integrationId}/**
```

The `{integrationId}` maps to an Integration Instance configured in Administration. The remainder of the path (`**`) is forwarded to the instance's endpoint URL.

**Example:**

```bash
curl -X POST "http://localhost:2700/api/v2/integration/my-aem-instance/index" \
  -H "Content-Type: application/json" \
  -d '{ "url": "https://example.com/content/page.html" }'
```

This endpoint is **public** (no authentication required).

### Integration Instance CRUD (Admin)

Manage integration instances. Authentication required.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/integration` | List all integration instances |
| `GET` | `/api/integration/{id}` | Get an integration instance |
| `POST` | `/api/integration` | Create an integration instance |
| `PUT` | `/api/integration/{id}` | Update an integration instance |
| `DELETE` | `/api/integration/{id}` | Delete an integration instance |
| `GET` | `/api/integration/vendor` | List available integration vendors |

---

## Assets API

Manage files and the RAG Knowledge Base. All endpoints require authentication.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/asset?prefix=` | List files and folders in a prefix (folder) |
| `POST` | `/api/asset` | Upload one or more files (`multipart/form-data`) |
| `GET` | `/api/asset/download?objectName=` | Download a file (`Content-Disposition: attachment`) |
| `GET` | `/api/asset/preview?objectName=` | Preview a file inline (`Content-Disposition: inline`) |
| `GET` | `/api/asset/metadata?objectName=` | Retrieve file metadata (size, type, date) |
| `POST` | `/api/asset/folder?path=` | Create a new folder |
| `DELETE` | `/api/asset?objectName=` | Delete a file or folder |
| `POST` | `/api/asset/train` | Start batch AI Training |
| `GET` | `/api/asset/train/status` | Poll current training status |
| `GET` | `/api/asset/train/records?objectNames=` | Get training timestamps for specific files |

After upload, an asynchronous event triggers AI indexing for each file. When a file is deleted, its embeddings are automatically removed from the vector store.

For full details, see [Assets](/docs/assets/).

---

## AI Agent API

### Agent Management

Manage AI Agents. Authentication required.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/ai-agent` | List all agents (ordered by title) |
| `GET` | `/api/ai-agent/structure` | Get empty structure template for a new agent |
| `GET` | `/api/ai-agent/{id}` | Get a specific agent |
| `POST` | `/api/ai-agent` | Create a new agent |
| `PUT` | `/api/ai-agent/{id}` | Update an existing agent |
| `DELETE` | `/api/ai-agent/{id}` | Delete an agent |

### Agent Chat

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v2/ai-agent/{agentId}/chat` | Stream chat response (SSE). Request body: `{ llmInstanceId, messages[] }` |
| `GET` | `/api/v2/ai-agent/{agentId}/chat/context-info` | Get LLM context window size. Query param: `llmInstanceId` |

### Native Tools

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/native-tool` | List all available tool groups with tool names and descriptions |

For full details, see [AI Agents](/docs/ai-agents/).

---

## Administration API

The following endpoints manage SN Sites, fields, and spotlights. All require authentication via the `Key` header. For full details, explore the Swagger UI at `http://localhost:2700/swagger-ui.html`.

### SN Site Management

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/sn` | List all SN Sites |
| `GET` | `/api/sn/{id}` | Get an SN Site |
| `POST` | `/api/sn` | Create an SN Site |
| `PUT` | `/api/sn/{id}` | Update an SN Site |
| `DELETE` | `/api/sn/{id}` | Delete an SN Site (removes index cores) |
| `GET` | `/api/sn/{id}/monitoring` | Site monitoring status (document count, queue size) |
| `GET` | `/api/sn/{id}/export` | Export an SN Site configuration |
| `GET` | `/api/sn/export` | Export all SN Site configurations |

**Example: list sites:**

```bash
curl "http://localhost:2700/api/sn" \
  -H "Key: <YOUR_API_TOKEN>"
```

### Field Management

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/sn/{snSiteId}/field` | List fields for a site |
| `GET` | `/api/sn/{snSiteId}/field/{id}` | Get a field |
| `POST` | `/api/sn/{snSiteId}/field` | Create a field |
| `PUT` | `/api/sn/{snSiteId}/field/{id}` | Update a field |
| `DELETE` | `/api/sn/{snSiteId}/field/{id}` | Delete a field |

### Spotlight Management

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/sn/{snSiteId}/spotlight` | List spotlights for a site |
| `GET` | `/api/sn/{snSiteId}/spotlight/{id}` | Get a spotlight |
| `POST` | `/api/sn/{snSiteId}/spotlight` | Create a spotlight |
| `PUT` | `/api/sn/{snSiteId}/spotlight/{id}` | Update a spotlight |
| `DELETE` | `/api/sn/{snSiteId}/spotlight/{id}` | Delete a spotlight |

### Intent Management

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/intent` | List all intents |
| `GET` | `/api/intent/enabled` | List enabled intents (sorted by display order) |
| `GET` | `/api/intent/{id}` | Get an intent with its actions |
| `POST` | `/api/intent` | Create an intent |
| `PUT` | `/api/intent/{id}` | Update an intent |
| `DELETE` | `/api/intent/{id}` | Delete an intent |

For full details, see [Intent](/docs/intent/).

### Import / Export

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/import` | Import SN Site configurations from a ZIP file |
| `POST` | `/api/sn/import` | Import content (JSON job items) |
| `POST` | `/api/sn/import/zip` | Import content from a ZIP with file attachments |

For full details, see [Import & Export](/docs/import-export/).

### Logging

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/logging` | Server logs (paginated, filterable) |
| `GET` | `/api/logging/indexing` | Indexing pipeline logs |
| `GET` | `/api/logging/aem` | AEM connector logs |

For full details, see [Logging](/docs/logging/).

---

## GraphQL API

Turing exposes a GraphQL endpoint that provides the same Semantic Navigation search capabilities as the REST API using GraphQL queries. The endpoint is **public** (no authentication required).

```
POST http://localhost:2700/graphql
```

| Query | Description |
|---|---|
| `siteNames` | Returns all configured SN Site names as a string array |
| `siteSearch(siteName, searchParams, locale)` | Performs a search against an SN Site |

An interactive GraphQL IDE is available at `/graphiql` with schema introspection, auto-completion, and query validation.

**Example:**

```bash
curl -X POST "http://localhost:2700/graphql" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query { siteSearch(siteName: SAMPLE, searchParams: { q: \"enterprise search\", rows: 10 }, locale: \"en_US\") { queryContext { count } results { document { fields { title url } } } } }"
  }'
```

For the full schema reference, query examples, dynamic fields, filter operators, and response types, see **[GraphQL API](/docs/graphql/)**.

---

## OCR API

Extracts text from documents via OCR. Two endpoints are available, one for file uploads and one for URLs. Both are **public** (no authentication required).

### Extract Text from File

```
POST http://localhost:2700/api/ocr/file
```

Accepts a `multipart/form-data` request with a `file` parameter.

**Example:**

```bash
curl -X POST "http://localhost:2700/api/ocr/file" \
  -F "file=@/path/to/document.pdf"
```

### Extract Text from URL

```
POST http://localhost:2700/api/ocr/url
```

Accepts a JSON body with a `url` field pointing to a remote document.

**Example:**

```bash
curl -X POST "http://localhost:2700/api/ocr/url" \
  -H "Content-Type: application/json" \
  -d '{ "url": "https://example.com/report.pdf" }'
```

Both endpoints return a JSON object containing the extracted text and file metadata.

---

## Related Pages

| Page | Description |
|---|---|
| [Authentication](/docs/security-authentication/) | How to create and use API Tokens |
| [Semantic Navigation](/docs/semantic-navigation/) | SN Site configuration and the search response structure |
| [Chat](/docs/chat/) | Front-end chat interface and GenAI API |
| [AI Agents](/docs/ai-agents/) | Agent composition, tool selection, and MCP Servers |
| [Assets](/docs/assets/) | File management and RAG Knowledge Base |
| [Intent](/docs/intent/) | Chat prompt suggestions API |
| [Import & Export](/docs/import-export/) | Site configuration and content import |
| [Logging](/docs/logging/) | Application and indexing log API |
| [Token Usage](/docs/token-usage/) | Token consumption reporting |

---


