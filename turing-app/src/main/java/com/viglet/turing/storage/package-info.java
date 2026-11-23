/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * ADR 0005 — the storage module (layer 2): object storage, assets and the Git server.
 * Its API is this package plus the packages marked {@code @NamedInterface}; every
 * other package is internal. It may depend only on the modules below it in ADR 0005
 * section 6 (T1322).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@org.springframework.modulith.ApplicationModule(displayName = "Storage",
        allowedDependencies = {
        "shared", "shared :: *", "system", "system :: *", "tenant", "tenant :: *", "commons",
        "commons :: *", "se", "se :: *", "client", "client :: *", "spring", "spring :: *" })
package com.viglet.turing.storage;
