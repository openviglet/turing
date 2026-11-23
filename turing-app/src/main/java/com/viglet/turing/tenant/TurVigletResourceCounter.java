/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.tenant;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.viglet.core.tenancy.VigletPlanLimits;
import com.viglet.core.tenancy.VigletResourceCounter;

/**
 * T396 / §XIV.9 — the {@link VigletResourceCounter} SPI feeding the shared
 * {@code VigletQuotaService}: Turing supplies the per-plan limits and the current
 * per-tenant counts; the compare-and-throw enforcement is shared.
 *
 * <p>The resource keys are the opaque contract between this counter and the
 * Turing quota call sites: {@value #AGENTS}, {@value #SITES},
 * {@value #MONTHLY_TOKENS}. T1322l — each is counted by the module that owns it,
 * through {@link TurTenantResourceCount}: agents and sites from their
 * {@code @TenantId}-filtered repositories, tokens from T276's per-tenant
 * attribution.
 *
 * <p>Plan mapping (lifted from {@code TurTenantPlanLimits}): the {@code FREE} plan
 * is bounded (3 agents, 2 sites, 100k monthly tokens); any other plan is unlimited
 * until a richer billing model lands, so a paying tenant is never blocked by
 * accident.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
@Component
public class TurVigletResourceCounter implements VigletResourceCounter {

    public static final String AGENTS = "agents";
    public static final String SITES = "sites";
    public static final String MONTHLY_TOKENS = "monthlyTokens";

    /** The bounded FREE plan: 3 agents, 2 sites, 100k monthly tokens. */
    private static final VigletPlanLimits FREE = VigletPlanLimits.of(
            AGENTS, 3L, SITES, 2L, MONTHLY_TOKENS, 100_000L);

    private final Map<String, TurTenantResourceCount> counts;

    /** T1322l — each resource is counted by the module that owns it. */
    public TurVigletResourceCounter(List<TurTenantResourceCount> counts) {
        this.counts = counts.stream().collect(Collectors.toMap(TurTenantResourceCount::resourceKey,
                Function.identity()));
    }

    @Override
    public long count(String tenantId, String resourceKey) {
        TurTenantResourceCount count = counts.get(resourceKey);
        return count == null ? 0L : count.count(tenantId);
    }

    @Override
    public VigletPlanLimits limitsFor(String plan) {
        if (plan != null && "FREE".equals(plan.trim().toUpperCase(Locale.ROOT))) {
            return FREE;
        }
        return VigletPlanLimits.UNLIMITED;
    }
}
