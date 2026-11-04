package com.viglet.turing.elasticsearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
import com.viglet.turing.persistence.model.sn.ranking.TurSNRankingCondition;
import com.viglet.turing.persistence.model.sn.ranking.TurSNRankingExpression;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.persistence.repository.sn.ranking.TurSNRankingConditionRepository;
import com.viglet.turing.persistence.repository.sn.ranking.TurSNRankingExpressionRepository;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/**
 * T1222 — a site's ranking expressions become Elasticsearch boosts by the rules
 * Lucene and Solr apply.
 */
@ExtendWith(MockitoExtension.class)
class TurElasticsearchRankingExpressionsTest {

    private static final long NOW = Instant.parse("2026-10-10T15:00:00Z").toEpochMilli();

    @Mock
    private TurSNRankingExpressionRepository expressionRepository;
    @Mock
    private TurSNRankingConditionRepository conditionRepository;
    @Mock
    private TurSNSiteFieldExtRepository fieldExtRepository;

    private final TurSNSite site = new TurSNSite();

    private TurElasticsearchRankingExpressions rankingExpressions() {
        return new TurElasticsearchRankingExpressions(expressionRepository, conditionRepository, fieldExtRepository);
    }

    private void fields(Object... nameAndType) {
        List<TurSNSiteFieldExt> fields = new ArrayList<>();
        for (int i = 0; i < nameAndType.length; i += 2) {
            fields.add(TurSNSiteFieldExt.builder().name((String) nameAndType[i])
                    .type((TurSEFieldType) nameAndType[i + 1]).build());
        }
        when(fieldExtRepository.findByTurSNSite(any(), any())).thenReturn(fields);
    }

    private TurSNRankingExpression expression(String name, float weight, int condition, String... attributeAndValue) {
        TurSNRankingExpression expression = new TurSNRankingExpression();
        expression.setName(name);
        expression.setWeight(weight);
        Set<TurSNRankingCondition> conditions = new LinkedHashSet<>();
        for (int i = 0; i < attributeAndValue.length; i += 2) {
            TurSNRankingCondition c = new TurSNRankingCondition();
            c.setAttribute(attributeAndValue[i]);
            c.setValue(attributeAndValue[i + 1]);
            c.setCondition(condition);
            conditions.add(c);
        }
        when(conditionRepository.findByTurSNRankingExpression(expression)).thenReturn(conditions);
        return expression;
    }

    private String only(TurSNRankingExpression... expressions) {
        when(expressionRepository.findByTurSNSite(any(), any())).thenReturn(new LinkedHashSet<>(List.of(expressions)));
        List<Query> boosts = rankingExpressions().boostQueries(site, NOW);
        assertThat(boosts).hasSize(1);
        return boosts.getFirst().toString();
    }

    @Test
    void anExpressionIsAWeightedBoolOfItsConditions() {
        fields("category", TurSEFieldType.STRING, "title", TurSEFieldType.TEXT, "salary", TurSEFieldType.INT);
        String json = only(expression("it", 8f, TurSNRankingCondition.IS, "category", "it", "title", "data engineer",
                "salary", "9000"));

        assertThat(json).contains("\"boost\":8.0")
                .contains("\"category.keyword\":{\"value\":\"it\"}")
                .contains("\"match_phrase\":{\"title\":{\"query\":\"data engineer\"}}")
                .contains("\"salary\":{\"value\":\"9000\"}");
    }

    @Test
    void isNotIsAMustNotAndExistsAnExistsQuery() {
        fields("category", TurSEFieldType.STRING, "salaryMin", TurSEFieldType.LONG);
        assertThat(only(expression("not-it", 3f, TurSNRankingCondition.IS_NOT, "category", "it")))
                .contains("\"must_not\":[{\"term\":{\"category.keyword\":{\"value\":\"it\"}}}]")
                .contains("\"match_all\"");
    }

    @Test
    void existsMatchesAnyValue() {
        fields("salaryMin", TurSEFieldType.LONG);
        assertThat(only(expression("has-salary", 4f, TurSNRankingCondition.EXISTS, "salaryMin", "")))
                .contains("\"exists\":{\"field\":\"salaryMin\"}");
    }

    @Test
    void recencyIsADistanceFeatureFromTodayAndAFutureDateScoresAsToday() {
        fields("publicationDate", TurSEFieldType.DATE);
        String json = only(expression("recent", 2f, TurSNRankingCondition.IS, "publicationDate", "asc"));

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
        String json = only(expression("upcoming", 2f, TurSNRankingCondition.IS, "eventEnd", "upcoming"));

        assertThat(json).contains("\"distance_feature\"").contains("\"gte\":\"2026-10-10T00:00:00Z\"")
                .doesNotContain("constant_score");
    }

    @Test
    void anExpressionThatCannotBeExpressedIsSkippedWhole() {
        fields("publicationDate", TurSEFieldType.DATE, "category", TurSEFieldType.STRING);
        TurSNRankingExpression notRecent = expression("not-recent", 2f, TurSNRankingCondition.IS_NOT,
                "publicationDate", "asc");
        TurSNRankingExpression badDate = expression("bad-date", 2f, TurSNRankingCondition.IS,
                "category", "it", "publicationDate", "yesterday");
        TurSNRankingExpression zero = expression("zero", 0f, TurSNRankingCondition.IS, "category", "it");
        when(expressionRepository.findByTurSNSite(any(), any()))
                .thenReturn(new LinkedHashSet<>(List.of(notRecent, badDate, zero)));

        assertThat(rankingExpressions().boostQueries(site, NOW)).isEmpty();
    }

    @Test
    void noExpressionsMeansNoBoosts() {
        when(expressionRepository.findByTurSNSite(any(), any())).thenReturn(Set.of());

        assertThat(rankingExpressions().boostQueries(site, NOW)).isEmpty();
    }
}
