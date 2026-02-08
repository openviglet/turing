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

package com.viglet.turing.solr;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for TurSolrConstants.
 *
 * @author Alexandre Oliveira
 * @since 2025.1.10
 */
class TurSolrConstantsTest {

    @Test
    void testConstructorThrowsException() throws NoSuchMethodException {
        Constructor<TurSolrConstants> constructor = TurSolrConstants.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .getCause()
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Solr Constants class");
    }

    @Test
    void testSortingConstants() {
        assertThat(TurSolrConstants.NEWEST).isEqualTo("newest");
        assertThat(TurSolrConstants.OLDEST).isEqualTo("oldest");
        assertThat(TurSolrConstants.ASC).isEqualTo("asc");
    }

    @Test
    void testFieldConstants() {
        assertThat(TurSolrConstants.COUNT).isEqualTo("count");
        assertThat(TurSolrConstants.SCORE).isEqualTo("score");
        assertThat(TurSolrConstants.VERSION).isEqualTo("_version_");
        assertThat(TurSolrConstants.BOOST).isEqualTo("boost");
        assertThat(TurSolrConstants.TURING_ENTITY).isEqualTo("turing_entity_");
        assertThat(TurSolrConstants.ID).isEqualTo("id");
        assertThat(TurSolrConstants.TITLE).isEqualTo("title");
        assertThat(TurSolrConstants.TYPE).isEqualTo("type");
        assertThat(TurSolrConstants.URL).isEqualTo("url");
    }

    @Test
    void testQueryConstants() {
        assertThat(TurSolrConstants.MORE_LIKE_THIS).isEqualTo("moreLikeThis");
        assertThat(TurSolrConstants.BOOST_QUERY).isEqualTo("bq");
        assertThat(TurSolrConstants.QUERY).isEqualTo("q");
        assertThat(TurSolrConstants.TRUE).isEqualTo("true");
        assertThat(TurSolrConstants.EDISMAX).isEqualTo("edismax");
        assertThat(TurSolrConstants.DEF_TYPE).isEqualTo("defType");
        assertThat(TurSolrConstants.AND).isEqualTo("AND");
        assertThat(TurSolrConstants.Q_OP).isEqualTo("q.op");
        assertThat(TurSolrConstants.OR).isEqualTo("OR");
    }

    @Test
    void testRecentDatesConstant() {
        assertThat(TurSolrConstants.RECENT_DATES).isEqualTo("{!func}recip(ms(NOW/DAY,%s),3.16e-11,1,1)");
        assertThat(TurSolrConstants.RECENT_DATES).contains("%s");
    }

    @Test
    void testHandlerConstants() {
        assertThat(TurSolrConstants.TUR_SUGGEST).isEqualTo("/tur_suggest");
        assertThat(TurSolrConstants.TUR_SPELL).isEqualTo("/tur_spell");
    }

    @Test
    void testFacetConstants() {
        assertThat(TurSolrConstants.FILTER_QUERY_OR).isEqualTo("{!tag=_all_}");
        assertThat(TurSolrConstants.FACET_OR).isEqualTo("{!ex=_all_}");
        assertThat(TurSolrConstants.NO_FACET_NAME).isEqualTo("__no_facet_name__");
        assertThat(TurSolrConstants.OR_OR).isEqualTo("OR-OR");
        assertThat(TurSolrConstants.OR_AND).isEqualTo("OR-AND");
        assertThat(TurSolrConstants.ALL).isEqualTo("_all_");
    }

    @Test
    void testMiscConstants() {
        assertThat(TurSolrConstants.PLUS_ONE).isEqualTo("+1");
        assertThat(TurSolrConstants.EMPTY).isEmpty();
        assertThat(TurSolrConstants.SOLR_DATE_PATTERN).isEqualTo("yyyy-MM-dd'T'HH:mm:ss'Z'");
        assertThat(TurSolrConstants.INDEX).isEqualTo("index");
        assertThat(TurSolrConstants.ROWS).isEqualTo("rows");
        assertThat(TurSolrConstants.HYPHEN).isEqualTo("-");
    }

    @Test
    void testAllConstantsAreNotNull() {
        assertThat(TurSolrConstants.NEWEST).isNotNull();
        assertThat(TurSolrConstants.OLDEST).isNotNull();
        assertThat(TurSolrConstants.ASC).isNotNull();
        assertThat(TurSolrConstants.COUNT).isNotNull();
        assertThat(TurSolrConstants.SCORE).isNotNull();
        assertThat(TurSolrConstants.VERSION).isNotNull();
        assertThat(TurSolrConstants.BOOST).isNotNull();
        assertThat(TurSolrConstants.TURING_ENTITY).isNotNull();
        assertThat(TurSolrConstants.ID).isNotNull();
        assertThat(TurSolrConstants.TITLE).isNotNull();
        assertThat(TurSolrConstants.TYPE).isNotNull();
        assertThat(TurSolrConstants.URL).isNotNull();
        assertThat(TurSolrConstants.MORE_LIKE_THIS).isNotNull();
        assertThat(TurSolrConstants.BOOST_QUERY).isNotNull();
        assertThat(TurSolrConstants.QUERY).isNotNull();
        assertThat(TurSolrConstants.TRUE).isNotNull();
        assertThat(TurSolrConstants.EDISMAX).isNotNull();
        assertThat(TurSolrConstants.DEF_TYPE).isNotNull();
        assertThat(TurSolrConstants.AND).isNotNull();
        assertThat(TurSolrConstants.Q_OP).isNotNull();
        assertThat(TurSolrConstants.RECENT_DATES).isNotNull();
        assertThat(TurSolrConstants.TUR_SUGGEST).isNotNull();
        assertThat(TurSolrConstants.TUR_SPELL).isNotNull();
        assertThat(TurSolrConstants.FILTER_QUERY_OR).isNotNull();
        assertThat(TurSolrConstants.FACET_OR).isNotNull();
        assertThat(TurSolrConstants.PLUS_ONE).isNotNull();
        assertThat(TurSolrConstants.EMPTY).isNotNull();
        assertThat(TurSolrConstants.SOLR_DATE_PATTERN).isNotNull();
        assertThat(TurSolrConstants.INDEX).isNotNull();
        assertThat(TurSolrConstants.ROWS).isNotNull();
        assertThat(TurSolrConstants.OR).isNotNull();
        assertThat(TurSolrConstants.NO_FACET_NAME).isNotNull();
        assertThat(TurSolrConstants.OR_OR).isNotNull();
        assertThat(TurSolrConstants.OR_AND).isNotNull();
        assertThat(TurSolrConstants.ALL).isNotNull();
        assertThat(TurSolrConstants.HYPHEN).isNotNull();
    }

    @Test
    void testConstantValuePatterns() {
        assertThat(TurSolrConstants.VERSION).startsWith("_");
        assertThat(TurSolrConstants.VERSION).endsWith("_");
        assertThat(TurSolrConstants.TURING_ENTITY).endsWith("_");
        assertThat(TurSolrConstants.TUR_SUGGEST).startsWith("/");
        assertThat(TurSolrConstants.TUR_SPELL).startsWith("/");
        assertThat(TurSolrConstants.FILTER_QUERY_OR).contains("tag=");
        assertThat(TurSolrConstants.FACET_OR).contains("ex=");
        assertThat(TurSolrConstants.NO_FACET_NAME).startsWith("__");
        assertThat(TurSolrConstants.NO_FACET_NAME).endsWith("__");
        assertThat(TurSolrConstants.ALL).startsWith("_");
        assertThat(TurSolrConstants.ALL).endsWith("_");
    }
}
