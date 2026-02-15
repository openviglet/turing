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

package com.viglet.turing.sn.template;

import static com.viglet.turing.commons.sn.field.TurSNFieldName.DEFAULT;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.viglet.turing.persistence.model.sn.TurSNSite;

/**
 * Unit tests for TurSNTemplate.
 *
 * @author Alexandre Oliveira
 * @since 2026.1.10
 */
class TurSNTemplateTest {

    @Test
    void testDefaultSNUI() {
        TurSNTemplate template = new TurSNTemplate(null, null, null, null, null, null, null);
        TurSNSite site = new TurSNSite();

        template.defaultSNUI(site);

        assertThat(site.getRowsPerPage()).isEqualTo(10);
        assertThat(site.getFacet()).isEqualTo(1);
        assertThat(site.getItemsPerFacet()).isEqualTo(10);
        assertThat(site.getHl()).isEqualTo(1);
        assertThat(site.getMlt()).isEqualTo(1);
        assertThat(site.getSpellCheck()).isEqualTo(1);
        assertThat(site.getSpellCheckFixes()).isEqualTo(1);
        assertThat(site.getThesaurus()).isEqualTo(0);
        assertThat(site.getExactMatch()).isEqualTo(1);
        assertThat(site.getDefaultField()).isEqualTo(DEFAULT);
    }
}
