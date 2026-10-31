package com.viglet.turing.api.sn.dsl;

import java.util.Locale;

import com.viglet.turing.api.sn.search.TurSNSiteSearchService;
import com.viglet.turing.commons.sn.search.TurSNParamType;
import com.viglet.turing.sn.dsl.TurDslQueryRequest;
import com.viglet.turing.sn.dsl.TurDslSearchResponse;
import com.viglet.turing.sn.dsl.TurDslSearchService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoint for Elasticsearch-compatible Query DSL searches.
 * <p>
 * Provides the same {@code _search} contract used by Elasticsearch, translating
 * queries to the configured search backend (currently Apache Solr).
 * <p>
 * Usage examples:
 * <pre>{@code
 * # Match query
 * POST /api/sn/MySite/_search?locale=en
 * { "query": { "match": { "title": "enterprise search" } } }
 *
 * # Bool query with filter and aggregations
 * POST /api/sn/MySite/_search?locale=en
 * {
 *   "query": {
 *     "bool": {
 *       "must": [{ "match": { "title": "enterprise search" } }],
 *       "filter": [{ "term": { "status": "published" } }]
 *     }
 *   },
 *   "from": 0,
 *   "size": 10,
 *   "sort": [{ "date": "desc" }],
 *   "aggs": {
 *     "categories": { "terms": { "field": "category", "size": 5 } }
 *   }
 * }
 *
 * # Match all (GET without body)
 * GET /api/sn/MySite/_search?locale=en
 * }</pre>
 *
 * @author Alexandre Oliveira
 * @since 2026.2.4
 */
@RestController
@RequestMapping("/api/sn/{siteName}/_search")
public class TurSNSiteDslSearchAPI {

    private final TurDslSearchService turDslSearchService;
    private final TurSNSiteSearchService turSNSiteSearchService;

    public TurSNSiteDslSearchAPI(TurDslSearchService turDslSearchService,
            TurSNSiteSearchService turSNSiteSearchService) {
        this.turDslSearchService = turDslSearchService;
        this.turSNSiteSearchService = turSNSiteSearchService;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TurDslSearchResponse> search(
            @PathVariable String siteName,
            @RequestParam(name = TurSNParamType.LOCALE, required = false) String setLocale,
            @RequestParam(name = "locale", required = false) String locale,
            @RequestBody TurDslQueryRequest request) {
        // T1189 — `_setlocale` as search reads it, `locale` kept as an alias; a
        // locale the site has no index for searches its default indexed one.
        Locale resolved = turSNSiteSearchService.resolveIndexedLocale(
                setLocale != null && !setLocale.isBlank() ? setLocale : locale, siteName);
        if (resolved == null) {
            return ResponseEntity.notFound().build();
        }
        return turDslSearchService.search(siteName, resolved.toString(), request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TurDslSearchResponse> searchGet(
            @PathVariable String siteName,
            @RequestParam(name = TurSNParamType.LOCALE, required = false) String setLocale,
            @RequestParam(name = "locale", required = false) String locale) {
        return search(siteName, setLocale, locale,
                new TurDslQueryRequest(null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null));
    }
}
