/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.sn;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.WordlistLoader;

import lombok.extern.slf4j.Slf4j;

/**
 * T1176 — the words of a query worth retrying a search that found nothing with:
 * the query's words minus the search locale's stop words. A question ("What do I
 * need for two days in the hills?") requires its function words under the
 * engines' AND, so it matches nothing; its main words ({@code need two days
 * hills}) are what the relaxed retry ORs under a minimum-should-match.
 *
 * <p>The stop words are Lucene's Snowball lists, by the locale's language; a
 * language without one keeps every word. Engine-neutral: every engine is handed
 * the same words and only the operator differs.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
@Slf4j
public final class TurSNQueryRelaxer {

    private static final String STOP_PATH = "org/apache/lucene/analysis/snowball/%s_stop.txt";
    private static final Map<String, String> LISTS = Map.ofEntries(
            Map.entry("da", "danish"), Map.entry("nl", "dutch"), Map.entry("en", "english"),
            Map.entry("fi", "finnish"), Map.entry("fr", "french"), Map.entry("de", "german"),
            Map.entry("hu", "hungarian"), Map.entry("id", "indonesian"), Map.entry("ga", "irish"),
            Map.entry("it", "italian"), Map.entry("no", "norwegian"), Map.entry("nb", "norwegian"),
            Map.entry("pt", "portuguese"), Map.entry("ru", "russian"), Map.entry("es", "spanish"),
            Map.entry("sv", "swedish"));
    private static final Map<String, CharArraySet> CACHE = new ConcurrentHashMap<>();

    private TurSNQueryRelaxer() {
        throw new IllegalStateException("Semantic Navigation query relaxer utility class");
    }

    /**
     * The main words of {@code query}, space-joined, or empty when there is
     * nothing to relax: a blank or match-all query, a query using the engine's
     * syntax (quotes, wildcards, fields, groups), a one-word query, or one whose
     * every word is a stop word.
     */
    public static Optional<String> relax(String query, Locale locale) {
        if (query == null || query.isBlank() || query.trim().equals("*") || usesSyntax(query)) {
            return Optional.empty();
        }
        String[] words = query.trim().split("[^\\p{L}\\p{N}]+");
        List<String> kept = new ArrayList<>();
        int total = 0;
        CharArraySet stop = stopWords(locale);
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            total++;
            if (!stop.contains(word.toLowerCase(Locale.ROOT))) {
                kept.add(word);
            }
        }
        return total < 2 || kept.isEmpty() ? Optional.empty() : Optional.of(String.join(" ", kept));
    }

    private static boolean usesSyntax(String query) {
        for (char c : query.toCharArray()) {
            if ("\"*:()[]{}~^".indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }

    /** The Snowball stop words of the locale's language; empty without a list. */
    private static CharArraySet stopWords(Locale locale) {
        String list = locale == null ? null : LISTS.get(locale.getLanguage());
        if (list == null) {
            return CharArraySet.EMPTY_SET;
        }
        return CACHE.computeIfAbsent(list, TurSNQueryRelaxer::load);
    }

    private static CharArraySet load(String list) {
        String path = STOP_PATH.formatted(list);
        try (InputStream in = TurSNQueryRelaxer.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return CharArraySet.EMPTY_SET;
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return CharArraySet.unmodifiableSet(WordlistLoader.getSnowballWordSet(reader));
            }
        } catch (IOException e) {
            log.warn("[Relax] could not read the stop words '{}': {}", path, e.getMessage());
            return CharArraySet.EMPTY_SET;
        }
    }
}
