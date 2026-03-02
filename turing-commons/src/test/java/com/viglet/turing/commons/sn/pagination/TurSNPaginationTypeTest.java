package com.viglet.turing.commons.sn.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TurSNPaginationTypeTest {

    @Test
    void shouldExposeExpectedTypeStrings() {
        assertThat(TurSNPaginationType.FIRST.toString()).isEqualTo("FIRST");
        assertThat(TurSNPaginationType.LAST.toString()).isEqualTo("LAST");
        assertThat(TurSNPaginationType.PREVIOUS.toString()).isEqualTo("PREVIOUS");
        assertThat(TurSNPaginationType.NEXT.toString()).isEqualTo("NEXT");
        assertThat(TurSNPaginationType.CURRENT.toString()).isEqualTo("CURRENT");
        assertThat(TurSNPaginationType.PAGE.toString()).isEqualTo("PAGE");
    }
}
