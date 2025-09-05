/*
 * Copyright (C) 2016-2023 the original author or authors.
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

package com.viglet.turing.spring.security;

import com.viglet.turing.properties.TurConfigProperties;
import com.viglet.turing.spring.security.auth.TurAuthTokenHeaderFilter;
import com.viglet.turing.spring.security.auth.TurLogoutHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@Profile("production")
@EnableMethodSecurity(securedEnabled = true)
@ComponentScan(basePackageClasses = TurCustomUserDetailsService.class)
public class TurSecurityConfigProduction {
    public static final String ERROR_PATH = "/error/**";
    @Autowired
    private UserDetailsService userDetailsService;
    @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri:''}")
    private String issuerUri;
    @Value("${spring.security.oauth2.client.registration.keycloak.client-id:''}")
    private String clientId;
    @Value("${turing.url:'http://localhost:2700'}")
    private String turingUrl;
    PathPatternRequestMatcher.Builder mvc = PathPatternRequestMatcher.withDefaults();

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    TurAuthTokenHeaderFilter turAuthTokenHeaderFilter,
                                    TurLogoutHandler turLogoutHandler,
                                    TurConfigProperties turConfigProperties,
                                    TurAuthenticationEntryPoint turAuthenticationEntryPoint) throws Exception {

        http.headers(header -> header.frameOptions(
                frameOptions -> frameOptions.disable().cacheControl(HeadersConfigurer.CacheControlConfig::disable)));
        http.cors(Customizer.withDefaults());
        http.addFilterBefore(turAuthTokenHeaderFilter, BasicAuthenticationFilter.class);
        http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
        http.csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new TurSpaCsrfTokenRequestHandler())
                        .ignoringRequestMatchers(
                                mvc.matcher("/api/genai/chat"),
                                mvc.matcher("/api/sn/**"),
                                mvc.matcher(ERROR_PATH),
                                mvc.matcher("/logout"),
                                mvc.matcher("/api/ocr/**"),
                                mvc.matcher("/api/genai/**"),
                                mvc.matcher("/api/v2/guest/**"),
                                mvc.matcher("/h2/**")))
                .addFilterAfter(new TurCsrfCookieFilter(), BasicAuthenticationFilter.class);
        if (turConfigProperties.isKeycloak()) {
            String keycloakUrlFormat =
                    String.format("%s/protocol/openid-connect/logout?client_id=%s&post_logout_redirect_uri=%s",
                            issuerUri, clientId, turingUrl);
            http.oauth2Login(withDefaults());
            http.authorizeHttpRequests(authorizeRequests -> {
                authorizeRequests.requestMatchers(
                        mvc.matcher(ERROR_PATH),
                        mvc.matcher("/api/discovery"),
                        mvc.matcher("/assets/**"),
                        mvc.matcher("/favicon.ico"),
                        mvc.matcher("/*.png"),
                        mvc.matcher("/manifest.json"),
                        mvc.matcher("/swagger-resources/**"),
                        mvc.matcher("/browserconfig.xml"),
                        mvc.matcher("/api/sn/*/ac"),
                        mvc.matcher("/api/sn/*/search"),
                        mvc.matcher("/api/sn/*/search/**"),
                        mvc.matcher("/api/sn/*/query"),
                        mvc.matcher("/api/sn/*/query/**"),
                        mvc.matcher("/api/sn/*/chat"),
                        mvc.matcher("/api/sn/*/chat/**"),
                        mvc.matcher("/api/sn/*/*/spell-check")).permitAll();
                authorizeRequests.anyRequest().authenticated();
            });
            http.logout(logout -> logout.addLogoutHandler(turLogoutHandler)
                    .logoutSuccessUrl(keycloakUrlFormat));
        } else {
            http.httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(turAuthenticationEntryPoint))
                    .authorizeHttpRequests(authorizeRequests -> {
                        authorizeRequests.requestMatchers(
                                mvc.matcher(ERROR_PATH),
                                mvc.matcher("/api/discovery"),
                                mvc.matcher("/logout"),
                                mvc.matcher("/index.html"),
                                mvc.matcher("/welcome/**"),
                                mvc.matcher("/login/**"),
                                mvc.matcher("/admin/**"),
                                mvc.matcher("/"),
                                mvc.matcher("/assets/**"),
                                mvc.matcher("/swagger-resources/**"),
                                mvc.matcher("/sn/**"),
                                mvc.matcher("/fonts/**"),
                                mvc.matcher("/api/sn/*/ac"),
                                mvc.matcher("/api/sn/*/search"),
                                mvc.matcher("/api/sn/*/search/**"),
                                mvc.matcher("/api/sn/*/query"),
                                mvc.matcher("/api/sn/*/query/**"),
                                mvc.matcher("/api/sn/*/chat"),
                                mvc.matcher("/api/sn/*/chat/**"),
                                mvc.matcher("/api/sn/*/*/spell-check"),
                                mvc.matcher("/favicon.ico"),
                                mvc.matcher("/*.png"),
                                mvc.matcher("/manifest.json"),
                                mvc.matcher("/browserconfig.xml"),
                                mvc.matcher("/console/**"),
                                mvc.matcher("/api/v2/guest/**")).permitAll();
                        authorizeRequests.anyRequest().authenticated();

                    });
            http.logout(logout -> logout.addLogoutHandler(turLogoutHandler).logoutSuccessUrl("/"));
        }
        return http.build();
    }

    @Bean
    WebSecurityCustomizer webSecurityCustomizer() {
        return web ->
            web.httpFirewall(allowUrlEncodedSlaturHttpFirewall()).ignoring().requestMatchers(mvc.matcher("/h2/**"));
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService);
    }

    @Bean(name = "passwordEncoder")
    PasswordEncoder passwordencoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    HttpFirewall allowUrlEncodedSlaturHttpFirewall() {
        // Allow double slash in URL
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        return firewall;
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("ROLE_ADMIN > ROLE_STAFF \n ROLE_STAFF > ROLE_USER");
    }
    @Bean
    public DefaultWebSecurityExpressionHandler customWebSecurityExpressionHandler() {
        DefaultWebSecurityExpressionHandler expressionHandler = new DefaultWebSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy());
        return expressionHandler;
    }

}
