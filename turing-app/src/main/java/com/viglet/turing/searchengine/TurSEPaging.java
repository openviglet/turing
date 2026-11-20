/*
 * Copyright (C) 2016-2026 the original author or authors.
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
package com.viglet.turing.searchengine;

import org.springframework.util.StringUtils;

import com.viglet.turing.commons.se.TurSEParameters;

/**
 * The page window and grouping a search request asks for (T1310, formerly on
 * {@code TurSolrUtils} and {@code TurSolrQueryBuilder}). Engine-neutral: the
 * spotlight placement and the SN response builder read them without knowing
 * which engine answered.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public final class TurSEPaging {

    private TurSEPaging() {
    }

    /**
     * The 0-based offset of the current page's first row. Clamped to 0: a
     * client-supplied page &lt;= 0 would otherwise give a negative start, which an
     * engine rejects.
     */
    public static int firstRow(TurSEParameters turSEParameters) {
        return Math.max(0,
                (turSEParameters.getCurrentPage() * turSEParameters.getRows()) - turSEParameters.getRows());
    }

    /** The exclusive end of the current page's rows. */
    public static int lastRow(TurSEParameters turSEParameters) {
        return turSEParameters.getCurrentPage() * turSEParameters.getRows();
    }

    /** Whether the request groups its results by a field. */
    public static boolean hasGroup(TurSEParameters turSEParameters) {
        return StringUtils.hasText(turSEParameters.getGroup());
    }
}
