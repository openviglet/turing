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

/**
 * T1322l — one resource a plan limits (agents, sites, monthly tokens), counted
 * by the module that owns it. Declared by tenant and implemented by each feature
 * module, so the quota counter imports none of them (ADR 0005 section 6: feature
 * modules sit above tenant). {@link TurVigletResourceCounter} routes a resource
 * key to the bean that counts it.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public interface TurTenantResourceCount {

    /** The resource key this counts, e.g. {@link TurVigletResourceCounter#AGENTS}. */
    String resourceKey();

    /** The current amount for {@code tenantId} (the repositories are tenant-filtered). */
    long count(String tenantId);
}
