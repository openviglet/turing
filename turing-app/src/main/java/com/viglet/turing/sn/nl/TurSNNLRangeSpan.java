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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * T1211 — the words of a text a range bound was read from: the amount as the text
 * formats it ({@code 2.200}, {@code 1,200.00}), widened to its adjacent currency
 * ({@code $}, {@code R$}, {@code reais}) and to the bound phrase around it
 * ({@code under}, {@code até}, {@code or less}); a year-dated bound widened to its
 * date phrase ({@code from 2024}). Two bounds whose spans only a connector
 * separates ({@code between $100 and $200}) become one span. The phrase lists
 * cover English, Portuguese and Spanish, compared without case or accents.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
final class TurSNNLRangeSpan {

    /** A span of the original text, {@code end} exclusive. */
    record Span(int start, int end) {
        boolean touches(Span other) {
            return start <= other.end && other.start <= end;
        }
    }

    private static final Pattern NUMBER = Pattern.compile("\\d(?:[\\d.,]*\\d)?");

    private static final List<String> CURRENCY_BEFORE = longestFirst("us$", "r$", "$", "€", "£", "¥", "usd",
            "brl", "eur", "gbp");
    private static final List<String> CURRENCY_AFTER = longestFirst("reais", "real", "dollars", "dollar",
            "dolares", "dolar", "bucks", "euros", "euro", "usd", "brl", "eur", "gbp", "€", "$");
    private static final List<String> AMOUNT_BEFORE = longestFirst(
            // en
            "less than", "lower than", "cheaper than", "no more than", "not more than", "more than",
            "greater than", "higher than", "at most", "at least", "up to", "under", "below", "over", "above",
            "between", "from", "max", "maximum", "min", "minimum", "within", "around", "about",
            // pt
            "menos de", "mais de", "acima de", "abaixo de", "no maximo", "no minimo", "ate", "por ate", "entre",
            "a partir de", "desde", "de", "maior que", "menor que", "inferior a", "superior a", "por menos de",
            "por volta de", "cerca de",
            // es
            "hasta", "mas de", "por debajo de", "por encima de", "alrededor de");
    private static final List<String> AMOUNT_AFTER = longestFirst("or less", "or more", "or under", "or below",
            "or above", "or cheaper", "and under", "and below", "and up", "and above", "max", "tops", "ou menos",
            "ou mais", "no maximo", "no minimo", "o menos", "o mas");
    private static final List<String> DATE_BEFORE = longestFirst("from", "since", "before", "after", "until",
            "till", "in", "during", "between", "of", "desde", "de", "do ano de", "em", "no ano de", "entre",
            "antes de", "depois de", "ate", "a partir de", "durante", "hasta", "en", "despues de");
    private static final List<String> DATE_AFTER = longestFirst("onwards", "onward", "and later", "or later",
            "and earlier", "or earlier", "or before", "or after", "em diante", "para frente", "pra frente",
            "en adelante");
    private static final Set<String> CONNECTORS = Set.of("and", "to", "through", "-", "–", "e", "a", "y",
            "ate", "hasta");

    private TurSNNLRangeSpan() {
    }

    /**
     * The span a numeric bound was read from, preferring an occurrence a bound phrase
     * or currency marks over a bare number elsewhere in the text; a bound that is not
     * a number is matched as written.
     */
    static Optional<Span> amount(String text, String bound) {
        BigDecimal wanted = number(bound);
        if (wanted == null) {
            return literal(TurSNNLFilterService.Folded.of(text), TurSNNLFilterService.normalize(bound))
                    .stream().findFirst().map(at -> toOriginal(TurSNNLFilterService.Folded.of(text), at));
        }
        TurSNNLFilterService.Folded folded = TurSNNLFilterService.Folded.of(text);
        Matcher matcher = NUMBER.matcher(folded.text());
        Span best = null;
        int bestScore = -1;
        while (matcher.find()) {
            if (!boundary(folded.text(), matcher.start() - 1) || !boundary(folded.text(), matcher.end())
                    || readings(matcher.group()).stream().noneMatch(r -> r.compareTo(wanted) == 0)) {
                continue;
            }
            int[] score = new int[1];
            Span widened = widen(folded.text(), new Span(matcher.start(), matcher.end()), true, score);
            if (score[0] > bestScore) {
                best = widened;
                bestScore = score[0];
            }
        }
        return Optional.ofNullable(best).map(span -> toOriginal(folded, span));
    }

    /**
     * The span a date bound was read from: the bound as written, else the year it
     * starts with, widened to its date phrase.
     */
    static Optional<Span> dated(String text, String bound) {
        TurSNNLFilterService.Folded folded = TurSNNLFilterService.Folded.of(text);
        List<Span> found = literal(folded, TurSNNLFilterService.normalize(bound));
        if (found.isEmpty() && bound.length() >= 4) {
            found = literal(folded, bound.substring(0, 4));
        }
        Span best = null;
        int bestScore = -1;
        for (Span at : found) {
            int[] score = new int[1];
            Span widened = widen(folded.text(), at, false, score);
            if (score[0] > bestScore) {
                best = widened;
                bestScore = score[0];
            }
        }
        return Optional.ofNullable(best).map(span -> toOriginal(folded, span));
    }

    /** The words of each span, two spans only a connector separates read as one. */
    static List<String> words(String text, List<Span> spans) {
        List<Span> sorted = new ArrayList<>(spans);
        sorted.sort(Comparator.comparingInt(Span::start));
        List<Span> merged = new ArrayList<>();
        for (Span span : sorted) {
            Span last = merged.isEmpty() ? null : merged.get(merged.size() - 1);
            if (last != null && (last.touches(span) || connected(text, last, span))) {
                merged.set(merged.size() - 1, new Span(last.start(), Math.max(last.end(), span.end())));
            } else {
                merged.add(span);
            }
        }
        Set<String> words = new LinkedHashSet<>();
        merged.forEach(span -> words.add(text.substring(span.start(), span.end())));
        return List.copyOf(words);
    }

    private static boolean connected(String text, Span left, Span right) {
        String gap = TurSNNLFilterService.normalize(text.substring(left.end(), right.start()));
        return gap.isEmpty() || CONNECTORS.contains(gap);
    }

    /**
     * Widens {@code at} to the currency and bound phrase around it, in folded
     * coordinates; {@code score} counts a phrase as two and a currency as one.
     */
    private static Span widen(String folded, Span at, boolean amount, int[] score) {
        int start = at.start();
        int end = at.end();
        if (amount) {
            int before = endsWithToken(folded, start, CURRENCY_BEFORE);
            if (before >= 0) {
                start = before;
                score[0] += 1;
            }
            int after = startsWithToken(folded, end, CURRENCY_AFTER);
            if (after >= 0) {
                end = after;
                score[0] += 1;
            }
        }
        int phrase = endsWithToken(folded, start, amount ? AMOUNT_BEFORE : DATE_BEFORE);
        if (phrase >= 0) {
            start = phrase;
            score[0] += 2;
        }
        int trailing = startsWithToken(folded, end, amount ? AMOUNT_AFTER : DATE_AFTER);
        if (trailing >= 0) {
            end = trailing;
            score[0] += 2;
        }
        return new Span(start, end);
    }

    /** Where a token ending just before {@code index} (spaces allowed between) starts, or -1. */
    private static int endsWithToken(String folded, int index, List<String> tokens) {
        int p = index;
        while (p > 0 && folded.charAt(p - 1) == ' ') {
            p--;
        }
        for (String token : tokens) {
            int from = p - token.length();
            if (from >= 0 && folded.startsWith(token, from)
                    && (!Character.isLetterOrDigit(token.charAt(0)) || boundary(folded, from - 1))
                    && (p < index || !Character.isLetterOrDigit(token.charAt(token.length() - 1))
                            || boundary(folded, p))) {
                return from;
            }
        }
        return -1;
    }

    /** Where a token starting just after {@code index} (spaces allowed between) ends, or -1. */
    private static int startsWithToken(String folded, int index, List<String> tokens) {
        int p = index;
        while (p < folded.length() && folded.charAt(p) == ' ') {
            p++;
        }
        for (String token : tokens) {
            int to = p + token.length();
            if (to <= folded.length() && folded.startsWith(token, p)
                    && (!Character.isLetterOrDigit(token.charAt(token.length() - 1)) || boundary(folded, to))
                    && (p > index || !Character.isLetterOrDigit(token.charAt(0)) || boundary(folded, p - 1))) {
                return to;
            }
        }
        return -1;
    }

    private static List<Span> literal(TurSNNLFilterService.Folded folded, String wanted) {
        List<Span> found = new ArrayList<>();
        if (wanted.isEmpty()) {
            return found;
        }
        for (int at = folded.text().indexOf(wanted); at >= 0; at = folded.text().indexOf(wanted, at + 1)) {
            int end = at + wanted.length();
            if (boundary(folded.text(), at - 1) && boundary(folded.text(), end)) {
                found.add(new Span(at, end));
            }
        }
        return found;
    }

    private static Span toOriginal(TurSNNLFilterService.Folded folded, Span span) {
        return new Span(folded.origin()[span.start()], folded.origin()[span.end() - 1] + 1);
    }

    private static boolean boundary(String text, int index) {
        return index < 0 || index >= text.length() || !Character.isLetterOrDigit(text.charAt(index));
    }

    private static BigDecimal number(String bound) {
        try {
            return bound == null ? null : new BigDecimal(bound.trim());
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }

    /** What a formatted number can mean: dot or comma as the decimal separator. */
    static Set<BigDecimal> readings(String token) {
        Set<BigDecimal> readings = new LinkedHashSet<>();
        reading(token, '.', ',').ifPresent(readings::add);
        reading(token, ',', '.').ifPresent(readings::add);
        return readings;
    }

    private static Optional<BigDecimal> reading(String token, char group, char decimal) {
        int dec = token.lastIndexOf(decimal);
        String integer = dec >= 0 ? token.substring(0, dec) : token;
        String fraction = dec >= 0 ? token.substring(dec + 1) : "";
        if (integer.indexOf(decimal) >= 0 || fraction.indexOf(group) >= 0 || integer.isEmpty()) {
            return Optional.empty();
        }
        String[] groups = integer.split(Pattern.quote(String.valueOf(group)), -1);
        if (groups.length > 1) {
            if (groups[0].isEmpty() || groups[0].length() > 3) {
                return Optional.empty();
            }
            for (int i = 1; i < groups.length; i++) {
                if (groups[i].length() != 3) {
                    return Optional.empty();
                }
            }
        }
        String digits = String.join("", groups) + (fraction.isEmpty() ? "" : "." + fraction);
        try {
            return Optional.of(new BigDecimal(digits));
        } catch (NumberFormatException notANumber) {
            return Optional.empty();
        }
    }

    private static List<String> longestFirst(String... tokens) {
        List<String> list = new ArrayList<>(List.of(tokens));
        list.sort(Comparator.comparingInt(String::length).reversed());
        return List.copyOf(list);
    }
}
