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
package com.viglet.turing.se.facet;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Setter
@Getter
public class TurSEFacetResult {
	private String facet;
	private int facetPosition;
	private Map<String, TurSEFacetResultAttr> turSEFacetResultAttr = new LinkedHashMap<>();

	public void add(String attribute, TurSEFacetResultAttr turSEFacetResultAttr) {
		this.turSEFacetResultAttr.put(attribute, turSEFacetResultAttr);
	}

	@Override
	public String toString() {
		return "TurSEFacetResult{" +
				"facet='" + facet + '\'' +
				", facetPosition=" + facetPosition +
				", turSEFacetResultAttr=" + turSEFacetResultAttr +
				'}';
	}
}
