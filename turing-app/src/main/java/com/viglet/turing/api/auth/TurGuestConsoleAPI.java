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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.persistence.model.auth.TurPrivilege;
import com.viglet.turing.persistence.repository.auth.TurPrivilegeRepository;
import com.viglet.turing.persistence.repository.auth.TurUserRepository;
import com.viglet.turing.properties.TurConfigProperties;
import com.viglet.turing.properties.TurGuestConsoleProperty;
import com.viglet.turing.spring.security.auth.TurGuestReadOnlyFilter;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * T1184 — opens the admin console as a read-only guest, without an account: a
 * demo host turns {@code turing.guest-console.enabled} on, and the login page
 * offers it. The session is signed in as the guest user with
 * {@link TurGuestReadOnlyFilter#GUEST_AUTHORITY} and the view privileges only,
 * never {@code ROLE_ADMIN}, and {@link TurGuestReadOnlyFilter} refuses every
 * request of it that would save anything.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@RestController
@RequestMapping("/api/v2/guest")
@Tag(name = "Guest console", description = "Read-only console for evaluators, when the host enables it")
public class TurGuestConsoleAPI {

    private final TurGuestConsoleProperty guestConsoleProperty;
    private final TurConfigProperties turConfigProperties;
    private final TurUserRepository turUserRepository;
    private final TurPrivilegeRepository turPrivilegeRepository;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public TurGuestConsoleAPI(TurGuestConsoleProperty guestConsoleProperty, TurConfigProperties turConfigProperties,
            TurUserRepository turUserRepository, TurPrivilegeRepository turPrivilegeRepository) {
        this.guestConsoleProperty = guestConsoleProperty;
        this.turConfigProperties = turConfigProperties;
        this.turUserRepository = turUserRepository;
        this.turPrivilegeRepository = turPrivilegeRepository;
    }

    /** Whether this host offers the guest console. */
    public record GuestConsoleStatus(boolean enabled) {
    }

    @GetMapping("/status")
    public GuestConsoleStatus status() {
        return new GuestConsoleStatus(available());
    }

    /**
     * Signs the session in as the guest and sends it to the console; 404 where
     * the host does not offer the guest console.
     */
    @GetMapping("/login")
    public ResponseEntity<Void> login(HttpServletRequest request, HttpServletResponse response) {
        if (!available()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(TurGuestReadOnlyFilter.GUEST_AUTHORITY));
        turPrivilegeRepository.findAll().stream()
                .map(TurPrivilege::getName)
                .filter(name -> name.endsWith("_VIEW") || "READ_PRIVILEGE".equals(name))
                .forEach(name -> authorities.add(new SimpleGrantedAuthority(name)));
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                guestConsoleProperty.getUsername(), null, authorities);

        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/")).build();
    }

    /** Offered only when enabled, under local authentication, and with the guest user seeded. */
    private boolean available() {
        return guestConsoleProperty.isEnabled() && !turConfigProperties.isKeycloak()
                && turUserRepository.findByUsername(guestConsoleProperty.getUsername()) != null;
    }
}
