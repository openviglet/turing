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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.viglet.turing.commons.se.field.TurSEFieldType;
import com.viglet.turing.persistence.model.sn.field.TurSNSiteFieldExt;
import com.viglet.turing.persistence.repository.sn.field.TurSNSiteFieldExtRepository;
import com.viglet.turing.se.result.TurSEResult;
import com.viglet.turing.sn.TurSNFieldProcess;

/**
 * Unit tests for TurSolrResultProcessor.
 *
 * @author Alexandre Oliveira
 * @since 2026.1.10
 */
@ExtendWith(MockitoExtension.class)
class TurSolrResultProcessorTest {

    @Mock
    private TurSNFieldProcess turSNFieldProcess;

    @Mock
    private TurSNSiteFieldExtRepository turSNSiteFieldExtRepository;

    @Test
    void testCreateTurSEResultAppliesHighlightAndRequiredFields() {
        TurSolrResultProcessor processor = new TurSolrResultProcessor(turSNFieldProcess,
                turSNSiteFieldExtRepository);
        TurSNSiteFieldExt titleField = TurSNSiteFieldExt.builder()
                .name("title")
                .type(TurSEFieldType.TEXT)
                .build();

        Map<String, TurSNSiteFieldExt> fieldExtMap = Map.of("title", titleField);
        Map<String, Object> requiredFields = Map.of("type", "doc");
        SolrDocument document = new SolrDocument();
        document.addField("id", "1");
        document.addField("title", "plain");

        Map<String, List<String>> highlight = Map.of("title", List.of("<em>highlight</em>"));

        TurSEResult result = processor.createTurSEResult(fieldExtMap, requiredFields, document,
                highlight);

        assertThat(result.getFields()).containsEntry("type", "doc");
        assertThat(result.getFields()).containsEntry("title", "<em>highlight</em>");
        assertThat(result.getFields()).containsEntry("id", "1");
    }
}
