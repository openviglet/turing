/*
 * Copyright (C) 2016-2023 the original author or authors.
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

package com.viglet.turing.connector.persistence.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The persistent class for the conn_indexing_rule database table.
 *
 * @author Alexandre Oliveira
 * @since 2025.2
 */

@Builder
@RequiredArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
@Entity
@Table(name = "con_indexing_rule", uniqueConstraints = {@UniqueConstraint(columnNames = {"id"})})
@AllArgsConstructor
public class TurConnectorIndexingRule implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(length = 50)
    private String name;

    @Column
    private String description;

    private String source;

    @Enumerated(EnumType.STRING)
    @Column
    private TurConnectorIndexingRuleType ruleType;

    @Column
    private String attribute;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "con_indexing_rule_values", joinColumns = @JoinColumn(name = "id"))
    @Column
    private List<String> values = new ArrayList<>();

}