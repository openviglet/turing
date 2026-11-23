/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * ADR 0005 — the auth module (layer 2): users, groups, roles, Keycloak and the login
 * endpoints. Its API is this package plus the packages marked {@code @NamedInterface};
 * every other package is internal. It may depend only on the modules below it in ADR
 * 0005 section 6 (T1322).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@org.springframework.modulith.ApplicationModule(displayName = "Authentication",
        allowedDependencies = {
        "shared", "shared :: *", "system", "system :: *", "tenant", "tenant :: *" })
package com.viglet.turing.auth;
