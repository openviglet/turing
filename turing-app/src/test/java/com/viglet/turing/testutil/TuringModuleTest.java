/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.testutil;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ApplicationModuleTest.BootstrapMode;
import org.springframework.test.context.TestPropertySource;

/**
 * T1325 / ADR 0005 — marks a module slice integration test: the module of the
 * annotated class's package boots with the modules it declares as dependencies, not
 * the whole application. It goes on the test class itself, since Spring Modulith reads
 * the module from the class that carries it; the class extends
 * {@link AbstractTuringModuleIT} for its own H2 file and an in-memory Artemis.
 *
 * <p>Module verification is off here: {@code TurModuleReportTest} verifies the whole
 * model on every build, without the library jars this bootstrap would count as modules.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApplicationModuleTest(mode = BootstrapMode.DIRECT_DEPENDENCIES, verifyAutomatically = false)
// GraphQL reads every module's schema files but its scalars are wired by sn, so a slice
// leaves the GraphQL endpoint out; the list replaces application.yaml's, so it repeats
// the module tracing exclusion.
@TestPropertySource(properties = {"spring.jmx.enabled=true",
        "spring.autoconfigure.exclude=org.springframework.boot.graphql.autoconfigure.GraphQlAutoConfiguration,"
                + "org.springframework.modulith.observability.autoconfigure.ModuleObservabilityAutoConfiguration"})
public @interface TuringModuleTest {

    /**
     * Declared dependencies the slice also boots. Spring Modulith infers the modules a
     * slice needs from the beans its components take in their constructors; one a
     * {@code @Bean} method takes as a parameter (storage's tenant context, say) goes
     * unseen and is named here.
     */
    @AliasFor(annotation = ApplicationModuleTest.class, attribute = "extraIncludes")
    String[] extraIncludes() default {};
}
