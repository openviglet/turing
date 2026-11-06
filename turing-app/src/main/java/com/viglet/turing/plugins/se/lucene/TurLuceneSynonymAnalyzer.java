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
package com.viglet.turing.plugins.se.lucene;

import java.io.Reader;
import java.util.Locale;

import com.viglet.turing.lucene.TurLuceneFoldingAnalyzer;
import com.viglet.turing.lucene.TurLuceneNfcCharFilter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;

/**
 * T665 / §XXXIX (Block AP) — a query-time analyzer that mirrors
 * {@code StandardAnalyzer}'s tokenize + lowercase, then applies a
 * {@link SynonymGraphFilter} built from the site's {@link SynonymMap}. Used only
 * on the query side (never at index time), so synonym edits take effect on the
 * next search with no reindex.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
public class TurLuceneSynonymAnalyzer extends Analyzer {

    private final SynonymMap synonymMap;
    private final String stemmer;
    private final org.apache.lucene.analysis.CharArraySet stopWords;

    public TurLuceneSynonymAnalyzer(SynonymMap synonymMap) {
        this(synonymMap, null);
    }

    /** T1170 — also adds the stem in {@code locale}'s language, as the index side does. */
    public TurLuceneSynonymAnalyzer(SynonymMap synonymMap, Locale locale) {
        this(synonymMap, locale, false);
    }

    /** T1242 — and drops the language's stop words, unless the query is made only of them. */
    public TurLuceneSynonymAnalyzer(SynonymMap synonymMap, Locale locale, boolean keepStopWords) {
        this.synonymMap = synonymMap;
        this.stemmer = TurLuceneFoldingAnalyzer.stemmerFor(locale);
        this.stopWords = keepStopWords ? org.apache.lucene.analysis.CharArraySet.EMPTY_SET
                : TurLuceneFoldingAnalyzer.stopWordsFor(locale);
    }

    /** T1194 — decomposed (NFD) accents recomposed, as the index side does. */
    @Override
    protected Reader initReader(String fieldName, Reader reader) {
        return TurLuceneNfcCharFilter.wrap(reader);
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new StandardTokenizer();
        TokenStream tokens = new LowerCaseFilter(source);
        // T1242 — stop words dropped before the synonyms: a filter that removes
        // tokens from a synonym graph breaks its position lengths. A rule spelled
        // with a stop word fires with it skipped, as on the index side.
        tokens = TurLuceneFoldingAnalyzer.stop(tokens, stopWords);
        tokens = new SynonymGraphFilter(tokens, synonymMap, true);
        // T1170 — stemmed after the synonyms, so a rule keyed on a plural still fires.
        tokens = TurLuceneFoldingAnalyzer.stem(tokens, stemmer);
        // T1080 — folded after the synonyms, so a rule keyed on an accented word
        // still fires, and its output matches the folded index too.
        tokens = TurLuceneFoldingAnalyzer.fold(tokens);
        return new TokenStreamComponents(source, tokens);
    }
}
