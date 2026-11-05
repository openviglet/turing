package com.viglet.turing.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.commons.sn.TurSNConfig;
import com.viglet.turing.commons.sn.bean.TurSNSearchParams;
import com.viglet.turing.commons.sn.search.TurSNSiteSearchContext;
import com.viglet.turing.lucene.TurLuceneCustomFacets;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.persistence.repository.sn.sort.TurSNSiteCustomSortRepository;
import com.viglet.turing.se.facet.TurSEFacetResult;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.FiltersBucket;
import co.elastic.clients.elasticsearch.core.SearchRequest;

/**
 * T1232 — an Elasticsearch site counts its custom facets: one {@code filters}
 * aggregation per facet, a bucket per item built by the query a click on it
 * filters with, read back in item order with the items' display labels.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
class TurElasticsearchCustomFacetsTest {

    private static final TurLuceneCustomFacets.Facet SALARY = new TurLuceneCustomFacets.Facet("salaryBand", 3,
            List.of(new TurLuceneCustomFacets.Item("up to 3000", "até 3000",
                    new TurLuceneCustomFacets.ItemFilter("salary", TurSEFieldType.INT, "[* TO 3000]")),
                    new TurLuceneCustomFacets.Item("over 3000", "over 3000",
                            new TurLuceneCustomFacets.ItemFilter("salary", TurSEFieldType.INT, "{3000 TO *]"))));

    @Test
    void eachFacetIsAFiltersAggregationKeyedByItemLabel() {
        SearchRequest.Builder builder = new SearchRequest.Builder().index("jobs");

        TurElasticsearch.addCustomFacetAggregations(builder, List.of(SALARY), Instant.now());

        var aggregation = builder.build().aggregations().get(TurElasticsearch.CUSTOM_FACET_AGG_PREFIX + "salaryBand");
        assertThat(aggregation.isFilters()).isTrue();
        var keyed = aggregation.filters().filters().keyed();
        assertThat(keyed.keySet()).containsExactly("up to 3000", "over 3000");
        assertThat(keyed.get("up to 3000").range().untyped().field()).isEqualTo("salary");
    }

    @Test
    void bucketsAreReadInItemOrderWithDisplayLabels() {
        Map<String, FiltersBucket> buckets = new LinkedHashMap<>();
        buckets.put("over 3000", FiltersBucket.of(b -> b.docCount(1)));
        buckets.put("up to 3000", FiltersBucket.of(b -> b.docCount(2)));
        Aggregate aggregate = Aggregate.of(a -> a.filters(f -> f.buckets(b -> b.keyed(buckets))));

        List<TurSEFacetResult> results = TurElasticsearch.customFacetResults(
                Map.of(TurElasticsearch.CUSTOM_FACET_AGG_PREFIX + "salaryBand", aggregate), List.of(SALARY));

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.getFacet()).isEqualTo("salaryBand");
            assertThat(result.getFacetPosition()).isEqualTo(3);
            assertThat(result.getTurSEFacetResultAttr().keySet()).containsExactly("up to 3000", "over 3000");
            assertThat(result.getTurSEFacetResultAttr().get("up to 3000").getCount()).isEqualTo(2);
            assertThat(result.getTurSEFacetResultAttr().get("up to 3000").getDisplayLabel()).isEqualTo("até 3000");
        });
    }

    @Test
    void theSearchRequestAsksForTheSitesCustomFacets() throws Exception {
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurLuceneCustomFacets customFacets = mock(TurLuceneCustomFacets.class);
        ElasticsearchClient client = mock(ElasticsearchClient.class);
        TurSNSite site = new TurSNSite();
        site.setName("jobs");
        site.setFacet(1);
        when(siteRepository.findByNameIgnoreCase("jobs")).thenReturn(Optional.of(site));
        when(customFacets.facets(site, Locale.US)).thenReturn(List.of(SALARY));
        ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);
        when(client.search(request.capture(), any(Type.class))).thenThrow(new IOException("stop here"));
        TurElasticsearch engine = new TurElasticsearch(siteRepository, mock(TurSNSiteFieldExtRepository.class),
                mock(TurSNSiteCustomSortRepository.class), customFacets);
        TurSNSearchParams params = new TurSNSearchParams();
        params.setQ("*");
        TurSNSiteSearchContext context = new TurSNSiteSearchContext("jobs", new TurSNConfig(),
                new TurSEParameters(params), Locale.US, URI.create("http://localhost/api/sn/jobs/search"));

        engine.retrieveElasticsearchFromSN(new TurElasticsearchInstance(client,
                URI.create("http://localhost:9200").toURL(), "jobs"), context);

        assertThat(request.getValue().aggregations())
                .containsKey(TurElasticsearch.CUSTOM_FACET_AGG_PREFIX + "salaryBand");
    }
}
