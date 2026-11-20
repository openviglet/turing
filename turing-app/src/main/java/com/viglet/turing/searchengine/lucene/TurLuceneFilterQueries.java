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
package com.viglet.turing.searchengine.lucene;

import java.time.Instant;



import java.time.format.DateTimeParseException;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;

import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.searchengine.TurSEDateMath;

/**
 * Turns the value of one {@code field:value} filter query into a Lucene query
 * that matches how {@link TurLuceneDocumentHandler} indexed the field (T1075).
 * Numeric and date fields are indexed as points, so a plain {@link TermQuery}
 * never matches them; this reads the value by the field's type instead.
 *
 * <p>
 * Accepts Solr's range syntax, {@code [from TO to]} with {@code {}} for an
 * exclusive bound and {@code *} for an open one, and on DATE fields Solr date
 * math: {@code NOW}, an ISO instant or a {@code yyyy-MM-dd} day, followed by
 * {@code +n}/{@code -n} steps and {@code /UNIT} rounding (e.g.
 * {@code NOW-7DAYS/DAY}). An exact value that does not parse for its type
 * falls back to a term lookup, which is what a value the handler could not
 * parse was indexed as; a range that does not parse is refused with an
 * {@link IllegalArgumentException} that says why.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
public final class TurLuceneFilterQueries {

    private static final Pattern RANGE = Pattern.compile("^([\\[{])\\s*(\\S+)\\s+TO\\s+(\\S+)\\s*([]}])$");
    private static final String OPEN = "*";

    private TurLuceneFilterQueries() {
    }

    /**
     * @param field the filtered field
     * @param value the filter's value, unquoted
     * @param type  the field's type, or {@code null} when the field is unknown
     * @param now   the instant {@code NOW} resolves to
     */
    public static Query build(String field, String value, TurSEFieldType type, Instant now) {
        Matcher range = RANGE.matcher(value.trim());
        if (range.matches()) {
            boolean lowerInclusive = "[".equals(range.group(1));
            boolean upperInclusive = "]".equals(range.group(4));
            Query query = rangeQuery(field, type, range.group(2), lowerInclusive, range.group(3),
                    upperInclusive, now);
            if (query == null) {
                // Refused rather than matching nothing, so the caller learns why the
                // page is empty (TurGlobalExceptionHandler answers 400 with this text).
                throw new IllegalArgumentException(String.format(
                        "Filter query '%s:%s' is not a range of %s values: use [from TO to] "
                                + "with numbers%s, '*' for an open end and {} for an exclusive one",
                        field, value, type, type == TurSEFieldType.DATE
                                ? " or dates (ISO instant, yyyy-MM-dd, NOW with date math such as NOW-7DAYS)"
                                : ""));
            }
            return query;
        }
        Query exact = exactQuery(field, value, type, now);
        return exact != null ? exact : new TermQuery(new Term(field, value));
    }

    private static Query exactQuery(String field, String value, TurSEFieldType type, Instant now) {
        if (type == null) {
            return null;
        }
        try {
            String trimmed = value.trim();
            return switch (type) {
                case INT -> IntPoint.newExactQuery(field, Integer.parseInt(trimmed));
                case LONG -> LongPoint.newExactQuery(field, Long.parseLong(trimmed));
                case FLOAT -> FloatPoint.newExactQuery(field, Float.parseFloat(trimmed));
                case DOUBLE, CURRENCY -> DoublePoint.newExactQuery(field, Double.parseDouble(trimmed));
                case DATE -> LongPoint.newExactQuery(field, TurSEDateMath.parse(trimmed, now).toEpochMilli());
                default -> null;
            };
        } catch (NumberFormatException | DateTimeParseException e) {
            return null;
        }
    }

    private static Query rangeQuery(String field, TurSEFieldType type, String from, boolean fromInclusive,
            String to, boolean toInclusive, Instant now) {
        try {
            if (type == null) {
                return termRange(field, from, fromInclusive, to, toInclusive);
            }
            return switch (type) {
                case INT -> IntPoint.newRangeQuery(field,
                        lowerInt(from, fromInclusive), upperInt(to, toInclusive));
                case LONG -> LongPoint.newRangeQuery(field,
                        lowerLong(OPEN.equals(from) ? null : Long.parseLong(from), fromInclusive),
                        upperLong(OPEN.equals(to) ? null : Long.parseLong(to), toInclusive));
                case DATE -> LongPoint.newRangeQuery(field,
                        lowerLong(OPEN.equals(from) ? null : TurSEDateMath.parse(from, now).toEpochMilli(), fromInclusive),
                        upperLong(OPEN.equals(to) ? null : TurSEDateMath.parse(to, now).toEpochMilli(), toInclusive));
                case FLOAT -> FloatPoint.newRangeQuery(field,
                        lowerFloat(from, fromInclusive), upperFloat(to, toInclusive));
                case DOUBLE, CURRENCY -> DoublePoint.newRangeQuery(field,
                        lowerDouble(from, fromInclusive), upperDouble(to, toInclusive));
                default -> termRange(field, from, fromInclusive, to, toInclusive);
            };
        } catch (NumberFormatException | DateTimeParseException | ArithmeticException e) {
            return null;
        }
    }

    private static Query termRange(String field, String from, boolean fromInclusive, String to,
            boolean toInclusive) {
        return TermRangeQuery.newStringRange(field, OPEN.equals(from) ? null : from,
                OPEN.equals(to) ? null : to, fromInclusive, toInclusive);
    }

    private static int lowerInt(String bound, boolean inclusive) {
        if (OPEN.equals(bound)) return Integer.MIN_VALUE;
        int value = Integer.parseInt(bound);
        return inclusive ? value : Math.addExact(value, 1);
    }

    private static int upperInt(String bound, boolean inclusive) {
        if (OPEN.equals(bound)) return Integer.MAX_VALUE;
        int value = Integer.parseInt(bound);
        return inclusive ? value : Math.subtractExact(value, 1);
    }

    private static long lowerLong(Long value, boolean inclusive) {
        if (value == null) return Long.MIN_VALUE;
        return inclusive ? value : Math.addExact(value, 1L);
    }

    private static long upperLong(Long value, boolean inclusive) {
        if (value == null) return Long.MAX_VALUE;
        return inclusive ? value : Math.subtractExact(value, 1L);
    }

    private static float lowerFloat(String bound, boolean inclusive) {
        if (OPEN.equals(bound)) return Float.NEGATIVE_INFINITY;
        float value = Float.parseFloat(bound);
        return inclusive ? value : Math.nextUp(value);
    }

    private static float upperFloat(String bound, boolean inclusive) {
        if (OPEN.equals(bound)) return Float.POSITIVE_INFINITY;
        float value = Float.parseFloat(bound);
        return inclusive ? value : Math.nextDown(value);
    }

    private static double lowerDouble(String bound, boolean inclusive) {
        if (OPEN.equals(bound)) return Double.NEGATIVE_INFINITY;
        double value = Double.parseDouble(bound);
        return inclusive ? value : Math.nextUp(value);
    }

    private static double upperDouble(String bound, boolean inclusive) {
        if (OPEN.equals(bound)) return Double.POSITIVE_INFINITY;
        double value = Double.parseDouble(bound);
        return inclusive ? value : Math.nextDown(value);
    }
}
