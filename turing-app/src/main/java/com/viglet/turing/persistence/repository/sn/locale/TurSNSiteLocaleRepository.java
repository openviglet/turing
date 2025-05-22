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

package com.viglet.turing.persistence.repository.sn.locale;

import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.persistence.model.sn.locale.TurSNSiteLocale;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Locale;

/**
 * @author Alexandre Oliveira
 * @since 0.3.4
 */
public interface TurSNSiteLocaleRepository extends JpaRepository<TurSNSiteLocale, String> {

	String FIND_BY_TUR_SN_SITE_AND_LANGUAGE = "turSNSiteLocaleFindByTurSNSiteAndLanguage";
	String EXISTS_BY_TUR_SN_SITE_AND_LANGUAGE = "turSNSiteLocaleExistsByTurSNSiteAndLanguage";
	String FIND_BY_TUR_SN_SITE_SORT = "turSNSiteLocaleFindByTurSNSiteSort";
	String FIND_BY_TUR_SN_SITE = "turSNSiteLocaleFindByTurSNSite";

	@Cacheable(FIND_BY_TUR_SN_SITE_AND_LANGUAGE)
	TurSNSiteLocale findByTurSNSiteAndLanguage(TurSNSite turSNSite, Locale language);
	@Cacheable(EXISTS_BY_TUR_SN_SITE_AND_LANGUAGE)
	boolean existsByTurSNSiteAndLanguage(TurSNSite turSNSite, Locale language);
	@Cacheable(FIND_BY_TUR_SN_SITE_SORT)
	List<TurSNSiteLocale> findByTurSNSite(Sort name, TurSNSite turSNSite);
	@Cacheable(FIND_BY_TUR_SN_SITE)
	List<TurSNSiteLocale> findByTurSNSite(TurSNSite turSNSite);

	@CacheEvict(value = {FIND_BY_TUR_SN_SITE_AND_LANGUAGE, EXISTS_BY_TUR_SN_SITE_AND_LANGUAGE,
			FIND_BY_TUR_SN_SITE_SORT, FIND_BY_TUR_SN_SITE}, allEntries = true)
	@NotNull
	TurSNSiteLocale save(@NotNull TurSNSiteLocale turSNSiteLocale);

	@CacheEvict(value = {FIND_BY_TUR_SN_SITE_AND_LANGUAGE, EXISTS_BY_TUR_SN_SITE_AND_LANGUAGE,
			FIND_BY_TUR_SN_SITE_SORT, FIND_BY_TUR_SN_SITE}, allEntries = true)
	void delete(@NotNull TurSNSiteLocale turSNSiteLocale);

}
