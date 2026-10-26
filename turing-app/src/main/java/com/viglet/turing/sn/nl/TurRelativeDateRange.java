/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.viglet.turing.sn.nl;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The relative date ranges a visitor spells, resolved in code against today
 * (T1117). A model asked for "last year" fills the range from its own training
 * date; these phrases are exact and cost nothing, so they never reach it.
 *
 * <p>English and Portuguese: last/this year and month, last week, the last N
 * days, weeks or months, and since a month or a year. Both bounds are
 * inclusive dates.
 *
 * @param phrase the words of the text that name the range
 * @param start  the first day of the range
 * @param end    the last day of the range
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
public record TurRelativeDateRange(String phrase, LocalDate start, LocalDate end) {

    private static final Map<String, Integer> MONTHS = months();
    private static final String MONTH_NAMES = String.join("|", MONTHS.keySet());

    private static final Pattern LAST_YEAR = word("last year|past year|no ano passado|do ano passado|ano passado");
    private static final Pattern THIS_YEAR = word("this year|neste ano|deste ano|este ano|esse ano");
    private static final Pattern LAST_MONTH = word("last month|no mes passado|do mes passado|mes passado");
    private static final Pattern THIS_MONTH = word("this month|neste mes|deste mes|este mes|esse mes");
    private static final Pattern LAST_WEEK = word("last week|past week|semana passada|na semana passada");
    private static final Pattern LAST_N = word(
            "(?:last|past|ultimos|ultimas)\\s+(\\d{1,3})\\s+(days?|weeks?|months?|dias|semanas|meses)");
    private static final Pattern SINCE_MONTH = word("(?:since|desde)\\s+(?:de\\s+)?(" + MONTH_NAMES + ")");
    private static final Pattern SINCE_YEAR = word("(?:since|desde)\\s+((?:19|20)\\d{2})");

    /** The first relative range {@code text} spells, resolved against {@code today}. */
    public static Optional<TurRelativeDateRange> resolve(String text, LocalDate today) {
        if (text == null || text.isBlank() || today == null) {
            return Optional.empty();
        }
        Folded folded = Folded.of(text);
        Matcher m;
        if ((m = LAST_N.matcher(folded.text())).find()) {
            int n = Integer.parseInt(m.group(1));
            String unit = m.group(2);
            LocalDate start = unit.startsWith("day") || unit.startsWith("dia") ? today.minusDays(n)
                    : unit.startsWith("week") || unit.startsWith("semana") ? today.minusWeeks(n)
                    : today.minusMonths(n);
            return Optional.of(of(folded, m, start, today));
        }
        if ((m = SINCE_MONTH.matcher(folded.text())).find()) {
            int month = MONTHS.get(m.group(1));
            int year = month <= today.getMonthValue() ? today.getYear() : today.getYear() - 1;
            return Optional.of(of(folded, m, LocalDate.of(year, month, 1), today));
        }
        if ((m = SINCE_YEAR.matcher(folded.text())).find()) {
            return Optional.of(of(folded, m, LocalDate.of(Integer.parseInt(m.group(1)), 1, 1), today));
        }
        if ((m = LAST_YEAR.matcher(folded.text())).find()) {
            int year = today.getYear() - 1;
            return Optional.of(of(folded, m, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)));
        }
        if ((m = THIS_YEAR.matcher(folded.text())).find()) {
            return Optional.of(of(folded, m, today.withDayOfYear(1), today));
        }
        if ((m = LAST_MONTH.matcher(folded.text())).find()) {
            LocalDate previous = today.minusMonths(1);
            return Optional.of(of(folded, m, previous.withDayOfMonth(1),
                    previous.with(TemporalAdjusters.lastDayOfMonth())));
        }
        if ((m = THIS_MONTH.matcher(folded.text())).find()) {
            return Optional.of(of(folded, m, today.withDayOfMonth(1), today));
        }
        if ((m = LAST_WEEK.matcher(folded.text())).find()) {
            return Optional.of(of(folded, m, today.minusWeeks(1), today));
        }
        return Optional.empty();
    }

    private static TurRelativeDateRange of(Folded folded, Matcher m, LocalDate start, LocalDate end) {
        return new TurRelativeDateRange(folded.original(m.start(), m.end()), start, end);
    }

    private static Pattern word(String alternatives) {
        return Pattern.compile("(?<![\\p{L}\\p{N}])(?:" + alternatives + ")(?![\\p{L}\\p{N}])");
    }

    private static Map<String, Integer> months() {
        Map<String, Integer> months = new LinkedHashMap<>();
        String[] en = { "january", "february", "march", "april", "may", "june", "july", "august",
                "september", "october", "november", "december" };
        String[] pt = { "janeiro", "fevereiro", "marco", "abril", "maio", "junho", "julho", "agosto",
                "setembro", "outubro", "novembro", "dezembro" };
        for (int i = 0; i < 12; i++) {
            months.put(en[i], i + 1);
            months.put(pt[i], i + 1);
        }
        return months;
    }

    /** Lower case without accents, keeping each folded character's index in the original. */
    private record Folded(String text, String source, int[] origin) {

        static Folded of(String original) {
            StringBuilder folded = new StringBuilder();
            int[] origin = new int[original.length() * 2 + 1];
            int n = 0;
            for (int i = 0; i < original.length(); i++) {
                String decomposed = Normalizer.normalize(String.valueOf(original.charAt(i)), Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
                for (int j = 0; j < decomposed.length(); j++) {
                    if (n == origin.length) {
                        origin = java.util.Arrays.copyOf(origin, n * 2);
                    }
                    folded.append(decomposed.charAt(j));
                    origin[n++] = i;
                }
            }
            return new Folded(folded.toString(), original, java.util.Arrays.copyOf(origin, n));
        }

        String original(int start, int end) {
            return source.substring(origin[start], origin[end - 1] + 1);
        }
    }
}
