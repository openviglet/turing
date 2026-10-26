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

package com.viglet.turing.api.dev.token;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.viglet.turing.persistence.dto.dev.token.TurDevTokenDto;
import com.viglet.turing.persistence.mapper.dev.token.TurDevTokenMapper;
import com.viglet.turing.persistence.model.dev.token.TurDevToken;
import com.viglet.turing.persistence.repository.dev.token.TurDevTokenRepository;
import com.viglet.turing.spring.utils.TurPersistenceUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/dev/token")
@Secured("ROLE_ADMIN")
@Tag(name = "Developer Token", description = "Developer Token API")
public class TurDevTokenAPI {

	private final TurDevTokenRepository turDevTokenRepository;
	private final TurDevTokenMapper turDevTokenMapper;

	public TurDevTokenAPI(TurDevTokenRepository turDevTokenRepository, TurDevTokenMapper turDevTokenMapper) {
		this.turDevTokenRepository = turDevTokenRepository;
		this.turDevTokenMapper = turDevTokenMapper;
	}

	@Operation(summary = "Developer Token List")
	@GetMapping
	public List<TurDevTokenDto> turDevTokenList() {
		List<TurDevTokenDto> dtos = turDevTokenMapper
				.toDtoList(this.turDevTokenRepository.findAll(TurPersistenceUtils.orderByTitleIgnoreCase()));
		dtos.forEach(TurDevTokenAPI::scrubToken);
		return dtos;
	}

	@Operation(summary = "Show a Developer Token")
	@GetMapping("/{id}")
	public TurDevTokenDto turDevTokenGet(@PathVariable String id) {
		return scrubToken(turDevTokenMapper.toDto(this.turDevTokenRepository.findById(id).orElse(new TurDevToken())));
	}

	@Operation(summary = "Update a Developer Token")
	@PutMapping("/{id}")
	public TurDevTokenDto turDevTokenUpdate(@PathVariable String id, @RequestBody TurDevTokenDto turDevTokenDto) {
		validatePartition(turDevTokenDto);
		TurDevToken source = turDevTokenMapper.toEntity(turDevTokenDto);
		return turDevTokenRepository.findById(id).map(existing -> {
			turDevTokenMapper.updateEntity(source, existing);
			turDevTokenRepository.save(existing);
			return scrubToken(turDevTokenMapper.toDto(existing));
		}).orElse(new TurDevTokenDto());

	}

	@Transactional
	@Operation(summary = "Delete a Developer Token")
	@DeleteMapping("/{id}")
	public boolean turDevTokenDelete(@PathVariable String id) {
		this.turDevTokenRepository.deleteById(id);
		return true;
	}

	@Operation(summary = "Create a Developer Token")
	@PostMapping
	public TurDevTokenDto turDevTokenAdd(@RequestBody TurDevTokenDto turDevTokenDto) {
		validatePartition(turDevTokenDto);
		TurDevToken turDevToken = turDevTokenMapper.toEntity(turDevTokenDto);
		turDevToken.setToken(UUID.randomUUID().toString().replace("-", "").substring(0, 25));
		this.turDevTokenRepository.save(turDevToken);
		// T646 / §XXXVII.8 — the create response is the ONLY place the cleartext
		// token value is returned (a one-time reveal); list/get/update scrub it.
		return turDevTokenMapper.toDto(turDevToken);
	}

	/** A field the engines can filter on without quoting its name. */
	private static final Pattern PARTITION_FIELD = Pattern.compile("^[A-Za-z0-9_.\\-]{1,255}$");

	/**
	 * T1133 — a partition is a field and a value, both or neither, and the field
	 * is a plain name: it goes into the engines' filters unquoted.
	 *
	 * @throws IllegalArgumentException (400) for half a partition or an unusable field name
	 */
	static void validatePartition(TurDevToken token) {
		boolean hasField = StringUtils.isNotBlank(token.getPartitionField());
		boolean hasValue = StringUtils.isNotBlank(token.getPartitionValue());
		if (hasField != hasValue) {
			throw new IllegalArgumentException("A partition needs both partitionField and partitionValue");
		}
		if (hasField && !PARTITION_FIELD.matcher(token.getPartitionField().trim()).matches()) {
			throw new IllegalArgumentException(
					"partitionField must be a field name of letters, digits, '_', '.' or '-'");
		}
		if (!hasField) {
			token.setPartitionField(null);
			token.setPartitionValue(null);
		}
	}

	/**
	 * T646 / §XXXVII.8 — never return a stored token value on read paths (it lands
	 * in logs / browser history / caches). The token is revealed once on create.
	 */
	private static TurDevTokenDto scrubToken(TurDevTokenDto dto) {
		if (dto != null) {
			dto.setToken(null);
		}
		return dto;
	}
}
