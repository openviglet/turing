/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.sn;

import org.springframework.stereotype.Component;

import com.viglet.turing.sn.api.TurSNSites;
import com.viglet.turing.tenant.TurTenantResourceCount;
import com.viglet.turing.tenant.TurVigletResourceCounter;

/**
 * T1322l — the SN sites a plan limits, counted by the module that owns them. The
 * repository is {@code @TenantId}-filtered, so {@code count()} is the current
 * tenant's.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@Component
public class TurSNSiteTenantCount implements TurTenantResourceCount {

    private final TurSNSites snSites;

    public TurSNSiteTenantCount(TurSNSites snSites) {
        this.snSites = snSites;
    }

    @Override
    public String resourceKey() {
        return TurVigletResourceCounter.SITES;
    }

    @Override
    public long count(String tenantId) {
        return snSites.count();
    }
}
