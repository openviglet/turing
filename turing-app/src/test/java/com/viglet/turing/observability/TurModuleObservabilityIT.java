/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import com.viglet.turing.testutil.AbstractTuringSpringIT;

/**
 * T1327 — with {@code turing.observability.modules.enabled=true} the application starts
 * with Spring Modulith's module observability: the modules' API beans are wrapped by
 * the interceptor that turns a call entering a module into an observation, and a
 * servlet filter is not, so Spring Security can still order it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@TestPropertySource(properties = TurModuleObservabilityConfig.ENABLED + "=true")
class TurModuleObservabilityIT extends AbstractTuringSpringIT {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private com.viglet.turing.auth.security.auth.TurAuthTokenHeaderFilter authTokenFilter;

    @Test
    void aServletFilterKeepsItsOwnClassSoSecurityCanOrderIt() {
        assertThat(authTokenFilter.getClass())
                .isEqualTo(com.viglet.turing.auth.security.auth.TurAuthTokenHeaderFilter.class);
    }

    @Test
    void theModulesApiBeansAreWrappedForTracing() {
        assertThat(context.containsBean("moduleTracingBeanPostProcessor")).isTrue();
        List<String> wrapped = Arrays.stream(context.getBeanDefinitionNames())
                .filter(name -> isTraced(context.getBean(name)))
                .sorted()
                .toList();
        assertThat(wrapped).as("the beans the modules expose").hasSizeGreaterThan(100);
    }

    private static boolean isTraced(Object bean) {
        return bean instanceof Advised advised && Arrays.stream(advised.getAdvisors())
                .anyMatch(advisor -> advisor.getAdvice().getClass().getSimpleName().equals("ModuleEntryInterceptor"));
    }
}
