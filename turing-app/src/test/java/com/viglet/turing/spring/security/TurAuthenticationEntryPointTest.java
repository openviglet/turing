package com.viglet.turing.spring.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TurAuthenticationEntryPointTest {

    @Test
    void shouldConfigureRealmNameAsTuring() {
        TurAuthenticationEntryPoint entryPoint = new TurAuthenticationEntryPoint();

        assertDoesNotThrow(entryPoint::afterPropertiesSet);
        assertEquals("Turing", entryPoint.getRealmName());
    }

    @Test
    void shouldRemainStableWhenAfterPropertiesSetIsCalledMoreThanOnce() {
        TurAuthenticationEntryPoint entryPoint = new TurAuthenticationEntryPoint();

        assertDoesNotThrow(entryPoint::afterPropertiesSet);
        assertDoesNotThrow(entryPoint::afterPropertiesSet);
        assertEquals("Turing", entryPoint.getRealmName());
    }
}
