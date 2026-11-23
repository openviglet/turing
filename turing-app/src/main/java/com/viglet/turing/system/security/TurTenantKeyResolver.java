package com.viglet.turing.system.security;

import com.viglet.core.crypto.VigletTenantKeyResolver;
import com.viglet.core.tenancy.VigletTenantContext;

/**
 * Bridges the tenant context onto the {@code viglet-core}
 * {@link VigletTenantKeyResolver} so the lifted crypto primitive can derive
 * per-tenant keys without depending on any Turing type (Block Q / T368).
 *
 * <p>The default tenant maps to the legacy global key; every other tenant gets
 * its own derived key.
 *
 * <p>T1322l — typed on viglet-core's {@link VigletTenantContext} rather than the
 * tenant module's subclass, so system does not import tenant (ADR 0005 section
 * 6: tenant sits above system). The tenant module's bean is that subclass, so
 * the wiring is unchanged.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.4
 */
public class TurTenantKeyResolver implements VigletTenantKeyResolver {

    /**
     * The default tenant's id when no context is wired (tests, non-tenant
     * contexts). Equal to {@code TurTenant.DEFAULT_TENANT_ID}, which
     * {@code TurTenantKeyResolverDefaultIdTest} pins.
     */
    static final String DEFAULT_TENANT_ID = "DEFAULT";

    private final VigletTenantContext tenantContext;

    public TurTenantKeyResolver(VigletTenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    @Override
    public String currentTenantId() {
        return tenantContext != null ? tenantContext.resolveCurrentTenant() : null;
    }

    @Override
    public boolean isDefaultTenant(String tenantId) {
        String defaultTenantId = tenantContext != null ? tenantContext.getDefaultTenantId()
                : DEFAULT_TENANT_ID;
        return defaultTenantId.equals(tenantId);
    }
}
