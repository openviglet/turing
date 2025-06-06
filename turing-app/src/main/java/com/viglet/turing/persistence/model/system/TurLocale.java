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

package com.viglet.turing.persistence.model.system;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;

/**
 * The persistent class for the TurLocale database table.
 * 
 */
@Setter
@Getter
@Entity
@Table(name = "locale")
public class TurLocale implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	@Column(unique = true, nullable = false, length = 5)
	private Locale initials;

	@Column
	private String en;

	@Column
	private String pt;

	public TurLocale(Locale initials, String en, String pt) {
		setInitials(initials);
		setEn(en);
		setPt(pt);
	}

	public TurLocale() {
		super();
	}

}