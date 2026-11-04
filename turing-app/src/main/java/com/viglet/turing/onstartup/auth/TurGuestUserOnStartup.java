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

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.viglet.turing.persistence.model.auth.TurUser;
import com.viglet.turing.persistence.repository.auth.TurUserRepository;
import com.viglet.turing.properties.TurGuestConsoleProperty;

import lombok.extern.slf4j.Slf4j;

/**
 * T1184 — seeds the guest console's user on every start where the guest console
 * is on and the user is missing, so an install that turns it on later gets it
 * too. The user has no password and belongs to no group: it is reached only
 * through {@code /api/v2/guest/login}, never through the login form.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Slf4j
@Component
public class TurGuestUserOnStartup implements ApplicationRunner {

    private final TurGuestConsoleProperty guestConsoleProperty;
    private final TurUserRepository turUserRepository;

    public TurGuestUserOnStartup(TurGuestConsoleProperty guestConsoleProperty,
            TurUserRepository turUserRepository) {
        this.guestConsoleProperty = guestConsoleProperty;
        this.turUserRepository = turUserRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String username = guestConsoleProperty.getUsername();
        if (!guestConsoleProperty.isEnabled() || !StringUtils.hasText(username)
                || turUserRepository.findByUsername(username) != null) {
            return;
        }
        turUserRepository.save(TurUser.builder()
                .username(username)
                .firstName("Guest")
                .lastName("")
                .realm("default")
                .enabled(1)
                .build());
        log.info("[GuestConsole] Seeded the read-only guest user '{}'", username);
    }
}
