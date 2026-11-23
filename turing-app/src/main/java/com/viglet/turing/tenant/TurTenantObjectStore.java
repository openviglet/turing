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
 * T1322l — the object store as tenant teardown purges it: whether it is on, and
 * delete everything under a prefix (the tenant-scoped store resolves the prefix
 * under the tenant's own folder). Declared by tenant and extended by the storage
 * module's {@code TurStorageService}, so tenant does not import storage (ADR 0005
 * section 6).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public interface TurTenantObjectStore {

    boolean isEnabled();

    void deleteObjectsWithPrefix(String prefix);
}
