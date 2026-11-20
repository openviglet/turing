/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.viglet.turing.shared.onstartup;

/**
 * A module's seeding of its own tables on a fresh install. The startup runner
 * collects every seeder and runs them in {@link #seedOrder()} order once, while
 * the {@code FIRST_TIME} marker is absent, so the runner never names a module
 * (T1311, ADR 0005). Seeding stays first-time only on purpose: re-seeding on
 * every boot would bring back a default user, role or locale an admin deleted.
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public interface TurFirstTimeSeeder {

    /** Locales 10, roles 20, groups 30, users 40, search engines 50, LLMs 60, stores 70, config 80. */
    int seedOrder();

    void createDefaultRows();
}
