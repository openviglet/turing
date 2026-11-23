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

import java.util.Optional;

/**
 * T1322l — where a tenant that hit a plan limit can upgrade, for the quota
 * refusal's {@code upgradeUrl}. Declared by tenant and implemented by llm's
 * {@code TurVigletUpgradeTargetService}, so tenant does not import llm (ADR 0005
 * section 6).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public interface TurTenantUpgradeTarget {

    /** The upgrade URL for {@code tenantId} after hitting {@code resource}, or empty. */
    Optional<String> upgradeUrl(String tenantId, String resource);
}
