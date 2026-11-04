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
package com.viglet.turing.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.viglet.turing.persistence.model.auth.TurPrivilege;
import com.viglet.turing.persistence.model.auth.TurUser;
import com.viglet.turing.persistence.repository.auth.TurPrivilegeRepository;
import com.viglet.turing.persistence.repository.auth.TurUserRepository;
import com.viglet.turing.properties.TurConfigProperties;
import com.viglet.turing.properties.TurGuestConsoleProperty;
import com.viglet.turing.spring.security.auth.TurGuestReadOnlyFilter;

/**
 * T1184 — the guest console signs in as the guest with view privileges only,
 * and only where the host enables it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurGuestConsoleAPITest {

    private final TurGuestConsoleProperty property = new TurGuestConsoleProperty();
    private final TurConfigProperties configProperties = mock(TurConfigProperties.class);
    private final TurUserRepository userRepository = mock(TurUserRepository.class);
    private final TurPrivilegeRepository privilegeRepository = mock(TurPrivilegeRepository.class);
    private final TurGuestConsoleAPI api = new TurGuestConsoleAPI(property, configProperties, userRepository,
            privilegeRepository);

    @BeforeEach
    void setUp() {
        when(userRepository.findByUsername("guest")).thenReturn(new TurUser());
        when(privilegeRepository.findAll()).thenReturn(List.of(new TurPrivilege("SN_VIEW"),
                new TurPrivilege("SN_EDIT"), new TurPrivilege("READ_PRIVILEGE"), new TurPrivilege("LLM_DELETE")));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void isNotOfferedUnlessTheHostEnablesIt() {
        assertThat(api.status().enabled()).isFalse();
        assertThat(api.login(new MockHttpServletRequest(), new MockHttpServletResponse()).getStatusCode().value())
                .isEqualTo(404);

        property.setEnabled(true);
        when(configProperties.isKeycloak()).thenReturn(true);
        assertThat(api.status().enabled()).isFalse();
    }

    @Test
    void signsInAsTheGuestWithTheViewPrivilegesOnly() {
        property.setEnabled(true);
        MockHttpServletRequest request = new MockHttpServletRequest();

        var answer = api.login(request, new MockHttpServletResponse());

        assertThat(answer.getStatusCode().value()).isEqualTo(302);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getName()).isEqualTo("guest");
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder(TurGuestReadOnlyFilter.GUEST_AUTHORITY, "SN_VIEW", "READ_PRIVILEGE");
        assertThat(request.getSession(false)).isNotNull();
    }
}
