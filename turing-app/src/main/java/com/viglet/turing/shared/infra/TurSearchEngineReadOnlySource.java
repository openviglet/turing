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

package com.viglet.turing.shared.infra;

/**
 * Whether the search-engine instance comes from the deployment's configuration
 * and so cannot be edited in the console. The searchengine module implements it,
 * so the feature flags in the system module read it without depending on that
 * module (T1311, ADR 0005).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public interface TurSearchEngineReadOnlySource {

    boolean isReadOnly();
}
