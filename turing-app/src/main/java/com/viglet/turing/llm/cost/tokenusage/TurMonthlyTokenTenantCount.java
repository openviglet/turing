/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.llm.cost.tokenusage;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.viglet.turing.llm.persistence.repository.TurLLMTokenUsageRepository;
import com.viglet.turing.tenant.TurTenantResourceCount;
import com.viglet.turing.tenant.TurVigletResourceCounter;

/**
 * T1322l — the tokens a tenant consumed this calendar month (T276 attribution),
 * which a plan limits; counted by the module that records token usage.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Component
public class TurMonthlyTokenTenantCount implements TurTenantResourceCount {

    private final TurLLMTokenUsageRepository tokenUsageRepository;

    public TurMonthlyTokenTenantCount(TurLLMTokenUsageRepository tokenUsageRepository) {
        this.tokenUsageRepository = tokenUsageRepository;
    }

    @Override
    public String resourceKey() {
        return TurVigletResourceCounter.MONTHLY_TOKENS;
    }

    @Override
    public long count(String tenantId) {
        LocalDateTime monthStart = LocalDateTime.now(ZoneId.systemDefault())
                .withDayOfMonth(1).toLocalDate().atStartOfDay();
        return tokenUsageRepository.findUsageByTenant(monthStart, monthStart.plusMonths(1)).stream()
                .filter(row -> tenantId != null && tenantId.equals(row[0]))
                .mapToLong(row -> ((Number) row[3]).longValue())
                .sum();
    }
}
