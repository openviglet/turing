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
package com.viglet.turing.searchengine;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Solr date math, read the same way by every engine (T1310). A filter or a custom
 * facet item spells a date as Solr does whichever engine serves the site, so the
 * Lucene and Elasticsearch translators and the SN custom-facet renderer all parse
 * it here rather than reaching into one engine's package.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public final class TurSEDateMath {

    private static final Pattern DATE_MATH_STEP = Pattern.compile("([+-]\\d+|/)([A-Za-z]+)");

    private TurSEDateMath() {
    }

    /**
     * Parses a date with Solr date math, in UTC. The anchor is {@code NOW}, an
     * ISO instant or a {@code yyyy-MM-dd} day; each following step adds or
     * subtracts units ({@code -7DAYS}) or rounds down to one ({@code /DAY}).
     *
     * @throws DateTimeParseException when the anchor or a step does not parse
     */
    public static Instant parse(String value, Instant now) {
        String upper = value.toUpperCase(Locale.ROOT);
        ZonedDateTime anchor;
        String math;
        if (upper.startsWith("NOW")) {
            anchor = now.atZone(ZoneOffset.UTC);
            math = value.substring(3);
        } else {
            int split = mathStart(value);
            String base = value.substring(0, split);
            anchor = parseAnchor(base);
            math = value.substring(split);
        }
        return applyMath(anchor, math).toInstant();
    }

    /** Where date math begins after an ISO anchor: the first {@code /}, or a sign after its 'Z'. */
    private static int mathStart(String value) {
        int z = value.toUpperCase(Locale.ROOT).indexOf('Z');
        int from = z >= 0 ? z + 1 : Math.min(value.length(), 10);
        for (int i = from; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '+' || c == '-' || c == '/') {
                return i;
            }
        }
        return value.length();
    }

    private static ZonedDateTime parseAnchor(String base) {
        try {
            return Instant.parse(base).atZone(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            return LocalDate.parse(base).atStartOfDay(ZoneOffset.UTC);
        }
    }

    private static ZonedDateTime applyMath(ZonedDateTime anchor, String math) {
        ZonedDateTime result = anchor;
        int position = 0;
        Matcher step = DATE_MATH_STEP.matcher(math);
        while (position < math.length()) {
            if (!step.find(position) || step.start() != position) {
                throw new DateTimeParseException("Unparseable date math", math, position);
            }
            ChronoUnit unit = unit(step.group(2), math, position);
            result = "/".equals(step.group(1))
                    ? round(result, unit)
                    : result.plus(Long.parseLong(step.group(1)), unit);
            position = step.end();
        }
        return result;
    }

    private static ChronoUnit unit(String name, String math, int position) {
        return switch (name.toUpperCase(Locale.ROOT)) {
            case "YEAR", "YEARS" -> ChronoUnit.YEARS;
            case "MONTH", "MONTHS" -> ChronoUnit.MONTHS;
            case "DAY", "DAYS", "DATE" -> ChronoUnit.DAYS;
            case "HOUR", "HOURS" -> ChronoUnit.HOURS;
            case "MINUTE", "MINUTES" -> ChronoUnit.MINUTES;
            case "SECOND", "SECONDS" -> ChronoUnit.SECONDS;
            case "MILLI", "MILLIS", "MILLISECOND", "MILLISECONDS" -> ChronoUnit.MILLIS;
            default -> throw new DateTimeParseException("Unknown date math unit " + name, math, position);
        };
    }

    private static ZonedDateTime round(ZonedDateTime value, ChronoUnit unit) {
        return switch (unit) {
            case YEARS -> value.truncatedTo(ChronoUnit.DAYS).withDayOfYear(1);
            case MONTHS -> value.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1);
            default -> value.truncatedTo(unit);
        };
    }
}
