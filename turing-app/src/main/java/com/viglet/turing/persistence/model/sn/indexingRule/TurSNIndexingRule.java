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

package com.viglet.turing.persistence.model.sn.indexingRule;

import com.viglet.turing.persistence.model.sn.TurSNSite;
import com.viglet.turing.spring.security.TurAuditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * The persistent class for the sn_indexing_rule database table.
 * 
 * @author Alexandre Oliveira
 * @since 2025.2
 */
@Getter
@Entity
@Table(name = "sn_indexing_rule")
public class TurSNIndexingRule extends TurAuditable<String>  implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	@Setter
	@Id
	@UuidGenerator
	@Column(name = "id", updatable = false, nullable = false)
	private String id;

	@Setter
	@Column(length = 50)
	private String name;

	@Setter
	@Column
	private String description;

	@Setter
	@ManyToOne
	@JoinColumn(name = "sn_site_id", nullable = false)
	private TurSNSite turSNSite;

	@Column
	private TurSNIndexingRuleType ruleType;

	@OneToMany(mappedBy = "turSNIndexingRule", orphanRemoval = true, fetch = FetchType.LAZY)
	@Cascade({ org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN  })
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Set<TurSNIndexingCondition> conditions = new HashSet<>();

	public void setTurSNIndexingRuleConditions(Set<TurSNIndexingCondition> turSNIndexingRuleConditions) {
		this.conditions.clear();
		if (turSNIndexingRuleConditions != null) {
			this.conditions.addAll(turSNIndexingRuleConditions);
		}
	}
}