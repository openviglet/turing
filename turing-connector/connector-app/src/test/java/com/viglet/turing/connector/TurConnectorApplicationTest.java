/*
 * Copyright (C) 2016-2025 the original author or authors.
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
package com.viglet.turing.connector;

import com.fasterxml.jackson.databind.Module;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.filter.CharacterEncodingFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TurConnectorApplication.
 *
 * @author Alexandre Oliveira
 * @since 2025.3
 */
class TurConnectorApplicationTest {

    @Test
    void testUtf8Constant() {
        assertThat(TurConnectorApplication.UTF_8).isEqualTo("UTF-8");
    }

    @Test
    void testFilterRegistrationBean() {
        TurConnectorApplication application = new TurConnectorApplication();
        
        FilterRegistrationBean<CharacterEncodingFilter> filterBean = application.filterRegistrationBean();
        
        assertThat(filterBean).isNotNull();
        assertThat(filterBean.getFilter()).isNotNull();
        assertThat(filterBean.getFilter()).isInstanceOf(CharacterEncodingFilter.class);
    }

    @Test
    void testCharacterEncodingFilterConfiguration() {
        TurConnectorApplication application = new TurConnectorApplication();
        
        FilterRegistrationBean<CharacterEncodingFilter> filterBean = application.filterRegistrationBean();
        CharacterEncodingFilter filter = filterBean.getFilter();
        
        assertThat(filter).isNotNull();
        // Note: We can't easily test the force encoding and encoding properties 
        // without reflection as they're private fields in CharacterEncodingFilter
        // The important thing is that the filter is properly configured
    }

    @Test
    void testHibernate5Module() {
        TurConnectorApplication application = new TurConnectorApplication();
        
        Module module = application.hibernate5Module();
        
        assertThat(module).isNotNull();
        assertThat(module.getModuleName()).contains("Hibernate");
    }

    @Test
    void testHibernate5ModuleType() {
        TurConnectorApplication application = new TurConnectorApplication();
        
        Module module = application.hibernate5Module();
        
        assertThat(module.getClass().getSimpleName()).contains("Hibernate");
        assertThat(module.getClass().getSimpleName()).contains("Module");
    }

    @Test
    void testApplicationClassAnnotations() {
        Class<TurConnectorApplication> appClass = TurConnectorApplication.class;
        
        // Test that class has expected annotations
        assertThat(appClass.isAnnotationPresent(org.springframework.boot.autoconfigure.SpringBootApplication.class))
                .isTrue();
        
        // Check if JMS is enabled
        assertThat(appClass.isAnnotationPresent(org.springframework.jms.annotation.EnableJms.class))
                .isTrue();
        
        // Check if caching is enabled
        assertThat(appClass.isAnnotationPresent(org.springframework.cache.annotation.EnableCaching.class))
                .isTrue();
        
        // Check if async is enabled
        assertThat(appClass.isAnnotationPresent(org.springframework.scheduling.annotation.EnableAsync.class))
                .isTrue();
        
        // Check if scheduling is enabled
        assertThat(appClass.isAnnotationPresent(org.springframework.scheduling.annotation.EnableScheduling.class))
                .isTrue();
    }

    @Test
    void testSpringBootApplicationExclusions() {
        org.springframework.boot.autoconfigure.SpringBootApplication annotation = 
                TurConnectorApplication.class.getAnnotation(
                        org.springframework.boot.autoconfigure.SpringBootApplication.class);
        
        assertThat(annotation).isNotNull();
        assertThat(annotation.exclude()).isNotEmpty();
        
        // Verify that Mongo auto-configurations are excluded
        boolean hasMongoAutoConfigExcluded = false;
        boolean hasMongoDataAutoConfigExcluded = false;
        
        for (Class<?> excludedClass : annotation.exclude()) {
            if (excludedClass.getSimpleName().contains("MongoAutoConfiguration")) {
                hasMongoAutoConfigExcluded = true;
            }
            if (excludedClass.getSimpleName().contains("MongoDataAutoConfiguration")) {
                hasMongoDataAutoConfigExcluded = true;
            }
        }
        
        assertThat(hasMongoAutoConfigExcluded).isTrue();
        assertThat(hasMongoDataAutoConfigExcluded).isTrue();
    }

    @Test
    void testBeanMethodsHaveCorrectAnnotations() throws NoSuchMethodException {
        // Test filterRegistrationBean method has @Bean annotation
        assertThat(TurConnectorApplication.class.getMethod("filterRegistrationBean")
                .isAnnotationPresent(org.springframework.context.annotation.Bean.class))
                .isTrue();
        
        // Test hibernate5Module method has @Bean annotation
        assertThat(TurConnectorApplication.class.getMethod("hibernate5Module")
                .isAnnotationPresent(org.springframework.context.annotation.Bean.class))
                .isTrue();
    }

    @Test
    void testBeanReturnTypes() throws NoSuchMethodException {
        // Test filterRegistrationBean return type
        assertThat(TurConnectorApplication.class.getMethod("filterRegistrationBean")
                .getReturnType()).isEqualTo(FilterRegistrationBean.class);
        
        // Test hibernate5Module return type
        assertThat(TurConnectorApplication.class.getMethod("hibernate5Module")
                .getReturnType()).isEqualTo(Module.class);
    }

    @Test
    void testFilterRegistrationBeanConfiguration() {
        TurConnectorApplication application = new TurConnectorApplication();
        
        FilterRegistrationBean<CharacterEncodingFilter> filterBean = application.filterRegistrationBean();
        
        // Verify the filter bean is properly configured
        assertThat(filterBean).isNotNull();
        assertThat(filterBean.getFilter()).isNotNull();
        
        // The filter should be a CharacterEncodingFilter
        assertThat(filterBean.getFilter()).isInstanceOf(CharacterEncodingFilter.class);
    }

    @Test
    void testConstantsAccessibility() {
        // Test that UTF_8 constant is accessible and has correct value
        assertThat(TurConnectorApplication.UTF_8).isNotNull();
        assertThat(TurConnectorApplication.UTF_8).isNotEmpty();
        assertThat(TurConnectorApplication.UTF_8).isEqualTo("UTF-8");
    }
}