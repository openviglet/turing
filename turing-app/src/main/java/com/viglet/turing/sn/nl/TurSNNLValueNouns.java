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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.pt.PortugueseLightStemFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import com.viglet.turing.searchengine.TurSNQueryRelaxer;

/**
 * T1359 — the words of a query that name a value a facet field holds:
 * {@code palestras} in "palestras sobre democracia" is the {@code Palestra} value
 * of {@code tipo_evento}, so it is a filter, not a search word.
 *
 * <p>A word and a value are compared by their content words, each lower-cased,
 * stemmed by the language's stemmer and stripped of accents, so the plural and the
 * singular, any case and any accenting meet. A value matches only as a whole, and
 * only when exactly one value of one field fits the words (T1300's rule): a word two
 * fields hold, or two values of one field, is left in the query. Stop words never
 * match on their own. A word that is the topic of another ("livros sobre
 * palestras") stays in the query too: the visitor wants books, not lectures.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
final class TurSNNLValueNouns {

    /** The words that make the next noun a topic, in the languages the site search speaks. */
    private static final Set<String> TOPIC_MARKERS = Set.of("sobre", "acerca", "about", "regarding",
            "concerning", "on", "sur", "propos");

    /** ISO language → Snowball stemmer name, as the index analyzer stems (T1170). */
    private static final Map<String, String> STEMMERS = Map.of("en", "English", "pt", "Portuguese",
            "es", "Spanish", "fr", "French", "de", "German", "it", "Italian", "nl", "Dutch");

    /**
     * One value the query names.
     *
     * @param start the offset in the query where its words start
     * @param end   the offset just past them
     * @param exact whether the words spell the value itself, folded for case and accents
     */
    record Match(String field, String value, int start, int end, boolean exact) {
    }

    private record Word(String text, String key, int start, int end, boolean stop) {
    }

    private record Span(int from, int length) {
    }

    private record Holder(String field, String value) {
    }

    private TurSNNLValueNouns() {
    }

    /**
     * The values {@code query} names, ordered as they appear in it.
     *
     * @param held       the values each facet field holds
     * @param skipFields fields that already carry a filter, which a noun must not widen
     */
    static List<Match> find(String query, String locale, Map<String, ? extends Collection<String>> held,
            Set<String> skipFields) {
        if (query == null || query.isBlank() || held == null || held.isEmpty()) {
            return List.of();
        }
        Locale language = locale == null || locale.isBlank() ? null
                : Locale.forLanguageTag(locale.replace('_', '-'));
        CharArraySet stopWords = language == null ? CharArraySet.EMPTY_SET : TurSNQueryRelaxer.stopWords(language);
        try (Analyzer analyzer = analyzer(language == null ? null : STEMMERS.get(language.getLanguage()))) {
            List<Word> words = words(analyzer, query, stopWords);
            List<Word> content = words.stream().filter(word -> !word.stop()).toList();
            if (content.isEmpty()) {
                return List.of();
            }
            Map<Span, Set<Holder>> spans = spans(analyzer, content, held, skipFields, stopWords);
            return accept(query, words, content, spans);
        }
    }

    /** Every span of content words that spells a whole value, with every value it spells. */
    private static Map<Span, Set<Holder>> spans(Analyzer analyzer, List<Word> content,
            Map<String, ? extends Collection<String>> held, Set<String> skipFields, CharArraySet stopWords) {
        List<String> keys = content.stream().map(Word::key).toList();
        Map<Span, Set<Holder>> spans = new HashMap<>();
        held.forEach((field, values) -> {
            if (skipFields.contains(field) || values == null) {
                return;
            }
            for (String value : values) {
                List<String> valueKeys = words(analyzer, value, stopWords).stream()
                        .filter(word -> !word.stop()).map(Word::key).toList();
                if (valueKeys.isEmpty()) {
                    continue;
                }
                for (int at = 0; at + valueKeys.size() <= keys.size(); at++) {
                    if (keys.subList(at, at + valueKeys.size()).equals(valueKeys)) {
                        spans.computeIfAbsent(new Span(at, valueKeys.size()), k -> new LinkedHashSet<>())
                                .add(new Holder(field, value));
                    }
                }
            }
        });
        return spans;
    }

    /**
     * The longest spans first, each kept only when one value holds it, it overlaps
     * no span already decided and it is not a topic. An ambiguous span still claims
     * its words, so a shorter value inside it is not guessed either.
     */
    private static List<Match> accept(String query, List<Word> words, List<Word> content,
            Map<Span, Set<Holder>> spans) {
        boolean[] decided = new boolean[content.size()];
        List<Match> matches = new ArrayList<>();
        spans.entrySet().stream()
                .sorted(java.util.Comparator
                        .comparingInt((Map.Entry<Span, Set<Holder>> entry) -> -entry.getKey().length())
                        .thenComparingInt(entry -> entry.getKey().from()))
                .forEach(entry -> {
                    Span span = entry.getKey();
                    for (int i = span.from(); i < span.from() + span.length(); i++) {
                        if (decided[i]) {
                            return;
                        }
                    }
                    for (int i = span.from(); i < span.from() + span.length(); i++) {
                        decided[i] = true;
                    }
                    Word first = content.get(span.from());
                    Word last = content.get(span.from() + span.length() - 1);
                    if (entry.getValue().size() != 1 || isTopic(words, first)) {
                        return;
                    }
                    Holder holder = entry.getValue().iterator().next();
                    String spelled = query.substring(first.start(), last.end());
                    matches.add(new Match(holder.field(), holder.value(), first.start(), last.end(),
                            TurSNNLFilterService.normalize(spelled)
                                    .equals(TurSNNLFilterService.normalize(holder.value()))));
                });
        matches.sort((a, b) -> Integer.compare(a.start(), b.start()));
        return matches;
    }

    /** Whether a topic marker comes before {@code first}, with only stop words between them. */
    private static boolean isTopic(List<Word> words, Word first) {
        for (int i = words.indexOf(first) - 1; i >= 0; i--) {
            String text = TurSNNLFilterService.normalize(words.get(i).text());
            if (TOPIC_MARKERS.contains(text)) {
                return true;
            }
            if (!words.get(i).stop()) {
                return false;
            }
        }
        return false;
    }

    /** The words of {@code text}, each with its comparison key and its offsets. */
    private static List<Word> words(Analyzer analyzer, String text, CharArraySet stopWords) {
        List<Word> words = new ArrayList<>();
        try (TokenStream tokens = analyzer.tokenStream("", text)) {
            CharTermAttribute term = tokens.addAttribute(CharTermAttribute.class);
            OffsetAttribute offset = tokens.addAttribute(OffsetAttribute.class);
            tokens.reset();
            while (tokens.incrementToken()) {
                String spelled = text.substring(offset.startOffset(), offset.endOffset());
                words.add(new Word(spelled, term.toString(), offset.startOffset(), offset.endOffset(),
                        stopWords.contains(spelled.toLowerCase(Locale.ROOT))));
            }
            tokens.end();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return words;
    }

    /**
     * Lower case, the language's stemmer, then ASCII folding: one key per word, the
     * same for {@code Palestra}, {@code palestras} and {@code PALESTRAS}. Portuguese
     * takes the light stemmer first, as the index does (T1196), so a plural in
     * {@code -ões} or {@code -ães} reaches its singular.
     */
    private static Analyzer analyzer(String stemmer) {
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                Tokenizer source = new StandardTokenizer();
                TokenStream tokens = new LowerCaseFilter(source);
                if ("Portuguese".equals(stemmer)) {
                    tokens = new PortugueseLightStemFilter(tokens);
                }
                if (stemmer != null) {
                    tokens = new SnowballFilter(tokens, stemmer);
                }
                return new TokenStreamComponents(source, new ASCIIFoldingFilter(tokens));
            }
        };
    }
}
