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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.viglet.turing.persistence.model.sn.metric.TurSNSiteMetricAccess;
import com.viglet.turing.persistence.model.sn.metric.TurSNSiteMetricClick;

/**
 * T1298 — e-mails, CPF/CNPJ and long digit runs never reach the search log; the
 * words around them, dates and long queries do.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurSearchTermPrivacyTest {

    @AfterEach
    void restore() {
        TurSearchTermPrivacy.setEnabled(true);
    }

    @Test
    void personalDataIsMaskedAndTheRestKept() {
        assertThat(TurSearchTermPrivacy.mask("20231234")).isEqualTo("***");
        assertThat(TurSearchTermPrivacy.mask("123.456.789-09")).isEqualTo("***");
        assertThat(TurSearchTermPrivacy.mask("12.345.678/0001-95")).isEqualTo("***");
        assertThat(TurSearchTermPrivacy.mask("aluno@fespsp.org.br")).isEqualTo("***");
        assertThat(TurSearchTermPrivacy.mask("boletim 20231234")).isEqualTo("boletim ***");
        assertThat(TurSearchTermPrivacy.mask("(11) 98765 4321")).isEqualTo("(11) ***");
    }

    @Test
    void ordinarySearchesAreLeftAlone() {
        assertThat(TurSearchTermPrivacy.mask("2026/11/20")).isEqualTo("2026/11/20");
        assertThat(TurSearchTermPrivacy.mask("agenda 20-11-2026")).isEqualTo("agenda 20-11-2026");
        assertThat(TurSearchTermPrivacy.mask("iphone 15 pro max 256")).isEqualTo("iphone 15 pro max 256");
        String longQuery = "como faço a rematrícula do segundo semestre de 2026";
        assertThat(TurSearchTermPrivacy.mask(longQuery)).isEqualTo(longQuery);
        assertThat(TurSearchTermPrivacy.mask(null)).isNull();
    }

    @Test
    void turnedOffTheTermIsStoredAsTyped() {
        TurSearchTermPrivacy.setEnabled(false);
        assertThat(TurSearchTermPrivacy.mask("9788535914849")).isEqualTo("9788535914849");
    }

    @Test
    void aSearchAndAClickStoreTheMaskedTerm() {
        TurSNSiteMetricAccess access = new TurSNSiteMetricAccess();
        access.setTerm("123.456.789-09");
        assertThat(access.getTerm()).isEqualTo("***");
        assertThat(access.getSanatizedTerm()).isEqualTo("***");

        TurSNSiteMetricClick click = new TurSNSiteMetricClick();
        click.setTerm("aluno@fespsp.org.br horários");
        assertThat(click.getTerm()).isEqualTo("*** horários");
    }
}
