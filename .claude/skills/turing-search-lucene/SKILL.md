---
name: turing-search-lucene
description: Turing search-engine plugin abstraction and Lucene 10 gotchas — the TurSearchEnginePlugin lifecycle interface, resolving plugins via TurSearchEnginePluginFactory, Lucene 10 API removals (no FacetsCollector.search(), no TopScoreDocCollector.create(int,int)), the primitive int flags on TurSNSiteFieldExt (compare == 1, not null), and that the embedded Lucene vector store (TurLuceneStoreProvider) is separate from the Lucene search-engine plugin. Use whenever working with search-engine plugins (Solr/Elasticsearch/Lucene), the searchengine module and its SPI, faceting, or the Lucene vector store.
---

# Search Engine & Lucene Notes

- The `searchengine` module (ADR 0005, layer 3) holds the engines. Its API is the base
  package: `TurSearchEnginePlugin`, the plugin lifecycle interface, and
  `TurSearchEnginePluginFactory` (`getPluginForSeInstance(String id)`,
  `getPluginForInstance(TurSEInstance)`, `getPlugin(String type)`), plus the `result`,
  `facet`, `dsl` and `spi` packages. The Solr, Lucene, Elasticsearch and Content AI
  implementations (`searchengine.solr`, `.lucene`, `.elasticsearch`, `.contentai`) are
  internal: other modules reach an engine only through the plugin. Plugin type strings
  come from `getPluginType()` and are lowercased.
- searchengine imports nothing from `sn` (T1310). The plugin contract takes the
  `TurSESite` / `TurSESiteLocale` records (`searchengine.spi`), which sn builds from its
  entities with `sn.api.TurSNSiteRefs.toSE(...)`; an engine reads a site's fields,
  facets, ranking expressions and sorts through the `TurSESiteSearchConfig` SPI that sn
  implements. A new piece of site configuration an engine needs extends that SPI.
- The Lucene search-engine plugin lives under
  `turing-app/src/main/java/com/viglet/turing/searchengine/lucene/` and uses Lucene 10.
  Lucene 10 removed `FacetsCollector.search()`; do not use it.
- `TopScoreDocCollector.create(int, int)` does not exist in Lucene 10. Use
  separate `searcher.search()` calls where needed.
- On sn's side, `TurSNSiteFieldExt.getFacet()`, `getEnabled()`, and `getMultiValued()` are
  primitive `int` flags; compare with `== 1`, not null checks. Only
  `getFacetPosition()` is nullable.
- The embedded Lucene vector store is separate from the search-engine Lucene
  plugin. It belongs to `llm` (`llm.provider.store.lucene.TurLuceneStoreProvider`,
  plugin type `lucene`), which owns the vector stores.
- In SN field-check flows, `TurSolrFieldBean.getType()` may be `null`; prefer
  `Objects.equals(actual, expected)` over calling `.equals()` on nullable values.
