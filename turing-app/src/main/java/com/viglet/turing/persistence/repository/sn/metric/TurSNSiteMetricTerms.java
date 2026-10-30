/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.persistence.repository.sn.metric;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.viglet.turing.persistence.model.sn.TurSNSite;

/**
 * T1179 — how a logged search is grouped and how it is shown.
 *
 * <p>{@link #fold} is the grouping key ({@code sanatizedTerm}) of a search and a
 * click: diacritics dropped, spaces collapsed, lower-cased, and no letter ever
 * deleted. It used to remove every non-ASCII character, which also emptied a
 * Cyrillic, Greek, Arabic or CJK search, so all of them collapsed into one blank
 * row.
 *
 * <p>{@link #asTyped} shows each grouped key as visitors typed it: the most
 * frequent spelling of it in the stored {@code term}, so {@code emergência}
 * searched three times and {@code emergencia} once reads {@code emergência}. A
 * key whose spelling cannot be read keeps the key.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.7
 */
public final class TurSNSiteMetricTerms {

    private TurSNSiteMetricTerms() {
        throw new IllegalStateException("Semantic Navigation metric terms utility class");
    }

    /** The grouping key of {@code term}: diacritics dropped, never a letter. */
    public static String fold(String term) {
        if (term == null) {
            return null;
        }
        return Normalizer.normalize(term, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    /**
     * {@code terms} with each grouped key replaced by its most frequent spelling as
     * typed (ties: the spelling that sorts last, which puts {@code emergência} over
     * {@code emergencia}). Returns {@code terms} itself, changed in place.
     */
    public static List<TurSNSiteMetricAccessTerm> asTyped(TurSNSiteMetricAccessRepository repository,
            TurSNSite site, List<TurSNSiteMetricAccessTerm> terms) {
        if (terms == null || terms.isEmpty()) {
            return terms;
        }
        List<String> keys = terms.stream().map(TurSNSiteMetricAccessTerm::getTerm).filter(Objects::nonNull)
                .distinct().toList();
        if (keys.isEmpty()) {
            return terms;
        }
        Map<String, String> typed = typedSpellings(repository.spellings(site, keys));
        terms.forEach(term -> {
            String spelling = typed.get(term.getTerm());
            if (spelling != null) {
                term.setTerm(spelling);
            }
        });
        return terms;
    }

    /** Key → its most frequent spelling, the spelling trimmed with its spaces collapsed. */
    static Map<String, String> typedSpellings(List<TurSNSiteMetricTermSpelling> spellings) {
        Map<String, TurSNSiteMetricTermSpelling> best = new HashMap<>();
        Comparator<TurSNSiteMetricTermSpelling> order = Comparator
                .comparingLong(TurSNSiteMetricTermSpelling::count)
                .thenComparing(s -> clean(s.term()));
        if (spellings != null) {
            for (TurSNSiteMetricTermSpelling spelling : spellings) {
                if (spelling == null || spelling.key() == null || clean(spelling.term()).isEmpty()) {
                    continue;
                }
                best.merge(spelling.key(), spelling, (a, b) -> order.compare(a, b) >= 0 ? a : b);
            }
        }
        Map<String, String> typed = new HashMap<>();
        best.forEach((key, spelling) -> typed.put(key, clean(spelling.term())));
        return typed;
    }

    private static String clean(String term) {
        return term == null ? "" : term.replaceAll("\\s+", " ").trim();
    }
}
