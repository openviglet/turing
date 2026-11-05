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
package com.viglet.turing.sn.facet;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteCustomFacetItem;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteCustomFacetOperatorEnum;

/**
 * T1083 — the filter query a custom facet item stands for, in Solr's range
 * syntax ({@code field:[start TO end]}), which both the Solr engine and, since
 * T1075, the Lucene engine read. Built in one place so one item cannot mean two
 * ranges on two engines.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
public final class TurSNCustomFacetFilterQuery {

    private static final String OPEN = "*";

    private TurSNCustomFacetFilterQuery() {
    }

    /** The filter query for {@code item} over the field it belongs to. */
    public static String of(String fieldName, TurSEFieldType fieldType, TurSNSiteCustomFacetItem item) {
        TurSNSiteCustomFacetOperatorEnum operator = Optional.ofNullable(item.getOperator())
                .orElse(TurSNSiteCustomFacetOperatorEnum.BETWEEN_INCLUSIVE);
        if (TurSEFieldType.DATE.equals(fieldType)) {
            // T1075 — a relative bound (NOW-7DAYS) wins over a fixed instant
            return byOperator(fieldName, operator,
                    dateBound(item.getRangeStartExpression(), item.getRangeStartDate()),
                    dateBound(item.getRangeEndExpression(), item.getRangeEndDate()));
        }
        return byOperator(fieldName, operator,
                Optional.ofNullable(item.getRangeStart()).map(TurSNCustomFacetFilterQuery::plain).orElse(null),
                Optional.ofNullable(item.getRangeEnd()).map(TurSNCustomFacetFilterQuery::plain).orElse(null));
    }

    /** T1224 — one condition of a span filter: {@code field} holds {@code value}, a range. */
    public record Clause(String field, String value) {
    }

    /**
     * T1224 — an item's filter over a span that starts at {@code field} and ends at
     * {@code endField}: a document with no end value is the point at its start and
     * matches {@code point} on {@code field}, as without an end field; a document
     * with one matches when every {@code overlap} clause holds.
     */
    public record Span(String field, String endField, String point, List<Clause> overlap) {
    }

    /**
     * The filter query for {@code item} over a span (T1224), in Solr syntax; with
     * no {@code endField} it is {@link #of(String, TurSEFieldType, TurSNSiteCustomFacetItem)}.
     */
    public static String of(String fieldName, String endField, TurSEFieldType fieldType,
            TurSNSiteCustomFacetItem item) {
        if (endField == null || endField.isBlank()) {
            return of(fieldName, fieldType, item);
        }
        Span span = span(fieldName, endField, fieldType, item);
        String overlap = span.overlap().stream()
                .map(clause -> "+" + clause.field() + ":" + clause.value())
                .collect(Collectors.joining(" "));
        return String.format("((+%s:%s -%s:[* TO *]) OR (%s))", span.field(), span.point(),
                span.endField(), overlap);
    }

    /**
     * T1224 — the parts of an item's span filter. A span overlaps an item when it
     * starts by the item's end and ends from the item's start. An item open at its
     * start ("Past") tests the end alone, so it holds what has ended by its end
     * rather than everything that started before it.
     */
    public static Span span(String fieldName, String endField, TurSEFieldType fieldType,
            TurSNSiteCustomFacetItem item) {
        String fq = of(fieldName, fieldType, item);
        String point = fq.substring(fq.indexOf(':') + 1);
        Bounds bounds = bounds(fieldType, item);
        List<Clause> overlap = new ArrayList<>();
        if (bounds.low() == null) {
            overlap.add(new Clause(endField, upTo(bounds)));
            return new Span(fieldName, endField, point, overlap);
        }
        if (bounds.high() != null) {
            overlap.add(new Clause(fieldName, upTo(bounds)));
        }
        overlap.add(new Clause(endField, (bounds.lowInclusive() ? "[" : "{") + bounds.low() + " TO *]"));
        return new Span(fieldName, endField, point, overlap);
    }

    /** The range from anywhere up to the item's end, open when the item has none. */
    private static String upTo(Bounds bounds) {
        if (bounds.high() == null) {
            return "[* TO *]";
        }
        return "[* TO " + bounds.high() + (bounds.highInclusive() ? "]" : "}");
    }

    /** An item's two bounds by operator; {@code null} is open. */
    private record Bounds(String low, boolean lowInclusive, String high, boolean highInclusive) {
    }

    private static Bounds bounds(TurSEFieldType fieldType, TurSNSiteCustomFacetItem item) {
        TurSNSiteCustomFacetOperatorEnum operator = Optional.ofNullable(item.getOperator())
                .orElse(TurSNSiteCustomFacetOperatorEnum.BETWEEN_INCLUSIVE);
        String start;
        String end;
        if (TurSEFieldType.DATE.equals(fieldType)) {
            start = dateBound(item.getRangeStartExpression(), item.getRangeStartDate());
            end = dateBound(item.getRangeEndExpression(), item.getRangeEndDate());
        } else {
            start = Optional.ofNullable(item.getRangeStart()).map(TurSNCustomFacetFilterQuery::plain).orElse(null);
            end = Optional.ofNullable(item.getRangeEnd()).map(TurSNCustomFacetFilterQuery::plain).orElse(null);
        }
        return switch (operator) {
            case EQUAL -> {
                String value = start != null ? start : end;
                yield new Bounds(value, true, value, true);
            }
            case GREATER_THAN_OR_EQUAL -> new Bounds(start, true, null, true);
            case GREATER_THAN -> new Bounds(start, false, null, true);
            case LESS_THAN_OR_EQUAL -> new Bounds(null, true, end, true);
            case LESS_THAN -> new Bounds(null, true, end, false);
            case BETWEEN_INCLUSIVE -> new Bounds(start, true, end, true);
            case BETWEEN_EXCLUSIVE -> new Bounds(start, false, end, false);
        };
    }

    /**
     * T1122 — a bound as the number it is, without the column's scale: the stored
     * value reads back as {@code 20.000000}, which an INT or LONG range cannot
     * parse, so every bucket of a stored integer facet was skipped and the facet
     * never reached the answer.
     */
    private static String plain(BigDecimal bound) {
        return bound.signum() == 0 ? "0" : bound.stripTrailingZeros().toPlainString();
    }

    private static String dateBound(String expression, Instant instant) {
        if (expression != null && !expression.isBlank()) {
            return expression.trim();
        }
        return instant != null ? instant.toString() : null;
    }

    /** Whether {@code value} is Solr date math anchored on NOW, rather than a date. */
    public static boolean isDateMath(String value) {
        return value != null && value.trim().toUpperCase(java.util.Locale.ROOT).startsWith("NOW");
    }

    /** The filter query for an operator and its bounds; a missing bound is open. */
    public static String byOperator(String fieldName, TurSNSiteCustomFacetOperatorEnum operator,
            String startValue, String endValue) {
        String start = startValue != null ? startValue : OPEN;
        String end = endValue != null ? endValue : OPEN;
        return switch (operator) {
            case EQUAL -> String.format("%s:%s", fieldName,
                    startValue != null ? startValue : end);
            case GREATER_THAN_OR_EQUAL -> String.format("%s:[%s TO *]", fieldName, start);
            case GREATER_THAN -> String.format("%s:{%s TO *]", fieldName, start);
            case LESS_THAN_OR_EQUAL -> String.format("%s:[* TO %s]", fieldName, end);
            case LESS_THAN -> String.format("%s:[* TO %s}", fieldName, end);
            case BETWEEN_INCLUSIVE -> String.format("%s:[%s TO %s]", fieldName, start, end);
            case BETWEEN_EXCLUSIVE -> String.format("%s:{%s TO %s}", fieldName, start, end);
        };
    }
}
