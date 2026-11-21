package com.viglet.turing.searchengine.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.sn.persistence.model.TurSNSite;
import com.viglet.turing.sn.persistence.model.field.TurSNSiteFieldExt;
import com.viglet.turing.sn.api.TurSNSiteFieldExts;
import com.viglet.turing.searchengine.spi.TurSERankingCondition;
import com.viglet.turing.searchengine.spi.TurSERankingCondition.Kind;
import com.viglet.turing.searchengine.spi.TurSERankingExpression;
import com.viglet.turing.searchengine.spi.TurSESiteSearchConfig;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/**
 * T1222 — a site's ranking expressions become Elasticsearch boosts by the rules
 * Lucene and Solr apply.
 */
@ExtendWith(MockitoExtension.class)
class TurElasticsearchRankingExpressionsTest {

    private static final long NOW = Instant.parse("2026-10-10T15:00:00Z").toEpochMilli();

    @Mock
    private TurSESiteSearchConfig siteSearchConfig;
    @Mock
    private TurSNSiteFieldExts fieldExtRepository;

    private final TurSNSite site = new TurSNSite();

    private TurElasticsearchRankingExpressions rankingExpressions() {
        return new TurElasticsearchRankingExpressions(siteSearchConfig, fieldExtRepository);
    }

    private void fields(Object... nameAndType) {
        List<TurSNSiteFieldExt> fields = new ArrayList<>();
        for (int i = 0; i < nameAndType.length; i += 2) {
            fields.add(TurSNSiteFieldExt.builder().name((String) nameAndType[i])
                    .type((TurSEFieldType) nameAndType[i + 1]).build());
        }
        when(fieldExtRepository.findByTurSNSite(any(), any())).thenReturn(fields);
    }

    private static TurSERankingExpression expression(String name, float weight, Kind kind,
            String... attributeAndValue) {
        List<TurSERankingCondition> conditions = new ArrayList<>();
        for (int i = 0; i < attributeAndValue.length; i += 2) {
            conditions.add(new TurSERankingCondition(attributeAndValue[i], kind, attributeAndValue[i + 1]));
        }
        return new TurSERankingExpression(name, weight, conditions);
    }

    private String only(TurSERankingExpression... expressions) {
        when(siteSearchConfig.rankingExpressions(any())).thenReturn(List.of(expressions));
        List<Query> boosts = rankingExpressions().boostQueries(site, NOW);
        assertThat(boosts).hasSize(1);
        return boosts.getFirst().toString();
    }

    @Test
    void anExpressionIsAWeightedBoolOfItsConditions() {
        fields("category", TurSEFieldType.STRING, "title", TurSEFieldType.TEXT, "salary", TurSEFieldType.INT);
        String json = only(expression("it", 8f, Kind.IS, "category", "it", "title", "data engineer",
                "salary", "9000"));

        assertThat(json).contains("\"boost\":8.0")
                .contains("\"category.keyword\":{\"value\":\"it\"}")
                .contains("\"match_phrase\":{\"title\":{\"query\":\"data engineer\"}}")
                .contains("\"salary\":{\"value\":\"9000\"}");
    }

    @Test
    void isNotIsAMustNotAndExistsAnExistsQuery() {
        fields("category", TurSEFieldType.STRING, "salaryMin", TurSEFieldType.LONG);
        assertThat(only(expression("not-it", 3f, Kind.IS_NOT, "category", "it")))
                .contains("\"must_not\":[{\"term\":{\"category.keyword\":{\"value\":\"it\"}}}]")
                .contains("\"match_all\"");
    }

    @Test
    void existsMatchesAnyValue() {
        fields("salaryMin", TurSEFieldType.LONG);
        assertThat(only(expression("has-salary", 4f, Kind.EXISTS, "salaryMin", "")))
                .contains("\"exists\":{\"field\":\"salaryMin\"}");
    }

    @Test
    void recencyIsADistanceFeatureFromTodayAndAFutureDateScoresAsToday() {
        fields("publicationDate", TurSEFieldType.DATE);
        String json = only(expression("recent", 2f, Kind.IS, "publicationDate", "asc"));

        assertThat(json).contains("\"distance_feature\"")
                .contains("\"origin\":\"2026-10-10T00:00:00Z\"")
                .contains("\"pivot\":\"" + TurElasticsearchRankingExpressions.RECENCY_PIVOT_MS + "ms\"")
                .contains("\"lte\":\"2026-10-10T00:00:00Z\"")
                .contains("\"constant_score\"")
                .contains("\"gte\":\"2026-10-10T00:00:00.001Z\"");
    }

    @Test
    void upcomingBoostsOnlyTodayOrLater() {
        fields("eventEnd", TurSEFieldType.DATE);
        String json = only(expression("upcoming", 2f, Kind.IS, "eventEnd", "upcoming"));

        assertThat(json).contains("\"distance_feature\"").contains("\"gte\":\"2026-10-10T00:00:00Z\"")
                .doesNotContain("constant_score");
    }

    @Test
    void anExpressionThatCannotBeExpressedIsSkippedWhole() {
        fields("publicationDate", TurSEFieldType.DATE, "category", TurSEFieldType.STRING);
        TurSERankingExpression notRecent = expression("not-recent", 2f, Kind.IS_NOT,
                "publicationDate", "asc");
        TurSERankingExpression badDate = expression("bad-date", 2f, Kind.IS,
                "category", "it", "publicationDate", "yesterday");
        TurSERankingExpression zero = expression("zero", 0f, Kind.IS, "category", "it");
        when(siteSearchConfig.rankingExpressions(any())).thenReturn(List.of(notRecent, badDate, zero));

        assertThat(rankingExpressions().boostQueries(site, NOW)).isEmpty();
    }

    @Test
    void noExpressionsMeansNoBoosts() {
        when(siteSearchConfig.rankingExpressions(any())).thenReturn(List.of());

        assertThat(rankingExpressions().boostQueries(site, NOW)).isEmpty();
    }
}
