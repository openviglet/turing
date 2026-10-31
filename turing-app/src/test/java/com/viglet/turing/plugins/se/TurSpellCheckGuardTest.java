/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.plugins.se;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.viglet.turing.commons.se.result.spellcheck.TurSESpellCheckResult;

/**
 * T1199 — a spell-check correction never changes a number, a symbol, or the
 * case of a token it did not correct.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.7
 */
class TurSpellCheckGuardTest {

    @Test
    void priceOnlyChangeIsNoCorrection() {
        TurSESpellCheckResult result = TurSpellCheckGuard.guard(
                "uma barraca para duas pessoas até R$ 2.200",
                "uma barraca para duas pessoas até r$ 2.100");
        assertThat(result.isCorrected()).isFalse();
    }

    @Test
    void enUsPriceOnlyChangeIsNoCorrection() {
        assertThat(TurSpellCheckGuard.guardedText("tent under $2,200", "tent under $2,100")).isNull();
        assertThat(TurSpellCheckGuard.guardedText("Tent under US$ 1,500.00", "tent under us$ 1,600.00"))
                .isNull();
    }

    @Test
    void realMisspellingNextToPriceKeepsPriceAndCase() {
        TurSESpellCheckResult result = TurSpellCheckGuard.guard(
                "uma barrca para duas pessoas até R$ 2.200",
                "uma barraca para duas pessoas até r$ 2.100");
        assertThat(result.isCorrected()).isTrue();
        assertThat(result.getCorrectedText()).isEqualTo("uma barraca para duas pessoas até R$ 2.200");
    }

    @Test
    void unchangedTokenKeepsTypedCase() {
        assertThat(TurSpellCheckGuard.guardedText("Barraca Quechua mochla", "barraca quechua mochila"))
                .isEqualTo("Barraca Quechua mochila");
    }

    @Test
    void caseOnlyChangeIsNoCorrection() {
        assertThat(TurSpellCheckGuard.guardedText("Barraca Quechua", "barraca quechua")).isNull();
    }

    @Test
    void differentTokenCountKeepsEngineCorrection() {
        assertThat(TurSpellCheckGuard.guardedText("sacode dormir", "saco de dormir"))
                .isEqualTo("saco de dormir");
        assertThat(TurSpellCheckGuard.guardedText("a b", "a b")).isNull();
    }

    @Test
    void blankCorrectionIsNoCorrection() {
        assertThat(TurSpellCheckGuard.guardedText("barraca", "")).isNull();
        assertThat(TurSpellCheckGuard.guardedText(null, "barraca")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = { "2.200", "2,200", "R$", "US$", "€", "45L", "42", "1.500,00", "+", "-" })
    void numbersAndSymbolsAreProtected(String token) {
        assertThat(TurSpellCheckGuard.isProtected(token)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "barraca", "até", "guarda-chuva", "d'água", "pessoas?" })
    void wordsAreNotProtected(String token) {
        assertThat(TurSpellCheckGuard.isProtected(token)).isFalse();
    }
}
