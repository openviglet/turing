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
package com.viglet.turing.exchange.sn;

import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.viglet.turing.persistence.model.sn.synonym.TurSNSynonym;
import com.viglet.turing.persistence.model.sn.synonym.TurSNSynonymType;

/**
 * T1263 — one SN site synonym rule as an export carries it: the rule without its
 * site, so the import re-attaches it to the site it recreates.
 *
 * @param id       the rule's id, kept so a re-import does not mint a new one
 * @param name     the optional admin label
 * @param type     the rule type; {@code null} reads as {@link TurSNSynonymType#REGULAR}
 * @param language the locale the rule applies to
 * @param input    the left-hand side of a directional rule, or {@code null}
 * @param terms    the rule's ordered token list
 * @param enabled  whether the rule is pushed to the engine
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TurSNSynonymExchange(String id, String name, TurSNSynonymType type, Locale language,
        String input, List<String> terms, boolean enabled) {

    public static TurSNSynonymExchange of(TurSNSynonym synonym) {
        return new TurSNSynonymExchange(synonym.getId(), synonym.getName(), synonym.getType(),
                synonym.getLanguage(), synonym.getInput(),
                synonym.getTerms() == null ? List.of() : List.copyOf(synonym.getTerms()),
                synonym.isEnabled());
    }
}
