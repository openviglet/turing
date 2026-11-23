/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */
package com.viglet.turing.system.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.viglet.core.tenancy.VigletTenantContext;
import com.viglet.turing.tenant.persistence.model.TurTenant;

/**
 * T1322l — system no longer imports tenant, so the key resolver's own default
 * tenant id must stay equal to the tenant module's, or the default tenant's
 * secrets would stop decrypting under the legacy global key.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
class TurTenantKeyResolverDefaultIdTest {

    @Test
    void theFallbackDefaultIdIsTheTenantModulesOwn() {
        assertThat(TurTenantKeyResolver.DEFAULT_TENANT_ID).isEqualTo(TurTenant.DEFAULT_TENANT_ID);
    }

    @Test
    void withoutAContextTheDefaultTenantIsStillRecognised() {
        TurTenantKeyResolver resolver = new TurTenantKeyResolver(null);

        assertThat(resolver.currentTenantId()).isNull();
        assertThat(resolver.isDefaultTenant(TurTenant.DEFAULT_TENANT_ID)).isTrue();
        assertThat(resolver.isDefaultTenant("acme")).isFalse();
        assertThat(resolver.isDefaultTenant(null)).isFalse();
    }

    @Test
    void withAContextTheContextNamesTheDefaultTenant() {
        TurTenantKeyResolver resolver = new TurTenantKeyResolver(
                new VigletTenantContext(true, TurTenant.DEFAULT_TENANT_ID));

        assertThat(resolver.isDefaultTenant(TurTenant.DEFAULT_TENANT_ID)).isTrue();
        assertThat(resolver.isDefaultTenant("acme")).isFalse();
    }
}
