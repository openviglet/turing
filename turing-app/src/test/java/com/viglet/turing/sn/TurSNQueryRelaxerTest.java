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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Test;

/**
 * T1176 — a question's main words are its words minus the locale's stop words.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.6
 */
class TurSNQueryRelaxerTest {

    @Test
    void aQuestionKeepsItsMainWords() {
        assertThat(TurSNQueryRelaxer.relax("What do I need for two days in the hills?", Locale.US))
                .contains("need two days hills");
        assertThat(TurSNQueryRelaxer.relax("O que eu preciso para dois dias na serra?",
                Locale.forLanguageTag("pt-BR"))).contains("preciso dois dias serra");
    }

    @Test
    void nothingToRelax() {
        assertThat(TurSNQueryRelaxer.relax("backpack", Locale.US)).isEmpty();
        assertThat(TurSNQueryRelaxer.relax("*", Locale.US)).isEmpty();
        assertThat(TurSNQueryRelaxer.relax("  ", Locale.US)).isEmpty();
        assertThat(TurSNQueryRelaxer.relax("\"two days\" hills", Locale.US)).isEmpty();
        assertThat(TurSNQueryRelaxer.relax("title:tent hills", Locale.US)).isEmpty();
        assertThat(TurSNQueryRelaxer.relax("what is the", Locale.US)).isEmpty();
    }

    @Test
    void aLanguageWithoutAListKeepsEveryWord() {
        assertThat(TurSNQueryRelaxer.relax("two days hills", Locale.JAPANESE)).contains("two days hills");
        assertThat(TurSNQueryRelaxer.relax("two days", null)).contains("two days");
    }
}
