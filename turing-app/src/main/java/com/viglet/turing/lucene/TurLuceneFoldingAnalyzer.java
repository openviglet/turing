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
package com.viglet.turing.lucene;

import java.io.Reader;
import java.util.Locale;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.miscellaneous.KeywordRepeatFilter;
import org.apache.lucene.analysis.miscellaneous.RemoveDuplicatesTokenFilter;
import org.apache.lucene.analysis.pt.PortugueseLightStemFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

/**
 * T1080 — {@code StandardAnalyzer}'s tokenize + lowercase, then ASCII folding, so
 * an accented and an unaccented spelling match each other ({@code saude} finds
 * {@code saúde}, {@code sao paulo} finds {@code São Paulo}).
 *
 * <p>The folded token is added <em>beside</em> the original, at the same position
 * ({@code preserveOriginal}), on both the index and the query side. That keeps an
 * index written before T1080 working: it holds only the accented terms, which an
 * accented query still produces. An unaccented query matches such a document once
 * it is re-indexed.
 *
 * <p>T1170 — built for a locale whose language has a Snowball stemmer, it also adds
 * the stem beside each token ({@code jackets} → {@code jackets}, {@code jacket}), so
 * a plural query finds the singular. The original token is kept for the same reason
 * the folded one is: an index written before T1170 still matches the exact word, and
 * a query stem matches its singular-spelled documents at once; a document spelled
 * in the plural is found by the singular after a reindex. An unknown language stays
 * unstemmed.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
public class TurLuceneFoldingAnalyzer extends Analyzer {

    private static final String PORTUGUESE = "Portuguese";

    /** ISO language → Snowball stemmer name. */
    private static final Map<String, String> STEMMERS = Map.ofEntries(
            Map.entry("en", "English"),
            Map.entry("pt", PORTUGUESE),
            Map.entry("es", "Spanish"),
            Map.entry("fr", "French"),
            Map.entry("de", "German"),
            Map.entry("it", "Italian"),
            Map.entry("nl", "Dutch"));

    private final String stemmer;
    private final CharArraySet stopWords;

    /** Folding only, no stemming and no stop words. */
    public TurLuceneFoldingAnalyzer() {
        this(null, CharArraySet.EMPTY_SET);
    }

    /**
     * Folding plus the stemmer of {@code locale}'s language, when it has one: the
     * index analyzer, which keeps stop words (T1242), so a query made only of them
     * still finds the documents that hold them.
     */
    public TurLuceneFoldingAnalyzer(Locale locale) {
        this(stemmerFor(locale), CharArraySet.EMPTY_SET);
    }

    /**
     * T1242 — the query analyzer: as {@link #TurLuceneFoldingAnalyzer(Locale)},
     * and without the language's stop words, so {@code uma barraca para 2 pessoas}
     * needs, and highlights, only the content words. {@code keepStopWords} is for
     * a query made only of stop words, which would otherwise analyze to nothing.
     */
    public static TurLuceneFoldingAnalyzer forQuery(Locale locale, boolean keepStopWords) {
        return new TurLuceneFoldingAnalyzer(stemmerFor(locale),
                keepStopWords ? CharArraySet.EMPTY_SET : stopWordsFor(locale));
    }

    private TurLuceneFoldingAnalyzer(String stemmer, CharArraySet stopWords) {
        this.stemmer = stemmer;
        this.stopWords = stopWords;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new StandardTokenizer();
        return new TokenStreamComponents(source,
                fold(stem(stop(new LowerCaseFilter(source), stopWords), stemmer)));
    }

    /**
     * T1242 — drops the stop words before stemming, keeping their positions, so a
     * phrase across a dropped word still lines up; an empty set returns
     * {@code tokens} unchanged.
     */
    public static TokenStream stop(TokenStream tokens, CharArraySet stopWords) {
        return stopWords == null || stopWords.isEmpty() ? tokens : new StopFilter(tokens, stopWords);
    }

    /** T1242 — the stop words of {@code locale}'s language: the Snowball lists the query relaxer uses. */
    public static CharArraySet stopWordsFor(Locale locale) {
        return com.viglet.turing.sn.TurSNQueryRelaxer.stopWords(locale);
    }

    /** T1194 — decomposed (NFD) accents recomposed before tokenizing, index and query side alike. */
    @Override
    protected Reader initReader(String fieldName, Reader reader) {
        return TurLuceneNfcCharFilter.wrap(reader);
    }

    /** T1194 — and before a prefix or wildcard term is normalized. */
    @Override
    protected Reader initReaderForNormalization(String fieldName, Reader reader) {
        return TurLuceneNfcCharFilter.wrap(reader);
    }

    /** Lowercase only, as StandardAnalyzer: a prefix term keeps the spelling typed. */
    @Override
    protected TokenStream normalize(String fieldName, TokenStream in) {
        return new LowerCaseFilter(in);
    }

    /** Adds the ASCII-folded form beside each token that has one. */
    public static TokenStream fold(TokenStream tokens) {
        return new ASCIIFoldingFilter(tokens, true);
    }

    /**
     * T1170 — adds the Snowball stem beside each token that has a different one;
     * a {@code null} stemmer returns {@code tokens} unchanged.
     */
    public static TokenStream stem(TokenStream tokens, String stemmer) {
        if (stemmer == null) {
            return tokens;
        }
        if (PORTUGUESE.equals(stemmer)) {
            // T1196 — Snowball leaves bastão/bastões, pão/pães and mão/mãos apart; the
            // light stemmer brings each plural to its singular, and that form is then
            // stemmed beside the word, so singular and plural share a term.
            tokens = new RemoveDuplicatesTokenFilter(
                    new PortugueseLightStemFilter(new KeywordRepeatFilter(tokens)));
        }
        return new RemoveDuplicatesTokenFilter(
                new SnowballFilter(new KeywordRepeatFilter(tokens), stemmer));
    }

    /** The Snowball stemmer for {@code locale}'s language, or {@code null}. */
    public static String stemmerFor(Locale locale) {
        return locale == null ? null : STEMMERS.get(locale.getLanguage());
    }
}
