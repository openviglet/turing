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

package com.viglet.turing.searchengine.solr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.searchengine.spi.TurSESiteCore;
import com.viglet.turing.searchengine.spi.TurSESiteSearchConfig;
import com.viglet.turing.system.properties.TurConfigProperties;
import com.viglet.turing.system.properties.TurSolrProperty;
import com.viglet.turing.searchengine.solr.source.TurSolrInstanceSource;

/**
 * Unit tests for TurSolrInstanceProcess.
 *
 * @author Alexandre Oliveira
 * @since 2025.1.10
 */
@ExtendWith(MockitoExtension.class)
class TurSolrInstanceProcessTest {

    @Mock
    private TurSESiteSearchConfig siteSearchConfig;

    @Mock
    private TurSolrCache turSolrCache;

    @Mock
    private TurConfigProperties turConfigProperties;

    @Mock
    private TurSolrProperty solrProperties;

    @Mock
    private TurSolrInstanceSource solrInstanceSource;

    private TurSolrInstanceProcess turSolrInstanceProcess;

    @BeforeEach
    void setUp() {
        lenient().when(turConfigProperties.getSolr()).thenReturn(solrProperties);
        lenient().when(solrProperties.getTimeout()).thenReturn(5000);
        lenient().when(solrInstanceSource.isReadOnly()).thenReturn(false);

        turSolrInstanceProcess = new TurSolrInstanceProcess(
                siteSearchConfig,
                solrInstanceSource, new com.viglet.turing.searchengine.InMemorySNSiteSEInstances().endpoints());
    }

    @Test
    void testConstructor() {
        assertThat(turSolrInstanceProcess).isNotNull();
    }

    @Test
    void testInitSolrInstanceBySiteNameNotFound() {
        when(siteSearchConfig.siteCore("nonexistent", Locale.ENGLISH)).thenReturn(Optional.empty());

        Optional<TurSolrInstance> result = turSolrInstanceProcess.initSolrInstance("nonexistent", Locale.ENGLISH);

        assertThat(result).isEmpty();
        verify(siteSearchConfig).siteCore("nonexistent", Locale.ENGLISH);
    }

    @Test
    void testInitSolrInstanceBySiteWithoutAnSEInstanceIsEmpty() {
        when(siteSearchConfig.siteCore("testSite", Locale.ENGLISH))
                .thenReturn(Optional.of(new TurSESiteCore("testSite", null, "en_core")));

        assertThat(turSolrInstanceProcess.initSolrInstance("testSite", Locale.ENGLISH)).isEmpty();
    }

    @Test
    void testInitSolrInstanceBySiteNameOpensTheSitesCore() throws Exception {
        var seInstances = new com.viglet.turing.searchengine.InMemorySNSiteSEInstances();
        var seInstance = new com.viglet.turing.searchengine.persistence.model.TurSEInstance();
        seInstance.setEndpointUrl("http://localhost:8983/solr");
        seInstances.attach(new com.viglet.turing.sn.persistence.model.TurSNSite(), seInstance);
        TurSolrInstanceProcess process = new TurSolrInstanceProcess(siteSearchConfig, solrInstanceSource,
                seInstances.endpoints());
        when(siteSearchConfig.siteCore("testSite", Locale.ENGLISH))
                .thenReturn(Optional.of(new TurSESiteCore("testSite", seInstance.getId(), "en_core")));

        Optional<TurSolrInstance> result = process.initSolrInstance("testSite", Locale.ENGLISH);

        assertThat(result).isPresent();
        assertThat(result.get().getCore()).isEqualTo("en_core");
        process.shutdown();
    }
}