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

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.solr.TurSolr;
import com.viglet.turing.solr.TurSolrInstanceProcess;
import com.viglet.turing.commons.se.TurSEParameters;
import com.viglet.turing.commons.sn.bean.spellcheck.TurSNSiteSpellCheckBean;
import com.viglet.turing.commons.sn.search.TurSNParamType;
import com.viglet.turing.commons.sn.search.TurSNSiteSearchContext;
import com.viglet.turing.sn.TurSNUtils;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/sn/{siteName}/{locale}/spell-check")
@Tag(name = "Semantic Navigation Spell Check", description = "Semantic Navigation Spell Check API")
public class TurSNSiteSpellCheckAPI {
	@Autowired
	private TurSolr turSolr;
	@Autowired
	private TurSolrInstanceProcess turSolrInstanceProcess;

	@GetMapping
	public TurSNSiteSpellCheckBean turSNSiteSpellCheck(@PathVariable String siteName, @PathVariable String locale,
			@RequestParam(required = true, name = TurSNParamType.QUERY) String q, HttpServletRequest request) {

		return turSolrInstanceProcess.initSolrInstance(siteName, locale).map(turSolrInstance -> {
			TurSNSiteSearchContext turSNSiteSearchContext = new TurSNSiteSearchContext(siteName,
					new TurSEParameters(q, null, 1, "relevance", 10, null, 0), locale,
					TurSNUtils.requestToURI(request));
			return new TurSNSiteSpellCheckBean(turSNSiteSearchContext, turSolr.spellCheckTerm(turSolrInstance, q));
		}).orElse(null);

	}
}
