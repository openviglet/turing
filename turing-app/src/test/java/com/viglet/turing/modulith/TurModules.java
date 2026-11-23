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

import java.util.List;

import org.springframework.modulith.core.ApplicationModules;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.viglet.turing.TuringES;

/**
 * T1323 / ADR 0005 — the module model every modulith test reads: the application's own
 * modules, without the library jars that share the {@code com.viglet.turing} base
 * package (turing-commons' {@code commons} and {@code se}, turing-java-sdk's
 * {@code client}, turing-spring's {@code spring}). Those are dependencies of the
 * application, not modules of it, so they are not imported at all and calls into them
 * are not module crossings.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
final class TurModules {

    /** The library packages, as they appear in a class file's location. */
    static final List<String> LIBRARY_PACKAGES = List.of("commons", "se", "client", "spring");

    private static final ImportOption WITHOUT_LIBRARIES = TurModules::isApplicationClass;

    private TurModules() {
    }

    static ApplicationModules model() {
        return ApplicationModules.of(TuringES.class, WITHOUT_LIBRARIES);
    }

    private static final ImportOption NOT_TESTS = new ImportOption.DoNotIncludeTests();

    private static boolean isApplicationClass(Location location) {
        return NOT_TESTS.includes(location) && LIBRARY_PACKAGES.stream()
                .noneMatch(library -> location.contains("/com/viglet/turing/" + library + "/"));
    }
}
