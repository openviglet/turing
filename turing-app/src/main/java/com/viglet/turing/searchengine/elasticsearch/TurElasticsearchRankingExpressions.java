/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.viglet.turing.searchengine.elasticsearch;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.searchengine.spi.TurSERankingCondition;
import com.viglet.turing.searchengine.spi.TurSERankingExpression;
import com.viglet.turing.searchengine.spi.TurSESiteSearchConfig;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.UntypedRangeQuery;
import co.elastic.clients.json.JsonData;
import lombok.extern.slf4j.Slf4j;

/**
 * T1222 — a site's ranking expressions as Elasticsearch boosts, the counterpart of
 * {@code TurLuceneRankingExpressions} (T1064) and of Solr's boost queries, from
 * the same repositories and the same rules.
 *
 * <p>Each expression becomes one {@code bool} whose conditions are all
 * {@code must}, boosted by the expression's weight. The caller adds it as a
 * {@code should} beside the text query, so it raises scores without changing
 * which documents match. {@code Is} is an exact match ({@code match_phrase} on
 * TEXT, {@code term} on {@code .keyword} for string-like fields, a plain
 * {@code term} otherwise), {@code Is not} a {@code must_not} (T1070), and
 * {@code Exists} an {@code exists} query. A DATE condition {@code asc} boosts the
 * recent past on the curve Lucene and Solr use (a year halves it) and scores a
 * future date as today; {@code upcoming} (T1219) boosts today or later, nearest
 * most, and leaves the past unboosted. A condition that cannot be expressed drops
 * its whole expression, logged at debug, rather than widening it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
@Slf4j
@Component
public class TurElasticsearchRankingExpressions {

    private static final String ASC = "asc";
    private static final String UPCOMING = "upcoming";
    private static final String KEYWORD = ".keyword";

    /**
     * The pivot of Solr's {@code recip(x, 3.16e-11, 1, 1)}: a document this many
     * milliseconds old scores half of one from today, as on Lucene.
     */
    static final long RECENCY_PIVOT_MS = Math.round(1 / 3.16e-11);

    private final TurSESiteSearchConfig siteSearchConfig;

    public TurElasticsearchRankingExpressions(TurSESiteSearchConfig siteSearchConfig) {
        this.siteSearchConfig = siteSearchConfig;
    }

    /** The boost clauses for site {@code siteId}'s ranking expressions; empty when it has none. */
    public List<Query> boostQueries(String siteId) {
        return boostQueries(siteId, System.currentTimeMillis());
    }

    /** {@link #boostQueries(String)} as of {@code nowMs}. */
    public List<Query> boostQueries(String siteId, long nowMs) {
        List<TurSERankingExpression> expressions = siteSearchConfig.rankingExpressions(siteId);
        if (expressions == null || expressions.isEmpty()) {
            return List.of();
        }
        Map<String, TurSEFieldType> fieldTypes = siteSearchConfig.fieldTypes(siteId);
        // Solr's NOW/DAY: the start of the current UTC day.
        Instant origin = Instant.ofEpochMilli(nowMs).truncatedTo(ChronoUnit.DAYS);
        List<Query> boosts = new ArrayList<>();
        for (TurSERankingExpression expression : expressions) {
            Query boost = expressionQuery(expression, fieldTypes, origin);
            if (boost != null) {
                boosts.add(boost);
            }
        }
        return boosts;
    }

    private Query expressionQuery(TurSERankingExpression expression, Map<String, TurSEFieldType> fieldTypes,
            Instant origin) {
        List<TurSERankingCondition> conditions = expression.conditions();
        if (conditions.isEmpty()) {
            return null;
        }
        float weight = expression.weight();
        if (!(weight > 0) || Float.isInfinite(weight)) {
            log.debug("[Elasticsearch] ranking expression '{}' skipped: weight {} is not a positive boost",
                    expression.name(), weight);
            return null;
        }
        List<Query> all = new ArrayList<>();
        for (TurSERankingCondition condition : conditions) {
            Query query = conditionQuery(condition, fieldTypes.get(condition.attribute()), origin);
            if (query == null) {
                log.debug("[Elasticsearch] ranking expression '{}' skipped: condition {}:{} cannot be applied",
                        expression.name(), condition.attribute(), condition.value());
                return null;
            }
            all.add(query);
        }
        return Query.of(q -> q.bool(b -> b.must(all).boost(weight)));
    }

    static Query conditionQuery(TurSERankingCondition condition, TurSEFieldType type, Instant origin) {
        String field = condition.attribute();
        String value = condition.value();
        if (!StringUtils.hasText(field)) {
            return null;
        }
        if (condition.kind() == TurSERankingCondition.Kind.EXISTS) {
            return Query.of(q -> q.exists(e -> e.field(field)));
        }
        if (value == null) {
            return null;
        }
        boolean negated = condition.kind() == TurSERankingCondition.Kind.IS_NOT;
        boolean scoredDate = type == TurSEFieldType.DATE
                && (ASC.equalsIgnoreCase(value) || UPCOMING.equalsIgnoreCase(value));
        if (negated && scoredDate) {
            return null; // T1070 — Is not has no meaning on the recency or upcoming form
        }
        Query match = matchQuery(field, value, type, origin);
        if (match == null || !negated) {
            return match;
        }
        // T1070 — Is not: every document except the ones with that value.
        return Query.of(q -> q.bool(b -> b.must(m -> m.matchAll(a -> a)).mustNot(match)));
    }

    private static Query matchQuery(String field, String value, TurSEFieldType type, Instant origin) {
        if (type == TurSEFieldType.DATE) {
            return dateQuery(field, value, origin);
        }
        if (type == TurSEFieldType.TEXT) {
            return Query.of(q -> q.matchPhrase(m -> m.field(field).query(value)));
        }
        String target = TurElasticsearchFilterQueries.isKeyword(type) ? field + KEYWORD : field;
        return Query.of(q -> q.term(t -> t.field(target).value(FieldValue.of(value.trim()))));
    }

    private static Query dateQuery(String field, String value, Instant origin) {
        String from = origin.toString();
        if (ASC.equalsIgnoreCase(value)) {
            // The past on the recency curve; a future date scores as today (T1219).
            Query past = Query.of(q -> q.bool(b -> b.filter(range(field, null, from))
                    .must(distanceFeature(field, from))));
            Query future = Query.of(q -> q.constantScore(c -> c
                    .filter(range(field, origin.plusMillis(1).toString(), null)).boost(1f)));
            return Query.of(q -> q.bool(b -> b.should(past).should(future)));
        }
        if (UPCOMING.equalsIgnoreCase(value)) {
            return Query.of(q -> q.bool(b -> b.filter(range(field, from, null))
                    .must(distanceFeature(field, from))));
        }
        try {
            String exact = Instant.parse(value.trim()).toString();
            return Query.of(q -> q.term(t -> t.field(field).value(FieldValue.of(exact))));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** {@code distance_feature} from {@code origin}, scoring 1 at it and ½ a pivot away. */
    private static Query distanceFeature(String field, String origin) {
        return Query.of(q -> q.distanceFeature(d -> d.date(dd -> dd.field(field).origin(origin)
                .pivot(Time.of(t -> t.time(RECENCY_PIVOT_MS + "ms"))))));
    }

    /** An inclusive range on {@code field}; a {@code null} bound is open. */
    private static Query range(String field, String gte, String lte) {
        UntypedRangeQuery.Builder builder = new UntypedRangeQuery.Builder().field(field);
        if (gte != null) {
            builder.gte(JsonData.of(gte));
        }
        if (lte != null) {
            builder.lte(JsonData.of(lte));
        }
        UntypedRangeQuery untyped = builder.build();
        return Query.of(q -> q.range(r -> r.untyped(untyped)));
    }
}
