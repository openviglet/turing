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
 * ADR 0005 — the shared kernel: the few types every module may depend on, such as the
 * API exceptions controllers map to HTTP statuses (T1307). It depends on no other
 * module and holds no business logic. A type earns a place here only when three or
 * more modules need it. An open module: every package of it is exposed, since being
 * used by every module is what it is for.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
@org.springframework.modulith.ApplicationModule(displayName = "Shared kernel",
        type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.viglet.turing.shared;
