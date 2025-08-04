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

package com.viglet.turing.api.sn.search;

import com.google.inject.Inject;
import com.viglet.turing.commons.sn.search.TurSNParamType;
import com.viglet.turing.solr.TurSolr;
import com.viglet.turing.solr.TurSolrInstanceProcess;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.LocaleUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/sn/{siteName}/query")
@Tag(name = "Semantic Navigation DSL Query", description = "Semantic Navigation DSL Query API")
public class TurSNSiteQueryAPI {
    private final TurSolr turSolr;
    private final TurSolrInstanceProcess turSolrInstanceProcess;
    @Inject
    public TurSNSiteQueryAPI(TurSolr turSolr, TurSolrInstanceProcess turSolrInstanceProcess) {
        this.turSolr = turSolr;
        this.turSolrInstanceProcess = turSolrInstanceProcess;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public String turSNSiteSearchSelectGet(
            @PathVariable String siteName,
            @RequestParam(name = TurSNParamType.LOCALE) String localeRequest,
            @RequestBody String json) {
        return turSolrInstanceProcess
                .initSolrInstance(siteName, LocaleUtils.toLocale(localeRequest))
                .map(turSolrInstance -> turSolr.dslQuery(turSolrInstance, json))
                .orElse("{}");
    }

}
