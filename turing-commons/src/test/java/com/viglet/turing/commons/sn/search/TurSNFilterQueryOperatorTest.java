package com.viglet.turing.commons.sn.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TurSNFilterQueryOperatorTest {

    @Test
    void shouldExposeExpectedOperatorStrings() {
        assertThat(TurSNFilterQueryOperator.AND.toString()).isEqualTo("AND");
        assertThat(TurSNFilterQueryOperator.OR.toString()).isEqualTo("OR");
        assertThat(TurSNFilterQueryOperator.NONE.toString()).isEqualTo("NONE");
    }
}
