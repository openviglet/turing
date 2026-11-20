package com.viglet.turing.sn.persistence.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TurSNSiteFacetSortEnumTest {

    @Test
    void shouldContainExpectedSortOptions() {
        assertArrayEquals(new TurSNSiteFacetSortEnum[] {
                TurSNSiteFacetSortEnum.COUNT,
                TurSNSiteFacetSortEnum.ALPHABETICAL
        }, TurSNSiteFacetSortEnum.values());
        assertEquals(TurSNSiteFacetSortEnum.COUNT, TurSNSiteFacetSortEnum.valueOf("COUNT"));
    }
}
