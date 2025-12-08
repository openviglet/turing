/*
 * Copyright (C) 2016-2022 the original author or authors.
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

package com.viglet.turing.spring;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
@AutoConfigureAfter(DispatcherServletAutoConfiguration.class)
public class TurStaticResourceConfiguration implements WebMvcConfigurer {
    @Value("${turing.allowedOrigins:localhost}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins(allowedOrigins).allowedMethods("PUT", "DELETE", "GET", "POST")
                .allowCredentials(false).maxAge(3600);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/console").setViewName("forward:/console/browser/index.html");
        registry.addViewController("/console/").setViewName("forward:/console/browser/index.html");
        registry.addViewController("/welcome").setViewName("forward:/welcome/browser/index.html");
        registry.addViewController("/welcome/").setViewName("forward:/welcome/browser/index.html");
        registry.addViewController("/sn/templates").setViewName("forward:/sn/templates/browser/index.html");
        registry.addViewController("/sn/templates/").setViewName("forward:/sn/templates/browser/index.html");
        registry.addViewController("/login").setViewName("forward:/index.html");
        registry.addViewController("/login/").setViewName("forward:/index.html");
        registry.addViewController("/admin").setViewName("forward:/index.html");
        registry.addViewController("/admin/").setViewName("forward:/index.html");

    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(-1);
    }

    @Override
    public void addResourceHandlers(@NotNull ResourceHandlerRegistry registry) {
        registryAngular(registry, "/console");
        registryAngular(registry, "/welcome");
        registryAngular(registry, "/sn/templates");
        registryReact(registry, "/login");
        registryReact(registry, "/admin");
    }

    private static void registryFrontend(ResourceHandlerRegistry registry, String context, String location,
            String path) {
        registry.addResourceHandler(context).addResourceLocations(location)
                .resourceChain(true).addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(@NotNull String resourcePath, @NotNull Resource location)
                            throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);

                        return requestedResource.exists() && requestedResource.isReadable() ? requestedResource
                                : new ClassPathResource(path);
                    }
                });
    }

    private static void registryAngular(ResourceHandlerRegistry registry, String context) {
        registryFrontend(registry, "%s/**".formatted(context), "classpath:/public%s/browser/".formatted(context),
                "/public%s/browser/index.html".formatted(context));
    }

    private static void registryReact(ResourceHandlerRegistry registry, String context) {
        registryFrontend(registry, "%s/**".formatted(context), "classpath:/public/", "/public/index.html");
    }

}