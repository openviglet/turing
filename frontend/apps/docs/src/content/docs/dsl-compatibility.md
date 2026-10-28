---
title: "DSL Compatibility Matrix"
description: "Detailed compatibility matrix of Viglet Turing DSL Query API across Elasticsearch, Apache Solr, and Apache Lucene engines."
---
export const N = () => (<span style={{backgroundColor: '#16a34a', color: '#fff', borderRadius: '4px', padding: '1px 7px', fontSize: '0.78em', fontWeight: 700, letterSpacing: '0.5px'}}>N</span>);
export const T = () => (<span style={{backgroundColor: '#2563eb', color: '#fff', borderRadius: '4px', padding: '1px 7px', fontSize: '0.78em', fontWeight: 700, letterSpacing: '0.5px'}}>T</span>);
export const P = () => (<span style={{backgroundColor: '#d97706', color: '#fff', borderRadius: '4px', padding: '1px 7px', fontSize: '0.78em', fontWeight: 700, letterSpacing: '0.5px'}}>P</span>);
export const F = () => (<span style={{backgroundColor: '#9333ea', color: '#fff', borderRadius: '4px', padding: '1px 7px', fontSize: '0.78em', fontWeight: 700, letterSpacing: '0.5px'}}>F</span>);
export const X = () => (<span style={{backgroundColor: '#d1d5db', color: '#6b7280', borderRadius: '4px', padding: '1px 7px', fontSize: '0.78em', fontWeight: 700, letterSpacing: '0.5px'}}>N/A</span>);


This page is the **support map** for the [DSL Query API](/docs/dsl-query/): for every query type, aggregation, and option, it shows how faithfully each search engine backend (Elasticsearch, Solr, Lucene) implements it. Because Viglet Turing exposes one Elasticsearch-compatible syntax over three different engines, a feature that's native on one may be translated or only partially supported on another.

Reach for this table when you're choosing a backend for a site, or when a DSL query behaves differently than you expected across engines: the badge tells you whether that's a 1:1 mapping, a translation, or a best-effort approximation. Read it alongside the query examples on the [DSL Query API](/docs/dsl-query/) page.

:::note[Content AI is not in this matrix]
A fourth engine, [Adobe AEM Content AI](/docs/search-engine/#content-ai), serves the DSL
API over a small, explicitly listed subset rather than a translation of the whole
syntax, so it has its own section: [Adobe AEM Content
AI](#adobe-aem-content-ai-a-listed-subset) at the bottom of this page. The three
columns below are Elasticsearch, Solr and Lucene.
:::

### Legend

| Badge | Level | Description |
|:---:|-------|-------------|
| <N/> | **Native** | Direct 1:1 mapping to the engine API |
| <T/> | **Translated** | Converted to equivalent engine syntax |
| <P/> | **Partial** | Basic support with limitations |
| <F/> | **Fallback** | Best-effort approximation |
| <X/> | **N/A** | Not supported by the engine |

---

## Queries (40 types)

### Full-text Queries

| Query | ES | Solr | Lucene |
|-------|:---:|:---:|:---:|
| `match` | <N/> | <T/> | <T/> |
| `multi_match` | <N/> | <T/> | <T/> |
| `match_phrase` | <N/> | <T/> | <T/> |
| `match_phrase_prefix` | <N/> | <T/> | <T/> |
| `match_bool_prefix` | <N/> | <T/> | <T/> |
| `combined_fields` | <N/> | <T/> | <T/> |
| `query_string` | <N/> | <T/> | <T/> |
| `simple_query_string` | <N/> | <T/> | <T/> |
| `match_all` | <N/> | <T/> | <T/> |

### Term-level Queries

| Query | ES | Solr | Lucene |
|-------|:---:|:---:|:---:|
| `term` | <N/> | <T/> | <T/> |
| `terms` | <N/> | <T/> | <T/> |
| `terms_set` | <N/> | <F/> | <F/> |
| `range` | <N/> | <T/> | <T/> |
| `exists` | <N/> | <T/> | <T/> |
| `prefix` | <N/> | <T/> | <T/> |
| `wildcard` | <N/> | <T/> | <T/> |
| `regexp` | <N/> | <T/> | <T/> |
| `fuzzy` | <N/> | <T/> | <T/> |
| `ids` | <N/> | <T/> | <T/> |

### Compound Queries

| Query | ES | Solr | Lucene |
|-------|:---:|:---:|:---:|
| `bool` | <N/> | <T/> | <T/> |
| `constant_score` | <N/> | <T/> | <T/> |
| `dis_max` | <N/> | <T/> | <T/> |
| `boosting` | <N/> | <T/> | <T/> |
| `function_score` | <N/> | <P/> | <P/> |
| `script_score` | <N/> | <F/> | <F/> |
| `pinned` | <N/> | <T/> | <T/> |

### Nested & Join Queries

| Query | ES | Solr | Lucene |
|-------|:---:|:---:|:---:|
| `nested` | <N/> | <F/> | <F/> |
| `has_child` | <N/> | <F/> | <F/> |
| `has_parent` | <N/> | <F/> | <F/> |

### Geo Queries

| Query | ES | Solr | Lucene |
|-------|:---:|:---:|:---:|
| `geo_distance` | <N/> | <T/> | <F/> |
| `geo_bounding_box` | <N/> | <T/> | <F/> |
| `geo_shape` | <N/> | <F/> | <F/> |

### Vector Search

| Query | ES | Solr | Lucene |
|-------|:---:|:---:|:---:|
| `knn` | <N/> | <T/> | <N/> |

### Span Queries

| Query | ES | Solr | Lucene |
|-------|:---:|:---:|:---:|
| `span_term` | <N/> | <T/> | <T/> |
| `span_near` | <N/> | <T/> | <T/> |
| `span_or` | <N/> | <T/> | <T/> |
| `span_not` | <N/> | <T/> | <T/> |
| `span_first` | <N/> | <F/> | <F/> |

### Specialized Queries

| Query | ES | Solr | Lucene |
|-------|:---:|:---:|:---:|
| `more_like_this` | <N/> | <T/> | <T/> |
| `intervals` | <N/> | <T/> | <T/> |
| `rank_feature` | <N/> | <F/> | <T/> |
| `distance_feature` | <N/> | <F/> | <F/> |
| `wrapper` | <N/> | <F/> | <F/> |
| `percolate` | <N/> | <F/> | <F/> |

---

## Aggregations (35 types)

### Bucket Aggregations

| Aggregation | ES | Solr | Lucene |
|-------------|:---:|:---:|:---:|
| `terms` | <N/> | <T/> | <T/> |
| `range` | <N/> | <T/> | <X/> |
| `date_histogram` | <N/> | <X/> | <X/> |
| `histogram` | <N/> | <X/> | <X/> |
| `filter` | <N/> | <T/> | <T/> |
| `filters` | <N/> | <T/> | <T/> |
| `significant_terms` | <N/> | <T/> | <T/> |
| `rare_terms` | <N/> | <P/> | <T/> |
| `nested` | <N/> | <X/> | <X/> |
| `reverse_nested` | <N/> | <X/> | <X/> |
| `auto_date_histogram` | <N/> | <X/> | <X/> |
| `multi_terms` | <N/> | <X/> | <X/> |
| `composite` | <N/> | <X/> | <X/> |
| `sampler` | <N/> | <X/> | <X/> |
| `diversified_sampler` | <N/> | <X/> | <X/> |
| `adjacency_matrix` | <N/> | <X/> | <X/> |
| `geo_distance` | <N/> | <X/> | <X/> |
| `variable_width_histogram` | <N/> | <X/> | <X/> |

### Metric Aggregations

| Aggregation | ES | Solr | Lucene |
|-------------|:---:|:---:|:---:|
| `avg` | <N/> | <T/> | <T/> |
| `sum` | <N/> | <T/> | <T/> |
| `min` | <N/> | <T/> | <T/> |
| `max` | <N/> | <T/> | <T/> |
| `cardinality` | <N/> | <T/> | <T/> |
| `value_count` | <N/> | <T/> | <T/> |
| `stats` | <N/> | <T/> | <T/> |
| `extended_stats` | <N/> | <T/> | <T/> |
| `percentiles` | <N/> | <T/> | <T/> |
| `percentile_ranks` | <N/> | <T/> | <T/> |
| `top_hits` | <N/> | <X/> | <X/> |
| `top_metrics` | <N/> | <X/> | <X/> |
| `median_absolute_deviation` | <N/> | <X/> | <X/> |
| `boxplot` | <N/> | <X/> | <X/> |
| `string_stats` | <N/> | <X/> | <X/> |
| `matrix_stats` | <N/> | <X/> | <X/> |
| `t_test` | <N/> | <X/> | <X/> |
| `rate` | <N/> | <X/> | <X/> |
| `scripted_metric` | <N/> | <X/> | <X/> |
| `geo_bounds` | <N/> | <X/> | <X/> |
| `geo_centroid` | <N/> | <X/> | <X/> |

---

## Request Features (29 fields)

| Feature | ES | Solr | Lucene |
|---------|:---:|:---:|:---:|
| `from` / `size` | <N/> | <T/> | <T/> |
| `sort` | <N/> | <T/> | <T/> |
| `_source` | <N/> | <T/> | <T/> |
| `highlight` | <N/> | <T/> | <T/> |
| `aggs` | <N/> | <T/> | <T/> |
| `post_filter` | <N/> | <T/> | <T/> |
| `min_score` | <N/> | <T/> | <P/> |
| `search_after` | <N/> | <X/> | <X/> |
| `collapse` | <N/> | <T/> | <X/> |
| `suggest` | <N/> | <P/> | <X/> |
| `rescore` | <N/> | <P/> | <X/> |
| `timeout` | <N/> | <T/> | <X/> |
| `explain` | <N/> | <T/> | <X/> |
| `script_fields` | <N/> | <X/> | <X/> |
| `indices_boost` | <N/> | <X/> | <X/> |
| `track_total_hits` | <N/> | <T/> | <T/> |
| `stored_fields` | <N/> | <T/> | <X/> |
| `scroll` | <N/> | <X/> | <X/> |
| `profile` | <N/> | <X/> | <X/> |
| `pit` | <N/> | <X/> | <X/> |
| `docvalue_fields` | <N/> | <X/> | <X/> |
| `version` | <N/> | <X/> | <X/> |
| `seq_no_primary_term` | <N/> | <X/> | <X/> |
| `preference` | <N/> | <X/> | <X/> |
| `routing` | <N/> | <X/> | <X/> |
| `terminate_after` | <N/> | <X/> | <X/> |

---

## Response Fields

| Field | ES | Solr | Lucene |
|-------|:---:|:---:|:---:|
| `took` | <N/> | <T/> | <T/> |
| `timed_out` | <N/> | <T/> | <T/> |
| `hits.total` | <N/> | <T/> | <T/> |
| `hits.max_score` | <N/> | <T/> | <T/> |
| `hits._id` | <N/> | <T/> | <T/> |
| `hits._score` | <N/> | <T/> | <T/> |
| `hits._source` | <N/> | <T/> | <T/> |
| `hits.highlight` | <N/> | <T/> | <T/> |
| `hits._explanation` | <N/> | <X/> | <X/> |
| `hits.fields` | <N/> | <X/> | <X/> |
| `hits._version` | <N/> | <X/> | <X/> |
| `hits._seq_no` | <N/> | <X/> | <X/> |
| `hits.sort` | <N/> | <X/> | <X/> |
| `aggregations` | <N/> | <T/> | <T/> |
| `suggest` | <N/> | <X/> | <X/> |
| `_scroll_id` | <N/> | <X/> | <X/> |
| `profile` | <N/> | <X/> | <X/> |
| `_shards` | <N/> | <X/> | <X/> |

---

## Coverage Summary

| Category | Total | ES | Solr | Lucene |
|----------|:-----:|:---:|:---:|:---:|
| **Queries** | 40 | <N/> 100% | <T/> 100% | <T/> 100% |
| **Aggregations** | 35 | <N/> 100% | <T/> 49% | <T/> 31% |
| **Request Features** | 29 | <N/> 100% | <T/> 41% | <T/> 21% |
| **Response Fields** | 18 | <N/> 100% | <T/> 44% | <T/> 44% |

:::tip[Recommendation]
For **full DSL compatibility**, use Elasticsearch as the search engine backend. Solr and Lucene provide excellent coverage for the most common queries and aggregations, with best-effort translations for advanced features.
:::

---

## Engine Selection Guide

| Use Case | Recommended |
|----------|:---:|
| Full Elasticsearch DSL compatibility | <N/> Elasticsearch |
| Production enterprise search with Solr infrastructure | <T/> Solr |
| Embedded search without external dependencies | <T/> Lucene |
| Vector / semantic search (knn) | <N/> ES or <N/> Lucene |
| Advanced aggregations (composite, nested, geo) | <N/> Elasticsearch |
| Simple faceted search with metric aggs | <T/> Solr or <T/> Lucene |

---

## Adobe AEM Content AI: a listed subset

Content AI is a [federated, read-only index](/docs/search-engine/#content-ai) whose query
language is Adobe's own: a filter DSL plus semantic and keyword queries, not Lucene
syntax. So rather than approximate the whole Elasticsearch surface, Turing translates
a defined subset and **refuses the rest by name**, with `501 Not Implemented`:

| Query / feature | Content AI | Notes |
|---|:---:|---|
| `match_all` | <T/> | `NOT exists(...)` over a reserved field name: an empty filter group is invalid in Adobe's schema |
| `match` | <T/> | A scoring `fulltext` query |
| `term` | <T/> | `StringFilter`, `NumberFilter` or `BooleanFilter`, chosen from the value's type |
| `terms` | <T/> | `InFilter` |
| `range` | <T/> | An `AND` of one-sided `NumberFilter` / `DateFilter` bounds |
| `exists` | <N/> | `ExistsFilter` |
| `prefix` | <N/> | `PrefixFilter` |
| `bool` | <P/> | Text clauses become the arms of a `composite`; the rest becomes one filter group used as their pre-filter. `minimum_should_match > 1` and a `must_not` over a text query are refused |
| `terms` aggregation | <T/> | `termBuckets` |
| Sorting | <T/> | `orderBy`; `_score` is dropped, since Adobe already sorts by descending score |
| `size` | <P/> | Capped at 50 by the API |
| `from` (offset) | <X/> | Adobe paginates by opaque cursor; a non-zero `from` is refused rather than answered with page one |
| Everything else | <X/> | `knn`, `nested`, geo, `function_score`, span, join queries, other aggregations |

**Why refuse rather than approximate.** A clause quietly dropped because it could not be
translated would leave a query that still runs, and answers confidently from the wrong
set of documents. A `501` naming the clause is a worse response and a better answer.

This subset is what the **vectorless copilot** needs, which is the point: that surface
needs no embeddings, so it is the grounded-answer path that suits a federated index, and
it runs entirely through this API.

---

## Related Pages

- [DSL Query API](/docs/dsl-query/): the query syntax this matrix describes
- [Search Engine](/docs/search-engine/): configure the Solr / Elasticsearch / Lucene backend
- [REST API Reference](/docs/rest-api/): the simpler `/search` endpoint

