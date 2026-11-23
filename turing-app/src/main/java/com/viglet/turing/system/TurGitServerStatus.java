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

/**
 * T1322l — whether the embedded Git server is on, for the features endpoint.
 * Declared by system and implemented by the storage module's
 * {@code TurGitServerService}, so system does not import storage (ADR 0005
 * section 6).
 *
 * @author Alexandre Oliveira
 * @since 2026.3.8
 */
public interface TurGitServerStatus {

    boolean isEnabled();
}
