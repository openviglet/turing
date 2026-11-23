/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.agent;

import org.springframework.stereotype.Component;

import com.viglet.turing.agent.persistence.repository.TurAIAgentRepository;
import com.viglet.turing.tenant.TurTenantResourceCount;
import com.viglet.turing.tenant.TurVigletResourceCounter;

/**
 * T1322l — the agents a plan limits, counted by the module that owns them. The
 * repository is {@code @TenantId}-filtered, so {@code count()} is the current
 * tenant's.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Component
public class TurAgentTenantCount implements TurTenantResourceCount {

    private final TurAIAgentRepository agentRepository;

    public TurAgentTenantCount(TurAIAgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    @Override
    public String resourceKey() {
        return TurVigletResourceCounter.AGENTS;
    }

    @Override
    public long count(String tenantId) {
        return agentRepository.count();
    }
}
