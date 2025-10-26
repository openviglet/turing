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

package com.viglet.turing.api.sn.search;

import java.util.List;

import org.apache.commons.lang3.LocaleUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.commons.sn.bean.TurSNSearchParams;
import com.viglet.turing.commons.sn.search.TurSNFilterQueryOperator;
import com.viglet.turing.commons.sn.search.TurSNParamType;
import com.viglet.turing.sn.ac.TurSNAutoComplete;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/sn/{siteName}/ac")
@Tag(name = "Semantic Navigation Auto Complete", description = "Semantic Navigation Auto Complete API")
public class TurSNSiteAutoCompleteAPI {

        private final TurSNAutoComplete turSNAutoComplete;

        public TurSNSiteAutoCompleteAPI(TurSNAutoComplete turSNAutoComplete) {
                this.turSNAutoComplete = turSNAutoComplete;
        }

        @GetMapping
        public List<String> turSNSiteAutoComplete(@PathVariable String siteName,
                        TurSNSearchParams turSNSearchParams,
                        @RequestParam(required = false, name = TurSNParamType.FILTER_QUERIES_DEFAULT) List<String> filterQueriesDefault,
                        @RequestParam(required = false, name = TurSNParamType.FILTER_QUERIES_AND) List<String> filterQueriesAnd,
                        @RequestParam(required = false, name = TurSNParamType.FILTER_QUERIES_OR) List<String> filterQueriesOr,
                        @RequestParam(required = false, name = TurSNParamType.FILTER_QUERY_OPERATOR, defaultValue = "NONE") TurSNFilterQueryOperator fqOperator,
                        @RequestParam(required = false, name = TurSNParamType.FILTER_QUERY_ITEM_OPERATOR, defaultValue = "NONE") TurSNFilterQueryOperator fqItemOperator,
                        @RequestParam(required = false, name = TurSNParamType.LOCALE) String locale,
                        HttpServletRequest request) {
                turSNSearchParams.setFq(filterQueriesDefault);
                turSNSearchParams.setFqAnd(filterQueriesAnd);
                turSNSearchParams.setFqOr(filterQueriesOr);
                turSNSearchParams.setFqOp(fqOperator);
                turSNSearchParams.setFqiOp(fqItemOperator);
                turSNSearchParams.setLocale(LocaleUtils.toLocale(locale));
                if (turSNSearchParams.getRows() < 0) {
                        turSNSearchParams.setRows(20);
                }
                if ((!CollectionUtils.isEmpty(turSNSearchParams.getFq()))
                                || !CollectionUtils.isEmpty(turSNSearchParams.getFqAnd())
                                || !CollectionUtils.isEmpty(turSNSearchParams.getFqOr())) {
                        return turSNAutoComplete.autoCompleteWithRegularSearch(siteName,
                                        turSNSearchParams, request);
                } else {
                        return turSNAutoComplete.autoComplete(siteName, turSNSearchParams.getQ(),
                                        LocaleUtils.toLocale(turSNSearchParams.getLocale()),
                                        turSNSearchParams.getRows());
                }
        }

}
