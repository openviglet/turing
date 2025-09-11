/*
 * Copyright (C) 2016-2024 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.viglet.turing.connector.constant;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for TurConnectorConstants.
 *
 * @author Alexandre Oliveira
 * @since 2025.3
 */
class TurConnectorConstantsTest {

    @Test
    void testConnectorIndexingQueueConstant() {
        assertThat(TurConnectorConstants.CONNECTOR_INDEXING_QUEUE)
                .isEqualTo("connector-indexing.queue");
    }

    @Test
    void testPrivateConstructorThrowsException() {
        assertThatThrownBy(() -> {
            Constructor<TurConnectorConstants> constructor = 
                    TurConnectorConstants.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        })
        .isInstanceOf(InvocationTargetException.class)
        .hasCauseInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Connector Constants class");
    }

    @Test
    void testConstructorIsPrivate() throws NoSuchMethodException {
        Constructor<TurConnectorConstants> constructor = 
                TurConnectorConstants.class.getDeclaredConstructor();
        
        assertThat(constructor.getModifiers()).isEqualTo(2); // 2 = private modifier
    }

    @Test
    void testConstantsAreStatic() throws NoSuchFieldException {
        assertThat(TurConnectorConstants.class.getDeclaredField("CONNECTOR_INDEXING_QUEUE")
                .getModifiers() & 8).isEqualTo(8); // 8 = static modifier
    }

    @Test
    void testConstantsAreFinal() throws NoSuchFieldException {
        assertThat(TurConnectorConstants.class.getDeclaredField("CONNECTOR_INDEXING_QUEUE")
                .getModifiers() & 16).isEqualTo(16); // 16 = final modifier
    }

    @Test
    void testConstantsArePublic() throws NoSuchFieldException {
        assertThat(TurConnectorConstants.class.getDeclaredField("CONNECTOR_INDEXING_QUEUE")
                .getModifiers() & 1).isEqualTo(1); // 1 = public modifier
    }

    @Test
    void testQueueNameFormat() {
        String queueName = TurConnectorConstants.CONNECTOR_INDEXING_QUEUE;
        
        assertThat(queueName).contains("connector");
        assertThat(queueName).contains("indexing");
        assertThat(queueName).contains("queue");
        assertThat(queueName).matches("^[a-z\\-\\.]+$"); // Only lowercase, hyphens and dots
    }

    @Test
    void testConstantImmutability() {
        // Verify that the constant value cannot be changed (compilation check)
        String originalValue = TurConnectorConstants.CONNECTOR_INDEXING_QUEUE;
        
        // Try to use the constant multiple times to ensure consistency
        assertThat(TurConnectorConstants.CONNECTOR_INDEXING_QUEUE).isEqualTo(originalValue);
        assertThat(TurConnectorConstants.CONNECTOR_INDEXING_QUEUE).isEqualTo(originalValue);
        assertThat(TurConnectorConstants.CONNECTOR_INDEXING_QUEUE).isEqualTo("connector-indexing.queue");
    }
}