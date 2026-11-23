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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

/**
 * T1306 / T1323 / ADR 0005 — the module gate. {@link ApplicationModules#verify()}
 * fails the build on a cycle between modules, on a call into another module's
 * internal package, and on a dependency a module does not declare. There is no
 * exemption list: a violation is fixed, or ADR 0005 is changed and the module's
 * declaration with it. The module list is also written to
 * {@code target/modulith/report.txt}.
 *
 * <p>When the report was added (T1306) it counted 27,039 violations over 33 modules;
 * the Block CZ moves brought the application's own modules to none.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurModuleReportTest {

    @Test
    void theModulesHaveNoCycleNoInternalAccessAndNoUndeclaredDependency() throws IOException {
        ApplicationModules modules = TurModules.model();

        StringBuilder report = new StringBuilder();
        report.append("Modules: ").append(modules.stream().count()).append('\n');
        for (ApplicationModule module : modules) {
            report.append("  ").append(module.getIdentifier()).append(" (")
                    .append(module.getBasePackage().getName()).append(")\n");
        }
        Path out = Path.of("target", "modulith", "report.txt");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report);

        assertThat(modules.stream().map(module -> module.getIdentifier().toString()))
                .as("the library jars are not modules of the application")
                .doesNotContainAnyElementsOf(TurModules.LIBRARY_PACKAGES)
                .contains("shared", "sn", "agent", "exchange");
        modules.verify();
    }
}
