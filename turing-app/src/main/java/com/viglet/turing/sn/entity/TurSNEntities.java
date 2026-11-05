/*
 * Copyright (C) 2016-2026 the original author or authors.
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

package com.viglet.turing.sn.entity;

import java.util.List;

/**
 * T1228 — the named entities found in one document, as the {@code record_entities}
 * tool receives them and as they are written to the {@code turing_entity_*} facets.
 *
 * @param people        people named
 * @param organizations companies, institutions and groups named
 * @param places        cities, regions, countries and sites named
 *
 * @author Alexandre Oliveira
 * @since 2026.3.5
 */
public record TurSNEntities(List<String> people, List<String> organizations, List<String> places) {

    public static final TurSNEntities NONE = new TurSNEntities(List.of(), List.of(), List.of());

    public TurSNEntities {
        people = people == null ? List.of() : List.copyOf(people);
        organizations = organizations == null ? List.of() : List.copyOf(organizations);
        places = places == null ? List.of() : List.copyOf(places);
    }

    public boolean isEmpty() {
        return people.isEmpty() && organizations.isEmpty() && places.isEmpty();
    }
}
