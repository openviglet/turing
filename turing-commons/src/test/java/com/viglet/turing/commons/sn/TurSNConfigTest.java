/*
 * Copyright (C) 2016-2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <https://www.gnu.org/licenses/>.
 */

package com.viglet.turing.commons.sn;

import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TurSNConfig.
 *
 * @author Alexandre Oliveira
 * @since 0.3.4
 */
class TurSNConfigTest {

    @Test
    void testDefaultConstructor() {
        TurSNConfig config = new TurSNConfig();
        
        // Default boolean value should be false
        assertThat(config.isHlEnabled()).isFalse();
    }

    @Test
    void testSetterAndGetter() {
        TurSNConfig config = new TurSNConfig();
        
        config.setHlEnabled(true);
        
        assertThat(config.isHlEnabled()).isTrue();
    }

    @Test
    void testSetterAndGetterFalse() {
        TurSNConfig config = new TurSNConfig();
        
        config.setHlEnabled(false);
        
        assertThat(config.isHlEnabled()).isFalse();
    }

    @Test
    void testToggleHlEnabled() {
        TurSNConfig config = new TurSNConfig();
        
        // Start with default (false)
        assertThat(config.isHlEnabled()).isFalse();
        
        // Toggle to true
        config.setHlEnabled(true);
        assertThat(config.isHlEnabled()).isTrue();
        
        // Toggle back to false
        config.setHlEnabled(false);
        assertThat(config.isHlEnabled()).isFalse();
    }

    @Test
    void testImplementsSerializable() {
        TurSNConfig config = new TurSNConfig();
        
        assertThat(config).isInstanceOf(Serializable.class);
    }
}