/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.sn.nl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Set;

import org.apache.lucene.analysis.CharArraySet;
import org.junit.jupiter.api.Test;

import com.viglet.turing.sn.TurSNQueryRelaxer;

/**
 * T1300 — a short name a visitor says grounds to the one facet value that holds all
 * its words; several or none keep it out.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurSNNLContainingValueTest {

    private static final CharArraySet PT = TurSNQueryRelaxer.stopWords(Locale.of("pt", "BR"));

    private static final Set<String> PLACES = Set.of("Casarão FESPSP", "Auditório Florestan Fernandes",
            "Biblioteca Central");
    private static final Set<String> PUBLISHERS = Set.of("Editora Sociologia e Política",
            "Revista de Sociologia Aplicada");

    @Test
    void theShortNameGroundsToTheValueThatHoldsIt() {
        assertThat(TurSNNLFilterService.containing("Casarão", PLACES, PT)).isEqualTo("Casarão FESPSP");
        assertThat(TurSNNLFilterService.containing("casarao", PLACES, PT)).isEqualTo("Casarão FESPSP");
        assertThat(TurSNNLFilterService.containing("Sociologia e Política", PUBLISHERS, PT))
                .isEqualTo("Editora Sociologia e Política");
    }

    @Test
    void aNameSeveralValuesHoldIsNotGuessed() {
        assertThat(TurSNNLFilterService.containing("Sociologia", PUBLISHERS, PT)).isNull();
    }

    @Test
    void stopWordsAndPartsOfWordsDoNotMatch() {
        assertThat(TurSNNLFilterService.containing("e de", PUBLISHERS, PT)).isNull();
        assertThat(TurSNNLFilterService.containing("Casa", PLACES, PT)).isNull();
        assertThat(TurSNNLFilterService.containing("Casarão Paulista", PLACES, PT)).isNull();
        assertThat(TurSNNLFilterService.containing("Casarão", null, PT)).isNull();
    }
}
