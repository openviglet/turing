/*
 * Copyright (C) 2016-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viglet.turing.system;

import com.viglet.turing.system.properties.TurStorageType;

/**
 * T1322l — whether object storage is configured, and which kind, for the
 * features and system-info endpoints. Declared by system and extended by the
 * storage module's {@code TurStorageService}, so system does not import storage
 * (ADR 0005 section 6).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public interface TurStorageStatus {

    TurStorageType getType();

    boolean isEnabled();
}
