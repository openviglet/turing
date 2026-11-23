/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.modulith;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

import com.viglet.turing.TuringES;

/**
 * T1322 / ADR 0005 — every module of the application declares itself: its base
 * package carries {@code @ApplicationModule}, and every module but the open shared
 * kernel lists the modules it may depend on. A new top-level package under
 * {@code com.viglet.turing} without a declaration fails here.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurModuleDeclarationTest {

    /**
     * The library jars that share the base package (turing-commons, turing-java-sdk,
     * turing-spring); they are not modules of this application and are declared
     * nowhere here.
     */
    private static final Set<String> LIBRARIES = Set.of("commons", "se", "client", "spring");

    @Test
    void everyApplicationModuleDeclaresItsAllowedDependencies() {
        ApplicationModules modules = ApplicationModules.of(TuringES.class);
        List<String> undeclared = new ArrayList<>();
        List<String> checked = new ArrayList<>();

        for (var module : modules) {
            String id = module.getIdentifier().toString();
            if (LIBRARIES.contains(id)) {
                continue;
            }
            checked.add(id);
            ApplicationModule declaration = declarationOf(module.getBasePackage().getName());
            if (declaration == null) {
                undeclared.add(id + " (no @ApplicationModule)");
            } else if (declaration.type() != ApplicationModule.Type.OPEN
                    && declaration.allowedDependencies().length == 0) {
                undeclared.add(id + " (no allowedDependencies)");
            }
        }

        assertThat(checked).contains("shared", "sn", "agent", "exchange");
        assertThat(undeclared).isEmpty();
    }

    private static ApplicationModule declarationOf(String basePackage) {
        try {
            return Class.forName(basePackage + ".package-info").getAnnotation(ApplicationModule.class);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
