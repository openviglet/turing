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

package com.viglet.turing.api.sn.graphql;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * GraphQL input class for search parameters.
 * 
 * @author Alexandre Oliveira
 * @since 0.3.6
 */
@Getter
@Setter
@ToString
public class TurSNSearchParamsInput {
    private String q = "*";
    private Integer p = 1;
    private List<String> fq;
    private List<String> fqAnd;
    private List<String> fqOr;
    private String fqOp = "NONE";
    private String fqiOp = "NONE";
    private String sort = "relevance";
    private Integer rows = -1;
    private String locale;
    private List<String> fl;
    private String group;
    private Integer nfpr = 1;
}