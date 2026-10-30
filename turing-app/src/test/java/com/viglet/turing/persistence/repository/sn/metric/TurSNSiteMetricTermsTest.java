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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * T1179 — the grouping key drops diacritics and never a letter; the shown
 * spelling is the one typed most.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.7
 */
class TurSNSiteMetricTermsTest {

    @Test
    void foldingDropsDiacriticsOnly() {
        assertThat(TurSNSiteMetricTerms.fold("  Abrigo  de Emergência ")).isEqualTo("abrigo de emergencia");
        assertThat(TurSNSiteMetricTerms.fold("jaqueta até R$ 400")).isEqualTo("jaqueta ate r$ 400");
        assertThat(TurSNSiteMetricTerms.fold("Палатка")).isEqualTo("палатка");
        assertThat(TurSNSiteMetricTerms.fold("テント")).isEqualTo("テント");
        assertThat(TurSNSiteMetricTerms.fold(null)).isNull();
    }

    @Test
    void theMostTypedSpellingIsShown() {
        Map<String, String> typed = TurSNSiteMetricTerms.typedSpellings(List.of(
                new TurSNSiteMetricTermSpelling("emergencia", "emergência", 3),
                new TurSNSiteMetricTermSpelling("emergencia", "emergencia", 1),
                new TurSNSiteMetricTermSpelling("tenda", "tenda", 1),
                new TurSNSiteMetricTermSpelling("tenda", "Tenda ", 2),
                new TurSNSiteMetricTermSpelling("ate", "até", 1),
                new TurSNSiteMetricTermSpelling("ate", "ate", 1)));

        assertThat(typed).containsEntry("emergencia", "emergência")
                .containsEntry("tenda", "Tenda")
                .containsEntry("ate", "até");
    }
}
