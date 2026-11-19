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
import org.apache.lucene.analysis.miscellaneous.ConditionalTokenFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
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
        // T1080 — folded after the synonyms, so a rule keyed on an accented word
        // still fires, and its output matches the folded index too.
        tokens = new VariantsExceptPhraseTail(tokens, stemmer);
        return new TokenStreamComponents(source, tokens);
    }

    /**
     * T1297 — the stem and folded variants for every token except the second and later
     * words of a multi-word synonym output. Those variants stack at each word of the
     * expansion, and the query parser turns every path through the graph into its own
     * phrase: six words with two to five variants each is hundreds of phrases per
     * field, past {@code IndexSearcher}'s clause limit, and the search failed. The index
     * keeps every original word beside its variants, so the expansion as written still
     * matches; the first word keeps its variants, so paths grow by a handful, not a
     * power. The words typed and single-word synonyms are stemmed and folded as before.
     *
     * <p>{@link SynonymGraphFilter} emits a multi-word output's first word, then the
     * word typed spanning the whole phrase ({@code positionLength > 1}), then the
     * output's other words, one position each. A synonym token at a position inside
     * such a span, after its start, is a tail word.
     */
    static final class VariantsExceptPhraseTail extends ConditionalTokenFilter {

        private final TypeAttribute type = addAttribute(TypeAttribute.class);
        private final PositionIncrementAttribute increment = addAttribute(PositionIncrementAttribute.class);
        private final PositionLengthAttribute length = addAttribute(PositionLengthAttribute.class);
        private int position = -1;
        private int spanStart = -1;
        private int spanEnd = -1;

        VariantsExceptPhraseTail(TokenStream input, String stemmer) {
            super(input, in -> TurLuceneFoldingAnalyzer.fold(TurLuceneFoldingAnalyzer.stem(in, stemmer)));
        }

        @Override
        protected boolean shouldFilter() {
            position += increment.getPositionIncrement();
            if (length.getPositionLength() > 1 && position + length.getPositionLength() - 1 > spanEnd) {
                spanStart = position;
                spanEnd = position + length.getPositionLength() - 1;
            }
            boolean tail = SynonymGraphFilter.TYPE_SYNONYM.equals(type.type())
                    && position > spanStart && position <= spanEnd;
            return !tail;
        }

        @Override
        public void reset() throws java.io.IOException {
            super.reset();
            position = -1;
            spanStart = -1;
            spanEnd = -1;
        }
    }
}
