/*
 * Copyright (C) 2016-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.viglet.turing.commons.sn.bean;

import java.io.Serializable;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.viglet.turing.commons.se.similar.TurSESimilarResult;
import com.viglet.turing.commons.sn.bean.spellcheck.TurSNSiteSpellCheckBean;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Details about facets and facet and "more like this" of Turing ES Semantic Navigation response.
 * 
 * @author Alexandre Oliveira
 * 
 * @since 0.3.4
 */

@RequiredArgsConstructor
@Accessors(chain = true)
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TurSNSiteSearchWidgetBean implements Serializable {
	private String icon;
	/**
	 * T713 / §XLV — <strong>legacy</strong>: a pre-serialized GET URL that clears
	 * every filter on the search. Kept for deep-linking / SEO / no-JS (see
	 * §XLV.5), but legacy for a JavaScript client: request
	 * {@code responseFormat: STRUCTURED} and clear the request's {@code fq}
	 * (see {@link #selectedFilterQueries}) instead of parsing this URL.
	 */
	private String cleanUpFacets;
	private List<String> selectedFilterQueries;
	private List<TurSNSiteSearchFacetBean> facet;
	private List<TurSNSiteSearchFacetBean> secondaryFacet;
	private TurSNSiteSearchFacetBean facetToRemove;
	private List<TurSESimilarResult> similar;
	private TurSNSiteSpellCheckBean spellCheck;
	private List<TurSNSiteLocaleBean> locales;
	private List<TurSNSiteSpotlightDocumentBean> spotlights;
	/**
	 * T1115 — {@link #facet} with the facets the request already filters on kept
	 * in. A site whose facet item type is not {@code OR} drills down: the legacy
	 * list drops an applied facet and lists it under {@link #facetToRemove}, which
	 * the structured response has no place for. This is an in-process hand-off to
	 * the structured mapper, so the legacy JSON stays unchanged; {@code null} when
	 * it would equal {@link #facet}.
	 *
	 * @since 2026.3.6
	 */
	@JsonIgnore
	private List<TurSNSiteSearchFacetBean> structuredFacet;
	/** T1115 — {@link #secondaryFacet}, kept the same way. @see #structuredFacet */
	@JsonIgnore
	private List<TurSNSiteSearchFacetBean> structuredSecondaryFacet;

}
