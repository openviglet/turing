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

package com.viglet.turing.sn.api;

import com.viglet.turing.sn.persistence.model.genai.TurSNSiteGenAi;
import com.viglet.turing.sn.persistence.repository.genai.TurSNSiteGenAiRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * What other modules may call on {@code TurSNSiteGenAiRepository}, under its own names, so the
 * repository stays internal to sn (T1312, ADR 0005). Each method delegates.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Service
public class TurSNSiteGenAis {
    private final TurSNSiteGenAiRepository turSNSiteGenAiRepository;

    public TurSNSiteGenAis(TurSNSiteGenAiRepository turSNSiteGenAiRepository) {
        this.turSNSiteGenAiRepository = turSNSiteGenAiRepository;
    }

    public List<TurSNSiteGenAi> findByTurAIAgentId(String agentId) {
        return turSNSiteGenAiRepository.findByTurAIAgentId(agentId);
    }

    public List<TurSNSiteGenAi> findAll() {
        return turSNSiteGenAiRepository.findAll();
    }

    public TurSNSiteGenAi saveAndFlush(TurSNSiteGenAi entity) {
        return turSNSiteGenAiRepository.saveAndFlush(entity);
    }

    public Optional<TurSNSiteGenAi> findById(String id) {
        return turSNSiteGenAiRepository.findById(id);
    }

    public TurSNSiteGenAi save(TurSNSiteGenAi entity) {
        return turSNSiteGenAiRepository.save(entity);
    }
}