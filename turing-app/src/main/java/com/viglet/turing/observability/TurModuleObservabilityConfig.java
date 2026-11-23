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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import jakarta.servlet.Filter;

/**
 * T1327 / ADR 0005 — per-module tracing, opt-in. With
 * {@code turing.observability.modules.enabled=true}, Spring Modulith's module
 * observability wraps the beans each module exposes, so a call entering a module from
 * another one becomes a Micrometer observation named after the module, and the existing
 * tracing (the OTel bridge) shows how a request's time splits across llm, rag, sn and
 * searchengine instead of one undivided span.
 *
 * <p>Off by default: it proxies every module's API beans and builds the module model at
 * startup. Its auto-configuration is excluded in {@code application.yaml} and imported
 * here only when the flag is on; it also stays off with {@code management.tracing.enabled=false}.
 *
 * <p>Servlet filters are left unwrapped: Spring Security orders a filter by its exact
 * class, which a proxy's subclass does not match, so {@link FiltersStayUnwrapped} marks
 * them as infrastructure beans, which module observability skips.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Configuration(proxyBeanMethods = false)
@Import(TurModuleObservabilityConfig.WhenEnabled.class)
public class TurModuleObservabilityConfig {

    /** The flag that turns per-module tracing on. */
    public static final String ENABLED = "turing.observability.modules.enabled";

    /** Spring Modulith's auto-configuration, which is package-private and so named here. */
    static final String MODULE_OBSERVABILITY =
            "org.springframework.modulith.observability.autoconfigure.ModuleObservabilityAutoConfiguration";

    static class WhenEnabled implements ImportSelector, EnvironmentAware {

        private Environment environment;

        @Override
        public void setEnvironment(Environment environment) {
            this.environment = environment;
        }

        @Override
        public String[] selectImports(AnnotationMetadata metadata) {
            return environment.getProperty(ENABLED, Boolean.class, false)
                    ? new String[] {MODULE_OBSERVABILITY, FiltersStayUnwrapped.class.getName()}
                    : new String[0];
        }
    }

    /** Marks every servlet filter bean as infrastructure, so module observability skips it. */
    static class FiltersStayUnwrapped implements BeanFactoryPostProcessor {

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
            for (String name : beanFactory.getBeanDefinitionNames()) {
                Class<?> type = beanFactory.getType(name, false);
                if (type != null && Filter.class.isAssignableFrom(type)
                        && beanFactory.getBeanDefinition(name) instanceof AbstractBeanDefinition definition) {
                    definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
                }
            }
        }
    }
}
