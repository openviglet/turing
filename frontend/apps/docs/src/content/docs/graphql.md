---
title: "GraphQL API"
description: "Query Semantic Navigation sites using the Viglet Turing GraphQL endpoint: schema, queries, dynamic fields, and examples."
---
Viglet Turing exposes a GraphQL endpoint that provides the same Semantic Navigation search capabilities as the REST API but using GraphQL queries. The endpoint is **public**, no authentication required.

```
POST http://localhost:2700/graphql
```

---

## GraphiQL Explorer

An interactive GraphQL IDE is available in the admin console and at `/graphiql`. It provides:

- **Schema introspection**: browse all types, queries, and fields
- **Auto-completion**: press Ctrl+Space for field suggestions
- **Query validation**: syntax and type errors are highlighted in real time
- **Query history**: previously executed queries are saved locally

The explorer synchronizes its theme (light/dark) with the Turing console.

---

## Queries

### siteNames

Returns all configured Semantic Navigation site names as a string array, sorted alphabetically.

```graphql
query {
  siteNames
}
```

**Response:**

```json
{
  "data": {
    "siteNames": ["Customer Portal", "Knowledge Base", "Sample"]
  }
}
```

### siteSearch

Performs a search against a Semantic Navigation site. This is the primary query: it mirrors the full capability of the REST search endpoint.

```graphql
query {
  siteSearch(
    siteName: TurSNSiteName!
    searchParams: SearchParamsInput!
    locale: String!
  ): SiteSearchResult
}
```

| Argument | Type | Description |
|---|---|---|
| `siteName` | `TurSNSiteName!` | Site name as a dynamic enum value (see [Site Name Enum](#site-name-enum)) |
| `searchParams` | `SearchParamsInput!` | Search parameters (see [SearchParamsInput](#searchparamsinput)) |
| `locale` | `String!` | Locale code (e.g., `en_US`, `pt_BR`). Falls back to `en` if not supported by the site. |

---

## Site Name Enum

Site names are exposed as a **dynamic GraphQL enum** (`TurSNSiteName`) that is generated at runtime from the configured sites in the database.

**Conversion rules:**

| Site Name | Enum Value | Rule Applied |
|---|---|---|
| `Sample` | `SAMPLE` | Uppercase |
| `My Sample Site` | `MY_SAMPLE_SITE` | Spaces → underscores |
| `Site 2.0!` | `SITE_2_0_` | Special characters → underscores |
| `123 Numbers` | `SITE_123_NUMBERS` | Numeric prefix → `SITE_` prepended |
| `__internal` | `SITE___INTERNAL` | Double underscore prefix → `SITE_` prepended |

**Duplicate handling:** If two sites produce the same enum value, the second one gets a `_2` suffix, the third `_3`, and so on.

The enum always includes `UNKNOWN` as the first value. Use the `siteNames` query to discover available site names, then convert to the enum format for `siteSearch`.

---

## SearchParamsInput

| Field | Type | Default | Description |
|---|---|---|---|
| `q` | `String` | `*` | Search query text |
| `rows` | `Int` | Site default | Results per page (use `-1` for site default) |
| `p` | `Int` | `1` | Page number |
| `sort` | `String` | `relevance` | Sort field and direction (e.g., `relevance`, `date desc`) |
| `group` | `String` | n/a | Group results by a field value |
| `nfpr` | `Int` | `1` | Number of facets per row in grouped results |
| `fq` | `[String]` | n/a | Filter queries (e.g., `["type:article"]`) |
| `fqAnd` | `[String]` | n/a | AND filter queries |
| `fqOr` | `[String]` | n/a | OR filter queries |
| `fqOp` | `String` | `NONE` | Filter operator between facets: `AND`, `OR`, or `NONE` |
| `fqiOp` | `String` | `NONE` | Filter operator within facet values: `AND`, `OR`, or `NONE` |
| `locale` | `String` | n/a | Override locale (takes precedence over the top-level `locale` argument) |
| `fl` | `[String]` | n/a | Field list: restrict which document fields are returned |

### Filter Operators

The `fqOp` and `fqiOp` parameters control how multiple filter queries are combined:

| Value | Behavior | Example |
|---|---|---|
| `NONE` | No filter logic applied | Default: filters are passed as-is |
| `AND` | All filters must match | `fq: ["type:article", "dept:HR"]` with `fqOp: "AND"` → documents must be type:article AND dept:HR |
| `OR` | Any filter may match | `fqOr: ["type:article", "type:news"]` with `fqOp: "OR"` → documents matching either type |

Invalid operator values fall back to `NONE`.

---

## Response Schema

### SiteSearchResult

The top-level response object:

| Field | Type | Description |
|---|---|---|
| `pagination` | `[SearchPagination]` | Pre-built page links |
| `queryContext` | `SearchQueryContext` | Result metadata and statistics |
| `results` | `SearchResults` | Document list with facets |
| `groups` | `[SearchGroup]` | Grouped results (when `group` parameter is used) |
| `widget` | `SearchWidget` | Widget HTML content |

### SearchQueryContext

| Field | Type | Description |
|---|---|---|
| `count` | `Int` | Total number of matching documents |
| `index` | `String` | Site name |
| `limit` | `Int` | Results per page |
| `offset` | `Int` | Result offset |
| `page` | `Int` | Current page number |
| `pageCount` | `Int` | Total number of pages |
| `pageEnd` | `Int` | Last result index on this page |
| `pageStart` | `Int` | First result index on this page |
| `responseTime` | `Int` | Query execution time in milliseconds |
| `query` | `SearchQuery` | Original query parameters (`q`, `sort`, `p`, `rows`, `group`, `nfpr`) |
| `defaultFields` | `SearchDefaultFields` | Default field mappings (`title`, `text`, `url`, `date`, `description`, `image`) |
| `facetType` | `String` | Facet operator type |
| `facetItemType` | `String` | Facet item operator type |

### SearchResults

| Field | Type | Description |
|---|---|---|
| `numFound` | `Int` | Total matching documents |
| `start` | `Int` | Result offset |
| `document` | `[SearchDocument]` | Array of matching documents |
| `facet` | `[SearchFacet]` | Facet groups with counts |

### SearchDocument

| Field | Type | Description |
|---|---|---|
| `source` | `String` | Source system (e.g., `TURING`) |
| `fields` | `SearchDocumentFields` | Document field values (see [Document Fields](#document-fields)) |
| `metadata` | `[SearchDocumentMetadata]` | Key-value metadata pairs with `name` and `value` |

### SearchFacet

| Field | Type | Description |
|---|---|---|
| `name` | `String` | Facet field name |
| `label` | `[SearchFacetLabel]` | Localized labels (`lang`, `text`) |
| `facets` | `[SearchFacetItem]` | Individual facet values |

### SearchFacetItem

| Field | Type | Description |
|---|---|---|
| `label` | `String` | Facet value label |
| `count` | `Int` | Number of documents with this value |
| `link` | `String` | Pre-built filter link |

### SearchPagination

| Field | Type | Description |
|---|---|---|
| `text` | `String` | Display text (e.g., "1", "Next") |
| `href` | `String` | Pre-built page link |
| `page` | `Int` | Page number |
| `current` | `Boolean` | Whether this is the current page |

### SearchGroup

| Field | Type | Description |
|---|---|---|
| `name` | `String` | Group value (e.g., category name) |
| `count` | `Int` | Number of documents in this group |
| `page` | `Int` | Current page within the group |
| `pageCount` | `Int` | Total pages in the group |
| `pageEnd` | `Int` | Last result index |
| `pageStart` | `Int` | First result index |
| `limit` | `Int` | Results per page |
| `results` | `SearchResults` | Documents within this group |
| `pagination` | `[SearchPagination]` | Pagination for this group |

### SearchWidget

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Widget identifier |
| `name` | `String` | Widget name |
| `description` | `String` | Widget description |
| `html` | `String` | Rendered HTML content |

---

## Document Fields

### Static Fields

Every document exposes these standard fields in `SearchDocumentFields`:

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Document unique identifier |
| `title` | `String` | Document title |
| `text` | `String` | Full text content |
| `url` | `String` | Document URL |
| `date` | `String` | Publication or modification date |
| `description` | `String` | Short description or summary |
| `image` | `String` | Image URL |

### Dynamic Fields

The GraphQL schema is **dynamically extended** at runtime with custom fields from your SN Site configuration. If your site defines fields like `category`, `author`, or `department`, they appear as additional fields on `SearchDocumentFields` with the `JSON` scalar type.

**How it works:**

1. At startup, Turing reads all field extensions from every configured SN Site
2. Fields with valid GraphQL names (matching `^[_A-Za-z]\w*$`, no `__` prefix) are collected
3. Static fields (`id`, `title`, `text`, `url`, `date`, `description`, `image`) are excluded to avoid conflicts
4. The schema is extended: `extend type SearchDocumentFields { category: JSON, author: JSON, ... }`

Dynamic fields use the `JSON` scalar type, which can hold strings, numbers, arrays, or nested objects, matching the flexible nature of indexed document attributes.

:::tip[Field list projection]
Use the `fl` parameter in `SearchParamsInput` to restrict which fields are returned. This reduces response size and can improve performance when you only need specific fields.

```graphql
searchParams: { q: "report", fl: ["title", "url", "category"] }
```
:::

---

## Locale Resolution

The locale for a search is resolved with the following precedence:

1. **`searchParams.locale`**: if provided in the input, takes highest priority
2. **Top-level `locale` argument**: the required `locale` parameter on `siteSearch`
3. **Default**: falls back to `en` if neither is provided or the locale is not supported by the site

If the resolved locale is not configured on the target SN Site, the search falls back to the site's first available locale.

---

## Examples

### Basic Search

```graphql
query BasicSearch {
  siteSearch(
    siteName: SAMPLE
    searchParams: {
      q: "technology"
      rows: 10
      p: 1
      sort: "relevance"
    }
    locale: "en"
  ) {
    queryContext {
      count
      page
      pageCount
      responseTime
    }
    results {
      numFound
      start
      document {
        fields { title, text, url, date }
        source
      }
    }
    pagination { text, href, page, current }
  }
}
```

### Advanced Search with Filters and Facets

```graphql
query AdvancedSearch {
  siteSearch(
    siteName: SAMPLE
    searchParams: {
      q: "annual report"
      rows: 10
      p: 1
      fq: ["type:article", "department:Finance"]
      fqOp: "AND"
      fl: ["title", "url", "date"]
    }
    locale: "en_US"
  ) {
    queryContext { count, responseTime }
    results {
      numFound
      document {
        fields { title, url, date }
        metadata { name, value }
      }
      facet {
        name
        label { lang, text }
        facets { label, count, link }
      }
    }
  }
}
```

### Grouped Search

```graphql
query GroupedSearch {
  siteSearch(
    siteName: SAMPLE
    searchParams: {
      q: "*"
      group: "category"
      rows: 5
    }
    locale: "en_US"
  ) {
    groups {
      name
      count
      page
      pageCount
      results {
        document {
          fields { title, url }
        }
      }
      pagination { text, page, current }
    }
  }
}
```

### Search with OR Filters

```graphql
query OrFilterSearch {
  siteSearch(
    siteName: SAMPLE
    searchParams: {
      q: "benefits"
      fqOr: ["type:article", "type:news"]
      fqOp: "OR"
    }
    locale: "en_US"
  ) {
    queryContext { count }
    results {
      document {
        fields { title, url }
      }
    }
  }
}
```

### Minimal Search

```graphql
query SimpleSearch {
  siteSearch(
    siteName: SAMPLE
    searchParams: { q: "search" }
    locale: "en"
  ) {
    results {
      document {
        fields { title, url }
      }
    }
  }
}
```

### cURL Example

```bash
curl -X POST "http://localhost:2700/graphql" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query { siteSearch(siteName: SAMPLE, searchParams: { q: \"enterprise search\", rows: 10 }, locale: \"en_US\") { queryContext { count } results { document { fields { title url } } } } }"
  }'
```

---

## Configuration

GraphQL is configured in `application.yaml`:

```yaml
spring:
  graphql:
    graphiql:
      enabled: true
      path: /graphiql
    http.path: /graphql
    cors:
      allowed-origins: ${turing.allowedOrigins}
      allowed-methods: GET,POST
      allowed-headers: "*"
```

| Property | Default | Description |
|---|---|---|
| `spring.graphql.http.path` | `/graphql` | GraphQL endpoint path |
| `spring.graphql.graphiql.enabled` | `true` | Enable GraphiQL browser IDE |
| `spring.graphql.graphiql.path` | `/graphiql` | GraphiQL IDE path |
| `spring.graphql.cors.allowed-origins` | `${turing.allowedOrigins}` | CORS allowed origins |
| `spring.graphql.cors.allowed-methods` | `GET,POST` | Allowed HTTP methods |

For the full configuration reference, see [Configuration Reference → GraphQL](/docs/configuration-reference/#graphql).

---

## Error Handling

| Scenario | Behavior |
|---|---|
| Invalid site name | Returns `null` for `siteSearch`: the site is not found |
| Unsupported locale | Falls back to the site's first available locale |
| Invalid filter operator | Falls back to `NONE` |
| Invalid field in `fl` | Field is ignored: only valid fields are returned |
| Empty query (`q: ""`) | Treated as `q: "*"` (match all) |

GraphQL validation errors (missing required fields, wrong types) are returned as standard GraphQL error responses before the query reaches the resolver.

---

## Related Pages

| Page | Description |
|---|---|
| [REST API Reference](/docs/rest-api/) | REST search endpoint with the same capabilities |
| [Semantic Navigation](/docs/semantic-navigation/) | SN Site configuration: fields, facets, and search behavior |
| [Configuration Reference](/docs/configuration-reference/) | Full `application.yaml` property reference |

---

