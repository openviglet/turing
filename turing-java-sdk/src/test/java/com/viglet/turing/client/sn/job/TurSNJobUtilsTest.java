package com.viglet.turing.client.sn.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

import com.viglet.turing.client.sn.TurSNServer;

class TurSNJobUtilsTest {

    @Test
    void utilityConstructorShouldThrowIllegalStateException() throws Exception {
        Constructor<TurSNJobUtils> constructor = TurSNJobUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        try {
            constructor.newInstance();
            fail("Expected InvocationTargetException");
        } catch (InvocationTargetException ex) {
            assertThat(ex.getCause()).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void shouldReturnFalseWhenImportJobIsNullOrEmpty() {
        TurSNServer turSNServer = mock(TurSNServer.class);

        assertThat(TurSNJobUtils.importItems(null, turSNServer, false)).isFalse();
        assertThat(TurSNJobUtils.importItems(new TurSNJobItems(), turSNServer, false)).isFalse();
    }
}
