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

import org.junit.jupiter.api.Test;

/**
 * Unit tests for TurSNSiteSearchGraphQLController.
 * 
 * @author Alexandre Oliveira
 * @since 0.3.6
 */
class TurSNSiteSearchGraphQLControllerTest {

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
        assert(toString.contains("TurSNSearchParamsInput"));
    }
}