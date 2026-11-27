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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * T1359 — a word of the query that names a facet value, matched by its stem and
 * without case or accents, only when one value of one field fits it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurSNNLValueNounsTest {

    private static final Map<String, List<String>> EVENTS = Map.of(
            "tipo_evento", List.of("Palestra", "Debate", "Lançamento de livro", "Lançamento"),
            "area", List.of("Sociologia", "Ciência Política"));

    private static List<TurSNNLValueNouns.Match> find(String q, Map<String, List<String>> held) {
        return TurSNNLValueNouns.find(q, "pt_BR", held, Set.of());
    }

    @Test
    @DisplayName("A plural noun names the singular value, and its words are where the query spells them")
    void aPluralNamesTheValue() {
        String q = "palestras sobre democracia";

        assertThat(find(q, EVENTS)).singleElement().satisfies(match -> {
            assertThat(match.field()).isEqualTo("tipo_evento");
            assertThat(match.value()).isEqualTo("Palestra");
            assertThat(q.substring(match.start(), match.end())).isEqualTo("palestras");
            assertThat(match.exact()).isFalse();
        });
    }

    @Test
    @DisplayName("Case and accents do not matter, and the same spelling is exact")
    void caseAndAccentsDoNotMatter() {
        assertThat(find("DEBATE em novembro", EVENTS)).singleElement()
                .satisfies(match -> {
                    assertThat(match.value()).isEqualTo("Debate");
                    assertThat(match.exact()).isTrue();
                });
        assertThat(find("ciencia politica", EVENTS)).singleElement()
                .satisfies(match -> assertThat(match.value()).isEqualTo("Ciência Política"));
    }

    @Test
    @DisplayName("A multi-word value wins over the shorter value inside it")
    void theLongerValueWins() {
        String q = "lançamentos de livros em dezembro";

        assertThat(find(q, EVENTS)).singleElement().satisfies(match -> {
            assertThat(match.value()).isEqualTo("Lançamento de livro");
            assertThat(q.substring(match.start(), match.end())).isEqualTo("lançamentos de livros");
        });
    }

    @Test
    @DisplayName("A word two fields hold stays in the query")
    void aWordTwoFieldsHoldIsLeft() {
        Map<String, List<String>> held = Map.of("tipo_evento", List.of("Debate"), "formato", List.of("Debate"));

        assertThat(find("debates de sexta", held)).isEmpty();
    }

    @Test
    @DisplayName("Two values of one field that spell the same words stay in the query")
    void twoValuesOfOneFieldAreLeft() {
        Map<String, List<String>> held = Map.of("tipo_evento", List.of("Debate", "Debates"));

        assertThat(find("debates", held)).isEmpty();
    }

    @Test
    @DisplayName("A stop word never matches, even when a value is spelled as one")
    void aStopWordNeverMatches() {
        Map<String, List<String>> held = Map.of("idioma", List.of("de", "em"));

        assertThat(find("palestras de economia em maio", held)).isEmpty();
    }

    @Test
    @DisplayName("A value that is the topic of another noun stays in the query")
    void aTopicStays() {
        assertThat(find("livros sobre palestras", EVENTS)).isEmpty();
        assertThat(find("livros acerca das palestras", EVENTS)).isEmpty();
    }

    @Test
    @DisplayName("A field that already carries a filter is not widened")
    void aFilteredFieldIsSkipped() {
        assertThat(TurSNNLValueNouns.find("palestras", "pt_BR", EVENTS, Set.of("tipo_evento"))).isEmpty();
    }

    @Test
    @DisplayName("Two values of one field are both named, in the order the query has them")
    void twoValuesAreBothNamed() {
        assertThat(find("palestras e debates", EVENTS)).extracting(TurSNNLValueNouns.Match::value)
                .containsExactly("Palestra", "Debate");
    }

    @Test
    @DisplayName("Nothing to match answers nothing")
    void nothingToMatch() {
        assertThat(find("democracia", EVENTS)).isEmpty();
        assertThat(find("  ", EVENTS)).isEmpty();
        assertThat(find("palestras", Map.of())).isEmpty();
        assertThat(TurSNNLValueNouns.find("lectures", null, Map.of("kind", List.of("Lecture")), Set.of()))
                .isEmpty();
    }

    @Test
    @DisplayName("English stems its plurals too")
    void englishPlurals() {
        assertThat(TurSNNLValueNouns.find("lectures about democracy", "en_US",
                Map.of("kind", List.of("Lecture")), Set.of())).singleElement()
                .satisfies(match -> assertThat(match.value()).isEqualTo("Lecture"));
    }
}
