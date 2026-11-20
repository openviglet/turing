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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.CharsRefBuilder;

import com.viglet.turing.searchengine.TurSNSynonymType;
import com.viglet.turing.searchengine.TurSESynonymRule;

/**
 * T665 / §XXXIX (Block AP) — pure mapping of Turing's engine-neutral synonym
 * rules onto a Lucene {@link SynonymMap} for a query-time
 * {@code SynonymGraphFilter} over the embedded store. Regular (bidirectional)
 * and one-way rules are natural; corrections degrade to one-way with a warning
 * (Lucene's filter has no per-typo budget); placeholders exceed the filter's
 * model and are surfaced as unsupported — the honest "Lucene limits" caveat —
 * rather than silently dropped.
 *
 * <p>Terms are lowercased so they match the {@code StandardAnalyzer}-lowercased
 * tokens the index was written with. Multi-word phrases (e.g. "galaxy tab") are
 * joined with {@link SynonymMap#WORD_SEPARATOR} via
 * {@link SynonymMap.Builder#join}.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
public final class TurLuceneSynonymMapBuilder {

    private TurLuceneSynonymMapBuilder() {
    }

    public static TurLuceneSynonymMap build(List<TurSESynonymRule> rules) {
        return build(rules, null);
    }

    /**
     * T1304 — as {@link #build(List)}, for a core in {@code locale}'s language: a
     * multi-word key is added also without that language's stop words, the way the
     * query analyzer hands it to the synonym filter (stop words dropped first), so
     * "fundação escola de sociologia e política" fires on the words a search keeps.
     * The outputs stay as written: the index keeps stop words, and a phrase must line
     * up with them. The query builder keeps the words typed as a way in of their own
     * (synonyms only add), so a key that fires never costs the page holding the words.
     */
    public static TurLuceneSynonymMap build(List<TurSESynonymRule> rules, Locale locale) {
        org.apache.lucene.analysis.CharArraySet stopWords = locale == null
                ? org.apache.lucene.analysis.CharArraySet.EMPTY_SET
                : com.viglet.turing.searchengine.lucene.TurLuceneFoldingAnalyzer.stopWordsFor(locale);
        return buildMap(rules, stopWords);
    }

    /** One rule, and its key without stop words when that differs and is still several words. */
    private static void add(SynonymMap.Builder builder, String from, String to,
            org.apache.lucene.analysis.CharArraySet stopWords) {
        builder.add(charsRef(from), charsRef(to), true);
        if (stopWords.isEmpty() || !from.contains(" ")) {
            return;
        }
        String kept = java.util.Arrays.stream(from.split("\\s+"))
                .filter(word -> !stopWords.contains(word))
                .collect(java.util.stream.Collectors.joining(" "));
        if (!kept.isEmpty() && !kept.equals(from) && !kept.equals(to)) {
            builder.add(charsRef(kept), charsRef(to), true);
        }
    }

    private static TurLuceneSynonymMap buildMap(List<TurSESynonymRule> rules,
            org.apache.lucene.analysis.CharArraySet stopWords) {
        SynonymMap.Builder builder = new SynonymMap.Builder(true);
        Set<TurSNSynonymType> unsupported = EnumSet.noneOf(TurSNSynonymType.class);
        List<String> warnings = new ArrayList<>();
        int applied = 0;

        if (rules != null) {
            for (TurSESynonymRule rule : rules) {
                if (rule == null || rule.type() == null) {
                    continue;
                }
                boolean contributed = switch (rule.type()) {
                    case REGULAR -> addRegular(builder, rule.terms(), stopWords);
                    case ONE_WAY -> addDirectional(builder, rule.input(), rule.terms(), stopWords);
                    case ALTERNATIVE_CORRECTION_1, ALTERNATIVE_CORRECTION_2 ->
                            addCorrection(builder, warnings, rule, stopWords);
                    case PLACEHOLDER -> {
                        unsupported.add(TurSNSynonymType.PLACEHOLDER);
                        warnings.add("Placeholder synonym '" + safe(rule.input())
                                + "' is not supported on Lucene and was skipped.");
                        yield false;
                    }
                };
                if (contributed) {
                    applied++;
                }
            }
        }

        SynonymMap map = null;
        if (applied > 0) {
            try {
                map = builder.build();
            } catch (java.io.IOException e) {
                // Building an in-memory FST never does real I/O; treat as "no map".
                warnings.add("Failed to build Lucene synonym map: " + e.getMessage());
            }
        }
        return new TurLuceneSynonymMap(map, applied, unsupported, warnings);
    }

    private static boolean addRegular(SynonymMap.Builder builder, List<String> terms,
            org.apache.lucene.analysis.CharArraySet stopWords) {
        List<String> clean = clean(terms);
        if (clean.size() < 2) {
            return false;
        }
        // Fully-equivalent set: every term expands to every other (both ways).
        for (String from : clean) {
            for (String to : clean) {
                if (!from.equals(to)) {
                    add(builder, from, to, stopWords);
                }
            }
        }
        return true;
    }

    private static boolean addDirectional(SynonymMap.Builder builder, String input, List<String> terms,
            org.apache.lucene.analysis.CharArraySet stopWords) {
        String key = input == null ? null : input.trim().toLowerCase(Locale.ROOT);
        List<String> clean = clean(terms);
        if (key == null || key.isEmpty() || clean.isEmpty()) {
            return false;
        }
        boolean any = false;
        for (String to : clean) {
            if (!to.equals(key)) {
                add(builder, key, to, stopWords);
                any = true;
            }
        }
        return any;
    }

    private static boolean addCorrection(SynonymMap.Builder builder, List<String> warnings,
            TurSESynonymRule rule, org.apache.lucene.analysis.CharArraySet stopWords) {
        boolean contributed = addDirectional(builder, rule.input(), rule.terms(), stopWords);
        if (contributed) {
            warnings.add("Correction '" + safe(rule.input())
                    + "' degraded to a one-way synonym (Lucene has no per-rule typo budget).");
        }
        return contributed;
    }

    /** Builds a Lucene {@link CharsRef} for a (possibly multi-word) phrase. */
    private static CharsRef charsRef(String phrase) {
        String[] words = phrase.split("\\s+");
        if (words.length == 1) {
            return new CharsRef(phrase);
        }
        return SynonymMap.Builder.join(words, new CharsRefBuilder());
    }

    private static List<String> clean(List<String> terms) {
        List<String> out = new ArrayList<>();
        if (terms == null) {
            return out;
        }
        for (String term : terms) {
            if (term == null) {
                continue;
            }
            String norm = term.trim().toLowerCase(Locale.ROOT);
            if (!norm.isEmpty() && !out.contains(norm)) {
                out.add(norm);
            }
        }
        return out;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
