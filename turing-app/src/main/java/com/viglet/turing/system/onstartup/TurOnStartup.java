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
package com.viglet.turing.system.onstartup;

import java.util.Comparator;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.viglet.turing.persistence.repository.system.TurConfigVarRepository;
import com.viglet.turing.shared.onstartup.TurFirstTimeSeeder;
import com.viglet.turing.shared.onstartup.TurStartupReconciler;

import lombok.extern.slf4j.Slf4j;

/**
 * Runs every module's first-time seeder on a fresh install, then every module's
 * idempotent reconciliation on each boot. It knows the seeders only through the
 * shared {@link TurFirstTimeSeeder} and {@link TurStartupReconciler} contracts,
 * so the system module depends on no module whose tables they write (T1311).
 */
@Slf4j
@Component
@Transactional
@Order(1)
public class TurOnStartup implements ApplicationRunner {
	public static final String FIRST_TIME = "FIRST_TIME";
	private final TurConfigVarRepository turConfigVarRepository;
	private final List<TurFirstTimeSeeder> seeders;
	private final List<TurStartupReconciler> reconcilers;

	public TurOnStartup(TurConfigVarRepository turConfigVarRepository,
			List<TurFirstTimeSeeder> seeders,
			List<TurStartupReconciler> reconcilers) {
		this.turConfigVarRepository = turConfigVarRepository;
		this.seeders = seeders.stream().sorted(Comparator.comparingInt(TurFirstTimeSeeder::seedOrder)).toList();
		this.reconcilers = reconcilers.stream()
				.sorted(Comparator.comparingInt(TurStartupReconciler::reconcileOrder)).toList();
	}

	@Override
	public void run(ApplicationArguments arg0) {
		if (turConfigVarRepository.findById(FIRST_TIME).isEmpty()) {
			log.info("First Time Configuration ...");
			seeders.forEach(TurFirstTimeSeeder::createDefaultRows);
			log.info("First Time Configuration completed. Set the admin password via the web interface.");
		}

		// Always-run, idempotent reconciliations — they must reach EXISTING installs
		// too (the block above runs only on a fresh database): the headless admin
		// password (TURING_ADMIN_PASSWORD), newly-added built-in LLM vendors (T754)
		// and the embedding-model unification (T755). Each is a no-op once applied.
		reconcilers.forEach(TurStartupReconciler::reconcileOnStartup);
	}

}