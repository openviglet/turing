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
import java.text.Normalizer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;

/**
 * T1194 — recomposes decomposed (Unicode NFD) accented letters before the
 * tokenizer sees them, so {@code impermeável} (an {@code a} followed by a
 * combining acute) is analysed exactly like {@code impermeável}.
 *
 * <p>Why it matters: {@link TurLuceneFoldingAnalyzer} folds accents with
 * {@code ASCIIFoldingFilter}, which maps precomposed letters only. A combining mark
 * stays inside the token, so a title stored in NFD (text pasted from macOS, some
 * CMS exports) indexed one term, {@code impermeável}, with no folded form and
 * no stem, and neither the accented nor the unaccented query a browser sends (NFC)
 * could match it.
 *
 * <p>A {@link MappingCharFilter} rather than a whole-text {@code Normalizer} pass:
 * it corrects offsets, so highlighting still marks the right characters. The map
 * holds every letter in the Latin, Greek and Cyrillic blocks whose canonical
 * decomposition differs from it, which {@link Normalizer} computes, so no table is
 * maintained by hand. Text already in NFC passes through unchanged.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public final class TurLuceneNfcCharFilter {

    /** The blocks holding the precomposed letters a site's text can carry. */
    private static final int[][] RANGES = {
            { 0x00C0, 0x024F }, // Latin-1 Supplement, Latin Extended-A and -B
            { 0x0370, 0x03FF }, // Greek and Coptic
            { 0x0400, 0x04FF }, // Cyrillic
            { 0x1E00, 0x1EFF }, // Latin Extended Additional (Vietnamese)
            { 0x1F00, 0x1FFF }, // Greek Extended
    };

    /**
     * T1217 — every decomposed spelling and the letter it recomposes to, in code
     * point order: the one table this filter and Elasticsearch's {@code mapping}
     * char filter are both built from.
     */
    public static final Map<String, String> PAIRS = pairs();

    static final NormalizeCharMap MAP = build();

    private TurLuceneNfcCharFilter() {
    }

    /** {@code reader} with decomposed letters recomposed. */
    public static Reader wrap(Reader reader) {
        return new MappingCharFilter(MAP, reader);
    }

    private static Map<String, String> pairs() {
        Map<String, String> pairs = new LinkedHashMap<>();
        for (int[] range : RANGES) {
            for (int cp = range[0]; cp <= range[1]; cp++) {
                if (!Character.isLetter(cp)) {
                    continue;
                }
                String composed = new String(Character.toChars(cp));
                String decomposed = Normalizer.normalize(composed, Normalizer.Form.NFD);
                if (!decomposed.equals(composed)
                        && Normalizer.normalize(decomposed, Normalizer.Form.NFC).equals(composed)) {
                    pairs.put(decomposed, composed);
                }
            }
        }
        return Collections.unmodifiableMap(pairs);
    }

    private static NormalizeCharMap build() {
        NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
        PAIRS.forEach(builder::add);
        return builder.build();
    }
}
