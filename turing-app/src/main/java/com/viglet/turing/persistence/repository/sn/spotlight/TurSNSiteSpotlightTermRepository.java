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
package com.viglet.turing.persistence.repository.sn.spotlight;

import com.viglet.turing.persistence.model.sn.spotlight.TurSNSiteSpotlight;
import com.viglet.turing.persistence.model.sn.spotlight.TurSNSiteSpotlightTerm;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Alexandre Oliveira
 * @since 0.3.4
 */
public interface TurSNSiteSpotlightTermRepository extends JpaRepository<TurSNSiteSpotlightTerm, String> {

	@SuppressWarnings("unchecked")
	@NotNull
	TurSNSiteSpotlightTerm save(@NotNull TurSNSiteSpotlightTerm turSNSiteSpotlightTerm);

	List<TurSNSiteSpotlightTerm> findByNameIn(Collection<String> names);
	Set<TurSNSiteSpotlightTerm> findByTurSNSiteSpotlight(TurSNSiteSpotlight turSNSiteSpotlight);
	
	void delete(@NotNull TurSNSiteSpotlightTerm turSNSiteSpotlightTerm);

	@Modifying
	@Query("delete from  TurSNSiteSpotlightTerm ssst where ssst.id = ?1")
	void delete(String id);
}
