package com.viglet.turing.sn.persistence.model.field;

import com.viglet.turing.searchengine.TurSNSiteFacetFieldEnum;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TurSNSiteFacetFieldEnumTest {

    @Test
    void shouldContainExpectedFacetFieldOperators() {
        assertArrayEquals(
                new TurSNSiteFacetFieldEnum[] { TurSNSiteFacetFieldEnum.DEFAULT, TurSNSiteFacetFieldEnum.AND,
                        TurSNSiteFacetFieldEnum.OR },
                TurSNSiteFacetFieldEnum.values());
        assertEquals(TurSNSiteFacetFieldEnum.AND, TurSNSiteFacetFieldEnum.valueOf("AND"));
    }
}
