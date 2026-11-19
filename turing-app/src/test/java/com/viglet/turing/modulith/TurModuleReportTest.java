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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.Violations;

import com.viglet.turing.TuringES;

/**
 * T1306 / ADR 0005 — the module report every Block CZ move is measured against. It
 * builds Spring Modulith's model of the application (each direct sub-package of
 * {@code com.viglet.turing} a module), prints the modules and the violations, and
 * writes them to {@code target/modulith/report.txt}. It fails on nothing: until the
 * verification is switched on (T1323), the count is a measurement, compared with
 * {@link #BASELINE}.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurModuleReportTest {

    /**
     * The violation count when the report was added (T1306): 27,039 over 33 modules,
     * of which 99 are cycles and 26,939 are references to another module's
     * non-exposed types. Two of the 33 are the {@code client} (Java SDK) and
     * {@code commons} jars, which share the base package. A task that moves code says
     * how many it removed; this number is not updated to hide an increase.
     */
    static final int BASELINE = 27_039;

    @Test
    void reportsTheModulesAndTheirViolations() throws IOException {
        ApplicationModules modules = ApplicationModules.of(TuringES.class);
        Violations violations = modules.detectViolations();
        List<String> messages = violations.getMessages();

        StringBuilder report = new StringBuilder();
        report.append("Modules: ").append(modules.stream().count()).append('\n');
        for (ApplicationModule module : modules) {
            report.append("  ").append(module.getIdentifier()).append(" (")
                    .append(module.getBasePackage().getName()).append(")\n");
        }
        report.append("Violations: ").append(messages.size());
        if (BASELINE >= 0) {
            report.append(" (baseline ").append(BASELINE).append(')');
        }
        report.append('\n');
        messages.forEach(message -> report.append("- ").append(message.replace('\n', ' ')).append('\n'));

        Path out = Path.of("target", "modulith", "report.txt");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report);
        System.out.println("[T1306] module report: " + modules.stream().count() + " modules, "
                + messages.size() + " violations, written to " + out.toAbsolutePath());

        assertThat(modules.stream().count()).isPositive();
    }
}
