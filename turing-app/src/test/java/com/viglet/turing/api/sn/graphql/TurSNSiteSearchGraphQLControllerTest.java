/*
 * Copyright (C) 2016-2022 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.viglet.turing.api.sn.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.viglet.turing.commons.sn.bean.TurSNSiteSearchBean;
import com.viglet.turing.commons.sn.search.TurSNFilterQueryOperator;
import com.viglet.turing.commons.sn.search.TurSNSiteSearchContext;
import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
import com.viglet.turing.persistence.repository.sn.TurSNSiteRepository;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.sn.TurSNSearchProcess;

/**
 * Unit tests for TurSNSiteSearchGraphQLController.
 * 
 * @author Alexandre Oliveira
 * @since 0.3.6
 */

class TurSNSiteSearchGraphQLControllerTest {

    @Test
    void testSiteSearchReturnsEmptyWhenLocaleNotSupported() {
        TurSNSearchProcess searchProcess = mock(TurSNSearchProcess.class);
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteFieldExtRepository fieldExtRepository = mock(TurSNSiteFieldExtRepository.class);
        TurSNSiteSearchGraphQLController controller = new TurSNSiteSearchGraphQLController(
                searchProcess, siteRepository, fieldExtRepository);

        when(searchProcess.existsByTurSNSiteAndLanguage("demo", Locale.ENGLISH)).thenReturn(false);

        TurSNSiteSearchBean result = controller.siteSearch("demo", new TurSNSearchParamsInput(), "en");

        assertNotNull(result);
    }

    @Test
    void testSiteSearchReturnsEmptyWhenSiteNotFound() {
        TurSNSearchProcess searchProcess = mock(TurSNSearchProcess.class);
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteFieldExtRepository fieldExtRepository = mock(TurSNSiteFieldExtRepository.class);
        TurSNSiteSearchGraphQLController controller = new TurSNSiteSearchGraphQLController(
                searchProcess, siteRepository, fieldExtRepository);

        when(searchProcess.existsByTurSNSiteAndLanguage("demo", Locale.ENGLISH)).thenReturn(true);
        when(siteRepository.findByName("demo")).thenReturn(Optional.empty());

        TurSNSiteSearchBean result = controller.siteSearch("demo", new TurSNSearchParamsInput(), "en");

        assertNotNull(result);
    }

    @Test
    void testSiteSearchBuildsDefaultContextAndCallsSearch() {
        TurSNSearchProcess searchProcess = mock(TurSNSearchProcess.class);
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteFieldExtRepository fieldExtRepository = mock(TurSNSiteFieldExtRepository.class);
        TurSNSiteSearchGraphQLController controller = new TurSNSiteSearchGraphQLController(
                searchProcess, siteRepository, fieldExtRepository);

        TurSNSite site = new TurSNSite();
        site.setName("demo");
        site.setHl(0);
        site.setHl(1);

        TurSNSiteFieldExt hlField = TurSNSiteFieldExt.builder().name("content").enabled(1).hl(1).build();

        when(searchProcess.existsByTurSNSiteAndLanguage("demo", Locale.ENGLISH)).thenReturn(true);
        when(siteRepository.findByName("demo")).thenReturn(Optional.of(site));
        when(fieldExtRepository.findByTurSNSiteAndHlAndEnabled(site, 1, 1)).thenReturn(List.of(hlField));

        TurSNSiteSearchBean expected = new TurSNSiteSearchBean();
        when(searchProcess.search(any(TurSNSiteSearchContext.class))).thenReturn(expected);

        TurSNSiteSearchBean result = controller.siteSearch("demo", null, null);

        assertSame(expected, result);

        ArgumentCaptor<TurSNSiteSearchContext> contextCaptor = ArgumentCaptor
                .forClass(TurSNSiteSearchContext.class);
        verify(searchProcess).search(contextCaptor.capture());
        TurSNSiteSearchContext context = contextCaptor.getValue();

        assertEquals("demo", context.getSiteName());
        assertNull(context.getLocale());
        assertEquals("*", context.getTurSEParameters().getQuery());
        assertEquals(Integer.valueOf(1), context.getTurSEParameters().getCurrentPage());
        assertEquals(Integer.valueOf(-1), context.getTurSEParameters().getRows());
        assertEquals("relevance", context.getTurSEParameters().getSort());
        assertTrue(context.getTurSNConfig().isHlEnabled());
        assertTrue(context.getUri().toString().contains("/graphql"));
    }

    @Test
    void testSiteSearchUsesLocaleArgumentOverInputLocale() {
        TurSNSearchProcess searchProcess = mock(TurSNSearchProcess.class);
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteFieldExtRepository fieldExtRepository = mock(TurSNSiteFieldExtRepository.class);
        TurSNSiteSearchGraphQLController controller = new TurSNSiteSearchGraphQLController(
                searchProcess, siteRepository, fieldExtRepository);

        TurSNSite site = new TurSNSite();
        site.setName("demo");
        site.setHl(0);

        TurSNSearchParamsInput input = new TurSNSearchParamsInput();
        input.setQ("java");
        input.setLocale("pt_BR");

        when(searchProcess.existsByTurSNSiteAndLanguage("demo", Locale.US)).thenReturn(true);
        when(siteRepository.findByName("demo")).thenReturn(Optional.of(site));
        when(fieldExtRepository.findByTurSNSiteAndHlAndEnabled(site, 1, 1)).thenReturn(List.of());
        when(searchProcess.search(any(TurSNSiteSearchContext.class))).thenReturn(new TurSNSiteSearchBean());

        controller.siteSearch("demo", input, "en_US");

        ArgumentCaptor<TurSNSiteSearchContext> contextCaptor = ArgumentCaptor
                .forClass(TurSNSiteSearchContext.class);
        verify(searchProcess).search(contextCaptor.capture());
        assertEquals(new Locale("pt", "BR"), contextCaptor.getValue().getLocale());
    }

    @Test
    void testSiteSearchInvalidOperatorsFallbackToNone() {
        TurSNSearchProcess searchProcess = mock(TurSNSearchProcess.class);
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteFieldExtRepository fieldExtRepository = mock(TurSNSiteFieldExtRepository.class);
        TurSNSiteSearchGraphQLController controller = new TurSNSiteSearchGraphQLController(
                searchProcess, siteRepository, fieldExtRepository);

        TurSNSite site = new TurSNSite();
        site.setName("demo");
        site.setHl(0);

        TurSNSearchParamsInput input = new TurSNSearchParamsInput();
        input.setQ("java");
        input.setFqOp("INVALID");
        input.setFqiOp("WRONG");

        when(searchProcess.existsByTurSNSiteAndLanguage("demo", Locale.ENGLISH)).thenReturn(true);
        when(siteRepository.findByName("demo")).thenReturn(Optional.of(site));
        when(fieldExtRepository.findByTurSNSiteAndHlAndEnabled(site, 1, 1)).thenReturn(List.of());
        when(searchProcess.search(any(TurSNSiteSearchContext.class))).thenReturn(new TurSNSiteSearchBean());

        controller.siteSearch("demo", input, "en");

        ArgumentCaptor<TurSNSiteSearchContext> contextCaptor = ArgumentCaptor
                .forClass(TurSNSiteSearchContext.class);
        verify(searchProcess).search(contextCaptor.capture());
        assertEquals(TurSNFilterQueryOperator.NONE,
                contextCaptor.getValue().getTurSEParameters().getTurSNFilterParams().getOperator());
        assertEquals(TurSNFilterQueryOperator.NONE,
                contextCaptor.getValue().getTurSEParameters().getTurSNFilterParams().getItemOperator());
    }

    @Test
    void testSiteSearchUsesCurrentRequestContextWhenAvailable() {
        TurSNSearchProcess searchProcess = mock(TurSNSearchProcess.class);
        TurSNSiteRepository siteRepository = mock(TurSNSiteRepository.class);
        TurSNSiteFieldExtRepository fieldExtRepository = mock(TurSNSiteFieldExtRepository.class);
        TurSNSiteSearchGraphQLController controller = new TurSNSiteSearchGraphQLController(
                searchProcess, siteRepository, fieldExtRepository);

        TurSNSite site = new TurSNSite();
        site.setName("demo");
        site.setHl(0);

        when(searchProcess.existsByTurSNSiteAndLanguage("demo", Locale.ENGLISH)).thenReturn(true);
        when(siteRepository.findByName("demo")).thenReturn(Optional.of(site));
        when(fieldExtRepository.findByTurSNSiteAndHlAndEnabled(site, 1, 1)).thenReturn(List.of());
        when(searchProcess.search(any(TurSNSiteSearchContext.class))).thenReturn(new TurSNSiteSearchBean());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/sn/search");
        request.setQueryString("q=graphql");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            TurSNSearchParamsInput input = new TurSNSearchParamsInput();
            input.setQ("graphql");

            controller.siteSearch("demo", input, "en");

            ArgumentCaptor<TurSNSiteSearchContext> contextCaptor = ArgumentCaptor
                    .forClass(TurSNSiteSearchContext.class);
            verify(searchProcess).search(contextCaptor.capture());
            assertTrue(contextCaptor.getValue().getUri().toString().contains("/api/sn/search"));
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void testSearchParamsInputCreation() {
        TurSNSearchParamsInput input = new TurSNSearchParamsInput();

        // Test default values
        assertEquals("*", input.getQ());
        assertEquals(Integer.valueOf(1), input.getP());
        assertEquals("relevance", input.getSort());
        assertEquals(Integer.valueOf(-1), input.getRows());
        assertEquals(Integer.valueOf(1), input.getNfpr());
        assertEquals("NONE", input.getFqOp());
        assertEquals("NONE", input.getFqiOp());

        // Test nullable fields
        assertNull(input.getLocale());
        assertNull(input.getGroup());
        assertNull(input.getFq());
        assertNull(input.getFqAnd());
        assertNull(input.getFqOr());
        assertNull(input.getFl());
    }

    @Test
    void testSearchParamsInputSetters() {
        TurSNSearchParamsInput input = new TurSNSearchParamsInput();

        // Test setters
        input.setQ("test query");
        input.setP(2);
        input.setRows(20);
        input.setSort("date");
        input.setLocale("pt");
        input.setGroup("category");
        input.setNfpr(5);
        input.setFqOp("AND");
        input.setFqiOp("OR");

        // Verify values
        assertEquals("test query", input.getQ());
        assertEquals(Integer.valueOf(2), input.getP());
        assertEquals(Integer.valueOf(20), input.getRows());
        assertEquals("date", input.getSort());
        assertEquals("pt", input.getLocale());
        assertEquals("category", input.getGroup());
        assertEquals(Integer.valueOf(5), input.getNfpr());
        assertEquals("AND", input.getFqOp());
        assertEquals("OR", input.getFqiOp());
    }

    @Test
    void testToString() {
        TurSNSearchParamsInput input = new TurSNSearchParamsInput();
        input.setQ("test");
        input.setP(1);

        String toString = input.toString();
        assertNotNull(toString);
        // Should contain class name and some field values
        assert (toString.contains("TurSNSearchParamsInput"));
    }
}
