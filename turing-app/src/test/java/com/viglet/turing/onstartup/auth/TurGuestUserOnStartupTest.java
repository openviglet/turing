/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.viglet.turing.onstartup.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.viglet.turing.persistence.model.auth.TurUser;
import com.viglet.turing.persistence.repository.auth.TurUserRepository;
import com.viglet.turing.properties.TurGuestConsoleProperty;

/**
 * T1184 — the guest user is seeded, without a password, only where the guest
 * console is on and the user is missing.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurGuestUserOnStartupTest {

    private final TurGuestConsoleProperty property = new TurGuestConsoleProperty();
    private final TurUserRepository userRepository = mock(TurUserRepository.class);
    private final TurGuestUserOnStartup seeder = new TurGuestUserOnStartup(property, userRepository);

    @Test
    void seedsNothingWhileTheGuestConsoleIsOff() {
        seeder.run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void seedsAPasswordlessGuestOnceWhenOn() {
        property.setEnabled(true);

        seeder.run(null);

        ArgumentCaptor<TurUser> saved = ArgumentCaptor.forClass(TurUser.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("guest");
        assertThat(saved.getValue().getPassword()).isNull();

        when(userRepository.findByUsername("guest")).thenReturn(saved.getValue());
        seeder.run(null);
        verify(userRepository).save(any());
    }
}
