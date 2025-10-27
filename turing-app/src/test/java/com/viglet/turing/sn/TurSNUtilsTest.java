package com.viglet.turing.sn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TurSNUtilsTest {

    @Test
    void testIsTrue() {
        assertTrue(TurSNUtils.isTrue(1));
        assertFalse(TurSNUtils.isTrue(0));
        assertFalse(TurSNUtils.isTrue(2));
        assertFalse(TurSNUtils.isTrue(null));
    }

    @Test
    void testConstants() {
        assertEquals("turing_entity", TurSNUtils.TURING_ENTITY);
        assertEquals("en", TurSNUtils.DEFAULT_LANGUAGE);
        assertEquals("url", TurSNUtils.URL);
    }
}
